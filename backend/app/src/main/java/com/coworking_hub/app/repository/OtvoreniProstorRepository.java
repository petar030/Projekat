package com.coworking_hub.app.repository;

import com.coworking_hub.app.model.OtvoreniProstor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface OtvoreniProstorRepository extends JpaRepository<OtvoreniProstor, Long> {

	List<OtvoreniProstor> findByProstorIdIn(Collection<Long> prostorIds);
}
