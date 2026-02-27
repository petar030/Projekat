package com.coworking_hub.app.controller;

import com.coworking_hub.app.model.Komentar;
import com.coworking_hub.app.model.Prostor;
import com.coworking_hub.app.model.SlikaProstora;
import com.coworking_hub.app.model.enums.StatusProstora;
import com.coworking_hub.app.model.enums.TipReakcije;
import com.coworking_hub.app.repository.KomentarRepository;
import com.coworking_hub.app.repository.ProstorRepository;
import com.coworking_hub.app.repository.ReakcijaRepository;
import com.coworking_hub.app.repository.SlikaProstoraRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/public")
public class PublicController {

        private static final String DEFAULT_SPACE_IMAGE = "/uploads/spaces/default-space.jpg";

    private final ProstorRepository prostorRepository;
    private final ReakcijaRepository reakcijaRepository;
    private final SlikaProstoraRepository slikaProstoraRepository;
    private final KomentarRepository komentarRepository;

    public PublicController(
            ProstorRepository prostorRepository,
            ReakcijaRepository reakcijaRepository,
            SlikaProstoraRepository slikaProstoraRepository,
            KomentarRepository komentarRepository
    ) {
        this.prostorRepository = prostorRepository;
        this.reakcijaRepository = reakcijaRepository;
        this.slikaProstoraRepository = slikaProstoraRepository;
        this.komentarRepository = komentarRepository;
    }

    @GetMapping("/home")
    public ResponseEntity<HomeResponse> home() {
        long totalApprovedSpaces = prostorRepository.countByStatus(StatusProstora.odobren);

        List<Prostor> approvedSpaces = prostorRepository.searchApproved(
                StatusProstora.odobren,
                null,
                List.of(),
                false
        );

        Map<Long, ReactionCounters> countersBySpace = buildReactionCounters(approvedSpaces.stream()
                .map(Prostor::getId)
                .toList());

        List<HomeSpaceDto> top5 = approvedSpaces.stream()
                .sorted(Comparator
                        .comparingLong((Prostor p) -> countersBySpace.getOrDefault(p.getId(), ReactionCounters.ZERO).likes())
                        .reversed()
                        .thenComparing(Prostor::getId))
                .limit(5)
                .map(space -> {
                    ReactionCounters counters = countersBySpace.getOrDefault(space.getId(), ReactionCounters.ZERO);
                    return new HomeSpaceDto(
                            space.getId(),
                            space.getNaziv(),
                            space.getGrad(),
                            counters.likes(),
                            counters.dislikes()
                    );
                })
                .toList();

        return ResponseEntity.ok(new HomeResponse(totalApprovedSpaces, top5));
    }

    @GetMapping("/spaces/cities")
    public ResponseEntity<CitiesResponse> cities() {
        List<String> cities = prostorRepository.findDistinctGradoviByStatus(StatusProstora.odobren);
        return ResponseEntity.ok(new CitiesResponse(cities));
    }

    @GetMapping("/spaces")
    public ResponseEntity<SearchResponse> searchSpaces(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) List<String> cities,
            @RequestParam(defaultValue = "naziv") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        List<String> cityFilters = cities == null ? List.of() : cities;

        List<Prostor> spaces = prostorRepository.searchApproved(
                StatusProstora.odobren,
                name,
                cityFilters,
                !cityFilters.isEmpty()
        );

        Comparator<Prostor> comparator = buildComparator(sortBy, sortDir);
        List<Prostor> sortedSpaces = spaces.stream().sorted(comparator).toList();

        Map<Long, ReactionCounters> countersBySpace = buildReactionCounters(sortedSpaces.stream()
                .map(Prostor::getId)
                .toList());

        List<SearchSpaceItemDto> content = sortedSpaces.stream()
                .map(space -> {
                    ReactionCounters counters = countersBySpace.getOrDefault(space.getId(), ReactionCounters.ZERO);
                    return new SearchSpaceItemDto(
                            space.getId(),
                            space.getNaziv(),
                            space.getGrad(),
                            space.getAdresa(),
                            space.getFirma().getNaziv(),
                            counters.likes(),
                            counters.dislikes()
                    );
                })
                .toList();

        return ResponseEntity.ok(new SearchResponse(content));
    }

    @GetMapping("/spaces/{spaceId}")
        @Transactional(readOnly = true)
    public ResponseEntity<?> spaceDetails(@PathVariable Long spaceId) {
        Optional<Prostor> spaceOptional = prostorRepository.findByIdAndStatus(spaceId, StatusProstora.odobren);
        if (spaceOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Prostor nije pronadjen"));
        }

        Prostor space = spaceOptional.get();
        long likes = reakcijaRepository.countByProstorIdAndTip(spaceId, TipReakcije.svidjanje);
        long dislikes = reakcijaRepository.countByProstorIdAndTip(spaceId, TipReakcije.nesvidjanje);

        List<String> images = resolveImages(spaceId);

        List<LatestCommentDto> latestComments = komentarRepository.findTop10ByProstorIdOrderByKreiranoDesc(spaceId)
                .stream()
                .map(comment -> new LatestCommentDto(
                        comment.getId(),
                        comment.getClan().getKorisnickoIme(),
                        comment.getKreirano(),
                        comment.getSadrzaj()
                ))
                .toList();

        SpaceDetailsResponse response = new SpaceDetailsResponse(
                space.getId(),
                space.getNaziv(),
                space.getGrad(),
                space.getAdresa(),
                space.getOpis(),
                space.getCenaPoSatu(),
                new FirmaDto(space.getFirma().getId(), space.getFirma().getNaziv()),
                new MenadzerDto(space.getMenadzer().getId(), fullName(space.getMenadzer().getIme(), space.getMenadzer().getPrezime())),
                new GeolocationDto(space.getGeografskaSirina(), space.getGeografskaDuzina()),
                new ReactionsDto(likes, dislikes),
                images,
                latestComments
        );

        return ResponseEntity.ok(response);
    }

    private Comparator<Prostor> buildComparator(String sortBy, String sortDir) {
        String normalizedSortBy = sortBy == null ? "naziv" : sortBy.toLowerCase(Locale.ROOT);
        Comparator<Prostor> comparator;

        if ("grad".equals(normalizedSortBy)) {
            comparator = Comparator.comparing(Prostor::getGrad, String.CASE_INSENSITIVE_ORDER);
        } else {
            comparator = Comparator.comparing(Prostor::getNaziv, String.CASE_INSENSITIVE_ORDER);
        }

        if (sortDir != null && "desc".equalsIgnoreCase(sortDir)) {
            comparator = comparator.reversed();
        }

        return comparator.thenComparing(Prostor::getId);
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

        private List<String> resolveImages(Long spaceId) {
                List<String> images = slikaProstoraRepository.findByProstorIdOrderByRedosledAscIdAsc(spaceId)
                                .stream()
                                .map(SlikaProstora::getPutanjaSlike)
                                .limit(6)
                                .toList();

                if (images.isEmpty()) {
                        return List.of(DEFAULT_SPACE_IMAGE);
                }

                return images;
    }

    private String fullName(String firstName, String lastName) {
        String first = firstName == null ? "" : firstName.trim();
        String last = lastName == null ? "" : lastName.trim();
        return (first + " " + last).trim();
    }

    private record ReactionCounters(long likes, long dislikes) {
        private static final ReactionCounters ZERO = new ReactionCounters(0, 0);
    }

    public record HomeResponse(long totalApprovedSpaces, List<HomeSpaceDto> top5Spaces) {
    }

        public record HomeSpaceDto(Long spaceId, String naziv, String grad, long likes, long dislikes) {
    }

    public record CitiesResponse(List<String> cities) {
    }

    public record SearchResponse(
            List<SearchSpaceItemDto> content
    ) {
    }

    public record SearchSpaceItemDto(
            Long id,
            String naziv,
            String grad,
            String adresa,
            String firmaNaziv,
            long likes,
            long dislikes
    ) {
    }

    public record SpaceDetailsResponse(
            Long id,
            String naziv,
            String grad,
            String adresa,
            String opis,
            BigDecimal cenaPoSatu,
            FirmaDto firma,
            MenadzerDto menadzer,
            GeolocationDto geolocation,
            ReactionsDto reactions,
            List<String> images,
            List<LatestCommentDto> latestComments
    ) {
    }

    public record FirmaDto(Long id, String naziv) {
    }

    public record MenadzerDto(Long id, String imePrezime) {
    }

    public record GeolocationDto(BigDecimal lat, BigDecimal lng) {
    }

    public record ReactionsDto(long likes, long dislikes) {
    }

    public record LatestCommentDto(Long id, String username, LocalDateTime createdAt, String text) {
    }
}
