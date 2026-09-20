package ru.vanillagame.voicechat.bettergroups;

import de.maxhenkel.voicechat.api.Group;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Temporary group leave. Audio routing remains entirely owned by Simple Voice Chat. */
final class GroupMuteService implements PluginMessageListener {
    private final BetterGroupsPlugin plugin;
    private final GroupLeadershipRegistry leadership;
    private record ReturnGrant(UUID group, boolean leader) {}
    private final Set<UUID> clients = ConcurrentHashMap.newKeySet();
    // A server-side return grant, never a password supplied by the client.
    private final ConcurrentHashMap<UUID, ReturnGrant> paused = new ConcurrentHashMap<>();
    private final Set<UUID> switching = ConcurrentHashMap.newKeySet();
    private boolean registered;

    GroupMuteService(BetterGroupsPlugin plugin, GroupLeadershipRegistry leadership) {
        this.plugin = plugin;
        this.leadership = leadership;
    }

    void register() {
        var messenger = plugin.getServer().getMessenger();
        messenger.registerIncomingPluginChannel(plugin, GroupMuteProtocol.REQUEST, this);
        messenger.registerOutgoingPluginChannel(plugin, GroupMuteProtocol.STATE);
        registered = true;
    }

    void unregister() {
        if (!registered) return;
        clear();
        var messenger = plugin.getServer().getMessenger();
        messenger.unregisterIncomingPluginChannel(plugin, GroupMuteProtocol.REQUEST, this);
        messenger.unregisterOutgoingPluginChannel(plugin, GroupMuteProtocol.STATE);
        clients.clear();
        registered = false;
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] bytes) {
        if (!GroupMuteProtocol.REQUEST.equals(channel)) return;
        var request = GroupMuteProtocol.decode(bytes);
        if (request == null) return;
        UUID id = player.getUniqueId();
        if (request.id() == 0) {
            clients.add(id);
            sendState(player, 0, GroupMuteProtocol.OK);
            return;
        }
        if (!clients.contains(id)) return;
        var api = plugin.getVoicechatApi();
        var connection = api == null ? null : api.getConnectionOf(id);
        boolean accepted = connection != null
                && (request.muted() ? pause(id, connection, request.group()) : resume(player, connection, request.group()));
        // setGroup() can be cancelled by another plugin. Only acknowledge the actual result.
        sendState(player, request.id(), accepted ? GroupMuteProtocol.OK : GroupMuteProtocol.REJECTED);
    }

    private boolean pause(UUID player, VoicechatConnection connection, UUID groupId) {
        if (connection.getGroup() == null) { // Retry also acknowledges a completed final leave.
            return groupId.equals(pausedGroup(player)) || plugin.getVoicechatApi().getGroup(groupId) == null;
        }
        if (!groupId.equals(connection.getGroup().getId())) return false;
        ReturnGrant grant = new ReturnGrant(groupId, player.equals(leadership.leaderOf(groupId)));
        ReturnGrant previous = paused.put(player, grant);
        if (previous != null && !previous.group().equals(groupId)) cleanupGroup(previous.group());
        switching.add(player);
        try {
            // A return grant survives only while the native group still exists.
            connection.setGroup(null);
            var updated = plugin.getVoicechatApi().getConnectionOf(player);
            if (updated != null && updated.getGroup() == null) {
                if (plugin.getVoicechatApi().getGroup(groupId) == null) groupRemoved(groupId);
                return true;
            }
            paused.remove(player, grant);
            cleanupGroup(groupId);
            return false;
        } finally {
            switching.remove(player);
        }
    }

    private boolean resume(Player player, VoicechatConnection connection, UUID groupId) {
        UUID id = player.getUniqueId();
        if (connection.getGroup() != null) {
            // A repeated successful return must not move the player out of another group.
            return groupId.equals(connection.getGroup().getId()) && !paused.containsKey(id);
        }
        ReturnGrant grant = paused.get(id);
        if (grant == null || !groupId.equals(grant.group())) return false;
        var api = plugin.getVoicechatApi();
        Group group = api.getGroup(groupId);
        if (group == null) {
            paused.remove(id, grant);
            return false;
        }
        if (!player.hasPermission("voicechat.groups") || !api.getServerConfig().getBoolean("enable_groups", true)) return false;
        switching.add(id);
        try {
            // The API joins the server's existing group without disclosing its password.
            connection.setGroup(group);
            var updated = api.getConnectionOf(id); // VoicechatConnection is a snapshot.
            if (updated == null || updated.getGroup() == null || !groupId.equals(updated.getGroup().getId())) return false;
            if (grant.leader()) {
                UUID currentLeader = leadership.leaderOf(groupId);
                if (currentLeader != null) {
                    plugin.publishLeadership(leadership.transferLeadership(groupId, currentLeader, id).transition());
                }
            }
            paused.remove(id, grant);
            cleanupGroup(groupId);
            return true;
        } finally {
            switching.remove(id);
        }
    }

    void groupRemoved(UUID group) {
        paused.forEach((player, destination) -> {
            if (group.equals(destination.group()) && paused.remove(player, destination)) publish(player);
        });
    }

    boolean holdsGroup(UUID group) {
        return paused.values().stream().anyMatch(grant -> group.equals(grant.group()));
    }

    void membershipChanged(UUID player) {
        // Our own leave/join fires the same events as manual group changes.
        if (switching.contains(player)) return;
        ReturnGrant grant = paused.remove(player);
        if (grant == null) return;
        UUID group = grant.group();
        // Events run before the native join commits. Removing an empty group here
        // could delete the very destination that the player is joining manually.
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            cleanupGroup(group);
            publish(player);
        });
    }

    void forget(UUID player) {
        clients.remove(player);
        clearPlayer(player);
    }

    void clearPlayer(UUID player) {
        ReturnGrant grant = paused.remove(player);
        if (grant == null) return;
        UUID group = grant.group();
        cleanupGroup(group);
        publish(player);
    }

    void clear() {
        Set<UUID> players = Set.copyOf(paused.keySet());
        Set<UUID> groups = new HashSet<>();
        paused.values().forEach(grant -> groups.add(grant.group()));
        paused.clear();
        groups.forEach(this::cleanupGroup);
        players.forEach(this::publish);
    }

    private void cleanupGroup(UUID id) {
        var api = plugin.getVoicechatApi();
        if (api == null || holdsGroup(id)) return;
        var group = api.getGroup(id);
        // SVC refuses to remove occupied groups. Never remove a persistent group.
        if (group != null && !group.isPersistent()) api.removeGroup(id);
    }

    private void publish(UUID playerId) {
        if (!plugin.isEnabled() || !clients.contains(playerId)) return;
        Player player = Bukkit.getPlayer(playerId);
        if (player != null && player.isOnline()) sendState(player, -1, GroupMuteProtocol.OK);
    }

    private UUID pausedGroup(UUID player) {
        ReturnGrant grant = paused.get(player);
        return grant == null ? null : grant.group();
    }

    private void sendState(Player player, int request, int result) {
        player.sendPluginMessage(plugin, GroupMuteProtocol.STATE,
                GroupMuteProtocol.encode(request, result, pausedGroup(player.getUniqueId())));
    }
}
