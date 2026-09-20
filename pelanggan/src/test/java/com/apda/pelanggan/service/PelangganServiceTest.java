package com.apda.pelanggan.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.apda.pelanggan.entity.Pelanggan;

@SpringBootTest
class PelangganServiceTest {

    @Autowired
    private PelangganService pelangganService;

    @Test
    void shouldSaveAndFindPelanggan() {
        Pelanggan pelanggan = new Pelanggan(null, "Amel", "Jl. Merdeka 1", "Perempuan");

        Pelanggan saved = pelangganService.save(pelanggan);

        assertThat(saved.getId()).isNotNull();
        assertThat(pelangganService.getById(saved.getId())).isPresent();
        assertThat(pelangganService.getById(saved.getId()).get().getNama()).isEqualTo("Amel");
    }
}
