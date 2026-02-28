package com.coworking_hub.app.controller;

import com.coworking_hub.app.model.Firma;
import com.coworking_hub.app.model.Kancelarija;
import com.coworking_hub.app.model.KonferencijskaSala;
import com.coworking_hub.app.model.Korisnik;
import com.coworking_hub.app.model.OtvoreniProstor;
import com.coworking_hub.app.model.Prostor;
import com.coworking_hub.app.model.Rezervacija;
import com.coworking_hub.app.model.enums.StatusProstora;
import com.coworking_hub.app.model.enums.StatusRezervacije;
import com.coworking_hub.app.repository.KancelarijaRepository;
import com.coworking_hub.app.repository.KonferencijskaSalaRepository;
import com.coworking_hub.app.repository.KorisnikRepository;
import com.coworking_hub.app.repository.OtvoreniProstorRepository;
import com.coworking_hub.app.repository.ProstorRepository;
import com.coworking_hub.app.repository.RezervacijaRepository;
import com.coworking_hub.app.repository.SlikaProstoraRepository;
import com.coworking_hub.app.security.AuthenticatedUser;
import com.coworking_hub.app.security.CurrentUserArgumentResolver;
import com.coworking_hub.app.security.JwtAuthInterceptor;
import com.coworking_hub.app.service.ImageStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ManagerControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ProstorRepository prostorRepository;

    @Mock
    private KorisnikRepository korisnikRepository;

    @Mock
    private OtvoreniProstorRepository otvoreniProstorRepository;

    @Mock
    private KancelarijaRepository kancelarijaRepository;

    @Mock
    private KonferencijskaSalaRepository konferencijskaSalaRepository;

    @Mock
    private RezervacijaRepository rezervacijaRepository;

    @Mock
    private SlikaProstoraRepository slikaProstoraRepository;

    @Mock
    private ImageStorageService imageStorageService;

    @BeforeEach
    void setUp() {
        ManagerController managerController = new ManagerController(
                prostorRepository,
                korisnikRepository,
                otvoreniProstorRepository,
                kancelarijaRepository,
                konferencijskaSalaRepository,
                rezervacijaRepository,
                slikaProstoraRepository,
                imageStorageService
        );
        mockMvc = MockMvcBuilders.standaloneSetup(managerController)
                .setCustomArgumentResolvers(new CurrentUserArgumentResolver())
                .build();
    }

    @Test
    void spacesShouldReturnManagerCompanySpacesWithElements() throws Exception {
        Korisnik manager = buildManager(12L, 2L);
        Prostor space = buildSpace(10L, "Hub Dorcol", "Beograd", StatusProstora.odobren, 3, 2L);

        when(korisnikRepository.findById(12L)).thenReturn(Optional.of(manager));
        when(prostorRepository.findByFirmaIdOrderByIdAsc(2L)).thenReturn(List.of(space));
        when(otvoreniProstorRepository.findByProstorIdIn(List.of(10L))).thenReturn(List.of(
                buildOpenSpace(3001L, 10L, 20)
        ));
        when(kancelarijaRepository.findByProstorIdIn(List.of(10L))).thenReturn(List.of(
                buildOffice(1001L, 10L, "Office A", 3)
        ));
        when(konferencijskaSalaRepository.findByProstorIdIn(List.of(10L))).thenReturn(List.of(
                buildMeetingRoom(2001L, 10L, "Sala Alfa", 10, "TV i whiteboard")
        ));

        mockMvc.perform(get("/api/manager/spaces")
                        .requestAttr(JwtAuthInterceptor.AUTH_USER_ATTR, authenticatedUser(12L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.spaces.length()").value(1))
                .andExpect(jsonPath("$.spaces[0].id").value(10))
                .andExpect(jsonPath("$.spaces[0].naziv").value("Hub Dorcol"))
                .andExpect(jsonPath("$.spaces[0].grad").value("Beograd"))
                .andExpect(jsonPath("$.spaces[0].status").value("odobren"))
                .andExpect(jsonPath("$.spaces[0].pragKazni").value(3))
                .andExpect(jsonPath("$.spaces[0].elements.openSpace.id").value(3001))
                .andExpect(jsonPath("$.spaces[0].elements.openSpace.brojStolova").value(20))
                .andExpect(jsonPath("$.spaces[0].elements.offices[0].naziv").value("Office A"))
                .andExpect(jsonPath("$.spaces[0].elements.offices[0].brojStolova").value(3))
                .andExpect(jsonPath("$.spaces[0].elements.meetingRooms[0].naziv").value("Sala Alfa"))
                .andExpect(jsonPath("$.spaces[0].elements.meetingRooms[0].dodatnaOprema").value("TV i whiteboard"));
    }

    @Test
    void spacesShouldReturnBadRequestWhenManagerHasNoCompany() throws Exception {
        Korisnik manager = new Korisnik();
        manager.setId(12L);

        when(korisnikRepository.findById(12L)).thenReturn(Optional.of(manager));

        mockMvc.perform(get("/api/manager/spaces")
                        .requestAttr(JwtAuthInterceptor.AUTH_USER_ATTR, authenticatedUser(12L)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Menadzer nema dodeljenu firmu"));
    }

            @Test
            void createSpaceShouldCreateOpenSpaceAndImages() throws Exception {
            Korisnik manager = buildManager(12L, 2L);
            Prostor savedSpace = buildSpace(10L, "Hub Dorcol", "Beograd", StatusProstora.na_cekanju, 3, 2L);

            when(korisnikRepository.findById(12L)).thenReturn(Optional.of(manager));
            when(prostorRepository.save(any(Prostor.class))).thenReturn(savedSpace);
            when(imageStorageService.storeSpaceImage(any(MockMultipartFile.class), any(Long.class)))
                .thenReturn("/uploads/spaces/10/i1.jpg");

            MockMultipartFile data = new MockMultipartFile(
                "data",
                "",
                "application/json",
                """
                    {
                      "naziv": "Hub Dorcol",
                      "grad": "Beograd",
                      "adresa": "Cara Dusana 10",
                      "opis": "Opis",
                      "cenaPoSatu": 15.0,
                      "pragKazni": 3,
                      "geografskaSirina": 44.8176,
                      "geografskaDuzina": 20.4633,
                      "openSpace": {
                        "brojStolova": 20
                      }
                    }
                    """.getBytes()
            );

            MockMultipartFile image = new MockMultipartFile(
                "images",
                "space.jpg",
                "image/jpeg",
                "fake-image".getBytes()
            );

            mockMvc.perform(multipart("/api/manager/spaces")
                    .file(data)
                    .file(image)
                    .with(postMethod())
                    .requestAttr(JwtAuthInterceptor.AUTH_USER_ATTR, authenticatedUser(12L)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.status").value("na_cekanju"));
            }

            @Test
            void createSpaceShouldRejectMoreThanFiveImages() throws Exception {
            Korisnik manager = buildManager(12L, 2L);
            when(korisnikRepository.findById(12L)).thenReturn(Optional.of(manager));

            MockMultipartFile data = new MockMultipartFile(
                "data",
                "",
                "application/json",
                """
                    {
                      "naziv": "Hub Dorcol",
                      "grad": "Beograd",
                      "adresa": "Cara Dusana 10",
                      "opis": "Opis",
                      "cenaPoSatu": 15.0,
                      "pragKazni": 3,
                      "openSpace": {
                        "brojStolova": 20
                      }
                    }
                    """.getBytes()
            );

            MockMultipartFile i1 = new MockMultipartFile("images", "1.jpg", "image/jpeg", "1".getBytes());
            MockMultipartFile i2 = new MockMultipartFile("images", "2.jpg", "image/jpeg", "2".getBytes());
            MockMultipartFile i3 = new MockMultipartFile("images", "3.jpg", "image/jpeg", "3".getBytes());
            MockMultipartFile i4 = new MockMultipartFile("images", "4.jpg", "image/jpeg", "4".getBytes());
            MockMultipartFile i5 = new MockMultipartFile("images", "5.jpg", "image/jpeg", "5".getBytes());
            MockMultipartFile i6 = new MockMultipartFile("images", "6.jpg", "image/jpeg", "6".getBytes());

            mockMvc.perform(multipart("/api/manager/spaces")
                    .file(data)
                    .file(i1).file(i2).file(i3).file(i4).file(i5).file(i6)
                    .with(postMethod())
                    .requestAttr(JwtAuthInterceptor.AUTH_USER_ATTR, authenticatedUser(12L)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Maksimalan broj slika je 5"));
            }

            @Test
            void addOfficeShouldCreateOffice() throws Exception {
            Korisnik manager = buildManager(12L, 2L);
            Prostor space = buildSpace(10L, "Hub Dorcol", "Beograd", StatusProstora.odobren, 3, 2L);
            Kancelarija saved = buildOffice(1001L, 10L, "Office A", 4);

            when(korisnikRepository.findById(12L)).thenReturn(Optional.of(manager));
            when(prostorRepository.findByIdAndFirmaId(10L, 2L)).thenReturn(Optional.of(space));
            when(kancelarijaRepository.save(any(Kancelarija.class))).thenReturn(saved);

            mockMvc.perform(post("/api/manager/spaces/10/offices")
                    .contentType("application/json")
                    .content("""
                        {
                          "naziv": "Office A",
                          "brojStolova": 4
                        }
                        """)
                    .requestAttr(JwtAuthInterceptor.AUTH_USER_ATTR, authenticatedUser(12L)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1001))
                .andExpect(jsonPath("$.spaceId").value(10))
                .andExpect(jsonPath("$.naziv").value("Office A"));
            }

            @Test
            void addMeetingRoomShouldCreateMeetingRoom() throws Exception {
            Korisnik manager = buildManager(12L, 2L);
            Prostor space = buildSpace(10L, "Hub Dorcol", "Beograd", StatusProstora.odobren, 3, 2L);
            KonferencijskaSala saved = buildMeetingRoom(2001L, 10L, "Sala Alfa", 10, "TV i whiteboard");

            when(korisnikRepository.findById(12L)).thenReturn(Optional.of(manager));
            when(prostorRepository.findByIdAndFirmaId(10L, 2L)).thenReturn(Optional.of(space));
            when(konferencijskaSalaRepository.save(any(KonferencijskaSala.class))).thenReturn(saved);

            mockMvc.perform(post("/api/manager/spaces/10/meeting-rooms")
                    .contentType("application/json")
                    .content("""
                        {
                          "naziv": "Sala Alfa",
                          "dodatnaOprema": "TV i whiteboard"
                        }
                        """)
                    .requestAttr(JwtAuthInterceptor.AUTH_USER_ATTR, authenticatedUser(12L)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2001))
                .andExpect(jsonPath("$.spaceId").value(10))
                .andExpect(jsonPath("$.naziv").value("Sala Alfa"))
                .andExpect(jsonPath("$.dodatnaOprema").value("TV i whiteboard"));
            }

                    @Test
                    void reservationsShouldReturnCurrentManagerReservations() throws Exception {
                    Korisnik manager = buildManager(12L, 2L);
                    Prostor space = buildSpace(10L, "Hub Dorcol", "Beograd", StatusProstora.odobren, 3, 2L);
                    LocalDateTime now = currentUtcTime();
                    Rezervacija reservation = buildReservation(501L, space, StatusRezervacije.aktivna, now.minusMinutes(2), now.plusHours(1));
                    reservation.setKancelarija(buildOffice(1001L, 10L, "Office A", 4));

                    when(korisnikRepository.findById(12L)).thenReturn(Optional.of(manager));
                    when(prostorRepository.findByFirmaIdOrderByIdAsc(2L)).thenReturn(List.of(space));
                    when(rezervacijaRepository.findByProstorIdInAndStatusNotOrderByDatumOdAsc(List.of(10L), StatusRezervacije.otkazana))
                        .thenReturn(List.of(reservation));

                    mockMvc.perform(get("/api/manager/reservations")
                            .requestAttr(JwtAuthInterceptor.AUTH_USER_ATTR, authenticatedUser(12L)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.content.length()").value(1))
                        .andExpect(jsonPath("$.content[0].id").value(501))
                        .andExpect(jsonPath("$.content[0].member.username").value("marko"))
                        .andExpect(jsonPath("$.content[0].type").value("kancelarija"))
                        .andExpect(jsonPath("$.content[0].resourceName").value("Office A"))
                        .andExpect(jsonPath("$.content[0].canConfirmOrNoShow").value(true));
                    }

                    @Test
                    void confirmReservationShouldUpdateStatusWithinWindow() throws Exception {
                    Korisnik manager = buildManager(12L, 2L);
                    Prostor space = buildSpace(10L, "Hub Dorcol", "Beograd", StatusProstora.odobren, 3, 2L);
                    LocalDateTime now = currentUtcTime();
                    Rezervacija reservation = buildReservation(601L, space, StatusRezervacije.aktivna, now.minusMinutes(3), now.plusMinutes(30));

                    when(korisnikRepository.findById(12L)).thenReturn(Optional.of(manager));
                    when(rezervacijaRepository.findById(601L)).thenReturn(Optional.of(reservation));
                    when(rezervacijaRepository.save(any(Rezervacija.class))).thenReturn(reservation);

                    mockMvc.perform(patch("/api/manager/reservations/601/confirm")
                            .requestAttr(JwtAuthInterceptor.AUTH_USER_ATTR, authenticatedUser(12L)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.id").value(601))
                        .andExpect(jsonPath("$.status").value("potvrdjena"));
                    }

                    @Test
                    void noShowReservationShouldFailWhenOutsideTenMinuteWindow() throws Exception {
                    Korisnik manager = buildManager(12L, 2L);
                    Prostor space = buildSpace(10L, "Hub Dorcol", "Beograd", StatusProstora.odobren, 3, 2L);
                    LocalDateTime now = currentUtcTime();
                    Rezervacija reservation = buildReservation(701L, space, StatusRezervacije.aktivna, now.minusMinutes(20), now.plusMinutes(40));

                    when(korisnikRepository.findById(12L)).thenReturn(Optional.of(manager));
                    when(rezervacijaRepository.findById(701L)).thenReturn(Optional.of(reservation));

                    mockMvc.perform(patch("/api/manager/reservations/701/no-show")
                            .requestAttr(JwtAuthInterceptor.AUTH_USER_ATTR, authenticatedUser(12L)))
                            .andExpect(status().is(422))
                        .andExpect(jsonPath("$.message").value("Odjavljivanje je dozvoljeno samo u roku od 10 minuta od pocetka aktivne rezervacije"));
                    }

                        @Test
                        void calendarShouldReturnEventsForSelectedResourceAndInterval() throws Exception {
                        Korisnik manager = buildManager(12L, 2L);
                        Prostor space = buildSpace(10L, "Hub Dorcol", "Beograd", StatusProstora.odobren, 3, 2L);

                        Rezervacija matchingReservation = buildReservation(
                            801L,
                            space,
                            StatusRezervacije.aktivna,
                            LocalDateTime.of(2026, 3, 10, 10, 0),
                            LocalDateTime.of(2026, 3, 10, 12, 0)
                        );
                        matchingReservation.setKancelarija(buildOffice(1001L, 10L, "Office A", 4));

                        Rezervacija otherResourceReservation = buildReservation(
                            802L,
                            space,
                            StatusRezervacije.aktivna,
                            LocalDateTime.of(2026, 3, 10, 11, 0),
                            LocalDateTime.of(2026, 3, 10, 13, 0)
                        );
                        otherResourceReservation.setKancelarija(buildOffice(1002L, 10L, "Office B", 5));

                        when(korisnikRepository.findById(12L)).thenReturn(Optional.of(manager));
                        when(prostorRepository.findByIdAndFirmaId(10L, 2L)).thenReturn(Optional.of(space));
                        when(kancelarijaRepository.findByProstorIdIn(List.of(10L))).thenReturn(List.of(
                            buildOffice(1001L, 10L, "Office A", 4),
                            buildOffice(1002L, 10L, "Office B", 5)
                        ));
                        when(rezervacijaRepository.findByProstorIdInAndStatusNotAndDatumDoGreaterThanAndDatumOdLessThanOrderByDatumOdAsc(
                            List.of(10L),
                            StatusRezervacije.otkazana,
                            LocalDateTime.of(2026, 3, 10, 0, 0),
                            LocalDateTime.of(2026, 3, 11, 0, 0)
                        )).thenReturn(List.of(matchingReservation, otherResourceReservation));

                        mockMvc.perform(get("/api/manager/calendar")
                            .param("spaceId", "10")
                            .param("type", "kancelarija")
                            .param("resourceId", "1001")
                            .param("from", "2026-03-10T00:00:00")
                            .param("to", "2026-03-11T00:00:00")
                            .requestAttr(JwtAuthInterceptor.AUTH_USER_ATTR, authenticatedUser(12L)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.events.length()").value(1))
                        .andExpect(jsonPath("$.events[0].reservationId").value(801))
                        .andExpect(jsonPath("$.events[0].title").value("marko"))
                        .andExpect(jsonPath("$.events[0].status").value("aktivna"));
                        }

                        @Test
                        void calendarShouldRejectInvalidType() throws Exception {
                        Korisnik manager = buildManager(12L, 2L);

                        when(korisnikRepository.findById(12L)).thenReturn(Optional.of(manager));

                        mockMvc.perform(get("/api/manager/calendar")
                            .param("spaceId", "10")
                            .param("type", "nepoznat")
                            .param("resourceId", "1001")
                            .param("from", "2026-03-10T00:00:00")
                            .param("to", "2026-03-11T00:00:00")
                            .requestAttr(JwtAuthInterceptor.AUTH_USER_ATTR, authenticatedUser(12L)))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.message").value("Type mora biti otvoreni, kancelarija ili sala"));
                        }

    private Korisnik buildManager(Long id, Long firmaId) {
        Firma firma = new Firma();
        firma.setId(firmaId);

        Korisnik manager = new Korisnik();
        manager.setId(id);
        manager.setFirma(firma);
        return manager;
    }

    private Prostor buildSpace(Long id, String naziv, String grad, StatusProstora status, Integer pragKazni, Long firmaId) {
        Firma firma = new Firma();
        firma.setId(firmaId);

        Prostor prostor = new Prostor();
        prostor.setId(id);
        prostor.setNaziv(naziv);
        prostor.setGrad(grad);
        prostor.setStatus(status);
        prostor.setPragKazni(pragKazni);
        prostor.setFirma(firma);
        return prostor;
    }

    private OtvoreniProstor buildOpenSpace(Long id, Long spaceId, Integer desks) {
        Prostor prostor = new Prostor();
        prostor.setId(spaceId);

        OtvoreniProstor openSpace = new OtvoreniProstor();
        openSpace.setId(id);
        openSpace.setProstor(prostor);
        openSpace.setBrojStolova(desks);
        return openSpace;
    }

    private Kancelarija buildOffice(Long id, Long spaceId, String naziv, Integer desks) {
        Prostor prostor = new Prostor();
        prostor.setId(spaceId);

        Kancelarija office = new Kancelarija();
        office.setId(id);
        office.setProstor(prostor);
        office.setNaziv(naziv);
        office.setBrojStolova(desks);
        return office;
    }

    private KonferencijskaSala buildMeetingRoom(Long id, Long spaceId, String naziv, Integer seats, String equipment) {
        Prostor prostor = new Prostor();
        prostor.setId(spaceId);

        KonferencijskaSala room = new KonferencijskaSala();
        room.setId(id);
        room.setProstor(prostor);
        room.setNaziv(naziv);
        room.setBrojMesta(seats);
        room.setDodatnaOprema(equipment);
        return room;
    }

    private Rezervacija buildReservation(Long id, Prostor prostor, StatusRezervacije status, LocalDateTime from, LocalDateTime to) {
        Korisnik member = new Korisnik();
        member.setId(33L);
        member.setKorisnickoIme("marko");

        Rezervacija reservation = new Rezervacija();
        reservation.setId(id);
        reservation.setClan(member);
        reservation.setProstor(prostor);
        reservation.setStatus(status);
        reservation.setDatumOd(from);
        reservation.setDatumDo(to);
        reservation.setKreirano(from.minusHours(1));
        reservation.setAzurirano(from.minusHours(1));
        return reservation;
    }

    private AuthenticatedUser authenticatedUser(Long userId) {
        return new AuthenticatedUser(userId, "manager", "menadzer", "odobren");
    }

    private LocalDateTime currentUtcTime() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    private RequestPostProcessor postMethod() {
        return request -> {
            request.setMethod("POST");
            return request;
        };
    }
}
