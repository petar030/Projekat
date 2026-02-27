package com.coworking_hub.app.controller;

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
import com.coworking_hub.app.security.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/member")
public class MemberController {

    private final ProstorRepository prostorRepository;
    private final ReakcijaRepository reakcijaRepository;
    private final OtvoreniProstorRepository otvoreniProstorRepository;
    private final KancelarijaRepository kancelarijaRepository;
    private final KonferencijskaSalaRepository konferencijskaSalaRepository;
    private final RezervacijaRepository rezervacijaRepository;

    public MemberController(
        ProstorRepository prostorRepository,
        ReakcijaRepository reakcijaRepository,
        OtvoreniProstorRepository otvoreniProstorRepository,
        KancelarijaRepository kancelarijaRepository,
        KonferencijskaSalaRepository konferencijskaSalaRepository,
        RezervacijaRepository rezervacijaRepository
    ) {
    this.prostorRepository = prostorRepository;
    this.reakcijaRepository = reakcijaRepository;
    this.otvoreniProstorRepository = otvoreniProstorRepository;
    this.kancelarijaRepository = kancelarijaRepository;
    this.konferencijskaSalaRepository = konferencijskaSalaRepository;
        this.rezervacijaRepository = rezervacijaRepository;
    }

    @GetMapping("/spaces")
    @Transactional(readOnly = true)
    public ResponseEntity<?> searchSpaces(
        @CurrentUser AuthenticatedUser authenticatedUser,
        @RequestParam(required = false) String name,
        @RequestParam(required = false) List<String> cities,
        @RequestParam String type,
        @RequestParam(required = false) Integer officeMinDesks
    ) {
    if (authenticatedUser == null) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    String normalizedType = type == null ? "" : type.toLowerCase(Locale.ROOT);
    if (!List.of("otvoreni", "kancelarija", "sala").contains(normalizedType)) {
        return ResponseEntity.badRequest().body(Map.of("message", "Type mora biti otvoreni, kancelarija ili sala"));
    }

    if (!"kancelarija".equals(normalizedType) && officeMinDesks != null) {
        return ResponseEntity.badRequest().body(Map.of("message", "officeMinDesks je dozvoljen samo za kancelarija tip"));
    }

    List<String> cityFilters = cities == null ? List.of() : cities;
    List<Prostor> spaces = prostorRepository.searchApproved(
        StatusProstora.odobren,
        name,
        cityFilters,
        !cityFilters.isEmpty()
    );

    List<Long> spaceIds = spaces.stream().map(Prostor::getId).toList();
    List<OtvoreniProstor> openSpaces = otvoreniProstorRepository.findByProstorIdIn(spaceIds);
    Map<Long, Integer> openDesksBySpace = openSpaces.stream()
        .collect(Collectors.toMap(item -> item.getProstor().getId(), OtvoreniProstor::getBrojStolova, (first, second) -> first));
    Map<Long, List<OtvoreniProstor>> openSpacesBySpace = openSpaces.stream()
        .collect(Collectors.groupingBy(item -> item.getProstor().getId()));

    Map<Long, List<Kancelarija>> officesBySpace = kancelarijaRepository.findByProstorIdIn(spaceIds).stream()
        .collect(Collectors.groupingBy(item -> item.getProstor().getId()));

    Map<Long, List<KonferencijskaSala>> roomsBySpace = konferencijskaSalaRepository.findByProstorIdIn(spaceIds).stream()
        .collect(Collectors.groupingBy(item -> item.getProstor().getId()));

    Map<Long, ReactionCounters> countersBySpace = buildReactionCounters(spaceIds);

    List<MemberSearchItemDto> content = spaces.stream()
        .map(space -> new SpaceWithMatchingIds(space, matchingSubspaceIds(
            space.getId(),
            normalizedType,
            officeMinDesks,
            openSpacesBySpace,
            officesBySpace,
            roomsBySpace
        )))
        .filter(item -> !item.matchingSubspaceIds().isEmpty())
        .map(item -> {
            Prostor space = item.space();
            Long spaceId = space.getId();
            ReactionCounters counters = countersBySpace.getOrDefault(spaceId, ReactionCounters.ZERO);
            int officeCount = officesBySpace.getOrDefault(spaceId, List.of()).size();
            Integer maxOfficeDesks = officesBySpace.getOrDefault(spaceId, List.of()).stream()
                .map(Kancelarija::getBrojStolova)
                .max(Integer::compareTo)
                .orElse(null);
            int meetingRoomCount = roomsBySpace.getOrDefault(spaceId, List.of()).size();

            return new MemberSearchItemDto(
                space.getId(),
                space.getNaziv(),
                space.getGrad(),
                space.getAdresa(),
                space.getFirma().getNaziv(),
                counters.likes(),
                counters.dislikes(),
                openDesksBySpace.get(spaceId),
                officeCount,
                maxOfficeDesks,
                meetingRoomCount,
                item.matchingSubspaceIds()
            );
        })
        .toList();

    return ResponseEntity.ok(new MemberSearchResponse(content));
    }

    @GetMapping("/reservations")
    @Transactional(readOnly = true)
    public ResponseEntity<MemberReservationsResponse> reservations(
            @CurrentUser AuthenticatedUser authenticatedUser
    ) {
        List<Rezervacija> rezervacije = rezervacijaRepository.findByClanIdOrderByDatumOdDesc(authenticatedUser.userId());

        List<MemberReservationItemDto> content = rezervacije.stream()
                .map(this::toDto)
                .toList();

        return ResponseEntity.ok(new MemberReservationsResponse(content));
    }

    @PostMapping("/spaces/{spaceId}/availability")
    @Transactional(readOnly = true)
    public ResponseEntity<?> availability(
            @CurrentUser AuthenticatedUser authenticatedUser,
            @PathVariable Long spaceId,
            @RequestBody MemberAvailabilityRequest request
    ) {
        if (authenticatedUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (request == null || request.type() == null || request.weekStart() == null || request.resourceIds() == null || request.resourceIds().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Neispravan request: type, weekStart i resourceIds su obavezni"));
        }

        String normalizedType = request.type().toLowerCase(Locale.ROOT);
        if (!List.of("otvoreni", "kancelarija", "sala").contains(normalizedType)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Type mora biti otvoreni, kancelarija ili sala"));
        }

        List<Long> requestedResourceIds = request.resourceIds().stream()
                .filter(resourceId -> resourceId != null && resourceId > 0)
                .distinct()
                .toList();
        if (requestedResourceIds.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "resourceIds mora sadrzati bar jedan validan ID"));
        }

        Map<Long, String> resourceNamesById = resolveResourceNamesForSpace(spaceId, normalizedType);
        if (resourceNamesById.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Za dati prostor i tip ne postoje podprostori"));
        }

        boolean hasInvalidIds = requestedResourceIds.stream().anyMatch(resourceId -> !resourceNamesById.containsKey(resourceId));
        if (hasInvalidIds) {
            return ResponseEntity.badRequest().body(Map.of("message", "Prosledjen je resourceId koji ne pripada trazenom prostoru/tipu"));
        }

        LocalDateTime weekStartAt = request.weekStart().atStartOfDay();
        LocalDateTime weekEndAt = weekStartAt.plusDays(7);

        List<Rezervacija> reservations = rezervacijaRepository
                .findByProstorIdAndStatusNotAndDatumDoGreaterThanAndDatumOdLessThan(
                        spaceId,
                        StatusRezervacije.otkazana,
                        weekStartAt,
                        weekEndAt
                );

        List<ResourceAvailabilityDto> resources = requestedResourceIds.stream()
                .map(resourceId -> {
                    List<BusySlotDto> busySlots = reservations.stream()
                            .filter(reservation -> resourceId.equals(reservationResourceId(normalizedType, reservation)))
                            .map(reservation -> new BusySlotDto(reservation.getDatumOd(), reservation.getDatumDo()))
                            .sorted(Comparator.comparing(BusySlotDto::from))
                            .toList();
                        return new ResourceAvailabilityDto(resourceId, resourceNamesById.get(resourceId), busySlots);
                })
                .toList();

        return ResponseEntity.ok(new MemberAvailabilityResponse(spaceId, normalizedType, request.weekStart(), resources));
    }

    @PatchMapping("/reservations/{reservationId}/cancel")
    public ResponseEntity<?> cancelReservation(
            @CurrentUser AuthenticatedUser authenticatedUser,
            @PathVariable Long reservationId
    ) {
        Optional<Rezervacija> rezervacijaOptional = rezervacijaRepository.findByIdAndClanId(reservationId, authenticatedUser.userId());
        if (rezervacijaOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Rezervacija nije pronadjena"));
        }

        Rezervacija rezervacija = rezervacijaOptional.get();
        if (!isCancellable(rezervacija)) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(Map.of("message", "Rezervaciju nije moguce otkazati manje od 12h pre pocetka ili ako nije aktivna"));
        }

        rezervacija.setStatus(StatusRezervacije.otkazana);
        rezervacija.setAzurirano(LocalDateTime.now());
        rezervacijaRepository.save(rezervacija);

        return ResponseEntity.ok(new CancelReservationResponse(rezervacija.getId(), rezervacija.getStatus().name()));
    }

    private MemberReservationItemDto toDto(Rezervacija rezervacija) {
        return new MemberReservationItemDto(
                rezervacija.getId(),
                rezervacija.getProstor().getId(),
                rezervacija.getProstor().getNaziv(),
                rezervacija.getProstor().getGrad(),
                rezervacija.getDatumOd(),
                rezervacija.getDatumDo(),
                rezervacija.getStatus().name(),
                isCancellable(rezervacija)
        );
    }

    private boolean isCancellable(Rezervacija rezervacija) {
        if (rezervacija.getStatus() != StatusRezervacije.aktivna) {
            return false;
        }

        LocalDateTime cancellationDeadline = rezervacija.getDatumOd().minusHours(12);
        return LocalDateTime.now().isBefore(cancellationDeadline);
    }

    private List<Long> matchingSubspaceIds(
            Long spaceId,
            String type,
            Integer officeMinDesks,
            Map<Long, List<OtvoreniProstor>> openSpacesBySpace,
            Map<Long, List<Kancelarija>> officesBySpace,
            Map<Long, List<KonferencijskaSala>> roomsBySpace
    ) {
        if ("otvoreni".equals(type)) {
            return openSpacesBySpace.getOrDefault(spaceId, List.of()).stream()
                    .map(OtvoreniProstor::getId)
                    .toList();
        }

        if ("kancelarija".equals(type)) {
            List<Kancelarija> offices = officesBySpace.getOrDefault(spaceId, List.of());
            if (offices.isEmpty()) {
                return List.of();
            }
            if (officeMinDesks == null) {
                return offices.stream().map(Kancelarija::getId).toList();
            }
            return offices.stream()
                    .filter(office -> office.getBrojStolova() >= officeMinDesks)
                    .map(Kancelarija::getId)
                    .toList();
        }

        return roomsBySpace.getOrDefault(spaceId, List.of()).stream()
                .map(KonferencijskaSala::getId)
                .toList();
    }

        private Map<Long, String> resolveResourceNamesForSpace(Long spaceId, String type) {
        if ("otvoreni".equals(type)) {
            return otvoreniProstorRepository.findByProstorIdIn(List.of(spaceId)).stream()
                .collect(Collectors.toMap(
                    OtvoreniProstor::getId,
                    item -> "Otvoreni prostor",
                    (first, second) -> first,
                    java.util.LinkedHashMap::new
                ));
        }

        if ("kancelarija".equals(type)) {
            return kancelarijaRepository.findByProstorIdIn(List.of(spaceId)).stream()
                .collect(Collectors.toMap(
                    Kancelarija::getId,
                    Kancelarija::getNaziv,
                    (first, second) -> first,
                    java.util.LinkedHashMap::new
                ));
        }

        return konferencijskaSalaRepository.findByProstorIdIn(List.of(spaceId)).stream()
            .collect(Collectors.toMap(
                KonferencijskaSala::getId,
                KonferencijskaSala::getNaziv,
                (first, second) -> first,
                java.util.LinkedHashMap::new
            ));
    }

    private Long reservationResourceId(String type, Rezervacija reservation) {
        if ("otvoreni".equals(type)) {
            return reservation.getOtvoreniProstor() == null ? null : reservation.getOtvoreniProstor().getId();
        }

        if ("kancelarija".equals(type)) {
            return reservation.getKancelarija() == null ? null : reservation.getKancelarija().getId();
        }

        return reservation.getSala() == null ? null : reservation.getSala().getId();
    }

    private Map<Long, ReactionCounters> buildReactionCounters(Collection<Long> spaceIds) {
        if (spaceIds.isEmpty()) {
            return Map.of();
        }

        return reakcijaRepository.countGroupedByProstorIds(spaceIds).stream()
                .collect(Collectors.toMap(
                        ReakcijaRepository.SpaceReactionCountProjection::getProstorId,
                        item -> new ReactionCounters(
                                item.getLikes() == null ? 0L : item.getLikes(),
                                item.getDislikes() == null ? 0L : item.getDislikes()
                        )
                ));
    }

    public record MemberReservationsResponse(List<MemberReservationItemDto> content) {
    }

        public record MemberSearchResponse(List<MemberSearchItemDto> content) {
        }

        public record MemberSearchItemDto(
            Long id,
            String naziv,
            String grad,
            String adresa,
            String firmaNaziv,
            long likes,
            long dislikes,
            Integer openDesks,
            int officeCount,
            Integer maxOfficeDesks,
            int meetingRoomCount,
            List<Long> matchingSubspaceIds
        ) {
        }

    public record MemberReservationItemDto(
            Long id,
            Long spaceId,
            String spaceName,
            String city,
            LocalDateTime from,
            LocalDateTime to,
            String status,
            boolean cancellable
    ) {
    }

    public record CancelReservationResponse(Long id, String status) {
    }

        public record MemberAvailabilityRequest(
            String type,
            List<Long> resourceIds,
            LocalDate weekStart
        ) {
        }

        public record MemberAvailabilityResponse(
            Long spaceId,
            String type,
            LocalDate weekStart,
            List<ResourceAvailabilityDto> resources
        ) {
        }

        public record ResourceAvailabilityDto(
            Long resourceId,
            String resourceName,
            List<BusySlotDto> busySlots
        ) {
        }

        public record BusySlotDto(
            LocalDateTime from,
            LocalDateTime to
        ) {
        }

    private record ReactionCounters(long likes, long dislikes) {
        private static final ReactionCounters ZERO = new ReactionCounters(0, 0);
    }

    private record SpaceWithMatchingIds(Prostor space, List<Long> matchingSubspaceIds) {
    }
}