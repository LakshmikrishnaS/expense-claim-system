package com.example.expense.config;

import com.example.expense.entity.Role;
import com.example.expense.entity.User;
import com.example.expense.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataSeeder {
    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    @Bean
    CommandLineRunner seedManager(UserRepository repo, PasswordEncoder encoder,
                                  @Value("${app.seed.manager-name}") String name,
                                  @Value("${app.seed.manager-email}") String email,
                                  @Value("${app.seed.manager-password}") String password) {
        return args -> {
            if (!repo.existsByEmailIgnoreCase(email)) {
                repo.save(User.builder().name(name).email(email).password(encoder.encode(password))
                        .role(Role.MANAGER).active(true).build());
                log.info("Seeded manager account: {}", email);
            }
        };
    }
}
