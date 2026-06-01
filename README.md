# CBRN-RN Strahler Konfigurator (Android)

Android-App zum Auffinden und Konfigurieren der **Strahler**-Geräte
(Firmware: `CBRN_Demonstrator_RN_Probe`) über **BLE/GATT** — als handytaugliche
Alternative zur Bedienung über den Geräteknopf.

Die App hat zwei Bereiche (Tabs unten):

- **Messen** — das Handy verhält sich wie die **Sonde** (RN_Sonde): es empfängt
  passiv die Advertising-Pakete der Strahler, berechnet aus der Signalstärke eine
  Näherungs-Dosisleistung und zeigt sie als Geigerzähler-Rundinstrument inkl.
  Klick-Geräuschen an.
- **Einrichten** — Konfigurator für den Strahler über GATT: Oberfläche im
  E-Paper-Look des Geräts (Q-Nummer, Nuklid, max. Dosisleistung, `AN/AUS/LR`) mit
  mobilen Bedienelementen.

## Was die App kann

### Messen (Geigerzähler-Ansicht)

- **Passiver Dauer-Scan** nach `Strahler-NNN`-Paketen (Normal **und**
  Long-Range/Coded-PHY).
- **Dosis-Berechnung** identisch zur Sonde-Firmware: Pfadverlust-Modell
  `I = I_max · 10^((RSSI_glatt − RSSI_peak) / (5·n))` mit `n = 12`, zweistufige
  RSSI-Glättung (1-s-Bins × 3 s gleitender Mittelwert), Quellen-Timeout 5 s,
  bis zu 16 Quellen. Angezeigt wird die Quelle mit der höchsten aktuellen Dosis.
- **Rundinstrument** mit Farbzonen (grün/gelb/orange/rot bei 50/75/90 %),
  Auto-Range der Einheit (nSv/µSv/mSv/Sv) und „Kalibrierung…"-/„Kein Signal"-Zustand.
- **Geiger-Klicks**: `0,3 cps` Grundrate + dosisproportional (`+0,5 cps/µSv·h⁻¹`),
  gedeckelt bei 50 cps, mit Poisson-artigem Jitter. Per Lautsprecher-Symbol oben
  rechts an/aus.

> Hinweis: Die RSSI-basierte Dosis ist eine **Demonstrations-Näherung** (kein
> echtes Dosimeter) — exakt nach demselben vereinfachten Modell wie die Lehr-Sonde.

### Was die App kann (Einrichten)

- **Suchen**: Scannt nach Geräten, deren Name mit `Strahler` beginnt
  (`Strahler-NNN`). Findet sowohl Geräte im **Normal**- (Legacy 1 Mbps) als auch
  im **Lang**-Modus (Coded PHY / Long Range), da der Scan mit
  `setLegacy(false)` läuft.
- **Verbinden & Auslesen**: Liest beim Verbinden die aktuelle Konfiguration aus
  dem GATT-Dienst `0x1900`.
- **Konfigurieren** (Werte werden sofort über GATT geschrieben):

  | Parameter | Characteristic | Bereich / Werte | Kodierung |
  |-----------|----------------|-----------------|-----------|
  | Sender-Nr | `0x2B00` | 1–254 | uint8 |
  | Nuklid | `0x2B01` | Cs-137, Co-60, Am-241, Ir-192, Ra-226, Th-232, U-238, Pu-239 | uint8 (1–8) |
  | Max. Dosisleistung | `0x2B02` | 0–65535 | uint16 **little-endian** |
  | Aktiv | `0x2B03` | an / aus | bool |
  | Reichweite (Long Range) | `0x2B04` | Normal / Lang | bool |
  | Einheit | `0x2B05` | nSv/h, µSv/h, mSv/h, Sv/h | uint8 (0–3) |

> Hinweis zur Endianness: Über GATT ist die Dosis **little-endian** (so erwartet
> es die Firmware in `onGATTWrite()`). Die passive Advertising-Manufacturer-Data
> nutzt big-endian — dieser Pfad wird von der App nicht verwendet.

## Voraussetzungen

- **Android Studio** (Ladybug / 2024.2 oder neuer empfohlen)
- **JDK 17** (in Android Studio gebündelt)
- Android-Gerät mit **Android 8.0 (API 26)** oder neuer und Bluetooth LE
  (Long-Range-Scan benötigt ein Gerät, das BLE-5-Coded-PHY-Scanning unterstützt)

## Bauen & Installieren

1. Ordner in **Android Studio** öffnen (`File → Open`) und Gradle-Sync abwarten.
   Das SDK-Verzeichnis trägt Android Studio automatisch in `local.properties` ein.
2. Gerät per USB anschließen (USB-Debugging aktiv) **oder** Emulator —
   für echtes BLE wird ein **physisches Gerät** benötigt, der Emulator hat kein
   Bluetooth.
3. **Run ▶** drücken.

Alternativ per Kommandozeile (SDK + JDK vorausgesetzt):

```powershell
# Debug-APK bauen
.\gradlew.bat assembleDebug

# Direkt auf angeschlossenes Gerät installieren
.\gradlew.bat installDebug
```

Die APK liegt danach unter `app/build/outputs/apk/debug/`.

## Bedienung

1. App starten und die **Bluetooth-Berechtigung** erteilen
   (Android 12+: „Geräte in der Nähe"; Android 11 und älter: Standort, da das
   System BLE-Scans dort an die Standortfreigabe koppelt).

Unten gibt es zwei Tabs: **Messen** und **Einrichten**.

**Messen (Geigerzähler):**
- Beim Öffnen des Tabs startet automatisch der passive Scan; die Anzeige zeigt
  nach ~3 s Kalibrierung die Dosisleistung der stärksten Quelle.
- Lautsprecher-Symbol oben rechts schaltet die Klick-Geräusche an/aus.
- Beim Verlassen des Tabs stoppt der Scan wieder (Akku-schonend).

**Einrichten (Konfigurator):**
1. **„Strahler suchen"** tippen. Gefundene Geräte erscheinen mit Name und
   Signalstärke (RSSI). Der Scan stoppt automatisch nach 15 s.
2. Auf ein Gerät tippen → App verbindet sich und liest die Konfiguration.
3. Werte ändern — jede Änderung wird **sofort** auf das Gerät geschrieben.
   Die Vorschaukarte oben spiegelt den Gerätestatus wider.
4. Über den **Zurück-Pfeil** oben links trennen.

## Berechtigungen

Deklariert in `app/src/main/AndroidManifest.xml`:

- **Android 12+ (API 31+)**: `BLUETOOTH_SCAN` (mit `neverForLocation`),
  `BLUETOOTH_CONNECT`
- **Android 11 und älter**: `BLUETOOTH`, `BLUETOOTH_ADMIN`, `ACCESS_FINE_LOCATION`

## Projektstruktur

```
app/src/main/java/de/thw/cbrn/strahler/
├── MainActivity.kt              # Einstiegspunkt, setzt das Compose-UI
├── model/StrahlerModel.kt       # Config, Nuklid- und Einheiten-Tabellen
├── ble/
│   ├── StrahlerGatt.kt          # Service-/Characteristic-UUIDs (= Firmware)
│   └── BleManager.kt            # Scan, Verbindung, serielle Read/Write-Queue
├── measure/
│   ├── DoseModel.kt             # Paket-Parsing + Dosis-/Auto-Range-Mathe (= Sonde)
│   ├── MeasurementEngine.kt     # Dauer-Scan, Quellen-Tracking, RSSI-Glättung, Audio
│   └── ClickPlayer.kt           # Synthetischer Geiger-Klick (AudioTrack)
└── ui/
    ├── StrahlerApp.kt           # Permission-Gate + Bottom-Navigation (Messen/Einrichten)
    ├── StrahlerViewModel.kt     # State, optimistische Updates, GATT-Writes, Messung
    ├── GeigerScreen.kt          # Rundinstrument (Canvas) + Klick-Audio-Toggle
    ├── ScanScreen.kt            # Geräteliste
    ├── ConfigScreen.kt          # Geräte-Vorschau + Bedienelemente
    └── theme/Theme.kt           # THW-Blau / E-Paper-Optik
```

## Technische Hinweise

- **Eine GATT-Operation gleichzeitig**: Android erlaubt nur eine ausstehende
  GATT-Operation. `BleManager` serialisiert alle Reads/Writes über eine Queue,
  die ausschließlich auf dem Main-Thread bearbeitet wird; die nächste Operation
  startet erst aus dem Completion-Callback der vorherigen.
- **API-33-Kompatibilität**: Sowohl die neue (`onCharacteristicRead` mit
  `value`-Parameter, `writeCharacteristic(chr, bytes, type)`) als auch die alte
  Callback-/Schreib-API werden je nach OS-Version bedient.
- Es findet **kein Pairing** statt — der Strahler-Dienst ist offen lesbar/schreibbar
  (entspricht der Firmware).

## Bekannte Grenzen

- Die App liest/schreibt nur die sechs GATT-Parameter. **Kontakt**-Anzeige und
  **Aus** (System-OFF) des Geräts sind reine Geräte-Funktionen ohne GATT-Pendant.
- Batteriestand wird vom Gerät nicht über GATT bereitgestellt und daher nicht
  angezeigt.
- Während eine App-Verbindung besteht, sendet das Gerät kein Advertising
  (SoftDevice-Limit: ein Peripheral-Slot) — nach dem Trennen läuft es mit den
  neuen Werten weiter. Ein gerade per GATT verbundener Strahler ist daher im
  **Messen**-Tab nicht sichtbar.
- Die im **Messen**-Tab angezeigte Dosisleistung ist eine RSSI-basierte
  **Näherung für Ausbildungszwecke** (kein kalibriertes Dosimeter) und hängt von
  Telefon-Antenne, Abschattung und Umgebung ab.
