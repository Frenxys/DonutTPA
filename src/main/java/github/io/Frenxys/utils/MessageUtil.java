package github.io.Frenxys.utils;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import github.io.Frenxys.Main;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public class MessageUtil {

    private final Main plugin;

    public MessageUtil(Main plugin) {
        this.plugin = plugin;
    }

    public void send(Player player, String messageKey, Map<String, String> placeholders) {
        ConfigurationSection section = this.plugin.getMessagesConfig().getConfig().getConfigurationSection("messages." + messageKey + ".module");
        if (section == null) {
            return;
        }
        this.sendModule(player, section, "chat", placeholders);
        this.sendModule(player, section, "actionbar", placeholders);
        this.sendModule(player, section, "title", placeholders);
    }

    public void sendConsole(CommandSender sender, String messageKey, Map<String, String> placeholders) {
        if (sender instanceof Player) {
            this.send((Player) sender, messageKey, placeholders);
            return;
        }
        ConfigurationSection section = this.plugin.getMessagesConfig().getConfig().getConfigurationSection("messages." + messageKey + ".module.chat");
        if (section == null || !section.getBoolean("enabled", false)) {
            return;
        }
        List<String> lines = section.getStringList("message");
        for (String line : lines) {
            if (line.isEmpty()) {
                continue;
            }
            sender.sendMessage(ColorUtil.color(this.applyPlaceholders(line, placeholders)));
        }
    }

    public void sendRequestNotification(Player target, String requesterName, boolean isHere) {
        String msgKey = isHere ? "receive-here-request" : "receive-request";
        String hoverKey = isHere ? "receive-here-request-hover" : "receive-request-hover";
        String hereWord = this.plugin.getMessagesConfig().getConfig().getString("messages.here", "&#00f986Here");
        ConfigurationSection section = this.plugin.getMessagesConfig().getConfig().getConfigurationSection("messages." + msgKey + ".module.chat");
        if (section == null || !section.getBoolean("enabled", false)) {
            return;
        }
        String hoverText = this.plugin.getMessagesConfig().getConfig().getString("messages." + hoverKey, "&fClick here to accept");
        List<String> lines = section.getStringList("message");
        for (String raw : lines) {
            if (raw.isEmpty()) {
                target.sendMessage(Component.empty());
                continue;
            }
            raw = raw.replace("%player%", requesterName).replace("%here%", "{HERE_PLACEHOLDER}");
            if (raw.contains("{HERE_PLACEHOLDER}")) {
                String[] parts = raw.split("\\{HERE_PLACEHOLDER\\}", 2);
                Component before = MessageUtil.legacy(ColorUtil.color(parts[0]));
                Component after = parts.length > 1 ? MessageUtil.legacy(ColorUtil.color(parts[1])) : Component.empty();
                Component hereComponent = MessageUtil.legacy(ColorUtil.color(hereWord))
                        .clickEvent(ClickEvent.runCommand("/tpaccept"))
                        .hoverEvent(HoverEvent.showText(MessageUtil.legacy(ColorUtil.color(hoverText))));
                target.sendMessage(before.append(hereComponent).append(after));
                continue;
            }
            target.sendMessage(MessageUtil.legacy(ColorUtil.color(raw)));
        }
        ConfigurationSection abSection = this.plugin.getMessagesConfig().getConfig().getConfigurationSection("messages." + msgKey + ".module.actionbar");
        if (abSection != null && abSection.getBoolean("enabled", false)) {
            String abMsg = String.join(" ", abSection.getStringList("message")).replace("%player%", requesterName);
            if (!abMsg.isEmpty()) {
                target.sendActionBar(MessageUtil.legacy(ColorUtil.color(abMsg)));
            }
        }
    }

    private void sendModule(Player player, ConfigurationSection parent, String module, Map<String, String> placeholders) {
        ConfigurationSection mod = parent.getConfigurationSection(module);
        if (mod == null || !mod.getBoolean("enabled", false)) {
            return;
        }
        List<String> lines = mod.getStringList("message");
        if (lines.isEmpty()) {
            return;
        }
        switch (module) {
            case "chat": {
                for (String line : lines) {
                    if (line.isEmpty()) {
                        player.sendMessage(Component.empty());
                        continue;
                    }
                    player.sendMessage(MessageUtil.legacy(ColorUtil.color(this.applyPlaceholders(line, placeholders))));
                }
                break;
            }
            case "actionbar": {
                String msg = String.join(" ", lines);
                if (msg.isEmpty()) {
                    break;
                }
                player.sendActionBar(MessageUtil.legacy(ColorUtil.color(this.applyPlaceholders(msg, placeholders))));
                break;
            }
            case "title": {
                ConfigurationSection sub = parent.getConfigurationSection("subtitle");
                String titleStr = this.applyPlaceholders(String.join(" ", lines), placeholders);
                String subStr = "";
                if (sub != null && sub.getBoolean("enabled", false)) {
                    subStr = this.applyPlaceholders(String.join(" ", sub.getStringList("message")), placeholders);
                }
                player.showTitle(Title.title(
                        MessageUtil.legacy(ColorUtil.color(titleStr)),
                        MessageUtil.legacy(ColorUtil.color(subStr)),
                        Title.Times.times(Duration.ofMillis(500L), Duration.ofSeconds(2L), Duration.ofMillis(500L))));
                break;
            }
            default:
                break;
        }
    }

    private String applyPlaceholders(String text, Map<String, String> placeholders) {
        if (placeholders == null) {
            return text;
        }
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            text = text.replace(entry.getKey(), entry.getValue());
        }
        return text;
    }

    private static Component legacy(String text) {
        return LegacyComponentSerializer.legacySection().deserialize(text);
    }
}
