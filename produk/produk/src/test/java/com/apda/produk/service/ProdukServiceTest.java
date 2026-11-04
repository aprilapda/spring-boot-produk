package com.apda.produk.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.apda.produk.entity.Produk;

@SpringBootTest
class ProdukServiceTest {

    @Autowired
    private ProdukService produkService;

    @Test
    void shouldSaveAndFindProdukById() {
        Produk produk = new Produk("Laptop", 12000000, "Laptop gaming");

        Produk saved = produkService.add(produk);
        assertNotNull(saved.getId());

        Optional<Produk> result = produkService.findById(saved.getId());

        assertTrue(result.isPresent());
        assertEquals("Laptop", result.get().getNama());
    }
}
