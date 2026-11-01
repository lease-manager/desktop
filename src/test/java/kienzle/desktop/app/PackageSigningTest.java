package kienzle.desktop.app;

import kienzle.desktop.TestContracts;
import kienzle.desktop.contract.ContractEntry;
import kienzle.desktop.contract.ContractService;
import kienzle.desktop.contract.JsonContractRepository;
import kienzle.desktop.pdf.Hashing;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Packages are signed with the Ed25519 key of the sending installation and checked against paired devices. */
class PackageSigningTest {
    @TempDir
    Path dir;

    private PairingService senderPairing;
    private ContractPackageService sender;
    private Path archive;

    @BeforeEach
    void exportPackage() throws IOException {
        Path senderDir = dir.resolve("sender");
        senderPairing = new PairingService(senderDir, "Sender");
        sender = new ContractPackageService(senderDir, new ZipLeasePackageCodec(), senderPairing);
        ContractService contracts = new ContractService(new JsonContractRepository(senderDir));
        ContractEntry entry = contracts.create(TestContracts.sampleContract());
        Path pdf = senderDir.resolve("documents").resolve(entry.id().toString()).resolve("original.pdf");
        Files.createDirectories(pdf.getParent());
        byte[] bytes = "pdf".getBytes(StandardCharsets.UTF_8);
        Files.write(pdf, bytes);
        entry = contracts.registerOriginalPdf(entry.id(), senderDir.relativize(pdf).toString(), Hashing.sha256(bytes));
        archive = dir.resolve("p.leasepack");
        sender.exportPackage(entry, archive);
    }

    private ContractPackageService receiver(String name, boolean paired) throws IOException {
        Path receiverDir = dir.resolve(name);
        PairingService pairing = new PairingService(receiverDir, name);
        if (paired) {
            pairing.acceptPairingCode(senderPairing.ownPairingCode());
        }
        return new ContractPackageService(receiverDir, new ZipLeasePackageCodec(), pairing);
    }

    @Test
    void pairedInstallationAcceptsThePackage() throws Exception {
        ContractPackageService receiver = receiver("receiver", true);

        assertDoesNotThrow(() -> receiver.readPackage(archive));
        assertEquals(TestContracts.sampleContract().name(), receiver.importPackage(archive).contract().name());
    }

    @Test
    void unpairedInstallationRejectsThePackage() throws Exception {
        ContractPackageService stranger = receiver("stranger", false);

        assertThrows(IOException.class, () -> stranger.readPackage(archive));
        assertThrows(IOException.class, () -> stranger.importPackage(archive));
    }

    @Test
    void changedManifestIsRejectedEvenWhenPaired() throws Exception {
        ContractPackageService receiver = receiver("receiver", true);
        var original = sender.readPackage(archive);
        var m = original.manifest();
        var forged = new ContractPackageService.LeasePackageManifest(m.schemaVersion(), m.contractId(),
                m.createdAt(), m.originalPdfSha256(), "Forged name", m.contractJson(), m.packageSignature(),
                m.senderId());
        Path forgedArchive = dir.resolve("forged.leasepack");
        new ZipLeasePackageCodec().writePackage(forgedArchive, forged, original.pdfBytes());

        assertThrows(IOException.class, () -> receiver.readPackage(forgedArchive));
    }

    @Test
    void removedSenderIsRejected() throws Exception {
        ContractPackageService receiver = receiver("receiver", true);
        var m = sender.readPackage(archive).manifest();
        var noSender = new ContractPackageService.LeasePackageManifest(m.schemaVersion(), m.contractId(),
                m.createdAt(), m.originalPdfSha256(), m.contractName(), m.contractJson(), m.packageSignature(), "");
        Path file = dir.resolve("nosender.leasepack");
        new ZipLeasePackageCodec().writePackage(file, noSender, sender.readPackage(archive).pdfBytes());

        assertThrows(IOException.class, () -> receiver.readPackage(file));
    }
}
