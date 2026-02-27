package com.coworking_hub.app.controller;

import com.coworking_hub.app.model.Korisnik;
import com.coworking_hub.app.model.enums.StatusKorisnika;
import com.coworking_hub.app.model.enums.Uloga;
import com.coworking_hub.app.repository.KorisnikRepository;
import com.coworking_hub.app.security.AuthenticatedUser;
import com.coworking_hub.app.security.CurrentUserArgumentResolver;
import com.coworking_hub.app.security.JwtAuthInterceptor;
import com.coworking_hub.app.service.ImageStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    private MockMvc mockMvc;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Mock
    private KorisnikRepository korisnikRepository;

    @Mock
    private ImageStorageService imageStorageService;

    @BeforeEach
    void setUp() {
        UserController userController = new UserController(korisnikRepository, imageStorageService);
        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setCustomArgumentResolvers(new CurrentUserArgumentResolver())
                .build();
    }

    @Test
    void meShouldReturnCurrentUserProfile() throws Exception {
        Korisnik korisnik = buildUser(12L, "marko", "marko@example.com");
        when(korisnikRepository.findById(12L)).thenReturn(Optional.of(korisnik));

        mockMvc.perform(get("/api/users/me")
                        .requestAttr(JwtAuthInterceptor.AUTH_USER_ATTR, authenticatedUser(12L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(12))
                .andExpect(jsonPath("$.username").value("marko"))
                .andExpect(jsonPath("$.uloga").value("clan"))
                .andExpect(jsonPath("$.profilnaSlika").value("/uploads/profiles/marko.png"));
    }

    @Test
    void meShouldReturnNotFoundWhenUserIsMissing() throws Exception {
        when(korisnikRepository.findById(404L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/users/me")
                        .requestAttr(JwtAuthInterceptor.AUTH_USER_ATTR, authenticatedUser(404L)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Korisnik nije pronadjen"));
    }

    @Test
    void updateProfileShouldReturnUpdatedUser() throws Exception {
        Korisnik korisnik = buildUser(12L, "marko", "marko@example.com");
        when(korisnikRepository.findById(12L)).thenReturn(Optional.of(korisnik));
        when(korisnikRepository.findByEmail("marko_new@example.com")).thenReturn(Optional.empty());

        String requestJson = """
                {
                  "ime": "Marko",
                  "prezime": "Markovic",
                  "telefon": "+38160123456",
                  "email": "marko_new@example.com"
                }
                """;

        mockMvc.perform(put("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                        .requestAttr(JwtAuthInterceptor.AUTH_USER_ATTR, authenticatedUser(12L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("marko_new@example.com"))
                .andExpect(jsonPath("$.ime").value("Marko"))
                .andExpect(jsonPath("$.prezime").value("Markovic"));

        verify(korisnikRepository).save(eq(korisnik));
    }

    @Test
    void updateProfileShouldReturnBadRequestWhenRequiredFieldsMissing() throws Exception {
        Korisnik korisnik = buildUser(12L, "marko", "marko@example.com");
        when(korisnikRepository.findById(12L)).thenReturn(Optional.of(korisnik));

        String requestJson = """
                {
                  "ime": "",
                  "prezime": "Markovic",
                  "telefon": "+38160123456",
                  "email": "marko_new@example.com"
                }
                """;

        mockMvc.perform(put("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                        .requestAttr(JwtAuthInterceptor.AUTH_USER_ATTR, authenticatedUser(12L)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Ime, prezime, telefon i email su obavezni"));

        verify(korisnikRepository, never()).save(any(Korisnik.class));
    }

    @Test
    void updateProfileShouldReturnConflictWhenEmailExists() throws Exception {
        Korisnik korisnik = buildUser(12L, "marko", "marko@example.com");
        Korisnik drugi = buildUser(99L, "pera", "marko_new@example.com");

        when(korisnikRepository.findById(12L)).thenReturn(Optional.of(korisnik));
        when(korisnikRepository.findByEmail("marko_new@example.com")).thenReturn(Optional.of(drugi));

        String requestJson = """
                {
                  "ime": "Marko",
                  "prezime": "Markovic",
                  "telefon": "+38160123456",
                  "email": "marko_new@example.com"
                }
                """;

        mockMvc.perform(put("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                        .requestAttr(JwtAuthInterceptor.AUTH_USER_ATTR, authenticatedUser(12L)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Email vec postoji"));

        verify(korisnikRepository, never()).save(any(Korisnik.class));
    }

    @Test
    void updateProfileImageShouldReturnNewImagePath() throws Exception {
        Korisnik korisnik = buildUser(12L, "marko", "marko@example.com");
        when(korisnikRepository.findById(12L)).thenReturn(Optional.of(korisnik));
        when(imageStorageService.storeProfileImage(any())).thenReturn("/uploads/profiles/marko_new.png");

        MockMultipartFile profileImage = new MockMultipartFile(
                "profileImage",
                "marko.png",
                "image/png",
                "fake-image-content".getBytes()
        );

        mockMvc.perform(multipart("/api/users/me/profile-image")
                        .file(profileImage)
                        .with(putMethod())
                        .requestAttr(JwtAuthInterceptor.AUTH_USER_ATTR, authenticatedUser(12L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profileImage").value("/uploads/profiles/marko_new.png"));

        verify(korisnikRepository).save(eq(korisnik));
    }

    @Test
    void updateProfileImageShouldReturnBadRequestForInvalidImage() throws Exception {
        Korisnik korisnik = buildUser(12L, "marko", "marko@example.com");
        when(korisnikRepository.findById(12L)).thenReturn(Optional.of(korisnik));
        when(imageStorageService.storeProfileImage(any())).thenThrow(new IllegalArgumentException("Dozvoljeni formati su JPG i PNG."));

        MockMultipartFile profileImage = new MockMultipartFile(
                "profileImage",
                "marko.gif",
                "image/gif",
                "fake-image-content".getBytes()
        );

        mockMvc.perform(multipart("/api/users/me/profile-image")
                        .file(profileImage)
                        .with(putMethod())
                        .requestAttr(JwtAuthInterceptor.AUTH_USER_ATTR, authenticatedUser(12L)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Dozvoljeni formati su JPG i PNG."));

        verify(korisnikRepository, never()).save(any(Korisnik.class));
    }

        @Test
        void updatePasswordShouldReturnOk() throws Exception {
        Korisnik korisnik = buildUser(12L, "marko", "marko@example.com");
        korisnik.setLozinka(passwordEncoder.encode("Stara123!"));
        when(korisnikRepository.findById(12L)).thenReturn(Optional.of(korisnik));

        mockMvc.perform(put("/api/users/me/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "newPassword": "Nova123!"
                    }
                    """)
                .requestAttr(JwtAuthInterceptor.AUTH_USER_ATTR, authenticatedUser(12L)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Lozinka je uspesno promenjena"));

        verify(korisnikRepository).save(eq(korisnik));
        }

        @Test
        void updatePasswordShouldRejectInvalidFormat() throws Exception {
        Korisnik korisnik = buildUser(12L, "marko", "marko@example.com");
        when(korisnikRepository.findById(12L)).thenReturn(Optional.of(korisnik));

        mockMvc.perform(put("/api/users/me/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "newPassword": "slaba"
                    }
                    """)
                .requestAttr(JwtAuthInterceptor.AUTH_USER_ATTR, authenticatedUser(12L)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Lozinka nije u ispravnom formatu"));

        verify(korisnikRepository, never()).save(any(Korisnik.class));
        }

    private Korisnik buildUser(Long id, String username, String email) {
        Korisnik korisnik = new Korisnik();
        korisnik.setId(id);
        korisnik.setKorisnickoIme(username);
        korisnik.setIme("Marko");
        korisnik.setPrezime("Markovic");
        korisnik.setTelefon("+38160123456");
        korisnik.setEmail(email);
        korisnik.setProfilnaSlika("/uploads/profiles/marko.png");
        korisnik.setUloga(Uloga.clan);
        korisnik.setStatus(StatusKorisnika.odobren);
        return korisnik;
    }

    private AuthenticatedUser authenticatedUser(Long userId) {
        return new AuthenticatedUser(userId, "marko", "clan", "odobren");
    }

    private RequestPostProcessor putMethod() {
        return request -> {
            request.setMethod("PUT");
            return request;
        };
    }
}