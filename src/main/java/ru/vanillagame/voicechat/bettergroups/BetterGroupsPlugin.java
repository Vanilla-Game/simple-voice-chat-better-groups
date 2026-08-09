package ru.vanillagame.voicechat.bettergroups;

import de.maxhenkel.voicechat.api.BukkitVoicechatService;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Clock;
import java.util.UUID;
import java.util.logging.Level;

public final class BetterGroupsPlugin extends JavaPlugin implements Listener {

    private volatile VoicechatServerApi voicechatApi;
    private InviteStore invites;
    private InviteCooldownStore inviteCooldowns;
    private RequestStore requests;
    private InviteCooldownStore requestCooldowns;
    private GroupLeadershipRegistry leadership;
    private GroupSyncService groupSync;
    private JoinNotifier joinNotifier;
    private PluginTranslations translations;

    @Override
    public void onEnable() {
        PluginSettings settings = PluginSettings.load(this);
        Clock clock = Clock.systemUTC();
        invites = new InviteStore(clock, settings.inviteExpiration(), new InviteStore.SecureTokenGenerator());
        inviteCooldowns = new InviteCooldownStore(clock, settings.inviteCooldown());
        requests = new RequestStore(clock, settings.requestExpiration());
        requestCooldowns = new InviteCooldownStore(clock, settings.requestCooldown());
        leadership = new GroupLeadershipRegistry();
        groupSync = new GroupSyncService(this, leadership);
        joinNotifier = new JoinNotifier(this, leadership);

        BukkitVoicechatService service = getServer().getServicesManager().load(BukkitVoicechatService.class);
        if (service == null) {
            getLogger().severe("Simple Voice Chat API service is unavailable; disabling Simple Voice Chat Group Management.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        translations = new PluginTranslations();
        try {
            translations.register(getClass().getClassLoader());
        } catch (RuntimeException exception) {
            getLogger().log(Level.SEVERE, "Could not load translations; disabling Simple Voice Chat Group Management.", exception);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        service.registerPlugin(new VoiceChatAddon(this, leadership, invites, requests));

        VcGroupCommand commandHandler = new VcGroupCommand(
                this,
                invites,
                leadership,
                inviteCooldowns,
                requests,
                requestCooldowns,
                settings
        );
        PluginCommand command = getCommand("voicegroup");
        if (command == null) {
            getLogger().severe("The /voicegroup command is missing from plugin.yml; disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        command.setExecutor(commandHandler);
        command.setTabCompleter(commandHandler);

        groupSync.register();
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getScheduler().runTaskTimer(this, this::cleanupExpiredState, 20L * 60L, 20L * 60L);
        getLogger().info("Simple Voice Chat Group Management enabled.");
    }

    @Override
    public void onDisable() {
        if (translations != null) {
            translations.unregister();
        }
        clearVoicechatState();
        if (groupSync != null) {
            groupSync.unregister();
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        if (leadership != null) {
            publishLeadership(leadership.disconnect(playerId));
        }
        if (groupSync != null) {
            groupSync.forget(playerId);
        }
        if (invites != null) {
            invites.invalidatePlayer(playerId);
        }
        if (inviteCooldowns != null) {
            inviteCooldowns.invalidate(playerId);
        }
        if (requests != null) {
            requests.invalidateRequester(playerId);
        }
        if (requestCooldowns != null) {
            requestCooldowns.invalidate(playerId);
        }
    }

    VoicechatServerApi getVoicechatApi() {
        return voicechatApi;
    }

    void setVoicechatApi(VoicechatServerApi voicechatApi) {
        this.voicechatApi = voicechatApi;
    }

    void clearVoicechatState() {
        voicechatApi = null;
        if (invites != null) {
            invites.clear();
        }
        if (leadership != null) {
            publishLeadership(leadership.clear());
        }
        if (inviteCooldowns != null) {
            inviteCooldowns.clear();
        }
        if (requests != null) {
            requests.clear();
        }
        if (requestCooldowns != null) {
            requestCooldowns.clear();
        }
        if (joinNotifier != null) {
            joinNotifier.clear();
        }
    }

    private void cleanupExpiredState() {
        invites.cleanupExpired();
        inviteCooldowns.cleanupExpired();
        requests.cleanupExpired();
        requestCooldowns.cleanupExpired();
    }

    void publishLeadership(GroupLeadershipRegistry.Transition transition) {
        if (groupSync != null) {
            groupSync.publish(transition);
        }
    }

    void notifyGroupJoin(java.util.UUID groupId, java.util.UUID joinerId) {
        if (joinNotifier != null) {
            joinNotifier.onJoin(groupId, joinerId);
        }
    }

    void attributeInvite(java.util.UUID joinerId, String inviterName) {
        if (joinNotifier != null) {
            joinNotifier.attributeInvite(joinerId, inviterName);
        }
    }

    void clearInviteAttribution(java.util.UUID joinerId) {
        if (joinNotifier != null) {
            joinNotifier.clearAttribution(joinerId);
        }
    }
}
