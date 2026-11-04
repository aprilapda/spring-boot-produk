package com.apda.pelanggan.controller;

import com.apda.pelanggan.entity.Pelanggan;
import com.apda.pelanggan.service.PelangganService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pelanggan")
public class PelangganController {

    private final PelangganService pelangganService;

    public PelangganController(PelangganService pelangganService) {
        this.pelangganService = pelangganService;
    }

    @GetMapping
    public List<Pelanggan> getAll(Authentication authentication) {
        return isAdministrator(authentication)
                ? pelangganService.getAll()
                : pelangganService.getAllForOwner(subjectOf(authentication));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Pelanggan> getById(@PathVariable Long id, Authentication authentication) {
        return findAccessibleById(id, authentication)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Pelanggan> create(@RequestBody Pelanggan pelanggan, Authentication authentication) {
        validatePayload(pelanggan);
        pelanggan.setId(null);
        pelanggan.setAuthSubject(isAdministrator(authentication) ? null : subjectOf(authentication));
        if (!isAdministrator(authentication)
                && pelangganService.getByAuthSubject(subjectOf(authentication)).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Akun ini sudah memiliki profil pelanggan");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(pelangganService.save(pelanggan));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Pelanggan> update(@PathVariable Long id, @RequestBody Pelanggan pelanggan,
            Authentication authentication) {
        validatePayload(pelanggan);
        return findAccessibleById(id, authentication)
                .map(existing -> {
                    existing.setNama(pelanggan.getNama());
                    existing.setAlamat(pelanggan.getAlamat());
                    existing.setJenisKelamin(pelanggan.getJenisKelamin());
                    return ResponseEntity.ok(pelangganService.save(existing));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication authentication) {
        if (findAccessibleById(id, authentication).isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        pelangganService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private java.util.Optional<Pelanggan> findAccessibleById(Long id, Authentication authentication) {
        return isAdministrator(authentication)
                ? pelangganService.getById(id)
                : pelangganService.getByIdAndAuthSubject(id, subjectOf(authentication));
    }

    private boolean isAdministrator(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN_PELANGGAN"::equals);
    }

    private String subjectOf(Authentication authentication) {
        return authentication.getName();
    }

    private void validatePayload(Pelanggan pelanggan) {
        if (pelanggan == null || pelanggan.getNama() == null || pelanggan.getNama().isBlank()
                || pelanggan.getAlamat() == null || pelanggan.getAlamat().isBlank()
                || pelanggan.getJenisKelamin() == null || pelanggan.getJenisKelamin().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Nama, alamat, dan jenis_kelamin wajib diisi");
        }
    }
}
