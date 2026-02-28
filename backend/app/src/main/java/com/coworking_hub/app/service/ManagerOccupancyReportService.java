package com.coworking_hub.app.service;

import com.coworking_hub.app.model.Kancelarija;
import com.coworking_hub.app.model.KonferencijskaSala;
import com.coworking_hub.app.model.OtvoreniProstor;
import com.coworking_hub.app.model.Prostor;
import com.coworking_hub.app.model.Rezervacija;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class ManagerOccupancyReportService {

    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("MM.yyyy");

    public byte[] generateMonthlyOccupancyPdf(
            Prostor space,
            YearMonth month,
            OtvoreniProstor openSpace,
            List<Kancelarija> offices,
            List<KonferencijskaSala> meetingRooms,
            List<Rezervacija> overlappingReservations
    ) {
        LocalDateTime monthStart = month.atDay(1).atStartOfDay();
        LocalDateTime monthEnd = month.plusMonths(1).atDay(1).atStartOfDay();
        long monthMinutes = Duration.between(monthStart, monthEnd).toMinutes();

        List<ResourceOccupancy> resources = new ArrayList<>();

        if (openSpace != null && openSpace.getId() != null && openSpace.getBrojStolova() != null && openSpace.getBrojStolova() > 0) {
            long occupiedMinutes = overlappingReservations.stream()
                    .filter(item -> item.getOtvoreniProstor() != null)
                    .filter(item -> openSpace.getId().equals(item.getOtvoreniProstor().getId()))
                    .mapToLong(item -> overlapMinutes(item.getDatumOd(), item.getDatumDo(), monthStart, monthEnd))
                    .sum();

            resources.add(new ResourceOccupancy(
                    "otvoreni",
                    openSpace.getId(),
                    "Otvoreni prostor",
                    openSpace.getBrojStolova(),
                    occupiedMinutes,
                    monthMinutes
            ));
        }

        for (Kancelarija office : offices) {
            if (office.getId() == null || office.getBrojStolova() == null || office.getBrojStolova() <= 0) {
                continue;
            }

            long occupiedSeatMinutes = overlappingReservations.stream()
                    .filter(item -> item.getKancelarija() != null)
                    .filter(item -> office.getId().equals(item.getKancelarija().getId()))
                    .mapToLong(item -> overlapMinutes(item.getDatumOd(), item.getDatumDo(), monthStart, monthEnd) * office.getBrojStolova())
                    .sum();

            resources.add(new ResourceOccupancy(
                    "kancelarija",
                    office.getId(),
                    office.getNaziv(),
                    office.getBrojStolova(),
                    occupiedSeatMinutes,
                    monthMinutes
            ));
        }

        for (KonferencijskaSala room : meetingRooms) {
            if (room.getId() == null || room.getBrojMesta() == null || room.getBrojMesta() <= 0) {
                continue;
            }

            long occupiedSeatMinutes = overlappingReservations.stream()
                    .filter(item -> item.getSala() != null)
                    .filter(item -> room.getId().equals(item.getSala().getId()))
                    .mapToLong(item -> overlapMinutes(item.getDatumOd(), item.getDatumDo(), monthStart, monthEnd) * room.getBrojMesta())
                    .sum();

            resources.add(new ResourceOccupancy(
                    "sala",
                    room.getId(),
                    room.getNaziv(),
                    room.getBrojMesta(),
                    occupiedSeatMinutes,
                    monthMinutes
            ));
        }

        double totalOccupiedSeatMinutes = resources.stream().mapToDouble(ResourceOccupancy::occupiedSeatMinutes).sum();
        double totalCapacitySeatMinutes = resources.stream().mapToDouble(ResourceOccupancy::totalCapacitySeatMinutes).sum();
        double overallPercent = safePercent(totalOccupiedSeatMinutes, totalCapacitySeatMinutes);

        return createPdf(space, month, overallPercent, resources);
    }

    private byte[] createPdf(Prostor space, YearMonth month, double overallPercent, List<ResourceOccupancy> resources) {
        List<String> lines = new ArrayList<>();
        lines.add("Coworking Hub Manager - Mesecni izvestaj popunjenosti");
        lines.add("Prostor: " + nullSafe(space.getNaziv()) + " (" + nullSafe(space.getGrad()) + ")");
        lines.add("Mesec: " + month.format(MONTH_FORMATTER));
        lines.add("Generisano (UTC): " + LocalDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        lines.add("");
        lines.add(String.format("Ukupna popunjenost kapaciteta: %.2f%%", overallPercent));
        lines.add("Broj analiziranih elemenata: " + resources.size());
        lines.add("");
        lines.add("Popunjenost po elementima prostora:");

        if (resources.isEmpty()) {
            lines.add("- Nema elemenata prostora za izracunavanje.");
        } else {
            for (ResourceOccupancy item : resources) {
                lines.add(String.format(
                        "- [%s] %s (ID=%d, kapacitet=%d): %.2f%%",
                        item.type(),
                        nullSafe(item.resourceName()),
                        item.resourceId(),
                        item.capacityUnits(),
                        item.occupancyPercent()
                ));
            }
        }

        try (PDDocument document = new PDDocument(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            float margin = 40f;
            float fontSize = 11f;
            float leading = 16f;

            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            PDPageContentStream stream = new PDPageContentStream(document, page);
            float y = page.getMediaBox().getHeight() - margin;

            for (String line : lines) {
                if (y <= margin) {
                    stream.close();
                    page = new PDPage(PDRectangle.A4);
                    document.addPage(page);
                    stream = new PDPageContentStream(document, page);
                    y = page.getMediaBox().getHeight() - margin;
                }

                stream.beginText();
                stream.setFont(font, fontSize);
                stream.newLineAtOffset(margin, y);
                stream.showText(sanitize(line));
                stream.endText();
                y -= leading;
            }

            stream.close();
            document.save(outputStream);
            return outputStream.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Neuspesno kreiranje PDF izvestaja", exception);
        }
    }

    private long overlapMinutes(LocalDateTime from, LocalDateTime to, LocalDateTime windowStart, LocalDateTime windowEnd) {
        if (from == null || to == null || !to.isAfter(from)) {
            return 0L;
        }

        LocalDateTime effectiveStart = from.isAfter(windowStart) ? from : windowStart;
        LocalDateTime effectiveEnd = to.isBefore(windowEnd) ? to : windowEnd;

        if (!effectiveEnd.isAfter(effectiveStart)) {
            return 0L;
        }

        return Duration.between(effectiveStart, effectiveEnd).toMinutes();
    }

    private double safePercent(double numerator, double denominator) {
        if (denominator <= 0d) {
            return 0d;
        }
        return (numerator / denominator) * 100d;
    }

    private String sanitize(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("č", "c")
                .replace("ć", "c")
                .replace("ž", "z")
                .replace("š", "s")
                .replace("đ", "dj")
                .replace("Č", "C")
                .replace("Ć", "C")
                .replace("Ž", "Z")
                .replace("Š", "S")
                .replace("Đ", "Dj");
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }

    private record ResourceOccupancy(
            String type,
            Long resourceId,
            String resourceName,
            int capacityUnits,
            long occupiedSeatMinutes,
            long monthMinutes
    ) {
        double totalCapacitySeatMinutes() {
            return (double) capacityUnits * (double) monthMinutes;
        }

        double occupancyPercent() {
            double denominator = totalCapacitySeatMinutes();
            if (denominator <= 0d) {
                return 0d;
            }
            return (occupiedSeatMinutes / denominator) * 100d;
        }
    }
}
