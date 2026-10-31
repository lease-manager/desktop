package kienzle.desktop;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.MonthDay;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MainLayoutControllerTest {

    @Test
    void buildDetailRows_shouldContainSelectedContractDetails() {
        LeaseContract contract = new LeaseContract(
                "Pachtvertrag Muster",
                "Müller GmbH",
                "Berlin",
                LocalDate.of(2025, 1, 1),
                "Hans Weber",
                "Berlin",
                LocalDate.of(2025, 1, 1),
                "123/4",
                "Gemarkung Ost",
                new BigDecimal("4800"),
                "Ackerland",
                "Erbpachtfläche für landwirtschaftliche Nutzung",
                new BigDecimal("2350.00"),
                "unbestimmte Dauer",
                LocalDate.of(2025, 1, 1),
                new BigDecimal("1900.00"),
                true,
                LocalDate.of(2027, 1, 1),
                LocalDate.of(2025, 1, 1),
                "Müller GmbH",
                "Raiffeisenbank Ost",
                "DE12 3456 7890 1234 5678 90",
                "GENODEF1RFS",
                "Amtsgericht Berlin",
                "6 Monate",
                MonthDay.of(10, 31),
                "§ 4 Pachtverhältnis: 6 Monate Kündigungsfrist; § 5 Gartenjahr endet am 31.10.",
                "§ 5: Kündigungsfrist 6 Monate, Gartenjahr endet am 31.10.",
                "Musterstraße 1, 10115 Berlin",
                "Beispielweg 2, 10117 Berlin",
                null,
                LocalDate.of(2026, 12, 31),
                LocalDate.of(2025, 1, 1),
                "jährlich",
                "zum 3. Werktag des Jahres",
                "eintausendneunhundert Euro",
                "zweitausenddreihundertfünfzig Euro"
        );

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
}
