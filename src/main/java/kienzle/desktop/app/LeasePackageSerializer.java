package kienzle.desktop.app;

import com.google.gson.Gson;
import kienzle.desktop.LeaseContract;

import java.time.LocalDate;
import java.time.MonthDay;

final class LeasePackageSerializer {
    private static final Gson GSON = new Gson();

    private LeasePackageSerializer() {
    }

    static String serialize(LeaseContract contract) {
        return String.join("\u0001",
                value(contract.name()),
                value(contract.lessor()),
                value(contract.lessorLocation()),
                value(contract.lessorDate()),
                value(contract.tenant()),
                value(contract.tenantLocation()),
                value(contract.tenantDate()),
                value(contract.parcelNumber()),
                value(contract.district()),
                value(contract.size()),
                value(contract.type()),
                value(contract.description()),
                value(contract.rent()),
                value(contract.leaseDuration()),
                value(contract.startDate()),
                value(contract.introductoryRent()),
                value(contract.hasIntroductoryRent()),
                value(contract.rentIncreaseDate()),
                value(contract.firstRentDate()),
                value(contract.rentRecipient()),
                value(contract.bankName()),
                value(contract.iban()),
                value(contract.bic()),
                value(contract.court()),
                value(contract.terminationNotice()),
                value(contract.gardeningYearEnd()),
                value(contract.paragraphFour()),
                value(contract.paragraphFive()),
                value(contract.lessorAddress()),
                value(contract.tenantAddress()),
                value(contract.leaseEndDate()),
                value(contract.introductoryRentFrom()),
                value(contract.introductoryRentUntil()),
                value(contract.rentDueInterval()),
                value(contract.rentDueDay()),
                value(contract.introductoryRentWords()),
                value(contract.rentWords()));
    }

    static LeaseContract deserialize(String raw) {
        String[] values = raw.split("\u0001", -1);
        if (values.length < 37) {
            throw new IllegalArgumentException("Invalid contract package payload");
        }
        return new LeaseContract(
                values[0],
                values[1],
                values[2],
                values[3].isBlank() ? null : LocalDate.parse(values[3]),
                values[4],
                values[5],
                values[6].isBlank() ? null : LocalDate.parse(values[6]),
                values[7],
                values[8],
                values[9].isBlank() ? null : new java.math.BigDecimal(values[9]),
                values[10],
                values[11],
                values[12].isBlank() ? null : new java.math.BigDecimal(values[12]),
                values[13],
                values[14].isBlank() ? null : LocalDate.parse(values[14]),
                values[15].isBlank() ? null : new java.math.BigDecimal(values[15]),
                Boolean.parseBoolean(values[16]),
                values[17].isBlank() ? null : LocalDate.parse(values[17]),
                values[18].isBlank() ? null : LocalDate.parse(values[18]),
                values[19],
                values[20],
                values[21],
                values[22],
                values[23],
                values[24],
                values[25].isBlank() ? null : MonthDay.parse(values[25]),
                values[26],
                values[27],
                values[28],
                values[29],
                values[30].isBlank() ? null : LocalDate.parse(values[30]),
                values[31].isBlank() ? null : LocalDate.parse(values[31]),
                values[32].isBlank() ? null : LocalDate.parse(values[32]),
                values[33],
                values[34],
                values[35],
                values[36]);
    }

    private static String value(Object value) {
        return value == null ? "" : value.toString();
    }
}
