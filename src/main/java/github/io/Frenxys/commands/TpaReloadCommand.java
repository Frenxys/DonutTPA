package github.io.Frenxys.commands;

import java.util.Map;
import github.io.Frenxys.Main;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TpaReloadCommand implements CommandExecutor {

    private final Main plugin;

    public TpaReloadCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!this.plugin.getPermissionManager().hasPermission(sender, "donuttpa.tpareload")) {
            if (sender instanceof Player) {
                Player p = (Player) sender;
                this.plugin.getMessageUtil().send(p, "no-permission", Map.of());
            }
            return true;
        }
        long start = System.currentTimeMillis();
        this.plugin.reloadAllConfigs();
        long ms = System.currentTimeMillis() - start;
        this.plugin.getMessageUtil().sendConsole(sender, "config-reload", Map.of("%ms%", String.valueOf(ms)));
        return true;
    }
}
