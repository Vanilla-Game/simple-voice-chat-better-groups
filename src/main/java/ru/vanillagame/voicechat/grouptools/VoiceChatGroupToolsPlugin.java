package ru.vanillagame.voicechat.grouptools;

import de.maxhenkel.voicechat.api.BukkitVoicechatService;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Clock;
import java.util.logging.Level;

public final class VoiceChatGroupToolsPlugin extends JavaPlugin implements Listener {

    private volatile VoicechatServerApi voicechatApi;
    private InviteStore invites;
    private GroupOwnershipRegistry ownership;
    private PluginTranslations translations;

    @Override
    public void onEnable() {
        invites = new InviteStore(Clock.systemUTC());
        ownership = new GroupOwnershipRegistry();

        BukkitVoicechatService service = getServer().getServicesManager().load(BukkitVoicechatService.class);
        if (service == null) {
            getLogger().severe("Simple Voice Chat API service is unavailable; disabling Voice Chat Group Tools.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        translations = new PluginTranslations();
        try {
            translations.register(getClass().getClassLoader());
        } catch (RuntimeException exception) {
            getLogger().log(Level.SEVERE, "Could not load translations; disabling Voice Chat Group Tools.", exception);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        service.registerPlugin(new VoiceChatAddon(this, ownership, invites));

        VcGroupCommand commandHandler = new VcGroupCommand(this, invites, ownership);
        PluginCommand command = getCommand("vcgroup");
        if (command == null) {
            getLogger().severe("The /vcgroup command is missing from plugin.yml; disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        command.setExecutor(commandHandler);
        command.setTabCompleter(commandHandler);

        getServer().getPluginManager().registerEvents(this, this);
        getServer().getScheduler().runTaskTimer(this, invites::cleanupExpired, 20L * 60L, 20L * 60L);
        getLogger().info("Voice Chat Group Tools enabled.");
    }

    @Override
    public void onDisable() {
        if (translations != null) {
            translations.unregister();
        }
        clearVoicechatState();
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (invites != null) {
            invites.invalidatePlayer(event.getPlayer().getUniqueId());
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
        if (ownership != null) {
            ownership.clear();
        }
    }
}
