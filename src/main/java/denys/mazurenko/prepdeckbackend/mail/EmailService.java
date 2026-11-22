package denys.mazurenko.prepdeckbackend.mail;

import denys.mazurenko.prepdeckbackend.config.AppMailProperties;
import denys.mazurenko.prepdeckbackend.domain.Language;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/** Renders a template and hands the letter to the SMTP server configured in `spring.mail.*`. */
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final EmailTemplates templates;
    private final AppMailProperties properties;

    public void send(String to, String template, Language language, Map<String, String> values) throws MessagingException {
        Letter letter = templates.render(template, language, values);
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
        helper.setFrom(properties.getFrom());
        helper.setTo(to);
        helper.setSubject(letter.subject());
        helper.setText(letter.html(), true);
        mailSender.send(message);
    }
}
