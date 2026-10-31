package kienzle.desktop;

import java.util.Map;

/** Maps a {@link LeaseContract} to the text values consumed by {@link LeaseContractPdfPreview}. */
public final class ContractPdfValues {
    private ContractPdfValues() {
    }

    public static Map<String, String> from(LeaseContract c) {
        boolean intro = c.hasIntroductoryRent();
        return Map.ofEntries(
                e("lessor", c.lessor()),
                e("lessorAddress", c.lessorAddress()),
                e("tenant", c.tenant()),
                e("tenantAddress", c.tenantAddress()),
                e("parcelNumber", c.parcelNumber()),
                e("district", c.district()),
                e("size", ContractFormats.number(c.size())),
                e("type", c.type()),
                e("description", c.description()),
                e("rent", ContractFormats.money(c.rent())),
                e("totalRent", ContractFormats.money(c.rent())),
                e("leaseDuration", c.leaseDuration()),
                e("startDate", ContractFormats.date(c.startDate())),
                e("introductoryRentFrom", intro ? ContractFormats.date(c.introductoryRentFrom()) : ""),
                e("introductoryRentUntil", intro ? ContractFormats.date(c.introductoryRentUntil()) : ""),
                e("hasIntroductoryRent", Boolean.toString(intro)),
                e("introductoryRent", intro ? ContractFormats.money(c.introductoryRent()) : ""),
                e("introductoryRentWords", intro ? c.introductoryRentWords() : ""),
                e("rentWords", c.rentWords()),
                e("rentIncreaseDate", intro ? ContractFormats.date(c.rentIncreaseDate()) : ""),
                e("rentDueDay", c.rentDueDay()),
                e("rentDueInterval", c.rentDueInterval()),
                e("firstRentDate", ContractFormats.date(c.firstRentDate())),
                e("rentRecipient", c.rentRecipient()),
                e("bankName", c.bankName()),
                e("iban", c.iban()),
                e("bic", c.bic()),
                e("court", c.court()),
                e("terminationNotice", terminationMonths(c.terminationNotice())),
                e("paragraphFour", c.paragraphFour()),
                e("paragraphFive", c.paragraphFive()),
                e("gardeningYearEnd", ContractFormats.monthDay(c.gardeningYearEnd())),
                e("leaseEndDate", ContractFormats.date(c.leaseEndDate())),
                e("lessorSignatureDate",
                        signatureDate(c.lessorLocation(), ContractFormats.date(c.lessorDate()))),
                e("tenantSignatureDate",
                        signatureDate(c.tenantLocation(), ContractFormats.date(c.tenantDate())))
        );
    }

    private static Map.Entry<String, String> e(String key, String value) {
        return Map.entry(key, value == null ? "" : value);
    }

    static String signatureDate(String location, String date) {
        if (location == null || location.isBlank()) {
            return date;
        }
        return date.isBlank() ? location : location + ", den " + date;
    }

    static String terminationMonths(String notice) {
        if (notice == null) {
            return "";
        }
        String months = notice.replaceAll("(?i)\\s*Monate?", "").trim();
        return switch (months) {
            case "1" -> "einen";
            case "2" -> "zwei";
            case "3" -> "drei";
            case "4" -> "vier";
            case "5" -> "fünf";
            case "6" -> "sechs";
            case "7" -> "sieben";
            case "8" -> "acht";
            case "9" -> "neun";
            case "10" -> "zehn";
            case "11" -> "elf";
            case "12" -> "zwölf";
            default -> notice;
        };
    }
}
