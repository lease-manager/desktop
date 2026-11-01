package kienzle.desktop.contract;

import kienzle.desktop.LeaseContract;
import kienzle.desktop.Parcel;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ContractJsonTest {
    @Test
    void multipleParcelsRoundTrip() {
        LeaseContract contract = LeaseContract.builder().name("A").parcels(List.of(
                new Parcel("1/2", "Flur", new BigDecimal("100.5"), "Acker", "Nord", new BigDecimal("10")),
                new Parcel("1/3", "Flur", null, "Wiese", "", new BigDecimal("5.5")))).build();

        LeaseContract read = ContractJson.compact().fromJson(ContractJson.compact().toJson(contract),
                LeaseContract.class);

        assertEquals(contract, read);
        assertEquals(0, new BigDecimal("15.5").compareTo(read.totalRent()));
    }

    @Test
    void contractFileWithSingleParcelFieldsIsMigrated() {
        String legacy = """
                {"name":"Alt","lessor":"L","parcelNumber":"12/3","district":"Senden","size":4800,
                 "type":"Acker","description":"Nordfeld","rent":1500.00,"leaseDuration":"x"}
                """;

        LeaseContract read = ContractJson.compact().fromJson(legacy, LeaseContract.class);

        assertEquals("Alt", read.name());
        assertEquals(1, read.parcels().size());
        Parcel parcel = read.parcels().get(0);
        assertEquals("12/3", parcel.number());
        assertEquals("Senden", parcel.district());
        assertEquals(0, new BigDecimal("4800").compareTo(parcel.size()));
        assertEquals("Acker", parcel.type());
        assertEquals("Nordfeld", parcel.designation());
        assertEquals("Nordfeld", read.description());
        assertEquals(0, new BigDecimal("1500").compareTo(read.totalRent()));
    }
}
