package com.apda.produk.service;

import com.apda.produk.entity.JenisProduk;
import com.apda.produk.repository.JenisProdukRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class JenisProdukService {

    @Autowired
    private JenisProdukRepository jenisProdukRepository;

    public List<JenisProduk> getAll() {
        return jenisProdukRepository.findAll();
    }

    public Optional<JenisProduk> findById(Long id) {
        return jenisProdukRepository.findById(id);
    }

    public JenisProduk add(JenisProduk jenisProduk) {
        return jenisProdukRepository.save(jenisProduk);
    }

    public Optional<JenisProduk> update(Long id, JenisProduk jenisProduk) {
        return jenisProdukRepository.findById(id).map(existing -> {
            existing.setNamaJenis(jenisProduk.getNamaJenis());
            return jenisProdukRepository.save(existing);
        });
    }

    public boolean deleteById(Long id) {
        if (!jenisProdukRepository.existsById(id)) {
            return false;
        }
        jenisProdukRepository.deleteById(id);
        return true;
    }
}
