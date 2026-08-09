package ru.vanillagame.voicechat.bettergroups;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.translation.GlobalTranslator;
import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PluginTranslationsTest {

    @Test
    void everySupportedBundleContainsExactlyTheDeclaredKeys() {
        Set<String> englishKeys = bundle(PluginTranslations.DEFAULT_LOCALE).keySet();
        Set<String> russianKeys = bundle(PluginTranslations.RUSSIAN_LOCALE).keySet();

        assertEquals(Messages.keys(), englishKeys);
        assertEquals(Messages.keys(), russianKeys);
    }

    @Test
    void translatorUsesPlayerLocaleAndEnglishFallback() {
        PluginTranslations translations = new PluginTranslations();
        translations.register(getClass().getClassLoader());
        try {
            Component message = Messages.component(
                    Messages.INVITE_SENT,
                    NamedTextColor.GREEN,
                    Component.text("Alex")
            );

            assertEquals("Invite sent to Alex.", render(message, Locale.US));
            assertEquals("Приглашение отправлено игроку Alex.", render(message, PluginTranslations.RUSSIAN_LOCALE));
            assertEquals("Invite sent to Alex.", render(message, Locale.GERMANY));
        } finally {
            translations.unregister();
        }
    }

    @Test
    void inviteLifetimeMessageUsesConfiguredValue() {
        PluginTranslations translations = new PluginTranslations();
        translations.register(getClass().getClassLoader());
        try {
            Component message = Messages.component(
                    Messages.INVITE_EXPIRES,
                    NamedTextColor.GRAY,
                    Component.text(12)
            );

            assertEquals("This invite expires after 12 min.", render(message, Locale.US));
            assertEquals(
                    "Приглашение истечёт через 12 мин.",
                    render(message, PluginTranslations.RUSSIAN_LOCALE)
            );
        } finally {
            translations.unregister();
        }
    }

    private static ResourceBundle bundle(Locale locale) {
        return ResourceBundle.getBundle(
                PluginTranslations.BUNDLE_NAME,
                locale,
                PluginTranslationsTest.class.getClassLoader(),
                ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_PROPERTIES)
        );
    }

    private static String render(Component component, Locale locale) {
        Component rendered = GlobalTranslator.render(component, locale);
        return PlainTextComponentSerializer.plainText().serialize(rendered);
    }
}
