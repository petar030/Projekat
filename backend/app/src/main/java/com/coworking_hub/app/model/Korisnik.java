package com.coworking_hub.app.model;

import com.coworking_hub.app.model.enums.StatusKorisnika;
import com.coworking_hub.app.model.enums.Uloga;
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
@Table(name = "korisnici")
public class Korisnik {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "korisnicko_ime", nullable = false, length = 100)
    private String korisnickoIme;

    @Column(nullable = false, length = 255)
    private String lozinka;

    @Column(nullable = false, length = 100)
    private String ime;

    @Column(nullable = false, length = 100)
    private String prezime;

    @Column(nullable = false, length = 30)
    private String telefon;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(name = "profilna_slika", length = 500)
    private String profilnaSlika;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Uloga uloga;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusKorisnika status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "firma_id")
    private Firma firma;

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

    public String getKorisnickoIme() {
        return korisnickoIme;
    }

    public void setKorisnickoIme(String korisnickoIme) {
        this.korisnickoIme = korisnickoIme;
    }

    public String getLozinka() {
        return lozinka;
    }

    public void setLozinka(String lozinka) {
        this.lozinka = lozinka;
    }

    public String getIme() {
        return ime;
    }

    public void setIme(String ime) {
        this.ime = ime;
    }

    public String getPrezime() {
        return prezime;
    }

    public void setPrezime(String prezime) {
        this.prezime = prezime;
    }

    public String getTelefon() {
        return telefon;
    }

    public void setTelefon(String telefon) {
        this.telefon = telefon;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getProfilnaSlika() {
        return profilnaSlika;
    }

    public void setProfilnaSlika(String profilnaSlika) {
        this.profilnaSlika = profilnaSlika;
    }

    public Uloga getUloga() {
        return uloga;
    }

    public void setUloga(Uloga uloga) {
        this.uloga = uloga;
    }

    public StatusKorisnika getStatus() {
        return status;
    }

    public void setStatus(StatusKorisnika status) {
        this.status = status;
    }

    public Firma getFirma() {
        return firma;
    }

    public void setFirma(Firma firma) {
        this.firma = firma;
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
