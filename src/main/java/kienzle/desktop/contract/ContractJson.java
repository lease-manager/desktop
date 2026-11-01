package kienzle.desktop.contract;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import kienzle.desktop.LeaseContract;

import java.time.Instant;
import java.time.LocalDate;
import java.time.MonthDay;
import java.util.function.Function;

/** Gson configuration for contracts; reads contracts stored before multi-parcel support. */
public final class ContractJson {
    private static final String[] LEGACY_PARCEL_KEYS = {"parcelNumber", "district", "size", "type", "rent"};

    private ContractJson() {
    }

    public static Gson pretty() {
        return create(true);
    }

    public static Gson compact() {
        return create(false);
    }

    private static Gson create(boolean pretty) {
        Gson plain = base(false).create();
        GsonBuilder builder = base(pretty)
                .registerTypeAdapter(LeaseContract.class,
                        (JsonDeserializer<LeaseContract>) (json, type, context) ->
                                plain.fromJson(migrated(json.getAsJsonObject()), LeaseContract.class));
        return builder.create();
    }

    private static GsonBuilder base(boolean pretty) {
        GsonBuilder builder = new GsonBuilder()
                .registerTypeAdapter(Instant.class, stringAdapter(Instant::parse))
                .registerTypeAdapter(LocalDate.class, stringAdapter(LocalDate::parse))
                .registerTypeAdapter(MonthDay.class, stringAdapter(MonthDay::parse));
        return pretty ? builder.setPrettyPrinting() : builder;
    }

    /** Turns the single-parcel fields of older contract files into a one-element parcel list. */
    private static JsonObject migrated(JsonObject contract) {
        if (contract.has("parcels")) {
            return contract;
        }
        JsonObject parcel = new JsonObject();
        copy(contract, "parcelNumber", parcel, "number");
        copy(contract, "district", parcel, "district");
        copy(contract, "size", parcel, "size");
        copy(contract, "type", parcel, "type");
        copy(contract, "rent", parcel, "rent");
        if (contract.has("description")) {
            parcel.add("designation", contract.get("description"));
        }
        for (String key : LEGACY_PARCEL_KEYS) {
            contract.remove(key);
        }
        JsonArray parcels = new JsonArray();
        parcels.add(parcel);
        contract.add("parcels", parcels);
        return contract;
    }

    private static void copy(JsonObject from, String fromKey, JsonObject to, String toKey) {
        JsonElement value = from.get(fromKey);
        if (value != null && !value.isJsonNull()) {
            to.add(toKey, value);
        }
    }

    private static <T> Object stringAdapter(Function<String, T> parser) {
        return new StringAdapter<>(parser);
    }

    private record StringAdapter<T>(Function<String, T> parser) implements JsonSerializer<T>, JsonDeserializer<T> {
        @Override
        public JsonElement serialize(T src, java.lang.reflect.Type type,
                                     com.google.gson.JsonSerializationContext context) {
            return new JsonPrimitive(src.toString());
        }

        @Override
        public T deserialize(JsonElement json, java.lang.reflect.Type type,
                             com.google.gson.JsonDeserializationContext context) {
            return parser.apply(json.getAsString());
        }
    }
}
