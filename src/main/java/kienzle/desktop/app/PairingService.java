package kienzle.desktop.app;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.EnumSet;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

/**
 * Device identity (Ed25519) and the list of paired devices. The pairing code of a device is a small JSON
 * document ({@code leasepair:v1}) that is shown as QR code and read by the other side. It carries only public data.
 */
public final class PairingService {
    public static final String SCHEMA_VERSION = "leasepair:v1";
    private static final String IDENTITY_FILE = "device-identity.json";
    private static final String PEERS_FILE = "paired-devices.json";
    private static final int MAX_CODE_LENGTH = 2048;
    private static final Gson GSON = new Gson();

    /** A device this installation trusts. */
    public record Peer(String id, String name, String publicKey, Instant pairedAt) {
        public String fingerprint() {
            return PairingService.fingerprint(publicKey);
        }
    }

    private record Identity(String id, String publicKey, String privateKey) {
    }

    private final Path dataDir;
    private final String deviceName;

    public PairingService(Path dataDir, String deviceName) {
        this.dataDir = dataDir.toAbsolutePath().normalize();
        this.deviceName = deviceName == null || deviceName.isBlank() ? "Desktop" : deviceName.trim();
    }

    /** The code that the other device scans or pastes to trust this installation. */
    public synchronized String ownPairingCode() throws IOException {
        Identity identity = identity();
        JsonObject object = new JsonObject();
        object.addProperty("v", SCHEMA_VERSION);
        object.addProperty("id", identity.id());
        object.addProperty("name", deviceName);
        object.addProperty("publicKey", identity.publicKey());
        return GSON.toJson(object);
    }

    public synchronized String ownFingerprint() throws IOException {
        return fingerprint(identity().publicKey());
    }

    /** Trusts the device described by {@code code}; pairing the same device again replaces its entry. */
    public synchronized Peer acceptPairingCode(String code) throws IOException {
        Peer peer = parse(code);
        if (peer.id().equals(identity().id())) {
            throw new IOException("This is the pairing code of this installation");
        }
        List<Peer> peers = new ArrayList<>(peers());
        peers.removeIf(existing -> existing.id().equals(peer.id()));
        peers.add(peer);
        writePeers(peers);
        return peer;
    }

    public synchronized List<Peer> peers() throws IOException {
        Path file = dataDir.resolve(PEERS_FILE);
        if (!Files.exists(file)) {
            return List.of();
        }
        try {
            List<PeerData> data = GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8),
                    new TypeToken<List<PeerData>>() { }.getType());
            if (data == null) {
                return List.of();
            }
            return data.stream()
                    .map(entry -> new Peer(entry.id, entry.name, entry.publicKey, Instant.parse(entry.pairedAt)))
                    .toList();
        } catch (RuntimeException e) {
            throw new IOException("Paired devices file is unreadable", e);
        }
    }

    public synchronized void remove(String peerId) throws IOException {
        List<Peer> peers = new ArrayList<>(peers());
        if (peers.removeIf(peer -> peer.id().equals(peerId))) {
            writePeers(peers);
        }
    }

    /** Signs {@code data} with this installation's private key; the result is Base64. */
    public synchronized String sign(byte[] data) throws IOException {
        try {
            Signature signature = Signature.getInstance("Ed25519");
            signature.initSign(privateKey(identity()));
            signature.update(data);
            return Base64.getEncoder().encodeToString(signature.sign());
        } catch (GeneralSecurityException e) {
            throw new IOException("Unable to sign", e);
        }
    }

    /** Checks a Base64 signature against the key of a paired device. Unknown devices never verify. */
    public synchronized boolean verify(String peerId, byte[] data, String signatureBase64) throws IOException {
        Peer peer = peers().stream().filter(candidate -> candidate.id().equals(peerId)).findFirst().orElse(null);
        if (peer == null || signatureBase64 == null) {
            return false;
        }
        try {
            Signature signature = Signature.getInstance("Ed25519");
            signature.initVerify(publicKey(peer.publicKey()));
            signature.update(data);
            return signature.verify(Base64.getDecoder().decode(signatureBase64));
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            return false;
        }
    }

    /** Short, human comparable form of a public key: first 8 bytes of its SHA-256 as hex groups. */
    public static String fingerprint(String publicKeyBase64) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(Base64.getDecoder().decode(publicKeyBase64));
            String hex = HexFormat.of().withUpperCase().formatHex(digest, 0, 8);
            return hex.replaceAll("(.{4})(?!$)", "$1 ");
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid public key", e);
        }
    }

    private Peer parse(String code) throws IOException {
        if (code == null || code.isBlank() || code.length() > MAX_CODE_LENGTH) {
            throw new IOException("Pairing code is empty or too long");
        }
        try {
            JsonElement parsed = GSON.fromJson(code.trim(), JsonElement.class);
            if (parsed == null || !parsed.isJsonObject()) {
                throw new IOException("Pairing code is malformed");
            }
            JsonObject object = parsed.getAsJsonObject();
            if (!SCHEMA_VERSION.equals(string(object, "v"))) {
                throw new IOException("Unsupported pairing code version");
            }
            String id = UUID.fromString(string(object, "id")).toString();
            String name = string(object, "name").trim();
            String key = string(object, "publicKey");
            publicKey(key);
            if (name.isEmpty() || name.length() > 100) {
                throw new IOException("Pairing code has an invalid device name");
            }
            return new Peer(id, name, key, Instant.now());
        } catch (JsonParseException | IllegalArgumentException | GeneralSecurityException e) {
            throw new IOException("Pairing code is malformed", e);
        }
    }

    private static String string(JsonObject object, String name) throws IOException {
        JsonElement element = object.get(name);
        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
            throw new IOException("Pairing code is missing field: " + name);
        }
        return element.getAsString();
    }

    private Identity identity() throws IOException {
        Path file = dataDir.resolve(IDENTITY_FILE);
        if (Files.exists(file)) {
            try {
                Identity identity = GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), Identity.class);
                if (identity == null || identity.id() == null || identity.publicKey() == null
                        || identity.privateKey() == null) {
                    throw new IOException("Device identity is incomplete");
                }
                return identity;
            } catch (JsonParseException e) {
                throw new IOException("Device identity is unreadable", e);
            }
        }
        try {
            KeyPair pair = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
            Identity identity = new Identity(UUID.randomUUID().toString(),
                    Base64.getEncoder().encodeToString(pair.getPublic().getEncoded()),
                    Base64.getEncoder().encodeToString(pair.getPrivate().getEncoded()));
            writePrivate(file, GSON.toJson(identity));
            return identity;
        } catch (GeneralSecurityException e) {
            throw new IOException("Unable to create the device identity", e);
        }
    }

    private static PrivateKey privateKey(Identity identity) throws GeneralSecurityException {
        return KeyFactory.getInstance("Ed25519").generatePrivate(
                new PKCS8EncodedKeySpec(Base64.getDecoder().decode(identity.privateKey())));
    }

    private static PublicKey publicKey(String base64) throws GeneralSecurityException {
        return KeyFactory.getInstance("Ed25519").generatePublic(
                new X509EncodedKeySpec(Base64.getDecoder().decode(base64)));
    }

    private void writePeers(List<Peer> peers) throws IOException {
        List<PeerData> data = peers.stream()
                .map(peer -> new PeerData(peer.id(), peer.name(), peer.publicKey(), peer.pairedAt().toString()))
                .toList();
        writePrivate(dataDir.resolve(PEERS_FILE), GSON.toJson(data));
    }

    private static void writePrivate(Path file, String content) throws IOException {
        Files.createDirectories(file.getParent());
        Path temporary = Files.createTempFile(file.getParent(), file.getFileName().toString(), ".tmp");
        try {
            try {
                Files.setPosixFilePermissions(temporary,
                        EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));
            } catch (UnsupportedOperationException ignored) {
                // Non-POSIX filesystems use their platform's default file permissions.
            }
            Files.writeString(temporary, content, StandardCharsets.UTF_8);
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static final class PeerData {
        final String id;
        final String name;
        final String publicKey;
        final String pairedAt;

        PeerData(String id, String name, String publicKey, String pairedAt) {
            this.id = id;
            this.name = name;
            this.publicKey = publicKey;
            this.pairedAt = pairedAt;
        }
    }
}
