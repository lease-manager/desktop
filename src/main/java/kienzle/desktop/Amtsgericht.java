package kienzle.desktop;

public record Amtsgericht(
        String name,
        String plz,
        String ort,
        String strasse,
        String hausnummer,
        String postfach
) {
    public String anschrift() {
        String zustelladresse = postfach == null || postfach.isBlank()
                ? (strasse + (hausnummer == null || hausnummer.isBlank() ? "" : " " + hausnummer)).trim()
                : "Postfach " + postfach;
        return zustelladresse + ", " + plz + " " + ort;
    }

    @Override
    public String toString() {
        return name + " - " + plz + " " + ort;
    }
}