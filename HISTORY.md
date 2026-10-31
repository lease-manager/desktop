# History – Desktop

Arbeitsprotokoll, damit bei der Weiterarbeit nicht bei null begonnen werden muss.
Neueste Einträge zuerst. Gesamtkonzept: `docs/concept/README.md`.

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
- Tests sichern Archivierung mit Dokumentverweisen und den Erhalt älterer PDF-Versionen ab.

## 2026-10-31 – Schritt 2: PDF mit QR-Code/Hash + Aufräumen

**PDF (`kienzle.desktop.pdf`)**
- `PdfStamper` stempelt auf jede Seite unten links einen QR-Code (ZXing) und setzt PDF-Metadaten
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
- **Schritt 3 – Scan-Import (Pfad B):** PDF/Bild importieren, QR via `QrCodes.decode` erkennen,
  automatisch zuordnen, sonst manuell aus offenen Verträgen wählen; unverändert speichern,
  Hash notieren, `ContractService.attachSigned(..., SCAN, ...)`.
- Drucken-Funktion (Pfad B Schritt 1).
- Schritt 4: `.leasepack`-Export/Import (Schema in `shared/`), dann Mobile-Apps.
- Sprachwechsel lädt die Ansicht neu → ungespeicherte Eingaben gehen verloren.
- Bekannt: Der Startordner `~/.lease-manager` wird beim ersten Start angelegt; Liste ist anfangs leer.
- Rechtlich klären: Schriftform § 585a BGB (Pfad B evtl. Standardfall).

## Hinweise
- Tests: `./gradlew test` (ZXing muss einmal online geladen werden, danach `--offline` möglich).
- Starten ohne Display: `xvfb-run -a ./gradlew run`.
