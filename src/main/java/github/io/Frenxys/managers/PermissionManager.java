package github.io.Frenxys.managers;

import github.io.Frenxys.Main;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import net.luckperms.api.query.QueryOptions;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public class PermissionManager {

    private final Main plugin;
    private LuckPerms luckPerms;
    private final boolean enabled;

    public PermissionManager(Main plugin) {
        this.plugin = plugin;
        boolean hooked = false;
        if (plugin.getServer().getPluginManager().getPlugin("LuckPerms") != null) {
            try {
                RegisteredServiceProvider<LuckPerms> provider = plugin.getServer().getServicesManager().getRegistration(LuckPerms.class);
                if (provider != null) {
                    this.luckPerms = provider.getProvider();
                    hooked = true;
                    plugin.getLogger().info("LuckPerms hooked successfully.");
                }
            } catch (Exception e) {
                plugin.getLogger().warning("LuckPerms hook failed: " + e.getMessage());
            }
        } else {
            plugin.getLogger().info("LuckPerms not found — using Bukkit permissions.");
        }
        this.enabled = hooked;
    }

    public boolean hasPermission(CommandSender sender, String permission) {
        if (sender.isOp()) {
            return true;
        }
        if (this.enabled && this.luckPerms != null && sender instanceof Player) {
            Player player = (Player) sender;
            try {
                User user = this.luckPerms.getUserManager().getUser(player.getUniqueId());
                if (user != null) {
                    return user.getCachedData().getPermissionData(QueryOptions.defaultContextualOptions()).checkPermission(permission).asBoolean();
                }
            } catch (Exception e) {
                this.plugin.getLogger().warning("LuckPerms check failed for " + sender.getName() + ": " + e.getMessage());
            }
        }
        return sender.hasPermission(permission);
    }

    public String getPrimaryGroup(Player player) {
        if (this.enabled && this.luckPerms != null) {
            try {
                User user = this.luckPerms.getUserManager().getUser(player.getUniqueId());
                if (user != null) {
                    return user.getPrimaryGroup();
                }
            } catch (Exception ignored) {
            }
        }
        return "default";
    }

    public boolean isEnabled() {
        return this.enabled;
    }
}
