# Smart Restaurant Reservation System

Veebirakendus, mis võimaldab kliendil:
- valida kuupäeva/kellaaja;
- määrata seltskonna suuruse;
- filtreerida tsooni järgi;
- lisada eelistusi (privaatsus, aknakoht, ligipääsetavus, lasteala);
- näha saaliplaanil, millised lauad on hõivatud ja milline laud on kõige parem soovitus;
- saada soovituseks päevapraadi TheMealDB avalikust API-st.

## Tehnoloogiad
- Java 21 (LTS)
- Spring Boot 3
- Maven
- Frontend: HTML + CSS (Bootstrap) + Vanilla JavaScript (Spring Boot static resources)

## Käivitamine
```bash
mvn spring-boot:run
```
Rakendus avaneb aadressil: `http://localhost:8080`

## Testid
```bash
mvn test
```

## Dockeris käivitamine
1. Paigalda Docker.
   Windowsis on enne vaja paigaldada WSL (Windows Subsystem for Linux).
2. Ava PowerShell kloonitud projekti kaustas ja käivita käsud:
```bash
docker build -t smart-restaurant-reservation .
docker run --rm -p 8080:8080 smart-restaurant-reservation
```

## Rakendatud loogika
1. Süsteem võtab sisendiks aja, seltskonna suuruse, tsooni ja eelistused.
2. Hõive simuleeritakse broneeringuplokkidena (2–3h), mis teeb vabade aegade hindamise realistlikumaks.
3. Iga vaba laud skooritakse:
   - suuruse sobivus (väiksem ülejääk = parem skoor),
   - eelistuste kattuvus,
   - tsooni sobivus.
4. Kui üksik laud ei mahu grupile, otsitakse kõrvuti asuvatest laudadest parim liitlaud.
5. Admin-vaates saab laudu hiirega lohistada ja salvestada paigutuse.

## Eeldused
- Selles versioonis kasutatakse in-memory mudelit, püsivat andmebaasi ei ole.
- Välise retsepti API tõrke korral kuvatakse varusoovitus.

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
- Ajapõhine admin-ajaloo salvestus (kes paigutust muutis).
- Autentimine admin-funktsioonidele.
