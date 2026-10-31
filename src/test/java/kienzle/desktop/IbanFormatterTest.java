package kienzle.desktop;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
