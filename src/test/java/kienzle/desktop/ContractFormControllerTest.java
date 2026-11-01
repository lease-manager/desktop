package kienzle.desktop;

import java.util.List;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.MonthDay;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ContractFormControllerTest {
    @BeforeAll
    static void startToolkit() throws Exception {
        JavaFxTestSupport.startToolkit();
    }

    @Test
    void fillThenRead_roundTripsContractValues() throws Exception {
        LeaseContract original = LeaseContract.builder()
                .name("Pachtvertrag Muster")
                .lessor("Müller GmbH")
                .lessorAddress("Musterstraße 1")
                .lessorLocation("Berlin")
                .lessorDate(LocalDate.of(2025, 1, 1))
                .tenant("Hans Weber")
                .tenantAddress("Beispielweg 2")
                .tenantLocation("Berlin")
                .tenantDate(LocalDate.of(2025, 1, 2))
                .parcels(List.of(
                        new Parcel("123/4", "Gemarkung Ost", new BigDecimal("4800.5"), "Ackerland",
                                "Nordfeld", new BigDecimal("2000.00")),
                        new Parcel("123/5", "Gemarkung West", new BigDecimal("1200"), "Wiese",
                                "", new BigDecimal("350.00"))))
                .description("Erbpachtfläche")
                .leaseDuration(ContractDefaults.DURATION_FIXED)
                .startDate(LocalDate.of(2025, 1, 1))
                .leaseEndDate(LocalDate.of(2026, 12, 31))
                .hasIntroductoryRent(true)
                .introductoryRent(new BigDecimal("1900.00"))
                .introductoryRentFrom(LocalDate.of(2025, 1, 1))
                .introductoryRentUntil(LocalDate.of(2025, 12, 31))
                .introductoryRentWords("eintausendneunhundert Euro")
                .rentIncreaseDate(LocalDate.of(2027, 1, 1))
                .rentWords("zweitausenddreihundertfünfzig Euro")
                .rentDueInterval("jährlich")
                .rentDueDay("3. Werktag")
                .firstRentDate(LocalDate.of(2025, 2, 1))
                .rentRecipient("Müller GmbH")
                .bankName("Raiffeisenbank Ost")
                .iban("DE89 3704 0044 0532 0130 00")
                .bic("GENODEF1RFS")
                .court("")
                .terminationNotice("6 Monate")
                .gardeningYearEnd(MonthDay.of(10, 31))
                .paragraphFour("§ 4")
                .paragraphFive("§ 5")
                .build();

        LeaseContract roundTripped = onFxThread(() -> {
            FXMLLoader loader = new FXMLLoader(ContractFormController.class.getResource("ContractForm.fxml"),
                    I18n.bundle());
            Parent ignored = loader.load();
            ContractFormController form = loader.getController();
            form.fill(original);
            return form.read();
        });

        assertEquals(original.name(), roundTripped.name());
        assertEquals(original.lessor(), roundTripped.lessor());
        assertEquals(original.tenant(), roundTripped.tenant());
        assertEquals(original.parcels().size(), roundTripped.parcels().size());
        for (int index = 0; index < original.parcels().size(); index++) {
            Parcel expected = original.parcels().get(index);
            Parcel actual = roundTripped.parcels().get(index);
            assertEquals(expected.number(), actual.number());
            assertEquals(expected.district(), actual.district());
            assertEquals(0, expected.size().compareTo(actual.size()));
            assertEquals(expected.designation(), actual.designation());
            assertEquals(0, expected.rent().compareTo(actual.rent()));
        }
        assertEquals(0, new BigDecimal("2350.00").compareTo(roundTripped.totalRent()));
        assertEquals(original.leaseDuration(), roundTripped.leaseDuration());
        assertEquals(original.startDate(), roundTripped.startDate());
        assertEquals(original.leaseEndDate(), roundTripped.leaseEndDate());
        assertEquals(original.hasIntroductoryRent(), roundTripped.hasIntroductoryRent());
        assertEquals(0, original.introductoryRent().compareTo(roundTripped.introductoryRent()));
        assertEquals(original.introductoryRentFrom(), roundTripped.introductoryRentFrom());
        assertEquals(original.introductoryRentUntil(), roundTripped.introductoryRentUntil());
        assertEquals(original.rentIncreaseDate(), roundTripped.rentIncreaseDate());
        assertEquals(original.firstRentDate(), roundTripped.firstRentDate());
        assertEquals(original.court(), roundTripped.court());
        assertEquals(original.gardeningYearEnd(), roundTripped.gardeningYearEnd());
    }

    @Test
    void fillThenRead_withoutIntroductoryRent_clearsOptionalIntroFields() throws Exception {
        LeaseContract original = LeaseContract.builder()
                .name("Ohne Einführungszins")
                .lessor("Beispiel Verpächter")
                .tenant("Beispiel Pächter")
                .leaseDuration(ContractDefaults.DURATION_INDEFINITE)
                .hasIntroductoryRent(false)
                .introductoryRent(null)
                .introductoryRentFrom(null)
                .introductoryRentUntil(null)
                .rentIncreaseDate(null)
                .build();

        LeaseContract roundTripped = onFxThread(() -> {
            FXMLLoader loader = new FXMLLoader(ContractFormController.class.getResource("ContractForm.fxml"),
                    I18n.bundle());
            Parent ignored = loader.load();
            ContractFormController form = loader.getController();
            form.fill(original);
            return form.read();
        });

        assertEquals(original.name(), roundTripped.name());
        assertEquals(original.lessor(), roundTripped.lessor());
        assertEquals(original.tenant(), roundTripped.tenant());
        assertEquals(original.leaseDuration(), roundTripped.leaseDuration());
        assertEquals(original.hasIntroductoryRent(), roundTripped.hasIntroductoryRent());
        assertNull(roundTripped.introductoryRent());
        assertNull(roundTripped.introductoryRentFrom());
        assertNull(roundTripped.introductoryRentUntil());
        assertNull(roundTripped.rentIncreaseDate());
    }

    @Test
    void read_rejectsInvalidIban() throws Exception {
        LeaseContract invalidIban = LeaseContract.builder()
                .name("Ungültige IBAN")
                .lessor("Lessor")
                .tenant("Tenant")
                .iban("DE12 0000")
                .build();

        String message = onFxThread(() -> {
            FXMLLoader loader = new FXMLLoader(ContractFormController.class.getResource("ContractForm.fxml"),
                    I18n.bundle());
            Parent ignored = loader.load();
            ContractFormController form = loader.getController();
            form.fill(invalidIban);
            assertTrue(form.hasValidationErrors());
            IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, form::read);
            return thrown.getMessage();
        });

        assertEquals(I18n.get("error.iban") + "DE12 0000", message);
    }

    @Test
    void fill_withValidIban_hasNoValidationErrors() throws Exception {
        LeaseContract validIban = LeaseContract.builder()
                .name("Gültige IBAN")
                .lessor("Lessor")
                .tenant("Tenant")
                .iban("DE89 3704 0044 0532 0130 00")
                .build();

        boolean hasErrors = onFxThread(() -> {
            FXMLLoader loader = new FXMLLoader(ContractFormController.class.getResource("ContractForm.fxml"),
                    I18n.bundle());
            Parent ignored = loader.load();
            ContractFormController form = loader.getController();
            form.fill(validIban);
            return form.hasValidationErrors();
        });

        assertFalse(hasErrors);
    }

    @Test
    void headerCallback_followsEditsOfNameAndLessor() throws Exception {
        java.util.List<String> seen = onFxThread(() -> {
            FXMLLoader loader = new FXMLLoader(ContractFormController.class.getResource("ContractForm.fxml"),
                    I18n.bundle());
            Parent ignored = loader.load();
            ContractFormController form = loader.getController();
            java.util.List<String> calls = new java.util.ArrayList<>();
            form.setOnHeaderChanged((name, lessor) -> calls.add(name + "|" + lessor));
            form.fill(TestContracts.sampleContract("Neu").toBuilder().lessor("Müller").build());
            return calls;
        });

        assertEquals("|", seen.get(0));
        assertEquals("Neu|Müller", seen.get(seen.size() - 1));
    }

    private static <T> T onFxThread(ThrowingSupplier<T> supplier) throws Exception {
        FutureTask<T> task = new FutureTask<>(supplier::get);
        Platform.runLater(task);
        return task.get(10, TimeUnit.SECONDS);
    }

    @FunctionalInterface
    private interface ThrowingSupplier<T> {
        T get() throws Exception;
    }
}
