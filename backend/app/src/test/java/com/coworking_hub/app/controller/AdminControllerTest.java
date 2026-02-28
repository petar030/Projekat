package com.coworking_hub.app.controller;

import com.coworking_hub.app.model.Firma;
import com.coworking_hub.app.model.Korisnik;
import com.coworking_hub.app.model.Prostor;
import com.coworking_hub.app.model.Reakcija;
import com.coworking_hub.app.model.Rezervacija;
import com.coworking_hub.app.model.enums.StatusKorisnika;
import com.coworking_hub.app.model.enums.StatusProstora;
import com.coworking_hub.app.model.enums.StatusRezervacije;
import com.coworking_hub.app.model.enums.TipReakcije;
import com.coworking_hub.app.model.enums.Uloga;
import com.coworking_hub.app.repository.KorisnikRepository;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    private MockMvc mockMvc;

    @Mock
    private KorisnikRepository korisnikRepository;

    @Mock
    private ProstorRepository prostorRepository;

    @Mock
    private ReakcijaRepository reakcijaRepository;

    @Mock
    private RezervacijaRepository rezervacijaRepository;

    @BeforeEach
    void setUp() {
        AdminController adminController = new AdminController(
            korisnikRepository,
            prostorRepository,
            reakcijaRepository,
            rezervacijaRepository
        );
        mockMvc = MockMvcBuilders.standaloneSetup(adminController)
                .setCustomArgumentResolvers(new CurrentUserArgumentResolver())
                .build();
    }

    @Test
    void usersShouldReturnFilteredContent() throws Exception {
        when(korisnikRepository.findAll()).thenReturn(List.of(
                buildUser(1L, "ana", Uloga.clan, StatusKorisnika.odobren),
                buildUser(2L, "marko", Uloga.clan, StatusKorisnika.na_cekanju),
                buildUser(3L, "menadzer_beograd", Uloga.menadzer, StatusKorisnika.odobren)
        ));

        mockMvc.perform(get("/api/admin/users")
                        .param("role", "clan")
                        .param("search", "ana")
                        .requestAttr(JwtAuthInterceptor.AUTH_USER_ATTR, authenticatedAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].username").value("ana"));
    }

    @Test
    void usersShouldRejectNonAdmin() throws Exception {
        mockMvc.perform(get("/api/admin/users")
                        .requestAttr(JwtAuthInterceptor.AUTH_USER_ATTR, authenticatedManager()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Samo admin ima pristup"));
    }

    @Test
    void registrationRequestsShouldReturnOnlyPendingMemberAndManager() throws Exception {
        when(korisnikRepository.findAll()).thenReturn(List.of(
                buildUser(1L, "ana", Uloga.clan, StatusKorisnika.na_cekanju),
                buildUser(2L, "admin", Uloga.admin, StatusKorisnika.na_cekanju),
                buildUser(3L, "marko", Uloga.clan, StatusKorisnika.odobren)
        ));

        mockMvc.perform(get("/api/admin/registration-requests")
                        .requestAttr(JwtAuthInterceptor.AUTH_USER_ATTR, authenticatedAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requests.length()").value(1))
                .andExpect(jsonPath("$.requests[0].username").value("ana"));
    }

    @Test
    void approveRegistrationRequestShouldUpdateStatus() throws Exception {
        Korisnik pending = buildUser(11L, "ana", Uloga.clan, StatusKorisnika.na_cekanju);

        when(korisnikRepository.findById(11L)).thenReturn(Optional.of(pending));
        when(korisnikRepository.save(any(Korisnik.class))).thenReturn(pending);

        mockMvc.perform(patch("/api/admin/registration-requests/11/approve")
                        .requestAttr(JwtAuthInterceptor.AUTH_USER_ATTR, authenticatedAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(11))
                .andExpect(jsonPath("$.status").value("odobren"));
    }

    @Test
    void spaceRequestsShouldReturnPendingSpaces() throws Exception {
        when(prostorRepository.findAll()).thenReturn(List.of(
                buildSpace(10L, "Hub Dorcol", StatusProstora.na_cekanju),
                buildSpace(11L, "Hub Novi Beograd", StatusProstora.odobren)
        ));

        mockMvc.perform(get("/api/admin/space-requests")
                        .requestAttr(JwtAuthInterceptor.AUTH_USER_ATTR, authenticatedAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requests.length()").value(1))
                .andExpect(jsonPath("$.requests[0].spaceId").value(10));
    }

    @Test
    void approveSpaceRequestShouldUpdateStatus() throws Exception {
        Prostor space = buildSpace(10L, "Hub Dorcol", StatusProstora.na_cekanju);

        when(prostorRepository.findById(10L)).thenReturn(Optional.of(space));
        when(prostorRepository.save(any(Prostor.class))).thenReturn(space);

        mockMvc.perform(patch("/api/admin/space-requests/10/approve")
                        .requestAttr(JwtAuthInterceptor.AUTH_USER_ATTR, authenticatedAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.spaceId").value(10))
                .andExpect(jsonPath("$.status").value("odobren"));
    }

    @Test
    void updateUserShouldReturnConflictWhenEmailExists() throws Exception {
        Korisnik target = buildUser(5L, "ana", Uloga.clan, StatusKorisnika.na_cekanju);
        Korisnik other = buildUser(7L, "marko", Uloga.clan, StatusKorisnika.odobren);
        other.setEmail("novi@example.com");

        when(korisnikRepository.findById(5L)).thenReturn(Optional.of(target));
        when(korisnikRepository.findByEmail("novi@example.com")).thenReturn(Optional.of(other));

        mockMvc.perform(put("/api/admin/users/5")
                        .contentType("application/json")
                        .content("""
                                {
                                  \"ime\": \"Ana\",
                                  \"prezime\": \"Anic\",
                                  \"telefon\": \"+38161111\",
                                  \"email\": \"novi@example.com\",
                                  \"status\": \"odobren\"
                                }
                                """)
                        .requestAttr(JwtAuthInterceptor.AUTH_USER_ATTR, authenticatedAdmin()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Email vec postoji"));
    }

    @Test
    void deleteUserShouldReturnNoContent() throws Exception {
        when(korisnikRepository.findById(33L)).thenReturn(Optional.of(buildUser(33L, "petar", Uloga.clan, StatusKorisnika.odobren)));

        mockMvc.perform(delete("/api/admin/users/33")
                        .requestAttr(JwtAuthInterceptor.AUTH_USER_ATTR, authenticatedAdmin()))
                .andExpect(status().isNoContent());
    }


            @Test
            void statsSpacesShouldReturnApprovedSpaces() throws Exception {
            when(prostorRepository.findAll()).thenReturn(List.of(
                buildSpace(10L, "Hub Dorcol", StatusProstora.odobren),
                buildSpace(11L, "Hub Novi Beograd", StatusProstora.na_cekanju)
            ));

            mockMvc.perform(get("/api/admin/stats/spaces")
                    .requestAttr(JwtAuthInterceptor.AUTH_USER_ATTR, authenticatedAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.spaces.length()").value(1))
                .andExpect(jsonPath("$.spaces[0].spaceId").value(10))
                .andExpect(jsonPath("$.spaces[0].spaceName").value("Hub Dorcol"));
            }

            @Test
            void spaceMonthlyStatsShouldReturnAllMonthlyMetrics() throws Exception {
            Prostor space = buildSpace(10L, "Hub Dorcol", StatusProstora.odobren);

            when(prostorRepository.findById(10L)).thenReturn(Optional.of(space));
            when(reakcijaRepository.findAll()).thenReturn(List.of(
                buildReaction(10L, TipReakcije.svidjanje, LocalDateTime.of(2026, 2, 10, 10, 0)),
                buildReaction(10L, TipReakcije.nesvidjanje, LocalDateTime.of(2026, 2, 11, 10, 0))
            ));
            when(rezervacijaRepository.findAll()).thenReturn(List.of(
                buildReservation(10L, StatusRezervacije.potvrdjena, LocalDateTime.of(2026, 2, 13, 10, 0), LocalDateTime.of(2026, 2, 13, 12, 0))
            ));

            mockMvc.perform(get("/api/admin/stats/space-monthly")
                    .param("spaceId", "10")
                    .param("year", "2026")
                    .requestAttr(JwtAuthInterceptor.AUTH_USER_ATTR, authenticatedAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.spaceId").value(10))
                .andExpect(jsonPath("$.year").value(2026))
                .andExpect(jsonPath("$.items.length()").value(12))
                .andExpect(jsonPath("$.items[1].month").value(2))
                .andExpect(jsonPath("$.items[1].likes").value(1))
                .andExpect(jsonPath("$.items[1].dislikes").value(1))
                .andExpect(jsonPath("$.items[1].reservations").value(1))
                .andExpect(jsonPath("$.items[1].revenue").value(25.0));
            }

    private Korisnik buildUser(Long id, String username, Uloga role, StatusKorisnika status) {
        Korisnik user = new Korisnik();
        user.setId(id);
        user.setKorisnickoIme(username);
        user.setIme("Ime");
        user.setPrezime("Prezime");
        user.setEmail(username + "@example.com");
        user.setTelefon("+381600000000");
        user.setUloga(role);
        user.setStatus(status);
        user.setKreirano(LocalDateTime.of(2026, 1, 10, 10, 0));
        user.setAzurirano(LocalDateTime.of(2026, 1, 10, 10, 0));
        return user;
    }

    private Prostor buildSpace(Long id, String naziv, StatusProstora status) {
        Firma firma = new Firma();
        firma.setId(2L);
        firma.setNaziv("Coworking Plus");

        Prostor space = new Prostor();
        space.setId(id);
        space.setNaziv(naziv);
        space.setGrad("Beograd");
        space.setStatus(status);
        space.setCenaPoSatu(java.math.BigDecimal.valueOf(12.5));
        space.setFirma(firma);
        space.setKreirano(LocalDateTime.of(2026, 2, 10, 10, 0));
        space.setAzurirano(LocalDateTime.of(2026, 2, 10, 10, 0));
        return space;
    }

    private Reakcija buildReaction(Long spaceId, TipReakcije type, LocalDateTime createdAt) {
        Prostor prostor = new Prostor();
        prostor.setId(spaceId);

        Reakcija reaction = new Reakcija();
        reaction.setProstor(prostor);
        reaction.setTip(type);
        reaction.setKreirano(createdAt);
        return reaction;
    }

    private Rezervacija buildReservation(Long spaceId, StatusRezervacije status, LocalDateTime from, LocalDateTime to) {
        Prostor prostor = new Prostor();
        prostor.setId(spaceId);
        prostor.setCenaPoSatu(java.math.BigDecimal.valueOf(12.5));

        Rezervacija reservation = new Rezervacija();
        reservation.setProstor(prostor);
        reservation.setStatus(status);
        reservation.setDatumOd(from);
        reservation.setDatumDo(to);
        return reservation;
    }

    private AuthenticatedUser authenticatedAdmin() {
        return new AuthenticatedUser(1L, "admin", "admin", "odobren");
    }

    private AuthenticatedUser authenticatedManager() {
        return new AuthenticatedUser(2L, "manager", "menadzer", "odobren");
    }
}
