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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The letter templates (location and names in {@link EmailTemplateProperties}), filled in by
 * name: every {@code {{key}}} becomes its value, HTML-escaped. The subject is the template's
 * {@code <title>}. No template engine: the placeholders are plain words, and that is all a
 * letter needs.
 */
@Component
@RequiredArgsConstructor
public class EmailTemplates {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{\\s*(\\w+)\\s*}}");
    private static final Pattern TITLE = Pattern.compile("<title>(.*?)</title>", Pattern.DOTALL);

    private final ResourceLoader resourceLoader;
    private final EmailTemplateProperties properties;
    private final Map<String, String> cache = new ConcurrentHashMap<>();

    /** Falls back to the configured language when the template has no file in this one. */
    public Letter render(String name, Language language, Map<String, String> values) {
        String source = load(name, language);
        Matcher placeholder = PLACEHOLDER.matcher(source);
        StringBuilder html = new StringBuilder();
        while (placeholder.find()) {
            String key = placeholder.group(1);
            String value = values.get(key);
            if (value == null) {
                throw new IllegalArgumentException("Template " + name + " needs a value for {{" + key + "}}");
            }
            placeholder.appendReplacement(html, Matcher.quoteReplacement(HtmlUtils.htmlEscape(value)));
        }
        placeholder.appendTail(html);

        Matcher title = TITLE.matcher(html);
        String subject = title.find() ? HtmlUtils.htmlUnescape(title.group(1).trim()) : name;
        return new Letter(subject, html.toString());
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
