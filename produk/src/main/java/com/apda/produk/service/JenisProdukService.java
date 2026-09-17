package com.apda.produk.service;

import com.apda.produk.entity.JenisProduk;
import com.apda.produk.repository.JenisProdukRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class JenisProdukService {

    @Autowired
    private JenisProdukRepository jenisProdukRepository;

    public List<JenisProduk> getAll() {
        return jenisProdukRepository.findAll();
    }

    public void add(JenisProduk jenisProduk) {
        jenisProdukRepository.save(jenisProduk);
    }
}