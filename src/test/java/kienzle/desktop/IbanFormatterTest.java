package kienzle.desktop;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IbanFormatterTest {
    @Test
    void formatGroupsInFours() {
        assertEquals("DE12 3456 7890", IbanFormatter.format("DE1234567890"));
    }

    @Test
    void normalizeStripsAndUppercasesAndLimits() {
        assertEquals("DE1234", IbanFormatter.normalize("de12 34"));
        assertEquals(34, IbanFormatter.normalize("A".repeat(50)).length());
    }

    @Test
    void isValid_checksIbanChecksum() {
        assertTrue(IbanFormatter.isValid("DE89 3704 0044 0532 0130 00"));
        assertTrue(IbanFormatter.isValid("GB82 WEST 1234 5698 7654 32"));
        assertFalse(IbanFormatter.isValid("DE89 3704 0044 0532 0130 01"));
        assertFalse(IbanFormatter.isValid("XX00 TEST"));
        assertFalse(IbanFormatter.isValid(""));
    }
}
