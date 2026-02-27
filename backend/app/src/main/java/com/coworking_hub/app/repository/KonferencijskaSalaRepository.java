package com.coworking_hub.app.repository;

import com.coworking_hub.app.model.KonferencijskaSala;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface KonferencijskaSalaRepository extends JpaRepository<KonferencijskaSala, Long> {

	List<KonferencijskaSala> findByProstorIdIn(Collection<Long> prostorIds);
}
