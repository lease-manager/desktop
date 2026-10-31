package kienzle.desktop;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.MonthDay;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ContractFormControllerTest {
    private static final AtomicBoolean TOOLKIT_STARTED = new AtomicBoolean(false);

    @BeforeAll
    static void startToolkit() throws Exception {
        if (TOOLKIT_STARTED.compareAndSet(false, true)) {
            CountDownLatch started = new CountDownLatch(1);
            Platform.startup(started::countDown);
            assertTrue(started.await(5, TimeUnit.SECONDS), "JavaFX toolkit did not start in time");
        }
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
                .parcelNumber("123/4")
                .district("Gemarkung Ost")
                .size(new BigDecimal("4800.5"))
                .type("Ackerland")
                .description("Erbpachtfläche")
                .rent(new BigDecimal("2350.00"))
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
        assertEquals(original.parcelNumber(), roundTripped.parcelNumber());
        assertEquals(original.district(), roundTripped.district());
        assertEquals(0, original.size().compareTo(roundTripped.size()));
        assertEquals(0, original.rent().compareTo(roundTripped.rent()));
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
            IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, form::read);
            return thrown.getMessage();
        });

        assertEquals(I18n.get("error.iban") + "DE12 0000", message);
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
