package denys.mazurenko.prepdeckbackend.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Sharing topics: its settings. */
@Configuration
@EnableConfigurationProperties(SharingProperties.class)
public class SharingConfig {
}
