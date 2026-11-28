package denys.mazurenko.prepdeckbackend.mail;

import denys.mazurenko.prepdeckbackend.config.EmailTemplateProperties;
import denys.mazurenko.prepdeckbackend.domain.Language;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The letter templates (location and names in {@link EmailTemplateProperties}), filled in by
 * name: every {@code {{key}}} becomes its value, HTML-escaped. A section
 * {@code {{#key}}…{{/key}}} is kept when the value is {@code true} and dropped when it is
 * {@code false}; for a list of maps it repeats once per map, whose keys then join the outer ones.
 * The subject is the template's {@code <title>}. No template engine: that is all a letter needs.
 */
@Component
@RequiredArgsConstructor
public class EmailTemplates {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{\\s*(\\w+)\\s*}}");
    private static final Pattern SECTION = Pattern.compile("\\{\\{#(\\w+)}}(.*?)\\{\\{/\\1}}", Pattern.DOTALL);
    private static final Pattern TITLE = Pattern.compile("<title>(.*?)</title>", Pattern.DOTALL);

    private final ResourceLoader resourceLoader;
    private final EmailTemplateProperties properties;
    private final Map<String, String> cache = new ConcurrentHashMap<>();

    /** Falls back to the configured language when the template has no file in this one. */
    public Letter render(String name, Language language, Map<String, ?> values) {
        String html = fill(name, load(name, language), values);
        Matcher title = TITLE.matcher(html);
        String subject = title.find() ? HtmlUtils.htmlUnescape(title.group(1).trim()) : name;
        return new Letter(subject, html);
    }

    /** Sections first, each filled with its own values, then the placeholders left. */
    private String fill(String name, String source, Map<String, ?> values) {
        Matcher section = SECTION.matcher(source);
        StringBuilder expanded = new StringBuilder();
        while (section.find()) {
            String key = section.group(1);
            String body = section.group(2);
            section.appendReplacement(expanded, Matcher.quoteReplacement(expand(name, key, body, values)));
        }
        section.appendTail(expanded);

        Matcher placeholder = PLACEHOLDER.matcher(expanded);
        StringBuilder html = new StringBuilder();
        while (placeholder.find()) {
            String key = placeholder.group(1);
            Object value = values.get(key);
            if (!(value instanceof String text)) {
                throw new IllegalArgumentException("Template " + name + " needs a text value for {{" + key + "}}");
            }
            placeholder.appendReplacement(html, Matcher.quoteReplacement(HtmlUtils.htmlEscape(text)));
        }
        placeholder.appendTail(html);
        return html.toString();
    }

    private String expand(String name, String key, String body, Map<String, ?> values) {
        Object value = values.get(key);
        if (value instanceof Boolean shown) {
            return shown ? fill(name, body, values) : "";
        }
        if (value instanceof List<?> items) {
            StringBuilder repeated = new StringBuilder();
            for (Object item : items) {
                if (!(item instanceof Map<?, ?> itemValues)) {
                    throw new IllegalArgumentException("Template " + name + " needs maps in {{#" + key + "}}");
                }
                Map<String, Object> scope = new HashMap<>(values);
                itemValues.forEach((itemKey, itemValue) -> scope.put(String.valueOf(itemKey), itemValue));
                repeated.append(fill(name, body, scope));
            }
            return repeated.toString();
        }
        throw new IllegalArgumentException("Template " + name + " needs true, false or a list for {{#" + key + "}}");
    }

    private String load(String name, Language language) {
        Resource resource = resourceLoader.getResource(locationOf(name, language));
        if (!resource.exists()) {
            resource = resourceLoader.getResource(locationOf(name, properties.getFallbackLanguage()));
        }
        Resource found = resource;
        return cache.computeIfAbsent(found.getDescription(), description -> {
            try {
                return found.getContentAsString(StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new UncheckedIOException("Cannot read email template " + description, e);
            }
        });
    }

    private String locationOf(String name, Language language) {
        return properties.getLocation() + name + "-" + language.code() + properties.getSuffix();
    }
}
