package uz.javachi.devops_assignment.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import uz.javachi.devops_assignment.model.User;
import uz.javachi.devops_assignment.model.UserRole;
import uz.javachi.devops_assignment.repository.UserRepository;

@Slf4j
@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        // Admin foydalanuvchini yaratish
        String adminEmail = "admin@example.com";
        
        if (userRepository.findByEmail(adminEmail).isEmpty()) {
            User admin = new User();
            admin.setName("System Admin");
            admin.setEmail(adminEmail);
            admin.setPassword(passwordEncoder.encode("admin123")); // Default password
            admin.setRole(UserRole.ADMIN);
            
            userRepository.save(admin);
            log.info("✅ Admin foydalanuvchi yaratildi: email={}, password=admin123", adminEmail);
        } else {
            log.info("ℹ️  Admin foydalanuvchi allaqachon mavjud: {}", adminEmail);
        }
        
        // Test foydalanuvchilar yaratish
        createTestUsers();
    }

    private void createTestUsers() {
        // Test farmer
        if (userRepository.findByEmail("farmer1@example.com").isEmpty()) {
            User farmer = new User();
            farmer.setName("Test Farmer");
            farmer.setEmail("farmer1@example.com");
            farmer.setPassword(passwordEncoder.encode("farmer123"));
            farmer.setRole(UserRole.FARMER);
            userRepository.save(farmer);
            log.info("✅ Test farmer yaratildi: farmer1@example.com");
        }
        
        // Test supplier
        if (userRepository.findByEmail("supplier1@example.com").isEmpty()) {
            User supplier = new User();
            supplier.setName("Test Supplier");
            supplier.setEmail("supplier1@example.com");
            supplier.setPassword(passwordEncoder.encode("supplier123"));
            supplier.setRole(UserRole.SUPPLIER);
            userRepository.save(supplier);
            log.info("✅ Test supplier yaratildi: supplier1@example.com");
        }
        
        // Test distributor
        if (userRepository.findByEmail("distributor1@example.com").isEmpty()) {
            User distributor = new User();
            distributor.setName("Test Distributor");
            distributor.setEmail("distributor1@example.com");
            distributor.setPassword(passwordEncoder.encode("distributor123"));
            distributor.setRole(UserRole.DISTRIBUTOR);
            userRepository.save(distributor);
            log.info("✅ Test distributor yaratildi: distributor1@example.com");
        }
    }
}
