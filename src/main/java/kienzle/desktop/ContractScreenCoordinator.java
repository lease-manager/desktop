package kienzle.desktop;

import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.ListView;
import kienzle.desktop.contract.ContractEntry;
import kienzle.desktop.contract.ContractStatus;

/** Coordinates selection, filtering and view updates for the contract screen. */
public final class ContractScreenCoordinator {
    private final ObservableList<ContractEntry> entries;
    private final FilteredList<ContractEntry> visibleEntries;
    private ContractEntry currentEntry;

    public ContractScreenCoordinator(ObservableList<ContractEntry> entries, FilteredList<ContractEntry> visibleEntries) {
        this.entries = entries;
        this.visibleEntries = visibleEntries;
    }

    public void applyFilter(StatusFilter filter) {
        visibleEntries.setPredicate(filter == null ? StatusFilter.ALL : filter);
    }

    public void replaceEntry(ContractEntry saved) {
        for (int index = 0; index < entries.size(); index++) {
            if (entries.get(index).id().equals(saved.id())) {
                entries.set(index, saved);
                return;
            }
        }
        entries.add(saved);
    }

    public void selectEntry(ListView<ContractEntry> contractList, ContractEntry entry) {
        currentEntry = entry;
        if (entry == null) {
            contractList.getSelectionModel().clearSelection();
            return;
        }
        contractList.getSelectionModel().select(entry);
    }

    public ContractEntry currentEntry() {
        return currentEntry;
    }

    public void setCurrentEntry(ContractEntry entry) {
        currentEntry = entry;
    }

    public boolean isEntryStatusAllowed(ContractStatus status) {
        return status == null || status == ContractStatus.DRAFT || status == ContractStatus.PDF_CREATED;
    }
}
