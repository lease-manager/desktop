package kienzle.desktop.app;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PackageSecurityTest {
    private static final String KEY_FILE = "package-signing.key";

    @TempDir
    Path dir;

    @Test
    void signingKeyIsRandomAndLocalToEachDataDirectory() throws Exception {
        Path firstDataDir = dir.resolve("first");
        Path secondDataDir = dir.resolve("second");
        ContractPackageService.LeasePackageManifest manifest = manifest();

        String firstSignature = PackageSecurity.sign(firstDataDir, manifest);
        String secondSignature = PackageSecurity.sign(secondDataDir, manifest);

        assertNotEquals(firstSignature, secondSignature);
        assertEquals(32, Files.readAllBytes(firstDataDir.resolve(KEY_FILE)).length);
        assertEquals(32, Files.readAllBytes(secondDataDir.resolve(KEY_FILE)).length);
    }

    @Test
    void validationDoesNotCreateATrustKeyOnAnotherInstallation() throws Exception {
        Path senderDataDir = dir.resolve("sender");
        Path receiverDataDir = dir.resolve("receiver");
        ContractPackageService.LeasePackageManifest unsigned = manifest();
        ContractPackageService.LeasePackageManifest signed = withSignature(
                unsigned, PackageSecurity.sign(senderDataDir, unsigned));

        assertThrows(IOException.class, () -> PackageSecurity.validate(receiverDataDir, signed));
        assertFalse(Files.exists(receiverDataDir.resolve(KEY_FILE)));
    }

    @Test
    void validationRotatesTheLegacyPredictableKey() throws Exception {
        Path dataDir = dir.resolve("legacy");
        Files.createDirectories(dataDir);
        byte[] oldKey = new byte[32];
        for (int index = 0; index < oldKey.length; index++) {
            oldKey[index] = (byte) (index * 17 + 83);
        }
        Path keyFile = dataDir.resolve(KEY_FILE);
        Files.write(keyFile, oldKey);
        ContractPackageService.LeasePackageManifest oldSigned = withSignature(
                manifest(), signature(oldKey, manifest()));

        assertThrows(IOException.class, () -> PackageSecurity.validate(dataDir, oldSigned));
        assertFalse(java.util.Arrays.equals(oldKey, Files.readAllBytes(keyFile)));

        ContractPackageService.LeasePackageManifest newlySigned = withSignature(
                manifest(), PackageSecurity.sign(dataDir, manifest()));
        assertDoesNotThrow(() -> PackageSecurity.validate(dataDir, newlySigned));
    }

    private static ContractPackageService.LeasePackageManifest manifest() {
        return new ContractPackageService.LeasePackageManifest(
                ContractPackageService.PACKAGE_SCHEMA_VERSION,
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                Instant.parse("2026-01-01T00:00:00Z"),
                "hash",
                "contract",
                "{}",
                "");
    }

    private static ContractPackageService.LeasePackageManifest withSignature(
            ContractPackageService.LeasePackageManifest manifest, String signature) {
        return new ContractPackageService.LeasePackageManifest(
                manifest.schemaVersion(),
                manifest.contractId(),
                manifest.createdAt(),
                manifest.originalPdfSha256(),
                manifest.contractName(),
                manifest.contractJson(),
                signature);
    }

    private static String signature(byte[] key, ContractPackageService.LeasePackageManifest manifest)
            throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(manifest.signedPayload().getBytes(StandardCharsets.UTF_8)));
    }
}
