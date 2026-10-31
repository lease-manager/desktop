package kienzle.desktop.pdf;

/** A contract PDF carrying its QR code and metadata. */
public record StampedPdf(byte[] bytes, String sha256, QrPayload payload) {
}
