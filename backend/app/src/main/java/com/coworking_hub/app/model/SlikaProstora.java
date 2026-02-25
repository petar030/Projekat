package com.coworking_hub.app.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "slike_prostora")
public class SlikaProstora {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prostor_id", nullable = false)
    private Prostor prostor;

    @Column(name = "putanja_slike", nullable = false, length = 500)
    private String putanjaSlike;

    @Column(nullable = false)
    private Boolean glavna;

    @Column(nullable = false)
    private Integer redosled;

    @Column(nullable = false, updatable = false)
    private LocalDateTime kreirano;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Prostor getProstor() {
        return prostor;
    }

    public void setProstor(Prostor prostor) {
        this.prostor = prostor;
    }

    public String getPutanjaSlike() {
        return putanjaSlike;
    }

    public void setPutanjaSlike(String putanjaSlike) {
        this.putanjaSlike = putanjaSlike;
    }

    public Boolean getGlavna() {
        return glavna;
    }

    public void setGlavna(Boolean glavna) {
        this.glavna = glavna;
    }

    public Integer getRedosled() {
        return redosled;
    }

    public void setRedosled(Integer redosled) {
        this.redosled = redosled;
    }

    public LocalDateTime getKreirano() {
        return kreirano;
    }

    public void setKreirano(LocalDateTime kreirano) {
        this.kreirano = kreirano;
    }
}
