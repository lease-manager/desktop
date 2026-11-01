package kienzle.desktop;

import java.util.ArrayList;
import java.util.List;

/** Label/value rows describing a contract in the detail table. */
public final class ContractDetailRows {
    private ContractDetailRows() {
    }

    public static List<ContractDetailRow> of(LeaseContract contract) {
        List<ContractDetailRow> rows = new ArrayList<>(List.of(
                new ContractDetailRow(I18n.get("detail.name"), contract.name()),
                new ContractDetailRow(I18n.get("lessor"), contract.lessor()),
                new ContractDetailRow(I18n.get("detail.lessor.address"), contract.lessorAddress()),
                new ContractDetailRow(I18n.get("detail.lessor.placeDate"), contract.lessorLocation() + " / " + ContractFormats.date(contract.lessorDate())),
                new ContractDetailRow(I18n.get("detail.tenant"), contract.tenant()),
                new ContractDetailRow(I18n.get("detail.tenant.address"), contract.tenantAddress()),
                new ContractDetailRow(I18n.get("detail.tenant.placeDate"), contract.tenantLocation() + " / " + ContractFormats.date(contract.tenantDate())),
                new ContractDetailRow(I18n.get("rent.regular.eur"), ContractFormats.moneyEuro(contract.totalRent())),
                new ContractDetailRow(I18n.get("detail.lease"), contract.leaseDuration()),
                new ContractDetailRow(I18n.get("detail.lease.start"), ContractFormats.date(contract.startDate())),
                new ContractDetailRow(I18n.get("detail.lease.end"), ContractFormats.date(contract.leaseEndDate())),
                new ContractDetailRow(I18n.get("detail.intro.exists"), contract.hasIntroductoryRent() ? "Ja" : "Nein"),
                new ContractDetailRow(I18n.get("detail.intro.eur"), ContractFormats.moneyEuro(contract.introductoryRent())),
                new ContractDetailRow(I18n.get("detail.intro.from"), ContractFormats.date(contract.introductoryRentFrom())),
                new ContractDetailRow(I18n.get("detail.intro.until"), ContractFormats.date(contract.introductoryRentUntil())),
                new ContractDetailRow(I18n.get("detail.intro.words"), contract.introductoryRentWords()),
                new ContractDetailRow(I18n.get("rent.regular.from"), ContractFormats.date(contract.rentIncreaseDate())),
                new ContractDetailRow(I18n.get("rent.regular.words"), contract.rentWords()),
                new ContractDetailRow(I18n.get("detail.rent.due.interval"), contract.rentDueInterval()),
                new ContractDetailRow(I18n.get("detail.rent.due.day"), contract.rentDueDay()),
                new ContractDetailRow(I18n.get("rent.first.payment"), ContractFormats.date(contract.firstRentDate())),
                new ContractDetailRow(I18n.get("rent.recipient"), contract.rentRecipient()),
                new ContractDetailRow(I18n.get("bank"), contract.bankName()),
                new ContractDetailRow(I18n.get("iban"), contract.iban()),
                new ContractDetailRow(I18n.get("bic"), contract.bic()),
                new ContractDetailRow(I18n.get("court"), contract.court()),
                new ContractDetailRow(I18n.get("notice"), contract.terminationNotice()),
                new ContractDetailRow(I18n.get("detail.gardening.end"), ContractFormats.monthDay(contract.gardeningYearEnd())),
                new ContractDetailRow("§ 5", contract.paragraphFour()),
                new ContractDetailRow("§ 6", contract.paragraphFive()),
                new ContractDetailRow(I18n.get("detail.description"), contract.description())
        ));
        rows.addAll(parcelRows(contract.parcels()));
        return rows;
    }

    private static List<ContractDetailRow> parcelRows(List<Parcel> parcels) {
        List<ContractDetailRow> rows = new ArrayList<>();
        for (int index = 0; index < parcels.size(); index++) {
            Parcel parcel = parcels.get(index);
            String label = I18n.get("parcel") + " " + (index + 1);
            String value = String.join(", ", java.util.stream.Stream.of(
                            parcel.number(),
                            parcel.district(),
                            parcel.size() == null ? "" : ContractFormats.number(parcel.size()) + " m²",
                            parcel.type(),
                            parcel.designation(),
                            parcel.rent() == null ? "" : ContractFormats.moneyEuro(parcel.rent()))
                    .filter(part -> !part.isBlank()).toList());
            rows.add(new ContractDetailRow(label, value));
        }
        return rows;
    }
}
