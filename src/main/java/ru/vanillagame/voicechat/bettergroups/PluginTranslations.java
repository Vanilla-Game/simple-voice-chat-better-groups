package ru.vanillagame.voicechat.bettergroups;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.translation.TranslationStore;

import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

final class PluginTranslations {

    static final Locale DEFAULT_LOCALE = Locale.US;
    static final Locale RUSSIAN_LOCALE = Locale.of("ru", "RU");
    static final List<Locale> SUPPORTED_LOCALES = List.of(
            DEFAULT_LOCALE,
            Locale.of("ca", "ES"),
            Locale.of("cs", "CZ"),
            Locale.GERMANY,
            Locale.of("es", "ES"),
            Locale.FRANCE,
            Locale.ITALY,
            Locale.of("nl", "NL"),
            Locale.of("no", "NO"),
            Locale.of("pl", "PL"),
            Locale.of("pt", "PT"),
            RUSSIAN_LOCALE,
            Locale.of("sv", "SE"),
            Locale.of("tr", "TR"),
            Locale.of("uk", "UA")
    );
    static final String BUNDLE_NAME = "lang.Messages";

    private final TranslationStore.StringBased<MessageFormat> store =
            TranslationStore.messageFormat(Key.key("vanillagame", "svc_better_groups"));
    private boolean registered;

    void register(ClassLoader classLoader) {
        store.defaultLocale(DEFAULT_LOCALE);
        SUPPORTED_LOCALES.forEach(locale -> registerBundle(classLoader, locale));

        registered = GlobalTranslator.translator().addSource(store);
        if (!registered) {
            throw new IllegalStateException("Could not register the Simple Voice Chat Group Management translation source");
        }
    }

    void unregister() {
        if (registered) {
            GlobalTranslator.translator().removeSource(store);
            registered = false;
        }
    }

    private void registerBundle(ClassLoader classLoader, Locale locale) {
        ResourceBundle bundle = ResourceBundle.getBundle(BUNDLE_NAME, locale, classLoader);
        store.registerAll(locale, bundle, true);
    }
}
