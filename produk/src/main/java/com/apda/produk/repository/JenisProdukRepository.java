package com.apda.produk.repository;

import com.apda.produk.entity.JenisProduk;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class JenisProdukRepository {
    private List<JenisProduk> data = new ArrayList<>();

    public List<JenisProduk> findAll() {
        return data;
    }

    public void save(JenisProduk jenisProduk) {
        data.add(jenisProduk);
    }
}