package com.apda.pelanggan.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

import com.apda.pelanggan.entity.Pelanggan;

@Repository
public interface PelangganRepository extends JpaRepository<Pelanggan, Long> {
    List<Pelanggan> findAllByAuthSubjectOrderByIdAsc(String authSubject);

    Optional<Pelanggan> findByAuthSubject(String authSubject);

    Optional<Pelanggan> findByIdAndAuthSubject(Long id, String authSubject);
}
