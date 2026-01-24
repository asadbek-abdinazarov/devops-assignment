package uz.javachi.devops_assignment.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // CSRF ni o'chirish (REST API uchun)
            .csrf(AbstractHttpConfigurer::disable)
            
            // CORS sozlamalari
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // Session management - stateless (JWT uchun tayyor)
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            
            // Authorization rules
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers(
                    "/api/users/test",
                    "/actuator/health",
                    "/actuator/info",
                    "/actuator/prometheus",
                    "/h2-console/**",
                    "/swagger-ui/**",
                    "/v3/api-docs/**",
                    "/swagger-ui.html"
                ).permitAll()
                
                // Dashboard - faqat authenticated users
                .requestMatchers("/api/dashboard/**").authenticated()
                
                // Products - barcha authenticated users
                .requestMatchers("/api/products/**").authenticated()
                
                // Orders - barcha authenticated users
                .requestMatchers("/api/orders/**").authenticated()
                
                // Notifications - barcha authenticated users
                .requestMatchers("/api/notifications/**").authenticated()
                
                // Users - barcha authenticated users
                .requestMatchers("/api/users/**").authenticated()
                
                // Boshqa barcha requestlar authentication talab qiladi
                .anyRequest().authenticated()
            )
            
            // HTTP Basic Authentication (hozircha, keyin JWT qo'shish mumkin)
            .httpBasic(httpBasic -> {})
            
            // H2 Console uchun frame options
            .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin));

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Ruxsat berilgan originlar
        configuration.setAllowedOrigins(Arrays.asList(
            "http://localhost:3000",
            "http://localhost:8080",
            "http://localhost:5173"
        ));
        
        // Ruxsat berilgan metodlar
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
        ));
        
        // Ruxsat berilgan headerlar
        configuration.setAllowedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type",
            "X-Requested-With"
        ));
        
        // Credentials ruxsat berish
        configuration.setAllowCredentials(true);
        
        // Preflight request cache vaqti
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
}
