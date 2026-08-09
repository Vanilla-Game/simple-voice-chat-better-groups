package ru.vanillagame.voicechat.groupmanagement;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// Announces joins to the other group members. Joins arrive through the public
// JoinGroupEvent regardless of the path (invite, request, password, open
// group); an invite attribution is registered by the accept flow just before
// the join and consumed here. The map is confined to the main thread.
final class JoinNotifier {

    private final SvcGroupManagementPlugin plugin;
    private final GroupLeadershipRegistry leadership;
    private final Map<UUID, String> pendingInviteAttributions = new HashMap<>();

    JoinNotifier(SvcGroupManagementPlugin plugin, GroupLeadershipRegistry leadership) {
        this.plugin = plugin;
        this.leadership = leadership;
    }

    void attributeInvite(UUID joinerId, String inviterName) {
        pendingInviteAttributions.put(joinerId, inviterName);
    }

    void clearAttribution(UUID joinerId) {
        pendingInviteAttributions.remove(joinerId);
    }

    void onJoin(UUID groupId, UUID joinerId) {
        runOnMainThread(() -> {
            String inviterName = pendingInviteAttributions.remove(joinerId);
            Player joiner = Bukkit.getPlayer(joinerId);
            if (joiner == null || !joiner.isOnline()) {
                return;
            }
            for (UUID memberId : leadership.membersOf(groupId)) {
                if (memberId.equals(joinerId)) {
                    continue;
                }
                Player member = Bukkit.getPlayer(memberId);
                if (member == null || !member.isOnline()) {
                    continue;
                }
                member.sendMessage(inviterName == null
                        ? Messages.component(
                                Messages.GROUP_MEMBER_JOINED,
                                NamedTextColor.YELLOW,
                                Component.text(joiner.getName()))
                        : Messages.component(
                                Messages.GROUP_MEMBER_JOINED_INVITED,
                                NamedTextColor.YELLOW,
                                Component.text(joiner.getName()),
                                Component.text(inviterName)));
            }
        });
    }

    void clear() {
        pendingInviteAttributions.clear();
    }

    private void runOnMainThread(Runnable task) {
        if (Bukkit.isPrimaryThread()) {
            task.run();
            return;
        }
        plugin.getServer().getScheduler().runTask(plugin, task);
    }
}
