package denys.mazurenko.prepdeckbackend.config;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.MapperConfig;

/**
 * Shared settings for every mapper: each one is a Spring bean, receives its collaborators through
 * the constructor, and has its generated implementation in an {@code impl} package beside it.
 */
@MapperConfig(
        componentModel = "spring",
        injectionStrategy = InjectionStrategy.CONSTRUCTOR,
        implementationPackage = "<PACKAGE_NAME>.impl"
)
public class MapStructConfig {
}
