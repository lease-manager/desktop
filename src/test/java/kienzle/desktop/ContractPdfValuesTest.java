package kienzle.desktop;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.MonthDay;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ContractPdfValuesTest {
    @Test
    void mapsTypedFieldsToGermanText() {
        LeaseContract contract = LeaseContract.builder()
                .rent(new BigDecimal("2350")).size(new BigDecimal("4800.00"))
                .startDate(LocalDate.of(2025, 1, 1)).gardeningYearEnd(MonthDay.of(10, 31))
                .terminationNotice("6 Monate").lessorLocation("Berlin").lessorDate(LocalDate.of(2025, 1, 2))
                .hasIntroductoryRent(false).introductoryRent(new BigDecimal("1")).build();

        Map<String, String> values = ContractPdfValues.from(contract);

        assertEquals("2350,00", values.get("rent"));
        assertEquals("4800", values.get("size"));
        assertEquals("01.01.2025", values.get("startDate"));
        assertEquals("31.10.", values.get("gardeningYearEnd"));
        assertEquals("sechs", values.get("terminationNotice"));
        assertEquals("Berlin, den 02.01.2025", values.get("lessorSignatureDate"));
        assertEquals("", values.get("introductoryRent"));
    }
}
