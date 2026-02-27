package com.coworking_hub.app.controller;

import com.coworking_hub.app.model.Firma;
import com.coworking_hub.app.model.Komentar;
import com.coworking_hub.app.model.Korisnik;
import com.coworking_hub.app.model.Prostor;
import com.coworking_hub.app.model.SlikaProstora;
import com.coworking_hub.app.model.enums.StatusProstora;
import com.coworking_hub.app.model.enums.TipReakcije;
import com.coworking_hub.app.repository.KomentarRepository;
import com.coworking_hub.app.repository.ProstorRepository;
import com.coworking_hub.app.repository.ReakcijaRepository;
import com.coworking_hub.app.repository.SlikaProstoraRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PublicControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ProstorRepository prostorRepository;

    @Mock
    private ReakcijaRepository reakcijaRepository;

    @Mock
    private SlikaProstoraRepository slikaProstoraRepository;

    @Mock
    private KomentarRepository komentarRepository;

    @BeforeEach
    void setUp() {
        PublicController publicController = new PublicController(
                prostorRepository,
                reakcijaRepository,
                slikaProstoraRepository,
                komentarRepository
        );
        mockMvc = MockMvcBuilders.standaloneSetup(publicController).build();
    }

    @Test
    void homeShouldReturnDashboardInfo() throws Exception {
        Prostor first = buildSpace(10L, "Hub Dorcol", "Beograd", "Coworking Plus", "Nikola", "Ilic");
        Prostor second = buildSpace(20L, "Hub NS", "Novi Sad", "Coworking NS", "Milan", "Milic");

        when(prostorRepository.countByStatus(StatusProstora.odobren)).thenReturn(2L);
        when(prostorRepository.searchApproved(eq(StatusProstora.odobren), eq(null), eq(List.of()), eq(false)))
                .thenReturn(List.of(first, second));

        when(reakcijaRepository.countGroupedByProstorIds(List.of(10L, 20L))).thenReturn(List.of(
                projection(10L, 54L, 3L),
                projection(20L, 11L, 1L)
        ));

        mockMvc.perform(get("/api/public/home"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalApprovedSpaces").value(2))
                .andExpect(jsonPath("$.top5Spaces[0].spaceId").value(10))
                .andExpect(jsonPath("$.top5Spaces[0].likes").value(54));
    }

    @Test
    void citiesShouldReturnDistinctCities() throws Exception {
        when(prostorRepository.findDistinctGradoviByStatus(StatusProstora.odobren))
                .thenReturn(List.of("Beograd", "Nis", "Novi Sad"));

        mockMvc.perform(get("/api/public/spaces/cities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cities[0]").value("Beograd"))
                .andExpect(jsonPath("$.cities[1]").value("Nis"))
                .andExpect(jsonPath("$.cities[2]").value("Novi Sad"));
    }

    @Test
    void searchSpacesShouldReturnSingleListResult() throws Exception {
        Prostor first = buildSpace(10L, "Hub Dorcol", "Beograd", "Coworking Plus", "Nikola", "Ilic");
        Prostor second = buildSpace(20L, "Hub NS", "Novi Sad", "Coworking NS", "Milan", "Milic");

        when(prostorRepository.searchApproved(eq(StatusProstora.odobren), eq("Hub"), eq(List.of("Beograd", "Novi Sad")), eq(true)))
                .thenReturn(List.of(first, second));

        when(reakcijaRepository.countGroupedByProstorIds(List.of(20L, 10L)))
            .thenReturn(List.of(
                projection(20L, 5L, 2L),
                projection(10L, 1L, 0L)
            ));

        mockMvc.perform(get("/api/public/spaces")
                        .param("name", "Hub")
                        .param("cities", "Beograd", "Novi Sad")
                        .param("sortBy", "grad")
                .param("sortDir", "desc"))
                .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].id").value(20))
                .andExpect(jsonPath("$.content[0].likes").value(5));
    }

    @Test
    void spaceDetailsShouldReturnFullDetails() throws Exception {
        Prostor space = buildSpace(10L, "Hub Dorcol", "Beograd", "Coworking Plus", "Nikola", "Ilic");
        space.setOpis("Duz opis prostora...");
        space.setCenaPoSatu(new BigDecimal("12.50"));
        space.setGeografskaSirina(new BigDecimal("44.8176000"));
        space.setGeografskaDuzina(new BigDecimal("20.4633000"));

        Komentar komentar = new Komentar();
        komentar.setId(88L);
        komentar.setClan(space.getMenadzer());
        komentar.setSadrzaj("Odlican prostor");
        komentar.setKreirano(LocalDateTime.of(2026, 2, 20, 11, 22));

        when(prostorRepository.findByIdAndStatus(10L, StatusProstora.odobren)).thenReturn(Optional.of(space));
        when(reakcijaRepository.countByProstorIdAndTip(10L, TipReakcije.svidjanje)).thenReturn(54L);
        when(reakcijaRepository.countByProstorIdAndTip(10L, TipReakcije.nesvidjanje)).thenReturn(3L);
        when(slikaProstoraRepository.findByProstorIdOrderByRedosledAscIdAsc(10L)).thenReturn(List.of(
                buildImage(10L, 0, "/uploads/spaces/10/main.jpg"),
                buildImage(10L, 1, "/uploads/spaces/10/t1.jpg"),
                buildImage(10L, 2, "/uploads/spaces/10/t2.jpg")
        ));
        when(komentarRepository.findTop10ByProstorIdOrderByKreiranoDesc(10L)).thenReturn(List.of(komentar));

        mockMvc.perform(get("/api/public/spaces/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.firma.naziv").value("Coworking Plus"))
                .andExpect(jsonPath("$.menadzer.imePrezime").value("Nikola Ilic"))
                .andExpect(jsonPath("$.reactions.likes").value(54))
                .andExpect(jsonPath("$.images[0]").value("/uploads/spaces/10/main.jpg"))
                .andExpect(jsonPath("$.images[1]").value("/uploads/spaces/10/t1.jpg"))
                .andExpect(jsonPath("$.latestComments[0].username").value("nikola"));
    }

    @Test
    void spaceDetailsShouldReturnNotFound() throws Exception {
        when(prostorRepository.findByIdAndStatus(999L, StatusProstora.odobren)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/public/spaces/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Prostor nije pronadjen"));
    }

    @Test
    void spaceDetailsShouldReturnDefaultImageWhenNoImagesExist() throws Exception {
        Prostor space = buildSpace(30L, "Hub Zvezdara", "Beograd", "Coworking Plus", "Nikola", "Ilic");

        when(prostorRepository.findByIdAndStatus(30L, StatusProstora.odobren)).thenReturn(Optional.of(space));
        when(reakcijaRepository.countByProstorIdAndTip(30L, TipReakcije.svidjanje)).thenReturn(0L);
        when(reakcijaRepository.countByProstorIdAndTip(30L, TipReakcije.nesvidjanje)).thenReturn(0L);
        when(slikaProstoraRepository.findByProstorIdOrderByRedosledAscIdAsc(30L)).thenReturn(List.of());
        when(komentarRepository.findTop10ByProstorIdOrderByKreiranoDesc(30L)).thenReturn(List.of());

        mockMvc.perform(get("/api/public/spaces/30"))
                .andExpect(status().isOk())
            .andExpect(jsonPath("$.images[0]").value("/uploads/spaces/default-space.jpg"));
    }

    private Prostor buildSpace(Long id, String naziv, String grad, String firmaNaziv, String managerFirstName, String managerLastName) {
        Firma firma = new Firma();
        firma.setId(2L);
        firma.setNaziv(firmaNaziv);

        Korisnik menadzer = new Korisnik();
        menadzer.setId(7L);
        menadzer.setKorisnickoIme(managerFirstName.toLowerCase());
        menadzer.setIme(managerFirstName);
        menadzer.setPrezime(managerLastName);

        Prostor prostor = new Prostor();
        prostor.setId(id);
        prostor.setNaziv(naziv);
        prostor.setGrad(grad);
        prostor.setAdresa("Cara Dusana 10");
        prostor.setFirma(firma);
        prostor.setMenadzer(menadzer);
        prostor.setStatus(StatusProstora.odobren);
        return prostor;
    }

        private SlikaProstora buildImage(Long prostorId, int redosled, String path) {
        Prostor prostor = new Prostor();
        prostor.setId(prostorId);

        SlikaProstora image = new SlikaProstora();
        image.setProstor(prostor);
        image.setRedosled(redosled);
        image.setPutanjaSlike(path);
        return image;
    }

    private ReakcijaRepository.SpaceReactionCountProjection projection(Long prostorId, Long likes, Long dislikes) {
        return new ReakcijaRepository.SpaceReactionCountProjection() {
            @Override
            public Long getProstorId() {
                return prostorId;
            }

            @Override
            public Long getLikes() {
                return likes;
            }

            @Override
            public Long getDislikes() {
                return dislikes;
            }
        };
    }
}
