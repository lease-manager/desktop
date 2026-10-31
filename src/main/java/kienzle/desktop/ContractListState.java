package kienzle.desktop;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.ListView;
import kienzle.desktop.contract.ContractEntry;

import java.util.List;
import java.util.UUID;

/** Maintains list, filter and selection state for the contract list view. */
final class ContractListState {
    private final ObservableList<ContractEntry> entries = FXCollections.observableArrayList();
    private final FilteredList<ContractEntry> visibleEntries = new FilteredList<>(entries);
    private ContractEntry currentEntry;
    private StatusFilter activeFilter = StatusFilter.ALL;

    ContractListState() {
        visibleEntries.setPredicate(activeFilter);
    }

    ObservableList<ContractEntry> visibleEntries() {
        return visibleEntries;
    }

    void setAll(List<ContractEntry> source) {
        entries.setAll(source);
        if (currentEntry != null && entries.stream().noneMatch(entry -> entry.id().equals(currentEntry.id()))) {
            currentEntry = null;
        }
    }

    void applyFilter(StatusFilter filter) {
        activeFilter = filter == null ? StatusFilter.ALL : filter;
        visibleEntries.setPredicate(activeFilter);
    }

    StatusFilter activeFilter() {
        return activeFilter;
    }

    boolean isFilteredOut(ContractEntry entry) {
        return entry != null && !activeFilter.test(entry);
    }

    void replaceEntry(ContractEntry saved) {
        if (saved == null) {
            return;
        }
        addEntry(saved);
    }

    void addEntry(ContractEntry entry) {
        if (entry == null) {
            return;
        }
        int index = indexOf(entry.id());
        if (index >= 0) {
            entries.set(index, entry);
            if (currentEntry != null && currentEntry.id().equals(entry.id())) {
                currentEntry = entry;
            }
            return;
        }
        entries.add(entry);
    }

    void removeCurrentEntry() {
        if (currentEntry == null) {
            return;
        }
        removeEntry(currentEntry.id());
        currentEntry = null;
    }

    private void removeEntry(UUID entryId) {
        entries.removeIf(entry -> entry.id().equals(entryId));
    }

    boolean isEmpty() {
        return entries.isEmpty();
    }

    ContractEntry currentEntry() {
        return currentEntry;
    }

    boolean hasCurrentEntry() {
        return currentEntry != null;
    }

    void setCurrentEntry(ContractEntry entry) {
        currentEntry = entry;
    }

    void clearCurrentEntry() {
        currentEntry = null;
    }

    void ensureVisible(ListView<ContractEntry> contractList, ContractEntry entry) {
        if (entry == null) {
            return;
        }
        boolean visible = visibleEntries.stream().anyMatch(visibleEntry -> visibleEntry.id().equals(entry.id()));
        if (!visible) {
            var selectionModel = contractList.getSelectionModel();
            selectionModel.clearSelection();
            selectionModel.select(-1);
            if (currentEntry != null && currentEntry.id().equals(entry.id())) {
                currentEntry = null;
            }
            return;
        }
        contractList.getSelectionModel().select(entry);
        currentEntry = entry;
    }

    private int indexOf(UUID entryId) {
        for (int index = 0; index < entries.size(); index++) {
            if (entries.get(index).id().equals(entryId)) {
                return index;
            }
        }
        return -1;
    }
}
