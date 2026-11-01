package kienzle.desktop.app;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PairingServiceTest {
    @TempDir
    Path dir;

    private PairingService desktop() {
        return new PairingService(dir.resolve("desktop"), "Desktop");
    }

    private PairingService phone() {
        return new PairingService(dir.resolve("phone"), "Phone");
    }

    private static final byte[] DATA = "payload".getBytes(StandardCharsets.UTF_8);

    @Test
    void identityIsCreatedOnceAndStable() throws Exception {
        PairingService service = desktop();
        String first = service.ownPairingCode();
        assertEquals(first, new PairingService(dir.resolve("desktop"), "Desktop").ownPairingCode());
        assertFalse(first.contains("privateKey"));
    }

    @Test
    void pairedPeerSignaturesVerifyAndOthersDoNot() throws Exception {
        PairingService desktop = desktop();
        PairingService phone = phone();
        PairingService.Peer phonePeer = desktop.acceptPairingCode(phone.ownPairingCode());
        phone.acceptPairingCode(desktop.ownPairingCode());

        String signature = phone.sign(DATA);
        assertTrue(desktop.verify(phonePeer.id(), DATA, signature));
        assertFalse(desktop.verify(phonePeer.id(), "other".getBytes(StandardCharsets.UTF_8), signature));
        assertFalse(desktop.verify(phonePeer.id(), DATA, desktop.sign(DATA)));
        assertFalse(desktop.verify("unknown", DATA, signature));
        assertFalse(desktop.verify(phonePeer.id(), DATA, "%%%"));
        assertFalse(desktop.verify(phonePeer.id(), DATA, null));
    }

    @Test
    void peersPersistAndCanBeRemovedAndRepaired() throws Exception {
        PairingService desktop = desktop();
        PairingService phone = phone();
        desktop.acceptPairingCode(phone.ownPairingCode());
        desktop.acceptPairingCode(phone.ownPairingCode());

        PairingService reopened = desktop();
        assertEquals(1, reopened.peers().size());
        assertEquals("Phone", reopened.peers().get(0).name());
        assertEquals(phone.ownFingerprint(), reopened.peers().get(0).fingerprint());

        reopened.remove(reopened.peers().get(0).id());
        assertTrue(desktop().peers().isEmpty());
    }

    @Test
    void rejectsInvalidCodes() throws Exception {
        PairingService desktop = desktop();
        String own = desktop.ownPairingCode();
        String[] bad = {null, "", "   ", "not json", "[]", "{}", "x".repeat(5000),
                own.replace("leasepair:v1", "leasepair:v9"),
                own.replaceAll("\"publicKey\":\"[^\"]*\"", "\"publicKey\":\"AAAA\""),
                own.replaceAll("\"id\":\"[^\"]*\"", "\"id\":\"nope\"")};
        for (String code : bad) {
            assertThrows(IOException.class, () -> desktop.acceptPairingCode(code), String.valueOf(code));
        }
        assertThrows(IOException.class, () -> desktop.acceptPairingCode(own));
        assertTrue(desktop.peers().isEmpty());
    }

    @Test
    void fingerprintIsGroupedHex() throws Exception {
        assertTrue(desktop().ownFingerprint().matches("([0-9A-F]{4} ){3}[0-9A-F]{4}"));
    }

    @Test
    void corruptFilesAreReportedNotIgnored() throws Exception {
        PairingService desktop = desktop();
        desktop.ownPairingCode();
        Files.writeString(dir.resolve("desktop/paired-devices.json"), "{broken");
        assertThrows(IOException.class, desktop::peers);
        Files.writeString(dir.resolve("desktop/device-identity.json"), "{broken");
        assertThrows(IOException.class, desktop::ownPairingCode);
    }
}
