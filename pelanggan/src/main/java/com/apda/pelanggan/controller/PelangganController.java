package com.apda.pelanggan.controller;

import com.apda.pelanggan.entity.Pelanggan;
import com.apda.pelanggan.service.PelangganService;
import java.util.List;
import org.springframework.http.ResponseEntity;
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
    public List<Pelanggan> getAll() {
        return pelangganService.getAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Pelanggan> getById(@PathVariable Long id) {
        return pelangganService.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Pelanggan create(@RequestBody Pelanggan pelanggan) {
        return pelangganService.save(pelanggan);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Pelanggan> update(@PathVariable Long id, @RequestBody Pelanggan pelanggan) {
        return pelangganService.getById(id)
                .map(existing -> {
                    existing.setNama(pelanggan.getNama());
                    existing.setAlamat(pelanggan.getAlamat());
                    existing.setJenisKelamin(pelanggan.getJenisKelamin());
                    return ResponseEntity.ok(pelangganService.save(existing));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (pelangganService.getById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        pelangganService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
