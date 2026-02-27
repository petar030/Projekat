package com.coworking_hub.app.controller;

import com.coworking_hub.app.model.Firma;
import com.coworking_hub.app.model.Kancelarija;
import com.coworking_hub.app.model.KonferencijskaSala;
import com.coworking_hub.app.model.OtvoreniProstor;
import com.coworking_hub.app.model.Prostor;
import com.coworking_hub.app.model.Rezervacija;
import com.coworking_hub.app.model.enums.StatusProstora;
import com.coworking_hub.app.model.enums.StatusRezervacije;
import com.coworking_hub.app.repository.KancelarijaRepository;
import com.coworking_hub.app.repository.KonferencijskaSalaRepository;
import com.coworking_hub.app.repository.OtvoreniProstorRepository;
import com.coworking_hub.app.repository.ProstorRepository;
import com.coworking_hub.app.repository.ReakcijaRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MemberControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ProstorRepository prostorRepository;

    @Mock
    private ReakcijaRepository reakcijaRepository;

    @Mock
    private OtvoreniProstorRepository otvoreniProstorRepository;

    @Mock
    private KancelarijaRepository kancelarijaRepository;

    @Mock
    private KonferencijskaSalaRepository konferencijskaSalaRepository;

    @Mock
    private RezervacijaRepository rezervacijaRepository;

    @BeforeEach
    void setUp() {
        MemberController memberController = new MemberController(
                prostorRepository,
                reakcijaRepository,
                otvoreniProstorRepository,
                kancelarijaRepository,
                konferencijskaSalaRepository,
                rezervacijaRepository
        );
        mockMvc = MockMvcBuilders.standaloneSetup(memberController)
                .setCustomArgumentResolvers(new CurrentUserArgumentResolver())
                .build();
    }

    @Test
    void searchSpacesShouldReturnOnlyOfficesMatchingMinDesks() throws Exception {
        Prostor dorcol = buildSpace(10L, "Hub Dorcol", "Beograd", "Coworking Plus");
        Prostor ns = buildSpace(20L, "NS Business Hub", "Novi Sad", "Coworking NS");

        when(prostorRepository.searchApproved(StatusProstora.odobren, "Hub", List.of("Beograd", "Novi Sad"), true))
                .thenReturn(List.of(dorcol, ns));

        when(otvoreniProstorRepository.findByProstorIdIn(List.of(10L, 20L))).thenReturn(List.of(
                buildOpenSpace(3001L, 10L, 20),
                buildOpenSpace(3002L, 20L, 16)
        ));

        when(kancelarijaRepository.findByProstorIdIn(List.of(10L, 20L))).thenReturn(List.of(
                buildOffice(1001L, 10L, 6),
                buildOffice(1002L, 20L, 4)
        ));

        when(konferencijskaSalaRepository.findByProstorIdIn(List.of(10L, 20L))).thenReturn(List.of(
                buildMeetingRoom(2001L, 10L),
                buildMeetingRoom(2002L, 20L)
        ));

        when(reakcijaRepository.countGroupedByProstorIds(List.of(10L, 20L))).thenReturn(List.of());

        mockMvc.perform(get("/api/member/spaces")
                        .param("name", "Hub")
                        .param("cities", "Beograd", "Novi Sad")
                        .param("type", "kancelarija")
                        .param("officeMinDesks", "5")
                        .requestAttr(JwtAuthInterceptor.AUTH_USER_ATTR, authenticatedUser(12L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(10))
                .andExpect(jsonPath("$.content[0].officeCount").value(1))
                .andExpect(jsonPath("$.content[0].maxOfficeDesks").value(6))
                .andExpect(jsonPath("$.content[0].matchingSubspaceIds.length()").value(1))
                .andExpect(jsonPath("$.content[0].matchingSubspaceIds[0]").value(1001));
    }

    @Test
    void searchSpacesShouldRejectOfficeMinDesksForNonOfficeType() throws Exception {
        mockMvc.perform(get("/api/member/spaces")
                        .param("type", "sala")
                        .param("officeMinDesks", "5")
                        .requestAttr(JwtAuthInterceptor.AUTH_USER_ATTR, authenticatedUser(12L)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("officeMinDesks je dozvoljen samo za kancelarija tip"));
    }

    @Test
    void availabilityShouldReturnBusySlotsForMultipleOfficeResources() throws Exception {
        when(kancelarijaRepository.findByProstorIdIn(List.of(10L))).thenReturn(List.of(
                buildOffice(1001L, 10L, 6),
                buildOffice(1002L, 10L, 8)
        ));

        Rezervacija officeAReservation = buildReservation(801L, 10L, "Hub Dorcol", "Beograd",
                LocalDateTime.of(2026, 2, 23, 9, 0),
                LocalDateTime.of(2026, 2, 23, 11, 0),
                StatusRezervacije.aktivna);
        officeAReservation.setKancelarija(buildOffice(1001L, 10L, 6));

        Rezervacija officeBReservation = buildReservation(802L, 10L, "Hub Dorcol", "Beograd",
                LocalDateTime.of(2026, 2, 24, 14, 0),
                LocalDateTime.of(2026, 2, 24, 16, 0),
                StatusRezervacije.potvrdjena);
        officeBReservation.setKancelarija(buildOffice(1002L, 10L, 8));

        when(rezervacijaRepository.findByProstorIdAndStatusNotAndDatumDoGreaterThanAndDatumOdLessThan(
                10L,
                StatusRezervacije.otkazana,
                LocalDateTime.of(2026, 2, 23, 0, 0),
                LocalDateTime.of(2026, 3, 2, 0, 0)
        )).thenReturn(List.of(officeAReservation, officeBReservation));

        mockMvc.perform(post("/api/member/spaces/10/availability")
                        .contentType("application/json")
                        .content("""
                                {
                                  "type": "kancelarija",
                                  "resourceIds": [1001, 1002],
                                  "weekStart": "2026-02-23"
                                }
                                """)
                        .requestAttr(JwtAuthInterceptor.AUTH_USER_ATTR, authenticatedUser(12L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.spaceId").value(10))
                .andExpect(jsonPath("$.type").value("kancelarija"))
                .andExpect(jsonPath("$.resources.length()").value(2))
                .andExpect(jsonPath("$.resources[0].resourceId").value(1001))
                .andExpect(jsonPath("$.resources[0].resourceName").value("Office 1001"))
                .andExpect(jsonPath("$.resources[0].busySlots.length()").value(1))
                .andExpect(jsonPath("$.resources[0].busySlots[0].from").value("2026-02-23T09:00:00"))
                .andExpect(jsonPath("$.resources[1].resourceId").value(1002))
                .andExpect(jsonPath("$.resources[1].resourceName").value("Office 1002"))
                .andExpect(jsonPath("$.resources[1].busySlots.length()").value(1))
                .andExpect(jsonPath("$.resources[1].busySlots[0].to").value("2026-02-24T16:00:00"));
    }

    @Test
    void availabilityShouldRejectResourceIdsThatDoNotBelongToSpaceAndType() throws Exception {
        when(kancelarijaRepository.findByProstorIdIn(List.of(10L))).thenReturn(List.of(
                buildOffice(1001L, 10L, 6)
        ));

        mockMvc.perform(post("/api/member/spaces/10/availability")
                        .contentType("application/json")
                        .content("""
                                {
                                  "type": "kancelarija",
                                  "resourceIds": [9999],
                                  "weekStart": "2026-02-23"
                                }
                                """)
                        .requestAttr(JwtAuthInterceptor.AUTH_USER_ATTR, authenticatedUser(12L)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Prosledjen je resourceId koji ne pripada trazenom prostoru/tipu"));
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

        private Prostor buildSpace(Long id, String naziv, String grad, String firmaNaziv) {
                Firma firma = new Firma();
                firma.setId(1L);
                firma.setNaziv(firmaNaziv);

                Prostor prostor = new Prostor();
                prostor.setId(id);
                prostor.setNaziv(naziv);
                prostor.setGrad(grad);
                prostor.setAdresa("Test adresa");
                prostor.setFirma(firma);
                return prostor;
        }

        private OtvoreniProstor buildOpenSpace(Long openSpaceId, Long spaceId, Integer desks) {
                Prostor prostor = new Prostor();
                prostor.setId(spaceId);

                OtvoreniProstor openSpace = new OtvoreniProstor();
                openSpace.setId(openSpaceId);
                openSpace.setProstor(prostor);
                openSpace.setBrojStolova(desks);
                return openSpace;
        }

        private Kancelarija buildOffice(Long officeId, Long spaceId, Integer desks) {
                Prostor prostor = new Prostor();
                prostor.setId(spaceId);

                Kancelarija office = new Kancelarija();
                office.setId(officeId);
                office.setProstor(prostor);
                office.setNaziv("Office " + officeId);
                office.setBrojStolova(desks);
                return office;
        }

        private KonferencijskaSala buildMeetingRoom(Long roomId, Long spaceId) {
                Prostor prostor = new Prostor();
                prostor.setId(spaceId);

                KonferencijskaSala room = new KonferencijskaSala();
                room.setId(roomId);
                room.setProstor(prostor);
                room.setBrojMesta(10);
                return room;
        }

    private AuthenticatedUser authenticatedUser(Long userId) {
        return new AuthenticatedUser(userId, "marko", "clan", "odobren");
    }
}