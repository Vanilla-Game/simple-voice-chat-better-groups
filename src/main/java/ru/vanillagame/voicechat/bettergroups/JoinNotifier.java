package ru.vanillagame.voicechat.bettergroups;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// Announces joins to the other group members. Joins arrive through the public
// JoinGroupEvent regardless of the path (invite, request, password, open
// group); an invite attribution is registered by the accept flow just before
// the join and consumed here. Attributions can arrive from different player regions.
final class JoinNotifier {

    private final BetterGroupsPlugin plugin;
    private final GroupLeadershipRegistry leadership;
    private final Map<UUID, String> pendingInviteAttributions = new ConcurrentHashMap<>();

    JoinNotifier(BetterGroupsPlugin plugin, GroupLeadershipRegistry leadership) {
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
        String inviterName = pendingInviteAttributions.remove(joinerId);
        Player joiner = Bukkit.getPlayer(joinerId);
        if (joiner == null) return;
        String joinerName = joiner.getName();
        for (UUID memberId : leadership.membersOf(groupId)) {
            if (memberId.equals(joinerId)) continue;
            Player member = Bukkit.getPlayer(memberId);
            if (member == null) continue;
            PlayerTasks.run(plugin, member, () -> {
                if (!joiner.isOnline() || !member.canSee(joiner)
                        || !groupId.equals(leadership.stateFor(memberId).groupId())) return;
                member.sendMessage(inviterName == null
                        ? Messages.component(Messages.GROUP_MEMBER_JOINED, NamedTextColor.YELLOW,
                                Component.text(joinerName))
                        : Messages.component(Messages.GROUP_MEMBER_JOINED_INVITED, NamedTextColor.YELLOW,
                                Component.text(joinerName), Component.text(inviterName)));
            });
        }
    }

    void clear() {
        pendingInviteAttributions.clear();
    }
}
