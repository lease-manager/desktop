package kienzle.desktop;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ContractDefaultsTest {
    @Test
    void newContract_hasDefaultTexts() {
        LeaseContract contract = ContractDefaults.newContract();
        assertEquals(ContractDefaults.DURATION_INDEFINITE, contract.leaseDuration());
        assertEquals("6 Monate", contract.terminationNotice());
        assertTrue(contract.paragraphFive().contains("6 Monate vor Ende des Gartenjahres (31.10.)"));
        assertFalse(contract.paragraphFour().isBlank());
    }
}
