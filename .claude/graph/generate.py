#!/usr/bin/env python3
"""Regenerates the project graph that coding agents read instead of the whole source tree.

Version 9 (2026-09-11). This script is copied between projects; bump the number whenever you
change it, so the newest copy is the one that says so rather than the one you have to diff.

A repository can hold more than one project: a single Maven module, a multi-module build, or --
as happens when one application is vendored into another's repository -- unrelated applications
side by side, each with its own pom and its own base package. All of them end up in one graph,
each keeping its own short package names, and nothing is wired across the seam between them.
The modules of one build are one project: they call each other, and the graph links them.

    python .claude/graph/generate.py

Writes .claude/graph/graph.json (machine) and .claude/graph/PROJECT_GRAPH.md (agent/human).
The prose overview at the top of the Markdown comes from .claude/graph/overview.md when that
file exists; it is hand-written and left alone by this script. Everything else is derived from
the sources.

Deliberately dependency-free and regex-based. It understands the conventions this codebase
actually uses -- Lombok constructor injection, Spring stereotypes, @ConfigurationProperties,
JPA entities, Liquibase changelogs -- and nothing beyond them. When a convention changes,
teach it here rather than hand-editing the output, which is overwritten on every run.
"""
from __future__ import annotations

import json
import re
from datetime import date
from pathlib import Path

OUT_DIR = Path(__file__).resolve().parent
ROOT = OUT_DIR.parents[1]


BUILD_DIRS = {"target", "build", "out", "node_modules"}


def generated(dirs: tuple[str, ...]) -> bool:
    """Whether a path runs through a directory a build writes into, or a hidden one."""
    return any(p in BUILD_DIRS or p.startswith(".") for p in dirs)


def source_trees(*parts: str) -> list[Path]:
    """Every source tree of that kind anywhere in the repository, at any depth.

    Found by walking rather than by reading `<modules>` from the pom, because a project is not
    always a module: an application vendored into another's repository keeps its own pom and is
    listed nowhere, so reading the pom would leave its whole source tree out of the graph.
    Directories a build writes into are skipped, so a copy of the sources under `target/` is
    never mistaken for a second project.
    """
    trees = []
    for tree in ROOT.rglob("/".join(parts)):
        if tree.is_dir() and not generated(tree.relative_to(ROOT).parts[:-len(parts)]):
            trees.append(tree)
    return sorted(trees)


MAIN_SRCS = source_trees("src", "main", "java")
TEST_SRCS = source_trees("src", "test", "java")
RESOURCE_DIRS = source_trees("src", "main", "resources")


def common_package(packages: list[str]) -> str:
    """The leading segments every package shares, stopping at the first one they disagree on."""
    split = [p.split(".") for p in packages if p]
    common: list[str] = []
    for segments in zip(*split) if split else ():
        if len(set(segments)) > 1:
            break
        common.append(segments[0])
    return ".".join(common)


def base_package(src: Path) -> str:
    """The root package of one source tree: the longest prefix its own files share.

    Detected rather than configured, so this script can be copied into another project as is,
    and held per tree rather than once for the repository. Two unrelated applications share
    nothing above `com`, and a base package that short would strip nothing from the package
    names while making every `com.fasterxml…` import look like project code.
    """
    packages = []
    for path in src.rglob("*.java"):
        m = re.search(r"(?m)^package\s+([\w.]+);", path.read_text(encoding="utf-8", errors="replace"))
        if m:
            packages.append(m.group(1))
    return common_package(packages)


def aggregators() -> dict[Path, Path]:
    """Each module directory, mapped to the directory of the pom that lists it in `<modules>`."""
    listed_by: dict[Path, Path] = {}
    for pom in ROOT.rglob("pom.xml"):
        if generated(pom.relative_to(ROOT).parts[:-1]):
            continue
        text = re.sub(r"<!--.*?-->", "", pom.read_text(encoding="utf-8", errors="replace"), flags=re.S)
        for name in re.findall(r"<module>\s*([^<]+?)\s*</module>", text):
            module = (pom.parent / name).resolve()
            listed_by[module.parent if module.suffix == ".xml" else module] = pom.parent
    return listed_by


def label_of(home: Path) -> str:
    return "" if home == ROOT else home.relative_to(ROOT).as_posix()


def projects() -> list[dict]:
    """One entry per project: where it lives, its base package, and the modules it is built from.

    A module is a directory holding `src/`; its main tree names its base package, or its test
    tree when it has no main one. A project is what one build compiles together: a module
    listed in some pom's `<modules>` belongs to the project of that pom, climbing as far as the
    listing goes, and a module listed nowhere -- a single-module build, or an application
    vendored into the repository -- is a project of its own. Taking every module for a project
    would cut each call from one module into another out of the graph; unrelated applications
    are the only seam that must not be crossed.

    The base package is the one the whole project shares, so a short package keeps the segment
    that says which module it is in, and a location can drop the module directory.
    """
    listed_by = aggregators()
    found: dict[Path, dict] = {}
    for home in sorted({src.parents[2] for src in MAIN_SRCS + TEST_SRCS}):
        top, seen = home, set()
        while top in listed_by and top not in seen:
            seen.add(top)
            top = listed_by[top]
        trees = [home / "src" / "main" / "java", home / "src" / "test" / "java"]
        base = next((b for b in (base_package(t) for t in trees if t.is_dir()) if b), "")
        project = found.setdefault(top, {"dir": top, "label": label_of(top), "modules": []})
        project["modules"].append({"dir": home, "label": label_of(home), "base": base})
    for project in found.values():
        project["bases"] = sorted({m["base"] for m in project["modules"] if m["base"]})
        project["base"] = common_package(project["bases"])
    return sorted(found.values(), key=lambda p: (p["label"] != "", p["label"]))


PROJECTS = projects()
MULTI = len(PROJECTS) > 1
BASE_PKGS = sorted({p["base"] for p in PROJECTS if p["base"]}, key=len, reverse=True)


def owner_of(path: Path) -> tuple[dict, dict]:
    """The module a file belongs to, and the project that module is built in.

    The deepest module, not the first: a vendored project can sit inside another's module
    directory, and both would claim its files.
    """
    owners = [(m, p) for p in PROJECTS for m in p["modules"] if path.is_relative_to(m["dir"])]
    if not owners:
        return {"label": "", "base": ""}, {"label": "", "base": "", "bases": []}
    return max(owners, key=lambda owner: len(owner[0]["dir"].parts))


def index_of(nodes: list[dict], project: str) -> dict[str, dict]:
    """The types of one project by simple name. Names repeat across projects; a graph must not."""
    return {n["name"]: n for n in nodes if n.get("project", "") == project}

MAPPING_ANNOTATIONS = {
    "GetMapping": "GET",
    "PostMapping": "POST",
    "PutMapping": "PUT",
    "PatchMapping": "PATCH",
    "DeleteMapping": "DELETE",
}

ROLE_ORDER = [
    "application", "controller", "advice", "service", "component", "config", "properties",
    "repository", "entity", "entity-base", "embeddable", "record", "enum", "interface",
    "exception", "class", "test",
]


def rel(path: Path) -> str:
    return path.relative_to(ROOT).as_posix()


# --------------------------------------------------------------------------- java text scanning

def balanced(text: str, i: int, pair: str = "()") -> int:
    """Index just past the closer matching the opener at i, skipping string and text-block literals."""
    opener, closer = pair
    depth = 0
    n = len(text)
    while i < n:
        c = text[i]
        if text.startswith('"""', i):
            end = text.find('"""', i + 3)
            i = n if end < 0 else end + 3
            continue
        if c in '"\'':
            quote = c
            i += 1
            while i < n and text[i] != quote:
                i += 2 if text[i] == "\\" else 1
            i += 1
            continue
        if c == opener:
            depth += 1
        elif c == closer:
            depth -= 1
            if depth == 0:
                return i + 1
        i += 1
    return n


def annotations_in(segment: str) -> list[tuple[str, str]]:
    """Every @Annotation(args) in a slice of source, in order."""
    found: list[tuple[str, str]] = []
    i = 0
    while True:
        at = segment.find("@", i)
        if at < 0:
            return found
        m = re.match(r"@(\w+)", segment[at:])
        if not m:
            i = at + 1
            continue
        k = at + m.end()
        while k < len(segment) and segment[k] in " \t\r\n":
            k += 1
        args = ""
        if k < len(segment) and segment[k] == "(":
            end = balanced(segment, k)
            args = segment[k + 1:end - 1]
            k = end
        found.append((m.group(1), args))
        i = k


JAVADOC_RE = re.compile(r"/\*\*(.*?)\*/", re.S)
BLOCK_TAG_RE = re.compile(r"(?m)^\s*@(param|return|throws|exception|see|since|deprecated)\b")
# Containers whose first type argument names a key rather than an injected bean.
KEYED_CONTAINERS = {"Map", "HashMap", "LinkedHashMap", "EnumMap", "TreeMap", "SortedMap",
                    "NavigableMap", "ConcurrentMap", "ConcurrentHashMap", "MultiValueMap"}

TYPE_DECL_RE = re.compile(
    r"(?m)^(?:public\s+)?(?:(?:final|abstract|sealed|non-sealed|static)\s+)*"
    r"(class|interface|enum|record)\s+(\w+)"
)
# Covers `int`, `List<String>` and `int[]` alike. An array-typed @ConfigurationProperties
# field binds indexed keys (white-lower[0]) and has to be counted like any other.
FIELD_TYPE = r"[\w.]+(?:<[^<>;=]*>)?(?:\s*\[\s*\])*"
FINAL_FIELD_RE = re.compile(r"(?m)^[ \t]+private\s+final\s+(" + FIELD_TYPE + r")\s+(\w+)\s*;")
ANY_FIELD_RE = re.compile(r"(?m)^[ \t]+private\s+(?!static)(" + FIELD_TYPE + r")\s+(\w+)\s*[;=]")


# What may stand between a javadoc and the declaration it documents. A modifier may; anything
# else means the comment belongs to something earlier.
GAP_MODIFIERS = {"public", "protected", "private", "static", "final", "abstract", "default",
                 "synchronized", "native", "transient", "volatile", "strictfp", "sealed",
                 "non-sealed"}


def documents(text: str, doc_end: int, idx: int) -> bool:
    """Whether the javadoc ending at doc_end is the one for the declaration starting at idx.

    Only whitespace, comments, annotations and modifiers may separate the two. Taking the nearest
    javadoc without this check gave every undocumented method the note of the last documented one
    above it, so one @param about a single argument turned up as the summary of three unrelated
    endpoints.
    """
    gap = text[doc_end:idx]
    i = 0
    while i < len(gap):
        if gap[i].isspace():
            i += 1
        elif gap.startswith("//", i):
            end = gap.find("\n", i)
            i = len(gap) if end < 0 else end + 1
        elif gap.startswith("/*", i):
            end = gap.find("*/", i + 2)
            i = len(gap) if end < 0 else end + 2
        elif gap[i] == "@":
            name_end = i + 1
            while name_end < len(gap) and (gap[name_end].isalnum() or gap[name_end] in "_."):
                name_end += 1
            args = name_end
            while args < len(gap) and gap[args].isspace():
                args += 1
            # balanced() steps over string literals, so a brace or parenthesis inside an
            # annotation argument cannot be mistaken for the start of a body.
            i = balanced(gap, args) if args < len(gap) and gap[args] == "(" else name_end
        else:
            word = re.match(r"[\w-]+", gap[i:])
            if not word or word.group(0) not in GAP_MODIFIERS:
                return False
            i += word.end()
    return True


def javadoc_before(text: str, idx: int) -> str:
    last = None
    for m in JAVADOC_RE.finditer(text, 0, idx):
        last = m
    if last is None or not documents(text, last.end(), idx):
        return ""
    return last.group(1)


def first_tag_text(body: str) -> str:
    """The one block tag worth reading, for a javadoc that is nothing but block tags.

    Such a comment still says what the method is for, so it beats an empty cell -- but only one
    tag at a time. Stripping the markers off every line instead ran two unrelated notes together
    into one sentence, and neither of them ended in a full stop to cut it back at.

    A @param or @throws keeps its subject, which is what stops a note about one argument from
    being read as a summary of the method itself.
    """
    parts = BLOCK_TAG_RE.split(body)
    if len(parts) < 3:
        return ""
    tag, text = parts[1], parts[2]
    if tag in ("param", "throws", "exception"):
        subject, _, rest = text.strip().partition(" ")
        return f"{subject} — {rest.strip()}" if rest.strip() else ""
    return text


def summarize(doc: str, limit: int = 200) -> str:
    """First sentence of a javadoc block, stripped of markup, on one line."""
    if not doc:
        return ""
    body = re.sub(r"(?m)^\s*\*\s?", "", doc)
    body = body.split("<p>")[0]
    head = BLOCK_TAG_RE.split(body)[0]
    if not head.strip():
        head = first_tag_text(body)
    body = head
    for pkg in BASE_PKGS:
        body = body.replace(pkg + ".", "")
    body = re.sub(r"\{@\w+\s+([^}]*)\}", r"\1", body)
    body = re.sub(r"\{@\w+\}", "", body)
    body = re.sub(r"<[^>]+>", "", body)
    body = " ".join(body.split())
    m = re.match(r"(.+?[.!?])(\s|$)", body)
    first = m.group(1) if m else body
    if len(first) > limit:
        first = first[:limit - 1].rstrip() + "…"
    return first.replace("|", "\\|")


def line_of(text: str, idx: int) -> int:
    return text.count("\n", 0, idx) + 1


def method_after(text: str, pos: int) -> str | None:
    """Name of the first method declared after pos, skipping further annotations and comments."""
    for raw in text[pos:pos + 900].splitlines():
        line = raw.strip()
        if not line or line.startswith(("@", "//", "*", "/*")):
            continue
        m = re.search(r"(\w+)\s*\(", line)
        return m.group(1) if m else None
    return None


def paths_from_args(args: str) -> list[str]:
    quoted = re.findall(r'"([^"]*)"', args)
    return quoted or [""]


def kebab(name: str) -> str:
    return re.sub(r"(?<!^)(?=[A-Z])", "-", name).lower()


CONSTANT_REF_RE = re.compile(r"\b(?:\w+\.)*([A-Z][A-Z0-9]*(?:_[A-Z0-9]+)+|[A-Z]{2,})\b")


def configuration_prefix(args: str, body: str) -> str:
    """The prefix of an @ConfigurationProperties, written as a literal or as a constant.

    `prefix = Foo.PREFIX` is what lets @ConditionalOnProperty, the tests and the class itself
    name one string instead of three copies of it, so it is worth understanding: reading only
    the literal form leaves such a class with no prefix at all and files `telegram.bot.token`
    under `token`, where nobody looking for the real key will find it, while `enabled` collides
    with the `enabled` of every other prefix-less class in the same table.

    The constant is looked up in the class being parsed, which is where that idiom keeps it.
    One declared in another class stays unresolved; an empty prefix is the honest answer then.
    """
    literal = re.search(r'"([^"]+)"', args)
    if literal:
        return literal.group(1)
    ref = CONSTANT_REF_RE.search(args)
    if not ref:
        return ""
    const = re.search(rf'static\s+final\s+String\s+{ref.group(1)}\s*=\s*"([^"]+)"', body)
    return const.group(1) if const else ""


NESTED_CLASS_RE = re.compile(
    r"(?m)^[ \t]*(?:(?:public|protected|private|static|final|abstract)\s+)*class\s+(\w+)\b[^{;]*\{")


def nested_groups(body: str) -> tuple[str, dict[str, str]]:
    """A class body with its nested classes cut out, and those classes by name.

    Cut out rather than skipped over, so that one scan of what remains sees the outer fields
    and only those: a field of a nested class is indented like any other and is otherwise
    indistinguishable from a field of the class that holds it.
    """
    groups: dict[str, str] = {}
    rest = body
    while True:
        m = NESTED_CLASS_RE.search(rest)
        if not m:
            return rest, groups
        opened = m.end() - 1
        end = balanced(rest, opened, "{}")
        groups[m.group(1)] = rest[opened + 1:end - 1]
        rest = rest[:m.start()] + "\n" + rest[end:]


def property_keys(prefix: str, body: str) -> list[str]:
    """Every key a properties class binds, nested groups included.

    A field whose type is one of the class's own nested classes is a group rather than a key:
    it contributes a segment to the path, and its fields hang below that segment. Reading the
    two as one flat level invents keys that nothing binds -- `monitoring.pool-size` for what
    Spring reads as `monitoring.scheduler.pool-size` -- and files the group name itself,
    `monitoring.scheduler`, as though a value were expected there.
    """
    outer, groups = nested_groups(body)
    keys: list[str] = []
    for typ, field in ANY_FIELD_RE.findall(outer):
        path = f"{prefix}.{kebab(field)}" if prefix else kebab(field)
        group = re.sub(r"<.*", "", typ).split(".")[-1]
        keys += property_keys(path, groups[group]) if group in groups else [path]
    return keys


def method_body(text: str, pos: int) -> str:
    """Rough slice of the method starting at pos: up to the closing brace at method indentation."""
    end = text.find("\n    }", pos)
    return text[pos:end if end > 0 else min(len(text), pos + 2000)]


# --------------------------------------------------------------------------- one file -> one node

def parse_java(path: Path, is_test: bool) -> dict | None:
    text = path.read_text(encoding="utf-8", errors="replace")

    decl = TYPE_DECL_RE.search(text)
    if not decl:
        return None
    kind, name = decl.group(1), decl.group(2)

    module, project = owner_of(path)
    base = project["base"]

    pkg_m = re.search(r"(?m)^package\s+([\w.]+);", text)
    package = pkg_m.group(1) if pkg_m else ""
    inside = not base or package == base or package.startswith(base + ".")
    short_pkg = (package[len(base):].lstrip(".") if inside else package) or "(root)"

    # Against the modules' own base packages rather than the project's: those are what tell a
    # class of the project from a library, even when the modules share little more than `com`.
    imports = re.findall(r"(?m)^import\s+(?:static\s+)?([\w.]+);", text)
    project_imports = sorted({i.split(".")[-1] for i in imports
                              if any(i.startswith(b + ".") for b in project["bases"])})

    header_end = text.find("{", decl.end())
    header = text[decl.end():header_end if header_end > 0 else decl.end()]
    extends = re.search(r"\bextends\s+([\w.]+(?:<[^{]*>)?)", header)
    implements = re.search(r"\bimplements\s+([^{]+)", header)

    type_annotations = annotations_in(text[:decl.start()])
    ann_names = {a for a, _ in type_annotations}
    ann_args = {a: v for a, v in type_annotations}

    body = text[header_end:] if header_end > 0 else ""

    node = {
        "name": name,
        "kind": kind,
        "package": short_pkg,
        "project": project["label"],
        "module": module["label"],
        "file": rel(path),
        "line": line_of(text, decl.start()),
        "lines": text.count("\n") + 1,
        "summary": summarize(javadoc_before(text, decl.start())),
        "annotations": sorted(ann_names),
        "extends": extends.group(1).strip() if extends else None,
        "implements": [s.strip() for s in implements.group(1).split(",")] if implements else [],
        "uses": project_imports,
        "test": is_test,
    }

    # role -------------------------------------------------------------------
    if is_test:
        role = "test"
    elif "SpringBootApplication" in ann_names:
        role = "application"
    elif ann_names & {"RestController", "Controller"}:
        role = "controller"
    elif ann_names & {"RestControllerAdvice", "ControllerAdvice"}:
        role = "advice"
    elif "ConfigurationProperties" in ann_names:
        role = "properties"
    elif "Entity" in ann_names:
        role = "entity"
    elif "MappedSuperclass" in ann_names:
        role = "entity-base"
    elif "Embeddable" in ann_names:
        role = "embeddable"
    elif "Configuration" in ann_names:
        role = "config"
    elif "Service" in ann_names:
        role = "service"
    elif "Component" in ann_names:
        role = "component"
    elif kind == "interface" and node["extends"] and "Repository" in node["extends"]:
        role = "repository"
    elif "Filter" in (node["extends"] or "") or any("Filter" in i for i in node["implements"]):
        role = "filter"
    elif kind == "enum":
        role = "enum"
    elif kind == "record":
        role = "record"
    elif name.endswith("Exception"):
        role = "exception"
    elif kind == "interface":
        role = "interface"
    else:
        role = "class"
    node["role"] = role

    # injected collaborators -------------------------------------------------
    injects = [[t, f] for t, f in FINAL_FIELD_RE.findall(body)]
    ctor = re.search(r"(?m)^[ \t]+(?:public|protected)\s+" + re.escape(name) + r"\s*\(", body)
    if ctor:
        open_paren = body.index("(", ctor.end() - 1)
        params = body[open_paren:balanced(body, open_paren)]
        for ptype, pname in re.findall(r"([\w.]+(?:<[^<>]*>)?)\s+(\w+)\s*(?=[,)])", params):
            if [ptype, pname] not in injects:
                injects.append([ptype, pname])
    node["injects"] = injects
    by_field = {f: t for t, f in injects}

    def collaborators(slice_text: str) -> list[str]:
        return sorted({by_field[f] for f in by_field if re.search(r"\b" + re.escape(f) + r"\b", slice_text)})

    # endpoints, jobs, beans, handlers ---------------------------------------
    base_path = paths_from_args(ann_args["RequestMapping"])[0] if "RequestMapping" in ann_args else ""
    endpoints: list[dict] = []
    jobs: list[dict] = []
    beans: list[dict] = []
    handlers: list[dict] = []
    for m in re.finditer(r"@(\w+)", body):
        ann = m.group(1)
        if ann not in MAPPING_ANNOTATIONS and ann not in {"Scheduled", "Bean", "ExceptionHandler"}:
            continue
        k = m.end()
        while k < len(body) and body[k] in " \t\r\n":
            k += 1
        args = ""
        if k < len(body) and body[k] == "(":
            end = balanced(body, k)
            args = body[k + 1:end - 1]
            k = end
        method = method_after(body, k)
        doc = summarize(javadoc_before(body, m.start()))
        calls = collaborators(method_body(body, k))
        line = line_of(text, header_end + m.start())
        if ann in MAPPING_ANNOTATIONS:
            for p in paths_from_args(args):
                if p.startswith("/") or not p:
                    endpoints.append({"http": MAPPING_ANNOTATIONS[ann], "path": (base_path + p) or "/",
                                      "method": method, "summary": doc, "calls": calls, "line": line})
        elif ann == "Scheduled":
            pairs = re.findall(r'(\w+)\s*=\s*"([^"]*)"', args)
            every = ", ".join(f"{k.replace('String', '')}={v}" for k, v in pairs) or " ".join(args.split())
            jobs.append({"method": method, "every": every, "summary": doc, "calls": calls, "line": line})
        elif ann == "Bean":
            beans.append({"method": method, "line": line})
        else:
            handlers.append({"method": method, "handles": re.findall(r"(\w+)\.class", args), "line": line})
    node["endpoints"] = endpoints
    node["jobs"] = jobs
    node["beans"] = beans
    node["handlers"] = handlers

    # persistence ------------------------------------------------------------
    if role in {"entity", "entity-base", "embeddable"}:
        table = re.search(r'@Table\s*\((?:[^()]|\([^()]*\))*?name\s*=\s*"([^"]+)"', text, re.S)
        node["table"] = table.group(1) if table else None
        columns = []
        for m in ANY_FIELD_RE.finditer(body):
            back = body[max(0, m.start() - 400):m.start()]
            col = re.findall(r'@Column\s*\((?:[^()]|\([^()]*\))*?name\s*=\s*"([^"]+)"', back, re.S)
            columns.append({"field": m.group(2), "type": m.group(1), "column": col[-1] if col else None})
        node["columns"] = columns
        node["relations"] = [{"kind": k, "field": f} for k, f in
                             re.findall(r"@(OneToOne|ManyToOne|OneToMany|ManyToMany|Embedded)[^;]*?\s(\w+)\s*;", body, re.S)]
    if role == "repository":
        ent = re.search(r"Repository<\s*(\w+)\s*,", node["extends"] or "")
        node["entity"] = ent.group(1) if ent else None
        node["queries"] = sorted({m.group(1) for m in
                                  re.finditer(r"(?m)^[ \t]+[\w.<>,\[\] ]+?\s(\w+)\s*\(", body)})

    # configuration ----------------------------------------------------------
    if role == "properties":
        prefix = configuration_prefix(ann_args.get("ConfigurationProperties", ""), body)
        node["prefix"] = prefix
        node["keys"] = property_keys(prefix, body)

    return node


# --------------------------------------------------------------------------- resources

def parse_properties() -> list[dict]:
    files = []
    for path in sorted(p for d in RESOURCE_DIRS for p in d.glob("application*.properties")):
        keys = []
        for line in path.read_text(encoding="utf-8", errors="replace").splitlines():
            line = line.strip()
            if line and not line.startswith("#") and "=" in line:
                key, value = line.split("=", 1)
                keys.append({"key": key.strip(), "value": value.strip()})
        files.append({"file": rel(path), "keys": keys})
    return files


def parse_liquibase() -> dict:
    """Tables and indexes as the changelogs declare them.

    An indentation-free scan is enough: within one change, `tableName`, `indexName` and the
    `name` of each column are unambiguous, and the enclosing `- <change>:` line says which
    of them is being read.
    """
    # Every project's changelogs, not the first directory that exists: two applications in one
    # repository each own a schema, and reading one of them would silently drop the other's.
    changes_dirs = [d / "db" / "changelog" / "changes" for d in RESOURCE_DIRS]
    tables: dict[str, dict] = {}
    indexes: list[dict] = []
    files = sorted(f for d in changes_dirs if d.exists()
                   for f in list(d.glob("*.yaml")) + list(d.glob("*.yml")))
    for path in files:
        change = table = changeset = index = None
        for line in path.read_text(encoding="utf-8", errors="replace").splitlines():
            stripped = line.strip()
            m = re.match(r"-?\s*id:\s*(\S+)$", stripped)
            if m:
                changeset = m.group(1)
                continue
            m = re.match(r"-\s*(\w+):\s*$", stripped)
            if m:
                if m.group(1) != "column":  # a column belongs to the change already open
                    change = m.group(1)
                    table = index = None
                continue
            m = re.match(r"tableName:\s*(\S+)$", stripped)
            if m:
                table = m.group(1)
                if change == "createTable":
                    tables.setdefault(table, {"table": table, "columns": [], "changeset": changeset,
                                              "file": rel(path)})
                elif index is not None:
                    index["table"] = table
                continue
            m = re.match(r"indexName:\s*(\S+)$", stripped)
            if m:
                index = {"index": m.group(1), "table": table, "columns": [], "file": rel(path)}
                indexes.append(index)
                continue
            m = re.match(r"name:\s*(\S+)$", stripped)
            if m:
                if change == "createTable" and table in tables:
                    tables[table]["columns"].append(m.group(1))
                elif change == "createIndex" and index is not None:
                    index["columns"].append(m.group(1))
    return {"tables": list(tables.values()), "indexes": indexes}


def parse_pom(home: Path) -> dict:
    path = home / "pom.xml"
    if not path.exists():
        return {}
    text = path.read_text(encoding="utf-8", errors="replace")
    # Read the module's own coordinates from the pom with <parent> cut out of it: the parent
    # block declares an artifactId of its own and, coming first, wins every search over the file.
    own = re.sub(r"<parent>.*?</parent>", "", text, flags=re.S)
    artifact = re.search(r"<artifactId>([^<]+)</artifactId>", own)
    java = re.search(r"<java\.version>([^<]+)</java\.version>", text)
    parent = re.search(r"<parent>.*?<version>([^<]+)</version>", text, re.S)
    deps = re.findall(r"<groupId>([^<]+)</groupId>\s*<artifactId>([^<]+)</artifactId>", own)
    return {
        "artifactId": artifact.group(1) if artifact else "",
        "java": java.group(1) if java else "",
        "springBoot": parent.group(1) if parent else "",
        "dependencies": [f"{g}:{a}" for g, a in deps],
    }


# --------------------------------------------------------------------------- graph assembly

def collect() -> dict:
    nodes: list[dict] = []
    for src, is_test in ([(s, False) for s in MAIN_SRCS] + [(s, True) for s in TEST_SRCS]):
        if not src.exists():
            continue
        for path in sorted(src.rglob("*.java")):
            node = parse_java(path, is_test)
            if node:
                nodes.append(node)

    known: dict[str, set[str]] = {}
    for node in nodes:
        known.setdefault(node["project"], set()).add(node["name"])
    edges: list[dict] = []
    seen: set[tuple[str, str, str, str]] = set()
    for node in nodes:
        # Resolution never leaves the node's own project: two applications in one repository
        # can each own a UserService, and by simple name the two are indistinguishable.
        peers = known.get(node["project"], set())
        for typ, field in node["injects"]:
            base = re.sub(r"<.*", "", typ).split(".")[-1]
            args = [t.strip() for part in re.findall(r"<([^<>]*)>", typ) for t in part.split(",")]
            # Spring injects the values of a Map<K, V>; the key is a lookup token, not a collaborator.
            if base in KEYED_CONTAINERS and len(args) > 1:
                args = args[1:]
            for target in [base] + args:
                target = re.sub(r"\s*\[\s*\]", "", target).split(".")[-1]
                edge = (node["project"], node["name"], target, field)
                # One collaborator can reach the same field twice -- List<Strategy> in the
                # constructor, Map<Enum, Strategy> on the field. That is one dependency, not two.
                if target in peers and target != node["name"] and edge not in seen:
                    seen.add(edge)
                    edges.append({"from": node["name"], "to": target, "kind": "injects",
                                  "field": field, "project": node["project"]})

    main_names: dict[str, set[str]] = {}
    for node in nodes:
        if not node["test"]:
            main_names.setdefault(node["project"], set()).add(node["name"])
    tests = []
    for node in nodes:
        if not node["test"]:
            continue
        peers = main_names.get(node["project"], set())
        stem = re.sub(r"(Tests?|IT|ITest)$", "", node["name"])
        if stem in peers:
            covers = stem
        else:
            # ArchiveWriterRollbackTest names what it covers and then what about it.
            prefixes = [n for n in peers if stem.startswith(n)]
            covers = max(prefixes, key=len) if prefixes else None
        tests.append({"test": node["name"], "covers": covers, "project": node["project"],
                      "file": node["file"], "line": node["line"]})

    return {
        "generated": date.today().isoformat(),
        "projects": [{"dir": p["label"] or ".", "base": p["base"], "pom": parse_pom(p["dir"]),
                      "modules": [{"dir": m["label"] or ".", "base": m["base"]} for m in p["modules"]]}
                     for p in PROJECTS],
        "nodes": nodes,
        "edges": edges,
        "tests": tests,
        "properties": parse_properties(),
        "database": parse_liquibase(),
    }


def dependency_tree(graph: dict, root: str, project: str = "", depth: int = 3) -> list[str]:
    by_name = index_of(graph["nodes"], project)
    out_edges: dict[str, list[str]] = {}
    for e in graph["edges"]:
        if e.get("project", "") == project:
            out_edges.setdefault(e["from"], []).append(e["to"])

    lines: list[str] = []

    def walk(name: str, level: int, seen: frozenset[str]) -> None:
        node = by_name.get(name)
        if node is None:
            return
        indent = "  " * level
        role = node["role"]
        lines.append(f"{indent}{name}" + ("" if role == "class" else f"  [{role}]"))
        if level >= depth or name in seen:
            return
        for child in sorted(set(out_edges.get(name, []))):
            if by_name.get(child, {}).get("role") in {"properties", "enum", "record", "exception"}:
                continue
            walk(child, level + 1, seen | {name})

    walk(root, 0, frozenset())
    return lines


# --------------------------------------------------------------------------- markdown rendering

def md_table(headers: list[str], rows: list[list[str]]) -> list[str]:
    if not rows:
        return ["_none_", ""]
    out = ["| " + " | ".join(headers) + " |",
           "|" + "|".join("---" for _ in headers) + "|"]
    out += ["| " + " | ".join(str(c) for c in row) + " |" for row in rows]
    out.append("")
    return out


def source_roots() -> list[tuple[str, str]]:
    """Path prefixes `where()` strips, longest first, each with what it is replaced by.

    A file loses its module directory and `src/main/java/<base package>/`: the base package is
    the project's, so what is left of the package already says which module the file is in.
    The project directory survives only when the repository holds more than one project:
    without it, two files with the same package and name would print as the same location.
    """
    roots = []
    for p in PROJECTS:
        shown_home = p["label"] + "/" if MULTI and p["label"] else ""
        pkg = p["base"].replace(".", "/") + "/" if p["base"] else ""
        for m in p["modules"]:
            home = m["label"] + "/" if m["label"] else ""
            for kind in ("main", "test"):
                roots.append((f"{home}src/{kind}/java/{pkg}",
                              shown_home + ("test/" if kind == "test" else "")))
    return sorted(roots, key=lambda r: -len(r[0]))


SOURCE_ROOTS = source_roots()


def where(path: str, line: int | None = None) -> str:
    """A short, still unambiguous location. The full path is in graph.json.

    The module directory and the base package are dropped: the package segments after it
    already say where the file sits, and repeating the part every file shares only makes the
    tables harder to read.
    """
    short = path
    for prefix, shown in SOURCE_ROOTS:
        if path.startswith(prefix):
            short = shown + path[len(prefix):]
            break
    return f"`{short}:{line}`" if line else f"`{short}`"


def loc(node: dict) -> str:
    return where(node["file"], node["line"])


def render(graph: dict) -> str:
    indexes = {p["label"]: index_of(graph["nodes"], p["label"]) for p in PROJECTS}
    main_nodes = [n for n in graph["nodes"] if not n["test"]]

    def peer(name: str, project: str) -> dict:
        """One project's type by name, or an empty dict -- never another project's namesake."""
        return indexes.get(project, {}).get(name, {})

    out: list[str] = []

    overview = OUT_DIR / "overview.md"

    out.append("# Project graph")
    out.append("")
    header = (f"*Generated by `.claude/graph/generate.py` on {graph['generated']}. "
              "Do not edit by hand — re-run the script after changing code.")
    if overview.exists():
        header += " The overview below lives in `.claude/graph/overview.md` and is hand-written."
    out.append(header + "*")
    out.append("")
    bases = ", ".join(f"`{p['dir']}` → `{p['base'] or '(default package)'}`"
                      for p in graph["projects"])
    out.append("Locations are paths under a module's `src/main/java/<base package>/`, the module "
               "directory left out because the package that remains already names it; those "
               "written `test/…` sit under `src/test/java` instead, and everything else is a "
               f"repository path. Base package{'s' if MULTI else ''} stripped: {bases}. Full "
               "paths, entity columns and every injection edge are in `.claude/graph/graph.json`.")
    if MULTI:
        out.append("")
        out.append("This repository holds several projects. Each is read on its own: a class is "
                   "only ever linked to a collaborator of its own project, so two namesakes in "
                   "two projects stay apart. The modules of one Maven build are one project. "
                   "A location in a project away from the repository root starts with its "
                   "directory.")
    out.append("")

    if overview.exists():
        out.append(overview.read_text(encoding="utf-8").strip())
        out.append("")

    # 1. entry points --------------------------------------------------------
    out.append("## 1. Entry points")
    out.append("")
    out.append("Everything this application does starts at one of these.")
    out.append("")

    out.append("### HTTP")
    out.append("")
    rows = []
    for node in sorted(main_nodes, key=lambda n: n["name"]):
        for e in node["endpoints"]:
            rows.append([f"`{e['http']}`", f"`{e['path']}`",
                         f"{node['name']}.{e['method']}()", e["summary"] or "-",
                         where(node['file'], e['line'])])
    out += md_table(["Method", "Path", "Handler", "What it does", "Where"], rows)

    out.append("### Scheduled jobs")
    out.append("")
    rows = []
    for node in sorted(main_nodes, key=lambda n: n["name"]):
        for j in node["jobs"]:
            rows.append([f"{node['name']}.{j['method']}()", f"`{j['every']}`",
                         ", ".join(j["calls"]) or "-", j["summary"] or "-",
                         where(node['file'], j['line'])])
    out += md_table(["Job", "Interval", "Calls", "What it does", "Where"], rows)

    out.append("### Start-up")
    out.append("")
    rows = []
    for node in main_nodes:
        if node["role"] == "application":
            rows.append([f"{node['name']}.main()", "boots Spring", loc(node)])
        if any("EnvironmentPostProcessor" in i for i in node["implements"]):
            rows.append([node["name"], "runs before any bean exists (spring.factories)", loc(node)])
        if any(i in {"ApplicationRunner", "CommandLineRunner"} for i in node["implements"]):
            rows.append([node["name"], "runs once after start-up", loc(node)])
    out += md_table(["What", "When", "Where"], rows)

    # 2. flows ---------------------------------------------------------------
    out.append("## 2. Flows")
    out.append("")
    out.append("Constructor-injection trees from each entry point. Configuration property "
               "classes, enums, records and exceptions are left out to keep the shape readable.")
    out.append("")
    flows: list[tuple[str, str, list[str]]] = []
    for node in sorted(main_nodes, key=lambda n: n["name"]):
        for j in node["jobs"]:
            targets = [t for t in j["calls"]
                       if peer(t, node["project"]).get("role") not in {None, "properties", "enum"}]
            if targets:
                flows.append((f"{node['name']}.{j['method']}() — {j['summary'] or 'scheduled'}",
                              node["project"], targets))
        if node["role"] == "controller" and node["endpoints"]:
            flows.append((f"{node['name']} — HTTP", node["project"], [node["name"]]))
    for title, project, targets in flows:
        out.append(f"**{title}**" + (f" — `{project}`" if MULTI and project else ""))
        out.append("")
        out.append("```")
        for target in targets:
            out += dependency_tree(graph, target, project)
        out.append("```")
        out.append("")

    # 3. classes by package --------------------------------------------------
    out.append("## 3. Classes by package")
    out.append("")
    packages: dict[tuple[str, str], list[dict]] = {}
    for node in main_nodes:
        packages.setdefault((node["project"], node["package"]), []).append(node)
    for project, pkg in sorted(packages):
        home = f"{project}/" if MULTI and project else ""
        out.append(f"### `{home}{pkg}`")
        out.append("")
        rows = []
        for node in sorted(packages[(project, pkg)], key=lambda n: (ROLE_ORDER.index(n["role"]) if n["role"] in ROLE_ORDER else 99, n["name"])):
            deps = ", ".join(sorted({re.sub(r"<.*", "", t).split(".")[-1] for t, _ in node["injects"]}))
            rows.append([f"**{node['name']}**", node["role"], node["summary"] or "-",
                         deps or "-", loc(node)])
        out += md_table(["Class", "Role", "Summary", "Injects", "Where"], rows)

    # 4. data model ----------------------------------------------------------
    out.append("## 4. Data model")
    out.append("")
    repos = {n.get("entity"): n for n in main_nodes if n["role"] == "repository"}
    rows = []
    for node in sorted([n for n in main_nodes if n["role"] in {"entity", "entity-base", "embeddable"}],
                       key=lambda n: n["name"]):
        repo = repos.get(node["name"])
        rows.append([f"**{node['name']}**", node["role"], f"`{node.get('table') or '-'}`",
                     str(len(node.get("columns", []))), repo["name"] if repo else "-", loc(node)])
    out += md_table(["Entity", "Kind", "Table", "Fields", "Repository", "Where"], rows)

    out.append("### Repositories")
    out.append("")
    rows = []
    for node in sorted([n for n in main_nodes if n["role"] == "repository"], key=lambda n: n["name"]):
        rows.append([f"**{node['name']}**", node.get("entity") or "-",
                     ", ".join(f"`{q}()`" for q in node.get("queries", [])) or "-", loc(node)])
    out += md_table(["Repository", "Entity", "Methods", "Where"], rows)

    out.append("### Tables (Liquibase)")
    out.append("")
    rows = [[f"`{t['table']}`", str(len(t["columns"])), t["changeset"] or "-", where(t['file'])]
            for t in graph["database"]["tables"]]
    out += md_table(["Table", "Columns", "Changeset", "Where"], rows)
    rows = [[f"`{i['index']}`", f"`{i['table']}`", ", ".join(i["columns"]) or "-"]
            for i in graph["database"]["indexes"]]
    out.append("### Indexes")
    out.append("")
    out += md_table(["Index", "Table", "Columns"], rows)

    # 5. configuration -------------------------------------------------------
    out.append("## 5. Configuration")
    out.append("")
    consumers: dict[tuple[str, str], list[str]] = {}
    for e in graph["edges"]:
        project = e.get("project", "")
        if peer(e["to"], project).get("role") == "properties":
            consumers.setdefault((project, e["to"]), []).append(e["from"])
    rows = []
    for node in sorted([n for n in main_nodes if n["role"] == "properties"], key=lambda n: n["name"]):
        read_by = consumers.get((node["project"], node["name"]), [])
        rows.append([f"**{node['name']}**", f"`{node.get('prefix') or '-'}`",
                     str(len(node.get("keys", []))),
                     ", ".join(sorted(set(read_by))) or "-", loc(node)])
    out += md_table(["Properties class", "Prefix", "Keys", "Read by", "Where"], rows)

    out.append("### Keys")
    out.append("")
    for node in sorted([n for n in main_nodes if n["role"] == "properties"], key=lambda n: n["name"]):
        out.append(f"- **{node['name']}** -- " + ", ".join(f"`{k}`" for k in node.get("keys", [])))
    out.append("")

    out.append("### Property files")
    out.append("")
    rows = [[f"`{f['file']}`", str(len(f["keys"]))] for f in graph["properties"]]
    out += md_table(["File", "Keys"], rows)

    out.append("### Beans built by hand")
    out.append("")
    rows = []
    for node in sorted(main_nodes, key=lambda n: n["name"]):
        for b in node["beans"]:
            rows.append([f"`{b['method']}()`", node["name"], where(node['file'], b['line'])])
    out += md_table(["Bean", "Declared in", "Where"], rows)

    # 6. errors --------------------------------------------------------------
    out.append("## 6. Errors")
    out.append("")
    rows = []
    for node in sorted(main_nodes, key=lambda n: n["name"]):
        for h in node["handlers"]:
            rows.append([", ".join(h["handles"]) or "-", f"{node['name']}.{h['method']}()",
                         where(node['file'], h['line'])])
    out += md_table(["Exception", "Handled by", "Where"], rows)
    rows = [[f"**{n['name']}**", n["summary"] or "-", loc(n)]
            for n in sorted(main_nodes, key=lambda n: n["name"]) if n["role"] == "exception"]
    out += md_table(["Exception", "Meaning", "Where"], rows)

    # 7. tests ---------------------------------------------------------------
    out.append("## 7. Tests")
    out.append("")
    rows = []
    for t in sorted(graph["tests"], key=lambda t: t["test"]):
        target = peer(t["covers"] or "", t.get("project", ""))
        rows.append([f"**{t['test']}**", t["covers"] or "-",
                     target.get("summary") or "-", where(t['file'])])
    out += md_table(["Test", "Covers", "What that class does", "Where"], rows)

    # 8. stats ---------------------------------------------------------------
    out.append("## 8. Numbers")
    out.append("")
    roles: dict[str, int] = {}
    for node in main_nodes:
        roles[node["role"]] = roles.get(node["role"], 0) + 1
    out.append(f"- main classes: **{len(main_nodes)}** "
               f"({sum(n['lines'] for n in main_nodes)} lines)")
    out.append(f"- test classes: **{len(graph['tests'])}** "
               f"({sum(n['lines'] for n in graph['nodes'] if n['test'])} lines)")
    out.append("- by role: " + ", ".join(f"{k} {v}" for k, v in sorted(roles.items())))
    out.append(f"- injection edges: **{len(graph['edges'])}**")
    for p in graph["projects"]:
        label = "" if p["dir"] == "." else p["dir"]
        pom = p["pom"]
        stack = (f"Spring Boot {pom.get('springBoot')}, Java {pom.get('java')}, "
                 f"artifact `{pom.get('artifactId')}`") if pom else "no pom.xml"
        modules = [m["dir"] for m in p["modules"] if m["dir"] != p["dir"]]
        listed = f", modules {', '.join(f'`{m}`' for m in modules)}" if modules else ""
        out.append(f"- `{p['dir']}` — {len([n for n in main_nodes if n['project'] == label])} "
                   f"main classes, package `{p['base']}`, {stack}{listed}")
    out.append("")
    out.append("---")
    out.append("")
    out.append("Regenerate with `python .claude/graph/generate.py`. "
               "The machine-readable form of the same data is `.claude/graph/graph.json`.")
    out.append("")
    return "\n".join(out)


def main() -> None:
    graph = collect()
    (OUT_DIR / "graph.json").write_text(json.dumps(graph, indent=2, ensure_ascii=False) + "\n",
                                        encoding="utf-8")
    (OUT_DIR / "PROJECT_GRAPH.md").write_text(render(graph), encoding="utf-8")
    print(f"{len(graph['nodes'])} types, {len(graph['edges'])} edges "
          f"-> {rel(OUT_DIR / 'PROJECT_GRAPH.md')}, {rel(OUT_DIR / 'graph.json')}")


if __name__ == "__main__":
    main()