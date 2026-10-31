package kienzle.desktop;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import kienzle.desktop.app.AppContext;
import kienzle.desktop.contract.ContractEntry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.MonthDay;
import java.util.List;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MainLayoutControllerTest {
    @TempDir
    Path tempDir;

    @BeforeAll
    static void startToolkit() throws Exception {
        JavaFxTestSupport.startToolkit();
    }

    @Test
    void buildDetailRows_shouldContainSelectedContractDetails() {
        LeaseContract contract = TestContracts.sampleContract().toBuilder()
                .name("Pachtvertrag Muster")
                .lessor("Müller GmbH")
                .tenant("Hans Weber")
                .tenantDate(LocalDate.of(2025, 1, 1))
                .parcelNumber("123/4")
                .district("Gemarkung Ost")
                .description("Erbpachtfläche für landwirtschaftliche Nutzung")
                .leaseDuration("unbestimmte Dauer")
                .firstRentDate(LocalDate.of(2025, 1, 1))
                .rentRecipient("Müller GmbH")
                .bankName("Raiffeisenbank Ost")
                .iban("DE12 3456 7890 1234 5678 90")
                .paragraphFour("§ 4 Pachtverhältnis: 6 Monate Kündigungsfrist; § 5 Gartenjahr endet am 31.10.")
                .paragraphFive("§ 5: Kündigungsfrist 6 Monate, Gartenjahr endet am 31.10.")
                .lessorAddress("Musterstraße 1, 10115 Berlin")
                .tenantAddress("Beispielweg 2, 10117 Berlin")
                .introductoryRentFrom(LocalDate.of(2026, 12, 31))
                .introductoryRentUntil(LocalDate.of(2025, 1, 1))
                .rentDueDay("zum 3. Werktag des Jahres")
                .introductoryRentWords("eintausendneunhundert Euro")
                .rentWords("zweitausenddreihundertfünfzig Euro")
                .build();

        List<ContractDetailRow> rows = ContractDetailRows.of(contract);

        ContractDetailRow vertragsname = rows.stream()
                .filter(row -> row.key().equals("Vertragsname"))
                .findFirst()
                .orElseThrow();
        ContractDetailRow verpaechter = rows.stream()
                .filter(row -> row.key().equals("Verpächter"))
                .findFirst()
                .orElseThrow();
        ContractDetailRow flurstueck = rows.stream()
                .filter(row -> row.key().equals("Flurstück"))
                .findFirst()
                .orElseThrow();
        ContractDetailRow pachtzins = rows.stream()
                .filter(row -> row.key().equals("Regulärer Pachtzins in Euro"))
                .findFirst()
                .orElseThrow();
        ContractDetailRow beschreibung = rows.stream()
                .filter(row -> row.key().equals("Beschreibung"))
                .findFirst()
                .orElseThrow();

        assertEquals("Pachtvertrag Muster", vertragsname.value());
        assertEquals("Müller GmbH", verpaechter.value());
        assertEquals("123/4", flurstueck.value());
        assertEquals("2.350,00 €", pachtzins.value());
        assertEquals("Erbpachtfläche für landwirtschaftliche Nutzung", beschreibung.value());
    }

    @Test
    void handleRestoredArchivedEntry_resetsFilterAndSelectsRestoredEntry() throws Exception {
        AppContext context = AppContext.create(tempDir);
        ContractEntry restored = context.workflow().save(null, TestContracts.sampleContract("Restored"));

        MainLayoutController controller = new MainLayoutController(context);
        onFxThread(() -> {
            wireUiFields(controller);
            controller.initialize();
            controller.selectListIndex(0);
            return null;
        });

        onFxThread(() -> {
            ComboBox<StatusFilter> filter = getField(controller, "statusFilter", ComboBox.class);
            filter.setValue(StatusFilter.SIGNED);
            invoke(controller, "handleRestoredArchivedEntry", ContractEntry.class, restored);
            return null;
        });

        ComboBox<StatusFilter> filter = getField(controller, "statusFilter", ComboBox.class);
        ListView<ContractEntry> list = getField(controller, "contractList", ListView.class);
        assertEquals(StatusFilter.ALL, filter.getValue());
        assertEquals(restored.id(), list.getSelectionModel().getSelectedItem().id());
    }

    private static void wireUiFields(MainLayoutController controller) throws Exception {
        setField(controller, "contractList", new ListView<ContractEntry>());
        setField(controller, "statusFilter", new ComboBox<StatusFilter>());
        setField(controller, "statusLabel", new Label());
        setField(controller, "saveButton", new Button());
        setField(controller, "deleteButton", new Button());
        setField(controller, "archiveButton", new Button());
        setField(controller, "createPdfButton", new Button());
        setField(controller, "openPdfButton", new Button());
        setField(controller, "awaitingButton", new Button());
        setField(controller, "revokeAwaitingButton", new Button());
        setField(controller, "printPdfButton", new Button());
        setField(controller, "germanButton", new Button());
        setField(controller, "englishButton", new Button());
        setField(controller, "lblTitle", new Label());
        setField(controller, "lblVerpaechterTxt", new Label());
        setField(controller, "contractFormController", loadedFormController());
    }

    private static ContractFormController loadedFormController() throws Exception {
        FXMLLoader loader = new FXMLLoader(ContractFormController.class.getResource("ContractForm.fxml"), I18n.bundle());
        Parent ignored = loader.load();
        return loader.getController();
    }

    private static <T> T onFxThread(ThrowingSupplier<T> supplier) throws Exception {
        FutureTask<T> task = new FutureTask<>(supplier::get);
        Platform.runLater(task);
        return task.get(10, TimeUnit.SECONDS);
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static <T> T getField(Object target, String name, Class<T> type) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return type.cast(field.get(target));
    }

    private static void invoke(Object target, String methodName, Class<?> parameterType, Object arg) throws Exception {
        Method method = target.getClass().getDeclaredMethod(methodName, parameterType);
        method.setAccessible(true);
        method.invoke(target, arg);
    }

    @FunctionalInterface
    private interface ThrowingSupplier<T> {
        T get() throws Exception;
    }
}
