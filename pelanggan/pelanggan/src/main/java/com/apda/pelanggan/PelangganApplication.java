package com.apda.pelanggan;

import com.apda.pelanggan.entity.Pelanggan;
import com.apda.pelanggan.repository.PelangganRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class PelangganApplication {

	@Bean
	CommandLineRunner seedPelangganData(PelangganRepository pelangganRepository) {
		return args -> {
			if (pelangganRepository.count() == 0) {
				pelangganRepository.save(new Pelanggan(null, "Budi Santoso", "Jl. Merdeka No. 12", "Laki-laki"));
				pelangganRepository.save(new Pelanggan(null, "Siti Aminah", "Jl. Sudirman No. 8", "Perempuan"));
			}
		};
	}

	public static void main(String[] args) {
		SpringApplication.run(PelangganApplication.class, args);
	}

}
