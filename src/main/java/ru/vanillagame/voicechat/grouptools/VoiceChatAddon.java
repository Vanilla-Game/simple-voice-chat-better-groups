package ru.vanillagame.voicechat.grouptools;

import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.events.CreateGroupEvent;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.RemoveGroupEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStartedEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStoppedEvent;

final class VoiceChatAddon implements VoicechatPlugin {

    static final String PLUGIN_ID = "vanilla_game_voicechat_group_tools";

    private final VoiceChatGroupToolsPlugin plugin;
    private final GroupOwnershipRegistry ownership;
    private final InviteStore invites;

    VoiceChatAddon(
            VoiceChatGroupToolsPlugin plugin,
            GroupOwnershipRegistry ownership,
            InviteStore invites
    ) {
        this.plugin = plugin;
        this.ownership = ownership;
        this.invites = invites;
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
        ownership.recordCreator(event.getGroup().getId(), connection.getPlayer().getUuid());
    }

    private void onGroupRemoved(RemoveGroupEvent event) {
        if (!plugin.isEnabled() || event.isCancelled()) {
            return;
        }

        ownership.remove(event.getGroup().getId());
        invites.invalidateGroup(event.getGroup().getId());
    }
}
