# Coworking Hub Manager - Mapiranje zahteva po nivoima implementacije

Ovaj dokument pokriva sve zahteve iz specifikacije projekta i za svaki
definise na kom nivou se izvrsava: **Constraint** (CHECK/UNIQUE/FK u bazi),
**Trigger** (MySQL trigger), **Backend-App** (Spring Boot logika),
**Frontend-App** (Angular validacija/UI).

Legenda:
- **C** = Constraint (baza)
- **T** = Trigger (baza)
- **B** = Backend-App (Spring Boot)
- **F** = Frontend-App (Angular)

---

## 1. Autentifikacija i registracija

| #    | Zahtev                                                                                       | C  | T  | B  | F  | Napomena                                                                                          |
|------|----------------------------------------------------------------------------------------------|:--:|:--:|:--:|:--:|---------------------------------------------------------------------------------------------------|
| 1.1  | Prijava korisnickim imenom i lozinkom                                                        |    |    | ✅ | ✅ | Backend autentifikacija (Spring Security + JWT); frontend forma sa validacijom                     |
| 1.2  | Poruka o pogresnim kredencijalima                                                            |    |    | ✅ | ✅ | Backend vraca 401; frontend prikazuje poruku                                                      |
| 1.3  | Zaboravljena lozinka - link za resetovanje                                                   |    |    | ✅ | ✅ | Backend generise token (30 min), salje email sa linkom; frontend forma za unos nove lozinke       |
| 1.4  | Token za reset istice za 30 minuta                                                           |    |    | ✅ |    | Backend proverava kolonu `istice` u tabeli `tokeni_za_reset_lozinke` pri koriscenju tokena        |
| 1.5  | Admin se prijavljuje na posebnoj ruti (nije javno vidljiva)                                  |    |    | ✅ | ✅ | Frontend: posebna ruta `/admin/login`; Backend: isti mehanizam autentifikacije                    |
| 1.6  | Korisnicko ime jedinstveno (na nivou svih korisnika)                                         | ✅ |    | ✅ | ✅ | `UNIQUE(korisnicko_ime)` constraint; backend provera pre insert-a; frontend async validacija      |
| 1.7  | Email jedinstven (najvise 1 nalog po email-u)                                                | ✅ |    | ✅ | ✅ | `UNIQUE(email)` constraint; backend provera pre insert-a; frontend async validacija               |
| 1.8  | Lozinka - regex (8-12 char, veliko slovo, broj, spec. znak, pocinje slovom)                  |    |    | ✅ | ✅ | Validacija regexom na frontu i backendu pre hashovanja. U bazi se cuva bcrypt hash.               |
| 1.9  | Lozinka se cuva kriptovana u bazi                                                            |    |    | ✅ |    | Spring Security `BCryptPasswordEncoder`                                                           |
| 1.10 | Registracija: unos svih obaveznih polja (ime, prezime, telefon, email, slika...)             |    |    | ✅ | ✅ | Frontend forma sa validacijom; Backend `@NotNull`/`@NotBlank` anotacije na entitetima             |
| 1.11 | Registracija ceka odobrenje administratora                                                   |    |    | ✅ | ✅ | Korisnik se kreira sa `status='na_cekanju'`; admin menja na `odobren`/`odbijen`                   |
| 1.12 | Profilna slika: min 100x100px, max 300x300px, JPG/PNG format                                |    |    | ✅ | ✅ | Frontend: provera dimenzija i tipa fajla pre upload-a; Backend: validacija velicine i formata     |
| 1.13 | Podrazumevana profilna slika ako korisnik ne doda svoju                                      | ✅ |    | ✅ |    | `DEFAULT 'default-profile.png'` u bazi; backend fallback logika                                   |
| 1.14 | Slika se unosi preko FileUpload-a (ne eksterni link)                                         |    |    | ✅ | ✅ | Frontend: `<input type="file">`; Backend: multipart upload endpoint                               |

---

## 2. Menadzer - dodatni podaci o firmi

| #   | Zahtev                                                                         | C  | T  | B  | F  | Napomena                                                                                     |
|-----|--------------------------------------------------------------------------------|:--:|:--:|:--:|:--:|----------------------------------------------------------------------------------------------|
| 2.1 | Naziv firme, adresa sedista                                                    |    |    | ✅ | ✅ | Backend: `@NotBlank`; Frontend: obavezna polja u formi za registraciju menadzera             |
| 2.2 | Maticni broj: tacno 8 cifara, jedinstven po firmi                              | ✅ |    | ✅ | ✅ | `CHECK(REGEXP '^[0-9]{8}$')` + `UNIQUE(maticni_broj)`; backend i frontend regex validacija   |
| 2.3 | PIB: 9 cifara, ne pocinje nulom, jedinstven                                   | ✅ |    | ✅ | ✅ | `CHECK(REGEXP '^[1-9][0-9]{8}$')` + `UNIQUE(pib)`; backend i frontend regex validacija       |
| 2.4 | Maksimalno 2 menadzera po firmi - treci se zabranjuje                          |    | ✅ | ✅ | ✅ | Trigeri `trg_max_menadzera_insert` i `trg_max_menadzera_update`; backend provera; frontend poruka |
| 2.5 | Menadzer mora imati firma_id; ostali korisnici ne smeju                        | ✅ |    | ✅ |    | `CHECK` constraint `chk_menadzer_firma` u tabeli `korisnici`                                 |

---

## 3. Neregistrovani korisnik - pocetna strana

| #   | Zahtev                                                                                | C  | T  | B  | F  | Napomena                                                                                      |
|-----|---------------------------------------------------------------------------------------|:--:|:--:|:--:|:--:|-----------------------------------------------------------------------------------------------|
| 3.1 | Opste informacije o ukupnom broju registrovanih prostora                              |    |    | ✅ | ✅ | Backend: API endpoint za statistiku `COUNT(*) FROM prostori WHERE status='odobren'`           |
| 3.2 | TOP 5 najbolje ocenjenih prostora (po broju lajkova/svidjanja)                        |    |    | ✅ | ✅ | Backend: query sa `COUNT(reakcije WHERE tip='svidjanje')` + `ORDER BY` + `LIMIT 5`           |
| 3.3 | Pretraga po nazivu prostora                                                           |    |    | ✅ | ✅ | Backend: `WHERE naziv LIKE '%..%'`; Frontend: input polje za unos teksta                     |
| 3.4 | Pretraga po gradu - padajuca lista sa mogucnoscu vise izbora                          |    |    | ✅ | ✅ | Backend: `WHERE grad IN (...)` filter; Frontend: multi-select dropdown                       |
| 3.5 | Padajuca lista prikazuje samo gradove sa aktivnim prostorima                          |    |    | ✅ | ✅ | Backend: `SELECT DISTINCT grad FROM prostori WHERE status='odobren'`                         |
| 3.6 | Rezultati pretrage u tabeli sa dugmetom "DETALJI"                                     |    |    | ✅ | ✅ | Backend: paginiran REST API; Frontend: tabela + routerLink na detalje                        |
| 3.7 | Abecedno sortiranje po nazivu prostora i gradu (rastuci/opadajuci)                   |    |    | ✅ | ✅ | Backend: `ORDER BY` sa parametrom smera; Frontend: klik na zaglavlje kolone                  |
| 3.8 | Detalji: naziv, grad, adresa, firma, menadzer, broj svidjanja/nesvidjanja             |    |    | ✅ | ✅ | Backend: JOIN query preko prostori/firme/korisnici/reakcije; Frontend: detaljna strana        |
| 3.9 | Galerija slika sa glavnom slikom i do 5 thumbnail-a                                   |    |    | ✅ | ✅ | Backend: vraca listu slika iz `slike_prostora`; Frontend: galerija komponenta sa uvecanjenjem |
| 3.10| Odabrana slika iz galerije se pamti u cookie-ju pregledaca                            |    |    |    | ✅ | Cisto frontend resenje - `document.cookie` ili `ngx-cookie-service`                          |

---

## 4. Clan mreze

| #    | Zahtev                                                                                    | C  | T  | B  | F  | Napomena                                                                                          |
|------|-------------------------------------------------------------------------------------------|:--:|:--:|:--:|:--:|---------------------------------------------------------------------------------------------------|
| 4.1  | Profil: pregled licnih podataka                                                           |    |    | ✅ | ✅ | Backend: `GET /api/korisnici/ja`; Frontend: profil strana                                         |
| 4.2  | Azuriranje podataka (zabrana promene korisnickog imena)                                   |    |    | ✅ | ✅ | Backend: PUT endpoint ignorise `korisnicko_ime`; Frontend: polje disabled                         |
| 4.3  | Mogucnost promene profilne slike                                                          |    |    | ✅ | ✅ | Backend: multipart upload; Frontend: file input sa preview-om                                     |
| 4.4  | Tabela svih rezervacija (prosle + aktuelne)                                               |    |    | ✅ | ✅ | Backend: `GET /api/rezervacije?clanId=X`; Frontend: tabela sa kolonama                            |
| 4.5  | Sortiranje rezervacija                                                                    |    |    | ✅ | ✅ | Backend: `ORDER BY` parametar; Frontend: klik na zaglavlje kolone                                 |
| 4.6  | Otkazivanje rezervacije samo 12+ sati pre pocetka                                        |    |    | ✅ | ✅ | Backend: provera `datum_od - NOW() >= 12h`; Frontend: disable dugme "Otkazi" ako je <12h         |
| 4.7  | Pretraga sa checkbox-ovima: radni sto / kancelarija / konferencijska sala                 |    |    | ✅ | ✅ | Backend: filter po tipu elementa; Frontend: checkbox logika (odabir jednog disabluje ostale)      |
| 4.8  | Numericko polje za velicinu kancelarije (za koliko osoba)                                 |    |    | ✅ | ✅ | Backend: filter `kancelarije.broj_stolova >= X`; Frontend: input sa uslovnom vidljivoscu          |
| 4.9  | Detalji sa duzim opisom, galerijom slika i dinamickom mapom                               |    |    | ✅ | ✅ | Backend: prosireni detalji + koordinate; Frontend: Leaflet/Google Maps integracija                |
| 4.10 | Interaktivni kalendar za prikaz zauzetosti (nedeljni prikaz)                              |    |    | ✅ | ✅ | Backend: API za zauzetost po danima; Frontend: FullCalendar komponenta                            |
| 4.11 | Rotacija kalendara za vise kancelarija/sala (levo/desno dugmici)                          |    |    |    | ✅ | Frontend: navigacija kroz kalendare sa prikazom jednog u isto vreme                               |
| 4.12 | Rezervacija prostora (dan + vremenski period)                                             |    |    | ✅ | ✅ | Backend: kreiranje rezervacije + provera preklapanja u transakciji; Frontend: forma/kalendar      |
| 4.13 | Like/Dislike samo ako clan ima barem 1 potvrdjenu rezervaciju                             |    | ✅ | ✅ | ✅ | Triger `trg_provera_reakcije`; Backend dupla provera; Frontend: sakri dugme ako nema pravo        |
| 4.14 | Broj reakcija (svidjanje/nesvidjanje) <= broj potvrdjenih rezervacija                     |    | ✅ | ✅ |    | Triger `trg_provera_reakcije`; Backend provera pre insert-a                                       |
| 4.15 | Komentar samo ako clan ima barem 1 potvrdjenu rezervaciju                                 |    | ✅ | ✅ | ✅ | Triger `trg_provera_komentara`; Backend provera; Frontend: sakri formu za komentar                |
| 4.16 | Broj komentara <= broj potvrdjenih rezervacija                                            |    | ✅ | ✅ |    | Triger `trg_provera_komentara`; Backend provera pre insert-a                                      |
| 4.17 | Prikaz poslednjih 10 komentara svih clanova                                               |    |    | ✅ | ✅ | Backend: `ORDER BY kreirano DESC LIMIT 10`; Frontend: lista komentara                             |
| 4.18 | Sopstveni komentari uokvireni diskretnom linijom u boji                                   |    |    |    | ✅ | Frontend: CSS uslovni stil na osnovu `clan_id == trenutniKorisnik.id`                             |

---

## 5. Menadzer prostora

| #    | Zahtev                                                                                    | C  | T  | B  | F  | Napomena                                                                                          |
|------|-------------------------------------------------------------------------------------------|:--:|:--:|:--:|:--:|---------------------------------------------------------------------------------------------------|
| 5.1  | Profil: pregled i azuriranje licnih podataka (zabrana promene korisnickog imena)           |    |    | ✅ | ✅ | Isto kao za clana, plus prikaz podataka o firmi                                                   |
| 5.2  | Mogucnost promene profilne slike                                                          |    |    | ✅ | ✅ | Backend: multipart upload; Frontend: file input                                                   |
| 5.3  | Tabela svih prostora koje firma nudi                                                      |    |    | ✅ | ✅ | Backend: `GET /api/prostori?firmaId=X`; Frontend: tabela sa detaljima                             |
| 5.4  | Prikaz elemenata prostora (otvoreni prostor + kancelarije + sale)                         |    |    | ✅ | ✅ | Backend: nested JSON podaci; Frontend: prosirivi redovi u tabeli                                  |
| 5.5  | Kreiranje novog prostora sa definisanom cenom po satu                                     |    |    | ✅ | ✅ | Backend: `POST /api/prostori`; Frontend: visekroacna forma                                        |
| 5.6  | Tacno 1 otvoreni radni prostor pri kreiranju                                              | ✅ |    | ✅ | ✅ | `UNIQUE(prostor_id)` na `otvoreni_prostori`; backend obavezuje; frontend obavezno polje           |
| 5.7  | Otvoreni prostor: minimalno 5 stolova                                                     | ✅ |    | ✅ | ✅ | `CHECK(broj_stolova >= 5)` constraint; backend/frontend `min=5` validacija                        |
| 5.8  | Kancelarije: jedinstven naziv u okviru istog prostora                                     | ✅ |    | ✅ | ✅ | `UNIQUE(prostor_id, naziv)` constraint; backend provera; frontend validacija                      |
| 5.9  | Konferencijske sale: jedinstven naziv u okviru istog prostora                             | ✅ |    | ✅ | ✅ | `UNIQUE(prostor_id, naziv)` constraint; backend provera; frontend validacija                      |
| 5.10 | Konferencijske sale: 10-12 mesta (ne unosi se, podrazumeva se)                            | ✅ |    | ✅ | ✅ | `CHECK(broj_mesta >= 10 AND broj_mesta <= 12)`; backend default vrednost; frontend info           |
| 5.11 | Dodatna oprema konferencijske sale: max 300 karaktera                                     | ✅ |    | ✅ | ✅ | `VARCHAR(300)` constraint; backend `@Size(max=300)`; frontend `maxlength`                         |
| 5.12 | Definisanje broja kaznenih prekrsaja (prag) pri kreiranju prostora                        |    |    | ✅ | ✅ | Backend: sacuva `prag_kazni` u tabeli `prostori`; Frontend: numericko polje                       |
| 5.13 | Dodavanje prostora iz JSON fajla (brza konfiguracija)                                     |    |    | ✅ | ✅ | Backend: parsira JSON + primenjuje iste validacije; Frontend: file upload + preview                |
| 5.14 | Tabelarni prikaz svih rezervacija clanova u prostorima menadzera                          |    |    | ✅ | ✅ | Backend: `GET /api/rezervacije?menadzerId=X`; Frontend: tabela                                    |
| 5.15 | Dugme "Potvrdi" / "Odjavi" vidljivo 10 min nakon pocetka termina                         |    |    | ✅ | ✅ | Backend: provera `NOW() >= datum_od + 10min`; Frontend: uslovni prikaz dugmeta                    |
| 5.16 | Kazneni prekrsaj se automatski kreira pri oznacavanju nepojavljivanja                     |    | ✅ | ✅ |    | Triger `trg_kreiranje_kazne_nepojavljivanje`; Backend menja status na `nepojavljivanje`           |
| 5.17 | Ban clana kad prekoraci prag kaznenih prekrsaja                                           |    | ✅ | ✅ | ✅ | Triger `trg_provera_ban_clana`; Backend provera pre rezervacije; Frontend prikaz poruke o banu    |
| 5.18 | Interaktivni kalendar za pregled svih prostora i elemenata                                |    |    | ✅ | ✅ | Backend: kalendarski API po elementima; Frontend: FullCalendar + padajuca lista za izbor          |
| 5.19 | Drag-and-drop pomeranje termina u kancelarijama/salama                                    |    |    | ✅ | ✅ | Backend: `PUT /api/rezervacije/{id}/pomeri`; Frontend: FullCalendar eventDrop                     |
| 5.20 | Mesecni PDF izvestaj o popunjenosti kapaciteta                                            |    |    | ✅ | ✅ | Backend: generisanje PDF-a (iText/OpenPDF); Frontend: dugme za download                          |
| 5.21 | Novi prostor ceka odobrenje administratora pre nego postane vidljiv                       |    |    | ✅ | ✅ | Prostor se kreira sa `status='na_cekanju'`; admin menja status                                   |

---

## 6. Administrator sistema

| #   | Zahtev                                                                                    | C  | T  | B  | F  | Napomena                                                                                          |
|-----|-------------------------------------------------------------------------------------------|:--:|:--:|:--:|:--:|---------------------------------------------------------------------------------------------------|
| 6.1 | Pregled, azuriranje i brisanje svih korisnickih naloga                                    |    |    | ✅ | ✅ | Backend: CRUD API sa admin autorizacijom; Frontend: admin panel sa tabelom korisnika              |
| 6.2 | Tabelarni pregled svih neodobrenih korisnika (zahtevi za registraciju)                    |    |    | ✅ | ✅ | Backend: `GET /api/korisnici?status=na_cekanju`; Frontend: tabela sa akcijama                     |
| 6.3 | Prihvatanje ili odbacivanje zahteva za registraciju                                       |    |    | ✅ | ✅ | Backend: `PUT /api/korisnici/{id}/odobri` ili `/odbij`; Frontend: dugmici u tabeli                |
| 6.4 | Odobravanje novih prostora pre nego postanu vidljivi                                      |    |    | ✅ | ✅ | Backend: `PUT /api/prostori/{id}/odobri`; Frontend: admin panel sa listom prostora na cekanju     |
| 6.5 | Statistika - grafikon popularnosti prostora                                               |    |    | ✅ | ✅ | Backend: agregirani podaci (broj reakcija/rezervacija); Frontend: ng2-charts/ngx-charts           |
| 6.6 | Statistika - prihodi na nivou svakog prostora                                             |    |    | ✅ | ✅ | Backend: `SUM(cena_po_satu * sati)` po prostoru; Frontend: bar/pie chart                         |

---

## 7. Ostale karakteristike aplikacije

| #   | Zahtev                                                                                    | C  | T  | B  | F  | Napomena                                                                                          |
|-----|-------------------------------------------------------------------------------------------|:--:|:--:|:--:|:--:|---------------------------------------------------------------------------------------------------|
| 7.1 | Otpornost na unos nekorektnih podataka                                                    | ✅ | ✅ | ✅ | ✅ | Viseslojni pristup: CHECK/FK constraint-i + trigeri + backend validacija + frontend validacija     |
| 7.2 | Uniformni CSS izgled aplikacije                                                           |    |    |    | ✅ | Angular globalni stilovi / CSS framework (Bootstrap / Angular Material)                           |
| 7.3 | Svaka strana ima meni, header i footer                                                    |    |    |    | ✅ | Angular layout komponenta sa `<router-outlet>`                                                    |
| 7.4 | Opcija za povratak na pocetni ekran sa korisnickim opcijama                               |    |    |    | ✅ | Angular `routerLink` u meniju                                                                     |
| 7.5 | Logout link na svim ekranima                                                              |    |    | ✅ | ✅ | Backend: invalidacija JWT tokena; Frontend: brisanje tokena + redirect na login                   |
| 7.6 | Efikasna serverska validacija                                                             |    |    | ✅ |    | Spring Boot `@Valid`, custom validatori, `@ControllerAdvice` exception handleri                   |
| 7.7 | Responsive web design (prilagodljiv manjim i vecim ekranima)                              |    |    |    | ✅ | CSS media queries / Bootstrap grid / Angular Flex Layout                                         |
| 7.8 | Testiranje u najmanje 3 standardna web pregledaca                                         |    |    |    | ✅ | Chrome, Firefox, Edge                                                                             |

---

## Rezime: ukupan broj zahteva po nivoima

| Nivo                | Broj zahteva | Opis                                                                        |
|---------------------|:------------:|-----------------------------------------------------------------------------|
| **Constraint (C)**  | 14           | UNIQUE, CHECK, FK, DEFAULT - strukturalna pravila direktno u bazi podataka  |
| **Trigger (T)**     | 7            | Max menadzera, reakcije/komentari, kazneni prekrsaji, ban, max slika        |
| **Backend-App (B)** | 47           | Poslovna logika, autorizacija, validacija, REST API-ji, PDF generisanje     |
| **Frontend-App (F)** | 44          | UI forme, validacija, prikaz, routing, kalendar, mapa, grafikoni           |

> **Napomena:** Vecina zahteva se validira na **vise nivoa** istovremeno (defense in depth).
> Baza podataka je poslednja linija odbrane sa constraint-ima i trigerima,
> backend je primarni sloj poslovne logike i validacije,
> a frontend pruza korisnicko iskustvo sa instant povratnim informacijama.

---

## Pregled tabela u bazi (12 ukupno)

| Tabela                    | Svrha                                                                      |
|---------------------------|----------------------------------------------------------------------------|
| `firme`                   | Podaci o firmama menadzera (naziv, adresa, maticni broj, PIB)              |
| `korisnici`               | Svi korisnici sistema (clan / menadzer / admin) sa ulogom i statusom       |
| `prostori`                | Coworking prostori sa cenom, lokacijom, statusom odobrenja                 |
| `slike_prostora`          | Slike prostora (max 6: 1 glavna + 5 thumbnail)                            |
| `otvoreni_prostori`       | Otvoreni radni prostor (tacno 1 po prostoru, min 5 stolova)               |
| `kancelarije`             | Kancelarije u prostoru (jedinstven naziv, definisan broj stolova)          |
| `konferencijske_sale`     | Konferencijske sale (10-12 mesta, dodatna oprema do 300 znakova)           |
| `rezervacije`             | Rezervacije sa polimorfnom vezom ka elementu prostora                      |
| `reakcije`                | Reakcije clanova na prostore (svidjanje / nesvidjanje)                     |
| `komentari`               | Komentari clanova o prostorima                                             |
| `kazne`                   | Kazneni prekrsaji za nepojavljivanje na rezervaciji                        |
| `tokeni_za_reset_lozinke` | Privremeni tokeni za resetovanje lozinke (30 min trajanje)                 |

---

## Pregled trigera u bazi (7 ukupno)

| Triger                                 | Tabela        | Dogadjaj       | Svrha                                                           |
|----------------------------------------|---------------|----------------|-----------------------------------------------------------------|
| `trg_max_menadzera_insert`             | `korisnici`   | BEFORE INSERT  | Zabranjuje registraciju 3. menadzera iste firme                 |
| `trg_max_menadzera_update`             | `korisnici`   | BEFORE UPDATE  | Zabranjuje odobrenje 3. menadzera iste firme                    |
| `trg_provera_reakcije`                 | `reakcije`    | BEFORE INSERT  | Proverava da clan ima potvrdjenu rez. i da count <= rez. count  |
| `trg_provera_komentara`               | `komentari`   | BEFORE INSERT  | Proverava da clan ima potvrdjenu rez. i da count <= rez. count  |
| `trg_kreiranje_kazne_nepojavljivanje`  | `rezervacije` | AFTER UPDATE   | Automatski kreira kaznu kad se status promeni na nepojavljivanje|
| `trg_provera_ban_clana`               | `rezervacije` | BEFORE INSERT  | Blokira rezervaciju ako je clan prekoracio prag kazni           |
| `trg_max_slika_prostora`              | `slike_prostora`| BEFORE INSERT | Ogranicava broj slika po prostoru na maksimalno 6               |