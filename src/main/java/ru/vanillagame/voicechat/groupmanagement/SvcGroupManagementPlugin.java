package ru.vanillagame.voicechat.groupmanagement;

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

public final class SvcGroupManagementPlugin extends JavaPlugin implements Listener {

    private volatile VoicechatServerApi voicechatApi;
    private InviteStore invites;
    private InviteCooldownStore inviteCooldowns;
    private GroupLeadershipRegistry leadership;
    private LeaderSyncService leaderSync;
    private PluginTranslations translations;

    @Override
    public void onEnable() {
        PluginSettings settings = PluginSettings.load(this);
        Clock clock = Clock.systemUTC();
        invites = new InviteStore(clock, settings.inviteExpiration(), new InviteStore.SecureTokenGenerator());
        inviteCooldowns = new InviteCooldownStore(clock, settings.inviteCooldown());
        leadership = new GroupLeadershipRegistry();
        leaderSync = new LeaderSyncService(this, leadership);

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

        service.registerPlugin(new VoiceChatAddon(this, leadership, invites));

        VcGroupCommand commandHandler = new VcGroupCommand(
                this,
                invites,
                leadership,
                inviteCooldowns,
                settings.inviteExpirationMinutes()
        );
        PluginCommand command = getCommand("vcgroup");
        if (command == null) {
            getLogger().severe("The /vcgroup command is missing from plugin.yml; disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        command.setExecutor(commandHandler);
        command.setTabCompleter(commandHandler);

        leaderSync.register();
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
        if (leaderSync != null) {
            leaderSync.unregister();
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        if (leadership != null) {
            publishLeadership(leadership.disconnect(playerId));
        }
        if (leaderSync != null) {
            leaderSync.forget(playerId);
        }
        if (invites != null) {
            invites.invalidatePlayer(playerId);
        }
        if (inviteCooldowns != null) {
            inviteCooldowns.invalidate(playerId);
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
    }

    private void cleanupExpiredState() {
        invites.cleanupExpired();
        inviteCooldowns.cleanupExpired();
    }

    void publishLeadership(GroupLeadershipRegistry.Transition transition) {
        if (leaderSync != null) {
            leaderSync.publish(transition);
        }
    }
}
