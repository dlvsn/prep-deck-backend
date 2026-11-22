package denys.mazurenko.prepdeckbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class PrepDeckBackendApplication {

    public static void main(String[] args) {
        // The server runs in UTC whatever the machine says. The PostgreSQL driver sends the JVM's
        // zone on connect, and Windows still names Kyiv "Europe/Kiev", which PostgreSQL rejects.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        SpringApplication.run(PrepDeckBackendApplication.class, args);
    }

}
