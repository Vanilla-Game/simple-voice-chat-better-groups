package ru.vanillagame.voicechat.bettergroups;

import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.Messenger;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

final class LeaderSyncService implements PluginMessageListener {

    private final BetterGroupsPlugin plugin;
    private final GroupLeadershipRegistry leadership;
    private final Set<UUID> compatibleClients = new HashSet<>();
    private boolean registered;

    LeaderSyncService(BetterGroupsPlugin plugin, GroupLeadershipRegistry leadership) {
        this.plugin = plugin;
        this.leadership = leadership;
    }

    void register() {
        Messenger messenger = plugin.getServer().getMessenger();
        messenger.registerIncomingPluginChannel(plugin, LeaderSyncProtocol.HELLO_CHANNEL, this);
        messenger.registerOutgoingPluginChannel(plugin, LeaderSyncProtocol.STATE_CHANNEL);
        registered = true;
    }

    void unregister() {
        if (!registered) {
            return;
        }
        Messenger messenger = plugin.getServer().getMessenger();
        messenger.unregisterIncomingPluginChannel(plugin, LeaderSyncProtocol.HELLO_CHANNEL, this);
        messenger.unregisterOutgoingPluginChannel(plugin, LeaderSyncProtocol.STATE_CHANNEL);
        compatibleClients.clear();
        registered = false;
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!LeaderSyncProtocol.HELLO_CHANNEL.equals(channel)
                || !LeaderSyncProtocol.isCompatibleHello(message)) {
            return;
        }

        compatibleClients.add(player.getUniqueId());
        sendCurrentState(player);
    }

    void publish(GroupLeadershipRegistry.Transition transition) {
        if (!transition.changed()) {
            return;
        }
        runOnMainThread(() -> {
            transition.affectedPlayerIds().forEach(playerId -> {
                Player player = Bukkit.getPlayer(playerId);
                if (player != null && player.isOnline()) {
                    sendCurrentState(player);
                }
            });
            notifyPromotedLeaders(transition);
        });
    }

    void forget(UUID playerId) {
        compatibleClients.remove(playerId);
    }

    private void sendCurrentState(Player player) {
        if (!compatibleClients.contains(player.getUniqueId())) {
            return;
        }
        byte[] message = LeaderSyncProtocol.encode(leadership.stateFor(player.getUniqueId()));
        player.sendPluginMessage(plugin, LeaderSyncProtocol.STATE_CHANNEL, message);
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
