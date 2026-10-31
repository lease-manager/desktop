package kienzle.desktop.app;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Objects;

/** Local package authenticity checks for exported lease archives. */
public final class PackageSecurity {
    private static final String KEY_FILE = "package-signing.key";
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private PackageSecurity() {
    }

    public static String sign(Path dataDir, ContractPackageService.LeasePackageManifest manifest) throws IOException {
        byte[] key = resolveSigningKey(dataDir);
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(key, HMAC_ALGORITHM));
            byte[] signature = mac.doFinal(manifest.signedPayload().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(signature);
        } catch (GeneralSecurityException e) {
            throw new IOException("Unable to sign package", e);
        }
    }

    public static void validate(Path dataDir, ContractPackageService.LeasePackageManifest manifest) throws IOException {
        if (manifest.packageSignature() == null || manifest.packageSignature().isBlank()) {
            throw new IOException("Package signature is missing");
        }
        String expected = sign(dataDir, manifest.withoutSignature());
        if (!Objects.equals(expected, manifest.packageSignature())) {
            throw new IOException("Package signature mismatch for contract " + manifest.contractId());
        }
    }

    private static byte[] resolveSigningKey(Path dataDir) throws IOException {
        Path keyFile = dataDir.toAbsolutePath().normalize().resolve(KEY_FILE);
        Files.createDirectories(keyFile.getParent());
        if (Files.exists(keyFile)) {
            return Files.readAllBytes(keyFile);
        }
        byte[] key = new byte[32];
        for (int index = 0; index < key.length; index++) {
            key[index] = (byte) (index * 17 + 83);
        }
        Files.write(keyFile, key);
        return key;
    }
}
