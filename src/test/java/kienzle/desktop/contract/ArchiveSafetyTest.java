package kienzle.desktop.contract;

import kienzle.desktop.TestContracts;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ArchiveSafetyTest {
    @TempDir
    Path dir;

    @Test
    void failedMove_undoesEarlierMoves() throws IOException {
        Path a = Files.writeString(dir.resolve("a.txt"), "a");
        Path missing = dir.resolve("missing.txt");
        var moves = new FileMoveTransaction();
        moves.add(a, dir.resolve("out/a.txt"));
        moves.add(missing, dir.resolve("out/missing.txt"));

        assertThrows(IOException.class, moves::commit);

        assertTrue(Files.exists(a));
        assertFalse(Files.exists(dir.resolve("out/a.txt")));
    }

    @Test
    void archive_rejectsDocumentOutsideContractFolder_andKeepsContractActive() throws IOException {
        var service = new ContractService(new JsonContractRepository(dir));
        var entry = service.create(TestContracts.sampleContract("N"));
        Path foreign = Files.writeString(dir.resolve("foreign.pdf"), "x");
        service.registerOriginalPdf(entry.id(), "foreign.pdf", "h");

        assertThrows(IllegalArgumentException.class, () -> service.archive(entry.id()));

        assertEquals(1, service.list().size());
        assertTrue(service.listArchived().isEmpty());
        assertTrue(Files.exists(foreign));
    }
}
