package com.apda.auth;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.apda.auth.entity.User;
import com.apda.auth.repository.UserRepository;

@SpringBootApplication
public class AuthApplication {

	public static void main(String[] args) {
		SpringApplication.run(AuthApplication.class, args);
	}

	@Bean
	CommandLineRunner seedAdminUser(UserRepository userRepository, PasswordEncoder passwordEncoder) {
		return args -> {
			// Akun awal ADMIN_PELANGGAN biar langsung bisa login
			if (userRepository.findByUsername("admin").isEmpty()) {
				userRepository.save(new User("admin", passwordEncoder.encode("admin123"), "ADMIN_PELANGGAN"));
				System.out.println(">>> Akun awal dibuat -> username: admin | password: admin123");
			}
		};
	}
}
