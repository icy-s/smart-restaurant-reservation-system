<img src="https://i.imgur.com/sVDseom.png" alt="Project Preview">

# Smart Restaurant Reservation System

Veebirakendus, mis võimaldab kliendil:
- valida kuupäeva/kellaaja;
- määrata seltskonna suuruse;
- filtreerida tsooni järgi;
- lisada eelistusi (privaatsus, aknakoht, ligipääsetavus, lasteala);
- näha saaliplaanil, millised lauad on hõivatud ja milline laud on kõige parem soovitus;
- saada soovituseks päevapraadi TheMealDB avalikust API-st.

## Repositooriumi kloonimine

Klooni projekt ja liigu projekti kausta:

```bash
git clone https://github.com/icy-s/smart-restaurant-reservation-system.git
cd smart-restaurant-reservation-system
```

## Tehnoloogiad
- Java 21 (LTS)
- Spring Boot 3
- Maven (Maven Wrapper kaasas)
- Frontend: HTML + CSS (Bootstrap) + Vanilla JavaScript (Spring Boot static resources)

## Eeldused

- **JDK 21** paigaldatud ja IDE-s valitud (Project SDK = Java 21).
- Git (projekti kloonimiseks).
- **Maven ei ole vajalik**, kui kasutad Maven Wrapper’it (`mvnw`).

## Käivitamine ilma Mavenita (Maven Wrapper)

> Kui sul puudub `mvn` (Maven), kasuta Maven Wrapper’it. See töötab ka puhtas masinas.

### macOS / Linux
```bash
./mvnw spring-boot:run
```

### Windows (PowerShell)

1) Ava **PowerShell** kloonitud projekti kaustas (või tee kõigepealt `cd smart-restaurant-reservation-system`).  
2) Käivita:

```powershell
.\mvnw.cmd spring-boot:run
```

Rakendus avaneb aadressil: `http://localhost:8080`

## Käivitamine IDE-st

1. Ava projekt (pom.xml) IDE-s: **File → Open**
2. Kontrolli, et IDE kasutab **JDK 21** (Project SDK / JAVA_HOME).
3. Leia põhiklass `*Application` (Spring Boot main class)
4. Vajuta klassi juures ▶ **Run** (või loo Run Configuration tüübiga **Spring Boot**)

## JAR-failina käivitamine (alternatiiv)

```bash
./mvnw clean package
java -jar target/*.jar
```

## Testid

**macOS / Linux**
```bash
./mvnw test
```

**Windows (PowerShell)**
```powershell
.\mvnw.cmd test
```

## Levinud probleemid

- **Port 8080 on kinni**: sulge teine rakendus, mis kasutab 8080 porti, või muuda `server.port` väärtust.
- **TheMealDB API**: vajab internetiühendust; tõrke korral kuvatakse varusoovitus.
- **Unsupported class file major version ...** → veendu, et kasutad **JDK 21** (JAVA_HOME / IDE Project SDK).

## Dockeris käivitamine
1. Paigalda Docker.
   Windowsis on enne vaja paigaldada WSL (Windows Subsystem for Linux).
2. Ava PowerShell kloonitud projekti kaustas ja käivita käsud:
```powershell
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

## Arenduse logi (aeg, ligikaudne)

- Projekti bootstrap (Spring Boot, struktuur, esialgne API): ~1 h
- Broneeringu/hõivatuse simulatsioon + skoorimine + liitlauad: ~1 h 15 min
- UI (filter, saaliplaan, värvid, põhjendused): ~1 h 30 min
- Admin-vaade (drag & drop, paigutuse salvestus, liitlaudade liigutamine): ~1 h
- TheMealDB integratsioon + varusoovitus: ~20 min
- Testid + refaktor/bugfixid: ~45 min
- Docker + Maven Wrapper + README täiendused: ~30–40 min

**Kokku:** ~6 h (±30 min)

_Märkus: aeg sisaldab iteratsioone, bugfix’e ja dokumentatsiooni täiendamist._

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
