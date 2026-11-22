package denys.mazurenko.prepdeckbackend.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Our mail settings: the sender, the support address, where the letter templates are. */
@Configuration
@EnableConfigurationProperties({AppMailProperties.class, EmailTemplateProperties.class})
public class MailConfig {
}
