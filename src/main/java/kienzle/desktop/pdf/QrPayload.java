package kienzle.desktop.pdf;

import java.util.Optional;
import java.util.UUID;

/**
 * Text encoded in the QR code of a contract PDF: {@code lease:v1:<contractId>:<contentHash>}.
 * The content hash is the SHA-256 of the PDF before the QR code was stamped onto it.
 */
public record QrPayload(UUID contractId, String contentHash) {
    private static final String PREFIX = "lease:v1:";

    public String encode() {
        return PREFIX + contractId + ":" + contentHash;
    }

    public static Optional<QrPayload> parse(String text) {
        if (text == null || !text.startsWith(PREFIX)) {
            return Optional.empty();
        }
        String[] parts = text.substring(PREFIX.length()).split(":");
        if (parts.length != 2 || !parts[1].matches("[0-9a-f]{64}")) {
            return Optional.empty();
        }
        try {
            return Optional.of(new QrPayload(UUID.fromString(parts[0]), parts[1]));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
