package denys.mazurenko.prepdeckbackend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * `@Async` work (sending letters) runs on Spring Boot's task executor, so a slow SMTP server
 * never holds up an HTTP response.
 */
@Configuration
@EnableAsync
public class AsyncConfig {
}
