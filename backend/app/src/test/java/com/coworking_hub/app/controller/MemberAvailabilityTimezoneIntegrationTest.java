package com.coworking_hub.app.controller;

import com.coworking_hub.app.model.Rezervacija;
import com.coworking_hub.app.model.enums.StatusRezervacije;
import com.coworking_hub.app.repository.RezervacijaRepository;
import com.coworking_hub.app.security.AuthenticatedUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class MemberAvailabilityTimezoneIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RezervacijaRepository rezervacijaRepository;

    @Autowired
    private MemberController memberController;

    @Test
    void officeBSeedShouldRemainNineToElevenInRepositoryRead() {
        Long officeBId = officeBId();

        List<Rezervacija> reservations = rezervacijaRepository
                .findByProstorIdAndStatusNotAndDatumDoGreaterThanAndDatumOdLessThan(
                        hubDorcolId(),
                        StatusRezervacije.otkazana,
                        LocalDateTime.of(2026, 2, 25, 0, 0),
                        LocalDateTime.of(2026, 2, 26, 0, 0)
                )
                .stream()
                .filter(item -> item.getKancelarija() != null && officeBId.equals(item.getKancelarija().getId()))
                .toList();

        assertTrue(reservations.size() >= 1);
        assertEquals(LocalDateTime.of(2026, 2, 25, 9, 0), reservations.get(0).getDatumOd());
        assertEquals(LocalDateTime.of(2026, 2, 25, 11, 0), reservations.get(0).getDatumDo());
    }

    @Test
        void officeBAvailabilityShouldReturnNineToElevenWithoutShift() {
        Long spaceId = hubDorcolId();
        Long officeBId = officeBId();

        MemberController.MemberAvailabilityRequest request = new MemberController.MemberAvailabilityRequest(
                "kancelarija",
                List.of(officeBId),
                LocalDate.of(2026, 2, 23)
        );

        var response = memberController.availability(
                new AuthenticatedUser(2L, "marko", "clan", "odobren"),
                spaceId,
                request
        );

        assertNotNull(response.getBody());
        MemberController.MemberAvailabilityResponse body = (MemberController.MemberAvailabilityResponse) response.getBody();
        assertNotNull(body.resources());
        assertTrue(body.resources().size() == 1);

        MemberController.ResourceAvailabilityDto resource = body.resources().get(0);
        assertNotNull(resource.busySlots());
        assertTrue(resource.busySlots().size() >= 1);
        assertEquals(LocalDateTime.of(2026, 2, 25, 9, 0), resource.busySlots().get(0).from());
        assertEquals(LocalDateTime.of(2026, 2, 25, 11, 0), resource.busySlots().get(0).to());
    }

    private Long hubDorcolId() {
        Long value = jdbcTemplate.queryForObject(
                "SELECT id FROM prostori WHERE naziv = 'Hub Dorcol' LIMIT 1",
                Long.class
        );
        assertNotNull(value);
        return value;
    }

    private Long officeBId() {
        Long value = jdbcTemplate.queryForObject(
                "SELECT k.id FROM kancelarije k JOIN prostori p ON p.id = k.prostor_id WHERE p.naziv = 'Hub Dorcol' AND k.naziv = 'Office B' LIMIT 1",
                Long.class
        );
        assertNotNull(value);
        return value;
    }
}
