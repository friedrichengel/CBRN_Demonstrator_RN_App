# CBRN-RN Strahler Konfigurator (Android)

Android-App zum Auffinden und Konfigurieren der **Strahler**-Geräte
(Firmware: `CBRN_Demonstrator_RN_Probe`) über **BLE/GATT** — als handytaugliche
Alternative zur Bedienung über den Geräteknopf.

Die Oberfläche bildet die E-Paper-Statusanzeige des Geräts nach (Q-Nummer,
Nuklid, max. Dosisleistung, Status `AN/AUS/LR`) und stellt darunter alle
einstellbaren Parameter als mobile Bedienelemente bereit.

## Was die App kann

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
2. **„Strahler suchen"** tippen. Gefundene Geräte erscheinen mit Name und
   Signalstärke (RSSI). Der Scan stoppt automatisch nach 15 s.
3. Auf ein Gerät tippen → App verbindet sich und liest die Konfiguration.
4. Werte ändern — jede Änderung wird **sofort** auf das Gerät geschrieben.
   Die Vorschaukarte oben spiegelt den Gerätestatus wider.
5. Über den **Zurück-Pfeil** oben links trennen.

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
└── ui/
    ├── StrahlerApp.kt           # Permission-Gate + Navigation
    ├── StrahlerViewModel.kt     # State, optimistische Updates, GATT-Writes
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
  neuen Werten weiter.
