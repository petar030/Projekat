package com.coworking_hub.app.controller;

import com.coworking_hub.app.model.Kancelarija;
import com.coworking_hub.app.model.KonferencijskaSala;
import com.coworking_hub.app.model.Korisnik;
import com.coworking_hub.app.model.OtvoreniProstor;
import com.coworking_hub.app.model.Prostor;
import com.coworking_hub.app.model.Rezervacija;
import com.coworking_hub.app.model.SlikaProstora;
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
import com.coworking_hub.app.security.CurrentUser;
import com.coworking_hub.app.service.ImageStorageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/manager")
public class ManagerController {

    private final ProstorRepository prostorRepository;
    private final KorisnikRepository korisnikRepository;
    private final OtvoreniProstorRepository otvoreniProstorRepository;
    private final KancelarijaRepository kancelarijaRepository;
    private final KonferencijskaSalaRepository konferencijskaSalaRepository;
        private final RezervacijaRepository rezervacijaRepository;
        private final SlikaProstoraRepository slikaProstoraRepository;
        private final ImageStorageService imageStorageService;

    public ManagerController(
            ProstorRepository prostorRepository,
            KorisnikRepository korisnikRepository,
            OtvoreniProstorRepository otvoreniProstorRepository,
            KancelarijaRepository kancelarijaRepository,
            KonferencijskaSalaRepository konferencijskaSalaRepository,
            RezervacijaRepository rezervacijaRepository,
                        SlikaProstoraRepository slikaProstoraRepository,
                        ImageStorageService imageStorageService
    ) {
        this.prostorRepository = prostorRepository;
        this.korisnikRepository = korisnikRepository;
        this.otvoreniProstorRepository = otvoreniProstorRepository;
        this.kancelarijaRepository = kancelarijaRepository;
        this.konferencijskaSalaRepository = konferencijskaSalaRepository;
        this.rezervacijaRepository = rezervacijaRepository;
                this.slikaProstoraRepository = slikaProstoraRepository;
                this.imageStorageService = imageStorageService;
    }

        @PostMapping("/spaces")
        @Transactional
        public ResponseEntity<?> createSpace(
                        @CurrentUser AuthenticatedUser authenticatedUser,
                        @RequestPart("data") CreateSpaceRequest request,
                        @RequestPart(value = "images", required = false) List<MultipartFile> images
        ) {
                Optional<Korisnik> managerOptional = korisnikRepository.findById(authenticatedUser.userId());
                if (managerOptional.isEmpty()) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Korisnik nije pronadjen"));
                }

                Korisnik manager = managerOptional.get();
                if (manager.getFirma() == null || manager.getFirma().getId() == null) {
                        return ResponseEntity.badRequest().body(Map.of("message", "Menadzer nema dodeljenu firmu"));
                }

                if (request == null
                                || isBlank(request.naziv())
                                || isBlank(request.grad())
                                || isBlank(request.adresa())
                                || request.cenaPoSatu() == null
                                || request.pragKazni() == null
                                || request.openSpace() == null
                                || request.openSpace().brojStolova() == null) {
                        return ResponseEntity.badRequest().body(Map.of("message", "Neispravan request za kreiranje prostora"));
                }

                if (request.openSpace().brojStolova() < 5) {
                        return ResponseEntity.badRequest().body(Map.of("message", "Open space mora imati najmanje 5 stolova"));
                }

                List<MultipartFile> safeImages = images == null ? List.of() : images;
                if (safeImages.size() > 5) {
                        return ResponseEntity.badRequest().body(Map.of("message", "Maksimalan broj slika je 5"));
                }

                LocalDateTime now = currentUtcTime();

                Prostor prostor = new Prostor();
                prostor.setNaziv(request.naziv().trim());
                prostor.setGrad(request.grad().trim());
                prostor.setAdresa(request.adresa().trim());
                prostor.setOpis(request.opis());
                prostor.setCenaPoSatu(request.cenaPoSatu());
                prostor.setPragKazni(request.pragKazni());
                prostor.setGeografskaSirina(request.geografskaSirina());
                prostor.setGeografskaDuzina(request.geografskaDuzina());
                prostor.setStatus(StatusProstora.na_cekanju);
                prostor.setFirma(manager.getFirma());
                prostor.setMenadzer(manager);
                prostor.setKreirano(now);
                prostor.setAzurirano(now);

                Prostor savedSpace = prostorRepository.save(prostor);

                OtvoreniProstor openSpace = new OtvoreniProstor();
                openSpace.setProstor(savedSpace);
                openSpace.setBrojStolova(request.openSpace().brojStolova());
                openSpace.setKreirano(now);
                otvoreniProstorRepository.save(openSpace);

                for (int index = 0; index < safeImages.size(); index++) {
                        MultipartFile image = safeImages.get(index);
                        String imagePath = imageStorageService.storeSpaceImage(image, savedSpace.getId());

                        SlikaProstora slika = new SlikaProstora();
                        slika.setProstor(savedSpace);
                        slika.setPutanjaSlike(imagePath);
                        slika.setRedosled(index);
                        slika.setKreirano(now);
                        slikaProstoraRepository.save(slika);
                }

                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(new CreateSpaceResponse(savedSpace.getId(), savedSpace.getStatus().name(), "Prostor je kreiran i ceka odobrenje admina"));
        }

        @PostMapping("/spaces/{spaceId}/offices")
        @Transactional
        public ResponseEntity<?> addOffice(
                        @CurrentUser AuthenticatedUser authenticatedUser,
                        @PathVariable Long spaceId,
                        @RequestBody CreateOfficeRequest request
        ) {
                Optional<Korisnik> managerOptional = korisnikRepository.findById(authenticatedUser.userId());
                if (managerOptional.isEmpty()) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Korisnik nije pronadjen"));
                }

                Korisnik manager = managerOptional.get();
                if (manager.getFirma() == null || manager.getFirma().getId() == null) {
                        return ResponseEntity.badRequest().body(Map.of("message", "Menadzer nema dodeljenu firmu"));
                }

                if (request == null || isBlank(request.naziv()) || request.brojStolova() == null || request.brojStolova() < 1) {
                        return ResponseEntity.badRequest().body(Map.of("message", "Neispravan request za dodavanje kancelarije"));
                }

                Optional<Prostor> spaceOptional = prostorRepository.findByIdAndFirmaId(spaceId, manager.getFirma().getId());
                if (spaceOptional.isEmpty()) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Prostor nije pronadjen"));
                }

                Kancelarija office = new Kancelarija();
                office.setProstor(spaceOptional.get());
                office.setNaziv(request.naziv().trim());
                office.setBrojStolova(request.brojStolova());
                office.setKreirano(currentUtcTime());

                Kancelarija saved = kancelarijaRepository.save(office);
                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(new OfficeResponse(saved.getId(), spaceId, saved.getNaziv(), saved.getBrojStolova()));
        }

        @PostMapping("/spaces/{spaceId}/meeting-rooms")
        @Transactional
        public ResponseEntity<?> addMeetingRoom(
                        @CurrentUser AuthenticatedUser authenticatedUser,
                        @PathVariable Long spaceId,
                        @RequestBody CreateMeetingRoomRequest request
        ) {
                Optional<Korisnik> managerOptional = korisnikRepository.findById(authenticatedUser.userId());
                if (managerOptional.isEmpty()) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Korisnik nije pronadjen"));
                }

                Korisnik manager = managerOptional.get();
                if (manager.getFirma() == null || manager.getFirma().getId() == null) {
                        return ResponseEntity.badRequest().body(Map.of("message", "Menadzer nema dodeljenu firmu"));
                }

                if (request == null || isBlank(request.naziv())) {
                        return ResponseEntity.badRequest().body(Map.of("message", "Neispravan request za dodavanje konferencijske sale"));
                }

                Optional<Prostor> spaceOptional = prostorRepository.findByIdAndFirmaId(spaceId, manager.getFirma().getId());
                if (spaceOptional.isEmpty()) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Prostor nije pronadjen"));
                }

                KonferencijskaSala room = new KonferencijskaSala();
                room.setProstor(spaceOptional.get());
                room.setNaziv(request.naziv().trim());
                room.setDodatnaOprema(request.dodatnaOprema());
                room.setBrojMesta(10);
                room.setKreirano(currentUtcTime());

                KonferencijskaSala saved = konferencijskaSalaRepository.save(room);
                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(new MeetingRoomResponse(saved.getId(), spaceId, saved.getNaziv(), saved.getBrojMesta(), saved.getDodatnaOprema()));
        }

        @GetMapping("/reservations")
        @Transactional(readOnly = true)
        public ResponseEntity<?> reservations(@CurrentUser AuthenticatedUser authenticatedUser) {
                Optional<Korisnik> managerOptional = korisnikRepository.findById(authenticatedUser.userId());
                if (managerOptional.isEmpty()) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Korisnik nije pronadjen"));
                }

                Korisnik manager = managerOptional.get();
                if (manager.getFirma() == null || manager.getFirma().getId() == null) {
                        return ResponseEntity.badRequest().body(Map.of("message", "Menadzer nema dodeljenu firmu"));
                }

                List<Long> managerSpaceIds = prostorRepository.findByFirmaIdOrderByIdAsc(manager.getFirma().getId()).stream()
                                .map(Prostor::getId)
                                .toList();

                if (managerSpaceIds.isEmpty()) {
                        return ResponseEntity.ok(new ManagerReservationsResponse(List.of()));
                }

                LocalDateTime now = currentUtcTime();
                List<ManagerReservationItemDto> content = rezervacijaRepository
                                .findByProstorIdInAndStatusNotOrderByDatumOdAsc(managerSpaceIds, StatusRezervacije.otkazana)
                                .stream()
                                .filter(reservation -> !reservation.getDatumDo().isBefore(now))
                                .map(reservation -> new ManagerReservationItemDto(
                                                reservation.getId(),
                                                new ReservationMemberDto(
                                                                reservation.getClan().getId(),
                                                                reservation.getClan().getKorisnickoIme()
                                                ),
                                                reservation.getProstor().getId(),
                                                resolveReservationType(reservation),
                                                resolveResourceName(reservation),
                                                reservation.getDatumOd(),
                                                reservation.getDatumDo(),
                                                reservation.getStatus().name(),
                                                canConfirmOrNoShow(reservation, now)
                                ))
                                .toList();

                return ResponseEntity.ok(new ManagerReservationsResponse(content));
        }

        @PatchMapping("/reservations/{reservationId}/confirm")
        @Transactional
        public ResponseEntity<?> confirmReservation(
                        @CurrentUser AuthenticatedUser authenticatedUser,
                        @PathVariable Long reservationId
        ) {
                Optional<Korisnik> managerOptional = korisnikRepository.findById(authenticatedUser.userId());
                if (managerOptional.isEmpty()) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Korisnik nije pronadjen"));
                }

                Korisnik manager = managerOptional.get();
                if (manager.getFirma() == null || manager.getFirma().getId() == null) {
                        return ResponseEntity.badRequest().body(Map.of("message", "Menadzer nema dodeljenu firmu"));
                }

                Optional<Rezervacija> reservationOptional = rezervacijaRepository.findById(reservationId);
                if (reservationOptional.isEmpty()) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Rezervacija nije pronadjena"));
                }

                Rezervacija reservation = reservationOptional.get();
                if (reservation.getProstor() == null
                                || reservation.getProstor().getFirma() == null
                                || reservation.getProstor().getFirma().getId() == null
                                || !reservation.getProstor().getFirma().getId().equals(manager.getFirma().getId())) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Rezervacija nije pronadjena"));
                }

                LocalDateTime now = currentUtcTime();
                if (!canConfirmOrNoShow(reservation, now)) {
                            return ResponseEntity.status(HttpStatusCode.valueOf(422))
                                        .body(Map.of("message", "Potvrda je dozvoljena samo u roku od 10 minuta od pocetka aktivne rezervacije"));
                }

                reservation.setStatus(StatusRezervacije.potvrdjena);
                reservation.setAzurirano(now);
                rezervacijaRepository.save(reservation);

                return ResponseEntity.ok(new ReservationStatusResponse(reservation.getId(), reservation.getStatus().name()));
        }

        @PatchMapping("/reservations/{reservationId}/no-show")
        @Transactional
        public ResponseEntity<?> noShowReservation(
                        @CurrentUser AuthenticatedUser authenticatedUser,
                        @PathVariable Long reservationId
        ) {
                Optional<Korisnik> managerOptional = korisnikRepository.findById(authenticatedUser.userId());
                if (managerOptional.isEmpty()) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Korisnik nije pronadjen"));
                }

                Korisnik manager = managerOptional.get();
                if (manager.getFirma() == null || manager.getFirma().getId() == null) {
                        return ResponseEntity.badRequest().body(Map.of("message", "Menadzer nema dodeljenu firmu"));
                }

                Optional<Rezervacija> reservationOptional = rezervacijaRepository.findById(reservationId);
                if (reservationOptional.isEmpty()) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Rezervacija nije pronadjena"));
                }

                Rezervacija reservation = reservationOptional.get();
                if (reservation.getProstor() == null
                                || reservation.getProstor().getFirma() == null
                                || reservation.getProstor().getFirma().getId() == null
                                || !reservation.getProstor().getFirma().getId().equals(manager.getFirma().getId())) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Rezervacija nije pronadjena"));
                }

                LocalDateTime now = currentUtcTime();
                if (!canConfirmOrNoShow(reservation, now)) {
                            return ResponseEntity.status(HttpStatusCode.valueOf(422))
                                        .body(Map.of("message", "Odjavljivanje je dozvoljeno samo u roku od 10 minuta od pocetka aktivne rezervacije"));
                }

                reservation.setStatus(StatusRezervacije.nepojavljivanje);
                reservation.setAzurirano(now);
                rezervacijaRepository.save(reservation);

                return ResponseEntity.ok(new NoShowStatusResponse(reservation.getId(), reservation.getStatus().name(), true));
        }

    @GetMapping("/spaces")
    @Transactional(readOnly = true)
    public ResponseEntity<?> spaces(@CurrentUser AuthenticatedUser authenticatedUser) {
        Optional<Korisnik> managerOptional = korisnikRepository.findById(authenticatedUser.userId());
        if (managerOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Korisnik nije pronadjen"));
        }

        Korisnik manager = managerOptional.get();
        if (manager.getFirma() == null || manager.getFirma().getId() == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Menadzer nema dodeljenu firmu"));
        }

        List<Prostor> spaces = prostorRepository.findByFirmaIdOrderByIdAsc(manager.getFirma().getId());
        List<Long> spaceIds = spaces.stream().map(Prostor::getId).toList();

        Map<Long, OtvoreniProstor> openSpaceBySpaceId = otvoreniProstorRepository.findByProstorIdIn(spaceIds).stream()
                .collect(Collectors.toMap(item -> item.getProstor().getId(), item -> item, (first, second) -> first));

        Map<Long, List<Kancelarija>> officesBySpaceId = kancelarijaRepository.findByProstorIdIn(spaceIds).stream()
                .collect(Collectors.groupingBy(item -> item.getProstor().getId()));

        Map<Long, List<KonferencijskaSala>> meetingRoomsBySpaceId = konferencijskaSalaRepository.findByProstorIdIn(spaceIds).stream()
                .collect(Collectors.groupingBy(item -> item.getProstor().getId()));

        List<ManagerSpaceDto> content = spaces.stream()
                .map(space -> {
                    OtvoreniProstor openSpace = openSpaceBySpaceId.get(space.getId());
                    OpenSpaceDto openSpaceDto = openSpace == null
                            ? null
                            : new OpenSpaceDto(openSpace.getId(), openSpace.getBrojStolova());

                    List<OfficeDto> officeDtos = officesBySpaceId.getOrDefault(space.getId(), List.of()).stream()
                            .map(office -> new OfficeDto(office.getId(), office.getNaziv(), office.getBrojStolova()))
                            .toList();

                    List<MeetingRoomDto> meetingRoomDtos = meetingRoomsBySpaceId.getOrDefault(space.getId(), List.of()).stream()
                            .map(room -> new MeetingRoomDto(room.getId(), room.getNaziv(), room.getBrojMesta(), room.getDodatnaOprema()))
                            .toList();

                    return new ManagerSpaceDto(
                            space.getId(),
                            space.getNaziv(),
                            space.getGrad(),
                            space.getStatus().name(),
                            space.getPragKazni(),
                            new SpaceElementsDto(openSpaceDto, officeDtos, meetingRoomDtos)
                    );
                })
                .toList();

        return ResponseEntity.ok(new ManagerSpacesResponse(content));
    }

    public record ManagerSpacesResponse(List<ManagerSpaceDto> spaces) {
    }

    public record ManagerSpaceDto(
            Long id,
            String naziv,
            String grad,
            String status,
            Integer pragKazni,
            SpaceElementsDto elements
    ) {
    }

    public record SpaceElementsDto(
            OpenSpaceDto openSpace,
            List<OfficeDto> offices,
            List<MeetingRoomDto> meetingRooms
    ) {
    }

    public record OpenSpaceDto(Long id, Integer brojStolova) {
    }

    public record OfficeDto(Long id, String naziv, Integer brojStolova) {
    }

    public record MeetingRoomDto(Long id, String naziv, Integer brojMesta, String dodatnaOprema) {
    }

    public record ManagerReservationsResponse(List<ManagerReservationItemDto> content) {
    }

    public record ManagerReservationItemDto(
            Long id,
            ReservationMemberDto member,
            Long spaceId,
            String type,
            String resourceName,
            LocalDateTime from,
            LocalDateTime to,
            String status,
            boolean canConfirmOrNoShow
    ) {
    }

    public record ReservationMemberDto(Long id, String username) {
    }

    public record ReservationStatusResponse(Long id, String status) {
    }

    public record NoShowStatusResponse(Long id, String status, boolean penaltyCreated) {
    }

        public record CreateSpaceRequest(
                        String naziv,
                        String grad,
                        String adresa,
                        String opis,
                        BigDecimal cenaPoSatu,
                        Integer pragKazni,
                        BigDecimal geografskaSirina,
                        BigDecimal geografskaDuzina,
                        OpenSpaceRequest openSpace
        ) {
        }

        public record OpenSpaceRequest(Integer brojStolova) {
        }

        public record CreateSpaceResponse(Long id, String status, String message) {
        }

        public record CreateOfficeRequest(String naziv, Integer brojStolova) {
        }

        public record OfficeResponse(Long id, Long spaceId, String naziv, Integer brojStolova) {
        }

        public record CreateMeetingRoomRequest(String naziv, String dodatnaOprema) {
        }

        public record MeetingRoomResponse(Long id, Long spaceId, String naziv, Integer brojMesta, String dodatnaOprema) {
        }

        private String resolveReservationType(Rezervacija reservation) {
                if (reservation.getOtvoreniProstor() != null) {
                        return "otvoreni";
                }
                if (reservation.getKancelarija() != null) {
                        return "kancelarija";
                }
                return "sala";
        }

        private String resolveResourceName(Rezervacija reservation) {
                if (reservation.getOtvoreniProstor() != null) {
                        return "Otvoreni prostor";
                }
                if (reservation.getKancelarija() != null) {
                        return reservation.getKancelarija().getNaziv();
                }
                if (reservation.getSala() != null) {
                        return reservation.getSala().getNaziv();
                }
                return "Nepoznat resurs";
        }

        private boolean canConfirmOrNoShow(Rezervacija reservation, LocalDateTime now) {
                if (reservation.getStatus() != StatusRezervacije.aktivna) {
                        return false;
                }

                LocalDateTime windowStart = reservation.getDatumOd();
                LocalDateTime windowEnd = reservation.getDatumOd().plusMinutes(10);
                return !now.isBefore(windowStart) && !now.isAfter(windowEnd);
        }

        private LocalDateTime currentUtcTime() {
                return LocalDateTime.now(ZoneOffset.UTC);
        }

        private boolean isBlank(String value) {
                return value == null || value.isBlank();
        }
}
