package com.coworking_hub.app.repository;

import com.coworking_hub.app.model.Prostor;
import com.coworking_hub.app.model.enums.StatusProstora;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProstorRepository extends JpaRepository<Prostor, Long> {

	long countByStatus(StatusProstora status);

	@Query("""
		select distinct p.grad
		from Prostor p
		where p.status = :status
		order by p.grad asc
	""")
	List<String> findDistinctGradoviByStatus(@Param("status") StatusProstora status);

	@Query("""
		select distinct p
		from Prostor p
		join fetch p.firma f
		where p.status = :status
		  and (:name is null or :name = '' or lower(p.naziv) like lower(concat('%', :name, '%')))
		  and (:applyCities = false or p.grad in :cities)
	""")
	List<Prostor> searchApproved(
			@Param("status") StatusProstora status,
			@Param("name") String name,
			@Param("cities") List<String> cities,
			@Param("applyCities") boolean applyCities
	);

	@Query("""
		select p
		from Prostor p
		join fetch p.firma
		join fetch p.menadzer
		where p.id = :id
		  and p.status = :status
	""")
	Optional<Prostor> findByIdAndStatus(@Param("id") Long id, @Param("status") StatusProstora status);
}
