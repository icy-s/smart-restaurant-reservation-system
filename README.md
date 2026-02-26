# Smart Restaurant Reservation System

Veebirakendus, mis võimaldab kliendil:
- valida kuupäeva/kellaaja;
- määrata seltskonna suuruse;
- filtreerida tsooni järgi;
- lisada eelistusi (privaatsus, aknakoht, ligipääsetavus, lasteala);
- näha saaliplaanil, millised lauad on hõivatud ja milline laud on kõige parem soovitus.

## Tehnoloogiad
- Java 21 (LTS)
- Spring Boot 3
- Maven
- Frontend: HTML + CSS + Vanilla JavaScript (Spring Boot static resources)

## Käivitamine
```bash
mvn spring-boot:run
```
Rakendus avaneb aadressil: `http://localhost:8080`

## Testid
```bash
mvn test
```

## Rakendatud loogika
1. Süsteem võtab sisendiks aja, seltskonna suuruse, tsooni ja eelistused.
2. Hõivatud lauad genereeritakse pseudo-juhuslikult valitud aja põhjal, et sama päring annaks sama tulemuse.
3. Iga vaba laud skooritakse:
   - suuruse sobivus (väiksem ülejääk = parem skoor),
   - eelistuste kattuvus,
   - tsooni sobivus.
4. Kõrgeima skooriga sobiv laud märgitakse soovitatud lauana.

## Eeldused
- Üks broneering hõivab valitud ajahetke ümber laua.
- Selles versioonis ei hallata püsivat andmebaasi; laua hõivatus simuleeritakse.
- Kui laud on seltskonna jaoks liiga väike, kuvatakse ta saaliplaanil põhjendusega, aga mitte soovitatavana.

## Kui ülesandes jäi ebaselgeks
- Kuna andmebaasi nõuet polnud, valisin kiire prototüübi in-memory mudeliga.
- Kuna frontend raamistikku ei nõutud, kasutasin lihtsat JavaScripti.

## Arenduse logi (aeg)
- Projektistruktuur ja API: ~45 min
- Skoorimise loogika ja hõivatuse simulatsioon: ~35 min
- UI (filter + saaliplaan + värvid): ~40 min
- Testid ja dokumentatsioon: ~20 min
- Kokku: ~2 h 20 min

## Keerukused ja lahendused
- **Ajaformaat brauseri ja backendi vahel:**
  - `datetime-local` väärtus teisendati `ISO_LOCAL_DATE_TIME` formaadiks.
- **Visuaalne plaan lihtsana:**
  - kasutasin fikseeritud koordinaate laudadele ja absoluutset paigutust.
- **Soovituse selgitamine kasutajale:**
  - iga laua juures kuvatakse põhjendus (`reason`), miks laud sobib/ei sobi.

## AI / väliste allikate kasutus
- Lahendus on loodud Codexi abiga selle proovitöö kontekstis.
- Väliseid koodiplokke (StackOverflow vms) ei kopeeritud otse.

## Edasised parendused
- PostgreSQL/H2 ja päris broneeringute salvestus.
- Dünaamiline laudade liitmine suurematele gruppidele.
- Arvestus, et laud vabaneb 2–3h pärast broneeringut.
- Admin vaade laudade lohistamiseks.
- Dockerfile + docker-compose.
