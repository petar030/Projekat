package com.coworking_hub.app.controller;

import com.coworking_hub.app.model.Prostor;
import com.coworking_hub.app.model.Rezervacija;
import com.coworking_hub.app.model.enums.StatusRezervacije;
import com.coworking_hub.app.repository.RezervacijaRepository;
import com.coworking_hub.app.security.AuthenticatedUser;
import com.coworking_hub.app.security.CurrentUserArgumentResolver;
import com.coworking_hub.app.security.JwtAuthInterceptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MemberControllerTest {

    private MockMvc mockMvc;

    @Mock
    private RezervacijaRepository rezervacijaRepository;

    @BeforeEach
    void setUp() {
        MemberController memberController = new MemberController(rezervacijaRepository);
        mockMvc = MockMvcBuilders.standaloneSetup(memberController)
                .setCustomArgumentResolvers(new CurrentUserArgumentResolver())
                .build();
    }

    @Test
    void reservationsShouldReturnCurrentMemberContentWithoutPagingFields() throws Exception {
        Rezervacija futureActive = buildReservation(
                501L,
                10L,
                "Hub Dorcol",
                "Beograd",
                LocalDateTime.now().plusDays(2),
                LocalDateTime.now().plusDays(2).plusHours(2),
                StatusRezervacije.aktivna
        );

        Rezervacija oldConfirmed = buildReservation(
                502L,
                20L,
                "Hub Novi Beograd",
                "Beograd",
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().minusDays(1).plusHours(2),
                StatusRezervacije.potvrdjena
        );

        when(rezervacijaRepository.findByClanIdOrderByDatumOdDesc(12L))
                .thenReturn(List.of(futureActive, oldConfirmed));

        mockMvc.perform(get("/api/member/reservations")
                        .requestAttr(JwtAuthInterceptor.AUTH_USER_ATTR, authenticatedUser(12L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].id").value(501))
                .andExpect(jsonPath("$.content[0].spaceName").value("Hub Dorcol"))
                .andExpect(jsonPath("$.content[0].cancellable").value(true))
                .andExpect(jsonPath("$.content[1].id").value(502))
                .andExpect(jsonPath("$.content[1].cancellable").value(false))
                .andExpect(jsonPath("$.page").doesNotExist())
                .andExpect(jsonPath("$.size").doesNotExist())
                .andExpect(jsonPath("$.totalElements").doesNotExist())
                .andExpect(jsonPath("$.totalPages").doesNotExist());
    }

    @Test
    void cancelReservationShouldReturnOkWhenReservationIsCancellable() throws Exception {
        Rezervacija reservation = buildReservation(
                700L,
                10L,
                "Hub Dorcol",
                "Beograd",
                LocalDateTime.now().plusDays(2),
                LocalDateTime.now().plusDays(2).plusHours(2),
                StatusRezervacije.aktivna
        );

        when(rezervacijaRepository.findByIdAndClanId(700L, 12L)).thenReturn(Optional.of(reservation));

        mockMvc.perform(patch("/api/member/reservations/700/cancel")
                        .requestAttr(JwtAuthInterceptor.AUTH_USER_ATTR, authenticatedUser(12L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(700))
                .andExpect(jsonPath("$.status").value("otkazana"));

        verify(rezervacijaRepository).save(reservation);
    }

    @Test
    void cancelReservationShouldReturnUnprocessableWhenTooLate() throws Exception {
        Rezervacija reservation = buildReservation(
                701L,
                10L,
                "Hub Dorcol",
                "Beograd",
                LocalDateTime.now().plusHours(6),
                LocalDateTime.now().plusHours(8),
                StatusRezervacije.aktivna
        );

        when(rezervacijaRepository.findByIdAndClanId(701L, 12L)).thenReturn(Optional.of(reservation));

        mockMvc.perform(patch("/api/member/reservations/701/cancel")
                        .requestAttr(JwtAuthInterceptor.AUTH_USER_ATTR, authenticatedUser(12L)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("Rezervaciju nije moguce otkazati manje od 12h pre pocetka ili ako nije aktivna"));

        verify(rezervacijaRepository, never()).save(reservation);
    }

    private Rezervacija buildReservation(
            Long reservationId,
            Long spaceId,
            String spaceName,
            String city,
            LocalDateTime from,
            LocalDateTime to,
            StatusRezervacije status
    ) {
        Prostor prostor = new Prostor();
        prostor.setId(spaceId);
        prostor.setNaziv(spaceName);
        prostor.setGrad(city);

        Rezervacija rezervacija = new Rezervacija();
        rezervacija.setId(reservationId);
        rezervacija.setProstor(prostor);
        rezervacija.setDatumOd(from);
        rezervacija.setDatumDo(to);
        rezervacija.setStatus(status);
        return rezervacija;
    }

    private AuthenticatedUser authenticatedUser(Long userId) {
        return new AuthenticatedUser(userId, "marko", "clan", "odobren");
    }
}