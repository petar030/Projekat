package com.coworking_hub.app.repository;

import com.coworking_hub.app.model.SlikaProstora;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SlikaProstoraRepository extends JpaRepository<SlikaProstora, Long> {

	List<SlikaProstora> findByProstorIdOrderByRedosledAscIdAsc(Long prostorId);
}
