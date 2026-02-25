package com.coworking_hub.app.repository;

import com.coworking_hub.app.model.Firma;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FirmaRepository extends JpaRepository<Firma, Long> {

	Optional<Firma> findByPib(String pib);

	Optional<Firma> findByMaticniBroj(String maticniBroj);
}
