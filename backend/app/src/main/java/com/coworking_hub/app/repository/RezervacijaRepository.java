package com.coworking_hub.app.repository;

import com.coworking_hub.app.model.Rezervacija;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RezervacijaRepository extends JpaRepository<Rezervacija, Long> {

	List<Rezervacija> findByClanIdOrderByDatumOdDesc(Long clanId);

	Optional<Rezervacija> findByIdAndClanId(Long id, Long clanId);
}
