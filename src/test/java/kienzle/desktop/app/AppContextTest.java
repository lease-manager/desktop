package kienzle.desktop.app;

import kienzle.desktop.LeaseContract;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.MonthDay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AppContextTest {
    @TempDir
    Path tempDir;

    @Test
    void create_buildsWorkflowAndPackageService() throws Exception {
        AppContext context = AppContext.create(tempDir);

        assertNotNull(context.workflow());
        assertNotNull(context.packages());
        assertEquals(0, context.workflow().list().size());

        LeaseContract contract = sampleContract();
        var saved = context.workflow().save(null, contract);

        assertEquals(1, context.workflow().list().size());
        assertEquals(saved.id(), context.workflow().list().get(0).id());

        context.workflow().archive(saved);
        assertEquals(0, context.workflow().list().size());

        var restored = context.workflow().restore(saved);
        assertEquals(saved.id(), restored.id());
    }

    private LeaseContract sampleContract() {
        return new LeaseContract(
                "Pachtvertrag",
                "Verpächter GmbH",
                "Berlin",
                LocalDate.of(2025, 1, 1),
                "Pächter",
                "Berlin",
                LocalDate.of(2025, 1, 2),
                "123/45",
                "Flur 1",
                new BigDecimal("4800.50"),
                "Ackerland",
                "Beschreibung",
                new BigDecimal("2350.00"),
                "Unbestimmte Dauer",
                LocalDate.of(2025, 1, 1),
                new BigDecimal("1900.00"),
                true,
                LocalDate.of(2027, 1, 1),
                LocalDate.of(2025, 2, 1),
                "Empfänger",
                "Bank",
                "DE89 3704 0044 0532 0130 00",
                "GENODEF1RFS",
                "Amtsgericht Berlin",
                "6 Monate",
                MonthDay.of(10, 31),
                "§ 4",
                "§ 5",
                "Musterstraße 1",
                "Musterstraße 2",
                null,
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2026, 12, 31),
                "jährlich",
                "1.",
                "Wortform 1",
                "Wortform 2"
        );
    }
}
