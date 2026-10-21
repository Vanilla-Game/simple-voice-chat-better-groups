package ru.vanillagame.voicechat.bettergroups;

import de.maxhenkel.voicechat.api.Group;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class GroupChatServiceTest {
    private final BetterGroupsPlugin plugin = mock(BetterGroupsPlugin.class);
    private final VoicechatServerApi api = mock(VoicechatServerApi.class);
    private final GroupChatService chat = new GroupChatService(plugin);

    @Test
    void onlyCurrentGroupMembersIncludingSenderReceiveLiteralText() {
        when(plugin.isEnabled()).thenReturn(true);
        when(plugin.getVoicechatApi()).thenReturn(api);
        Player sender = player("Sender");
        Player member = player("Member");
        Player outsider = player("Outsider");
        Player paused = player("Paused");
        Player unavailable = player("Unavailable");
        Group group = group();
        Group otherGroup = group();
        membership(sender, group);
        membership(member, group);
        membership(outsider, otherGroup);
        membership(paused, null);
        String text = "120 64 -350 <click:run_command:'/op Sender'> &c";

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getOnlinePlayers).thenReturn(List.of(sender, member, outsider, paused, unavailable));
            bukkit.when(() -> Bukkit.isOwnedByCurrentRegion(any(Player.class))).thenReturn(true);
            chat.send(sender, text, api);
        }

        Component expected = Messages.component(Messages.CHAT_MESSAGE, NamedTextColor.AQUA,
                Component.text("Sender"), Component.text(text, NamedTextColor.WHITE));
        verify(sender).sendMessage(expected);
        verify(member).sendMessage(expected);
        verify(outsider, never()).sendMessage(any(Component.class));
        verify(paused, never()).sendMessage(any(Component.class));
        verify(unavailable, never()).sendMessage(any(Component.class));
    }

    @Test
    void unavailableAndUngroupedSendersCannotBroadcast() {
        Player sender = player("Sender");
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            chat.send(sender, "hello", api);
            verify(sender).sendMessage(Messages.component(Messages.CONNECTION_SELF_UNAVAILABLE, NamedTextColor.RED));
            membership(sender, null);
            chat.send(sender, "hello", api);
            verify(sender).sendMessage(Messages.component(Messages.CHAT_NOT_IN_GROUP, NamedTextColor.RED));
            bukkit.verify(Bukkit::getOnlinePlayers, never());
        }
    }

    @Test
    void whitespaceOnlyMessagesAreRejected() {
        Player sender = player("Sender");
        membership(sender, group());
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            chat.send(sender, "   ", api);
            verify(sender).sendMessage(Messages.component(Messages.CHAT_USAGE, NamedTextColor.YELLOW,
                    Component.text("/voicegroup chat")));
            bukkit.verify(Bukkit::getOnlinePlayers, never());
        }
    }

    @Test
    void groupChatDoesNotRevealHiddenSenders() {
        when(plugin.isEnabled()).thenReturn(true);
        when(plugin.getVoicechatApi()).thenReturn(api);
        Player sender = player("Hidden");
        Player member = player("Member");
        Group group = group();
        membership(sender, group);
        membership(member, group);
        when(member.canSee(sender)).thenReturn(false);
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getOnlinePlayers).thenReturn(List.of(sender, member));
            bukkit.when(() -> Bukkit.isOwnedByCurrentRegion(any(Player.class))).thenReturn(true);
            chat.send(sender, "hello", api);
        }
        verify(sender).sendMessage(any(Component.class));
        verify(member, never()).sendMessage(any(Component.class));
    }

    private void membership(Player player, Group group) {
        VoicechatConnection connection = mock(VoicechatConnection.class);
        when(connection.getGroup()).thenReturn(group);
        when(api.getConnectionOf(player.getUniqueId())).thenReturn(connection);
    }

    private static Player player(String name) {
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.getName()).thenReturn(name);
        when(player.isOnline()).thenReturn(true);
        when(player.canSee(any(Player.class))).thenReturn(true);
        return player;
    }

    private static Group group() {
        Group group = mock(Group.class);
        when(group.getId()).thenReturn(UUID.randomUUID());
        return group;
    }
}
