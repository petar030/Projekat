package com.coworking_hub.app.repository;

import com.coworking_hub.app.model.Komentar;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KomentarRepository extends JpaRepository<Komentar, Long> {

	List<Komentar> findTop10ByProstorIdOrderByKreiranoDesc(Long prostorId);

	List<Komentar> findByProstorIdOrderByKreiranoDesc(Long prostorId, Pageable pageable);
}
