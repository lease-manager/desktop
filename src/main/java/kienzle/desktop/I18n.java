package kienzle.desktop;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;

/**
 * Holds the current UI language. The language is persisted in a config file
 * ({@code ~/.lease-manager/config.properties}, key {@code language=de|en}).
 */
public final class I18n {
    static final String BUNDLE_NAME = "kienzle.desktop.messages";
    static final String LANGUAGE_KEY = "language";
    private static final Locale DEFAULT_LOCALE = Locale.GERMAN;

    private static Locale locale = DEFAULT_LOCALE;
    private static ResourceBundle bundle = loadBundle(locale);

    private I18n() {
    }

    public static Path defaultConfigPath() {
        return Path.of(System.getProperty("user.home"), ".lease-manager", "config.properties");
    }

    public static Locale locale() {
        return locale;
    }

    public static ResourceBundle bundle() {
        return bundle;
    }

    public static String get(String key) {
        return bundle.getString(key);
    }

    public static void setLocale(Locale newLocale) {
        locale = supported(newLocale);
        bundle = loadBundle(locale);
    }

    public static void loadFromConfig(Path configFile) {
        Properties properties = new Properties();
        if (Files.isRegularFile(configFile)) {
            try (InputStream in = Files.newInputStream(configFile)) {
                properties.load(in);
            } catch (IOException ignored) {
                // Fall back to the default language.
            }
        }
        setLocale(Locale.forLanguageTag(properties.getProperty(LANGUAGE_KEY, DEFAULT_LOCALE.getLanguage())));
    }

    public static void saveToConfig(Path configFile) throws IOException {
        Properties properties = new Properties();
        if (Files.isRegularFile(configFile)) {
            try (InputStream in = Files.newInputStream(configFile)) {
                properties.load(in);
            }
        }
        properties.setProperty(LANGUAGE_KEY, locale.getLanguage());
        Files.createDirectories(configFile.getParent());
        try (OutputStream out = Files.newOutputStream(configFile)) {
            properties.store(out, "Lease Manager configuration");
        }
    }

    private static Locale supported(Locale requested) {
        return requested != null && Locale.ENGLISH.getLanguage().equals(requested.getLanguage())
                ? Locale.ENGLISH
                : DEFAULT_LOCALE;
    }

    private static ResourceBundle loadBundle(Locale target) {
        return ResourceBundle.getBundle(BUNDLE_NAME, target);
    }
}
