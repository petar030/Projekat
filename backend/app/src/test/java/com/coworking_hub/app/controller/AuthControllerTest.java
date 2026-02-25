package com.coworking_hub.app.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.coworking_hub.app.model.Korisnik;
import com.coworking_hub.app.model.TokenZaResetLozinke;
import com.coworking_hub.app.model.enums.StatusKorisnika;
import com.coworking_hub.app.model.enums.Uloga;
import com.coworking_hub.app.repository.FirmaRepository;
import com.coworking_hub.app.repository.KorisnikRepository;
import com.coworking_hub.app.repository.TokenZaResetLozinkeRepository;
import com.coworking_hub.app.security.JwtService;
import com.coworking_hub.app.service.ImageStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;

        private final ObjectMapper objectMapper = new ObjectMapper();
        private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

        @Mock
    private JwtService jwtService;

        @Mock
        private KorisnikRepository korisnikRepository;

        @Mock
        private FirmaRepository firmaRepository;

        @Mock
        private TokenZaResetLozinkeRepository tokenZaResetLozinkeRepository;

        @Mock
        private ImageStorageService imageStorageService;

    @BeforeEach
    void setUp() {
                AuthController authController = new AuthController(
                                jwtService,
                                korisnikRepository,
                                firmaRepository,
                                tokenZaResetLozinkeRepository,
                                imageStorageService
                );
                mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
    }

    @Test
    void loginShouldReturnAccessToken() throws Exception {
                Korisnik korisnik = new Korisnik();
                korisnik.setId(12L);
                korisnik.setKorisnickoIme("marko");
                korisnik.setLozinka(passwordEncoder.encode("Marko123!"));
                korisnik.setUloga(Uloga.clan);
                korisnik.setStatus(StatusKorisnika.odobren);

                when(korisnikRepository.findByKorisnickoIme("marko")).thenReturn(Optional.of(korisnik));
        when(jwtService.generateAccessToken(anyLong(), anyString(), anyString(), anyString()))
                .thenReturn("test.jwt.token");

        String requestJson = """
                {
                  "username": "marko",
                  "password": "Marko123!"
                }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("test.jwt.token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.username").value("marko"))
                .andExpect(jsonPath("$.user.role").value("clan"));
    }

    @Test
    void adminLoginWithNonAdminShouldReturnUnauthorized() throws Exception {
                                Korisnik korisnik = new Korisnik();
                                korisnik.setId(12L);
                                korisnik.setKorisnickoIme("marko");
                                korisnik.setLozinka(passwordEncoder.encode("Marko123!"));
                                korisnik.setUloga(Uloga.clan);
                                korisnik.setStatus(StatusKorisnika.odobren);

                                when(korisnikRepository.findByKorisnickoIme("marko")).thenReturn(Optional.of(korisnik));

        String requestJson = """
                {
                  "username": "marko",
                  "password": "Marko123!"
                }
                """;

        mockMvc.perform(post("/api/auth/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Pogresni admin kredencijali"));
    }

    @Test
    void registerMemberShouldReturnCreated() throws Exception {
                when(korisnikRepository.existsByKorisnickoIme("milica")).thenReturn(false);
                when(korisnikRepository.existsByEmail("milica@example.com")).thenReturn(false);
                when(korisnikRepository.save(any(Korisnik.class))).thenAnswer(invocation -> {
                        Korisnik saved = invocation.getArgument(0);
                        saved.setId(33L);
                        return saved;
                });

        MockMultipartFile dataPart = new MockMultipartFile(
                "data",
                "",
                "application/json",
                """
                        {
                          "username": "milica",
                          "password": "Milica123!",
                          "ime": "Milica",
                          "prezime": "Petrovic",
                          "telefon": "+38160111222",
                          "email": "milica@example.com"
                        }
                        """.getBytes()
        );

        mockMvc.perform(multipart("/api/auth/register/member")
                        .file(dataPart)
                        .contentType(MediaType.MULTIPART_FORM_DATA_VALUE))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("milica"))
                .andExpect(jsonPath("$.role").value("clan"))
                .andExpect(jsonPath("$.status").value("na_cekanju"));

                verify(korisnikRepository).save(any(Korisnik.class));
    }

    @Test
    void registerManagerShouldReturnCreated() throws Exception {
                when(korisnikRepository.existsByKorisnickoIme("menadzer_novi")).thenReturn(false);
                when(korisnikRepository.existsByEmail("nikola@firma.rs")).thenReturn(false);
                when(firmaRepository.findByPib("123456789")).thenReturn(Optional.empty());
                when(firmaRepository.findByMaticniBroj("12345678")).thenReturn(Optional.empty());
                when(firmaRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
                when(korisnikRepository.save(any(Korisnik.class))).thenAnswer(invocation -> {
                        Korisnik saved = invocation.getArgument(0);
                        saved.setId(44L);
                        return saved;
                });

        MockMultipartFile dataPart = new MockMultipartFile(
                "data",
                "",
                "application/json",
                """
                        {
                          "username": "menadzer_novi",
                          "password": "Menadzer1!",
                          "ime": "Nikola",
                          "prezime": "Ilic",
                          "telefon": "+38164123456",
                          "email": "nikola@firma.rs",
                          "firma": {
                            "naziv": "Coworking Plus",
                            "adresa": "Bulevar 1, Beograd",
                            "maticniBroj": "12345678",
                            "pib": "123456789"
                          }
                        }
                        """.getBytes()
        );

        mockMvc.perform(multipart("/api/auth/register/manager")
                        .file(dataPart)
                        .contentType(MediaType.MULTIPART_FORM_DATA_VALUE))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("menadzer_novi"))
                .andExpect(jsonPath("$.role").value("menadzer"))
                .andExpect(jsonPath("$.status").value("na_cekanju"));

        verify(korisnikRepository).save(any(Korisnik.class));
    }

    @Test
    void passwordResetFlowShouldWork() throws Exception {
        Korisnik korisnik = new Korisnik();
        korisnik.setId(12L);
        korisnik.setKorisnickoIme("marko");
        korisnik.setLozinka(passwordEncoder.encode("Marko123!"));
        korisnik.setUloga(Uloga.clan);
        korisnik.setStatus(StatusKorisnika.odobren);

        when(korisnikRepository.findByKorisnickoIme("marko")).thenReturn(Optional.of(korisnik));
        when(tokenZaResetLozinkeRepository.findByKorisnikIdAndIskoriscenFalse(12L)).thenReturn(List.of());
        when(tokenZaResetLozinkeRepository.save(any(TokenZaResetLozinke.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String requestJson = """
                {
                  "usernameOrEmail": "marko"
                }
                """;

        String response = mockMvc.perform(post("/api/auth/password-reset/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.resetUrl").exists())
                .andExpect(jsonPath("$.expiresInSeconds").value(1800))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode root = objectMapper.readTree(response);
        String resetUrl = root.get("resetUrl").asText();
        String token = resetUrl.substring(resetUrl.indexOf("token=") + 6);

        TokenZaResetLozinke tokenEntity = new TokenZaResetLozinke();
        tokenEntity.setToken(token);
        tokenEntity.setKorisnik(korisnik);
        tokenEntity.setIstice(LocalDateTime.now().plusMinutes(20));
        tokenEntity.setIskoriscen(false);

        when(tokenZaResetLozinkeRepository.findByToken(token)).thenReturn(Optional.of(tokenEntity));
        when(korisnikRepository.save(any(Korisnik.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post("/api/auth/password-reset/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + token + "\",\"newPassword\":\"Marko123!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Lozinka je uspesno promenjena"));

        verify(korisnikRepository).save(any(Korisnik.class));
                verify(tokenZaResetLozinkeRepository, times(2)).save(any(TokenZaResetLozinke.class));
    }

    @Test
    void logoutShouldReturnOk() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Uspesno izlogovan"));
    }
}
