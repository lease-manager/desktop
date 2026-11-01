package kienzle.desktop;

import java.util.List;
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
        assertNull(contract.totalRent());
        assertEquals(0, contract.parcels().size());
        assertNull(contract.startDate());
        assertFalse(contract.hasIntroductoryRent());
    }

    @Test
    void toBuilder_copiesAndAllowsModification() {
        LeaseContract original = LeaseContract.builder().name("A")
                .parcels(List.of(new Parcel("1", "", null, "", "", new BigDecimal("10"))))
                .startDate(LocalDate.of(2025, 1, 1)).build();
        LeaseContract copy = original.toBuilder().name("B").build();
        assertEquals("B", copy.name());
        assertEquals(original.parcels(), copy.parcels());
        assertEquals(original.startDate(), copy.startDate());
        assertEquals("A", original.name());
        assertEquals(original, original.toBuilder().build());
    }
}
