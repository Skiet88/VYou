package za.ac.skiet.vyou2;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class VYou2Application {
    public static void main(String[] args) {
        SpringApplication.run(VYou2Application.class, args);
    }
}
