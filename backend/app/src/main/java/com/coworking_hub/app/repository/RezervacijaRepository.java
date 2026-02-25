package com.coworking_hub.app.repository;

import com.coworking_hub.app.model.Rezervacija;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RezervacijaRepository extends JpaRepository<Rezervacija, Long> {
}
