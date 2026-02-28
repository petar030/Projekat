# Coworking Hub Manager - REST API specifikacija

Ovaj dokument definise **kompletan skup REST endpointa** koje backend treba da ima prema projektnim zahtevima.

## 1) Osnovna pravila

- Base path: `/api`
- Svi zahtevi i odgovori su `application/json` osim upload/download ruta.
- Autentifikacija: **JWT access token** (bez refresh tokena).
- Header za zasticene rute:

```http
Authorization: Bearer <access_token>
```

- Preporuceni format greske:

```json
{
  "timestamp": "2026-02-25T12:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/auth/register/member",
  "details": [
    {
      "field": "email",
      "message": "Email vec postoji"
    }
  ]
}
```

---

## 2) JWT model

### 2.1 Login response (za sve login rute)

```json
{
  "accessToken": "<jwt>",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "user": {
    "id": 1,
    "username": "admin",
    "role": "admin",
    "status": "odobren"
  }
}
```

### 2.2 Primer JWT claim-ova

```json
{
  "sub": "admin",
  "uid": 1,
  "role": "admin",
  "status": "odobren",
  "iat": 1772011200,
  "exp": 1772014800
}
```

---

## 3) Auth i registracija

## 3.1 Login clan/menadzer

- `POST /api/auth/login`
- Auth: public

Request:

```json
{
  "username": "marko",
  "password": "Marko123!"
}
```

Response `200`:

```json
{
  "accessToken": "<jwt>",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "user": {
    "id": 12,
    "username": "marko",
    "role": "clan",
    "status": "odobren"
  }
}
```

Moguce greske: `401`, `423` (npr. nalog nije odobren), `400`.

## 3.2 Login admin (posebna ruta)

- `POST /api/auth/admin/login`
- Auth: public (ali samo `uloga=admin`)

Request:

```json
{
  "username": "admin",
  "password": "Admin123!"
}
```

Response: isto kao login response.

## 3.3 Registracija clana

- `POST /api/auth/register/member`
- Auth: public
- Content-Type: `multipart/form-data`

Part `data` (JSON):

```json
{
  "username": "milica",
  "password": "Milica123!",
  "ime": "Milica",
  "prezime": "Petrovic",
  "telefon": "+38160111222",
  "email": "milica@example.com"
}
```

Part `profileImage`: fajl `jpg/png` (opciono).

Response `201`:

```json
{
  "id": 33,
  "username": "milica",
  "role": "clan",
  "status": "na_cekanju",
  "message": "Zahtev za registraciju je kreiran"
}
```

## 3.4 Registracija menadzera

- `POST /api/auth/register/manager`
- Auth: public
- Content-Type: `multipart/form-data`

Part `data` (JSON):

```json
{
  "username": "menadzer1",
  "password": "Menadzer1!",
  "ime": "Nikola",
  "prezime": "Ilic",
  "telefon": "+38164123456",
  "email": "nikola@firma.rs",
  "firma": {
    "naziv": "Coworking Plus",
    "adresa": "Bulevar 1, Beograd",
    "maticniBroj": "12345678",
    "pib": "123456789"
  }
}
```

Part `profileImage`: fajl `jpg/png` (opciono).

Response `201`: isto kao za clana, uz `role=menadzer`.

## 3.5 Reset lozinke - jednostavan flow (bez email-a)

Po tvojoj postavci koristi se najjednostavniji tok:

1. Korisnik u formi unosi `usernameOrEmail`.
2. Backend vraca reset token + vreme isteka.
3. Frontend formira link (npr. ruta `/reset-password?token=...`) i prikazuje formu.
4. Frontend prikaze formu za novu lozinku i odbrojavanje do isteka.
5. Frontend posalje `token + newPassword` backendu.
6. Ako je token validan i nije istekao, lozinka se menja.

**Vazno:** query param nosi samo token; backend je jedini izvor istine za validnost i istek.

## 3.6 Kreiranje reset linka (bez slanja email-a)

- `POST /api/auth/password-reset/request`
- Auth: public

Request:

```json
{
  "usernameOrEmail": "marko_user"
}
```

Response `202`:

```json
{
  "token": "<reset_token>",
  "tokenExpiresAt": "2026-02-25T14:10:00",
  "expiresInSeconds": 1800
}
```

Napomene implementacije:
- Ako korisnik ne postoji, vrati `404` (u ovoj varijanti to je prihvatljivo jer je flow interni/jednostavan).
- Token je jednokratan i vazi 30 minuta.
- Pri novom request-u za isti nalog: prethodne neiskoriscene tokene oznaciti kao nevalidne (ili `iskoriscen=true`).

Frontend timer se racuna iz `tokenExpiresAt` koji dolazi iz request endpoint-a.
Backend i dalje proverava token i istek pri confirm pozivu.

## 3.7 Potvrda nove lozinke

- `POST /api/auth/password-reset/confirm`
- Auth: public

Request:

```json
{
  "token": "<reset_token>",
  "newPassword": "Novo123!"
}
```

Response `200`:

```json
{
  "message": "Lozinka je uspesno promenjena"
}
```

Posle uspesnog reseta:
- `tokeni_za_reset_lozinke.iskoriscen = true`
- opciono invalidirati sve ostale aktivne reset tokene tog korisnika
- opciono invalidirati sve aktivne sesije/JWT tokene korisnika

## 3.8 Logout

- `POST /api/auth/logout`
- Auth: `clan|menadzer|admin`

Request:

```json
{}
```

Response `200`:

```json
{
  "message": "Uspesno izlogovan"
}
```

Napomena: posto nema refresh tokena, backend moze biti stateless (frontend samo brise access token), ili opciono blacklist mehanizam.

---

## 4) Public (neregistrovani)

## 4.1 Pocetni dashboard info

- `GET /api/public/home`
- Auth: public

Response `200`:

```json
{
  "totalApprovedSpaces": 42,
  "top5Spaces": [
    {
      "spaceId": 10,
      "naziv": "Hub Dorcol",
      "grad": "Beograd",
      "likes": 54,
      "dislikes": 3
    }
  ]
}
```

## 4.2 Lista gradova za filter

- `GET /api/public/spaces/cities`
- Auth: public

Response `200`:

```json
{
  "cities": ["Beograd", "Novi Sad", "Nis"]
}
```

## 4.3 Pretraga prostora

- `GET /api/public/spaces`
- Auth: public
- Query params:
  - `name` (opciono)
  - `cities` (opciono, vise vrednosti)

Response `200`:

```json
{
  "content": [
    {
      "id": 10,
      "naziv": "Hub Dorcol",
      "grad": "Beograd",
      "adresa": "Cara Dusana 10",
      "firmaNaziv": "Coworking Plus",
      "likes": 54,
      "dislikes": 3
    }
  ]
}
```

## 4.4 Detalji prostora

- `GET /api/public/spaces/{spaceId}`
- Auth: public

Response `200`:

```json
{
  "id": 10,
  "naziv": "Hub Dorcol",
  "grad": "Beograd",
  "adresa": "Cara Dusana 10",
  "opis": "Duz opis prostora...",
  "cenaPoSatu": 12.5,
  "firma": {
    "id": 2,
    "naziv": "Coworking Plus"
  },
  "menadzer": {
    "id": 7,
    "imePrezime": "Nikola Ilic"
  },
  "geolocation": {
    "lat": 44.8176,
    "lng": 20.4633
  },
  "reactions": {
    "likes": 54,
    "dislikes": 3
  },
  "images": [
    "/uploads/spaces/10/i1.jpg",
    "/uploads/spaces/10/i2.jpg",
    "/uploads/spaces/10/i3.jpg"
  ],
  "latestComments": [
    {
      "id": 88,
      "username": "mika",
      "createdAt": "2026-02-20T11:22:00",
      "text": "Odlican prostor"
    }
  ]
}
```

---

## 5) Korisnik (clan/menadzer/admin)

## 5.1 Trenutni korisnik

- `GET /api/users/me`
- Auth: `clan|menadzer|admin`

Response `200`:

```json
{
  "id": 12,
  "username": "marko",
  "ime": "Marko",
  "prezime": "Markovic",
  "telefon": "+38160123456",
  "email": "marko@example.com",
  "profilnaSlika": "/uploads/profiles/marko.png",
  "uloga": "clan",
  "status": "odobren",
  "firma": null
}
```

## 5.2 Azuriranje profila

- `PUT /api/users/me`
- Auth: `clan|menadzer|admin`
- Napomena: korisnicko ime (`username`) se ne menja ovim endpoint-om.

Request:

```json
{
  "ime": "Marko",
  "prezime": "Markovic",
  "telefon": "+38160123456",
  "email": "marko_new@example.com"
}
```

Response `200`: azuriran user JSON.

## 5.3 Promena profilne slike

- `PUT /api/users/me/profile-image`
- Auth: `clan|menadzer|admin`
- Content-Type: `multipart/form-data`
- Part: `profileImage`

Response `200`:

```json
{
  "profileImage": "/uploads/profiles/marko_new.png"
}
```

## 5.4 Promena lozinke

- `PUT /api/users/me/password`
- Auth: `clan|menadzer|admin`

Request:

```json
{
  "newPassword": "Nova123!"
}
```

Validacija lozinke je ista kao pri registraciji:
- mora poceti slovom
- najmanje jedno veliko slovo
- najmanje jedna cifra
- najmanje jedan specijalni karakter
- duzina 8-12 karaktera

Response `200`:

```json
{
  "message": "Lozinka je uspesno promenjena"
}
```

---

## 6) Clan - rezervacije, pretraga, reakcije i komentari

## 6.1 Pretraga za clana (prosireni filteri)

- `GET /api/member/spaces`
- Auth: `clan`
- Query params:
  - `name`, `cities[]`
  - `type` = `otvoreni|kancelarija|sala` (tacno jedan)
  - `officeMinDesks` (opciono, samo za kancelarija)
  - bez sortiranja i paginacije (uskladjeno sa public pretragom)

Response `200`: lista (slicno public), sa dodatnim poljima po tipu.

Napomena: svaki rezultat sadrzi i `matchingSubspaceIds` (lista ID-jeva podprostora koji zadovoljavaju filter za izabrani `type`).

## 6.2 Zauzetost za kalendar

- `POST /api/member/spaces/{spaceId}/availability`
- Auth: `clan`

Request:

```json
{
  "type": "kancelarija",
  "resourceIds": [34, 35, 36],
  "weekStart": "2026-02-23"
}
```

- `type` = `otvoreni|kancelarija|sala`
- `resourceIds` = lista ID-jeva podprostora za izabrani tip (najcesce iz `matchingSubspaceIds` iz pretrage)
- `weekStart` (`YYYY-MM-DD`)
- Svaki element u `resources` vraca i `resourceName`
- Za `type = sala`, svaki element u `resources` opciono vraca i `additionalEquipment` (moze biti `null`)

Response `200`:

```json
{
  "spaceId": 10,
  "type": "kancelarija",
  "weekStart": "2026-02-23",
  "resources": [
    {
      "resourceId": 34,
      "resourceName": "Office A",
      "additionalEquipment": null,
      "busySlots": [
        {
          "from": "2026-02-23T09:00:00",
          "to": "2026-02-23T12:00:00"
        }
      ]
    },
    {
      "resourceId": 35,
      "resourceName": "Office B",
      "additionalEquipment": null,
      "busySlots": []
    },
    {
      "resourceId": 2001,
      "resourceName": "Sala Alfa",
      "additionalEquipment": "TV i whiteboard",
      "busySlots": []
    }
  ]
}
```

## 6.3 Kreiranje rezervacije

- `POST /api/member/reservations`
- Auth: `clan`

Request:

```json
{
  "spaceId": 10,
  "type": "kancelarija",
  "resourceId": 34,
  "from": "2026-02-27T10:00:00",
  "to": "2026-02-27T14:00:00"
}
```

Response `201`:

```json
{
  "id": 501,
  "status": "aktivna",
  "spaceId": 10,
  "type": "kancelarija",
  "resourceId": 34,
  "from": "2026-02-27T10:00:00",
  "to": "2026-02-27T14:00:00"
}
```

Greske: `409` (preklapanje), `403` (ban), `400` (los interval).

Napomena: rezervacija se kreira u okviru jednog dana (`from` i `to` moraju biti isti datum).

## 6.4 Sve rezervacije trenutnog clana

- `GET /api/member/reservations`
- Auth: `clan`
- Bez query parametara (vraca sve prethodne i trenutne rezervacije clana)

Response `200`:

```json
{
  "content": [
    {
      "id": 501,
      "spaceId": 10,
      "spaceName": "Hub Dorcol",
      "city": "Beograd",
      "from": "2026-02-27T10:00:00",
      "to": "2026-02-27T14:00:00",
      "status": "aktivna",
      "cancellable": true
    }
  ]
}
```

## 6.5 Otkazivanje rezervacije (12h pravilo)

- `PATCH /api/member/reservations/{reservationId}/cancel`
- Auth: `clan`

Request:

```json
{}
```

Response `200`:

```json
{
  "id": 501,
  "status": "otkazana"
}
```

## 6.6 Like/dislike prostora

- `POST /api/member/spaces/{spaceId}/reactions`
- Auth: `clan`

Request:

```json
{
  "tip": "svidjanje"
}
```

Response `201`:

```json
{
  "id": 1001,
  "spaceId": 10,
  "userId": 12,
  "tip": "svidjanje",
  "createdAt": "2026-02-25T12:10:00"
}
```

## 6.7 Komentar prostora

- `POST /api/member/spaces/{spaceId}/comments`
- Auth: `clan`

Request:

```json
{
  "text": "Vrlo prijatan ambijent"
}
```

Response `201`:

```json
{
  "id": 2001,
  "spaceId": 10,
  "userId": 12,
  "username": "marko",
  "text": "Vrlo prijatan ambijent",
  "createdAt": "2026-02-25T12:15:00"
}
```

## 6.8 Poslednjih 10 komentara prostora

- `GET /api/member/spaces/{spaceId}/comments/latest?limit=10`
- Auth: `clan`

Response `200`:

```json
{
  "comments": [
    {
      "id": 2001,
      "userId": 12,
      "username": "marko",
      "text": "Vrlo prijatan ambijent",
      "createdAt": "2026-02-25T12:15:00",
      "mine": true
    }
  ]
}
```

---

## 7) Menadzer - prostori, elementi, rezervacije, kalendar, izvestaji

## 7.1 Prostori menadzera

- `GET /api/manager/spaces`
- Auth: `menadzer`

Response `200`:

```json
{
  "spaces": [
    {
      "id": 10,
      "naziv": "Hub Dorcol",
      "grad": "Beograd",
      "status": "odobren",
      "pragKazni": 3,
      "elements": {
        "openSpace": { "id": 3, "brojStolova": 20 },
        "offices": [
          { "id": 34, "naziv": "Kancelarija A", "brojStolova": 4 }
        ],
        "meetingRooms": [
          { "id": 20, "naziv": "Sala 1", "brojMesta": 10, "dodatnaOprema": "TV" }
        ]
      }
    }
  ]
}
```

## 7.2 Kreiranje prostora (samo openSpace + max 5 slika)

- `POST /api/manager/spaces`
- Auth: `menadzer`
- Content-Type: `multipart/form-data`

Part `data` (JSON):

```json
{
  "naziv": "Hub Novi Beograd",
  "grad": "Beograd",
  "adresa": "Bulevar Zorana Djindjica 1",
  "opis": "Moderan coworking",
  "cenaPoSatu": 15.0,
  "pragKazni": 3,
  "geografskaSirina": 44.8100,
  "geografskaDuzina": 20.4000,
  "openSpace": {
    "brojStolova": 25
  }
}
```

Part `images[]` (opciono, maksimalno 5 fajlova).

Response `201`:

```json
{
  "id": 77,
  "status": "na_cekanju",
  "message": "Prostor je kreiran i ceka odobrenje admina"
}
```

## 7.3 Dodavanje kancelarije u postojeci prostor

- `POST /api/manager/spaces/{spaceId}/offices`
- Auth: `menadzer`

Request:

```json
{
  "naziv": "Office B",
  "brojStolova": 6
}
```

Response `201`:

```json
{
  "id": 35,
  "spaceId": 10,
  "naziv": "Office B",
  "brojStolova": 6
}
```

## 7.4 Dodavanje konferencijske sale u postojeci prostor

- `POST /api/manager/spaces/{spaceId}/meeting-rooms`
- Auth: `menadzer`

Request:

```json
{
  "naziv": "Sala Beta",
  "dodatnaOprema": "TV, whiteboard"
}
```

Response `201`:

```json
{
  "id": 21,
  "spaceId": 10,
  "naziv": "Sala Beta",
  "brojMesta": 10,
  "dodatnaOprema": "TV, whiteboard"
}
```

## 7.5 Brza konfiguracija prostora (JSON + slike u jednom pozivu)

- `POST /api/manager/spaces/import-json`
- Auth: `menadzer`
- Content-Type: `multipart/form-data`

Part `data` (JSON, potpuna struktura prostora):

```json
{
  "naziv": "Hub Novi Beograd",
  "grad": "Beograd",
  "adresa": "Bulevar Zorana Djindjica 1",
  "opis": "Moderan coworking",
  "cenaPoSatu": 15.0,
  "pragKazni": 3,
  "geografskaSirina": 44.8100,
  "geografskaDuzina": 20.4000,
  "openSpace": {
    "brojStolova": 25
  },
  "offices": [
    { "naziv": "Office A", "brojStolova": 3 },
    { "naziv": "Office B", "brojStolova": 6 }
  ],
  "meetingRooms": [
    { "naziv": "Sala Alfa", "dodatnaOprema": "Projektor, TV" },
    { "naziv": "Sala Beta", "dodatnaOprema": "TV, whiteboard" }
  ]
}
```

Part `images[]` (opciono, maksimalno 5 fajlova) - svi se upload-uju u istom pozivu.

Response `201`:

```json
{
  "id": 78,
  "status": "na_cekanju",
  "message": "Prostor iz JSON payload-a je uspesno kreiran"
}
```

Napomena:
- Endpointi za azuriranje i brisanje elemenata prostora nisu deo ovog minimalnog flow-a.
- Za 7.2 i 7.5 vazi pravilo: maksimalno 5 slika pri kreiranju prostora.

## 7.6 Rezervacije za menadzerove prostore

- `GET /api/manager/reservations`
- Auth: `menadzer`
- Query params: `spaceId`, `status`, `from`, `to`

Response `200`:

```json
{
  "content": [
    {
      "id": 501,
      "member": { "id": 12, "username": "marko" },
      "spaceId": 10,
      "type": "kancelarija",
      "resourceName": "Office A",
      "from": "2026-02-27T10:00:00",
      "to": "2026-02-27T14:00:00",
      "status": "aktivna",
      "canConfirmOrNoShow": true
    }
  ]
}
```

## 7.7 Potvrda dolaska

- `PATCH /api/manager/reservations/{reservationId}/confirm`
- Auth: `menadzer`

Request:

```json
{}
```

Response `200`:

```json
{
  "id": 501,
  "status": "potvrdjena"
}
```

## 7.8 Oznaci nepojavljivanje

- `PATCH /api/manager/reservations/{reservationId}/no-show`
- Auth: `menadzer`

Request:

```json
{}
```

Response `200`:

```json
{
  "id": 501,
  "status": "nepojavljivanje",
  "penaltyCreated": true
}
```

## 7.9 Kalendar menadzera

- `GET /api/manager/calendar`
- Auth: `menadzer`
- Query params:
  - `spaceId`
  - `type` = `otvoreni|kancelarija|sala`
  - `resourceId`
  - `from`, `to`

Response `200`:

```json
{
  "events": [
    {
      "reservationId": 501,
      "title": "marko",
      "from": "2026-02-27T10:00:00",
      "to": "2026-02-27T14:00:00",
      "status": "aktivna"
    }
  ]
}
```

## 7.10 Drag-and-drop pomeranje termina

- `PATCH /api/manager/reservations/{reservationId}/move`
- Auth: `menadzer`

Request:

```json
{
  "from": "2026-02-27T11:00:00",
  "to": "2026-02-27T15:00:00"
}
```

Response `200`:

```json
{
  "id": 501,
  "from": "2026-02-27T11:00:00",
  "to": "2026-02-27T15:00:00",
  "status": "aktivna"
}
```

## 7.11 Mesecni PDF izvestaj

- `GET /api/manager/reports/occupancy?spaceId={spaceId}&year=2026&month=2`
- Auth: `menadzer`
- Response content-type: `application/pdf`

Backend je jedini izvor istine za izvestaj i kompletan obracun popunjenosti.
Frontend ne racuna procente, vec samo salje parametre i preuzima PDF fajl.

Pravila:
- `month` mora biti u opsegu `1-12`.
- Mesec moze biti samo tekuci ili prosli (ne buduci).
- Izvestaj obuhvata sve elemente prostora (`otvoreni`, sve `kancelarije`, sve `sale`).
- U obracun ulaze rezervacije koje se preklapaju sa izabranim mesecom, bez `otkazana` i bez `nepojavljivanje` statusa.
- PDF sadrzi zbirnu popunjenost kapaciteta i breakdown po svakom podprostoru.

Response `200`:
- binarni PDF fajl
- `Content-Disposition: attachment; filename="occupancy-space-<spaceId>-<year>-<month>.pdf"`

---

## 8) Admin - korisnici, odobravanje, statistika

## 8.1 Svi korisnici

- `GET /api/admin/users`
- Auth: `admin`
- Query params: `role`, `status`, `search`, `sortBy`, `sortDir`

Response `200`:

```json
{
  "content": [
    {
      "id": 12,
      "username": "marko",
      "ime": "Marko",
      "prezime": "Markovic",
      "email": "marko@example.com",
      "telefon": "+38160123456",
      "role": "clan",
      "status": "odobren"
    }
  ]
}
```

## 8.2 Detalj korisnika

- `GET /api/admin/users/{userId}`
- Auth: `admin`

Response `200`: user detalji.

## 8.3 Azuriranje korisnika

- `PUT /api/admin/users/{userId}`
- Auth: `admin`

Request:

```json
{
  "ime": "Marko",
  "prezime": "Markovic",
  "telefon": "+381601999888",
  "email": "marko_new@example.com",
  "status": "odobren"
}
```

Response `200`.

## 8.4 Brisanje korisnika

- `DELETE /api/admin/users/{userId}`
- Auth: `admin`

Response `204`.

## 8.5 Neodobreni korisnici (zahtevi)

- `GET /api/admin/registration-requests`
- Auth: `admin`

Response `200`:

```json
{
  "requests": [
    {
      "userId": 33,
      "username": "milica",
      "role": "clan",
      "status": "na_cekanju",
      "createdAt": "2026-02-24T09:00:00"
    }
  ]
}
```

## 8.6 Odobri korisnika

- `PATCH /api/admin/registration-requests/{userId}/approve`
- Auth: `admin`

Request:

```json
{}
```

Response `200`:

```json
{
  "userId": 33,
  "status": "odobren"
}
```

## 8.7 Odbij korisnika

- `PATCH /api/admin/registration-requests/{userId}/reject`
- Auth: `admin`

Request:

```json
{
  "reason": "Nedostaje validan PIB"
}
```

Response `200`:

```json
{
  "userId": 33,
  "status": "odbijen"
}
```

## 8.8 Prostori na cekanju

- `GET /api/admin/space-requests`
- Auth: `admin`

Response `200`:

```json
{
  "requests": [
    {
      "spaceId": 77,
      "naziv": "Hub Novi Beograd",
      "grad": "Beograd",
      "firmaNaziv": "Coworking Plus",
      "status": "na_cekanju"
    }
  ]
}
```

## 8.9 Odobri prostor

- `PATCH /api/admin/space-requests/{spaceId}/approve`
- Auth: `admin`

Request:

```json
{}
```

Response `200`:

```json
{
  "spaceId": 77,
  "status": "odobren"
}
```

## 8.10 Odbij prostor

- `PATCH /api/admin/space-requests/{spaceId}/reject`
- Auth: `admin`

Request:

```json
{
  "reason": "Neispravna adresa"
}
```

Response `200`:

```json
{
  "spaceId": 77,
  "status": "odbijen"
}
```

## 8.11 Lista prostora za admin statistiku

- `GET /api/admin/stats/spaces`
- Auth: `admin`

Response `200`:

```json
{
  "spaces": [
    {
      "spaceId": 10,
      "spaceName": "Hub Dorcol"
    }
  ]
}
```

## 8.12 Mesečna statistika za izabrani prostor

- `GET /api/admin/stats/space-monthly`
- Auth: `admin`
- Query params:
  - `spaceId` (obavezno)
  - `year` (obavezno)

Response `200`:

```json
{
  "spaceId": 10,
  "spaceName": "Hub Dorcol",
  "year": 2026,
  "items": [
    {
      "month": 1,
      "likes": 12,
      "dislikes": 1,
      "reservations": 34,
      "revenue": 45200.0,
      "currency": "RSD"
    },
    {
      "month": 2,
      "likes": 9,
      "dislikes": 2,
      "reservations": 29,
      "revenue": 39800.0,
      "currency": "RSD"
    }
  ]
}
```

---

## 9) Kodovi statusa po endpointima

- `200 OK` - uspesno citanje/akcija
- `201 Created` - uspesno kreiranje
- `204 No Content` - uspesno brisanje
- `400 Bad Request` - validacija/pogresan payload
- `401 Unauthorized` - bez tokena ili nevalidan token
- `403 Forbidden` - nema pravo pristupa
- `404 Not Found` - resurs ne postoji
- `409 Conflict` - konflikt (npr. duplikat, preklapanje termina)
- `422 Unprocessable Entity` - poslovno pravilo nije zadovoljeno
- `423 Locked` - nalog nije odobren/odbijen
- `500 Internal Server Error` - neocekivana greska

---

## 10) Minimalni DTO ugovori (preporuka)

Da endpointi budu stabilni i jasni, backend treba da koristi odvojene DTO klase (ne direktno JPA entitete):

- `AuthLoginRequest`, `AuthLoginResponse`
- `RegisterMemberRequest`, `RegisterManagerRequest`
- `ForgotPasswordRequest`, `ResetPasswordRequest`
- `UserProfileResponse`, `UpdateProfileRequest`
- `SpaceSearchResponse`, `SpaceDetailsResponse`
- `CreateReservationRequest`, `ReservationResponse`
- `CreateReactionRequest`, `CreateCommentRequest`, `CommentResponse`
- `CreateSpaceRequest`, `UpdateSpaceRequest`, `SpaceResponse`
- `ManagerReservationResponse`, `MoveReservationRequest`
- `AdminUserResponse`, `AdminUpdateUserRequest`
- `AdminStatSpacesResponse`, `SpaceMonthlyStatsResponse`

---

## 11) Napomena za implementaciju JWT biblioteke

U backend je predvidjeno koriscenje biblioteke:
- `io.jsonwebtoken:jjwt-api`
- `io.jsonwebtoken:jjwt-impl`
- `io.jsonwebtoken:jjwt-jackson`

plus Spring Security starter.

U ovom koraku se **ne implementira** JWT filter/login logika, vec je samo definisan API ugovor i dodata biblioteka.
