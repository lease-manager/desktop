package kienzle.desktop;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class LeaseContractBuilderTest {
    @Test
    void builder_defaultsTextToEmptyAndOthersToNull() {
        LeaseContract contract = LeaseContract.builder().name("A").build();
        assertEquals("A", contract.name());
        assertEquals("", contract.lessor());
        assertNull(contract.rent());
        assertNull(contract.startDate());
        assertFalse(contract.hasIntroductoryRent());
    }

    @Test
    void toBuilder_copiesAndAllowsModification() {
        LeaseContract original = LeaseContract.builder().name("A").rent(new BigDecimal("10"))
                .startDate(LocalDate.of(2025, 1, 1)).build();
        LeaseContract copy = original.toBuilder().name("B").build();
        assertEquals("B", copy.name());
        assertEquals(original.rent(), copy.rent());
        assertEquals(original.startDate(), copy.startDate());
        assertEquals("A", original.name());
        assertEquals(original, original.toBuilder().build());
    }
}
