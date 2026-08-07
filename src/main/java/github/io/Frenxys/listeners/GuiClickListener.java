package github.io.Frenxys.listeners;

import java.util.List;
import java.util.Map;
import github.io.Frenxys.Main;
import github.io.Frenxys.gui.TPAGui;
import github.io.Frenxys.models.TPARequest;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;

public class GuiClickListener implements Listener {

    private final Main plugin;

    public GuiClickListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        HumanEntity humanEntity = event.getWhoClicked();
        if (!(humanEntity instanceof Player)) {
            return;
        }
        Player player = (Player) humanEntity;
        String title = LegacyComponentSerializer.legacySection().serialize(event.getView().title());
        if (!TPAGui.isOurGui(this.plugin, title)) {
            return;
        }
        // Always cancel clicks on our GUIs, even when the metadata is missing.
        // Without this, a second /tpa while the GUI is open (which re-opens the
        // GUI and triggers the metadata race) leaves the GUI with no metadata:
        // clicks were not cancelled and the decorative icons (grass block,
        // feather, ...) ended up in the inventory as loot (dupe).
        event.setCancelled(true);
        boolean hasSend = player.hasMetadata(TPAGui.META_SEND);
        boolean hasAccept = player.hasMetadata(TPAGui.META_ACCEPT);
        if (!hasSend && !hasAccept) {
            return; // Our GUI but state lost (race) → cancel and do nothing, no loot
        }
        int slot = event.getRawSlot();
        if (hasSend) {
            List<MetadataValue> meta = player.getMetadata(TPAGui.META_SEND);
            if (meta.isEmpty()) {
                return;
            }
            TPARequest draft = (TPARequest) meta.get(0).value();
            if (draft == null) {
                return;
            }
            int confirmSlot = TPAGui.getSendConfirmSlot(this.plugin, draft);
            int cancelSlot = TPAGui.getSendCancelSlot(this.plugin, draft);
            if (slot == confirmSlot) {
                player.closeInventory();
                player.removeMetadata(TPAGui.META_SEND, this.plugin.getHost());
                this.plugin.getSoundUtil().play(player, "accept/send");
                this.plugin.getTpaManager().dispatchRequest(player, draft);
            } else if (slot == cancelSlot) {
                player.closeInventory();
                player.removeMetadata(TPAGui.META_SEND, this.plugin.getHost());
                this.plugin.getSoundUtil().play(player, "decline/cancel");
                this.plugin.getMessageUtil().send(player, "cancel-requests", Map.of());
            }
            return;
        }
        if (hasAccept) {
            List<MetadataValue> meta = player.getMetadata(TPAGui.META_ACCEPT);
            if (meta.isEmpty()) {
                return;
            }
            TPARequest request = (TPARequest) meta.get(0).value();
            if (request == null) {
                return;
            }
            int confirmSlot = TPAGui.getAcceptConfirmSlot(this.plugin, request);
            int cancelSlot = TPAGui.getAcceptCancelSlot(this.plugin, request);
            if (slot == confirmSlot) {
                player.closeInventory();
                player.removeMetadata(TPAGui.META_ACCEPT, this.plugin.getHost());
                this.plugin.getSoundUtil().play(player, "accept/send");
                this.plugin.getTpaManager().acceptRequest(player, request);
            } else if (slot == cancelSlot) {
                player.closeInventory();
                player.removeMetadata(TPAGui.META_ACCEPT, this.plugin.getHost());
                this.plugin.getSoundUtil().play(player, "decline/cancel");
                this.plugin.getTpaManager().denyRequest(player, request);
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        HumanEntity humanEntity = event.getPlayer();
        if (!(humanEntity instanceof Player)) {
            return;
        }
        Player player = (Player) humanEntity;
        // SYNCHRONOUS metadata removal: with runTaskLater(1L) the removal
        // arrived one tick AFTER the setMetadata of a possible GUI re-open
        // (e.g. a second /tpa while the GUI is already open), leaving the GUI
        // open with no metadata and therefore clickable (dupe).
        if (player.hasMetadata(TPAGui.META_SEND)) {
            player.removeMetadata(TPAGui.META_SEND, this.plugin.getHost());
        }
        if (player.hasMetadata(TPAGui.META_ACCEPT)) {
            player.removeMetadata(TPAGui.META_ACCEPT, this.plugin.getHost());
        }
    }
}
