package com.coworking_hub.app.repository;

import com.coworking_hub.app.model.Reakcija;
import com.coworking_hub.app.model.enums.TipReakcije;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ReakcijaRepository extends JpaRepository<Reakcija, Long> {

	long countByProstorIdAndTip(Long prostorId, TipReakcije tip);

	@Query("""
		select r.prostor.id as prostorId,
			   sum(case when r.tip = com.coworking_hub.app.model.enums.TipReakcije.svidjanje then 1 else 0 end) as likes,
			   sum(case when r.tip = com.coworking_hub.app.model.enums.TipReakcije.nesvidjanje then 1 else 0 end) as dislikes
		from Reakcija r
		where r.prostor.id in :prostorIds
		group by r.prostor.id
	""")
	List<SpaceReactionCountProjection> countGroupedByProstorIds(@Param("prostorIds") Collection<Long> prostorIds);

	interface SpaceReactionCountProjection {
		Long getProstorId();

		Long getLikes();

		Long getDislikes();
	}
}
