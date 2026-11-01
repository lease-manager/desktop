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