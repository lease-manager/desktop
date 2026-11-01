package kienzle.desktop.app;

import kienzle.desktop.LeaseContract;
import kienzle.desktop.Parcel;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LeasePackageSerializerTest {
    @Test
    void roundTripsSeveralParcels() {
        LeaseContract contract = LeaseContract.builder().name("A").parcels(List.of(
                new Parcel("1", "F", new BigDecimal("1"), "Acker", "N", new BigDecimal("2")),
                new Parcel("2", "F", null, "", "", new BigDecimal("3")))).build();

        assertEquals(contract, LeasePackageSerializer.deserialize(LeasePackageSerializer.serialize(contract)));
    }

    @Test
    void readsPackagesWrittenBeforeMultiParcelSupport() {
        String[] fields = new String[37];
        java.util.Arrays.fill(fields, "");
        fields[0] = "Alt";
        fields[7] = "12/3";
        fields[8] = "Senden";
        fields[9] = "4800";
        fields[10] = "Acker";
        fields[11] = "Nordfeld";
        fields[12] = "1500.00";
        fields[16] = "false";

        LeaseContract read = LeasePackageSerializer.deserialize(String.join("\u0001", fields));

        assertEquals("Alt", read.name());
        assertEquals(List.of(new Parcel("12/3", "Senden", new BigDecimal("4800"), "Acker", "Nordfeld",
                new BigDecimal("1500.00"))), read.parcels());
        assertEquals("Nordfeld", read.description());
    }

    @Test
    void rejectsMalformedPayload() {
        assertThrows(IllegalArgumentException.class, () -> LeasePackageSerializer.deserialize("{not json"));
        assertThrows(IllegalArgumentException.class, () -> LeasePackageSerializer.deserialize("a\u0001b"));
    }
}
