package com.apda.auth.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.apda.auth.entity.User;
import com.apda.auth.repository.UserRepository;
import com.apda.auth.service.JwtService;

class AuthControllerTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final JwtService jwtService = mock(JwtService.class);
    private final AuthController controller = new AuthController(userRepository, passwordEncoder, jwtService);

    @BeforeEach
    void resetMocks() {
        org.mockito.Mockito.reset(userRepository, passwordEncoder, jwtService);
    }

    @Test
    void registerHashesPasswordPersistsEmailAndIssuesCustomerToken() {
        when(passwordEncoder.encode("secret")).thenReturn("bcrypt-hash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtService.generateToken("siswa1", "PELANGGAN", "siswa1@example.test")).thenReturn("jwt-token");

        User request = new User("siswa1", "secret", null);
        request.setEmail("siswa1@example.test");
        ResponseEntity<?> response = controller.register(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        org.mockito.ArgumentCaptor<User> captor = org.mockito.ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertEquals("bcrypt-hash", saved.getPassword());
        assertEquals("PELANGGAN", saved.getRole());
        assertEquals("siswa1@example.test", saved.getEmail());
        assertFalse(saved.getPassword().equals("secret"));
        verify(jwtService).generateToken("siswa1", "PELANGGAN", "siswa1@example.test");
    }

    @Test
    void publicRegistrationCannotAssignAdminRole() {
        ResponseEntity<?> response = controller.register(new User("intruder", "secret", "ADMIN_PELANGGAN"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void registrationRequiresEmail() {
        ResponseEntity<?> response = controller.register(new User("siswa1", "secret", null));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void loginRejectsMissingOrBlankCredentialsAsBadRequest() {
        ResponseEntity<?> missingPassword = controller.login(new User("siswa1", null, null));
        ResponseEntity<?> blankUsername = controller.login(new User("  ", "secret", null));

        assertEquals(HttpStatus.BAD_REQUEST, missingPassword.getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, blankUsername.getStatusCode());
        verify(userRepository, never()).findByUsername(any());
    }

    @Test
    void loginChecksHashAndReturnsTokenForValidCredentials() {
        User user = new User("siswa1", "bcrypt-hash", "USER");
        user.setEmail("siswa1@example.test");
        when(userRepository.findByUsername("siswa1")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret", "bcrypt-hash")).thenReturn(true);
        when(jwtService.generateToken("siswa1", "USER", "siswa1@example.test")).thenReturn("jwt-token");

        ResponseEntity<?> response = controller.login(new User("siswa1", "secret", null));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(passwordEncoder).matches("secret", "bcrypt-hash");
        verify(jwtService).generateToken("siswa1", "USER", "siswa1@example.test");
    }
}
