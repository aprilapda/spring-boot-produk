package com.apda.auth.controller;

import java.util.Map;
import java.util.Optional;
import java.util.LinkedHashMap;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.apda.auth.entity.User;
import com.apda.auth.repository.UserRepository;
import com.apda.auth.service.JwtService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    // POST /api/auth/register -> buat akun baru
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User request) {
        if (request == null || request.getUsername() == null || request.getUsername().isBlank()
                || request.getPassword() == null || request.getPassword().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Username dan password wajib diisi"));
        }

        // Registrasi publik tidak dapat menetapkan role; role hanya ditentukan server.
        if (request.getRole() != null && !request.getRole().isBlank()
                && !"PELANGGAN".equalsIgnoreCase(request.getRole().trim())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Role registrasi ditentukan oleh server"));
        }

        if (request.getEmail() == null || request.getEmail().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email wajib diisi"));
        }

        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Username sudah dipakai"));
        }

        // Password disimpan ter-hash (BCrypt), bukan teks asli
        User newUser = new User(request.getUsername(), passwordEncoder.encode(request.getPassword()), "PELANGGAN");
        newUser.setEmail(request.getEmail().trim());
        User user = userRepository.save(newUser);

        return ResponseEntity.status(HttpStatus.CREATED).body(authResponse(user));
    }

    // POST /api/auth/login -> verifikasi & terbitkan token
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody User request) {
        if (request == null || request.getUsername() == null || request.getUsername().isBlank()
                || request.getPassword() == null || request.getPassword().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Username dan password wajib diisi"));
        }

        Optional<User> found = userRepository.findByUsername(request.getUsername());

        // Cek ada user-nya? Password cocok?
        if (found.isEmpty() || !passwordEncoder.matches(request.getPassword(), found.get().getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Username atau password salah"));
        }

        User user = found.get();
        return ResponseEntity.ok(authResponse(user));
    }

    private Map<String, String> authResponse(User user) {
        Map<String, String> response = new LinkedHashMap<>();
        response.put("token", jwtService.generateToken(user.getUsername(), user.getRole(), user.getEmail()));
        response.put("username", user.getUsername());
        response.put("role", user.getRole());
        if (user.getEmail() != null) {
            response.put("email", user.getEmail());
        }
        return response;
    }
}
