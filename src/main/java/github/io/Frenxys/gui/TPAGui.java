package github.io.Frenxys.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import github.io.Frenxys.Main;
import github.io.Frenxys.models.TPARequest;
import github.io.Frenxys.utils.ColorUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;

public class TPAGui {

    public static final String META_SEND = "donuttpa_send_confirm";
    public static final String META_ACCEPT = "donuttpa_accept";

    public static void openSendConfirmGui(Main plugin, Player sender, TPARequest draft) {
        boolean isHere = draft.getType() == TPARequest.Type.TPAHERE;
        FileConfiguration cfg = isHere ? plugin.getTpaHereSendGuiConfig().getConfig() : plugin.getTpaSendGuiConfig().getConfig();
        String rootKey = isHere ? "request-inventory" : "tpa-request-inventory";
        Player other = Bukkit.getPlayer(draft.getTargetUUID());
        TPAGui.buildAndOpen(plugin, sender, draft, cfg, rootKey, other, META_SEND);
    }

    public static void openAcceptGui(Main plugin, Player target, TPARequest request) {
        boolean isHere = request.getType() == TPARequest.Type.TPAHERE;
        FileConfiguration cfg = isHere ? plugin.getTpaHereAcceptGuiConfig().getConfig() : plugin.getTpaAcceptGuiConfig().getConfig();
        String rootKey = isHere ? "accept-inventory" : "tpa-accept-inventory";
        Player other = Bukkit.getPlayer(request.getSenderUUID());
        TPAGui.buildAndOpen(plugin, target, request, cfg, rootKey, other, META_ACCEPT);
    }

    private static void buildAndOpen(Main plugin, Player viewer, TPARequest request, FileConfiguration cfg, String rootKey, Player otherPlayer, String metaKey) {
        String rawTitle = cfg.getString(rootKey + ".name", "&8TPA");
        int rows = cfg.getInt(rootKey + ".rows", 3);
        int size = rows * 9;
        Inventory inv = Bukkit.createInventory(null, size, TPAGui.legacy(ColorUtil.color(rawTitle)));
        ConfigurationSection icons = cfg.getConfigurationSection(rootKey + ".icons");
        if (icons != null) {
            for (String iconKey : icons.getKeys(false)) {
                ConfigurationSection icon = icons.getConfigurationSection(iconKey);
                if (icon == null) {
                    continue;
                }
                int slot = icon.getInt("slot", -1);
                if (slot < 0 || slot >= size) {
                    continue;
                }
                String matName = icon.getString("material", "STONE");
                String dispName = ColorUtil.color(icon.getString("display-name", ""));
                List<String> rawLore = icon.getStringList("lore");
                String otherName = otherPlayer != null ? otherPlayer.getName() : (request.getSenderUUID().equals(viewer.getUniqueId()) ? request.getTargetName() : request.getSenderName());
                boolean otherFlying = otherPlayer != null && otherPlayer.isFlying();
                String flyingStr = otherFlying ? "&aYes" : "&cNo";
                String worldRaw = otherPlayer != null ? otherPlayer.getWorld().getName() : viewer.getWorld().getName();
                String worldNick = TPAGui.getWorldNick(plugin, worldRaw);
                dispName = dispName.replace("%player%", otherName);
                List<String> lore = new ArrayList<>();
                for (String l : rawLore) {
                    lore.add(ColorUtil.color(l.replace("%player%", otherName).replace("%world%", worldNick).replace("%is_flying%", flyingStr)));
                }
                Material mat;
                try {
                    mat = Material.valueOf(matName.toUpperCase());
                } catch (IllegalArgumentException e) {
                    mat = Material.STONE;
                }
                ItemStack item = mat == Material.PLAYER_HEAD ? TPAGui.buildHead(otherPlayer, dispName, lore) : TPAGui.buildItem(mat, dispName, lore);
                inv.setItem(slot, item);
            }
        }
        viewer.openInventory(inv);
        viewer.setMetadata(metaKey, new FixedMetadataValue(plugin.getHost(), request));
    }

    public static int getSendConfirmSlot(Main plugin, TPARequest draft) {
        boolean isHere = draft.getType() == TPARequest.Type.TPAHERE;
        FileConfiguration cfg = isHere ? plugin.getTpaHereSendGuiConfig().getConfig() : plugin.getTpaSendGuiConfig().getConfig();
        String root = isHere ? "request-inventory" : "tpa-request-inventory";
        return cfg.getInt(root + ".icons.confirm-icon.slot", 16);
    }

    public static int getSendCancelSlot(Main plugin, TPARequest draft) {
        boolean isHere = draft.getType() == TPARequest.Type.TPAHERE;
        FileConfiguration cfg = isHere ? plugin.getTpaHereSendGuiConfig().getConfig() : plugin.getTpaSendGuiConfig().getConfig();
        String root = isHere ? "request-inventory" : "tpa-request-inventory";
        return cfg.getInt(root + ".icons.cancel-icon.slot", 10);
    }

    public static int getAcceptConfirmSlot(Main plugin, TPARequest request) {
        boolean isHere = request.getType() == TPARequest.Type.TPAHERE;
        FileConfiguration cfg = isHere ? plugin.getTpaHereAcceptGuiConfig().getConfig() : plugin.getTpaAcceptGuiConfig().getConfig();
        String root = isHere ? "accept-inventory" : "tpa-accept-inventory";
        return cfg.getInt(root + ".icons.confirm-icon.slot", 16);
    }

    public static int getAcceptCancelSlot(Main plugin, TPARequest request) {
        boolean isHere = request.getType() == TPARequest.Type.TPAHERE;
        FileConfiguration cfg = isHere ? plugin.getTpaHereAcceptGuiConfig().getConfig() : plugin.getTpaAcceptGuiConfig().getConfig();
        String root = isHere ? "accept-inventory" : "tpa-accept-inventory";
        return cfg.getInt(root + ".icons.cancel-icon.slot", 10);
    }

    public static boolean isOurGui(Main plugin, String title) {
        String[] cfgTitles = new String[]{
                plugin.getTpaSendGuiConfig().getConfig().getString("tpa-request-inventory.name", ""),
                plugin.getTpaHereSendGuiConfig().getConfig().getString("request-inventory.name", ""),
                plugin.getTpaAcceptGuiConfig().getConfig().getString("tpa-accept-inventory.name", ""),
                plugin.getTpaHereAcceptGuiConfig().getConfig().getString("accept-inventory.name", "")
        };
        for (String raw : cfgTitles) {
            if (title.equals(ColorUtil.color(raw))) {
                return true;
            }
        }
        return false;
    }

    private static ItemStack buildItem(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        meta.displayName(TPAGui.legacy(name));
        if (!lore.isEmpty()) {
            meta.lore(lore.stream().map(TPAGui::legacy).toList());
        }
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack buildHead(Player player, String name, List<String> lore) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta == null) {
            return head;
        }
        if (player != null) {
            meta.setOwningPlayer(player);
        }
        meta.displayName(TPAGui.legacy(name));
        if (!lore.isEmpty()) {
            meta.lore(lore.stream().map(TPAGui::legacy).toList());
        }
        head.setItemMeta(meta);
        return head;
    }

    private static String getWorldNick(Main plugin, String worldName) {
        return ColorUtil.color(plugin.getWorldNickConfig().getConfig().getString("world-nicknames." + worldName, "&7" + worldName));
    }

    private static Component legacy(String text) {
        return LegacyComponentSerializer.legacySection().deserialize(text);
    }
}
