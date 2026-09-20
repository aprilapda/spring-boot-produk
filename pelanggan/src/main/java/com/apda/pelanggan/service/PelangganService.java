package com.apda.pelanggan.service;

import com.apda.pelanggan.entity.Pelanggan;
import com.apda.pelanggan.repository.PelangganRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class PelangganService {

    private final PelangganRepository pelangganRepository;

    public PelangganService(PelangganRepository pelangganRepository) {
        this.pelangganRepository = pelangganRepository;
    }

    public Pelanggan save(Pelanggan pelanggan) {
        return pelangganRepository.save(pelanggan);
    }

    public List<Pelanggan> getAll() {
        return pelangganRepository.findAll();
    }

    public Optional<Pelanggan> getById(Long id) {
        return pelangganRepository.findById(id);
    }

    public void deleteById(Long id) {
        pelangganRepository.deleteById(id);
    }
}
