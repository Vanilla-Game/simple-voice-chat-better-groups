package ru.vanillagame.voicechat.groupmanagement;

import net.kyori.adventure.key.InvalidKeyException;
import net.kyori.adventure.key.Key;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Duration;

// requestSound is a validated sound event key, or null when disabled.
record PluginSettings(
        int inviteExpirationMinutes,
        int inviteCooldownSeconds,
        int requestExpirationMinutes,
        int requestCooldownSeconds,
        String requestSound,
        float requestSoundVolume,
        float requestSoundPitch
) {

    static final int DEFAULT_INVITE_EXPIRATION_MINUTES = 5;
    static final int DEFAULT_INVITE_COOLDOWN_SECONDS = 10;
    static final int DEFAULT_REQUEST_EXPIRATION_MINUTES = 5;
    static final int DEFAULT_REQUEST_COOLDOWN_SECONDS = 30;
    static final String DEFAULT_REQUEST_SOUND = "block.anvil.land";
    static final float DEFAULT_REQUEST_SOUND_VOLUME = 1.0F;
    static final float DEFAULT_REQUEST_SOUND_PITCH = 1.0F;

    private static final String INVITE_EXPIRATION_PATH = "invites.expiration-minutes";
    private static final String INVITE_COOLDOWN_PATH = "invites.cooldown-seconds";
    private static final String REQUEST_EXPIRATION_PATH = "requests.expiration-minutes";
    private static final String REQUEST_COOLDOWN_PATH = "requests.cooldown-seconds";
    private static final String REQUEST_SOUND_PATH = "requests.sound";
    private static final String REQUEST_SOUND_VOLUME_PATH = "requests.sound-volume";
    private static final String REQUEST_SOUND_PITCH_PATH = "requests.sound-pitch";

    PluginSettings {
        if (inviteExpirationMinutes <= 0) {
            throw new IllegalArgumentException("inviteExpirationMinutes must be positive");
        }
        if (inviteCooldownSeconds < 0) {
            throw new IllegalArgumentException("inviteCooldownSeconds must not be negative");
        }
        if (requestExpirationMinutes <= 0) {
            throw new IllegalArgumentException("requestExpirationMinutes must be positive");
        }
        if (requestCooldownSeconds < 0) {
            throw new IllegalArgumentException("requestCooldownSeconds must not be negative");
        }
    }

    static PluginSettings load(JavaPlugin plugin) {
        plugin.saveDefaultConfig();
        FileConfiguration config = plugin.getConfig();
        return new PluginSettings(
                readInt(plugin, config, INVITE_EXPIRATION_PATH, DEFAULT_INVITE_EXPIRATION_MINUTES, 1),
                readInt(plugin, config, INVITE_COOLDOWN_PATH, DEFAULT_INVITE_COOLDOWN_SECONDS, 0),
                readInt(plugin, config, REQUEST_EXPIRATION_PATH, DEFAULT_REQUEST_EXPIRATION_MINUTES, 1),
                readInt(plugin, config, REQUEST_COOLDOWN_PATH, DEFAULT_REQUEST_COOLDOWN_SECONDS, 0),
                readSound(plugin, config),
                readFloat(plugin, config, REQUEST_SOUND_VOLUME_PATH,
                        DEFAULT_REQUEST_SOUND_VOLUME, 0.0F, 10.0F),
                // Minecraft clamps pitch to [0.5, 2.0]; values outside it would be
                // silently distorted, so they are rejected up front.
                readFloat(plugin, config, REQUEST_SOUND_PITCH_PATH,
                        DEFAULT_REQUEST_SOUND_PITCH, 0.5F, 2.0F)
        );
    }

    Duration inviteExpiration() {
        return Duration.ofMinutes(inviteExpirationMinutes);
    }

    Duration inviteCooldown() {
        return Duration.ofSeconds(inviteCooldownSeconds);
    }

    Duration requestExpiration() {
        return Duration.ofMinutes(requestExpirationMinutes);
    }

    Duration requestCooldown() {
        return Duration.ofSeconds(requestCooldownSeconds);
    }

    private static String readSound(JavaPlugin plugin, FileConfiguration config) {
        String value = config.getString(REQUEST_SOUND_PATH, DEFAULT_REQUEST_SOUND);
        if (value == null || value.isBlank() || value.equalsIgnoreCase("none")) {
            return null;
        }
        try {
            Key.key(value);
            return value;
        } catch (InvalidKeyException invalidKey) {
            plugin.getLogger().warning(
                    "Invalid config value for '" + REQUEST_SOUND_PATH + "' (must be a sound event id like "
                            + DEFAULT_REQUEST_SOUND + ", or none); using " + DEFAULT_REQUEST_SOUND + ".");
            return DEFAULT_REQUEST_SOUND;
        }
    }

    private static float readFloat(
            JavaPlugin plugin,
            FileConfiguration config,
            String path,
            float defaultValue,
            float minimum,
            float maximum
    ) {
        Object raw = config.get(path);
        if (!(raw instanceof Number number)) {
            warn(plugin, path, defaultValue, "must be a number");
            return defaultValue;
        }
        float value = number.floatValue();
        if (value < minimum || value > maximum) {
            warn(plugin, path, defaultValue, "must be between " + minimum + " and " + maximum);
            return defaultValue;
        }
        return value;
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

    private static void warn(JavaPlugin plugin, String path, Object defaultValue, String reason) {
        plugin.getLogger().warning(
                "Invalid config value for '" + path + "' (" + reason + "); using " + defaultValue + "."
        );
    }
}
