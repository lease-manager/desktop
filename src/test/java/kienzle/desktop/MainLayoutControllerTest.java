package kienzle.desktop;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MainLayoutControllerTest {

    @Test
    void buildDetailRows_shouldContainSelectedContractDetails() {
        LeaseContract contract = new LeaseContract(
                "PV-001",
                "Pachtvertrag Muster",
                "Müller GmbH",
                "Berlin",
                "01.01.2025",
                "Hans Weber",
                "Berlin",
                "01.01.2025",
                "123/4",
                "Gemarkung Ost",
                "4800",
                "Ackerland",
                "Erbpachtfläche für landwirtschaftliche Nutzung",
                "2.350,00 €",
                "unbestimmte Dauer",
                "01.01.2025",
                "1.900,00 € für 24 Monate, danach 2.350,00 €",
                true,
                "01.01.2027",
                "01.01.2025",
                "Müller GmbH",
                "Raiffeisenbank Ost",
                "DE12 3456 7890 1234 5678 90",
                "GENODEF1RFS",
                "Amtsgericht Berlin",
                "6 Monate",
                "31. Oktober",
                "§ 4 Pachtverhältnis: 6 Monate Kündigungsfrist; § 5 Gartenjahr endet am 31.10.",
                "§ 5: Kündigungsfrist 6 Monate, Gartenjahr endet am 31.10.",
                "Musterstraße 1, 10115 Berlin",
                "Beispielweg 2, 10117 Berlin",
                "",
                "31.12.2026",
                "01.01.2025",
                "jährlich",
                "zum 3. Werktag des Jahres",
                "eintausendneunhundert Euro",
                "zweitausenddreihundertfünfzig Euro"
        );

        List<ContractDetailRow> rows = new MainLayoutController().buildDetailRows(contract);

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
