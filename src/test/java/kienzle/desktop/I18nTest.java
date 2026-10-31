package kienzle.desktop;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class I18nTest {

    @AfterEach
    void resetLocale() {
        I18n.setLocale(Locale.GERMAN);
    }

    @Test
    void bundles_shouldContainSameKeys() {
        Set<String> german = new HashSet<>(ResourceBundle.getBundle(I18n.BUNDLE_NAME, Locale.GERMAN).keySet());
        Set<String> english = new HashSet<>(ResourceBundle.getBundle(I18n.BUNDLE_NAME, Locale.ENGLISH).keySet());
        assertEquals(german, english);
    }

    @Test
    void fxml_shouldOnlyReferenceExistingKeys() throws IOException {
        String fxml = new String(I18nTest.class.getResourceAsStream("/kienzle/desktop/ContractView.fxml")
                .readAllBytes(), StandardCharsets.UTF_8);
        Matcher matcher = Pattern.compile("=\"%([\\w.]+)\"").matcher(fxml);
        Set<String> keys = ResourceBundle.getBundle(I18n.BUNDLE_NAME, Locale.GERMAN).keySet();
        int found = 0;
        while (matcher.find()) {
            found++;
            assertTrue(keys.contains(matcher.group(1)), matcher.group(1));
        }
        assertTrue(found > 0);
    }

    @Test
    void language_shouldBePersistedInConfig(@TempDir Path dir) throws IOException {
        Path config = dir.resolve("sub").resolve("config.properties");
        I18n.setLocale(Locale.ENGLISH);
        I18n.saveToConfig(config);
        I18n.setLocale(Locale.GERMAN);
        I18n.loadFromConfig(config);
        assertEquals("en", I18n.locale().getLanguage());
        assertEquals("Lease contracts", I18n.get("contract.list"));
    }
}
