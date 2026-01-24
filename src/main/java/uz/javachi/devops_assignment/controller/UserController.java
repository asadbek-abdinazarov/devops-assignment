package uz.javachi.devops_assignment.controller;

import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uz.javachi.devops_assignment.model.User;
import uz.javachi.devops_assignment.service.UserService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final Counter userRequestCounter;
    private final Counter userCreateCounter;
    private final Counter userErrorCounter;


    public UserController(UserService userService, MeterRegistry meterRegistry) {
        this.userService = userService;
        
        // Custom metricalar yaratish
        this.userRequestCounter = Counter.builder("users.requests.total")
                .description("Total number of user API requests")
                .tag("api", "users")
                .register(meterRegistry);
                
        this.userCreateCounter = Counter.builder("users.created.total")
                .description("Total number of users created")
                .tag("operation", "create")
                .register(meterRegistry);
                
        this.userErrorCounter = Counter.builder("users.errors.total")
                .description("Total number of user API errors")
                .tag("api", "users")
                .register(meterRegistry);
    }

    @GetMapping
    @Timed(value = "users.get.all", description = "Time taken to get all users")
    public ResponseEntity<?> getAllUsers() {
        log.info("Get all users endpoint called");
        userRequestCounter.increment();
        
        try {
            List<User> users = userService.getAllUsers();
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            log.error("Error getting users", e);
            userErrorCounter.increment();
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    @Timed(value = "users.get.byid", description = "Time taken to get user by ID")
    public ResponseEntity<?> getUserById(@PathVariable String id) {
        log.info("Get user by id endpoint called: {}", id);
        userRequestCounter.increment();
        
        try {
            User user = userService.getUserById(id);
            if (user != null) {
                return ResponseEntity.ok(user);
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error getting user by id", e);
            userErrorCounter.increment();
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    @PostMapping
    @Timed(value = "users.create", description = "Time taken to create user")
    public ResponseEntity<?> createUser(@RequestBody User user) {
        log.info("Create user endpoint called");
        userRequestCounter.increment();
        
        try {
            User created = userService.createUser(user);
            userCreateCounter.increment();
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            log.error("Error creating user", e);
            userErrorCounter.increment();
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/test")
    public ResponseEntity<?> test() {
        log.info("Test endpoint called");
        userRequestCounter.increment();
        return ResponseEntity.ok("Test endpoint is working! Total requests: " + userRequestCounter.count());
    }

}
