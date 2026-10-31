package kienzle.desktop;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.MessageFormat;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class AmtsgerichtRepository {
    private static final String RESOURCE = "amtsgerichte.json";

    public List<Amtsgericht> load() throws IOException {
        try (InputStream stream = AmtsgerichtRepository.class.getResourceAsStream(RESOURCE)) {
            if (stream == null) {
                throw new IOException(message("court.error.notFound", RESOURCE));
            }
            JsonElement root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
            if (!root.isJsonArray()) {
                throw new IOException(message("court.error.notArray"));
            }
            return parseCourts(root.getAsJsonArray());
        } catch (IllegalStateException exception) {
            throw new IOException(message("court.error.invalidJson"), exception);
        }
    }

    List<Amtsgericht> parseCourts(JsonArray entries) throws IOException {
        List<Amtsgericht> courts = new ArrayList<>();
        for (int index = 0; index < entries.size(); index++) {
            JsonElement entry = entries.get(index);
            if (!entry.isJsonObject()) {
                throw new IOException(message("court.error.notObject", index + 1));
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
            boolean hasStreetAddress = !court.strasse().isBlank();
            boolean hasPostfach = !court.postfach().isBlank();
            if (hasStreetAddress == hasPostfach) {
                throw new IOException(message("court.error.address", index + 1));
            }
            courts.add(court);
        }
        return List.copyOf(courts);
    }

    private static String message(String key, Object... arguments) {
        return MessageFormat.format(I18n.get(key), arguments);
    }

    private String required(JsonObject object, String key, int index) throws IOException {
        String value = optional(object, key);
        if (value.isBlank()) {
            throw new IOException(message("court.error.required", key, index + 1));
        }
        return value;
    }

    private String optional(JsonObject object, String key) {
        JsonElement value = object.get(key);
        return value == null || value.isJsonNull() ? "" : value.getAsString().trim();
    }
}