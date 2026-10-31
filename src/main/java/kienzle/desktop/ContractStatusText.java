package kienzle.desktop;

import kienzle.desktop.contract.ContractEntry;

import java.time.ZoneId;
import java.util.Locale;

/** Localized, human-readable status line for a contract. */
public final class ContractStatusText {
    private ContractStatusText() {
    }

    public static String describe(ContractEntry entry) {
        String text = I18n.get("status." + entry.status().name().toLowerCase(Locale.ROOT));
        if (entry.signature() != null) {
            text += " (" + I18n.get("source." + entry.signature().source().name().toLowerCase(Locale.ROOT))
                    + ", " + ContractFormats.date(entry.signature().signedAt().atZone(java.time.ZoneId.systemDefault())
                    .toLocalDate()) + ")";
        }
        return text;
    }
}
