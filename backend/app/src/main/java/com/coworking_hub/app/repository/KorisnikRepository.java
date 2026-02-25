package com.coworking_hub.app.repository;

import com.coworking_hub.app.model.Korisnik;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface KorisnikRepository extends JpaRepository<Korisnik, Long> {

	Optional<Korisnik> findByKorisnickoIme(String korisnickoIme);

	Optional<Korisnik> findByEmail(String email);

	boolean existsByKorisnickoIme(String korisnickoIme);

	boolean existsByEmail(String email);
}
