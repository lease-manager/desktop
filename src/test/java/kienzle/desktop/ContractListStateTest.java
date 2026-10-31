package kienzle.desktop;

import javafx.collections.FXCollections;
import javafx.scene.control.ListView;
import kienzle.desktop.contract.ContractEntry;
import kienzle.desktop.contract.ContractStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.MonthDay;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContractListStateTest {

    @BeforeAll
    static void initJavaFx() throws Exception {
        JavaFxTestSupport.startToolkit();
    }

    @Test
    void replaceEntry_updatesExistingEntryAndCurrentSelection() {
        ContractListState state = new ContractListState();

        ContractEntry original = contract("alpha", ContractStatus.DRAFT);
        ContractEntry replacement = contract("beta", ContractStatus.PDF_CREATED);
        state.setAll(FXCollections.observableArrayList(original));

        state.replaceEntry(replacement);
        assertEquals(2, state.visibleEntries().size());
        assertNull(state.currentEntry());

        state.setCurrentEntry(original);
        state.replaceEntry(original);
        assertEquals(original.id(), state.currentEntry().id());

        ListView<ContractEntry> list = new ListView<>();
        list.setItems(state.visibleEntries());
        state.ensureVisible(list, replacement);
        assertEquals(replacement.id(), state.currentEntry().id());
    }

    @Test
    void applyFilter_filtersByStatus() {
        ContractListState state = new ContractListState();
        state.setAll(FXCollections.observableArrayList(
                contract("draft", ContractStatus.DRAFT),
                contract("pdf", ContractStatus.PDF_CREATED),
                contract("signed", ContractStatus.SIGNED)
        ));

        state.applyFilter(StatusFilter.OPEN);
        assertEquals(2, state.visibleEntries().size());
        assertEquals("draft", state.visibleEntries().get(0).contract().name());

        state.applyFilter(StatusFilter.ALL);
        assertEquals(3, state.visibleEntries().size());
    }

    @Test
    void addEntry_replacesExistingEntryWithSameId() {
        ContractListState state = new ContractListState();
        ContractEntry first = contract("first", ContractStatus.DRAFT);
        ContractEntry updated = contract("first-updated", ContractStatus.PDF_CREATED);
        updated = new ContractEntry(
                first.id(),
                updated.contract(),
                updated.status(),
                updated.createdAt(),
                updated.updatedAt(),
                updated.documents(),
                updated.signature()
        );
        state.setAll(FXCollections.observableArrayList(first));

        state.addEntry(updated);

        assertEquals(1, state.visibleEntries().size());
        assertEquals("first-updated", state.visibleEntries().get(0).contract().name());
        assertEquals(first.id(), state.visibleEntries().get(0).id());
    }

    @Test
    void removeCurrentEntry_removesSelectedEntryFromList() {
        ContractListState state = new ContractListState();
        ContractEntry first = contract("first", ContractStatus.DRAFT);
        ContractEntry second = contract("second", ContractStatus.PDF_CREATED);
        state.setAll(FXCollections.observableArrayList(first, second));
        state.setCurrentEntry(first);

        state.removeCurrentEntry();

        assertEquals(1, state.visibleEntries().size());
        assertEquals(second.id(), state.visibleEntries().get(0).id());
        assertNull(state.currentEntry());
    }

    @Test
    void isFilteredOut_reflectsCurrentFilter() {
        ContractListState state = new ContractListState();
        ContractEntry signed = contract("signed", ContractStatus.SIGNED);

        state.applyFilter(StatusFilter.OPEN);
        assertTrue(state.isFilteredOut(signed));

        state.applyFilter(StatusFilter.ALL);
        assertFalse(state.isFilteredOut(signed));
    }

    @Test
    void ensureVisible_clearsCurrentEntryWhenEntryHiddenByFilter() {
        ContractListState state = new ContractListState();
        ContractEntry signed = contract("signed", ContractStatus.SIGNED);
        state.setAll(FXCollections.observableArrayList(signed));
        state.applyFilter(StatusFilter.OPEN);
        state.setCurrentEntry(signed);

        ListView<ContractEntry> list = new ListView<>();
        list.setItems(state.visibleEntries());
        list.getSelectionModel().select(signed);

        state.ensureVisible(list, signed);

        assertNull(state.currentEntry());
    }

    private static ContractEntry contract(String name, ContractStatus status) {
        LeaseContract contract = LeaseContract.builder()
                .name(name)
                .lessor("Lessor")
                .lessorLocation("Berlin")
                .lessorDate(LocalDate.of(2025, 1, 1))
                .tenant("Lessee")
                .tenantLocation("Berlin")
                .tenantDate(LocalDate.of(2025, 1, 2))
                .parcelNumber("123/45")
                .district("Parcel")
                .size(new BigDecimal("3000"))
                .type("Field")
                .description("Description")
                .rent(new BigDecimal("1500"))
                .leaseDuration("permanent")
                .startDate(LocalDate.of(2025, 1, 3))
                .introductoryRent(new BigDecimal("900"))
                .hasIntroductoryRent(true)
                .rentIncreaseDate(LocalDate.of(2027, 1, 1))
                .firstRentDate(LocalDate.of(2025, 2, 1))
                .rentRecipient("Bank")
                .bankName("IBAN")
                .iban("DE89 3704 0044 0532 0130 00")
                .bic("GENODEF1RFS")
                .court("Court")
                .terminationNotice("6 months")
                .gardeningYearEnd(MonthDay.of(10, 31))
                .paragraphFour("Clause 1")
                .paragraphFive("Clause 2")
                .lessorAddress("Street 1")
                .tenantAddress("Street 2")
                .leaseEndDate(null)
                .introductoryRentFrom(LocalDate.of(2025, 1, 1))
                .introductoryRentUntil(LocalDate.of(2026, 12, 31))
                .rentDueInterval("annual")
                .rentDueDay("first")
                .introductoryRentWords("word one")
                .rentWords("word two")
                .build();
        return new ContractEntry(UUID.randomUUID(), contract, status, Instant.now(), Instant.now(), java.util.List.of(), null);
    }
}
