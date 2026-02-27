package com.coworking_hub.app.repository;

import com.coworking_hub.app.model.Kancelarija;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface KancelarijaRepository extends JpaRepository<Kancelarija, Long> {

	List<Kancelarija> findByProstorIdIn(Collection<Long> prostorIds);
}
