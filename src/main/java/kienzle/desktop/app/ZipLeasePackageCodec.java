package kienzle.desktop.app;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ZipLeasePackageCodec implements LeasePackageCodec {
    private static final Gson GSON = new Gson();

    @Override
    public void writePackage(Path targetFile, ContractPackageService.LeasePackageManifest manifest, byte[] pdfBytes) throws IOException {
        try (OutputStream out = Files.newOutputStream(targetFile);
             ZipOutputStream zip = new ZipOutputStream(out, StandardCharsets.UTF_8)) {
            zip.putNextEntry(new ZipEntry("manifest.json"));
            zip.write(manifestJson(manifest).getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();

            zip.putNextEntry(new ZipEntry("contract.pdf"));
            zip.write(pdfBytes);
            zip.closeEntry();
        }
    }

    @Override
    public ContractPackageService.ContractPackage readPackage(Path packageFile) throws IOException {
        ContractPackageService.LeasePackageManifest manifest = null;
        byte[] pdfBytes = null;

        try (InputStream in = Files.newInputStream(packageFile);
             ZipInputStream zip = new ZipInputStream(in, StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                byte[] data = zip.readAllBytes();
                if ("manifest.json".equals(entry.getName())) {
                    manifest = manifestFromJson(new String(data, StandardCharsets.UTF_8));
                } else if ("contract.pdf".equals(entry.getName())) {
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

    private static ContractPackageService.LeasePackageManifest manifestFromJson(String json) {
        JsonObject object = GSON.fromJson(json, JsonObject.class);
        return new ContractPackageService.LeasePackageManifest(
                object.get("schemaVersion").getAsString(),
                UUID.fromString(object.get("contractId").getAsString()),
                Instant.parse(object.get("createdAt").getAsString()),
                object.get("originalPdfSha256").getAsString(),
                object.get("contractName").getAsString(),
                object.has("contractJson") ? object.get("contractJson").getAsString() : "",
                object.has("packageSignature") ? object.get("packageSignature").getAsString() : "");
    }

    private static String manifestJson(ContractPackageService.LeasePackageManifest manifest) {
        return "{\n"
                + "  \"schemaVersion\": \"" + escape(manifest.schemaVersion()) + "\",\n"
                + "  \"contractId\": \"" + manifest.contractId() + "\",\n"
                + "  \"createdAt\": \"" + manifest.createdAt() + "\",\n"
                + "  \"originalPdfSha256\": \"" + escape(manifest.originalPdfSha256()) + "\",\n"
                + "  \"contractName\": \"" + escape(manifest.contractName()) + "\",\n"
                + "  \"contractJson\": \"" + escape(manifest.contractJson()) + "\",\n"
                + "  \"packageSignature\": \"" + escape(manifest.packageSignature()) + "\"\n"
                + "}";
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
