package kienzle.desktop.app;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DateTimeException;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/** Reads and writes {@code .leasesigned} archives (mobile to desktop); see {@code shared/libs/leasepack}. */
public final class SignedReturnCodec {
    /** Current format: signed by the Ed25519 key of the mobile device. */
    public static final String SCHEMA_VERSION = "leasesigned:v2";
    /** Legacy format without a signature; no longer accepted. */
    public static final String LEGACY_SCHEMA_VERSION = "leasesigned:v1";
    private static final Gson GSON = new Gson();
    private static final String MANIFEST = "manifest.json";
    private static final String PDF = "signed.pdf";
    private static final int MAX_MANIFEST_BYTES = 1024 * 1024;
    private static final int MAX_PDF_BYTES = 50 * 1024 * 1024;
    private static final long MAX_ARCHIVE_BYTES = MAX_PDF_BYTES + MAX_MANIFEST_BYTES + 64 * 1024;

    public record Manifest(
            String schemaVersion,
            UUID contractId,
            String originalPdfSha256,
            String signedPdfSha256,
            Instant signedAt,
            String signerName,
            String deviceInfo,
            String signerId,
            String signature
    ) {
        public Manifest {
            signerId = signerId == null ? "" : signerId;
            signature = signature == null ? "" : signature;
        }

        /** Manifest without signer data (legacy form). */
        public Manifest(String schemaVersion, UUID contractId, String originalPdfSha256, String signedPdfSha256,
                        Instant signedAt, String signerName, String deviceInfo) {
            this(schemaVersion, contractId, originalPdfSha256, signedPdfSha256, signedAt, signerName, deviceInfo,
                    "", "");
        }

        /** The text that the mobile device signs; missing optional values count as empty strings. */
        public String signedPayload() {
            return String.join("|", schemaVersion, contractId.toString(), originalPdfSha256, signedPdfSha256,
                    signedAt.toString(), signerName == null ? "" : signerName,
                    deviceInfo == null ? "" : deviceInfo, signerId);
        }
    }

    public record SignedReturn(Manifest manifest, byte[] pdfBytes) {
    }

    public void write(Path target, Manifest manifest, byte[] pdfBytes) throws IOException {
        JsonObject object = new JsonObject();
        object.addProperty("schemaVersion", manifest.schemaVersion());
        object.addProperty("contractId", manifest.contractId().toString());
        object.addProperty("originalPdfSha256", manifest.originalPdfSha256());
        object.addProperty("signedPdfSha256", manifest.signedPdfSha256());
        object.addProperty("signedAt", manifest.signedAt().toString());
        object.addProperty("signerName", manifest.signerName());
        object.addProperty("deviceInfo", manifest.deviceInfo());
        if (!manifest.signerId().isBlank()) {
            object.addProperty("signerId", manifest.signerId());
            object.addProperty("signature", manifest.signature());
        }
        try (OutputStream out = Files.newOutputStream(target);
             ZipOutputStream zip = new ZipOutputStream(out, StandardCharsets.UTF_8)) {
            zip.putNextEntry(new ZipEntry(MANIFEST));
            zip.write(GSON.toJson(object).getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry(PDF));
            zip.write(pdfBytes);
            zip.closeEntry();
        }
    }

    public SignedReturn read(Path file) throws IOException {
        if (Files.size(file) > MAX_ARCHIVE_BYTES) {
            throw new IOException("Signed return exceeds the maximum archive size");
        }
        Manifest manifest = null;
        byte[] pdf = null;
        Set<String> seen = new HashSet<>();
        try (InputStream in = Files.newInputStream(file);
             ZipInputStream zip = new ZipInputStream(in, StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                String name = entry.getName();
                if (entry.isDirectory() || (!MANIFEST.equals(name) && !PDF.equals(name))) {
                    throw new IOException("Unexpected entry in signed return: " + name);
                }
                if (!seen.add(name)) {
                    throw new IOException("Duplicate entry in signed return: " + name);
                }
                int limit = MANIFEST.equals(name) ? MAX_MANIFEST_BYTES : MAX_PDF_BYTES;
                byte[] data = zip.readNBytes(limit + 1);
                if (data.length > limit) {
                    throw new IOException("Signed return entry exceeds the maximum size: " + name);
                }
                if (MANIFEST.equals(name)) {
                    manifest = parse(new String(data, StandardCharsets.UTF_8));
                } else {
                    pdf = data;
                }
            }
        }
        if (manifest == null || pdf == null) {
            throw new IOException("Signed return must contain " + MANIFEST + " and " + PDF);
        }
        return new SignedReturn(manifest, pdf);
    }

    private static Manifest parse(String json) throws IOException {
        try {
            JsonElement parsed = GSON.fromJson(json, JsonElement.class);
            if (parsed == null || !parsed.isJsonObject()) {
                throw new IOException("Signed return manifest must be a JSON object");
            }
            JsonObject o = parsed.getAsJsonObject();
            return new Manifest(
                    string(o, "schemaVersion", true),
                    UUID.fromString(string(o, "contractId", true)),
                    string(o, "originalPdfSha256", true),
                    string(o, "signedPdfSha256", true),
                    Instant.parse(string(o, "signedAt", true)),
                    string(o, "signerName", false),
                    string(o, "deviceInfo", false),
                    string(o, "signerId", false),
                    string(o, "signature", false));
        } catch (JsonParseException | IllegalArgumentException | DateTimeException e) {
            throw new IOException("Signed return manifest is malformed", e);
        }
    }

    private static String string(JsonObject o, String name, boolean required) throws IOException {
        JsonElement e = o.get(name);
        if (e == null || e.isJsonNull()) {
            if (required) {
                throw new IOException("Signed return manifest is missing field: " + name);
            }
            return "";
        }
        if (!e.isJsonPrimitive() || !e.getAsJsonPrimitive().isString()) {
            throw new IOException("Signed return manifest field is not a string: " + name);
        }
        return e.getAsString();
    }
}
