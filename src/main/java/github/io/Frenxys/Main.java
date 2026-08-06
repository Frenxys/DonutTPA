package github.io.Frenxys;

import github.io.Frenxys.commands.TpAcceptCommand;
import github.io.Frenxys.commands.TpaAutoCommand;
import github.io.Frenxys.commands.TpaCancelCommand;
import github.io.Frenxys.commands.TpaCommand;
import github.io.Frenxys.commands.TpaDenyCommand;
import github.io.Frenxys.commands.TpaGuiToggleCommand;
import github.io.Frenxys.commands.TpaHereCommand;
import github.io.Frenxys.commands.TpaHereToggleCommand;
import github.io.Frenxys.commands.TpaReloadCommand;
import github.io.Frenxys.commands.TpaToggleCommand;
import github.io.Frenxys.config.BaseConfig;
import github.io.Frenxys.config.GuiConfig;
import github.io.Frenxys.listeners.GuiClickListener;
import github.io.Frenxys.listeners.PlayerListener;
import github.io.Frenxys.managers.PermissionManager;
import github.io.Frenxys.managers.TPAManager;
import github.io.Frenxys.storage.StorageManager;
import github.io.Frenxys.utils.MessageUtil;
import github.io.Frenxys.utils.SoundUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * NA — TPA plugin by Enea (github.com/Frenxys).
 * Based on DonutTpa v13 by SqidgeonStudios.
 */
public final class Main extends JavaPlugin {

    private static Main instance;

    private BaseConfig mainConfig;
    private BaseConfig messagesConfig;
    private BaseConfig soundsConfig;
    private BaseConfig requestCooldownConfig;
    private BaseConfig worldNickConfig;
    private GuiConfig tpaSendGuiConfig;
    private GuiConfig tpaAcceptGuiConfig;
    private GuiConfig tpaHereSendGuiConfig;
    private GuiConfig tpaHereAcceptGuiConfig;
    private StorageManager storageManager;
    private PermissionManager permissionManager;
    private TPAManager tpaManager;
    private MessageUtil messageUtil;
    private SoundUtil soundUtil;

    @Override
    public void onEnable() {
        this.enable();
    }

    @Override
    public void onDisable() {
        this.disable();
    }

    public void enable() {
        instance = this;
        this.loadConfigs();
        this.storageManager = new StorageManager(this);
        this.storageManager.init();
        this.permissionManager = new PermissionManager(this);
        this.tpaManager = new TPAManager(this);
        this.messageUtil = new MessageUtil(this);
        this.soundUtil = new SoundUtil(this);
        this.registerCommands();
        this.registerListeners();
        for (Player p : Bukkit.getOnlinePlayers()) {
            this.tpaManager.loadSettings(p);
        }
        this.getLogger().info("NA v" + this.getDescription().getVersion() + " enabled!");
        this.getLogger().info("LuckPerms: " + (this.permissionManager.isEnabled() ? "hooked" : "using Bukkit perms"));
    }

    public void disable() {
        if (this.tpaManager != null) {
            this.tpaManager.cancelAllRequests();
        }
        if (this.storageManager != null) {
            this.storageManager.close();
        }
        instance = null;
        this.getLogger().info("NA disabled.");
    }

    private void loadConfigs() {
        // Files are prefixed with tpa-* so they stay grouped in the data folder.
        this.mainConfig = new BaseConfig(this, "tpa-config.yml");
        this.messagesConfig = new BaseConfig(this, "tpa-messages.yml");
        this.soundsConfig = new BaseConfig(this, "tpa-sounds.yml");
        this.requestCooldownConfig = new BaseConfig(this, "tpa-request-cooldown.yml");
        this.worldNickConfig = new BaseConfig(this, "tpa-world-nick.yml");
        this.tpaSendGuiConfig = new GuiConfig(this, "tpa-gui/tpa-send.yml");
        this.tpaAcceptGuiConfig = new GuiConfig(this, "tpa-gui/tpa-accept.yml");
        this.tpaHereSendGuiConfig = new GuiConfig(this, "tpa-gui/tpa-here-send.yml");
        this.tpaHereAcceptGuiConfig = new GuiConfig(this, "tpa-gui/tpa-here-accept.yml");
    }

    public void reloadAllConfigs() {
        this.mainConfig.reload();
        this.messagesConfig.reload();
        this.soundsConfig.reload();
        this.requestCooldownConfig.reload();
        this.worldNickConfig.reload();
        this.tpaSendGuiConfig.reload();
        this.tpaAcceptGuiConfig.reload();
        this.tpaHereSendGuiConfig.reload();
        this.tpaHereAcceptGuiConfig.reload();
    }

    private void registerCommands() {
        TpaCommand tpaCmd = new TpaCommand(this);
        this.getCommand("tpa").setExecutor(tpaCmd);
        this.getCommand("tpa").setTabCompleter(tpaCmd);
        TpaHereCommand tpaHereCmd = new TpaHereCommand(this);
        this.getCommand("tpahere").setExecutor(tpaHereCmd);
        this.getCommand("tpahere").setTabCompleter(tpaHereCmd);
        TpAcceptCommand tpAcceptCmd = new TpAcceptCommand(this);
        this.getCommand("tpaccept").setExecutor(tpAcceptCmd);
        this.getCommand("tpaccept").setTabCompleter(tpAcceptCmd);
        this.getCommand("tpadeny").setExecutor(new TpaDenyCommand(this));
        this.getCommand("tpacancel").setExecutor(new TpaCancelCommand(this));
        this.getCommand("tpatoggle").setExecutor(new TpaToggleCommand(this));
        this.getCommand("tpaheretoggle").setExecutor(new TpaHereToggleCommand(this));
        this.getCommand("tpaauto").setExecutor(new TpaAutoCommand(this));
        this.getCommand("tpaguitoggle").setExecutor(new TpaGuiToggleCommand(this));
        this.getCommand("tpareload").setExecutor(new TpaReloadCommand(this));
    }

    private void registerListeners() {
        this.getServer().getPluginManager().registerEvents(new GuiClickListener(this), this);
        this.getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
    }

    /** Kept so the engine classes (which receive this plugin) can resolve the host. */
    public JavaPlugin getHost() {
        return this;
    }

    public static Main getInstance() {
        return instance;
    }

    public BaseConfig getMainConfig() {
        return mainConfig;
    }

    public BaseConfig getMessagesConfig() {
        return messagesConfig;
    }

    public BaseConfig getSoundsConfig() {
        return soundsConfig;
    }

    public BaseConfig getRequestCooldownConfig() {
        return requestCooldownConfig;
    }

    public BaseConfig getWorldNickConfig() {
        return worldNickConfig;
    }

    public GuiConfig getTpaSendGuiConfig() {
        return tpaSendGuiConfig;
    }

    public GuiConfig getTpaAcceptGuiConfig() {
        return tpaAcceptGuiConfig;
    }

    public GuiConfig getTpaHereSendGuiConfig() {
        return tpaHereSendGuiConfig;
    }

    public GuiConfig getTpaHereAcceptGuiConfig() {
        return tpaHereAcceptGuiConfig;
    }

    public StorageManager getStorageManager() {
        return storageManager;
    }

    public PermissionManager getPermissionManager() {
        return permissionManager;
    }

    public TPAManager getTpaManager() {
        return tpaManager;
    }

    public MessageUtil getMessageUtil() {
        return messageUtil;
    }

    public SoundUtil getSoundUtil() {
        return soundUtil;
    }
}
