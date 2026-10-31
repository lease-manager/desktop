package kienzle.desktop;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.MonthDay;

import static org.junit.jupiter.api.Assertions.*;

class ContractFormatsTest {
    @Test
    void parseDecimal_acceptsGermanAndPlainNumbers() {
        assertEquals(new BigDecimal("2350.00"), ContractFormats.parseDecimal("2.350,00 €"));
        assertEquals(new BigDecimal("2350.5"), ContractFormats.parseDecimal("2350.5"));
        assertEquals(new BigDecimal("48000"), ContractFormats.parseDecimal("48.000"));
        assertNull(ContractFormats.parseDecimal("  "));
        assertThrows(NumberFormatException.class, () -> ContractFormats.parseDecimal("abc"));
    }

    @Test
    void formatsAreGerman() {
        assertEquals("2350,00", ContractFormats.money(new BigDecimal("2350")));
        assertEquals("2.350,00 €", ContractFormats.moneyEuro(new BigDecimal("2350")));
        assertEquals("4800", ContractFormats.number(new BigDecimal("4800.00")));
        assertEquals("01.02.2025", ContractFormats.date(LocalDate.of(2025, 2, 1)));
        assertEquals("31.10.", ContractFormats.monthDay(MonthDay.of(10, 31)));
    }

    @Test
    void parseDates() {
        assertEquals(LocalDate.of(2025, 2, 1), ContractFormats.parseDate("1.2.2025"));
        assertNull(ContractFormats.parseDate("quatsch"));
        assertEquals(MonthDay.of(10, 31), ContractFormats.parseMonthDay("31. Oktober"));
        assertEquals(MonthDay.of(10, 31), ContractFormats.parseMonthDay("31.10."));
        assertEquals(MonthDay.of(10, 31), ContractFormats.parseMonthDay("31.10.2024"));
    }
}
