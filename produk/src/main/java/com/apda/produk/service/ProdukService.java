package com.apda.produk.service;

import com.apda.produk.entity.Produk;
import com.apda.produk.repository.ProdukRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProdukService {

    @Autowired
    private ProdukRepository produkRepository;

    public List<Produk> getAll() {
        return produkRepository.findAll();
    }

    public void add(Produk produk) {
        produkRepository.save(produk);
    }
}