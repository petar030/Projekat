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
@Table(name = "konferencijske_sale")
public class KonferencijskaSala {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prostor_id", nullable = false)
    private Prostor prostor;

    @Column(nullable = false, length = 255)
    private String naziv;

    @Column(name = "broj_mesta", nullable = false)
    private Integer brojMesta;

    @Column(name = "dodatna_oprema", length = 300)
    private String dodatnaOprema;

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

    public String getNaziv() {
        return naziv;
    }

    public void setNaziv(String naziv) {
        this.naziv = naziv;
    }

    public Integer getBrojMesta() {
        return brojMesta;
    }

    public void setBrojMesta(Integer brojMesta) {
        this.brojMesta = brojMesta;
    }

    public String getDodatnaOprema() {
        return dodatnaOprema;
    }

    public void setDodatnaOprema(String dodatnaOprema) {
        this.dodatnaOprema = dodatnaOprema;
    }

    public LocalDateTime getKreirano() {
        return kreirano;
    }

    public void setKreirano(LocalDateTime kreirano) {
        this.kreirano = kreirano;
    }
}
