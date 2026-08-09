package ru.vanillagame.voicechat.groupmanagement;

import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.events.CreateGroupEvent;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.JoinGroupEvent;
import de.maxhenkel.voicechat.api.events.LeaveGroupEvent;
import de.maxhenkel.voicechat.api.events.RemoveGroupEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStartedEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStoppedEvent;

final class VoiceChatAddon implements VoicechatPlugin {

    static final String PLUGIN_ID = "vanilla_game_svc_group_management";

    private final SvcGroupManagementPlugin plugin;
    private final GroupLeadershipRegistry leadership;
    private final InviteStore invites;
    private final RequestStore requests;

    VoiceChatAddon(
            SvcGroupManagementPlugin plugin,
            GroupLeadershipRegistry leadership,
            InviteStore invites,
            RequestStore requests
    ) {
        this.plugin = plugin;
        this.leadership = leadership;
        this.invites = invites;
        this.requests = requests;
    }

    @Override
    public String getPluginId() {
        return PLUGIN_ID;
    }

    @Override
    public void initialize(VoicechatApi api) {
        if (api instanceof VoicechatServerApi serverApi) {
            plugin.setVoicechatApi(serverApi);
        }
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(VoicechatServerStartedEvent.class, this::onServerStarted);
        registration.registerEvent(VoicechatServerStoppedEvent.class, this::onServerStopped);
        registration.registerEvent(CreateGroupEvent.class, this::onGroupCreated, -1000);
        registration.registerEvent(JoinGroupEvent.class, this::onGroupJoined, -1000);
        registration.registerEvent(LeaveGroupEvent.class, this::onGroupLeft, -1000);
        registration.registerEvent(RemoveGroupEvent.class, this::onGroupRemoved, -1000);
    }

    private void onServerStarted(VoicechatServerStartedEvent event) {
        if (plugin.isEnabled()) {
            plugin.setVoicechatApi(event.getVoicechat());
        }
    }

    private void onServerStopped(VoicechatServerStoppedEvent event) {
        if (plugin.isEnabled()) {
            plugin.clearVoicechatState();
        }
    }

    private void onGroupCreated(CreateGroupEvent event) {
        if (!plugin.isEnabled() || event.isCancelled()) {
            return;
        }

        VoicechatConnection connection = event.getConnection();
        if (connection == null || connection.getPlayer() == null) {
            return;
        }
        plugin.publishLeadership(leadership.createGroup(
                event.getGroup().getId(),
                connection.getPlayer().getUuid()
        ));
    }

    private void onGroupJoined(JoinGroupEvent event) {
        if (!plugin.isEnabled() || event.isCancelled() || event.getConnection().getPlayer() == null) {
            return;
        }
        plugin.publishLeadership(leadership.join(
                event.getGroup().getId(),
                event.getConnection().getPlayer().getUuid()
        ));
    }

    private void onGroupLeft(LeaveGroupEvent event) {
        if (!plugin.isEnabled() || event.isCancelled() || event.getGroup() == null
                || event.getConnection().getPlayer() == null) {
            return;
        }
        plugin.publishLeadership(leadership.leave(
                event.getGroup().getId(),
                event.getConnection().getPlayer().getUuid()
        ));
    }

    private void onGroupRemoved(RemoveGroupEvent event) {
        if (!plugin.isEnabled() || event.isCancelled()) {
            return;
        }

        plugin.publishLeadership(leadership.removeGroup(event.getGroup().getId()));
        invites.invalidateGroup(event.getGroup().getId());
        requests.invalidateGroup(event.getGroup().getId());
    }
}
