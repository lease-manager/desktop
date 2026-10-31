package kienzle.desktop;

import kienzle.desktop.contract.ContractEntry;
import kienzle.desktop.contract.ContractStatus;

import java.util.function.Predicate;

public enum StatusFilter implements Predicate<ContractEntry> {
    ALL, OPEN, SIGNED;

    @Override
    public boolean test(ContractEntry entry) {
        return switch (this) {
            case ALL -> true;
            case OPEN -> entry.status() != ContractStatus.SIGNED;
            case SIGNED -> entry.status() == ContractStatus.SIGNED;
        };
    }
}
