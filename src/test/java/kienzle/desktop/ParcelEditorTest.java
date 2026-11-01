package kienzle.desktop;

import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ParcelEditorTest {
    @BeforeAll
    static void startToolkit() throws Exception {
        JavaFxTestSupport.startToolkit();
        I18n.setLocale(Locale.GERMAN);
    }

    @Test
    void startsWithOneBlankRowThatReadsAsNoParcels() throws Exception {
        onFx(() -> {
            ParcelEditor editor = new ParcelEditor();
            assertEquals(1, editor.rowCount());
            assertEquals(List.of(), editor.read());
            return null;
        });
    }

    @Test
    void flagsFilledRowsWithoutNumberOrValidRent() throws Exception {
        onFx(() -> {
            ParcelEditor editor = new ParcelEditor();
            assertEquals(false, editor.hasErrors());
            editor.setDrafts(List.of(new ParcelEditor.Draft("", "Flur", "", "", "", "10")));
            assertEquals(true, editor.hasErrors());
            editor.setDrafts(List.of(new ParcelEditor.Draft("1", "", "", "", "", "")));
            assertEquals(true, editor.hasErrors());
            editor.setDrafts(List.of(new ParcelEditor.Draft("1", "", "", "", "", "abc")));
            assertEquals(true, editor.hasErrors());
            editor.setDrafts(List.of(new ParcelEditor.Draft("1", "", "", "", "", "10,00")));
            assertEquals(false, editor.hasErrors());
            return null;
        });
    }

    @Test
    void acceptsOnlyOneParcelNumberPerRow() {
        for (String ok : List.of("1080", "1080/1", " 7/3 ")) {
            assertEquals(true, ParcelEditor.validParcelNumber(ok), ok);
        }
        for (String bad : List.of("", "abc", "1080/", "/1", "10 80x", "1080/1 1080/2", "1080/1, 1080/2")) {
            assertEquals(false, ParcelEditor.validParcelNumber(bad), bad);
        }
    }

    @Test
    void readsSeveralParcelsAndSumsTheRent() throws Exception {
        onFx(() -> {
            ParcelEditor editor = new ParcelEditor();
            editor.setDrafts(List.of(
                    new ParcelEditor.Draft("1/2", "Flur", "4.800,5", "Acker", "Nord", "2.000,00"),
                    new ParcelEditor.Draft("", "", "", "", "", ""),
                    new ParcelEditor.Draft("1/3", "Flur", "", "Wiese", "", "350,50")));

            List<Parcel> parcels = editor.read();

            assertEquals(2, parcels.size());
            assertEquals(0, new BigDecimal("4800.5").compareTo(parcels.get(0).size()));
            assertEquals(0, new BigDecimal("2350.50").compareTo(editor.totalRent()));
            return null;
        });
    }

    @Test
    void invalidNumberIsReportedWhenReadingButKeptInTheDrafts() throws Exception {
        onFx(() -> {
            ParcelEditor editor = new ParcelEditor();
            ParcelEditor.Draft draft = new ParcelEditor.Draft("1", "", "abc", "", "", "10");
            editor.setDrafts(List.of(draft));

            assertThrows(IllegalArgumentException.class, editor::read);
            assertEquals(List.of(draft), editor.drafts());
            assertEquals(0, BigDecimal.TEN.compareTo(editor.totalRent()));
            return null;
        });
    }

    @Test
    void notifiesOnChangeAndNeverRemovesTheLastRow() throws Exception {
        onFx(() -> {
            ParcelEditor editor = new ParcelEditor();
            int[] calls = {0};
            editor.setOnChange(() -> calls[0]++);
            editor.setParcels(List.of(new Parcel("1", "", null, "", "", new BigDecimal("5"))));
            int afterSet = calls[0];
            ((javafx.scene.control.Button) editor.lookup("#addParcelButton")).fire();

            assertEquals(2, editor.rowCount());
            assertEquals(afterSet + 1, calls[0]);
            return null;
        });
    }

    private static <T> T onFx(Callable<T> callable) throws Exception {
        FutureTask<T> task = new FutureTask<>(callable);
        Platform.runLater(task);
        return task.get(10, TimeUnit.SECONDS);
    }
}
