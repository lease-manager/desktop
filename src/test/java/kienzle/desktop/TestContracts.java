package kienzle.desktop;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.MonthDay;

public final class TestContracts {
    private TestContracts() {
    }

    public static LeaseContract sampleContract() {
        return sampleContract("Pachtvertrag");
    }

    public static LeaseContract sampleContract(String name) {
        return LeaseContract.builder()
                .name(name)
                .lessor("Verpächter GmbH")
                .lessorLocation("Berlin")
                .lessorDate(LocalDate.of(2025, 1, 1))
                .tenant("Pächter")
                .tenantLocation("Berlin")
                .tenantDate(LocalDate.of(2025, 1, 2))
                .parcelNumber("123/45")
                .district("Flur 1")
                .size(new BigDecimal("4800.50"))
                .type("Ackerland")
                .description("Beschreibung")
                .rent(new BigDecimal("2350.00"))
                .leaseDuration("Unbestimmte Dauer")
                .startDate(LocalDate.of(2025, 1, 1))
                .introductoryRent(new BigDecimal("1900.00"))
                .hasIntroductoryRent(true)
                .rentIncreaseDate(LocalDate.of(2027, 1, 1))
                .firstRentDate(LocalDate.of(2025, 2, 1))
                .rentRecipient("Empfänger")
                .bankName("Bank")
                .iban("DE89 3704 0044 0532 0130 00")
                .bic("GENODEF1RFS")
                .court("Amtsgericht Berlin")
                .terminationNotice("6 Monate")
                .gardeningYearEnd(MonthDay.of(10, 31))
                .paragraphFour("§ 4")
                .paragraphFive("§ 5")
                .lessorAddress("Musterstraße 1")
                .tenantAddress("Musterstraße 2")
                .leaseEndDate(null)
                .introductoryRentFrom(LocalDate.of(2025, 1, 1))
                .introductoryRentUntil(LocalDate.of(2026, 12, 31))
                .rentDueInterval("jährlich")
                .rentDueDay("1.")
                .introductoryRentWords("Wortform 1")
                .rentWords("Wortform 2")
                .build();
    }
}
