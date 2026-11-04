package com.apda.produk.service;

import com.apda.produk.entity.Produk;
import com.apda.produk.repository.ProdukRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProdukService {

    private final ProdukRepository produkRepository;

    public ProdukService(ProdukRepository produkRepository) {
        this.produkRepository = produkRepository;
    }

    public List<Produk> getAll() {
        return produkRepository.findAll();
    }

    public Optional<Produk> findById(Long id) {
        return produkRepository.findById(id);
    }

    public Produk add(Produk produk) {
        return produkRepository.save(produk);
    }

    public Optional<Produk> update(Long id, Produk produk) {
        return produkRepository.findById(id).map(existing -> {
            existing.setNama(produk.getNama());
            existing.setHarga(produk.getHarga());
            existing.setDeskripsi(produk.getDeskripsi());
            return produkRepository.save(existing);
        });
    }

    public boolean deleteById(Long id) {
        if (!produkRepository.existsById(id)) {
            return false;
        }
        produkRepository.deleteById(id);
        return true;
    }
}
