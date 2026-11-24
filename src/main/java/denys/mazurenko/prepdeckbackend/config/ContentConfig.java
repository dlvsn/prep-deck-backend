package denys.mazurenko.prepdeckbackend.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Topics, cards and their pictures: their settings, and the scheduled removal of unused uploads. */
@Configuration
@EnableScheduling
@EnableConfigurationProperties({ImageProperties.class, ImageS3Properties.class, CardProperties.class})
public class ContentConfig {
}
