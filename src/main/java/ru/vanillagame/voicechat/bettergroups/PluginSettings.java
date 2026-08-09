package ru.vanillagame.voicechat.bettergroups;

import net.kyori.adventure.key.InvalidKeyException;
import net.kyori.adventure.key.Key;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Duration;

// Sound values are validated sound event keys, or null when disabled.
record PluginSettings(
        int inviteExpirationMinutes,
        int inviteCooldownSeconds,
        String inviteSound,
        float inviteSoundVolume,
        float inviteSoundPitch,
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
    static final String DEFAULT_SOUND = "block.anvil.land";
    static final float DEFAULT_SOUND_VOLUME = 0.5F;
    static final float DEFAULT_SOUND_PITCH = 2.0F;

    private static final String INVITE_EXPIRATION_PATH = "invites.expiration-minutes";
    private static final String INVITE_COOLDOWN_PATH = "invites.cooldown-seconds";
    private static final String INVITE_SOUND_PATH = "invites.sound";
    private static final String REQUEST_EXPIRATION_PATH = "requests.expiration-minutes";
    private static final String REQUEST_COOLDOWN_PATH = "requests.cooldown-seconds";
    private static final String REQUEST_SOUND_PATH = "requests.sound";

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
        SoundSetting inviteSound = readSound(plugin, config, INVITE_SOUND_PATH);
        SoundSetting requestSound = readSound(plugin, config, REQUEST_SOUND_PATH);
        return new PluginSettings(
                readInt(plugin, config, INVITE_EXPIRATION_PATH, DEFAULT_INVITE_EXPIRATION_MINUTES, 1),
                readInt(plugin, config, INVITE_COOLDOWN_PATH, DEFAULT_INVITE_COOLDOWN_SECONDS, 0),
                inviteSound.key(),
                inviteSound.volume(),
                inviteSound.pitch(),
                readInt(plugin, config, REQUEST_EXPIRATION_PATH, DEFAULT_REQUEST_EXPIRATION_MINUTES, 1),
                readInt(plugin, config, REQUEST_COOLDOWN_PATH, DEFAULT_REQUEST_COOLDOWN_SECONDS, 0),
                requestSound.key(),
                requestSound.volume(),
                requestSound.pitch()
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

    private record SoundSetting(String key, float volume, float pitch) {
    }

    // CMI-style single line: <sound id>[:volume[:pitch]]. Trailing numeric
    // segments are volume and pitch; the rest is the sound id, which may itself
    // contain one namespace colon (vanilla sound path segments are never purely
    // numeric, so the split is unambiguous).
    private static SoundSetting readSound(JavaPlugin plugin, FileConfiguration config, String path) {
        String value = config.getString(path, DEFAULT_SOUND);
        if (value == null || value.isBlank() || value.equalsIgnoreCase("none")) {
            return new SoundSetting(null, DEFAULT_SOUND_VOLUME, DEFAULT_SOUND_PITCH);
        }

        String[] parts = value.split(":");
        java.util.List<Float> numbers = new java.util.ArrayList<>();
        int keyParts = parts.length;
        while (keyParts > 1 && numbers.size() < 2) {
            Float parsed = tryParseFloat(parts[keyParts - 1]);
            if (parsed == null) {
                break;
            }
            numbers.add(0, parsed);
            keyParts--;
        }

        String keyValue = String.join(":", java.util.Arrays.copyOfRange(parts, 0, keyParts));
        try {
            Key.key(keyValue);
        } catch (InvalidKeyException invalidKey) {
            warn(plugin, path, DEFAULT_SOUND,
                    "must be <sound id>[:volume[:pitch]] like " + DEFAULT_SOUND + ":0.5:2, or none");
            return new SoundSetting(DEFAULT_SOUND,
                    DEFAULT_SOUND_VOLUME, DEFAULT_SOUND_PITCH);
        }

        float volume = DEFAULT_SOUND_VOLUME;
        if (!numbers.isEmpty()) {
            float candidate = numbers.get(0);
            if (candidate < 0.0F || candidate > 10.0F) {
                warn(plugin, path, DEFAULT_SOUND_VOLUME,
                        "volume must be between 0.0 and 10.0");
            } else {
                volume = candidate;
            }
        }
        // Minecraft clamps pitch to [0.5, 2.0]; values outside it would be
        // silently distorted, so they are rejected up front.
        float pitch = DEFAULT_SOUND_PITCH;
        if (numbers.size() == 2) {
            float candidate = numbers.get(1);
            if (candidate < 0.5F || candidate > 2.0F) {
                warn(plugin, path, DEFAULT_SOUND_PITCH,
                        "pitch must be between 0.5 and 2.0");
            } else {
                pitch = candidate;
            }
        }
        return new SoundSetting(keyValue, volume, pitch);
    }

    private static Float tryParseFloat(String value) {
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException notANumber) {
            return null;
        }
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
