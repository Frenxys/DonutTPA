package github.io.Frenxys.commands;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import github.io.Frenxys.Main;
import github.io.Frenxys.models.TPARequest;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public class TpaHereCommand implements CommandExecutor, TabCompleter {

    private final Main plugin;

    public TpaHereCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            this.plugin.getMessageUtil().sendConsole(sender, "not-player", Map.of());
            return true;
        }
        Player player = (Player) sender;
        if (!this.plugin.getPermissionManager().hasPermission(player, "donuttpa.tpahere")) {
            this.plugin.getMessageUtil().send(player, "no-permission", Map.of());
            return true;
        }
        if (args.length < 1) {
            this.plugin.getMessageUtil().send(player, "tpahere-usage", Map.of());
            return true;
        }
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            this.plugin.getMessageUtil().send(player, "user-not-found", Map.of("%player%", args[0]));
            return true;
        }
        if (target.equals(player)) {
            this.plugin.getMessageUtil().send(player, "self-teleport", Map.of());
            return true;
        }
        this.plugin.getTpaManager().initiateTPA(player, target, TPARequest.Type.TPAHERE);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 1) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}
