package github.io.Frenxys.commands;

import java.util.Map;
import github.io.Frenxys.Main;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TpaHereToggleCommand implements CommandExecutor {

    private final Main plugin;

    public TpaHereToggleCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            this.plugin.getMessageUtil().sendConsole(sender, "not-player", Map.of());
            return true;
        }
        Player player = (Player) sender;
        if (!this.plugin.getPermissionManager().hasPermission(player, "donuttpa.tpaheretoggle")) {
            this.plugin.getMessageUtil().send(player, "no-permission", Map.of());
            return true;
        }
        boolean on = this.plugin.getTpaManager().toggleTpaHere(player);
        this.plugin.getMessageUtil().send(player, on ? "tpa-here-toggle-on" : "tpa-here-toggle-off", Map.of());
        return true;
    }
}
