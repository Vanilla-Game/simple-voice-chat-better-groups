package ru.vanillagame.voicechat.grouptools;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.translation.TranslationStore;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

final class PluginTranslations {

    static final Locale DEFAULT_LOCALE = Locale.US;
    static final Locale RUSSIAN_LOCALE = Locale.of("ru", "RU");
    static final String BUNDLE_NAME = "lang.Messages";

    private final TranslationStore.StringBased<MessageFormat> store =
            TranslationStore.messageFormat(Key.key("vanillagame", "voicechat_group_tools"));
    private boolean registered;

    void register(ClassLoader classLoader) {
        store.defaultLocale(DEFAULT_LOCALE);
        registerBundle(classLoader, DEFAULT_LOCALE);
        registerBundle(classLoader, RUSSIAN_LOCALE);

        registered = GlobalTranslator.translator().addSource(store);
        if (!registered) {
            throw new IllegalStateException("Could not register the Voice Chat Group Tools translation source");
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
