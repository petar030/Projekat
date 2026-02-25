package com.coworking_hub.app.repository;

import com.coworking_hub.app.model.Kancelarija;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KancelarijaRepository extends JpaRepository<Kancelarija, Long> {
}
