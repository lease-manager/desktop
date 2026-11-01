package kienzle.desktop;

import kienzle.desktop.contract.ContractEntry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContractListTextTest {
    @Test
    void listText_shouldShowParcelCountOnlyForSeveralParcels() {
        LeaseContract one = TestContracts.sampleContract();
        LeaseContract two = LeaseContract.builder().name("Zwei")
                .parcels(java.util.List.of(new Parcel("1", "", null, "", "", null),
                        new Parcel("2", "", null, "", "", null))).build();
        assertFalse(MainLayoutController.listText(ContractEntry.draft(one, java.time.Instant.now())).contains("·"));
        assertTrue(MainLayoutController.listText(ContractEntry.draft(two, java.time.Instant.now())).contains("2"));
    }
}
