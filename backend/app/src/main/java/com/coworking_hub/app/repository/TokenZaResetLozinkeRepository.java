package com.coworking_hub.app.repository;

import com.coworking_hub.app.model.TokenZaResetLozinke;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TokenZaResetLozinkeRepository extends JpaRepository<TokenZaResetLozinke, Long> {

	Optional<TokenZaResetLozinke> findByToken(String token);

	List<TokenZaResetLozinke> findByKorisnikIdAndIskoriscenFalse(Long korisnikId);
}
