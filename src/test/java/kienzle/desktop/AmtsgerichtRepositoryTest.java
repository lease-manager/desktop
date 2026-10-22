package kienzle.desktop;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AmtsgerichtRepositoryTest {
    private final AmtsgerichtRepository repository = new AmtsgerichtRepository();

    @Test
    void load_shouldReadCourtsFromJsonResource() throws IOException {
        var courts = repository.load();

        assertTrue(courts.size() >= 3);
        Amtsgericht berlin = courts.stream()
                .filter(court -> court.name().equals("Amtsgericht Mitte"))
                .findFirst()
                .orElseThrow();
        assertEquals("10179", berlin.plz());
        assertEquals("Berlin", berlin.ort());
        assertEquals("Littenstraße 12-17", berlin.strasse() + " " + berlin.hausnummer());
        assertEquals("Littenstraße 12-17, 10179 Berlin", berlin.anschrift());
    }

    @Test
    void parseCourts_shouldAcceptPostfachInsteadOfStreetAddress() throws IOException {
        var courts = repository.parseCourts(JsonParser.parseString("""
                [{"name":"Amtsgericht Test","plz":"12345","ort":"Teststadt","postfach":"42"}]
                """).getAsJsonArray());

        assertEquals("Postfach 42, 12345 Teststadt", courts.get(0).anschrift());
    }

    @Test
    void parseCourts_shouldAcceptStreetAddressWithoutHouseNumber() throws IOException {
        var courts = repository.parseCourts(JsonParser.parseString("""
                [{"name":"Amtsgericht Test","plz":"12345","ort":"Teststadt","strasse":"Schloss"}]
                """).getAsJsonArray());

        assertEquals("Schloss, 12345 Teststadt", courts.get(0).anschrift());
    }

    @Test
    void parseCourts_shouldRejectEntryWithoutCompleteAddress() {
        assertThrows(IOException.class, () -> repository.parseCourts(JsonParser.parseString("""
                [{"name":"Amtsgericht Test","plz":"12345","ort":"Teststadt"}]
                """).getAsJsonArray()));
    }
}
