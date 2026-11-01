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

public class ZipLeasePackageCodec implements LeasePackageCodec {
    private static final Gson GSON = new Gson();
    private static final int MAX_MANIFEST_BYTES = 1024 * 1024;
    private static final int MAX_PDF_BYTES = 50 * 1024 * 1024;
    private static final long MAX_ARCHIVE_BYTES = MAX_PDF_BYTES + MAX_MANIFEST_BYTES + 64 * 1024;

    @Override
    public void writePackage(Path targetFile, ContractPackageService.LeasePackageManifest manifest, byte[] pdfBytes) throws IOException {
        if (pdfBytes.length > MAX_PDF_BYTES) {
            throw new IOException("Contract PDF exceeds the maximum package size");
        }
        byte[] manifestBytes = manifestJson(manifest).getBytes(StandardCharsets.UTF_8);
        if (manifestBytes.length > MAX_MANIFEST_BYTES) {
            throw new IOException("Package manifest exceeds the maximum size");
        }

        try (OutputStream out = Files.newOutputStream(targetFile);
             ZipOutputStream zip = new ZipOutputStream(out, StandardCharsets.UTF_8)) {
            zip.putNextEntry(new ZipEntry("manifest.json"));
            zip.write(manifestBytes);
            zip.closeEntry();

            zip.putNextEntry(new ZipEntry("contract.pdf"));
            zip.write(pdfBytes);
            zip.closeEntry();
        }
    }

    @Override
    public ContractPackageService.ContractPackage readPackage(Path packageFile) throws IOException {
        if (Files.size(packageFile) > MAX_ARCHIVE_BYTES) {
            throw new IOException("Lease package exceeds the maximum archive size");
        }

        ContractPackageService.LeasePackageManifest manifest = null;
        byte[] pdfBytes = null;
        Set<String> entries = new HashSet<>();

        try (InputStream in = Files.newInputStream(packageFile);
             ZipInputStream zip = new ZipInputStream(in, StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                String name = entry.getName();
                if (entry.isDirectory() || (!"manifest.json".equals(name) && !"contract.pdf".equals(name))) {
                    throw new IOException("Unexpected entry in lease package: " + name);
                }
                if (!entries.add(name)) {
                    throw new IOException("Duplicate entry in lease package: " + name);
                }

                int limit = "manifest.json".equals(name) ? MAX_MANIFEST_BYTES : MAX_PDF_BYTES;
                byte[] data = readBounded(zip, limit, name);
                if ("manifest.json".equals(name)) {
                    manifest = manifestFromJson(new String(data, StandardCharsets.UTF_8));
                } else {
                    pdfBytes = data;
                }
                zip.closeEntry();
            }
        }

        if (manifest == null) {
            throw new IOException("Archive does not contain a manifest.json");
        }
        if (pdfBytes == null) {
            throw new IOException("Archive does not contain contract.pdf");
        }
        return new ContractPackageService.ContractPackage(manifest, pdfBytes);
    }

    private static byte[] readBounded(InputStream input, int limit, String entryName) throws IOException {
        byte[] data = input.readNBytes(limit + 1);
        if (data.length > limit) {
            throw new IOException("Lease package entry exceeds the maximum size: " + entryName);
        }
        return data;
    }

    private static ContractPackageService.LeasePackageManifest manifestFromJson(String json) throws IOException {
        try {
            JsonElement parsed = GSON.fromJson(json, JsonElement.class);
            if (parsed == null || !parsed.isJsonObject()) {
                throw new IOException("Package manifest must be a JSON object");
            }
            JsonObject object = parsed.getAsJsonObject();
            return new ContractPackageService.LeasePackageManifest(
                    requiredString(object, "schemaVersion"),
                    UUID.fromString(requiredString(object, "contractId")),
                    Instant.parse(requiredString(object, "createdAt")),
                    requiredString(object, "originalPdfSha256"),
                    requiredString(object, "contractName"),
                    requiredString(object, "contractJson"),
                    requiredString(object, "packageSignature"),
                    optionalString(object, "senderId"));
        } catch (JsonParseException | IllegalArgumentException | DateTimeException e) {
            throw new IOException("Package manifest is malformed", e);
        }
    }

    private static String optionalString(JsonObject object, String name) throws IOException {
        JsonElement element = object.get(name);
        if (element == null || element.isJsonNull()) {
            return "";
        }
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
            throw new IOException("Package manifest field is not a string: " + name);
        }
        return element.getAsString();
    }

    private static String requiredString(JsonObject object, String name) throws IOException {
        JsonElement element = object.get(name);
        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
            throw new IOException("Package manifest is missing string field: " + name);
        }
        return element.getAsString();
    }

    private static String manifestJson(ContractPackageService.LeasePackageManifest manifest) {
        JsonObject object = new JsonObject();
        object.addProperty("schemaVersion", manifest.schemaVersion());
        object.addProperty("contractId", manifest.contractId().toString());
        object.addProperty("createdAt", manifest.createdAt().toString());
        object.addProperty("originalPdfSha256", manifest.originalPdfSha256());
        object.addProperty("contractName", manifest.contractName());
        object.addProperty("contractJson", manifest.contractJson());
        object.addProperty("packageSignature", manifest.packageSignature());
        if (!manifest.senderId().isBlank()) {
            object.addProperty("senderId", manifest.senderId());
        }
        return GSON.toJson(object);
    }
}
