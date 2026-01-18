package uz.javachi.devops_assignment;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DevopsAssignmentApplication {

    private static final Logger log = LoggerFactory.getLogger(DevopsAssignmentApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(DevopsAssignmentApplication.class, args);
        init();
    }

    @PostConstruct
    public static void init() {
        log.info("Devops Assignment Application started");
    }
}
