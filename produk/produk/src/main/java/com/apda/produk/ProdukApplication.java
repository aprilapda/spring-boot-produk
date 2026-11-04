package com.apda.produk;

import com.apda.produk.entity.Produk;
import com.apda.produk.repository.ProdukRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class ProdukApplication {

	@Bean
	CommandLineRunner seedProdukData(ProdukRepository produkRepository) {
		return args -> {
			if (produkRepository.findAll().isEmpty()) {
				produkRepository.save(new Produk("Laptop", 12000000, "Laptop gaming"));
				produkRepository.save(new Produk("Smartphone", 5000000, "Android flagship"));
			}
		};
	}

	public static void main(String[] args) {
		SpringApplication.run(ProdukApplication.class, args);
	}

}
