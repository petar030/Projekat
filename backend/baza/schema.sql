-- =============================================================================
-- Coworking Hub Manager - Sema baze podataka
-- MySQL 8.0+
-- =============================================================================

DROP DATABASE IF EXISTS coworking_hub;
CREATE DATABASE coworking_hub CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE coworking_hub;

-- =============================================================================
-- 1. FIRME (podaci o firmama menadzera)
-- =============================================================================
CREATE TABLE firme (
    id              BIGINT          AUTO_INCREMENT PRIMARY KEY,
    naziv           VARCHAR(255)    NOT NULL,
    adresa          VARCHAR(500)    NOT NULL,
    maticni_broj    CHAR(8)         NOT NULL,
    pib             CHAR(9)         NOT NULL,
    kreirano        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    azurirano       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT uq_firma_maticni     UNIQUE (maticni_broj),
    CONSTRAINT uq_firma_pib         UNIQUE (pib),
    CONSTRAINT chk_maticni_format   CHECK (maticni_broj REGEXP '^[0-9]{8}$'),
    CONSTRAINT chk_pib_format       CHECK (pib REGEXP '^[1-9][0-9]{8}$')
) ENGINE=InnoDB;

-- =============================================================================
-- 2. KORISNICI (svi korisnici: clan, menadzer, admin)
-- =============================================================================
CREATE TABLE korisnici (
    id              BIGINT          AUTO_INCREMENT PRIMARY KEY,
    korisnicko_ime  VARCHAR(100)    NOT NULL,
    lozinka         VARCHAR(255)    NOT NULL,       -- bcrypt hash
    ime             VARCHAR(100)    NOT NULL,
    prezime         VARCHAR(100)    NOT NULL,
    telefon         VARCHAR(30)     NOT NULL,
    email           VARCHAR(255)    NOT NULL,
    profilna_slika  VARCHAR(500)    DEFAULT '/uploads/profiles/default-profile.png',
    uloga           ENUM('clan', 'menadzer', 'admin') NOT NULL,
    status          ENUM('na_cekanju', 'odobren', 'odbijen') NOT NULL DEFAULT 'na_cekanju',
    firma_id        BIGINT          NULL,           -- samo za menadzere
    kreirano        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    azurirano       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT uq_korisnik_ime      UNIQUE (korisnicko_ime),
    CONSTRAINT uq_korisnik_email    UNIQUE (email),
    CONSTRAINT fk_korisnik_firma    FOREIGN KEY (firma_id) REFERENCES firme(id)
                                    ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE INDEX idx_korisnici_uloga     ON korisnici(uloga);
CREATE INDEX idx_korisnici_status    ON korisnici(status);
CREATE INDEX idx_korisnici_firma     ON korisnici(firma_id);

-- ---------------------------------------------------------------------------
-- T0: Menadzer mora imati firmu; ostali ne smeju (INSERT/UPDATE)
-- ---------------------------------------------------------------------------
DELIMITER //
CREATE TRIGGER trg_korisnik_uloga_firma_insert
BEFORE INSERT ON korisnici
FOR EACH ROW
BEGIN
    IF (NEW.uloga = 'menadzer' AND NEW.firma_id IS NULL)
       OR (NEW.uloga <> 'menadzer' AND NEW.firma_id IS NOT NULL) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Neispravna kombinacija uloge i firme: menadzer mora imati firmu, ostali ne smeju.';
    END IF;
END //
DELIMITER ;

DELIMITER //
CREATE TRIGGER trg_korisnik_uloga_firma_update
BEFORE UPDATE ON korisnici
FOR EACH ROW
BEGIN
    IF (NEW.uloga = 'menadzer' AND NEW.firma_id IS NULL)
       OR (NEW.uloga <> 'menadzer' AND NEW.firma_id IS NOT NULL) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Neispravna kombinacija uloge i firme: menadzer mora imati firmu, ostali ne smeju.';
    END IF;
END //
DELIMITER ;

-- =============================================================================
-- 3. PROSTORI (coworking prostori)
-- =============================================================================
CREATE TABLE prostori (
    id                  BIGINT          AUTO_INCREMENT PRIMARY KEY,
    naziv               VARCHAR(255)    NOT NULL,
    grad                VARCHAR(150)    NOT NULL,
    adresa              VARCHAR(500)    NOT NULL,
    opis                TEXT            NULL,
    cena_po_satu        DECIMAL(10, 2)  NOT NULL,
    firma_id            BIGINT          NOT NULL,
    menadzer_id         BIGINT          NOT NULL,
    status              ENUM('na_cekanju', 'odobren', 'odbijen') NOT NULL DEFAULT 'na_cekanju',
    prag_kazni          INT             NOT NULL DEFAULT 3,
    geografska_sirina   DECIMAL(10, 7)  NULL,
    geografska_duzina   DECIMAL(10, 7)  NULL,
    kreirano            DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    azurirano           DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_prostor_firma     FOREIGN KEY (firma_id) REFERENCES firme(id)
                                    ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_prostor_menadzer  FOREIGN KEY (menadzer_id) REFERENCES korisnici(id)
                                    ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT chk_cena_pozitivna   CHECK (cena_po_satu > 0),
    CONSTRAINT chk_prag_pozitivan   CHECK (prag_kazni >= 1)
) ENGINE=InnoDB;

CREATE INDEX idx_prostori_grad       ON prostori(grad);
CREATE INDEX idx_prostori_status     ON prostori(status);
CREATE INDEX idx_prostori_firma      ON prostori(firma_id);
CREATE INDEX idx_prostori_menadzer   ON prostori(menadzer_id);

-- =============================================================================
-- 4. SLIKE_PROSTORA (slike: 1 glavna + do 5 thumbnail-a = max 6)
-- =============================================================================
CREATE TABLE slike_prostora (
    id              BIGINT          AUTO_INCREMENT PRIMARY KEY,
    prostor_id      BIGINT          NOT NULL,
    putanja_slike   VARCHAR(500)    NOT NULL,
    glavna          BOOLEAN         NOT NULL DEFAULT FALSE,
    redosled        INT             NOT NULL DEFAULT 0,
    kreirano        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_slika_prostor     FOREIGN KEY (prostor_id) REFERENCES prostori(id)
                                    ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE INDEX idx_slike_prostor       ON slike_prostora(prostor_id);

-- =============================================================================
-- 5. OTVORENI_PROSTORI (tacno 1 po prostoru, min 5 stolova)
-- =============================================================================
CREATE TABLE otvoreni_prostori (
    id              BIGINT          AUTO_INCREMENT PRIMARY KEY,
    prostor_id      BIGINT          NOT NULL,
    broj_stolova    INT             NOT NULL,
    kreirano        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_otvoreni_po_prostoru  UNIQUE (prostor_id),
    CONSTRAINT fk_otvoreni_prostor      FOREIGN KEY (prostor_id) REFERENCES prostori(id)
                                        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT chk_min_stolova          CHECK (broj_stolova >= 5)
) ENGINE=InnoDB;

-- =============================================================================
-- 6. KANCELARIJE (vise po prostoru, jedinstven naziv u okviru prostora)
-- =============================================================================
CREATE TABLE kancelarije (
    id              BIGINT          AUTO_INCREMENT PRIMARY KEY,
    prostor_id      BIGINT          NOT NULL,
    naziv           VARCHAR(255)    NOT NULL,
    broj_stolova    INT             NOT NULL,
    kreirano        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_naziv_kancelarije     UNIQUE (prostor_id, naziv),
    CONSTRAINT fk_kancelarija_prostor   FOREIGN KEY (prostor_id) REFERENCES prostori(id)
                                        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT chk_stolovi_kancelarija  CHECK (broj_stolova >= 1)
) ENGINE=InnoDB;

CREATE INDEX idx_kancelarije_prostor ON kancelarije(prostor_id);

-- =============================================================================
-- 7. KONFERENCIJSKE_SALE (10-12 mesta, oprema do 300 znakova)
-- =============================================================================
CREATE TABLE konferencijske_sale (
    id                  BIGINT          AUTO_INCREMENT PRIMARY KEY,
    prostor_id          BIGINT          NOT NULL,
    naziv               VARCHAR(255)    NOT NULL,
    broj_mesta          INT             NOT NULL DEFAULT 10,
    dodatna_oprema      VARCHAR(300)    NULL,
    kreirano            DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_naziv_sale            UNIQUE (prostor_id, naziv),
    CONSTRAINT fk_sala_prostor          FOREIGN KEY (prostor_id) REFERENCES prostori(id)
                                        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT chk_mesta_sale           CHECK (broj_mesta >= 10 AND broj_mesta <= 12)
) ENGINE=InnoDB;

CREATE INDEX idx_sale_prostor        ON konferencijske_sale(prostor_id);

-- =============================================================================
-- 8. REZERVACIJE (polimorfna veza ka elementu prostora)
-- =============================================================================
CREATE TABLE rezervacije (
    id                      BIGINT      AUTO_INCREMENT PRIMARY KEY,
    clan_id                 BIGINT      NOT NULL,
    prostor_id              BIGINT      NOT NULL,
    -- Polimorfna veza: tacno jedno od ova tri mora biti popunjeno
    otvoreni_prostor_id     BIGINT      NULL,
    kancelarija_id          BIGINT      NULL,
    sala_id                 BIGINT      NULL,
    datum_od                DATETIME    NOT NULL,
    datum_do                DATETIME    NOT NULL,
    status                  ENUM('aktivna', 'potvrdjena', 'otkazana', 'nepojavljivanje')
                                        NOT NULL DEFAULT 'aktivna',
    kreirano                DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    azurirano               DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_rezervacija_clan      FOREIGN KEY (clan_id) REFERENCES korisnici(id)
                                        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_rezervacija_prostor   FOREIGN KEY (prostor_id) REFERENCES prostori(id)
                                        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_rezervacija_otvoreni  FOREIGN KEY (otvoreni_prostor_id) REFERENCES otvoreni_prostori(id)
                                        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_rezervacija_kancelarija FOREIGN KEY (kancelarija_id) REFERENCES kancelarije(id)
                                        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_rezervacija_sala      FOREIGN KEY (sala_id) REFERENCES konferencijske_sale(id)
                                        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT chk_datum_opseg          CHECK (datum_do > datum_od)
) ENGINE=InnoDB;

CREATE INDEX idx_rez_clan            ON rezervacije(clan_id);
CREATE INDEX idx_rez_prostor         ON rezervacije(prostor_id);
CREATE INDEX idx_rez_datumi          ON rezervacije(datum_od, datum_do);
CREATE INDEX idx_rez_status          ON rezervacije(status);
CREATE INDEX idx_rez_otvoreni        ON rezervacije(otvoreni_prostor_id);
CREATE INDEX idx_rez_kancelarija     ON rezervacije(kancelarija_id);
CREATE INDEX idx_rez_sala            ON rezervacije(sala_id);

-- ---------------------------------------------------------------------------
-- T0b: Tacno jedan element rezervacije mora biti referenciran (INSERT/UPDATE)
-- ---------------------------------------------------------------------------
DELIMITER //
CREATE TRIGGER trg_rezervacija_tacno_jedan_element_insert
BEFORE INSERT ON rezervacije
FOR EACH ROW
BEGIN
    IF NOT (
        (NEW.otvoreni_prostor_id IS NOT NULL AND NEW.kancelarija_id IS NULL     AND NEW.sala_id IS NULL)
        OR (NEW.otvoreni_prostor_id IS NULL  AND NEW.kancelarija_id IS NOT NULL AND NEW.sala_id IS NULL)
        OR (NEW.otvoreni_prostor_id IS NULL  AND NEW.kancelarija_id IS NULL     AND NEW.sala_id IS NOT NULL)
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Neispravna rezervacija: mora biti izabran tacno jedan element (otvoreni prostor, kancelarija ili sala).';
    END IF;
END //
DELIMITER ;

DELIMITER //
CREATE TRIGGER trg_rezervacija_tacno_jedan_element_update
BEFORE UPDATE ON rezervacije
FOR EACH ROW
BEGIN
    IF NOT (
        (NEW.otvoreni_prostor_id IS NOT NULL AND NEW.kancelarija_id IS NULL     AND NEW.sala_id IS NULL)
        OR (NEW.otvoreni_prostor_id IS NULL  AND NEW.kancelarija_id IS NOT NULL AND NEW.sala_id IS NULL)
        OR (NEW.otvoreni_prostor_id IS NULL  AND NEW.kancelarija_id IS NULL     AND NEW.sala_id IS NOT NULL)
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Neispravna rezervacija: mora biti izabran tacno jedan element (otvoreni prostor, kancelarija ili sala).';
    END IF;
END //
DELIMITER ;

-- =============================================================================
-- 9. REAKCIJE (svidjanje / nesvidjanje prostora)
-- =============================================================================
CREATE TABLE reakcije (
    id              BIGINT          AUTO_INCREMENT PRIMARY KEY,
    clan_id         BIGINT          NOT NULL,
    prostor_id      BIGINT          NOT NULL,
    tip             ENUM('svidjanje', 'nesvidjanje') NOT NULL,
    kreirano        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_reakcija_clan     FOREIGN KEY (clan_id) REFERENCES korisnici(id)
                                    ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_reakcija_prostor  FOREIGN KEY (prostor_id) REFERENCES prostori(id)
                                    ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE INDEX idx_reakcije_clan_prostor ON reakcije(clan_id, prostor_id);

-- =============================================================================
-- 10. KOMENTARI (komentari clanova o prostorima)
-- =============================================================================
CREATE TABLE komentari (
    id              BIGINT          AUTO_INCREMENT PRIMARY KEY,
    clan_id         BIGINT          NOT NULL,
    prostor_id      BIGINT          NOT NULL,
    sadrzaj         TEXT            NOT NULL,
    kreirano        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_komentar_clan     FOREIGN KEY (clan_id) REFERENCES korisnici(id)
                                    ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_komentar_prostor  FOREIGN KEY (prostor_id) REFERENCES prostori(id)
                                    ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE INDEX idx_komentari_clan_prostor ON komentari(clan_id, prostor_id);
CREATE INDEX idx_komentari_kreirano     ON komentari(kreirano DESC);

-- =============================================================================
-- 11. KAZNE (kazneni prekrsaji za nepojavljivanje)
-- =============================================================================
CREATE TABLE kazne (
    id              BIGINT          AUTO_INCREMENT PRIMARY KEY,
    clan_id         BIGINT          NOT NULL,
    prostor_id      BIGINT          NOT NULL,
    rezervacija_id  BIGINT          NOT NULL,
    kreirano        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_kazna_clan        FOREIGN KEY (clan_id) REFERENCES korisnici(id)
                                    ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_kazna_prostor     FOREIGN KEY (prostor_id) REFERENCES prostori(id)
                                    ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_kazna_rezervacija FOREIGN KEY (rezervacija_id) REFERENCES rezervacije(id)
                                    ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT uq_kazna_rezervacija UNIQUE (rezervacija_id)     -- 1 kazna po rezervaciji
) ENGINE=InnoDB;

CREATE INDEX idx_kazne_clan_prostor  ON kazne(clan_id, prostor_id);

-- =============================================================================
-- 12. TOKENI_ZA_RESET_LOZINKE (privremeni linkovi, 30 min)
-- =============================================================================
CREATE TABLE tokeni_za_reset_lozinke (
    id              BIGINT          AUTO_INCREMENT PRIMARY KEY,
    korisnik_id     BIGINT          NOT NULL,
    token           VARCHAR(255)    NOT NULL,
    istice          DATETIME        NOT NULL,
    iskoriscen      BOOLEAN         NOT NULL DEFAULT FALSE,
    kreirano        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_reset_token       UNIQUE (token),
    CONSTRAINT fk_reset_korisnik    FOREIGN KEY (korisnik_id) REFERENCES korisnici(id)
                                    ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE INDEX idx_reset_token         ON tokeni_za_reset_lozinke(token);
CREATE INDEX idx_reset_istice        ON tokeni_za_reset_lozinke(istice);

-- =============================================================================
-- TRIGERI
-- =============================================================================

-- ---------------------------------------------------------------------------
-- T1: Maksimalno 2 odobrena menadzera po firmi (BEFORE INSERT)
-- ---------------------------------------------------------------------------
DELIMITER //
CREATE TRIGGER trg_max_menadzera_insert
BEFORE INSERT ON korisnici
FOR EACH ROW
BEGIN
    DECLARE broj_menadzera INT;
    IF NEW.uloga = 'menadzer' AND NEW.firma_id IS NOT NULL AND NEW.status = 'odobren' THEN
        SELECT COUNT(*) INTO broj_menadzera
        FROM korisnici
        WHERE firma_id = NEW.firma_id
          AND uloga = 'menadzer'
          AND status = 'odobren';
        IF broj_menadzera >= 2 THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'Firma vec ima maksimalan broj menadzera (2).';
        END IF;
    END IF;
END //
DELIMITER ;

-- ---------------------------------------------------------------------------
-- T2: Maksimalno 2 odobrena menadzera po firmi (BEFORE UPDATE - odobrenje)
-- ---------------------------------------------------------------------------
DELIMITER //
CREATE TRIGGER trg_max_menadzera_update
BEFORE UPDATE ON korisnici
FOR EACH ROW
BEGIN
    DECLARE broj_menadzera INT;
    IF NEW.uloga = 'menadzer' AND NEW.firma_id IS NOT NULL AND NEW.status = 'odobren'
       AND (OLD.status != 'odobren' OR OLD.firma_id != NEW.firma_id) THEN
        SELECT COUNT(*) INTO broj_menadzera
        FROM korisnici
        WHERE firma_id = NEW.firma_id
          AND uloga = 'menadzer'
          AND status = 'odobren'
          AND id != NEW.id;
        IF broj_menadzera >= 2 THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'Firma vec ima maksimalan broj menadzera (2).';
        END IF;
    END IF;
END //
DELIMITER ;

-- ---------------------------------------------------------------------------
-- T3: Reakcija (like/dislike) - clan mora imati potvrdjenu rezervaciju,
--     i broj reakcija ne sme preci broj potvrdjenih rezervacija
-- ---------------------------------------------------------------------------
DELIMITER //
CREATE TRIGGER trg_provera_reakcije
BEFORE INSERT ON reakcije
FOR EACH ROW
BEGIN
    DECLARE broj_potvrdjenih INT;
    DECLARE broj_reakcija INT;

    -- Broj potvrdjenih rezervacija clana u tom prostoru
    SELECT COUNT(*) INTO broj_potvrdjenih
    FROM rezervacije
    WHERE clan_id = NEW.clan_id
      AND prostor_id = NEW.prostor_id
      AND status = 'potvrdjena';

    IF broj_potvrdjenih = 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Clan mora imati barem jednu potvrdjenu rezervaciju da bi ostavio reakciju.';
    END IF;

    -- Postojeci broj reakcija clana za taj prostor
    SELECT COUNT(*) INTO broj_reakcija
    FROM reakcije
    WHERE clan_id = NEW.clan_id
      AND prostor_id = NEW.prostor_id;

    IF broj_reakcija >= broj_potvrdjenih THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Broj reakcija ne sme biti veci od broja potvrdjenih rezervacija.';
    END IF;
END //
DELIMITER ;

-- ---------------------------------------------------------------------------
-- T4: Komentar - isto pravilo kao za reakcije
-- ---------------------------------------------------------------------------
DELIMITER //
CREATE TRIGGER trg_provera_komentara
BEFORE INSERT ON komentari
FOR EACH ROW
BEGIN
    DECLARE broj_potvrdjenih INT;
    DECLARE broj_komentara INT;

    SELECT COUNT(*) INTO broj_potvrdjenih
    FROM rezervacije
    WHERE clan_id = NEW.clan_id
      AND prostor_id = NEW.prostor_id
      AND status = 'potvrdjena';

    IF broj_potvrdjenih = 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Clan mora imati barem jednu potvrdjenu rezervaciju da bi ostavio komentar.';
    END IF;

    SELECT COUNT(*) INTO broj_komentara
    FROM komentari
    WHERE clan_id = NEW.clan_id
      AND prostor_id = NEW.prostor_id;

    IF broj_komentara >= broj_potvrdjenih THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Broj komentara ne sme biti veci od broja potvrdjenih rezervacija.';
    END IF;
END //
DELIMITER ;

-- ---------------------------------------------------------------------------
-- T5: Automatsko kreiranje kazne kada menadzer oznaci nepojavljivanje
-- ---------------------------------------------------------------------------
DELIMITER //
CREATE TRIGGER trg_kreiranje_kazne_nepojavljivanje
AFTER UPDATE ON rezervacije
FOR EACH ROW
BEGIN
    IF NEW.status = 'nepojavljivanje' AND OLD.status != 'nepojavljivanje' THEN
        INSERT INTO kazne (clan_id, prostor_id, rezervacija_id)
        VALUES (NEW.clan_id, NEW.prostor_id, NEW.id);
    END IF;
END //
DELIMITER ;

-- ---------------------------------------------------------------------------
-- T6: Provera da li je clan banovan pre kreiranja rezervacije
-- ---------------------------------------------------------------------------
DELIMITER //
CREATE TRIGGER trg_provera_ban_clana
BEFORE INSERT ON rezervacije
FOR EACH ROW
BEGIN
    DECLARE broj_kazni INT;
    DECLARE prag INT;

    SELECT p.prag_kazni INTO prag
    FROM prostori p
    WHERE p.id = NEW.prostor_id;

    SELECT COUNT(*) INTO broj_kazni
    FROM kazne
    WHERE clan_id = NEW.clan_id
      AND prostor_id = NEW.prostor_id;

    IF broj_kazni >= prag THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Clan je banovan iz ovog prostora zbog prekoracenja broja kaznenih prekrsaja.';
    END IF;
END //
DELIMITER ;

-- ---------------------------------------------------------------------------
-- T7: Ogranicenje broja slika po prostoru (max 6: 1 glavna + 5 thumbnail)
-- ---------------------------------------------------------------------------
DELIMITER //
CREATE TRIGGER trg_max_slika_prostora
BEFORE INSERT ON slike_prostora
FOR EACH ROW
BEGIN
    DECLARE broj_slika INT;

    SELECT COUNT(*) INTO broj_slika
    FROM slike_prostora
    WHERE prostor_id = NEW.prostor_id;

    IF broj_slika >= 6 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Prostor moze imati najvise 6 slika (1 glavna + 5 thumbnail).';
    END IF;
END //
DELIMITER ;

-- =============================================================================
-- POCETNI PODACI: Podrazumevani admin nalog
-- =============================================================================
-- Lozinka: Admin123! (bcrypt hash)
INSERT INTO korisnici (korisnicko_ime, lozinka, ime, prezime, telefon, email, uloga, status)
VALUES (
    'admin',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    'System',
    'Administrator',
    '+381600000000',
    'admin@coworkinghub.rs',
    'admin',
    'odobren'
);
