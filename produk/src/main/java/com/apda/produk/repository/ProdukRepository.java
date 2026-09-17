package com.apda.produk.repository;

import com.apda.produk.entity.Produk;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class ProdukRepository {
    private List<Produk> data = new ArrayList<>();

    public List<Produk> findAll() {
        return data;
    }

    public void save(Produk produk) {
        data.add(produk);
    }
}