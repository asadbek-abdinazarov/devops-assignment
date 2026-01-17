package uz.javachi.devops_assignment;

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

    public static void init() {
        log.info("Devops Assignment Application started");
        log.info("This is a simople Spring Boot application for DevOps assignment.");
        log.info("This is a simople Spring Boot application for DevOps assignment.");
        log.info("This is a simople Spring Boot application for DevOps assignment.");
        log.info("This is a simople Spring Boot application for DevOps assignment.");
        log.info("This is a simople Spring Boot application for DevOps assignment.");
        log.info("This is a simople Spring Boot application for DevOps assignment.");
        log.info("This is a simople Spring Boot application for DevOps assignment.");
        log.error("Test error log message.");
        log.error("Test error log message.");
        log.error("Test error log message.");
        log.error("Test error log message.");
        log.error("Test error log message.");
        log.error("Test error log message.");
        log.error("Test error log message.");
        log.error("Test error log message.");
        log.warn("Test warn log message.");
        log.warn("Test warn log message.");
        log.warn("Test warn log message.");
        log.warn("Test warn log message.");
        log.warn("Test warn log message.");
        log.warn("Test warn log message.");
        log.warn("Test warn log message.");
        log.warn("Test warn log message.");
    }
}
