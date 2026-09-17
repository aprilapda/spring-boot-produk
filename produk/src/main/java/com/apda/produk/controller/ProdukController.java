package com.apda.produk.controller;

import com.apda.produk.entity.Produk;
import com.apda.produk.service.ProdukService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/produk")
public class ProdukController {

    @Autowired
    private ProdukService produkService;

    @GetMapping
    public List<Produk> getAll() {
        return produkService.getAll();
    }

    @PostMapping
    public String add(@RequestBody Produk produk) {
        produkService.add(produk);
        return "Produk berhasil ditambahkan!";
    }
}