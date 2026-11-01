package kienzle.desktop.app;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.EnumSet;
import java.util.HexFormat;

/** Local package authenticity checks for exported lease archives. */
public final class PackageSecurity {
    private static final String KEY_FILE = "package-signing.key";
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final int KEY_LENGTH = 32;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PackageSecurity() {
    }

    public static String sign(Path dataDir, ContractPackageService.LeasePackageManifest manifest) throws IOException {
        byte[] key = resolveSigningKey(dataDir);
        return HexFormat.of().formatHex(signature(key, manifest));
    }

    private static byte[] signature(byte[] key, ContractPackageService.LeasePackageManifest manifest)
            throws IOException {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(key, HMAC_ALGORITHM));
            return mac.doFinal(manifest.signedPayload().getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IOException("Unable to sign package", e);
        }
    }

    /** v2 packages are checked against the Ed25519 key of a paired device (or this one); v1 against the local HMAC. */
    public static void validate(Path dataDir, PairingService pairing,
                                ContractPackageService.LeasePackageManifest manifest) throws IOException {
        if (!ContractPackageService.PACKAGE_SCHEMA_V2.equals(manifest.schemaVersion())) {
            validate(dataDir, manifest);
            return;
        }
        if (manifest.senderId().isBlank()) {
            throw new IOException("Package sender is missing");
        }
        byte[] payload = manifest.withoutSignature().signedPayload().getBytes(StandardCharsets.UTF_8);
        if (!pairing.verifyPeerOrSelf(manifest.senderId(), payload, manifest.packageSignature())) {
            throw new IOException("Package is not signed by a paired device (contract " + manifest.contractId() + ")");
        }
    }

    public static void validate(Path dataDir, ContractPackageService.LeasePackageManifest manifest) throws IOException {
        if (manifest.packageSignature() == null || manifest.packageSignature().isBlank()) {
            throw new IOException("Package signature is missing");
        }
        byte[] actual;
        try {
            actual = HexFormat.of().parseHex(manifest.packageSignature());
        } catch (IllegalArgumentException e) {
            throw new IOException("Package signature is malformed", e);
        }
        if (actual.length != KEY_LENGTH) {
            throw new IOException("Package signature has an invalid length");
        }

        byte[] key = resolveExistingSigningKey(dataDir);
        byte[] expected = signature(key, manifest.withoutSignature());
        if (!MessageDigest.isEqual(expected, actual)) {
            throw new IOException("Package signature mismatch for contract " + manifest.contractId());
        }
    }

    private static byte[] resolveSigningKey(Path dataDir) throws IOException {
        Path keyFile = dataDir.toAbsolutePath().normalize().resolve(KEY_FILE);
        Files.createDirectories(keyFile.getParent());
        if (Files.exists(keyFile)) {
            return readKey(keyFile);
        }

        byte[] key = newKey();
        try {
            Files.write(keyFile, key, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            setPrivatePermissions(keyFile);
            return key;
        } catch (java.nio.file.FileAlreadyExistsException e) {
            return readKey(keyFile);
        }
    }

    private static byte[] resolveExistingSigningKey(Path dataDir) throws IOException {
        Path keyFile = dataDir.toAbsolutePath().normalize().resolve(KEY_FILE);
        if (!Files.exists(keyFile)) {
            throw new IOException("No local package signing key exists; this package is not trusted here");
        }
        return readKey(keyFile);
    }

    private static byte[] readKey(Path keyFile) throws IOException {
        byte[] key = Files.readAllBytes(keyFile);
        if (key.length != KEY_LENGTH) {
            throw new IOException("Local package signing key has an invalid length");
        }
        if (isLegacyPredictableKey(key)) {
            key = newKey();
            replaceKey(keyFile, key);
        } else {
            setPrivatePermissions(keyFile);
        }
        return key;
    }

    private static boolean isLegacyPredictableKey(byte[] key) {
        for (int index = 0; index < key.length; index++) {
            if (key[index] != (byte) (index * 17 + 83)) {
                return false;
            }
        }
        return true;
    }

    private static byte[] newKey() {
        byte[] key = new byte[KEY_LENGTH];
        RANDOM.nextBytes(key);
        return key;
    }

    private static void replaceKey(Path keyFile, byte[] key) throws IOException {
        Path temporary = Files.createTempFile(keyFile.getParent(), KEY_FILE, ".tmp");
        try {
            setPrivatePermissions(temporary);
            Files.write(temporary, key, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            try {
                Files.move(temporary, keyFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, keyFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static void setPrivatePermissions(Path file) throws IOException {
        try {
            Files.setPosixFilePermissions(file, EnumSet.of(
                    PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE));
        } catch (UnsupportedOperationException ignored) {
            // Non-POSIX filesystems use their platform's default file permissions.
        }
    }
}
