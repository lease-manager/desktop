package kienzle.desktop;

import java.math.BigDecimal;

/** One leased plot of land; a contract lists one or more of them in its § 1 table. */
public record Parcel(
        String number,
        String district,
        BigDecimal size,
        String type,
        String designation,
        BigDecimal rent
) {
    public Parcel {
        number = number == null ? "" : number;
        district = district == null ? "" : district;
        type = type == null ? "" : type;
        designation = designation == null ? "" : designation;
    }

    public static Parcel empty() {
        return new Parcel("", "", null, "", "", null);
    }
}
