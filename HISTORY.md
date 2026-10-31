# History – Desktop

Work log so that work can be resumed without starting from scratch.
Newest entries first. Overall concept: `docs/concept/README.md`.

## 2026-10-31 – Live header

- The header above the form (contract name and "Lessor: …") now follows the input fields while typing
  (`ContractFormController.setOnHeaderChanged`) instead of only updating when a contract is selected.
  The lessor line is hidden while no lessor is entered. Test in `ContractFormControllerTest`.

## 2026-10-31 – Section numbering in the contract

- PDF and UI now number the sections: § 4 payment default and jurisdiction, § 5 early termination,
  § 6 other agreements. The model fields `paragraphFour`/`paragraphFive` keep their names (stored
  contracts stay compatible); only the displayed numbers changed. The "(§ 4)" suffix was dropped from the
  "Term and termination" form section. PDFs generated earlier keep the old numbering until regenerated.

## 2026-10-31 – Permanently delete contracts

- New "Delete" button (next to "Archive"): permanently removes a wrongly created contract together with its
  documents (after a confirmation prompt). Only for contracts that are not signed yet; signed contracts can
  still only be archived (button disabled, `ContractService.delete` rejects them).
- `ContractRepository.delete` (record + `documents/<id>/`), passed through Lifecycle/Workflow/UiActions.
- Tests in `ContractServiceTest`.

## 2026-10-31 – Hash check on scan import

- The hash in the scan's QR code is compared with the hash embedded in the original PDF (`LeaseContentHash`)
  (`ScanImportService.matchesOriginal`, `ContractDocumentUseCase.isCurrentScan`).
- If it differs (the scan comes from an older version, e.g. after regenerating the PDF), a warning is shown
  (`scan.outdated`); the user can still accept or cancel. Originals without an embedded hash (legacy data)
  are not rejected.
- `detectContractId` → `detectPayload` (returns `QrPayload`). Tests added to `ScanImportServiceTest`.

## 2026-10-31 – Scan import (step 3, path B)

- New "Import scan" button: choose a file (PDF, PNG, JPG) → the QR code is read → the contract is matched
  (confirmed by the user); without a detected QR code the user picks from the contracts in status
  "awaiting signature". The scan is stored **unchanged** as `documents/<id>/signed-scan-<uuid>.<ext>`,
  its SHA-256 is recorded, status → `SIGNED` (`attachSigned(..., SCAN, ...)`).
- `pdf/ScanImportService` (`detectPayload`, `attachScan`; allowed extensions, 50 MB size limit);
  passed through `ContractDocumentUseCase`, `ContractWorkflow`, `ContractUiActions`.
- QR detection runs in a background task (wait cursor) so the UI does not block.
- "Open PDF" now opens the signed document if present (otherwise the original PDF);
  printing still uses the original.
- **Bug fixed:** The QR code was at the bottom left and covered the IBAN box on page 1 → not readable there.
  It is now at the top right (with the ID text next to it) on every page.
- `QrCodes.decode`: corner search (top right, bottom left for older PDFs) instead of searching the whole
  lower half (hit in ~1 s instead of a >30 s failed attempt).
- Tests: `ScanImportServiceTest` (PDF/image scan, no QR, wrong extension, wrong status, document choice).
- Known: rotated or very noisy scans may not be detected (manual selection then). Real-world test with a
  real scan is still pending.

## 2026-10-31 – Revoke signature, print PDF, Linux fixes

- "Given for signature" is reversible: `ContractService.revokeAwaitingSignature` resets
  `AWAITING_SIGNATURE` to `PDF_CREATED` (only from this status; `SIGNED` stays locked).
  Passed through `ContractLifecycleUseCase`, `ContractWorkflow`, `ContractUiActions`; new button
  "Revoke signature" (DE/EN). Test in `ContractServiceTest`.
- Print PDF: new button; `PdfPrinter` shows the JavaFX `PrinterJob` print dialog (OS independent),
  renders the pages via PDFBox (200 dpi) and prints them as images. Cancelling the dialog is a no-op,
  a missing printer/failure yields a localized error message. Same path validation as "Open PDF".
- "Open PDF": `xdg-open` on Linux, `open` on macOS, otherwise fallback `Desktop.open`.
- FXML namespace version unified to `17.0.17` (fixes the "FXML 23.0.1 vs. runtime 17.0.17" warning).
- Known/harmless: On Linux, `Gdk-WARNING XSetErrorHandler() called with a GDK error trap pushed` appears
  (AWT and JavaFX/GTK in the same process). `java.awt.headless=true` is **not** a fix: it prevents the
  print dialog and was removed again.
- Not implemented: transfer to smartphone (roadmap step 5/7); so far only manual `.leasepack`
  export/import.
- Print dialog and print output are not covered by automated tests (they need a printer/display).

## 2026-10-31 – UI state refactoring + increased test coverage

- Follow-up stability refactoring in the list state:
  - `ContractListState` handles replacement by the same ID consistently (`addEntry` replaces instead of duplicating).
  - `currentEntry` is cleaned up properly on filter/visibility changes and removed entries.
  - `ensureVisible(...)` checks visibility via IDs in the filtered list and keeps the selection state consistent.
- Tests adapted to the current `ContractEntry` model (`documents`/`signature` instead of legacy fields).
- JavaFX test initialization unified in `ContractListStateTest` (`JavaFxTestSupport.startToolkit()` via `@BeforeAll`).
- Test fixtures consolidated:
  - central test helper `TestContracts` introduced.
  - new API `sampleContract(String name)` added; existing `sampleContract()` delegates to it.
  - several tests switched to the shared fixture + `toBuilder()` overrides.
- Readability improved:
  - manual 37-parameter test constructions reduced further.
  - indentation in `LeaseContract.toBuilder()` cleaned up.
- Validation: full test suite passed after the refactoring (`./gradlew test`).

- `MainLayoutController` slimmed down further: list/filter/selection state moved to the new class
  `ContractListState`.
- `ContractScreenCoordinator` removed; duplicate state logic cleaned up.
- Restore flow from the archive stabilized:
  - A restored contract is immediately visible/selected in the main screen.
  - If an active filter hides the contract, the filter consistently switches to `ALL`.
- Additional tests:
  - `ContractListStateTest` (filter, visibility, remove/selection cases).
  - `ArchiveDialogTest` (load archive list, restore without selection is a no-op).
  - `MainLayoutControllerTest` (restore resets the filter and selects the entry).
- Coverage improved (JaCoCo):
  - Overall: instructions **77% → 80%**, branches **54% → 58%**.
  - `ArchiveDialog`: **0% → 74%**.
  - `MainLayoutController`: **27% → 38%**.
  - `ContractListState`: **31% → 67%**.

## 2026-10-31 – Contract archive and PDF versions

- Archiving moves the contract record to `archive/contracts/<uuid>.json` and its documents to
  `archive/documents/<uuid>/`; the contract is thereby removed from the active list.
- Archive view in the desktop UI shows archived contracts; restoring moves the record and documents back
  to the active store.
- Confirmation dialog and button name the operation as archiving in German and English.
- Every newly generated original PDF gets a unique file name; regenerating does not overwrite an earlier
  version and does not invalidate its hash.
- Clean code: `JsonContractRepository` is now only orchestration (`relocate`); file moving lives in
  `FileMoveTransaction` (command with undo/rollback), path validation and target names in `DocumentRelocator`
  (strategy enum `Direction` ARCHIVE/RESTORE); archive window extracted from the controller as `ArchiveDialog`.
- Form extracted: `ContractForm.fxml` (via `fx:include`) + `ContractFormController` (`read()`/`fill()`,
  field configuration, date formats, district court selection). `MainLayoutController` shrinks from ~690 to
  ~320 lines; `ViewLoader` creates the main controller via injection, sub-controllers via default constructor.
- New JavaFX round-trip test `ContractFormControllerTest`: loads `ContractForm.fxml` headless, fills the
  contract via `fill()` and verifies reading it back via `read()`.
- Additional round-trip case without introductory rent: verifies that `introductoryRent*` and
  `rentIncreaseDate` consistently stay `null` when the checkbox is disabled.
- IBAN validation added (ISO 13616 modulo 97) including a localized error message; invalid IBANs are
  rejected on save.
- IBAN errors are now also shown as a red inline hint directly below the field; saving is disabled for an
  invalid IBAN.
- `ContractFormController` now offers `hasValidationErrors()` and the callback hook
  `setOnValidationChanged(...)` so the parent controller can drive the save button reactively without
  duplicating form logic.
- IBAN validation stays two-stage: inline feedback while typing **and** a hard check on the final
  read/save (`read()`), so programmatically set values are not left unchecked either.
- JavaFX tests extended: an invalid IBAN still raises `IllegalArgumentException` with a localized
  message; a valid IBAN sets no validation error state.
- Tests cover archiving with document references and the preservation of older PDF versions.

## 2026-10-31 – Step 2: PDF with QR code/hash + cleanup

**PDF (`kienzle.desktop.pdf`)**
- `PdfStamper` stamps a QR code on every page (ZXing; top right since the 2026-10-31 "scan import" entry,
  bottom left before) and sets PDF metadata
  (`/Keywords`, `LeaseContractId`, `LeaseContentHash`).
- QR payload: `lease:v1:<contractId>:<contentHash>` (`QrPayload`); `contentHash` = SHA-256 of the PDF
  *before* stamping. The SHA-256 of the finished file is stored in `Document.sha256` (`Hashing`).
- `QrCodes` can encode and decode (decoding was prepared for the scan import, still unused at that point).
- `PdfStorage` stores files under `<dataDir>/documents/<contractId>/` (relative paths).
- `ContractPdfService`: render → stamp → store → status `PDF_CREATED`.
- UI: buttons "Create PDF", "Open PDF", "Given for signature" (`AWAITING_SIGNATURE`).
- New dependencies: `com.google.zxing:core` and `:javase` 3.5.3.

**Cleanup (clean code)**
- Builder: `LeaseContract.builder()` / `toBuilder()` instead of the 37-parameter constructor.
- Facade: `app.ContractWorkflow` is the only entry point for the UI.
- Dependency injection: `app.AppContext` (composition root), `ViewLoader` with `controllerFactory`,
  constructor injection in `MainLayoutController` and `ContractPdfService`.
- Extracted from the controller: `ContractDefaults`, `ContractDetailRows`, `IbanFormatter`,
  `StatusFilter`, `ContractStatusText`, `ContractPdfValues`.
- Deliberately no singleton (DI replaces it) and no proxy (no use case); `toBuilder()` covers prototype.
- New tests: builder, defaults, IBAN, PDF values, StatusFilter, PdfStamper (QR readable on every page),
  ContractPdfService.

## 2026-10-31 – Step 1: data model, persistence, UI integration

**Model/persistence (`kienzle.desktop.contract`)**
- `ContractEntry` (UUID, `LeaseContract`, status, timestamps, documents, signature),
  `ContractStatus` (`DRAFT → PDF_CREATED → AWAITING_SIGNATURE → SIGNED`), `SignatureSource`,
  `DocumentKind`, `ContractDocument`, `SignatureInfo`.
- `ContractRepository` + `JsonContractRepository`: one file per contract under
  `~/.lease-manager/contracts/<uuid>.json` (written atomically).
- `ContractService` enforces the status flow; editing only in status `DRAFT`.

**Typing**
- `LeaseContract`: `LocalDate`, `MonthDay` (end of garden year), `BigDecimal` (size, rent); field `id`
  removed. `ContractFormats` formats/parses German numbers and dates.

**UI**
- Sample contracts removed; the list comes from the repository. Buttons New/Save/Delete,
  filter All/Open/Signed, status line (incl. source Mobile/Scan). Texts in DE and EN.
- `module-info`: new packages exported, Gson reflection on `kienzle.desktop(.contract)`.

## Open / next steps
- Scan import (step 3) is implemented, see above; real-world test with a real scan is still pending.
- Print function (path B step 1): implemented as a JavaFX print dialog, see above; real-world test pending.
- Hash comparison of the scan with the original: done (see "Hash check on scan import").
- Step 4: `.leasepack` export/import (schema in `shared/`), then mobile apps.
- Switching the language reloads the view → unsaved input is lost.
- Known: The start folder `~/.lease-manager` is created on first start; the list is empty at first.
- Legal clarification needed: written form § 585a BGB (path B possibly the standard case).

## Notes
- Tests: `./gradlew test` (ZXing must be downloaded online once, `--offline` possible afterwards).
- Start without a display: `xvfb-run -a ./gradlew run`.
