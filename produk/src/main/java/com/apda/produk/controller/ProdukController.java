package com.apda.produk.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import com.apda.produk.entity.Produk;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/produk")
public class ProdukController {
    @GetMapping
    public String ping() {
        return "ping";
    }


    @GetMapping ("/{id}")
    public ResponseEntity<Produk> getProdukById(@PathVariable("id") int id) {
        if (id == 1) {
            return ResponseEntity.ok(
                new Produk(1, "Mouse", 100000, "merk 1"));

        }
        return null;
    }

    @PostMapping
    public ResponseEntity<Produk> createProduk(@RequestBody Produk produk) {
        return ResponseEntity.ok(produk);
    }

}