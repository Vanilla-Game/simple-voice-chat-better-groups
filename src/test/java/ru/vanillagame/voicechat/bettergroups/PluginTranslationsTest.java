package ru.vanillagame.voicechat.bettergroups;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.translation.GlobalTranslator;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class PluginTranslationsTest {

    private static final Pattern MESSAGE_FORMAT_PLACEHOLDER = Pattern.compile("\\{\\d+}");
    private static final Pattern CLIENT_ENTRY = Pattern.compile("\\s*\\\"([^\\\"]+)\\\"\\s*:\\s*\\\"(.*)\\\"[,]?");
    private static final Pattern CLIENT_PLACEHOLDER = Pattern.compile("%s");

    @Test
    void everySupportedBundleContainsExactlyTheDeclaredKeys() {
        Set<String> englishKeys = bundle(PluginTranslations.DEFAULT_LOCALE).keySet();
        assertEquals(Messages.keys(), englishKeys);

        for (Locale locale : PluginTranslations.SUPPORTED_LOCALES) {
            ResourceBundle localized = bundle(locale);
            assertEquals(englishKeys, localized.keySet(), locale.toString());
            for (String key : englishKeys) {
                assertEquals(
                        placeholders(bundle(PluginTranslations.DEFAULT_LOCALE).getString(key), MESSAGE_FORMAT_PLACEHOLDER),
                        placeholders(localized.getString(key), MESSAGE_FORMAT_PLACEHOLDER),
                        locale + ": " + key
                );
            }
        }
    }

    @Test
    void everyClientLocaleContainsTheEnglishKeysAndPlaceholders() throws IOException {
        Path languageDirectory = Path.of("client-fabric/src/main/resources/assets/svc_better_groups_client/lang");
        Map<String, String> english = clientEntries(languageDirectory.resolve("en_us.json"));
        assertFalse(english.isEmpty());

        for (Locale locale : PluginTranslations.SUPPORTED_LOCALES) {
            Path localizedPath = languageDirectory.resolve(locale.toString().toLowerCase(Locale.ROOT) + ".json");
            Map<String, String> localized = clientEntries(localizedPath);
            assertEquals(english.keySet(), localized.keySet(), localizedPath.toString());
            for (String key : english.keySet()) {
                assertEquals(
                        placeholders(english.get(key), CLIENT_PLACEHOLDER),
                        placeholders(localized.get(key), CLIENT_PLACEHOLDER),
                        localizedPath + ": " + key
                );
            }
        }
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
            assertEquals("Einladung an Alex gesendet.", render(message, Locale.GERMANY));
            assertEquals("Invite sent to Alex.", render(message, Locale.JAPAN));
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

    private static List<String> placeholders(String value, Pattern pattern) {
        return pattern.matcher(value).results().map(result -> result.group()).sorted().toList();
    }

    private static Map<String, String> clientEntries(Path path) throws IOException {
        Map<String, String> entries = new HashMap<>();
        for (String line : Files.readAllLines(path)) {
            Matcher matcher = CLIENT_ENTRY.matcher(line);
            if (matcher.matches()) {
                entries.put(matcher.group(1), matcher.group(2));
            }
        }
        return entries;
    }
}
