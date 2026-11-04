package com.apda.produk.controller;

import com.apda.produk.entity.Produk;
import com.apda.produk.service.ProdukService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/produk")
public class ProdukController {

    private final ProdukService produkService;

    public ProdukController(ProdukService produkService) {
        this.produkService = produkService;
    }

    @GetMapping
    public List<Produk> getAll() {
        return produkService.getAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Produk> getById(@PathVariable Long id) {
        return produkService.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public Produk add(@RequestBody Produk produk) {
        return produkService.add(produk);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Produk> update(@PathVariable Long id, @RequestBody Produk produk) {
        return produkService.update(id, produk)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!produkService.deleteById(id)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }
}
