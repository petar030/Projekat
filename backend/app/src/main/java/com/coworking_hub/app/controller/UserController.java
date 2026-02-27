package com.coworking_hub.app.controller;

import com.coworking_hub.app.model.Firma;
import com.coworking_hub.app.model.Korisnik;
import com.coworking_hub.app.repository.KorisnikRepository;
import com.coworking_hub.app.security.AuthenticatedUser;
import com.coworking_hub.app.security.CurrentUser;
import com.coworking_hub.app.service.ImageStorageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final KorisnikRepository korisnikRepository;
    private final ImageStorageService imageStorageService;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserController(KorisnikRepository korisnikRepository, ImageStorageService imageStorageService) {
        this.korisnikRepository = korisnikRepository;
        this.imageStorageService = imageStorageService;
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(@CurrentUser AuthenticatedUser authenticatedUser) {
        Optional<Korisnik> korisnikOptional = korisnikRepository.findById(authenticatedUser.userId());
        if (korisnikOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Korisnik nije pronadjen"));
        }

        return ResponseEntity.ok(toUserProfileResponse(korisnikOptional.get()));
    }

    @PutMapping("/me")
    @Transactional
    public ResponseEntity<?> updateProfile(
            @CurrentUser AuthenticatedUser authenticatedUser,
            @RequestBody UpdateProfileRequest request
    ) {
        Optional<Korisnik> korisnikOptional = korisnikRepository.findById(authenticatedUser.userId());
        if (korisnikOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Korisnik nije pronadjen"));
        }

        if (isBlank(request.ime()) || isBlank(request.prezime()) || isBlank(request.telefon()) || isBlank(request.email())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Ime, prezime, telefon i email su obavezni"));
        }

        Korisnik korisnik = korisnikOptional.get();
        Optional<Korisnik> postojeciPoEmailu = korisnikRepository.findByEmail(request.email().trim());
        if (postojeciPoEmailu.isPresent() && !postojeciPoEmailu.get().getId().equals(korisnik.getId())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "Email vec postoji"));
        }

        korisnik.setIme(request.ime().trim());
        korisnik.setPrezime(request.prezime().trim());
        korisnik.setTelefon(request.telefon().trim());
        korisnik.setEmail(request.email().trim());
        korisnik.setAzurirano(LocalDateTime.now());

        korisnikRepository.save(korisnik);
        return ResponseEntity.ok(toUserProfileResponse(korisnik));
    }

    @PutMapping(path = "/me/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    public ResponseEntity<?> updateProfileImage(
            @CurrentUser AuthenticatedUser authenticatedUser,
            @RequestPart("profileImage") MultipartFile profileImage
    ) {
        Optional<Korisnik> korisnikOptional = korisnikRepository.findById(authenticatedUser.userId());
        if (korisnikOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Korisnik nije pronadjen"));
        }

        String imagePath;
        try {
            imagePath = imageStorageService.storeProfileImage(profileImage);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }

        Korisnik korisnik = korisnikOptional.get();
        korisnik.setProfilnaSlika(imagePath);
        korisnik.setAzurirano(LocalDateTime.now());
        korisnikRepository.save(korisnik);

        return ResponseEntity.ok(new ProfileImageResponse(imagePath));
    }

    @PutMapping("/me/password")
    @Transactional
    public ResponseEntity<?> updatePassword(
            @CurrentUser AuthenticatedUser authenticatedUser,
            @RequestBody UpdatePasswordRequest request
    ) {
        Optional<Korisnik> korisnikOptional = korisnikRepository.findById(authenticatedUser.userId());
        if (korisnikOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Korisnik nije pronadjen"));
        }

        if (isBlank(request.newPassword())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Nova lozinka je obavezna"));
        }

        if (!isPasswordValid(request.newPassword())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Lozinka nije u ispravnom formatu"));
        }

        Korisnik korisnik = korisnikOptional.get();
        korisnik.setLozinka(passwordEncoder.encode(request.newPassword()));
        korisnik.setAzurirano(LocalDateTime.now());
        korisnikRepository.save(korisnik);

        return ResponseEntity.ok(Map.of("message", "Lozinka je uspesno promenjena"));
    }

    private UserProfileResponse toUserProfileResponse(Korisnik korisnik) {
        return new UserProfileResponse(
                korisnik.getId(),
                korisnik.getKorisnickoIme(),
                korisnik.getIme(),
                korisnik.getPrezime(),
                korisnik.getTelefon(),
                korisnik.getEmail(),
                korisnik.getProfilnaSlika(),
                korisnik.getUloga().name(),
                korisnik.getStatus().name(),
                toFirmaDto(korisnik.getFirma())
        );
    }

    private FirmaDto toFirmaDto(Firma firma) {
        if (firma == null) {
            return null;
        }
        return new FirmaDto(firma.getId(), firma.getNaziv(), firma.getAdresa(), firma.getMaticniBroj(), firma.getPib());
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private boolean isPasswordValid(String password) {
        if (password == null) {
            return false;
        }
        return password.matches("^(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9])[A-Za-z].{7,11}$");
    }

    public record UpdateProfileRequest(String ime, String prezime, String telefon, String email) {
    }

    public record UserProfileResponse(
            Long id,
            String username,
            String ime,
            String prezime,
            String telefon,
            String email,
            String profilnaSlika,
            String uloga,
            String status,
            FirmaDto firma
    ) {
    }

    public record FirmaDto(Long id, String naziv, String adresa, String maticniBroj, String pib) {
    }

    public record ProfileImageResponse(String profileImage) {
    }

    public record UpdatePasswordRequest(String newPassword) {
    }
}