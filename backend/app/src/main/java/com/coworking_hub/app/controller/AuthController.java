package com.coworking_hub.app.controller;

import com.coworking_hub.app.model.Firma;
import com.coworking_hub.app.model.Korisnik;
import com.coworking_hub.app.model.TokenZaResetLozinke;
import com.coworking_hub.app.model.enums.StatusKorisnika;
import com.coworking_hub.app.model.enums.Uloga;
import com.coworking_hub.app.repository.FirmaRepository;
import com.coworking_hub.app.repository.KorisnikRepository;
import com.coworking_hub.app.repository.TokenZaResetLozinkeRepository;
import com.coworking_hub.app.security.JwtService;
import com.coworking_hub.app.service.ImageStorageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final long ACCESS_TOKEN_EXPIRES_SECONDS = 3600;
    private static final long RESET_TOKEN_EXPIRES_SECONDS = 1800;
        private static final String DEFAULT_PROFILE_IMAGE = "/uploads/profiles/default-profile.png";

    private final JwtService jwtService;
        private final KorisnikRepository korisnikRepository;
        private final FirmaRepository firmaRepository;
        private final TokenZaResetLozinkeRepository tokenZaResetLozinkeRepository;
        private final ImageStorageService imageStorageService;
        private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

        public AuthController(
            JwtService jwtService,
            KorisnikRepository korisnikRepository,
            FirmaRepository firmaRepository,
            TokenZaResetLozinkeRepository tokenZaResetLozinkeRepository,
            ImageStorageService imageStorageService
        ) {
        this.jwtService = jwtService;
        this.korisnikRepository = korisnikRepository;
        this.firmaRepository = firmaRepository;
        this.tokenZaResetLozinkeRepository = tokenZaResetLozinkeRepository;
        this.imageStorageService = imageStorageService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        Optional<Korisnik> userOptional = korisnikRepository.findByKorisnickoIme(request.username());
        if (userOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Pogresni kredencijali"));
        }

        Korisnik user = userOptional.get();
        if (user.getUloga() == Uloga.admin) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Nije moguce se prijaviti kao admin preko ove stranice"));
        }

        if (!passwordEncoder.matches(request.password(), user.getLozinka())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", "Pogresni kredencijali"));
        }

        if (user.getStatus() != StatusKorisnika.odobren) {
            return ResponseEntity.status(HttpStatus.LOCKED)
                    .body(Map.of("message", "Nalog nije odobren"));
        }

        String accessToken = jwtService.generateAccessToken(
            user.getId(),
            user.getKorisnickoIme(),
            user.getUloga().name(),
            user.getStatus().name()
        );

        return ResponseEntity.ok(new LoginResponse(
                accessToken,
                "Bearer",
                ACCESS_TOKEN_EXPIRES_SECONDS,
            new LoginUserDto(user.getId(), user.getKorisnickoIme(), user.getUloga().name(), user.getStatus().name(), user.getProfilnaSlika())
        ));
    }

    @PostMapping("/admin/login")
    public ResponseEntity<?> adminLogin(@RequestBody LoginRequest request) {
        Optional<Korisnik> userOptional = korisnikRepository.findByKorisnickoIme(request.username());
        if (userOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", "Pogresni admin kredencijali"));
        }

        Korisnik user = userOptional.get();
        if (user.getUloga() != Uloga.admin) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("message", "Ovaj endpoint je samo za admin naloge"));
        }

        if (!passwordEncoder.matches(request.password(), user.getLozinka())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Pogresni admin kredencijali"));
        }

        String accessToken = jwtService.generateAccessToken(
            user.getId(),
            user.getKorisnickoIme(),
            user.getUloga().name(),
            user.getStatus().name()
        );

        return ResponseEntity.ok(new LoginResponse(
                accessToken,
                "Bearer",
                ACCESS_TOKEN_EXPIRES_SECONDS,
            new LoginUserDto(user.getId(), user.getKorisnickoIme(), user.getUloga().name(), user.getStatus().name(), user.getProfilnaSlika())
        ));
    }

    @PostMapping(path = "/register/member", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        @Transactional
    public ResponseEntity<RegistrationResponse> registerMember(
            @RequestPart("data") RegisterMemberRequest request,
            @RequestPart(value = "profileImage", required = false) MultipartFile profileImage
    ) {
        if (korisnikRepository.existsByKorisnickoIme(request.username())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new RegistrationResponse(null, request.username(), "clan", "na_cekanju", "Korisnicko ime vec postoji"));
        }

        if (korisnikRepository.existsByEmail(request.email())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new RegistrationResponse(null, request.username(), "clan", "na_cekanju", "Email vec postoji"));
        }

        if (!isPasswordValid(request.password())) {
            return ResponseEntity.badRequest()
                    .body(new RegistrationResponse(null, request.username(), "clan", "na_cekanju", "Lozinka nije u ispravnom formatu"));
        }

        Korisnik korisnik = new Korisnik();
        korisnik.setKorisnickoIme(request.username());
        korisnik.setLozinka(passwordEncoder.encode(request.password()));
        korisnik.setIme(request.ime());
        korisnik.setPrezime(request.prezime());
        korisnik.setTelefon(request.telefon());
        korisnik.setEmail(request.email());
        korisnik.setUloga(Uloga.clan);
        korisnik.setStatus(StatusKorisnika.na_cekanju);
        try {
            korisnik.setProfilnaSlika(resolveProfileImagePath(profileImage));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest()
                    .body(new RegistrationResponse(null, request.username(), "clan", "na_cekanju", ex.getMessage()));
        }
        korisnik.setKreirano(currentUtcTime());
        korisnik.setAzurirano(currentUtcTime());

        korisnik = korisnikRepository.save(korisnik);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new RegistrationResponse(korisnik.getId(), request.username(), "clan", "na_cekanju", "Zahtev za registraciju je kreiran"));
    }

    @PostMapping(path = "/register/manager", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        @Transactional
    public ResponseEntity<RegistrationResponse> registerManager(
            @RequestPart("data") RegisterManagerRequest request,
            @RequestPart(value = "profileImage", required = false) MultipartFile profileImage
    ) {
        if (korisnikRepository.existsByKorisnickoIme(request.username())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new RegistrationResponse(null, request.username(), "menadzer", "na_cekanju", "Korisnicko ime vec postoji"));
        }

        if (korisnikRepository.existsByEmail(request.email())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new RegistrationResponse(null, request.username(), "menadzer", "na_cekanju", "Email vec postoji"));
        }

        if (!isPasswordValid(request.password())) {
            return ResponseEntity.badRequest()
                    .body(new RegistrationResponse(null, request.username(), "menadzer", "na_cekanju", "Lozinka nije u ispravnom formatu"));
        }

        if (request.firma() == null) {
            return ResponseEntity.badRequest()
                .body(new RegistrationResponse(null, request.username(), "menadzer", "na_cekanju", "Podaci o firmi su obavezni"));
        }

        Firma firma;
        try {
            firma = findOrCreateFirma(request.firma());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest()
                    .body(new RegistrationResponse(null, request.username(), "menadzer", "na_cekanju", ex.getMessage()));
        }

        if (firma.getId() != null) {
            long brojMenadzera = korisnikRepository.countByFirmaIdAndUlogaAndStatusIn(
                firma.getId(),
                Uloga.menadzer,
                List.of(StatusKorisnika.na_cekanju, StatusKorisnika.odobren)
            );
            if (brojMenadzera >= 2) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new RegistrationResponse(
                    null,
                    request.username(),
                    "menadzer",
                    "na_cekanju",
                    "Firma vec ima maksimalno 2 menadzera (odobrena ili na cekanju)."
                ));
            }
        }

        Korisnik korisnik = new Korisnik();
        korisnik.setKorisnickoIme(request.username());
        korisnik.setLozinka(passwordEncoder.encode(request.password()));
        korisnik.setIme(request.ime());
        korisnik.setPrezime(request.prezime());
        korisnik.setTelefon(request.telefon());
        korisnik.setEmail(request.email());
        korisnik.setUloga(Uloga.menadzer);
        korisnik.setStatus(StatusKorisnika.na_cekanju);
        korisnik.setFirma(firma);
        try {
            korisnik.setProfilnaSlika(resolveProfileImagePath(profileImage));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest()
                    .body(new RegistrationResponse(null, request.username(), "menadzer", "na_cekanju", ex.getMessage()));
        }
        korisnik.setKreirano(currentUtcTime());
        korisnik.setAzurirano(currentUtcTime());

        korisnik = korisnikRepository.save(korisnik);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new RegistrationResponse(korisnik.getId(), request.username(), "menadzer", "na_cekanju", "Zahtev za registraciju je kreiran"));
    }

    @PostMapping("/password-reset/request")
        @Transactional
    public ResponseEntity<?> requestPasswordReset(@RequestBody PasswordResetRequestDto request) {
        Optional<Korisnik> userOptional = korisnikRepository.findByKorisnickoIme(request.usernameOrEmail());
        if (userOptional.isEmpty()) {
            userOptional = korisnikRepository.findByEmail(request.usernameOrEmail());
        }
        if (userOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Korisnik ne postoji"));
        }

        Korisnik korisnik = userOptional.get();

        List<TokenZaResetLozinke> stariTokeni = tokenZaResetLozinkeRepository
            .findByKorisnikIdAndIskoriscenFalse(korisnik.getId());
        for (TokenZaResetLozinke token : stariTokeni) {
            token.setIskoriscen(true);
        }
        tokenZaResetLozinkeRepository.saveAll(stariTokeni);

        String token = UUID.randomUUID().toString();
        LocalDateTime expiresAt = currentUtcTime().plusSeconds(RESET_TOKEN_EXPIRES_SECONDS);

        TokenZaResetLozinke resetToken = new TokenZaResetLozinke();
        resetToken.setKorisnik(korisnik);
        resetToken.setToken(token);
        resetToken.setIstice(expiresAt);
        resetToken.setIskoriscen(false);
        resetToken.setKreirano(currentUtcTime());
        tokenZaResetLozinkeRepository.save(resetToken);

        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(new PasswordResetRequestResponse(
                token,
                        expiresAt,
                        RESET_TOKEN_EXPIRES_SECONDS
                ));
    }

    @PostMapping("/password-reset/confirm")
    @Transactional
    public ResponseEntity<?> confirmPasswordReset(@RequestBody PasswordResetConfirmRequest request) {
        if (!isPasswordValid(request.newPassword())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Lozinka nije u ispravnom formatu"));
        }

        Optional<TokenZaResetLozinke> tokenDataOptional = resolveActiveResetToken(request.token());
        if (tokenDataOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.GONE)
                    .body(Map.of("message", "Token je neispravan ili je istekao"));
        }

        TokenZaResetLozinke tokenData = tokenDataOptional.get();
        Korisnik user = tokenData.getKorisnik();

        user.setLozinka(passwordEncoder.encode(request.newPassword()));
        user.setAzurirano(currentUtcTime());
        korisnikRepository.save(user);

        tokenData.setIskoriscen(true);
        tokenZaResetLozinkeRepository.save(tokenData);

        return ResponseEntity.ok(Map.of("message", "Lozinka je uspesno promenjena"));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout() {
        return ResponseEntity.ok(Map.of("message", "Uspesno izlogovan"));
    }

    private Optional<TokenZaResetLozinke> resolveActiveResetToken(String token) {
        Optional<TokenZaResetLozinke> tokenData = tokenZaResetLozinkeRepository.findByToken(token);
        if (tokenData.isEmpty()) {
            return Optional.empty();
        }

        TokenZaResetLozinke resetToken = tokenData.get();
        if (Boolean.TRUE.equals(resetToken.getIskoriscen()) || resetToken.getIstice().isBefore(currentUtcTime())) {
            return Optional.empty();
        }
        return Optional.of(resetToken);
    }

    private Firma findOrCreateFirma(FirmaRequest firmaRequest) {
        Optional<Firma> poPib = firmaRepository.findByPib(firmaRequest.pib());
        if (poPib.isPresent()) {
            Firma firma = poPib.get();
            if (!firma.getMaticniBroj().equals(firmaRequest.maticniBroj())) {
                throw new IllegalArgumentException("PIB i maticni broj firme se ne poklapaju sa postojecim podacima.");
            }
            return firma;
        }

        Optional<Firma> poMaticnom = firmaRepository.findByMaticniBroj(firmaRequest.maticniBroj());
        if (poMaticnom.isPresent()) {
            Firma firma = poMaticnom.get();
            if (!firma.getPib().equals(firmaRequest.pib())) {
                throw new IllegalArgumentException("Maticni broj i PIB firme se ne poklapaju sa postojecim podacima.");
            }
            return firma;
        }

        Firma novaFirma = new Firma();
        novaFirma.setNaziv(firmaRequest.naziv());
        novaFirma.setAdresa(firmaRequest.adresa());
        novaFirma.setMaticniBroj(firmaRequest.maticniBroj());
        novaFirma.setPib(firmaRequest.pib());
        novaFirma.setKreirano(currentUtcTime());
        novaFirma.setAzurirano(currentUtcTime());
        return firmaRepository.save(novaFirma);
    }

    private LocalDateTime currentUtcTime() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    private String resolveProfileImagePath(MultipartFile profileImage) {
        if (profileImage == null || profileImage.isEmpty()) {
            return DEFAULT_PROFILE_IMAGE;
        }
        return imageStorageService.storeProfileImage(profileImage);
    }

    private boolean isPasswordValid(String password) {
        if (password == null) {
            return false;
        }
        return password.matches("^(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9])[A-Za-z].{7,11}$");
    }

    public record LoginRequest(String username, String password) {
    }

    public record LoginResponse(String accessToken, String tokenType, long expiresIn, LoginUserDto user) {
    }

    public record LoginUserDto(Long id, String username, String role, String status, String profileImage) {
    }

    public record RegisterMemberRequest(
            String username,
            String password,
            String ime,
            String prezime,
            String telefon,
            String email
    ) {
    }

    public record RegisterManagerRequest(
            String username,
            String password,
            String ime,
            String prezime,
            String telefon,
            String email,
            FirmaRequest firma
    ) {
    }

    public record FirmaRequest(String naziv, String adresa, String maticniBroj, String pib) {
    }

    public record RegistrationResponse(Long id, String username, String role, String status, String message) {
    }

    public record PasswordResetRequestDto(String usernameOrEmail) {
    }

    public record PasswordResetRequestResponse(String token, LocalDateTime tokenExpiresAt, long expiresInSeconds) {
    }

    public record PasswordResetConfirmRequest(String token, String newPassword) {
    }
}
