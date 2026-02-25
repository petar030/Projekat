package com.coworking_hub.app.model;

import com.coworking_hub.app.model.enums.StatusProstora;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "prostori")
public class Prostor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String naziv;

    @Column(nullable = false, length = 150)
    private String grad;

    @Column(nullable = false, length = 500)
    private String adresa;

    @Column(columnDefinition = "TEXT")
    private String opis;

    @Column(name = "cena_po_satu", nullable = false, precision = 10, scale = 2)
    private BigDecimal cenaPoSatu;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "firma_id", nullable = false)
    private Firma firma;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menadzer_id", nullable = false)
    private Korisnik menadzer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusProstora status;

    @Column(name = "prag_kazni", nullable = false)
    private Integer pragKazni;

    @Column(name = "geografska_sirina", precision = 10, scale = 7)
    private BigDecimal geografskaSirina;

    @Column(name = "geografska_duzina", precision = 10, scale = 7)
    private BigDecimal geografskaDuzina;

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

    public String getNaziv() {
        return naziv;
    }

    public void setNaziv(String naziv) {
        this.naziv = naziv;
    }

    public String getGrad() {
        return grad;
    }

    public void setGrad(String grad) {
        this.grad = grad;
    }

    public String getAdresa() {
        return adresa;
    }

    public void setAdresa(String adresa) {
        this.adresa = adresa;
    }

    public String getOpis() {
        return opis;
    }

    public void setOpis(String opis) {
        this.opis = opis;
    }

    public BigDecimal getCenaPoSatu() {
        return cenaPoSatu;
    }

    public void setCenaPoSatu(BigDecimal cenaPoSatu) {
        this.cenaPoSatu = cenaPoSatu;
    }

    public Firma getFirma() {
        return firma;
    }

    public void setFirma(Firma firma) {
        this.firma = firma;
    }

    public Korisnik getMenadzer() {
        return menadzer;
    }

    public void setMenadzer(Korisnik menadzer) {
        this.menadzer = menadzer;
    }

    public StatusProstora getStatus() {
        return status;
    }

    public void setStatus(StatusProstora status) {
        this.status = status;
    }

    public Integer getPragKazni() {
        return pragKazni;
    }

    public void setPragKazni(Integer pragKazni) {
        this.pragKazni = pragKazni;
    }

    public BigDecimal getGeografskaSirina() {
        return geografskaSirina;
    }

    public void setGeografskaSirina(BigDecimal geografskaSirina) {
        this.geografskaSirina = geografskaSirina;
    }

    public BigDecimal getGeografskaDuzina() {
        return geografskaDuzina;
    }

    public void setGeografskaDuzina(BigDecimal geografskaDuzina) {
        this.geografskaDuzina = geografskaDuzina;
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
