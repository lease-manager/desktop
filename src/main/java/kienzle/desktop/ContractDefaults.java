package kienzle.desktop;

import java.time.MonthDay;

/** Default texts and the template for a new contract. */
public final class ContractDefaults {
    public static final String DURATION_INDEFINITE = "Unbestimmte Dauer";
    public static final String DURATION_FIXED = "Bestimmte Dauer";
    public static final String DEFAULT_NOTICE = "6 Monate";
    public static final MonthDay DEFAULT_GARDENING_YEAR_END = MonthDay.of(10, 31);

        public static final String PARAGRAPH_FOUR = "Sollte der/die Verpächter(in) die verpachteten Gründe ganz oder teilweise selbst benötigen oder an einen Dritten veräußern, der über die Pachtgründe anderweitig verfügen will, so ist der/die Verpächter(in) berechtigt, das Pachtverhältnis sofort zu lösen.\n"
            + "Eine sofortige Kündigung ist außerdem bei einem Verstoß gegen diesen Pachtvertrag möglich.";

    private ContractDefaults() {
    }

    public static LeaseContract newContract() {
        return LeaseContract.builder()
                .leaseDuration(DURATION_INDEFINITE)
                .terminationNotice(DEFAULT_NOTICE)
                .gardeningYearEnd(DEFAULT_GARDENING_YEAR_END)
                .paragraphFour(PARAGRAPH_FOUR)
                .paragraphFive(paragraphFive(DEFAULT_NOTICE, ContractFormats.monthDay(DEFAULT_GARDENING_YEAR_END)))
                .build();
    }

    public static String paragraphFive(String terminationNotice, String gardeningYearEnd) {
        return "1. Für den Flächeninhalt und den Ertrag des Grundstücks übernimmt der/die Verpächter(in) keine Gewähr.\n"
                + "2. Für die Erhaltung von Grenzanlagen und -zeichen hat der/die Pächter(in) zu sorgen. "
                + "Grunddienstbarkeiten und beschränkt persönliche Dienstbarkeiten, mit denen das verpachtete "
                + "Grundstück belastet ist, muss der/die Pächter(in) dulden.\n"
                + "3. Während der Pachtdauer ist das Grundstück ordentlich zu bewirtschaften. Schäden durch "
                + "Witterung, Naturereignisse oder Wild hat der/die Pächter(in) zu tragen und zu bezahlen. "
                + "Laufender Unterhalt und gewöhnliche Ausbesserungen, insbesondere an Wegen und Gräben, obliegen "
                + "dem/der Pächter(in).\n"
                + "4. Auf dem Grundstück vorhandene Bäume und Hecken müssen unbeschädigt erhalten bleiben.\n"
                + "5. Eine Änderung der Kulturart des Grundstücks ist ohne Genehmigung des/der Verpächter(s/in) "
                + "nicht gestattet.\n"
                + "6. Der/Die Pächter(in) darf die Nutzung des Grundstücks nur mit schriftlicher Erlaubnis des/der "
                + "Verpächter(s/in) einem Dritten überlassen oder unterverpachten.\n"
                + "7. Die auf dem Grundstück ruhenden Lasten trägt der/die Verpächter(in).\n"
                + "8. Verändern sich während der Pachtzeit die Lebenshaltungskosten, können die Parteien eine "
                + "Anpassung des Pachtzinses nach billigem Ermessen vereinbaren.\n"
                + "9. Eine Kündigung des Pachtvertrags muss " + terminationNotice
                + " vor Ende des Gartenjahres (" + gardeningYearEnd + ") erfolgen.";
    }
}
