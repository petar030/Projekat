package com.coworking_hub.app.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "komentari")
public class Komentar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clan_id", nullable = false)
    private Korisnik clan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prostor_id", nullable = false)
    private Prostor prostor;

    @Lob
    @Column(nullable = false)
    private String sadrzaj;

    @Column(nullable = false, updatable = false)
    private LocalDateTime kreirano;

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

    public String getSadrzaj() {
        return sadrzaj;
    }

    public void setSadrzaj(String sadrzaj) {
        this.sadrzaj = sadrzaj;
    }

    public LocalDateTime getKreirano() {
        return kreirano;
    }

    public void setKreirano(LocalDateTime kreirano) {
        this.kreirano = kreirano;
    }
}
