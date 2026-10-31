package kienzle.desktop;

import javafx.collections.FXCollections;
import javafx.scene.control.ListView;
import kienzle.desktop.contract.ContractEntry;
import kienzle.desktop.contract.ContractStatus;
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
    void removeCurrentEntry_removesSelectedEntryFromList() {
        ContractListState state = new ContractListState();
        ContractEntry first = contract("first", ContractStatus.DRAFT);
        ContractEntry second = contract("second", ContractStatus.PDF_CREATED);
        state.setAll(FXCollections.observableArrayList(first, second));
        state.setCurrentEntry(first);

        state.removeCurrentEntry();

        assertEquals(1, state.visibleEntries().size());
        assertEquals(second.id(), state.visibleEntries().get(0).id());
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
    void ensureVisible_clearsSelectionWhenEntryHiddenByFilter() {
        ContractListState state = new ContractListState();
        ContractEntry signed = contract("signed", ContractStatus.SIGNED);
        state.setAll(FXCollections.observableArrayList(signed));
        state.applyFilter(StatusFilter.OPEN);
        state.setCurrentEntry(signed);

        ListView<ContractEntry> list = new ListView<>();
        list.setItems(state.visibleEntries());
        list.getSelectionModel().select(signed);

        state.ensureVisible(list, signed);

        assertNull(list.getSelectionModel().getSelectedItem());
    }

    private static ContractEntry contract(String name, ContractStatus status) {
        LeaseContract contract = new LeaseContract(
                name,
                "Lessor",
                "Berlin",
                LocalDate.of(2025, 1, 1),
                "Lessee",
                "Berlin",
                LocalDate.of(2025, 1, 2),
                "123/45",
                "Parcel",
                new BigDecimal("3000"),
                "Field",
                "Description",
                new BigDecimal("1500"),
                "permanent",
                LocalDate.of(2025, 1, 3),
                new BigDecimal("900"),
                true,
                LocalDate.of(2027, 1, 1),
                LocalDate.of(2025, 2, 1),
                "Bank",
                "IBAN",
                "DE89 3704 0044 0532 0130 00",
                "GENODEF1RFS",
                "Court",
                "6 months",
                MonthDay.of(10, 31),
                "Clause 1",
                "Clause 2",
                "Street 1",
                "Street 2",
                null,
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2026, 12, 31),
                "annual",
                "first",
                "word one",
                "word two"
        );
        return new ContractEntry(UUID.randomUUID(), contract, status, Instant.now(), Instant.now(), java.util.List.of(), null);
    }
}
