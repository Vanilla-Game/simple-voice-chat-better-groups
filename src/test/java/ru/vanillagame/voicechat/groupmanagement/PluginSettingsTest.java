package ru.vanillagame.voicechat.groupmanagement;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        when(plugin.getConfig()).thenReturn(config);

        PluginSettings settings = PluginSettings.load(plugin);

        verify(plugin).saveDefaultConfig();
        assertEquals(Duration.ofMinutes(12), settings.inviteExpiration());
        assertEquals(Duration.ofSeconds(3), settings.inviteCooldown());
    }

    @Test
    void invalidValuesFallBackToSafeDefaults() {
        JavaPlugin plugin = mock(JavaPlugin.class);
        YamlConfiguration config = new YamlConfiguration();
        config.set("invites.expiration-minutes", 0);
        config.set("invites.cooldown-seconds", -1);
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
    }
}
