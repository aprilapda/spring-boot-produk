package com.apda.pelanggan.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import com.apda.pelanggan.entity.Pelanggan;
import com.apda.pelanggan.service.PelangganService;

class PelangganControllerTest {

    private final PelangganService pelangganService = mock(PelangganService.class);
    private final PelangganController controller = new PelangganController(pelangganService);

    @Test
    void createBindsProfileToAuthenticatedCustomer() {
        Authentication authentication = authentication("siswa1", "ROLE_PELANGGAN");
        when(pelangganService.getByAuthSubject("siswa1")).thenReturn(Optional.empty());
        when(pelangganService.save(any(Pelanggan.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Pelanggan request = new Pelanggan(null, "April", "Padang", "P");

        var response = controller.create(request, authentication);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("siswa1", response.getBody().getAuthSubject());
        verify(pelangganService).save(request);
    }

    @Test
    void customerCannotReadAnotherOwnersProfile() {
        Authentication authentication = authentication("siswa1", "ROLE_PELANGGAN");
        when(pelangganService.getByIdAndAuthSubject(42L, "siswa1")).thenReturn(Optional.empty());

        var response = controller.getById(42L, authentication);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(pelangganService, never()).getById(42L);
    }

    private Authentication authentication(String subject, String role) {
        Authentication authentication = mock(Authentication.class);
        GrantedAuthority authority = () -> role;
        when(authentication.getName()).thenReturn(subject);
        when(authentication.getAuthorities()).thenAnswer(invocation -> List.of(authority));
        return authentication;
    }
}
