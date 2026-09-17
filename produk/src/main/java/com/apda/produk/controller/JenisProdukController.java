package com.apda.produk.controller;

import com.apda.produk.entity.JenisProduk;
import com.apda.produk.service.JenisProdukService;
import org.springframework.beans.factory.annotation.Autowired;
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

    @PostMapping
    public String add(@RequestBody JenisProduk jenisProduk) {
        jenisProdukService.add(jenisProduk);
        return "Jenis Produk berhasil ditambahkan!";
    }
}