package com.coworking_hub.app.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "firme")
public class Firma {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String naziv;

    @Column(nullable = false, length = 500)
    private String adresa;

    @Column(name = "maticni_broj", nullable = false, length = 8)
    private String maticniBroj;

    @Column(nullable = false, length = 9)
    private String pib;

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

    public String getAdresa() {
        return adresa;
    }

    public void setAdresa(String adresa) {
        this.adresa = adresa;
    }

    public String getMaticniBroj() {
        return maticniBroj;
    }

    public void setMaticniBroj(String maticniBroj) {
        this.maticniBroj = maticniBroj;
    }

    public String getPib() {
        return pib;
    }

    public void setPib(String pib) {
        this.pib = pib;
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
