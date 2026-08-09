package ru.vanillagame.voicechat.groupmanagement;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JoinNotifierTest {

    @Test
    void notifiesExistingMembersOnceAndConsumesInviteAttribution() {
        GroupLeadershipRegistry leadership = new GroupLeadershipRegistry();
        SvcGroupManagementPlugin plugin = mock(SvcGroupManagementPlugin.class);
        JoinNotifier notifier = new JoinNotifier(plugin, leadership);
        UUID groupId = UUID.randomUUID();
        Player creator = player("Creator");
        Player joiner = player("Joiner");
        leadership.createGroup(groupId, creator.getUniqueId());
        leadership.join(groupId, joiner.getUniqueId());

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::isPrimaryThread).thenReturn(true);
            bukkit.when(() -> Bukkit.getPlayer(creator.getUniqueId())).thenReturn(creator);
            bukkit.when(() -> Bukkit.getPlayer(joiner.getUniqueId())).thenReturn(joiner);

            notifier.attributeInvite(joiner.getUniqueId(), "Inviter");
            notifier.onJoin(groupId, joiner.getUniqueId());
            // Attribution is one-shot: a later join announcement is generic.
            notifier.onJoin(groupId, joiner.getUniqueId());
        }

        verify(creator, times(2)).sendMessage(any(Component.class));
        verify(joiner, never()).sendMessage(any(Component.class));
    }

    private static Player player(String name) {
        Player player = mock(Player.class);
        when(player.getName()).thenReturn(name);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.isOnline()).thenReturn(true);
        return player;
    }
}
