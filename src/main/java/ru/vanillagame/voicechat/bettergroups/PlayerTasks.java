package ru.vanillagame.voicechat.bettergroups;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/** Entity scheduling follows the player across regions; Paper runs it on its main thread. */
final class PlayerTasks {
    private PlayerTasks() {}

    static void run(Plugin plugin, Player player, Runnable action) {
        if (!plugin.isEnabled()) return;
        if (Bukkit.isOwnedByCurrentRegion(player)) {
            if (player.isOnline()) action.run();
        } else {
            later(plugin, player, action);
        }
    }

    static void later(Plugin plugin, Player player, Runnable action) {
        if (!plugin.isEnabled()) return;
        player.getScheduler().execute(plugin, () -> {
            if (plugin.isEnabled() && player.isOnline()) action.run();
        }, null, 1L);
    }
}
