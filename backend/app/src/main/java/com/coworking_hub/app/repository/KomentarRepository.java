package com.coworking_hub.app.repository;

import com.coworking_hub.app.model.Komentar;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KomentarRepository extends JpaRepository<Komentar, Long> {
}
