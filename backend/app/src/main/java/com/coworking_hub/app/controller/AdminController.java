package com.coworking_hub.app.controller;

import com.coworking_hub.app.model.Korisnik;
import com.coworking_hub.app.model.Prostor;
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
import com.coworking_hub.app.security.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.math.BigDecimal;
import java.math.RoundingMode;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final KorisnikRepository korisnikRepository;
    private final ProstorRepository prostorRepository;
    private final ReakcijaRepository reakcijaRepository;
    private final RezervacijaRepository rezervacijaRepository;

    public AdminController(
            KorisnikRepository korisnikRepository,
            ProstorRepository prostorRepository,
            ReakcijaRepository reakcijaRepository,
            RezervacijaRepository rezervacijaRepository
    ) {
        this.korisnikRepository = korisnikRepository;
        this.prostorRepository = prostorRepository;
        this.reakcijaRepository = reakcijaRepository;
        this.rezervacijaRepository = rezervacijaRepository;
    }

    @GetMapping("/users")
    @Transactional(readOnly = true)
    public ResponseEntity<?> users(
            @CurrentUser AuthenticatedUser authenticatedUser,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "id") String sortBy,
            @RequestParam(required = false, defaultValue = "asc") String sortDir
    ) {
        ResponseEntity<?> forbidden = ensureAdmin(authenticatedUser);
        if (forbidden != null) {
            return forbidden;
        }

        Uloga parsedRoleFilter = null;
        if (!isBlank(role)) {
            try {
                parsedRoleFilter = Uloga.valueOf(role.trim().toLowerCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                return ResponseEntity.badRequest().body(Map.of("message", "Neispravan role parametar"));
            }
        }

        StatusKorisnika parsedStatusFilter = null;
        if (!isBlank(status)) {
            try {
                parsedStatusFilter = StatusKorisnika.valueOf(status.trim().toLowerCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                return ResponseEntity.badRequest().body(Map.of("message", "Neispravan status parametar"));
            }
        }

        final Uloga roleFilter = parsedRoleFilter;
        final StatusKorisnika statusFilter = parsedStatusFilter;

        String searchNormalized = isBlank(search) ? null : search.trim().toLowerCase(Locale.ROOT);
        Comparator<Korisnik> comparator = userComparator(sortBy);
        if (comparator == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Neispravan sortBy parametar"));
        }

        if ("desc".equalsIgnoreCase(sortDir)) {
            comparator = comparator.reversed();
        }

        List<AdminUserDto> content = korisnikRepository.findAll().stream()
                .filter(item -> roleFilter == null || item.getUloga() == roleFilter)
                .filter(item -> statusFilter == null || item.getStatus() == statusFilter)
                .filter(item -> matchesSearch(item, searchNormalized))
                .sorted(comparator)
                .map(item -> new AdminUserDto(
                        item.getId(),
                        item.getKorisnickoIme(),
                        item.getIme(),
                        item.getPrezime(),
                        item.getEmail(),
                        item.getTelefon(),
                        item.getUloga().name(),
                        item.getStatus().name()
                ))
                .toList();

        return ResponseEntity.ok(new AdminUsersResponse(content));
    }

    @GetMapping("/users/{userId}")
    @Transactional(readOnly = true)
    public ResponseEntity<?> userDetails(
            @CurrentUser AuthenticatedUser authenticatedUser,
            @PathVariable Long userId
    ) {
        ResponseEntity<?> forbidden = ensureAdmin(authenticatedUser);
        if (forbidden != null) {
            return forbidden;
        }

        Optional<Korisnik> userOptional = korisnikRepository.findById(userId);
        if (userOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Korisnik nije pronadjen"));
        }

        Korisnik user = userOptional.get();
        return ResponseEntity.ok(new AdminUserDetailsDto(
                user.getId(),
                user.getKorisnickoIme(),
                user.getIme(),
                user.getPrezime(),
                user.getEmail(),
                user.getTelefon(),
                user.getUloga().name(),
                user.getStatus().name(),
                user.getFirma() == null ? null : user.getFirma().getId(),
                user.getFirma() == null ? null : user.getFirma().getNaziv(),
                user.getKreirano()
        ));
    }

    @PutMapping("/users/{userId}")
    @Transactional
    public ResponseEntity<?> updateUser(
            @CurrentUser AuthenticatedUser authenticatedUser,
            @PathVariable Long userId,
            @RequestBody AdminUpdateUserRequest request
    ) {
        ResponseEntity<?> forbidden = ensureAdmin(authenticatedUser);
        if (forbidden != null) {
            return forbidden;
        }

        Optional<Korisnik> userOptional = korisnikRepository.findById(userId);
        if (userOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Korisnik nije pronadjen"));
        }

        if (request == null || isBlank(request.ime()) || isBlank(request.prezime()) || isBlank(request.telefon()) || isBlank(request.email()) || isBlank(request.status())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Ime, prezime, telefon, email i status su obavezni"));
        }

        StatusKorisnika newStatus;
        try {
            newStatus = StatusKorisnika.valueOf(request.status().trim().toLowerCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(Map.of("message", "Neispravan status"));
        }

        Korisnik user = userOptional.get();
        Optional<Korisnik> existingByEmail = korisnikRepository.findByEmail(request.email().trim());
        if (existingByEmail.isPresent() && !existingByEmail.get().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "Email vec postoji"));
        }

        user.setIme(request.ime().trim());
        user.setPrezime(request.prezime().trim());
        user.setTelefon(request.telefon().trim());
        user.setEmail(request.email().trim());
        user.setStatus(newStatus);
        user.setAzurirano(currentUtcTime());
        korisnikRepository.save(user);

        return ResponseEntity.ok(new AdminUserStatusResponse(user.getId(), user.getStatus().name()));
    }

    @DeleteMapping("/users/{userId}")
    @Transactional
    public ResponseEntity<?> deleteUser(
            @CurrentUser AuthenticatedUser authenticatedUser,
            @PathVariable Long userId
    ) {
        ResponseEntity<?> forbidden = ensureAdmin(authenticatedUser);
        if (forbidden != null) {
            return forbidden;
        }

        Optional<Korisnik> userOptional = korisnikRepository.findById(userId);
        if (userOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Korisnik nije pronadjen"));
        }

        korisnikRepository.delete(userOptional.get());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/registration-requests")
    @Transactional(readOnly = true)
    public ResponseEntity<?> registrationRequests(@CurrentUser AuthenticatedUser authenticatedUser) {
        ResponseEntity<?> forbidden = ensureAdmin(authenticatedUser);
        if (forbidden != null) {
            return forbidden;
        }

        List<RegistrationRequestDto> requests = korisnikRepository.findAll().stream()
                .filter(item -> item.getStatus() == StatusKorisnika.na_cekanju)
                .filter(item -> item.getUloga() == Uloga.clan || item.getUloga() == Uloga.menadzer)
                .sorted(Comparator.comparing(Korisnik::getKreirano, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .map(item -> new RegistrationRequestDto(
                        item.getId(),
                        item.getKorisnickoIme(),
                        item.getUloga().name(),
                        item.getStatus().name(),
                        item.getKreirano()
                ))
                .toList();

        return ResponseEntity.ok(new RegistrationRequestsResponse(requests));
    }

    @PatchMapping("/registration-requests/{userId}/approve")
    @Transactional
    public ResponseEntity<?> approveRegistrationRequest(
            @CurrentUser AuthenticatedUser authenticatedUser,
            @PathVariable Long userId
    ) {
        ResponseEntity<?> forbidden = ensureAdmin(authenticatedUser);
        if (forbidden != null) {
            return forbidden;
        }

        Optional<Korisnik> userOptional = korisnikRepository.findById(userId);
        if (userOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Korisnik nije pronadjen"));
        }

        Korisnik user = userOptional.get();
        if (user.getStatus() != StatusKorisnika.na_cekanju) {
            return ResponseEntity.status(HttpStatusCode.valueOf(422)).body(Map.of("message", "Zahtev nije u statusu na_cekanju"));
        }

        user.setStatus(StatusKorisnika.odobren);
        user.setAzurirano(currentUtcTime());
        korisnikRepository.save(user);

        return ResponseEntity.ok(new AdminUserStatusResponse(user.getId(), user.getStatus().name()));
    }

    @PatchMapping("/registration-requests/{userId}/reject")
    @Transactional
    public ResponseEntity<?> rejectRegistrationRequest(
            @CurrentUser AuthenticatedUser authenticatedUser,
            @PathVariable Long userId,
            @RequestBody(required = false) RejectReasonRequest request
    ) {
        ResponseEntity<?> forbidden = ensureAdmin(authenticatedUser);
        if (forbidden != null) {
            return forbidden;
        }

        Optional<Korisnik> userOptional = korisnikRepository.findById(userId);
        if (userOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Korisnik nije pronadjen"));
        }

        Korisnik user = userOptional.get();
        if (user.getStatus() != StatusKorisnika.na_cekanju) {
            return ResponseEntity.status(HttpStatusCode.valueOf(422)).body(Map.of("message", "Zahtev nije u statusu na_cekanju"));
        }

        user.setStatus(StatusKorisnika.odbijen);
        user.setAzurirano(currentUtcTime());
        korisnikRepository.save(user);

        return ResponseEntity.ok(new AdminUserStatusResponse(user.getId(), user.getStatus().name()));
    }

    @GetMapping("/space-requests")
    @Transactional(readOnly = true)
    public ResponseEntity<?> spaceRequests(@CurrentUser AuthenticatedUser authenticatedUser) {
        ResponseEntity<?> forbidden = ensureAdmin(authenticatedUser);
        if (forbidden != null) {
            return forbidden;
        }

        List<SpaceRequestDto> requests = prostorRepository.findAll().stream()
                .filter(item -> item.getStatus() == StatusProstora.na_cekanju)
                .sorted(Comparator.comparing(Prostor::getKreirano, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .map(item -> new SpaceRequestDto(
                        item.getId(),
                        item.getNaziv(),
                        item.getGrad(),
                        item.getFirma() == null ? "" : item.getFirma().getNaziv(),
                        item.getStatus().name()
                ))
                .toList();

        return ResponseEntity.ok(new SpaceRequestsResponse(requests));
    }

    @PatchMapping("/space-requests/{spaceId}/approve")
    @Transactional
    public ResponseEntity<?> approveSpaceRequest(
            @CurrentUser AuthenticatedUser authenticatedUser,
            @PathVariable Long spaceId
    ) {
        ResponseEntity<?> forbidden = ensureAdmin(authenticatedUser);
        if (forbidden != null) {
            return forbidden;
        }

        Optional<Prostor> spaceOptional = prostorRepository.findById(spaceId);
        if (spaceOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Prostor nije pronadjen"));
        }

        Prostor space = spaceOptional.get();
        if (space.getStatus() != StatusProstora.na_cekanju) {
            return ResponseEntity.status(HttpStatusCode.valueOf(422)).body(Map.of("message", "Zahtev nije u statusu na_cekanju"));
        }

        space.setStatus(StatusProstora.odobren);
        space.setAzurirano(currentUtcTime());
        prostorRepository.save(space);

        return ResponseEntity.ok(new SpaceStatusResponse(space.getId(), space.getStatus().name()));
    }

    @PatchMapping("/space-requests/{spaceId}/reject")
    @Transactional
    public ResponseEntity<?> rejectSpaceRequest(
            @CurrentUser AuthenticatedUser authenticatedUser,
            @PathVariable Long spaceId,
            @RequestBody(required = false) RejectReasonRequest request
    ) {
        ResponseEntity<?> forbidden = ensureAdmin(authenticatedUser);
        if (forbidden != null) {
            return forbidden;
        }

        Optional<Prostor> spaceOptional = prostorRepository.findById(spaceId);
        if (spaceOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Prostor nije pronadjen"));
        }

        Prostor space = spaceOptional.get();
        if (space.getStatus() != StatusProstora.na_cekanju) {
            return ResponseEntity.status(HttpStatusCode.valueOf(422)).body(Map.of("message", "Zahtev nije u statusu na_cekanju"));
        }

        space.setStatus(StatusProstora.odbijen);
        space.setAzurirano(currentUtcTime());
        prostorRepository.save(space);

        return ResponseEntity.ok(new SpaceStatusResponse(space.getId(), space.getStatus().name()));
    }


        @GetMapping("/stats/spaces")
        @Transactional(readOnly = true)
        public ResponseEntity<?> statsSpaces(
            @CurrentUser AuthenticatedUser authenticatedUser
        ) {
            ResponseEntity<?> forbidden = ensureAdmin(authenticatedUser);
            if (forbidden != null) {
            return forbidden;
            }

            List<AdminStatSpaceDto> spaces = prostorRepository.findAll().stream()
                .filter(space -> space.getStatus() == StatusProstora.odobren)
                .sorted(Comparator.comparing(Prostor::getNaziv, Comparator.nullsLast(String::compareToIgnoreCase)))
                .map(space -> new AdminStatSpaceDto(space.getId(), space.getNaziv()))
                .toList();

            return ResponseEntity.ok(new AdminStatSpacesResponse(spaces));
        }

        @GetMapping("/stats/space-monthly")
        @Transactional(readOnly = true)
        public ResponseEntity<?> spaceMonthlyStats(
            @CurrentUser AuthenticatedUser authenticatedUser,
            @RequestParam Long spaceId,
            @RequestParam Integer year
        ) {
            ResponseEntity<?> forbidden = ensureAdmin(authenticatedUser);
            if (forbidden != null) {
            return forbidden;
            }

            if (spaceId == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "spaceId je obavezan"));
            }

            if (year == null || year < 2000 || year > 2100) {
            return ResponseEntity.badRequest().body(Map.of("message", "Neispravna godina"));
            }

            Optional<Prostor> spaceOptional = prostorRepository.findById(spaceId);
            if (spaceOptional.isEmpty() || spaceOptional.get().getStatus() != StatusProstora.odobren) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Prostor nije pronadjen"));
            }

            Prostor space = spaceOptional.get();

            List<SpaceMonthlyItemDto> items = java.util.stream.IntStream.rangeClosed(1, 12)
                .mapToObj(month -> {
                LocalDateTime monthStart = LocalDateTime.of(year, month, 1, 0, 0);
                LocalDateTime monthEnd = monthStart.plusMonths(1);

                long likes = reakcijaRepository.findAll().stream()
                    .filter(reaction -> reaction.getProstor() != null && spaceId.equals(reaction.getProstor().getId()))
                    .filter(reaction -> reaction.getKreirano() != null && !reaction.getKreirano().isBefore(monthStart) && reaction.getKreirano().isBefore(monthEnd))
                    .filter(reaction -> reaction.getTip() == TipReakcije.svidjanje)
                    .count();

                long dislikes = reakcijaRepository.findAll().stream()
                    .filter(reaction -> reaction.getProstor() != null && spaceId.equals(reaction.getProstor().getId()))
                    .filter(reaction -> reaction.getKreirano() != null && !reaction.getKreirano().isBefore(monthStart) && reaction.getKreirano().isBefore(monthEnd))
                    .filter(reaction -> reaction.getTip() == TipReakcije.nesvidjanje)
                    .count();

                long reservations = rezervacijaRepository.findAll().stream()
                    .filter(reservation -> reservation.getProstor() != null && spaceId.equals(reservation.getProstor().getId()))
                    .filter(reservation -> reservation.getStatus() != StatusRezervacije.otkazana)
                    .filter(reservation -> reservation.getDatumOd() != null && !reservation.getDatumOd().isBefore(monthStart) && reservation.getDatumOd().isBefore(monthEnd))
                    .count();

                BigDecimal revenue = rezervacijaRepository.findAll().stream()
                    .filter(reservation -> reservation.getProstor() != null && spaceId.equals(reservation.getProstor().getId()))
                    .filter(reservation -> reservation.getStatus() == StatusRezervacije.aktivna || reservation.getStatus() == StatusRezervacije.potvrdjena)
                    .map(reservation -> reservationRevenueInRange(reservation, monthStart, monthEnd, space.getCenaPoSatu()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(2, RoundingMode.HALF_UP);

                return new SpaceMonthlyItemDto(month, likes, dislikes, reservations, revenue, "RSD");
                })
                .toList();

            return ResponseEntity.ok(new SpaceMonthlyStatsResponse(space.getId(), space.getNaziv(), year, items));
        }

    private ResponseEntity<?> ensureAdmin(AuthenticatedUser authenticatedUser) {
        if (authenticatedUser == null || authenticatedUser.role() == null || !"admin".equals(authenticatedUser.role())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Samo admin ima pristup"));
        }
        return null;
    }

    private Comparator<Korisnik> userComparator(String sortBy) {
        String normalized = sortBy == null ? "id" : sortBy.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "id" -> Comparator.comparing(Korisnik::getId, Comparator.nullsLast(Comparator.naturalOrder()));
            case "username" -> Comparator.comparing(Korisnik::getKorisnickoIme, Comparator.nullsLast(String::compareToIgnoreCase));
            case "ime" -> Comparator.comparing(Korisnik::getIme, Comparator.nullsLast(String::compareToIgnoreCase));
            case "prezime" -> Comparator.comparing(Korisnik::getPrezime, Comparator.nullsLast(String::compareToIgnoreCase));
            case "email" -> Comparator.comparing(Korisnik::getEmail, Comparator.nullsLast(String::compareToIgnoreCase));
            case "telefon" -> Comparator.comparing(Korisnik::getTelefon, Comparator.nullsLast(String::compareToIgnoreCase));
            case "role" -> Comparator.comparing(item -> item.getUloga() == null ? "" : item.getUloga().name(), String::compareToIgnoreCase);
            case "status" -> Comparator.comparing(item -> item.getStatus() == null ? "" : item.getStatus().name(), String::compareToIgnoreCase);
            case "createdat" -> Comparator.comparing(Korisnik::getKreirano, Comparator.nullsLast(Comparator.naturalOrder()));
            default -> null;
        };
    }

    private boolean matchesSearch(Korisnik user, String searchNormalized) {
        if (searchNormalized == null) {
            return true;
        }

        return contains(user.getKorisnickoIme(), searchNormalized)
                || contains(user.getIme(), searchNormalized)
                || contains(user.getPrezime(), searchNormalized)
                || contains(user.getEmail(), searchNormalized)
                || contains(user.getTelefon(), searchNormalized);
    }

    private boolean contains(String value, String searchNormalized) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(searchNormalized);
    }

    private BigDecimal reservationRevenueInRange(Rezervacija reservation, LocalDateTime from, LocalDateTime to, BigDecimal hourlyPrice) {
        if (reservation.getDatumOd() == null || reservation.getDatumDo() == null || hourlyPrice == null || !reservation.getDatumDo().isAfter(reservation.getDatumOd())) {
            return BigDecimal.ZERO;
        }

        LocalDateTime effectiveStart = reservation.getDatumOd().isAfter(from) ? reservation.getDatumOd() : from;
        LocalDateTime effectiveEnd = reservation.getDatumDo().isBefore(to) ? reservation.getDatumDo() : to;

        if (!effectiveEnd.isAfter(effectiveStart)) {
            return BigDecimal.ZERO;
        }

        long minutes = java.time.Duration.between(effectiveStart, effectiveEnd).toMinutes();
        BigDecimal hours = BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 4, RoundingMode.HALF_UP);
        return hourlyPrice.multiply(hours);
    }

    private LocalDateTime currentUtcTime() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public record AdminUsersResponse(List<AdminUserDto> content) {
    }

    public record AdminUserDto(
            Long id,
            String username,
            String ime,
            String prezime,
            String email,
            String telefon,
            String role,
            String status
    ) {
    }

    public record AdminUserDetailsDto(
            Long id,
            String username,
            String ime,
            String prezime,
            String email,
            String telefon,
            String role,
            String status,
            Long firmaId,
            String firmaNaziv,
            LocalDateTime createdAt
    ) {
    }

    public record AdminUpdateUserRequest(
            String ime,
            String prezime,
            String telefon,
            String email,
            String status
    ) {
    }

    public record AdminUserStatusResponse(Long userId, String status) {
    }

    public record RegistrationRequestsResponse(List<RegistrationRequestDto> requests) {
    }

    public record RegistrationRequestDto(
            Long userId,
            String username,
            String role,
            String status,
            LocalDateTime createdAt
    ) {
    }

    public record SpaceRequestsResponse(List<SpaceRequestDto> requests) {
    }

    public record SpaceRequestDto(
            Long spaceId,
            String naziv,
            String grad,
            String firmaNaziv,
            String status
    ) {
    }

    public record SpaceStatusResponse(Long spaceId, String status) {
    }

    public record RejectReasonRequest(String reason) {
    }

        public record AdminStatSpacesResponse(List<AdminStatSpaceDto> spaces) {
        }

        public record AdminStatSpaceDto(Long spaceId, String spaceName) {
        }

        public record SpaceMonthlyStatsResponse(
            Long spaceId,
            String spaceName,
            Integer year,
            List<SpaceMonthlyItemDto> items
        ) {
        }

        public record SpaceMonthlyItemDto(
            Integer month,
            Long likes,
            Long dislikes,
            Long reservations,
            BigDecimal revenue,
            String currency
        ) {
        }
}
