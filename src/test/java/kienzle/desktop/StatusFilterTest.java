package kienzle.desktop;

import kienzle.desktop.contract.ContractEntry;
import kienzle.desktop.contract.ContractStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class StatusFilterTest {
    private final ContractEntry draft = ContractEntry.draft(LeaseContract.builder().build(), Instant.now());
    private final ContractEntry signed = draft.withStatus(ContractStatus.SIGNED, Instant.now());

    @Test
    void filtersByStatus() {
        assertTrue(StatusFilter.ALL.test(draft));
        assertTrue(StatusFilter.OPEN.test(draft));
        assertFalse(StatusFilter.OPEN.test(signed));
        assertTrue(StatusFilter.SIGNED.test(signed));
        assertFalse(StatusFilter.SIGNED.test(draft));
    }
}
