package kienzle.desktop;

import kienzle.desktop.app.AppContext;
import kienzle.desktop.contract.ContractEntry;
import kienzle.desktop.contract.ContractStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ContractUiActionsTest {
    @TempDir
    Path dir;

    @Test
    void createPdf_canBeRepeatedOnceThePdfExists() throws Exception {
        ContractUiActions actions = new ContractUiActions(AppContext.create(dir).workflow());
        ContractEntry first = actions.createPdf(null, TestContracts.sampleContract());
        assertEquals(ContractStatus.PDF_CREATED, first.status());

        ContractEntry again = actions.createPdf(first, TestContracts.sampleContract("Changed in form"));
        assertEquals(ContractStatus.PDF_CREATED, again.status());
        assertEquals(first.contract().name(), again.contract().name());
    }
}
