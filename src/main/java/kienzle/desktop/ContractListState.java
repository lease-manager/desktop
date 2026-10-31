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
        int index = indexOf(saved.id());
        if (index >= 0) {
            entries.set(index, saved);
            return;
        }
        entries.add(saved);
    }

    void addEntry(ContractEntry entry) {
        entries.add(entry);
    }

    void removeCurrentEntry() {
        if (currentEntry == null) {
            return;
        }
        UUID currentId = currentEntry.id();
        entries.removeIf(entry -> entry.id().equals(currentId));
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
        if (!visibleEntries.contains(entry)) {
            contractList.getSelectionModel().clearSelection();
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
