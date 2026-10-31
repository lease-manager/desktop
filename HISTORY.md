# History – Desktop

Arbeitsprotokoll, damit bei der Weiterarbeit nicht bei null begonnen werden muss.
Neueste Einträge zuerst. Gesamtkonzept: `docs/concept/README.md`.

## 2026-10-31 – Verträge endgültig löschen

- Neuer Button „Löschen“ (neben „Archivieren“): entfernt einen falsch angelegten Vertrag samt Dokumenten
  endgültig (mit Sicherheitsabfrage). Nur für Verträge, die noch nicht unterschrieben sind; unterschriebene
  Verträge lassen sich weiterhin nur archivieren (Button deaktiviert, `ContractService.delete` lehnt ab).
- `ContractRepository.delete` (Datensatz + `documents/<id>/`), durchgereicht über Lifecycle/Workflow/UiActions.
- Tests in `ContractServiceTest`.

## 2026-10-31 – Hash-Abgleich beim Scan-Import

- Der Hash im QR-Code des Scans wird mit dem im Original-PDF eingebetteten Hash (`LeaseContentHash`)
  verglichen (`ScanImportService.matchesOriginal`, `ContractDocumentUseCase.isCurrentScan`).
- Weicht er ab (Scan stammt von einer älteren Fassung, z. B. nach erneutem PDF-Erzeugen), erscheint eine
  Warnung (`scan.outdated`); der Nutzer kann trotzdem übernehmen oder abbrechen. Originale ohne
  eingebetteten Hash (Altbestand) werden nicht beanstandet.
- `detectContractId` → `detectPayload` (liefert `QrPayload`). Tests in `ScanImportServiceTest` ergänzt.

## 2026-10-31 – Scan-Import (Schritt 3, Pfad B)

- Neuer Button „Scan importieren“: Datei (PDF, PNG, JPG) wählen → QR-Code wird gelesen → Vertrag wird
  zugeordnet (Bestätigung durch den Nutzer); ohne erkannten QR-Code Auswahl aus den Verträgen im Status
  „wartet auf Unterschrift“. Der Scan wird **unverändert** unter `documents/<id>/signed-scan-<uuid>.<ext>`
  gespeichert, SHA-256 notiert, Status → `SIGNED` (`attachSigned(..., SCAN, ...)`).
- `pdf/ScanImportService` (`detectPayload`, `attachScan`; erlaubte Endungen, Größenlimit 50 MB);
  Durchreichung über `ContractDocumentUseCase`, `ContractWorkflow`, `ContractUiActions`.
- QR-Erkennung läuft im Hintergrund-Task (Warte-Cursor), damit die UI nicht blockiert.
- „PDF öffnen“ öffnet jetzt das unterschriebene Dokument, falls vorhanden (sonst das Original-PDF);
  Drucken nutzt weiter das Original.
- **Fehler behoben:** Der QR-Code lag unten links und überdeckte auf Seite 1 den IBAN-Kasten → dort nicht
  lesbar. Er sitzt jetzt oben rechts (inkl. ID-Text links daneben) auf jeder Seite.
- `QrCodes.decode`: Ecken-Suche (oben rechts, für ältere PDFs unten links) statt Suche über die ganze untere
  Hälfte (Treffer ~1 s statt >30 s Fehlversuch).
- Tests: `ScanImportServiceTest` (PDF-/Bild-Scan, kein QR, falsche Endung, falscher Status, Dokumentwahl).
- Bekannt: verdrehte oder stark verrauschte Scans werden evtl. nicht erkannt (dann manuelle Auswahl). Praxistest mit echtem Scan steht aus.

## 2026-10-31 – Unterschrift zurücknehmen, PDF drucken, Linux-Fixes

- „Zur Unterschrift gegeben“ ist umkehrbar: `ContractService.revokeAwaitingSignature` setzt
  `AWAITING_SIGNATURE` zurück auf `PDF_CREATED` (nur aus diesem Status; `SIGNED` bleibt gesperrt).
  Durchgereicht über `ContractLifecycleUseCase`, `ContractWorkflow`, `ContractUiActions`; neuer Button
  „Unterschrift zurücknehmen“ (DE/EN). Test in `ContractServiceTest`.
- PDF drucken: neuer Button; `PdfPrinter` zeigt den JavaFX-`PrinterJob`-Druckdialog (betriebssystemunabhängig),
  rendert die Seiten per PDFBox (200 dpi) und druckt sie als Bild. Abbruch im Dialog ist ein No-Op,
  fehlender Drucker/Fehlschlag liefert lokalisierte Fehlermeldung. Dieselbe Pfadprüfung wie bei „PDF öffnen“.
- „PDF öffnen“: unter Linux `xdg-open`, unter macOS `open`, sonst Fallback `Desktop.open`.
- FXML-Namespace-Version auf `17.0.17` vereinheitlicht (behebt die Warnung „FXML 23.0.1 vs. Runtime 17.0.17“).
- Bekannt/harmlos: Unter Linux erscheint `Gdk-WARNING XSetErrorHandler() called with a GDK error trap pushed`
  (AWT und JavaFX/GTK im selben Prozess). `java.awt.headless=true` ist **keine** Lösung: Es verhindert den
  Druckdialog und wurde wieder entfernt.
- Nicht umgesetzt: Übertragung aufs Smartphone (Roadmap Schritt 5/7); bisher nur manueller
  `.leasepack`-Export/Import.
- Druckdialog und Druckausgabe sind nicht automatisiert getestet (benötigen Drucker/Display).

## 2026-10-31 – UI-State-Refactoring + Testabdeckung erhöht

- Nachlaufendes Stabilitäts-Refactoring im Listen-State:
  - `ContractListState` behandelt Austausch per gleicher ID konsistent (`addEntry` ersetzt statt zu duplizieren).
  - `currentEntry` wird bei Filter-/Sichtbarkeitswechseln und entfernten Einträgen sauber bereinigt.
  - `ensureVisible(...)` prüft Sichtbarkeit über IDs in der gefilterten Liste und hält Selection-State konsistent.
- Tests an das aktuelle `ContractEntry`-Modell angepasst (`documents`/`signature` statt Alt-Felder).
- JavaFX-Testinitialisierung in `ContractListStateTest` vereinheitlicht (`JavaFxTestSupport.startToolkit()` via `@BeforeAll`).
- Test-Fixtures konsolidiert:
  - zentrale Testhilfe `TestContracts` eingeführt.
  - neue API `sampleContract(String name)` ergänzt; bestehendes `sampleContract()` delegiert darauf.
  - mehrere Tests auf die gemeinsame Fixture + `toBuilder()`-Overrides umgestellt.
- Lesbarkeit verbessert:
  - manuelle 37-Parameter-Testkonstruktionen weiter reduziert.
  - Einrückung in `LeaseContract.toBuilder()` bereinigt.
- Validierung: nach Refactoring vollständige Testsuite erfolgreich (`./gradlew test`).

- `MainLayoutController` weiter entschlackt: Listen-/Filter-/Selection-State in neue Klasse
  `ContractListState` ausgelagert.
- `ContractScreenCoordinator` entfernt; doppelte Zustandslogik bereinigt.
- Restore-Flow aus dem Archiv stabilisiert:
  - Wiederhergestellten Vertrag sofort im Hauptscreen sichtbar/ausgewählt.
  - Bei aktivem Filter, der den Vertrag ausblendet, wird konsistent auf `ALL` gewechselt.
- Zusätzliche Tests:
  - `ContractListStateTest` (Filter-, Sichtbarkeits-, Remove-/Selection-Fälle).
  - `ArchiveDialogTest` (Archivliste laden, Restore ohne Selektion als No-Op).
  - `MainLayoutControllerTest` (Restore setzt Filter zurück und selektiert Eintrag).
- Coverage verbessert (JaCoCo):
  - Gesamt: Instruktionen **77% → 80%**, Branches **54% → 58%**.
  - `ArchiveDialog`: **0% → 74%**.
  - `MainLayoutController`: **27% → 38%**.
  - `ContractListState`: **31% → 67%**.

## 2026-10-31 – Vertragsarchiv und PDF-Versionen

- Archivieren verschiebt den Vertragsdatensatz nach `archive/contracts/<uuid>.json` und seine Dokumente
  nach `archive/documents/<uuid>/`; der Vertrag wird dadurch aus der aktiven Liste entfernt.
- Archivansicht in der Desktop-UI zeigt archivierte Verträge; Wiederherstellen verschiebt Datensatz und
  Dokumente zurück in die aktive Ablage.
- Bestätigungsdialog und Button benennen den Vorgang in Deutsch und Englisch als Archivierung.
- Jedes neu erzeugte Original-PDF erhält einen eindeutigen Dateinamen; erneutes Erzeugen überschreibt
  keine frühere Version und macht deren Hash nicht ungültig.
- Clean Code: `JsonContractRepository` nur noch Orchestrierung (`relocate`); Dateiverschiebung in
  `FileMoveTransaction` (Command mit Undo/Rollback), Pfadprüfung und Zielnamen in `DocumentRelocator`
  (Strategy-Enum `Direction` ARCHIVE/RESTORE); Archivfenster als `ArchiveDialog` aus dem Controller ausgelagert.
- Formular ausgelagert: `ContractForm.fxml` (per `fx:include`) + `ContractFormController` (`read()`/`fill()`,
  Feldkonfiguration, Datumsformate, Amtsgericht-Auswahl). `MainLayoutController` schrumpft von ~690 auf ~320 Zeilen;
  `ViewLoader` erzeugt den Haupt-Controller per Injektion, Unter-Controller per Standardkonstruktor.
- Neuer JavaFX-Round-Trip-Test `ContractFormControllerTest`: lädt `ContractForm.fxml` headless, füllt den
  Vertrag per `fill()` und verifiziert die Rücklese per `read()`.
- Zusätzlicher Round-Trip-Fall ohne Einführungszins: prüft, dass `introductoryRent*` und
  `rentIncreaseDate` bei deaktivierter Checkbox konsistent `null` bleiben.
- IBAN-Validierung ergänzt (ISO-13616-Modulo-97) inkl. lokalisierter Fehlermeldung; ungültige IBANs werden
  beim Speichern abgelehnt.
- IBAN-Fehler jetzt zusätzlich als roter Inline-Hinweis direkt unter dem Feld; Speichern ist bei ungültiger
  IBAN deaktiviert.
- `ContractFormController` bietet jetzt `hasValidationErrors()` und Callback-Hook
  `setOnValidationChanged(...)`, damit der übergeordnete Controller den Speichern-Button reaktiv
  steuern kann, ohne Formularlogik zu duplizieren.
- IBAN-Validierung bleibt zweistufig: Inline-Feedback während der Eingabe **und** harte Prüfung beim
  finalen Lesen/Speichern (`read()`), damit auch programmatisch gesetzte Werte nicht ungeprüft bleiben.
- JavaFX-Tests erweitert: ungültige IBAN erzeugt weiterhin `IllegalArgumentException` mit lokalisierter
  Meldung; gültige IBAN setzt keinen Validierungsfehlerstatus.
- Tests sichern Archivierung mit Dokumentverweisen und den Erhalt älterer PDF-Versionen ab.

## 2026-10-31 – Schritt 2: PDF mit QR-Code/Hash + Aufräumen

**PDF (`kienzle.desktop.pdf`)**
- `PdfStamper` stempelt auf jede Seite einen QR-Code (ZXing; ab 2026-10-31 „Scan-Import“ oben rechts,
  vorher unten links) und setzt PDF-Metadaten
  (`/Keywords`, `LeaseContractId`, `LeaseContentHash`).
- QR-Payload: `lease:v1:<contractId>:<contentHash>` (`QrPayload`); `contentHash` = SHA-256 des PDFs
  *vor* dem Stempeln. SHA-256 der fertigen Datei steht in `Document.sha256` (`Hashing`).
- `QrCodes` kann encodieren und decodieren (Decode ist für den Scan-Import vorbereitet, noch ungenutzt).
- `PdfStorage` legt Dateien unter `<dataDir>/documents/<contractId>/` ab (relative Pfade).
- `ContractPdfService`: rendern → stempeln → speichern → Status `PDF_CREATED`.
- UI: Buttons „PDF erstellen“, „PDF öffnen“, „Zur Unterschrift gegeben“ (`AWAITING_SIGNATURE`).
- Neue Abhängigkeiten: `com.google.zxing:core` und `:javase` 3.5.3.

**Aufräumen (Clean Code)**
- Builder: `LeaseContract.builder()` / `toBuilder()` statt 37-Parameter-Konstruktor.
- Facade: `app.ContractWorkflow` ist der einzige Einstieg der UI.
- Dependency Injection: `app.AppContext` (Composition Root), `ViewLoader` mit `controllerFactory`,
  Konstruktor-Injektion in `MainLayoutController` und `ContractPdfService`.
- Aus dem Controller ausgelagert: `ContractDefaults`, `ContractDetailRows`, `IbanFormatter`,
  `StatusFilter`, `ContractStatusText`, `ContractPdfValues`.
- Bewusst kein Singleton (DI ersetzt es) und kein Proxy (kein Anwendungsfall); `toBuilder()` deckt Prototype ab.
- Neue Tests: Builder, Defaults, IBAN, PDF-Werte, StatusFilter, PdfStamper (QR lesbar auf jeder Seite),
  ContractPdfService.

## 2026-10-31 – Schritt 1: Datenmodell, Persistenz, UI-Anbindung

**Modell/Persistenz (`kienzle.desktop.contract`)**
- `ContractEntry` (UUID, `LeaseContract`, Status, Zeitstempel, Dokumente, Signatur),
  `ContractStatus` (`DRAFT → PDF_CREATED → AWAITING_SIGNATURE → SIGNED`), `SignatureSource`,
  `DocumentKind`, `ContractDocument`, `SignatureInfo`.
- `ContractRepository` + `JsonContractRepository`: eine Datei pro Vertrag unter
  `~/.lease-manager/contracts/<uuid>.json` (atomar geschrieben).
- `ContractService` erzwingt den Statusablauf; bearbeiten nur im Status `DRAFT`.

**Typisierung**
- `LeaseContract`: `LocalDate`, `MonthDay` (Gartenjahr-Ende), `BigDecimal` (Größe, Pachtzins);
  Feld `id` entfernt. `ContractFormats` formatiert/parst deutsche Zahlen und Daten.

**UI**
- Beispielverträge entfernt; Liste kommt aus dem Repository. Buttons Neu/Speichern/Löschen,
  Filter Alle/Offen/Unterschrieben, Statuszeile (inkl. Quelle Mobil/Scan). Texte in DE und EN.
- `module-info`: neue Pakete exportiert, Gson-Reflection auf `kienzle.desktop(.contract)`.

## Offen / nächste Schritte
- Scan-Import (Schritt 3) ist umgesetzt, siehe oben; Praxistest mit echtem Scan steht aus.
- Drucken-Funktion (Pfad B Schritt 1): als JavaFX-Druckdialog umgesetzt, siehe oben; Praxistest steht aus.
- Optional: Hash-Abgleich des Scans mit dem Original (QR enthält `contentHash` des ungestempelten PDFs).
- Schritt 4: `.leasepack`-Export/Import (Schema in `shared/`), dann Mobile-Apps.
- Sprachwechsel lädt die Ansicht neu → ungespeicherte Eingaben gehen verloren.
- Bekannt: Der Startordner `~/.lease-manager` wird beim ersten Start angelegt; Liste ist anfangs leer.
- Rechtlich klären: Schriftform § 585a BGB (Pfad B evtl. Standardfall).

## Hinweise
- Tests: `./gradlew test` (ZXing muss einmal online geladen werden, danach `--offline` möglich).
- Starten ohne Display: `xvfb-run -a ./gradlew run`.
