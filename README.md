# Lease Manager – Desktop

JavaFX desktop application for creating and managing lease contracts, generating PDFs, importing
signed scans, and archiving contracts.

## Requirements and commands

- Java 17 (Gradle uses the configured toolchain)
- Start the application: `./gradlew run`
- Run the tests: `./gradlew test`

Run these commands from this directory.

## Local data

The default data directory is `~/.lease-manager`. It contains contract records, PDFs, archived
records, the UI language configuration and the local package-signing key.

Use **Daten sichern / wiederherstellen** (German UI) or **Back up / restore data** (English UI) to
create a `.leasebackup` file or restore one. Restoring creates a separate data directory and opens
the restored copy; it does not overwrite the current saved data. Save the current contract first:
unsaved form input is not part of a backup.

Backups contain personal contract information and the local signing key. They are not encrypted;
store them in a private location and keep them separate from the live data directory. On POSIX
systems the application restricts the backup file to the current user.

`.leasepack` signatures are currently verifiable only with the key in the same data directory.
For that reason, lease packages are not yet transferable between installations. A full backup
preserves the key so the restored copy retains its local package trust.

## Release package

`./gradlew packageApp` builds a self-contained app image (bundled Java runtime, no installed Java needed)
in `build/jpackage/LeaseManager`; start it with `build/jpackage/LeaseManager/bin/LeaseManager`
(`.exe` on Windows). It is built for the operating system it runs on. The image has been smoke-tested on Linux only.

## Quick user guide

1. **New contract:** fill in lessor, tenant, parcels (add rows with *Flurstück hinzufügen*), term, rent and
   bank data; the total rent is calculated from the parcels. Save.
2. **PDF:** open the preview, print it or save the PDF. It carries a QR code with the contract ID and hash.
3. **Sign on paper:** print, sign, scan, then import the scan (the QR code identifies the contract; a warning
   appears if the scan belongs to an older version). Signed contracts can only be archived, not deleted.
4. **Sign on mobile:** export a `.leasepack`, sign it in the Android app and import the returned
   `.leasesigned` file with *Mobil-Unterschrift importieren*.
5. **Backup:** *Daten sichern / wiederherstellen* creates and restores `.leasebackup` files.

