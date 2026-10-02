# Cooldown Tracker (Fabric 1.21.11) – készítő: FeherHollo69

Csak kliens oldali, vizuális cooldown jelzők a hotbar fölött.

| Item | Trigger | Idő |
|---|---|---|
| Ender Pearl | jobb klikk | 9 mp |
| Gyémántfejsze (Jég Balta) | ütés | 10 mp |
| Cukor (Gyorsaság II) | jobb klikk | 30 mp |
| Blaze Powder (Erő II) | jobb klikk | 30 mp |
| Blaze Rod (Bamboozle) | ütés | 90 mp |
| Lila festék (Gyöngy Gyűlölő) | ütés | 2 perc |
| Jég (Fagyasztás) | ütés | 2 perc |
| Nyúlláb (Lökés) | jobb klikk | 15 mp |

Config menü: **K** billentyű (Beállítások → Irányítás alatt átállítható).
A menüben a **Kártyák mozgatása** gombbal az egérrel bárhová húzhatók a jelzők (az **Alaphelyzet** gomb visszaállítja őket).
Fájl: `.minecraft/config/cooldowntracker.json`

## Build
1. Ellenőrizd a verziókat a `gradle.properties`-ben: https://fabricmc.net/develop
2. Másold be a Gradle wrappert (gradlew, gradlew.bat, gradle/wrapper/) a hivatalos Fabric example mod sablonból
3. `./gradlew build` -> `build/libs/cooldowntracker-1.2.1.jar`

### Gyors build (Windows: `gradlew.bat build`)
Java 21 szükséges. A kész mod: `build/libs/cooldowntracker-1.2.1.jar` (a `-sources` jar nem kell) -> `.minecraft/mods/`. Fabric API is kell mellé.

### Boss időzítők
A mod percenként (15 mp-nél gyakrabban nem) magától elküldi a `/boss` parancsot, a válaszból (World Boss, Lich King, Oog) kártyát csinál visszaszámlálóval, és a válasz sorait elrejti a chatből. Lejáratkor "Spawnolt!" felirat és hangjelzés. A **Boss: Be/Ki** gombbal kapcsolható. Ha 3 egymás utáni lekérdezésre nem jön használható válasz, az automatika leáll (a kézi `/boss` újraindítja).
