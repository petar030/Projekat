package com.coworking_hub.app.model;

import com.coworking_hub.app.model.enums.StatusRezervacije;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "rezervacije")
public class Rezervacija {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clan_id", nullable = false)
    private Korisnik clan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prostor_id", nullable = false)
    private Prostor prostor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "otvoreni_prostor_id")
    private OtvoreniProstor otvoreniProstor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kancelarija_id")
    private Kancelarija kancelarija;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sala_id")
    private KonferencijskaSala sala;

    @Column(name = "datum_od", nullable = false)
    private LocalDateTime datumOd;

    @Column(name = "datum_do", nullable = false)
    private LocalDateTime datumDo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusRezervacije status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime kreirano;

    @Column(nullable = false)
    private LocalDateTime azurirano;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Korisnik getClan() {
        return clan;
    }

    public void setClan(Korisnik clan) {
        this.clan = clan;
    }

    public Prostor getProstor() {
        return prostor;
    }

    public void setProstor(Prostor prostor) {
        this.prostor = prostor;
    }

    public OtvoreniProstor getOtvoreniProstor() {
        return otvoreniProstor;
    }

    public void setOtvoreniProstor(OtvoreniProstor otvoreniProstor) {
        this.otvoreniProstor = otvoreniProstor;
    }

    public Kancelarija getKancelarija() {
        return kancelarija;
    }

    public void setKancelarija(Kancelarija kancelarija) {
        this.kancelarija = kancelarija;
    }

    public KonferencijskaSala getSala() {
        return sala;
    }

    public void setSala(KonferencijskaSala sala) {
        this.sala = sala;
    }

    public LocalDateTime getDatumOd() {
        return datumOd;
    }

    public void setDatumOd(LocalDateTime datumOd) {
        this.datumOd = datumOd;
    }

    public LocalDateTime getDatumDo() {
        return datumDo;
    }

    public void setDatumDo(LocalDateTime datumDo) {
        this.datumDo = datumDo;
    }

    public StatusRezervacije getStatus() {
        return status;
    }

    public void setStatus(StatusRezervacije status) {
        this.status = status;
    }

    public LocalDateTime getKreirano() {
        return kreirano;
    }

    public void setKreirano(LocalDateTime kreirano) {
        this.kreirano = kreirano;
    }

    public LocalDateTime getAzurirano() {
        return azurirano;
    }

    public void setAzurirano(LocalDateTime azurirano) {
        this.azurirano = azurirano;
    }
}
