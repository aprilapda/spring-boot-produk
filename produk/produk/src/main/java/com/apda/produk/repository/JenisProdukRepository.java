package com.apda.produk.repository;

import com.apda.produk.entity.JenisProduk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JenisProdukRepository extends JpaRepository<JenisProduk, Long> {
}
