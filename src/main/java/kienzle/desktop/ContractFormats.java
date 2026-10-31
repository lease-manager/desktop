package kienzle.desktop;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.MonthDay;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

/** German text formats for the typed contract fields (the contract PDF stays German). */
public final class ContractFormats {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.uuuu");
    private static final DateTimeFormatter DATE_SHORT = DateTimeFormatter.ofPattern("d.M.uuuu");
    private static final DateTimeFormatter MONTH_DAY = DateTimeFormatter.ofPattern("dd.MM.");

    private ContractFormats() {
    }

    public static String date(LocalDate date) {
        return date == null ? "" : date.format(DATE);
    }

    public static String monthDay(MonthDay monthDay) {
        return monthDay == null ? "" : monthDay.format(MONTH_DAY);
    }

    /** Amount with two decimals and a decimal comma, e.g. {@code 2350,00}. */
    public static String money(BigDecimal amount) {
        return amount == null ? "" : format("0.00", amount);
    }

    /** Amount with thousands separator and currency, e.g. {@code 2.350,00 €}. */
    public static String moneyEuro(BigDecimal amount) {
        return amount == null ? "" : format("#,##0.00", amount) + " €";
    }

    /** Plain number without trailing zeros, e.g. {@code 4800} or {@code 4800,5}. */
    public static String number(BigDecimal value) {
        return value == null ? "" : format("0.##", value);
    }

    public static LocalDate parseDate(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String trimmed = text.trim();
        for (DateTimeFormatter formatter : new DateTimeFormatter[]{DATE, DATE_SHORT, DateTimeFormatter.ISO_LOCAL_DATE}) {
            try {
                return LocalDate.parse(trimmed, formatter);
            } catch (DateTimeParseException ignored) {
                // Try the next supported input format.
            }
        }
        return null;
    }

    public static MonthDay parseMonthDay(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String trimmed = text.trim();
        String withYear;
        if (trimmed.matches(".*\\d{4}$")) {
            withYear = trimmed;
        } else if (trimmed.matches(".*\\p{L}.*")) {
            withYear = trimmed + " 2000";
        } else {
            withYear = trimmed.replaceAll("\\.$", "") + ".2000";
        }
        for (DateTimeFormatter formatter : new DateTimeFormatter[]{DATE, DATE_SHORT,
                DateTimeFormatter.ofPattern("d. MMMM yyyy", Locale.GERMAN)}) {
            try {
                return MonthDay.from(LocalDate.parse(withYear, formatter));
            } catch (DateTimeParseException ignored) {
                // Try the next supported input format.
            }
        }
        return null;
    }

    /**
     * Parses German ({@code 2.350,00}) and plain ({@code 2350.5}) amounts.
     *
     * @return {@code null} for blank input
     * @throws NumberFormatException for text that is not a number
     */
    public static BigDecimal parseDecimal(String text) {
        if (text == null) {
            return null;
        }
        String cleaned = text.replace("€", "").replace("m²", "").trim().replace(" ", "");
        if (cleaned.isEmpty()) {
            return null;
        }
        if (cleaned.contains(",")) {
            cleaned = cleaned.replace(".", "").replace(',', '.');
        } else if (cleaned.matches("\\d{1,3}(\\.\\d{3})+")) {
            cleaned = cleaned.replace(".", "");
        }
        return new BigDecimal(cleaned);
    }

    private static String format(String pattern, BigDecimal value) {
        return new DecimalFormat(pattern, DecimalFormatSymbols.getInstance(Locale.GERMANY)).format(value);
    }
}
