package com.apda.produk.controller;

import com.apda.produk.entity.JenisProduk;
import com.apda.produk.service.JenisProdukService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/jenis-produk")
public class JenisProdukController {

    @Autowired
    private JenisProdukService jenisProdukService;

    @GetMapping
    public List<JenisProduk> getAll() {
        return jenisProdukService.getAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<JenisProduk> getById(@PathVariable Long id) {
        return jenisProdukService.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public JenisProduk add(@RequestBody JenisProduk jenisProduk) {
        return jenisProdukService.add(jenisProduk);
    }

    @PutMapping("/{id}")
    public ResponseEntity<JenisProduk> update(@PathVariable Long id, @RequestBody JenisProduk jenisProduk) {
        return jenisProdukService.update(id, jenisProduk)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!jenisProdukService.deleteById(id)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }
}
