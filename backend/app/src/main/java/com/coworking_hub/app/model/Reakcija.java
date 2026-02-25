package com.coworking_hub.app.model;

import com.coworking_hub.app.model.enums.TipReakcije;
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
@Table(name = "reakcije")
public class Reakcija {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clan_id", nullable = false)
    private Korisnik clan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prostor_id", nullable = false)
    private Prostor prostor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipReakcije tip;

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

    public TipReakcije getTip() {
        return tip;
    }

    public void setTip(TipReakcije tip) {
        this.tip = tip;
    }

    public LocalDateTime getKreirano() {
        return kreirano;
    }

    public void setKreirano(LocalDateTime kreirano) {
        this.kreirano = kreirano;
    }
}
