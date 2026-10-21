package kienzle.desktop;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class AmtsgerichtRepository {
    private static final String RESOURCE = "amtsgerichte.json";

    public List<Amtsgericht> load() throws IOException {
        try (InputStream stream = AmtsgerichtRepository.class.getResourceAsStream(RESOURCE)) {
            if (stream == null) {
                throw new IOException("Die Amtsgericht-Liste " + RESOURCE + " wurde nicht gefunden.");
            }
            JsonElement root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
            if (!root.isJsonArray()) {
                throw new IOException("Die Amtsgericht-Liste muss ein JSON-Array sein.");
            }
            return parseCourts(root.getAsJsonArray());
        } catch (IllegalStateException exception) {
            throw new IOException("Die Amtsgericht-Liste enthält ungültige JSON-Daten.", exception);
        }
    }

    List<Amtsgericht> parseCourts(JsonArray entries) throws IOException {
        List<Amtsgericht> courts = new ArrayList<>();
        for (int index = 0; index < entries.size(); index++) {
            JsonElement entry = entries.get(index);
            if (!entry.isJsonObject()) {
                throw new IOException("Eintrag " + (index + 1) + " muss ein JSON-Objekt sein.");
            }
            JsonObject object = entry.getAsJsonObject();
            Amtsgericht court = new Amtsgericht(
                    required(object, "name", index),
                    required(object, "plz", index),
                    required(object, "ort", index),
                    optional(object, "strasse"),
                    optional(object, "hausnummer"),
                    optional(object, "postfach")
            );
            boolean hasStreetAddress = !court.strasse().isBlank() && !court.hausnummer().isBlank();
            boolean hasPostfach = !court.postfach().isBlank();
            if (hasStreetAddress == hasPostfach) {
                throw new IOException("Eintrag " + (index + 1)
                        + " benötigt entweder Straße und Hausnummer oder ein Postfach.");
            }
            courts.add(court);
        }
        return List.copyOf(courts);
    }

    private String required(JsonObject object, String key, int index) throws IOException {
        String value = optional(object, key);
        if (value.isBlank()) {
            throw new IOException("Pflichtfeld '" + key + "' fehlt in Eintrag " + (index + 1) + ".");
        }
        return value;
    }

    private String optional(JsonObject object, String key) {
        JsonElement value = object.get(key);
        return value == null || value.isJsonNull() ? "" : value.getAsString().trim();
    }
}