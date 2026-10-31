package kienzle.desktop.contract;

import java.time.Instant;
import java.util.UUID;

public record SignatureInfo(
        UUID contractId,
        SignatureSource source,
        Instant signedAt,
        String signerName,
        String deviceInfo
) {
}
