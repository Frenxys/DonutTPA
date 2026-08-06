package github.io.Frenxys.commands;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import github.io.Frenxys.Main;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public class TpAcceptCommand implements CommandExecutor, TabCompleter {

    private final Main plugin;

    public TpAcceptCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            this.plugin.getMessageUtil().sendConsole(sender, "not-player", Map.of());
            return true;
        }
        Player player = (Player) sender;
        if (!this.plugin.getPermissionManager().hasPermission(player, "donuttpa.tpaccept")) {
            this.plugin.getMessageUtil().send(player, "no-permission", Map.of());
            return true;
        }
        this.plugin.getTpaManager().openAcceptGui(player);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 1 && sender instanceof Player) {
            Player p = (Player) sender;
            return this.plugin.getTpaManager().getPendingRequestSenderNames(p.getUniqueId()).stream()
                    .filter(n -> n.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}
