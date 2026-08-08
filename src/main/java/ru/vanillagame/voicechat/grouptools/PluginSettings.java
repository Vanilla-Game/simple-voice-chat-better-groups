package ru.vanillagame.voicechat.grouptools;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Duration;

record PluginSettings(int inviteExpirationMinutes, int inviteCooldownSeconds) {

    static final int DEFAULT_INVITE_EXPIRATION_MINUTES = 5;
    static final int DEFAULT_INVITE_COOLDOWN_SECONDS = 10;

    private static final String EXPIRATION_PATH = "invites.expiration-minutes";
    private static final String COOLDOWN_PATH = "invites.cooldown-seconds";

    PluginSettings {
        if (inviteExpirationMinutes <= 0) {
            throw new IllegalArgumentException("inviteExpirationMinutes must be positive");
        }
        if (inviteCooldownSeconds < 0) {
            throw new IllegalArgumentException("inviteCooldownSeconds must not be negative");
        }
    }

    static PluginSettings load(JavaPlugin plugin) {
        plugin.saveDefaultConfig();
        FileConfiguration config = plugin.getConfig();
        int expirationMinutes = readInt(
                plugin,
                config,
                EXPIRATION_PATH,
                DEFAULT_INVITE_EXPIRATION_MINUTES,
                1
        );
        int cooldownSeconds = readInt(
                plugin,
                config,
                COOLDOWN_PATH,
                DEFAULT_INVITE_COOLDOWN_SECONDS,
                0
        );
        return new PluginSettings(expirationMinutes, cooldownSeconds);
    }

    Duration inviteExpiration() {
        return Duration.ofMinutes(inviteExpirationMinutes);
    }

    Duration inviteCooldown() {
        return Duration.ofSeconds(inviteCooldownSeconds);
    }

    private static int readInt(
            JavaPlugin plugin,
            FileConfiguration config,
            String path,
            int defaultValue,
            int minimum
    ) {
        if (!config.isInt(path)) {
            warn(plugin, path, defaultValue, "must be an integer");
            return defaultValue;
        }

        int value = config.getInt(path);
        if (value < minimum) {
            warn(plugin, path, defaultValue, "must be at least " + minimum);
            return defaultValue;
        }
        return value;
    }

    private static void warn(JavaPlugin plugin, String path, int defaultValue, String reason) {
        plugin.getLogger().warning(
                "Invalid config value for '" + path + "' (" + reason + "); using " + defaultValue + "."
        );
    }
}
