package ru.vanillagame.voicechat.groupmanagement;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PluginSettingsTest {

    @Test
    void loadsConfiguredInviteDurations() {
        JavaPlugin plugin = mock(JavaPlugin.class);
        YamlConfiguration config = new YamlConfiguration();
        config.set("invites.expiration-minutes", 12);
        config.set("invites.cooldown-seconds", 3);
        config.set("requests.expiration-minutes", 7);
        config.set("requests.cooldown-seconds", 45);
        config.set("requests.sound", "entity.experience_orb.pickup");
        config.set("requests.sound-volume", 0.5);
        config.set("requests.sound-pitch", 1.5);
        when(plugin.getConfig()).thenReturn(config);

        PluginSettings settings = PluginSettings.load(plugin);

        verify(plugin).saveDefaultConfig();
        assertEquals(Duration.ofMinutes(12), settings.inviteExpiration());
        assertEquals(Duration.ofSeconds(3), settings.inviteCooldown());
        assertEquals(Duration.ofMinutes(7), settings.requestExpiration());
        assertEquals(Duration.ofSeconds(45), settings.requestCooldown());
        assertEquals("entity.experience_orb.pickup", settings.requestSound());
        assertEquals(0.5F, settings.requestSoundVolume());
        assertEquals(1.5F, settings.requestSoundPitch());
    }

    @Test
    void invalidValuesFallBackToSafeDefaults() {
        JavaPlugin plugin = mock(JavaPlugin.class);
        YamlConfiguration config = new YamlConfiguration();
        config.set("invites.expiration-minutes", 0);
        config.set("invites.cooldown-seconds", -1);
        config.set("requests.expiration-minutes", 0);
        config.set("requests.cooldown-seconds", -1);
        config.set("requests.sound", "NOT A VALID KEY");
        config.set("requests.sound-volume", -1.0);
        config.set("requests.sound-pitch", 5.0);
        when(plugin.getConfig()).thenReturn(config);
        when(plugin.getLogger()).thenReturn(Logger.getAnonymousLogger());

        PluginSettings settings = PluginSettings.load(plugin);

        assertEquals(
                Duration.ofMinutes(PluginSettings.DEFAULT_INVITE_EXPIRATION_MINUTES),
                settings.inviteExpiration()
        );
        assertEquals(
                Duration.ofSeconds(PluginSettings.DEFAULT_INVITE_COOLDOWN_SECONDS),
                settings.inviteCooldown()
        );
        assertEquals(
                Duration.ofMinutes(PluginSettings.DEFAULT_REQUEST_EXPIRATION_MINUTES),
                settings.requestExpiration()
        );
        assertEquals(
                Duration.ofSeconds(PluginSettings.DEFAULT_REQUEST_COOLDOWN_SECONDS),
                settings.requestCooldown()
        );
        assertEquals(PluginSettings.DEFAULT_REQUEST_SOUND, settings.requestSound());
        assertEquals(PluginSettings.DEFAULT_REQUEST_SOUND_VOLUME, settings.requestSoundVolume());
        assertEquals(PluginSettings.DEFAULT_REQUEST_SOUND_PITCH, settings.requestSoundPitch());
    }

    @Test
    void noneDisablesTheRequestSound() {
        JavaPlugin plugin = mock(JavaPlugin.class);
        YamlConfiguration config = new YamlConfiguration();
        config.set("requests.sound", "none");
        when(plugin.getConfig()).thenReturn(config);
        when(plugin.getLogger()).thenReturn(Logger.getAnonymousLogger());

        assertNull(PluginSettings.load(plugin).requestSound());
    }
}
