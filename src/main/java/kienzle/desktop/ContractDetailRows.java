package kienzle.desktop;

import java.util.List;

/** Label/value rows describing a contract in the detail table. */
public final class ContractDetailRows {
    private ContractDetailRows() {
    }

    public static List<ContractDetailRow> of(LeaseContract contract) {
        return List.of(
                new ContractDetailRow(I18n.get("detail.name"), contract.name()),
                new ContractDetailRow(I18n.get("lessor"), contract.lessor()),
                new ContractDetailRow(I18n.get("detail.lessor.address"), contract.lessorAddress()),
                new ContractDetailRow(I18n.get("detail.lessor.placeDate"), contract.lessorLocation() + " / " + ContractFormats.date(contract.lessorDate())),
                new ContractDetailRow(I18n.get("detail.tenant"), contract.tenant()),
                new ContractDetailRow(I18n.get("detail.tenant.address"), contract.tenantAddress()),
                new ContractDetailRow(I18n.get("detail.tenant.placeDate"), contract.tenantLocation() + " / " + ContractFormats.date(contract.tenantDate())),
                new ContractDetailRow(I18n.get("parcel"), contract.parcelNumber()),
                new ContractDetailRow(I18n.get("district"), contract.district()),
                new ContractDetailRow(I18n.get("detail.size"), ContractFormats.number(contract.size())),
                new ContractDetailRow(I18n.get("detail.land.use"), contract.type()),
                new ContractDetailRow(I18n.get("rent.regular.eur"), ContractFormats.moneyEuro(contract.rent())),
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
        );
    }
}
