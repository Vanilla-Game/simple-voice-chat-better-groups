package ru.vanillagame.voicechat.bettergroups;

import de.maxhenkel.voicechat.api.VoicechatServerApi;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

/** Text delivery follows live SVC membership, independently of leadership tracking. */
final class GroupChatService {
    private final BetterGroupsPlugin plugin;

    GroupChatService(BetterGroupsPlugin plugin) {
        this.plugin = plugin;
    }

    void send(Player sender, String text, VoicechatServerApi api) {
        UUID senderId = sender.getUniqueId();
        var connection = api.getConnectionOf(senderId);
        if (connection == null) {
            sender.sendMessage(Messages.component(Messages.CONNECTION_SELF_UNAVAILABLE, NamedTextColor.RED));
            return;
        }
        var group = connection.getGroup();
        if (group == null) {
            sender.sendMessage(Messages.component(Messages.CHAT_NOT_IN_GROUP, NamedTextColor.RED));
            return;
        }
        if (text.isBlank()) {
            sender.sendMessage(Messages.component(Messages.CHAT_USAGE, NamedTextColor.YELLOW,
                    Component.text("/voicegroup chat")));
            return;
        }

        UUID groupId = group.getId();
        Component message = Messages.component(Messages.CHAT_MESSAGE, NamedTextColor.AQUA,
                Component.text(sender.getName(), NamedTextColor.GOLD), Component.text(text, NamedTextColor.WHITE));
        for (Player recipient : Bukkit.getOnlinePlayers()) {
            UUID recipientId = recipient.getUniqueId();
            // Capture the audience at send time: later joiners do not receive queued messages.
            if (!inGroup(api, recipientId, groupId)) continue;
            PlayerTasks.run(plugin, recipient, () -> {
                // Folia delivery can be deferred; pause, kick, leave and shutdown revoke access.
                if (plugin.getVoicechatApi() != api || !sender.isOnline()
                        || !inGroup(api, senderId, groupId)
                        || !inGroup(api, recipientId, groupId)
                        || !recipient.canSee(sender)) return;
                recipient.sendMessage(message);
            });
        }
    }

    private static boolean inGroup(VoicechatServerApi api, UUID playerId, UUID groupId) {
        var connection = api.getConnectionOf(playerId);
        return connection != null && connection.getGroup() != null
                && groupId.equals(connection.getGroup().getId());
    }
}
