# Sicherheitshinweise / Security Policy

## Geltungsbereich

Dieses Projekt ist ein **privates Ausbildungs-Hilfsmittel**. Die App misst keine
Strahlung und steuert keine sicherheitsrelevanten Systeme. Sie kommuniziert
ausschließlich per Bluetooth Low Energy mit den Trainings-Strahlern des
CBRN-RN-Demonstrators und hat keinen Internetzugriff.

## Bekannte, bewusst akzeptierte Eigenschaften

- Der GATT-Dienst der Strahler ist **ohne Pairing** les- und schreibbar. Jedes
  BLE-Gerät in Funkreichweite kann die Trainings-Strahler umkonfigurieren. Das
  entspricht dem Design der Firmware und ist für den Übungsbetrieb gewollt.
- Die **Diagnose-PIN** ist im Quelltext hinterlegt. Sie schützt lediglich die
  Anzeige der Simulationsmechanik vor Übungsteilnehmern und ist kein
  Sicherheitsmerkmal.

## Meldung von Schwachstellen

Wenn du dennoch ein Problem findest, das über die oben genannten Punkte
hinausgeht, melde es bitte über ein
[GitHub-Issue](https://github.com/friedrichengel/CBRN_Demonstrator_RN_App/issues)
oder, falls es vertraulich sein soll, über die GitHub-Funktion
„Report a vulnerability" (Security Advisories) dieses Repositories.
