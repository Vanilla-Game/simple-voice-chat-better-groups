package ru.vanillagame.voicechat.bettergroups;

import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.Messenger;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

final class GroupSyncService implements PluginMessageListener {

    private final BetterGroupsPlugin plugin;
    private final GroupLeadershipRegistry leadership;
    private final Set<UUID> compatibleClients = new HashSet<>();
    private boolean registered;

    GroupSyncService(BetterGroupsPlugin plugin, GroupLeadershipRegistry leadership) {
        this.plugin = plugin;
        this.leadership = leadership;
    }

    void register() {
        Messenger messenger = plugin.getServer().getMessenger();
        messenger.registerIncomingPluginChannel(plugin, GroupSyncProtocol.CLIENT_HELLO_CHANNEL, this);
        messenger.registerOutgoingPluginChannel(plugin, GroupSyncProtocol.SERVER_HELLO_CHANNEL);
        messenger.registerOutgoingPluginChannel(plugin, GroupSyncProtocol.GROUP_STATE_CHANNEL);
        registered = true;
    }

    void unregister() {
        if (!registered) {
            return;
        }
        Messenger messenger = plugin.getServer().getMessenger();
        messenger.unregisterIncomingPluginChannel(plugin, GroupSyncProtocol.CLIENT_HELLO_CHANNEL, this);
        messenger.unregisterOutgoingPluginChannel(plugin, GroupSyncProtocol.SERVER_HELLO_CHANNEL);
        messenger.unregisterOutgoingPluginChannel(plugin, GroupSyncProtocol.GROUP_STATE_CHANNEL);
        compatibleClients.clear();
        registered = false;
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!GroupSyncProtocol.CLIENT_HELLO_CHANNEL.equals(channel)
                || !GroupSyncProtocol.isCompatibleClientHello(message)) {
            return;
        }

        compatibleClients.add(player.getUniqueId());
        sendServerHello(player);
        GroupLeadershipRegistry.PlayerLeadershipState state = leadership.stateFor(player.getUniqueId());
        if (state.inGroup()) {
            sendGroupState(player, state);
        }
    }

    void publish(GroupLeadershipRegistry.Transition transition) {
        if (!transition.changed()) {
            return;
        }
        runOnMainThread(() -> {
            transition.affectedPlayerIds().forEach(playerId -> {
                Player player = Bukkit.getPlayer(playerId);
                if (player != null && player.isOnline()) {
                    sendCurrentGroupState(player);
                }
            });
            notifyPromotedLeaders(transition);
        });
    }

    void forget(UUID playerId) {
        compatibleClients.remove(playerId);
    }

    private void sendServerHello(Player player) {
        player.sendPluginMessage(
                plugin,
                GroupSyncProtocol.SERVER_HELLO_CHANNEL,
                GroupSyncProtocol.encodeServerHello()
        );
    }

    private void sendCurrentGroupState(Player player) {
        if (!compatibleClients.contains(player.getUniqueId())) {
            return;
        }
        sendGroupState(player, leadership.stateFor(player.getUniqueId()));
    }

    private void sendGroupState(Player player, GroupLeadershipRegistry.PlayerLeadershipState state) {
        byte[] message = GroupSyncProtocol.encodeGroupState(state);
        player.sendPluginMessage(plugin, GroupSyncProtocol.GROUP_STATE_CHANNEL, message);
    }

    private void notifyPromotedLeaders(GroupLeadershipRegistry.Transition transition) {
        transition.leadershipChanges().forEach(change -> {
            if (change.previousLeaderId() == null || change.newLeaderId() == null) {
                return;
            }
            Player newLeader = Bukkit.getPlayer(change.newLeaderId());
            if (newLeader != null && newLeader.isOnline()) {
                newLeader.sendMessage(Messages.component(Messages.LEADER_PROMOTED, NamedTextColor.GREEN));
            }
        });
    }

    private void runOnMainThread(Runnable task) {
        if (Bukkit.isPrimaryThread()) {
            task.run();
            return;
        }
        plugin.getServer().getScheduler().runTask(plugin, task);
    }
}
