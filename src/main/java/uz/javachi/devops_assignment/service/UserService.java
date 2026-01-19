package uz.javachi.devops_assignment.service;

import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uz.javachi.devops_assignment.model.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
public class UserService {

    private final Map<Long, User> userDatabase = new ConcurrentHashMap<>();
    private final AtomicInteger userCount = new AtomicInteger(0);
    private final Timer databaseQueryTimer;

    public UserService(MeterRegistry meterRegistry) {

        // Gauge metric - real-time user count
        Gauge.builder("users.active.count", userCount, AtomicInteger::get)
                .description("Current number of users in system")
                .tag("type", "active")
                .register(meterRegistry);
        
        // Timer metric - database query time
        this.databaseQueryTimer = Timer.builder("users.database.query.time")
                .description("Time taken for database queries")
                .tag("operation", "query")
                .register(meterRegistry);
        
        // Test uchun ba'zi userlar qo'shamiz
        initializeTestUsers();
    }

    private void initializeTestUsers() {
        for (int i = 1; i <= 5; i++) {
            User user = new User();
            user.setUuid("uuid-" + i);
            user.setName("Test User " + i);
            userDatabase.put((long) i, user);
            userCount.incrementAndGet();
        }
        log.info("Initialized {} test users", userCount.get());
    }

    @Timed(value = "users.service.getAll", description = "Time to fetch all users")
    public List<User> getAllUsers() {
        return databaseQueryTimer.record(() -> {
            log.info("Getting all users. Current count: {}", userCount.get());
            return new ArrayList<>(userDatabase.values());
        });
    }

    @Timed(value = "users.service.getById", description = "Time to fetch user by ID")
    public User getUserById(Long id) {
        return databaseQueryTimer.record(() -> {
            log.info("Getting user by id: {}", id);
            return userDatabase.get(id);
        });
    }

    @Timed(value = "users.service.create", description = "Time to create user")
    public User createUser(User user) {
        return databaseQueryTimer.record(() -> {
            Long id = (long) (userDatabase.size() + 1);
            user.setUuid("uuid-" + id);
            userDatabase.put(id, user);
            userCount.incrementAndGet();
            
            log.info("Created new user: {} (Total: {})", user.getName(), userCount.get());
            return user;
        });
    }

    public int getUserCount() {
        return userCount.get();
    }
}
