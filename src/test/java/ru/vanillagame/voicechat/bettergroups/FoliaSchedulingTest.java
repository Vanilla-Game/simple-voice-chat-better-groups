package ru.vanillagame.voicechat.bettergroups;

import com.mojang.brigadier.CommandDispatcher;
import de.maxhenkel.voicechat.api.Group;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.threadedregions.scheduler.EntityScheduler;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class FoliaSchedulingTest {
    private final BetterGroupsPlugin plugin = mock(BetterGroupsPlugin.class);
    private final VoicechatServerApi api = mock(VoicechatServerApi.class);
    private final GroupLeadershipRegistry leadership = new GroupLeadershipRegistry();
    private final RequestStore requests = new RequestStore(Clock.systemUTC(), Duration.ofMinutes(5));
    private final Group group = mock(Group.class);
    private final UUID groupId = UUID.randomUUID();
    private MockedStatic<Bukkit> bukkit;
    private Player owner;

    @BeforeEach void setup() {
        bukkit = mockStatic(Bukkit.class);
        bukkit.when(() -> Bukkit.isOwnedByCurrentRegion(any(Player.class)))
                .thenAnswer(call -> call.getArgument(0) == owner);
        bukkit.when(Bukkit::getScheduler).thenThrow(new AssertionError("Legacy scheduler used"));
        when(plugin.isEnabled()).thenReturn(true);
        when(plugin.getVoicechatApi()).thenReturn(api);
        when(group.getId()).thenReturn(groupId);
        when(api.getGroup(groupId)).thenReturn(group);
    }

    @AfterEach void close() { bukkit.close(); }

    @Test void owningThreadRunsImmediatelyOnPaperOrFolia() {
        Region region = region("Player");
        owner = region.player();
        Runnable action = mock(Runnable.class);
        PlayerTasks.run(plugin, region.player(), action);
        verify(action).run();
        assertTrue(region.tasks().isEmpty());
    }

    @Test void foreignRegionWaitsForPlayerScheduler() {
        Region region = region("Player");
        Runnable action = mock(Runnable.class);
        PlayerTasks.run(plugin, region.player(), action);
        verifyNoInteractions(action);
        region.tick();
        verify(action).run();
    }

    @Test void retiredPlayerDoesNotFallBackToGlobalOrLegacyThread() {
        Region region = region("Player");
        when(region.player().getScheduler().execute(eq(plugin), any(), isNull(), eq(1L))).thenReturn(false);
        Runnable action = mock(Runnable.class);
        PlayerTasks.run(plugin, region.player(), action);
        verifyNoInteractions(action);
        assertTrue(region.tasks().isEmpty());
    }

    @Test void playerDisconnectBeforeExecutionDropsTask() {
        Region region = region("Player");
        Runnable action = mock(Runnable.class);
        PlayerTasks.run(plugin, region.player(), action);
        when(region.player().isOnline()).thenReturn(false);
        region.tick();
        verifyNoInteractions(action);
    }

    @Test void disableDropsQueuedAndNewPlayerTasks() {
        Region region = region("Player");
        Runnable action = mock(Runnable.class);
        PlayerTasks.run(plugin, region.player(), action);
        when(plugin.isEnabled()).thenReturn(false);
        region.tick();
        PlayerTasks.run(plugin, region.player(), action);
        verifyNoInteractions(action);
        assertTrue(region.tasks().isEmpty());
    }

    @Test void deferredWorkWaitsEvenOnOwningThread() {
        Region region = region("Player");
        owner = region.player();
        Runnable action = mock(Runnable.class);
        PlayerTasks.later(plugin, region.player(), action);
        verifyNoInteractions(action);
        region.tick();
        verify(action).run();
    }

    @Test void kickMutatesTargetOnItsRegionAndRepliesOnLeadersRegion() throws Exception {
        Region leader = region("Leader");
        Region target = region("Target");
        addMembers(leader, target);
        VoicechatConnection connection = connection(group);
        when(api.getConnectionOf(target.id())).thenReturn(connection);
        doAnswer(call -> {
            assertSame(target.player(), owner);
            when(api.getConnectionOf(target.id())).thenAnswer(ignored -> connection(null));
            return null;
        }).when(connection).setGroup(null);
        execute(leader, "voicegroup kick Target");
        verify(connection, never()).setGroup(any());
        target.tick();
        verify(connection).setGroup(null);
        verify(leader.player(), never()).sendMessage(any(Component.class));
        leader.tick();
        verify(leader.player()).sendMessage(any(Component.class));
    }

    @Test void queuedKickLosesAuthorityWhenLeadershipChanges() throws Exception {
        Region leader = region("Leader");
        Region target = region("Target");
        addMembers(leader, target);
        VoicechatConnection connection = connection(group);
        when(api.getConnectionOf(target.id())).thenReturn(connection);
        execute(leader, "voicegroup kick Target");
        leadership.transferLeadership(groupId, leader.id(), target.id());
        target.tick();
        verify(connection, never()).setGroup(any());
    }

    @Test void queuedApprovalRechecksLeadershipAndLeavesRequestAvailable() throws Exception {
        Region leader = region("Leader");
        Region successor = region("Successor");
        Region requester = region("Requester");
        addMembers(leader, successor);
        VoicechatConnection connection = connection(null);
        when(api.getConnectionOf(requester.id())).thenReturn(connection);
        String token = requests.create(requester.id(), groupId);
        execute(leader, "voicegroup approve " + token);
        leadership.transferLeadership(groupId, leader.id(), successor.id());
        requester.tick();
        verify(connection, never()).setGroup(any());
        assertEquals(RequestStore.LookupStatus.VALID, requests.lookup(token).status());
    }

    @Test void queuedApprovalsCannotReuseConsumedToken() throws Exception {
        Region leader = region("Leader");
        Region requester = region("Requester");
        leadership.createGroup(groupId, leader.id());
        VoicechatConnection connection = connection(null);
        when(api.getConnectionOf(requester.id())).thenReturn(connection);
        doAnswer(call -> {
            assertSame(requester.player(), owner);
            when(api.getConnectionOf(requester.id())).thenAnswer(ignored -> connection(group));
            return null;
        }).when(connection).setGroup(group);
        String token = requests.create(requester.id(), groupId);
        execute(leader, "voicegroup approve " + token);
        execute(leader, "voicegroup approve " + token);
        verify(connection, never()).setGroup(any());
        requester.tick();
        // Even if the player leaves again, the queued duplicate has no authorization.
        when(api.getConnectionOf(requester.id())).thenReturn(connection);
        requester.tick();
        verify(connection, times(1)).setGroup(group);
        assertEquals(RequestStore.LookupStatus.NOT_FOUND, requests.lookup(token).status());
    }

    @Test void queuedApprovalDoesNotReplaceGroupJoinedInTheMeantime() throws Exception {
        Region leader = region("Leader");
        Region requester = region("Requester");
        leadership.createGroup(groupId, leader.id());
        String token = requests.create(requester.id(), groupId);
        execute(leader, "voicegroup approve " + token);
        VoicechatConnection connection = connection(mock(Group.class));
        when(api.getConnectionOf(requester.id())).thenReturn(connection);
        requester.tick();
        verify(connection, never()).setGroup(any());
    }

    @Test void syncReadsLatestMembershipWhenRecipientsRegionTicks() {
        Region leader = region("Leader");
        Region member = region("Member");
        leadership.createGroup(groupId, leader.id());
        GroupSyncService sync = new GroupSyncService(plugin, leadership);
        sync.onPluginMessageReceived(GroupSyncProtocol.CLIENT_HELLO_CHANNEL, member.player(),
                new byte[]{GroupSyncProtocol.VERSION});
        clearInvocations(member.player());
        sync.publish(leadership.join(groupId, member.id()));
        verify(member.player(), never()).sendPluginMessage(any(), anyString(), any());
        leadership.leave(groupId, member.id());
        member.tick();
        verify(member.player()).sendPluginMessage(same(plugin), eq(GroupSyncProtocol.GROUP_STATE_CHANNEL),
                aryEq(GroupSyncProtocol.encodeGroupState(GroupLeadershipRegistry.PlayerLeadershipState.notInGroup())));
    }

    @Test void joinNotificationChecksVisibilityOnRecipientsRegion() {
        Region member = region("Member");
        Region joiner = region("Joiner");
        addMembers(member, joiner);
        when(member.player().canSee(joiner.player())).thenAnswer(call -> {
            assertSame(member.player(), owner);
            return false;
        });
        JoinNotifier notifier = new JoinNotifier(plugin, leadership);
        notifier.onJoin(groupId, joiner.id());
        verify(member.player(), never()).canSee(any(Player.class));
        member.tick();
        verify(member.player()).canSee(joiner.player());
        verify(member.player(), never()).sendMessage(any(Component.class));
    }

    private void addMembers(Region leader, Region member) {
        leadership.createGroup(groupId, leader.id());
        leadership.join(groupId, member.id());
        when(api.getConnectionOf(leader.id())).thenAnswer(ignored -> connection(group));
    }

    private void execute(Region sender, String input) throws Exception {
        var command = new VoiceGroupCommand(plugin, new InviteStore(Clock.systemUTC()), leadership,
                new InviteCooldownStore(Clock.systemUTC(), Duration.ZERO), requests,
                new InviteCooldownStore(Clock.systemUTC(), Duration.ZERO),
                new PluginSettings(5, 0, null, 1, 1, 5, 0, null, 1, 1));
        var dispatcher = new CommandDispatcher<CommandSourceStack>();
        dispatcher.getRoot().addChild(command.createCommand());
        var source = mock(CommandSourceStack.class);
        when(source.getSender()).thenReturn(sender.player());
        owner = sender.player();
        assertEquals(1, dispatcher.execute(input, source));
        owner = null;
    }

    private VoicechatConnection connection(Group membership) {
        var connection = mock(VoicechatConnection.class);
        when(connection.getGroup()).thenReturn(membership);
        return connection;
    }

    private Region region(String name) {
        Player player = mock(Player.class);
        UUID id = UUID.randomUUID();
        var scheduler = mock(EntityScheduler.class);
        Deque<Runnable> tasks = new ArrayDeque<>();
        when(player.getUniqueId()).thenReturn(id);
        when(player.getName()).thenReturn(name);
        when(player.isOnline()).thenReturn(true);
        when(player.hasPermission(anyString())).thenReturn(true);
        when(player.getScheduler()).thenReturn(scheduler);
        when(scheduler.execute(eq(plugin), any(), isNull(), eq(1L))).thenAnswer(call -> {
            tasks.add(call.getArgument(1));
            return true;
        });
        bukkit.when(() -> Bukkit.getPlayer(id)).thenReturn(player);
        bukkit.when(() -> Bukkit.getPlayerExact(name)).thenReturn(player);
        return new Region(player, id, tasks);
    }

    private final class Region {
        private final Player player;
        private final UUID id;
        private final Deque<Runnable> tasks;
        Region(Player player, UUID id, Deque<Runnable> tasks) {
            this.player = player;
            this.id = id;
            this.tasks = tasks;
        }
        Player player() { return player; }
        UUID id() { return id; }
        Deque<Runnable> tasks() { return tasks; }
        void tick() {
            owner = player;
            tasks.remove().run();
            owner = null;
        }
    }
}
