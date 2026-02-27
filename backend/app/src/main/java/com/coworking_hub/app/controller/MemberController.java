package com.coworking_hub.app.controller;

import com.coworking_hub.app.model.Rezervacija;
import com.coworking_hub.app.model.enums.StatusRezervacije;
import com.coworking_hub.app.repository.RezervacijaRepository;
import com.coworking_hub.app.security.AuthenticatedUser;
import com.coworking_hub.app.security.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/member")
public class MemberController {

    private final RezervacijaRepository rezervacijaRepository;

    public MemberController(RezervacijaRepository rezervacijaRepository) {
        this.rezervacijaRepository = rezervacijaRepository;
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

    public record MemberReservationsResponse(List<MemberReservationItemDto> content) {
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
}