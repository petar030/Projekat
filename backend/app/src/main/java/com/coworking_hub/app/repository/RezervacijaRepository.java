package com.coworking_hub.app.repository;

import com.coworking_hub.app.model.Rezervacija;
import com.coworking_hub.app.model.enums.StatusRezervacije;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RezervacijaRepository extends JpaRepository<Rezervacija, Long> {

	List<Rezervacija> findByClanIdOrderByDatumOdDesc(Long clanId);

	List<Rezervacija> findByProstorIdAndStatusNotAndDatumDoGreaterThanAndDatumOdLessThan(
		Long prostorId,
		StatusRezervacije status,
		LocalDateTime datumOd,
		LocalDateTime datumDo
	);

	Optional<Rezervacija> findByIdAndClanId(Long id, Long clanId);
}
