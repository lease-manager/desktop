package kienzle.desktop;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.MonthDay;
import java.util.List;

public record LeaseContract(
        String name,
        String lessor,
        String lessorLocation,
        LocalDate lessorDate,
        String tenant,
        String tenantLocation,
        LocalDate tenantDate,
        List<Parcel> parcels,
        String description,
        String leaseDuration,
        LocalDate startDate,
        BigDecimal introductoryRent,
        boolean hasIntroductoryRent,
        LocalDate rentIncreaseDate,
        LocalDate firstRentDate,
        String rentRecipient,
        String bankName,
        String iban,
        String bic,
        String court,
        String terminationNotice,
        MonthDay gardeningYearEnd,
        String paragraphFour,
        String paragraphFive,
        String lessorAddress,
        String tenantAddress,
        LocalDate leaseEndDate,
        LocalDate introductoryRentFrom,
        LocalDate introductoryRentUntil,
        String rentDueInterval,
        String rentDueDay,
        String introductoryRentWords,
        String rentWords
) {

    public LeaseContract {
        parcels = parcels == null ? List.of() : List.copyOf(parcels);
    }

    /** Sum of the rents of all parcels; {@code null} if no parcel has a rent. */
    public BigDecimal totalRent() {
        return parcels.stream().map(Parcel::rent).filter(java.util.Objects::nonNull)
                .reduce(BigDecimal::add).orElse(null);
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        Builder builder = new Builder();
        builder.name = name;
        builder.lessor = lessor;
        builder.lessorLocation = lessorLocation;
        builder.lessorDate = lessorDate;
        builder.tenant = tenant;
        builder.tenantLocation = tenantLocation;
        builder.tenantDate = tenantDate;
        builder.parcels = parcels;
        builder.description = description;
        builder.leaseDuration = leaseDuration;
        builder.startDate = startDate;
        builder.introductoryRent = introductoryRent;
        builder.hasIntroductoryRent = hasIntroductoryRent;
        builder.rentIncreaseDate = rentIncreaseDate;
        builder.firstRentDate = firstRentDate;
        builder.rentRecipient = rentRecipient;
        builder.bankName = bankName;
        builder.iban = iban;
        builder.bic = bic;
        builder.court = court;
        builder.terminationNotice = terminationNotice;
        builder.gardeningYearEnd = gardeningYearEnd;
        builder.paragraphFour = paragraphFour;
        builder.paragraphFive = paragraphFive;
        builder.lessorAddress = lessorAddress;
        builder.tenantAddress = tenantAddress;
        builder.leaseEndDate = leaseEndDate;
        builder.introductoryRentFrom = introductoryRentFrom;
        builder.introductoryRentUntil = introductoryRentUntil;
        builder.rentDueInterval = rentDueInterval;
        builder.rentDueDay = rentDueDay;
        builder.introductoryRentWords = introductoryRentWords;
        builder.rentWords = rentWords;
        return builder;
    }

    /** Fluent builder; text fields default to the empty string, all other fields to {@code null}/{@code false}. */
    public static final class Builder {
        private String name = "";
        private String lessor = "";
        private String lessorLocation = "";
        private LocalDate lessorDate;
        private String tenant = "";
        private String tenantLocation = "";
        private LocalDate tenantDate;
        private List<Parcel> parcels = List.of();
        private String description = "";
        private String leaseDuration = "";
        private LocalDate startDate;
        private BigDecimal introductoryRent;
        private boolean hasIntroductoryRent;
        private LocalDate rentIncreaseDate;
        private LocalDate firstRentDate;
        private String rentRecipient = "";
        private String bankName = "";
        private String iban = "";
        private String bic = "";
        private String court = "";
        private String terminationNotice = "";
        private MonthDay gardeningYearEnd;
        private String paragraphFour = "";
        private String paragraphFive = "";
        private String lessorAddress = "";
        private String tenantAddress = "";
        private LocalDate leaseEndDate;
        private LocalDate introductoryRentFrom;
        private LocalDate introductoryRentUntil;
        private String rentDueInterval = "";
        private String rentDueDay = "";
        private String introductoryRentWords = "";
        private String rentWords = "";

        public Builder name(String value) {
            this.name = value;
            return this;
        }

        public Builder lessor(String value) {
            this.lessor = value;
            return this;
        }

        public Builder lessorLocation(String value) {
            this.lessorLocation = value;
            return this;
        }

        public Builder lessorDate(LocalDate value) {
            this.lessorDate = value;
            return this;
        }

        public Builder tenant(String value) {
            this.tenant = value;
            return this;
        }

        public Builder tenantLocation(String value) {
            this.tenantLocation = value;
            return this;
        }

        public Builder tenantDate(LocalDate value) {
            this.tenantDate = value;
            return this;
        }

        public Builder parcels(List<Parcel> value) {
            this.parcels = value;
            return this;
        }

        public Builder description(String value) {
            this.description = value;
            return this;
        }

        public Builder leaseDuration(String value) {
            this.leaseDuration = value;
            return this;
        }

        public Builder startDate(LocalDate value) {
            this.startDate = value;
            return this;
        }

        public Builder introductoryRent(BigDecimal value) {
            this.introductoryRent = value;
            return this;
        }

        public Builder hasIntroductoryRent(boolean value) {
            this.hasIntroductoryRent = value;
            return this;
        }

        public Builder rentIncreaseDate(LocalDate value) {
            this.rentIncreaseDate = value;
            return this;
        }

        public Builder firstRentDate(LocalDate value) {
            this.firstRentDate = value;
            return this;
        }

        public Builder rentRecipient(String value) {
            this.rentRecipient = value;
            return this;
        }

        public Builder bankName(String value) {
            this.bankName = value;
            return this;
        }

        public Builder iban(String value) {
            this.iban = value;
            return this;
        }

        public Builder bic(String value) {
            this.bic = value;
            return this;
        }

        public Builder court(String value) {
            this.court = value;
            return this;
        }

        public Builder terminationNotice(String value) {
            this.terminationNotice = value;
            return this;
        }

        public Builder gardeningYearEnd(MonthDay value) {
            this.gardeningYearEnd = value;
            return this;
        }

        public Builder paragraphFour(String value) {
            this.paragraphFour = value;
            return this;
        }

        public Builder paragraphFive(String value) {
            this.paragraphFive = value;
            return this;
        }

        public Builder lessorAddress(String value) {
            this.lessorAddress = value;
            return this;
        }

        public Builder tenantAddress(String value) {
            this.tenantAddress = value;
            return this;
        }

        public Builder leaseEndDate(LocalDate value) {
            this.leaseEndDate = value;
            return this;
        }

        public Builder introductoryRentFrom(LocalDate value) {
            this.introductoryRentFrom = value;
            return this;
        }

        public Builder introductoryRentUntil(LocalDate value) {
            this.introductoryRentUntil = value;
            return this;
        }

        public Builder rentDueInterval(String value) {
            this.rentDueInterval = value;
            return this;
        }

        public Builder rentDueDay(String value) {
            this.rentDueDay = value;
            return this;
        }

        public Builder introductoryRentWords(String value) {
            this.introductoryRentWords = value;
            return this;
        }

        public Builder rentWords(String value) {
            this.rentWords = value;
            return this;
        }

        public LeaseContract build() {
            return new LeaseContract(
                name,
                lessor,
                lessorLocation,
                lessorDate,
                tenant,
                tenantLocation,
                tenantDate,
                parcels,
                description,
                leaseDuration,
                startDate,
                introductoryRent,
                hasIntroductoryRent,
                rentIncreaseDate,
                firstRentDate,
                rentRecipient,
                bankName,
                iban,
                bic,
                court,
                terminationNotice,
                gardeningYearEnd,
                paragraphFour,
                paragraphFive,
                lessorAddress,
                tenantAddress,
                leaseEndDate,
                introductoryRentFrom,
                introductoryRentUntil,
                rentDueInterval,
                rentDueDay,
                introductoryRentWords,
                rentWords);
        }
    }
}
