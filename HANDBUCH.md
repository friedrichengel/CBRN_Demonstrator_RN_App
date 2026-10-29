# Handbuch — CBRN-RN Strahler Konfigurator (Android-App)

**Mess- und Einrichtungs-App für den CBRN-RN-Demonstrator**
Privates Projekt, eingesetzt in der Ausbildung im THW OV Berlin
Friedrichshain-Kreuzberg

> **Hinweis:** Dies ist ein **privates Projekt** und **kein offizielles
> Produkt oder Ausbildungsmittel des THW**. Quellcode und Lizenz (MIT):
> <https://github.com/friedrichengel/CBRN_Demonstrator_RN_App>

---

## 1. Wozu die App dient

Die App übernimmt am Smartphone zwei Aufgaben des Demonstrator-Sets:

- **Messen** — das Handy verhält sich wie die **Sonde**: Es empfängt die
  Funksignale der Trainings-Strahler und zeigt eine daraus berechnete
  Dosisleistung als Rundinstrument mit Geigerzähler-Klicks an.
- **Einrichten** — das Handy ersetzt die umständliche Ein-Knopf-Bedienung am
  **Strahler**: Sender-Nummer, Nuklid, Dosisleistung, Einheit, Aktiv-Zustand
  und Reichweite lassen sich bequem per Bluetooth einstellen.

> **Wichtig:** Es wird **keine Strahlung gemessen und keine erzeugt.** Der
> angezeigte Wert entsteht ausschließlich aus der Funk-Signalstärke der
> Trainings-Strahler. Die App ist ein **Ausbildungsmittel**, kein Dosimeter,
> und darf niemals für reale Messungen oder Lagebeurteilungen verwendet werden.

Die Messgenauigkeit hängt zusätzlich von Antenne und Bauform des jeweiligen
Telefons ab — die Anzeige zweier verschiedener Handys am selben Ort kann
deshalb voneinander abweichen.

---

## 2. Voraussetzungen

| Punkt | Anforderung |
|---|---|
| Betriebssystem | Android 8.0 oder neuer |
| Funk | Bluetooth LE, im Gerät eingeschaltet |
| Für den Modus „Lang" (LR) | Telefon muss Bluetooth-5-Empfang beherrschen |

Ein Koppeln („Pairing") der Strahler ist **nicht** nötig und auch nicht
vorgesehen. Die Geräte tauchen nicht in den Bluetooth-Einstellungen von
Android auf, sondern nur in der App.

---

## 3. Erster Start und Berechtigungen

Beim ersten Start zeigt die App den Hinweis **„Bluetooth-Berechtigung
erforderlich"**. Auf **„Berechtigung erteilen"** tippen und die Systemabfrage
bestätigen.

| Android-Version | abgefragte Berechtigung |
|---|---|
| Android 12 und neuer | „Geräte in der Nähe" |
| Android 11 und älter | „Standort" — Android koppelt Bluetooth-Suchen dort an die Standortfreigabe |

Ohne diese Berechtigung kann die App weder messen noch Geräte finden. Die
Standortfreigabe wird ausschließlich technisch für die Bluetooth-Suche
benötigt; die App wertet keinen Standort aus.

Nach der Freigabe erscheint unten die Leiste mit den beiden Bereichen
**Messen** und **Einrichten**.

---

## 4. Bereich „Messen"

Der Messbetrieb startet **automatisch**, sobald der Bereich geöffnet wird, und
stoppt beim Verlassen wieder — das schont den Akku.

### 4.1 Aufbau des Bildschirms

| Bereich | Bedeutung |
|---|---|
| **Kopfzeile „Messen"** | mit zwei Schaltflächen rechts (siehe unten) |
| **Rundinstrument** | Zeiger mit Farbzonen und beschrifteter Skala |
| **Große Zahl** | aktuelle Dosisleistung, darunter die Einheit |
| **Hinweiszeile** | erscheint nur bei Problemen oder im Diagnose-Modus |

**Schaltflächen in der Kopfzeile (von rechts):**

| Symbol | Funktion |
|---|---|
| Lautsprecher | Klickton an- und ausschalten |
| Fernglas/Diagnose | Diagnose-Anzeige freischalten (PIN) bzw. wieder abschalten |

### 4.2 Farbzonen

| Anteil der Skala | Farbe |
|---|---|
| unter 50 % | grün |
| 50 % bis 75 % | gelb |
| 75 % bis 90 % | orange |
| ab 90 % | rot |

### 4.3 Automatische Skalierung

Skalenendwert und Einheit ergeben sich aus der **maximalen Dosisleistung des
gerade nächstgelegenen Strahlers**. Die Einheit wird selbstständig so gewählt,
dass der Wert lesbar bleibt:

| Vollausschlag des Strahlers | Einheit |
|---|---|
| unter 1 µSv/h | nSv/h |
| 1 bis 1 000 µSv/h | µSv/h |
| 1 000 µSv/h bis 1 Sv/h | mSv/h |
| ab 1 Sv/h | Sv/h |

Vollausschlag bedeutet: „so nah an dieser Quelle, wie die App sie seit dem
Öffnen des Mess-Bereichs überhaupt gesehen hat."

### 4.4 Zustandsanzeigen

| Anzeige | Bedeutung |
|---|---|
| **`----`** in Gelb | Kalibrierung läuft — die App mittelt die ersten ca. 3 Sekunden. Normal. |
| **„Messung inaktiv (Bluetooth aus?)"** | Bluetooth ist am Telefon ausgeschaltet oder die Suche wurde vom System gestoppt. |
| Zeiger auf Null, keine Klicks | Es wird gerade kein Strahler empfangen. |

### 4.5 Klickton

Der Lautsprecher-Schalter oben rechts schaltet die Geigerzähler-Klicks um. Die
Klickrate steigt mit der Dosisleistung: eine leise Grundrate ist immer da, bei
hoher Dosis werden es bis zu 50 Klicks pro Sekunde. Die Abstände sind leicht
zufällig, damit es wie ein echtes Zählrohr klingt.

Die Lautstärke folgt der Medien-Lautstärke des Telefons.

### 4.6 Diagnose-Anzeige (PIN-geschützt)

Die Diagnose zeigt technische Details der empfangenen Quellen. Sie verrät die
Simulationsmechanik und ist deshalb **standardmäßig ausgeblendet**.

**Freischalten:** auf das Diagnose-Symbol oben rechts tippen, die vierstellige
PIN eingeben und mit **OK** bestätigen. Bei falscher Eingabe erscheint „PIN
falsch". Die PIN ist dieselbe wie bei der Sonden-Firmware und bei der
Gerätebetreuung zu erfragen.

**Abschalten:** jederzeit durch erneutes Tippen auf dasselbe Symbol. Die
Freischaltung wird **nicht gespeichert** — nach jedem Neustart der App ist die
Diagnose wieder gesperrt.

**Angezeigt werden dann:**

| Zeile | Bedeutung |
|---|---|
| **Quellen** | Anzahl der momentan empfangenen Strahler |
| **Nächste** | Gerätename der stärksten Quelle (`Strahler-NNN`) |
| **Nuklid** | eingestelltes Nuklid dieser Quelle |
| **RSSI** | geglättete Signalstärke in dBm |
| **„Kalibrierung…"** | eine Quelle ist da, wird aber noch eingemittelt |
| **„Kein Signal"** | keine Quelle in Reichweite |

### 4.7 Mehrere Strahler

Die App verfolgt bis zu **16 Strahler gleichzeitig** und zeigt immer den mit
der **höchsten aktuellen Dosisleistung**. Ein Strahler gilt nach **5 Sekunden
ohne Empfang** als verschwunden.

Es werden sowohl Geräte im Modus **Normal** als auch im Modus **Lang (LR)**
gefunden — die App sucht auf beiden Funkverfahren gleichzeitig, sofern das
Telefon das unterstützt.

---

## 5. Bereich „Einrichten"

Hier wird ein Strahler per Bluetooth konfiguriert. Der Bereich hat zwei
Ansichten: die **Geräteliste** und den **Konfigurator**.

### 5.1 Gerät suchen und verbinden

1. Auf **„Strahler suchen"** tippen. Die Schaltfläche wechselt auf „Suche
   läuft… (Stoppen)".
2. Gefundene Geräte erscheinen als Karten mit **Name** (`Strahler-NNN`),
   **Bluetooth-Adresse** und **Signalstärke in dBm**.
3. Die Suche stoppt **automatisch nach 15 Sekunden**. Danach kann sie erneut
   gestartet werden.
4. Auf die gewünschte Karte tippen. Die App zeigt „Verbinde…", dann „Lese
   Konfiguration…" und wechselt anschließend in den Konfigurator.

Solange „Kein Gerät gefunden" steht, wurde nichts empfangen — mögliche
Ursachen siehe Abschnitt 7.

### 5.2 Der Konfigurator

Oben steht der Gerätename, links davon der **Zurück-Pfeil zum Trennen**.

Darunter folgt eine **Vorschau im E-Paper-Look**, die das Display des Geräts
nachbildet: Sender-Nummer `Q001`, Zustandsfeld `AN` / `AUS` / `AN/LR`, Nuklid
und maximale Dosisleistung. So lässt sich ohne Blick auf das Gerät prüfen, was
dort gerade steht.

> **Jede Änderung wird sofort auf das Gerät geschrieben.** Es gibt keine
> Speichern-Schaltfläche und keinen Abbruch.

### 5.3 Die Einstellungskarten

| Karte | Bedienelement | Wertebereich |
|---|---|---|
| **Sender-Nr** | Zahlenfeld + Schrittknöpfe ± 1 und ± 10 | 1 … 254 |
| **Nuklid** | Auswahlliste | Cs-137, Co-60, Am-241, Ir-192, Ra-226, Th-232, U-238, Pu-239, Se-75 |
| **Max. Dosisleistung** | Zahlenfeld + Schrittknöpfe ± 1, ± 10, ± 100, ± 1000 | 0 … 65535 |
| **Einheit** | Auswahlliste | nSv/h, µSv/h, mSv/h, Sv/h |
| **Aktiv** | Schalter | „Sendet (Advertising an)" / „Aus" |
| **Reichweite** | Schalter | „Normal (1 Mbps, ~30–50 m)" / „Lang (Coded PHY, ~150–200 m)" |

Bei den Zahlenfeldern kann der Wert auch direkt eingetippt werden; die App
begrenzt automatisch auf den zulässigen Bereich.

> **Achtung bei der Einheit:** Der Zahlenwert wird beim Wechsel der Einheit
> **nicht umgerechnet.** Aus `250 µSv/h` wird `250 mSv/h`. Deshalb immer
> **zuerst die Einheit** wählen und **danach** die Dosisleistung einstellen.

### 5.4 Verbindung trennen

Auf den **Zurück-Pfeil** oben links tippen. Die App kehrt zur Geräteliste
zurück.

> Solange die App verbunden ist, **sendet der Strahler nicht** — er hat nur
> einen Verbindungsplatz. Nach dem Trennen läuft er automatisch mit den neuen
> Werten weiter. Ein gerade verbundener Strahler ist deshalb im Bereich
> **Messen** nicht sichtbar.

---

## 6. Typische Abläufe

### Mehrere Strahler für eine Übung einrichten

Für jedes Gerät:

1. Bereich **Einrichten** → **„Strahler suchen"**.
2. Gerät antippen und verbinden.
3. **Sender-Nr** vergeben — pro Gerät eine eigene Nummer.
4. **Nuklid** nach Szenario wählen.
5. **Einheit** setzen, **danach** die **Max. Dosisleistung**.
6. **Reichweite** setzen — bei allen Geräten und der Sonde **gleich**.
7. **Aktiv** einschalten, wenn das Gerät ausgelegt werden soll.
8. Mit dem **Zurück-Pfeil** trennen, damit das Gerät wieder sendet.

### Aufbau vor Übungsbeginn prüfen

1. Bereich **Messen** öffnen und einmal am Rand des Geländes entlanggehen.
2. Bei Bedarf die Diagnose freischalten und in der Zeile **Quellen** prüfen,
   ob alle ausgelegten Strahler gefunden werden.
3. Diagnose vor Übungsbeginn wieder abschalten.

---

## 7. Störungen und Abhilfe

| Beobachtung | Mögliche Ursache und Abhilfe |
|---|---|
| „Bluetooth-Berechtigung erforderlich" bleibt stehen | Berechtigung wurde abgelehnt. In den Android-Einstellungen unter App → Berechtigungen nachtragen. |
| „Messung inaktiv (Bluetooth aus?)" | Bluetooth am Telefon einschalten. |
| „Kein Gerät gefunden" | Strahler ausgeschaltet, außer Reichweite oder gerade mit einer anderen App verbunden. Suche erneut starten. |
| Strahler im Modus „Lang" wird nicht gefunden | Telefon beherrscht kein Bluetooth-5-Coded-PHY. Strahler vorübergehend über den Geräteknopf auf „Normal" stellen. |
| Anzeige bleibt bei `----` | Kalibrierung (ca. 3 s). Bleibt es dauerhaft, wird nichts empfangen. |
| Wert schwankt oder fällt verzögert | Normal. Abschattung durch Körper, Wände oder Fahrzeuge; die Anzeige mittelt bewusst über 3 Sekunden. |
| Zwei Handys zeigen unterschiedliche Werte | Normal — die Anzeige hängt von der Antenne des Telefons ab. |
| Keine Klicks hörbar | Lautsprecher-Symbol oben rechts prüfen; Medien-Lautstärke des Telefons prüfen. |
| Verbindung bricht ab | Abstand zu groß oder Akku des Strahlers leer. Erneut suchen und verbinden. |
| Verbundener Strahler fehlt im Bereich „Messen" | So gewollt — während einer Verbindung sendet das Gerät nicht. Trennen. |
| Änderung kommt am Gerät nicht an | Verbindung prüfen (Kopfzeile zeigt den Gerätenamen). Trennen und neu verbinden. |

---

## 8. Grenzen der App

- Nur die sechs Konfigurationswerte sind erreichbar. **Kontakt-Anzeige** und
  **Ausschalten** des Strahlers sind reine Gerätefunktionen und nur am
  Geräteknopf verfügbar.
- Der **Akkustand des Strahlers** wird nicht über Funk bereitgestellt und
  deshalb nicht angezeigt.
- Während einer Verbindung sendet der Strahler nicht.
- Die Messanzeige ist eine **Näherung für Ausbildungszwecke**.

---

## 9. Technischer Anhang

*Dieser Abschnitt richtet sich an Ausbilder und die Gerätebetreuung.*

### 9.1 Suche und Verbindung

| Größe | Wert |
|---|---|
| Namensfilter bei der Suche | Name beginnt mit `Strahler` |
| Suchdauer im Bereich „Einrichten" | 15 Sekunden, danach automatischer Stopp |
| Suchverfahren | Legacy **und** Extended Advertising, alle unterstützten PHY |
| Kopplung | keine — der Dienst ist offen les- und schreibbar |
| Gleichzeitige Funkbefehle | eine; die App reiht Lese- und Schreibvorgänge auf |

### 9.2 Konfigurationsschnittstelle (GATT)

Dienst `0x1900` des Strahlers:

| Kennung | Parameter | Format |
|---|---|---|
| `0x2B00` | Sender-Nr | uint8, 1–254 |
| `0x2B01` | Nuklid-ID | uint8, 1–9 |
| `0x2B02` | Max. Dosisleistung | uint16 **Little Endian**, 0–65535 |
| `0x2B03` | Aktiv | 0 = aus, 1 = an |
| `0x2B04` | Reichweite | 0 = Normal, 1 = Lang |
| `0x2B05` | Einheiten-Index | uint8, 0–3 |

Änderungen werden einzeln geschrieben; die Anzeige wird sofort aktualisiert
und erst danach durch die Antwort des Geräts bestätigt.

### 9.3 Empfangenes Funkprotokoll (Bereich „Messen")

Ausgewertet werden die Herstellerdaten der Advertising-Pakete — ohne
Verbindung. Android liefert die Daten ohne die zweistellige Firmenkennung,
weshalb die Bytezählung gegenüber der Firmware um zwei verschoben ist.

| Byte (App) | Inhalt |
|---|---|
| 0 | Protokollversion, muss `0x02` sein |
| 1 | Sender-Nummer |
| 2 | Nuklid-Kennung (1–9) |
| 3–4 | max. Dosisleistung, Ganzzahl **Big Endian** |
| 5 | Einheiten-Index (0 = nSv/h, 1 = µSv/h, 2 = mSv/h, 3 = Sv/h) |
| 6 | Sendeleistung in dBm |
| 7 | Prüfsumme (XOR über Byte 0–6) |

Pakete mit falscher Version oder falscher Prüfsumme werden verworfen.

### 9.4 Rechenmodell der Dosisleistung

Identisch zur Sonden-Firmware:

```
I / I_max = 10 ^ ( ΔRSSI / (5 · n) )      mit ΔRSSI = RSSI_aktuell − RSSI_beste
```

| Größe | Wert |
|---|---|
| Pfadverlust-Exponent n | 12 |
| Mittelung Stufe 1 | Median je 1-Sekunden-Fenster |
| Mittelung Stufe 2 | gleitender Mittelwert über 3 Sekundenwerte |
| Kalibrierdauer nach Erstkontakt | ca. 3 Sekunden |
| Timeout einer Quelle | 5 Sekunden |
| maximale Anzahl Quellen | 16 |
| Klickrate | 0,3 + 0,5 × Dosisleistung [µSv/h], begrenzt auf 50 pro Sekunde |

### 9.5 Berechtigungen

| Android-Version | Berechtigungen |
|---|---|
| ab 12 (API 31) | `BLUETOOTH_SCAN` (mit `neverForLocation`), `BLUETOOTH_CONNECT` |
| bis 11 | `BLUETOOTH`, `BLUETOOTH_ADMIN`, `ACCESS_FINE_LOCATION` |

### 9.6 Diagnose-PIN

Die vierstellige PIN ist in der App hinterlegt und identisch mit der der
Sonden-Firmware. Sie ist bei der Gerätebetreuung zu erfragen und sollte nicht
an Übungsteilnehmer weitergegeben werden.

---

*Handbuch zur Android-App „CBRN-RN Strahler Konfigurator" · privates Projekt, kein offizielles THW-Produkt · MIT-Lizenz*
