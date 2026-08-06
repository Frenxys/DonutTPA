package github.io.Frenxys.managers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import github.io.Frenxys.Main;
import github.io.Frenxys.gui.TPAGui;
import github.io.Frenxys.models.PlayerSettings;
import github.io.Frenxys.models.TPARequest;
import github.io.Frenxys.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

public class TPAManager {

    private final Main plugin;
    private final Map<UUID, List<TPARequest>> pendingRequests = new ConcurrentHashMap<>();
    private final Map<UUID, PlayerSettings> settingsCache = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> warmupTasks = new ConcurrentHashMap<>();
    private final Map<UUID, Location> warmupLocations = new ConcurrentHashMap<>();
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();
    private final Map<String, BukkitTask> expiryTasks = new ConcurrentHashMap<>();

    public TPAManager(Main plugin) {
        this.plugin = plugin;
    }

    public void loadSettings(Player player) {
        this.settingsCache.put(player.getUniqueId(), this.plugin.getStorageManager().loadSettings(player.getUniqueId()));
    }

    public void saveSettings(Player player) {
        PlayerSettings s = this.settingsCache.get(player.getUniqueId());
        if (s != null) {
            this.plugin.getStorageManager().saveSettings(s);
        }
    }

    public void unloadSettings(Player player) {
        this.saveSettings(player);
        this.settingsCache.remove(player.getUniqueId());
    }

    public PlayerSettings getSettings(UUID uuid) {
        return this.settingsCache.computeIfAbsent(uuid, id -> this.plugin.getStorageManager().loadSettings(id));
    }

    public void initiateTPA(Player sender, Player target, TPARequest.Type type) {
        MessageUtil msg = this.plugin.getMessageUtil();
        if (this.isOnCooldown(sender)) {
            msg.send(sender, "request-cooldown", Map.of("%sec%", String.valueOf(this.getRemainingCooldown(sender))));
            return;
        }
        boolean targetAllows = type == TPARequest.Type.TPA
                ? this.getSettings(target.getUniqueId()).isTpaEnabled()
                : this.getSettings(target.getUniqueId()).isTpaHereEnabled();
        if (!targetAllows && !this.plugin.getPermissionManager().hasPermission(sender, "donuttpa.tpa.bypass")) {
            msg.send(sender, type == TPARequest.Type.TPA ? "block-tpa-request" : "block-tphere-request", Map.of());
            return;
        }
        if (this.hasActiveRequestFrom(sender.getUniqueId())) {
            msg.send(sender, "request-limit-reached", Map.of());
            return;
        }
        TPARequest draft = new TPARequest(sender.getUniqueId(), sender.getName(), target.getUniqueId(), target.getName(), type);
        TPAGui.openSendConfirmGui(this.plugin, sender, draft);
        this.plugin.getSoundUtil().play(sender, "player-head-click");
    }

    public void dispatchRequest(Player sender, TPARequest draft) {
        MessageUtil msg = this.plugin.getMessageUtil();
        Player target = Bukkit.getPlayer(draft.getTargetUUID());
        if (target == null || !target.isOnline()) {
            msg.send(sender, "user-not-found", Map.of("%player%", draft.getTargetName()));
            return;
        }
        this.addRequest(draft);
        this.setCooldown(sender);
        boolean isHere = draft.getType() == TPARequest.Type.TPAHERE;
        msg.send(sender, isHere ? "sent-here-request" : "sent-request", Map.of("%player%", target.getName()));
        msg.send(sender, isHere ? "tpa-here-request-requester-actionbar" : "tpa-request-requester-actionbar", Map.of("%player%", target.getName()));
        if (this.getSettings(target.getUniqueId()).isAutoAccept()) {
            this.executeAccept(draft);
            return;
        }
        msg.sendRequestNotification(target, sender.getName(), isHere);
        this.plugin.getSoundUtil().play(target, "accept/send");
        this.scheduleExpiry(draft);
    }

    public void openAcceptGui(Player target) {
        TPARequest request = this.getLatestRequest(target.getUniqueId());
        if (request == null) {
            this.plugin.getMessageUtil().send(target, "no-active-request", Map.of());
            return;
        }
        TPAGui.openAcceptGui(this.plugin, target, request);
        this.plugin.getSoundUtil().play(target, "player-head-click");
    }

    public void acceptRequest(Player target, TPARequest request) {
        this.executeAccept(request);
    }

    private void executeAccept(TPARequest request) {
        Player sender = Bukkit.getPlayer(request.getSenderUUID());
        Player target = Bukkit.getPlayer(request.getTargetUUID());
        this.removeRequest(request);
        this.cancelExpiry(request);
        if (sender == null || !sender.isOnline() || target == null || !target.isOnline()) {
            return;
        }
        this.plugin.getMessageUtil().send(target, "accepted-teleport", Map.of());
        this.plugin.getMessageUtil().send(sender, "accepted-teleport-sender", Map.of("%player%", target.getName()));
        this.plugin.getSoundUtil().play(target, "accept/send");
        this.plugin.getSoundUtil().play(sender, "accept/send");
        if (request.getType() == TPARequest.Type.TPA) {
            this.startWarmup(sender, target.getLocation());
        } else {
            this.startWarmup(target, sender.getLocation());
        }
    }

    public void denyLatestRequest(Player target) {
        TPARequest request = this.getLatestRequest(target.getUniqueId());
        if (request == null) {
            this.plugin.getMessageUtil().send(target, "no-active-request", Map.of());
            return;
        }
        this.denyRequest(target, request);
    }

    public void denyRequest(Player target, TPARequest request) {
        Player sender = Bukkit.getPlayer(request.getSenderUUID());
        this.removeRequest(request);
        this.cancelExpiry(request);
        this.plugin.getMessageUtil().send(target, "cancelled-request", Map.of("%player%", request.getSenderName()));
        if (sender != null && sender.isOnline()) {
            this.plugin.getMessageUtil().send(sender, "cancelled-request-sender", Map.of("%player%", target.getName()));
            this.plugin.getSoundUtil().play(sender, "decline/cancel");
        }
        this.plugin.getSoundUtil().play(target, "decline/cancel");
    }

    public void cancelRequests(Player sender) {
        List<TPARequest> sent = this.getSentRequests(sender.getUniqueId());
        if (sent.isEmpty()) {
            this.plugin.getMessageUtil().send(sender, "cancel-requests-failed", Map.of());
            return;
        }
        for (TPARequest req : new ArrayList<>(sent)) {
            this.removeRequest(req);
            this.cancelExpiry(req);
        }
        this.plugin.getMessageUtil().send(sender, "cancel-requests", Map.of());
        this.plugin.getSoundUtil().play(sender, "decline/cancel");
    }

    private void startWarmup(final Player player, final Location destination) {
        // Cancel any previous warmup for this player before starting a new one.
        // When two players swap tpa/tpahere and both accept at the same time, the
        // SAME player can end up with two warmup tasks. The second overwrites the
        // map entry but the first keeps running — and since its own cleanup only
        // cancels whatever is in the map, the orphaned task re-teleports the player
        // to the old destination every second, forever.
        this.cleanupWarmup(player.getUniqueId());
        final int seconds = this.plugin.getMainConfig().getConfig().getInt("teleport-warmup", 3);
        if (seconds <= 0) {
            player.teleport(destination);
            this.plugin.getSoundUtil().play(player, "teleport-done");
            return;
        }
        this.warmupLocations.put(player.getUniqueId(), player.getLocation().clone());
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(this.plugin.getHost(), new Runnable() {
            int remaining = seconds;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    TPAManager.this.cleanupWarmup(player.getUniqueId());
                    return;
                }
                if (remaining > 0) {
                    TPAManager.this.plugin.getMessageUtil().send(player, "teleport-countdown", Map.of("%time%", String.valueOf(remaining)));
                    TPAManager.this.plugin.getSoundUtil().play(player, "count-down");
                    --remaining;
                } else {
                    player.teleport(destination);
                    TPAManager.this.plugin.getSoundUtil().play(player, "teleport-done");
                    TPAManager.this.cleanupWarmup(player.getUniqueId());
                }
            }
        }, 0L, 20L);
        this.warmupTasks.put(player.getUniqueId(), task);
    }

    public void cancelWarmup(Player player) {
        BukkitTask task = this.warmupTasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
            this.warmupLocations.remove(player.getUniqueId());
            this.plugin.getMessageUtil().send(player, "teleport-cancelled", Map.of());
        }
    }

    public boolean hasWarmup(Player player) {
        return this.warmupTasks.containsKey(player.getUniqueId());
    }

    private void cleanupWarmup(UUID uuid) {
        BukkitTask t = this.warmupTasks.remove(uuid);
        if (t != null) {
            t.cancel();
        }
        this.warmupLocations.remove(uuid);
    }

    private void scheduleExpiry(TPARequest request) {
        int timeout = this.plugin.getMainConfig().getConfig().getInt("request-timeout", 60);
        String key = this.expiryKey(request);
        BukkitTask task = Bukkit.getScheduler().runTaskLater(this.plugin.getHost(), () -> {
            if (!this.hasRequest(request)) {
                return;
            }
            this.removeRequest(request);
            this.expiryTasks.remove(key);
            Player s = Bukkit.getPlayer(request.getSenderUUID());
            Player t = Bukkit.getPlayer(request.getTargetUUID());
            if (s != null && s.isOnline()) {
                this.plugin.getMessageUtil().send(s, "no-active-request", Map.of());
            }
            if (t != null && t.isOnline()) {
                this.plugin.getMessageUtil().send(t, "no-active-request", Map.of());
            }
        }, (long) timeout * 20L);
        this.expiryTasks.put(key, task);
    }

    private void cancelExpiry(TPARequest request) {
        BukkitTask t = this.expiryTasks.remove(this.expiryKey(request));
        if (t != null) {
            t.cancel();
        }
    }

    private String expiryKey(TPARequest r) {
        return String.valueOf(r.getSenderUUID()) + ":" + String.valueOf(r.getTargetUUID());
    }

    private void setCooldown(Player player) {
        this.cooldowns.put(player.getUniqueId(), System.currentTimeMillis());
    }

    public boolean isOnCooldown(Player player) {
        if (this.plugin.getPermissionManager().hasPermission(player, "donuttpa.admin")) {
            return false;
        }
        Long last = this.cooldowns.get(player.getUniqueId());
        if (last == null) {
            return false;
        }
        int cd = this.plugin.getRequestCooldownConfig().getConfig().getInt("request-cooldown", 1);
        return System.currentTimeMillis() - last < (long) cd * 1000L;
    }

    public long getRemainingCooldown(Player player) {
        Long last = this.cooldowns.get(player.getUniqueId());
        if (last == null) {
            return 0L;
        }
        int cd = this.plugin.getRequestCooldownConfig().getConfig().getInt("request-cooldown", 1);
        return Math.max(0L, (long) cd - (System.currentTimeMillis() - last) / 1000L);
    }

    public boolean toggleTpa(Player p) {
        PlayerSettings s = this.getSettings(p.getUniqueId());
        s.setTpaEnabled(!s.isTpaEnabled());
        this.plugin.getStorageManager().saveSettings(s);
        return s.isTpaEnabled();
    }

    public boolean toggleTpaHere(Player p) {
        PlayerSettings s = this.getSettings(p.getUniqueId());
        s.setTpaHereEnabled(!s.isTpaHereEnabled());
        this.plugin.getStorageManager().saveSettings(s);
        return s.isTpaHereEnabled();
    }

    public boolean toggleAutoAccept(Player p) {
        PlayerSettings s = this.getSettings(p.getUniqueId());
        s.setAutoAccept(!s.isAutoAccept());
        this.plugin.getStorageManager().saveSettings(s);
        return s.isAutoAccept();
    }

    public boolean toggleGui(Player p) {
        PlayerSettings s = this.getSettings(p.getUniqueId());
        s.setGuiEnabled(!s.isGuiEnabled());
        this.plugin.getStorageManager().saveSettings(s);
        return s.isGuiEnabled();
    }

    public List<String> getPendingRequestSenderNames(UUID targetUUID) {
        List<TPARequest> reqs = this.pendingRequests.getOrDefault(targetUUID, List.of());
        List<String> names = new ArrayList<>();
        for (TPARequest r : reqs) {
            names.add(r.getSenderName());
        }
        return names;
    }

    private void addRequest(TPARequest r) {
        this.pendingRequests.computeIfAbsent(r.getTargetUUID(), k -> new ArrayList<>()).add(r);
    }

    private void removeRequest(TPARequest r) {
        List<TPARequest> list = this.pendingRequests.get(r.getTargetUUID());
        if (list != null) {
            list.remove(r);
            if (list.isEmpty()) {
                this.pendingRequests.remove(r.getTargetUUID());
            }
        }
    }

    private boolean hasRequest(TPARequest r) {
        List<TPARequest> list = this.pendingRequests.get(r.getTargetUUID());
        return list != null && list.contains(r);
    }

    public TPARequest getLatestRequest(UUID targetUUID) {
        List<TPARequest> list = this.pendingRequests.get(targetUUID);
        if (list == null || list.isEmpty()) {
            return null;
        }
        return list.get(list.size() - 1);
    }

    private boolean hasActiveRequestFrom(UUID senderUUID) {
        for (List<TPARequest> list : this.pendingRequests.values()) {
            for (TPARequest r : list) {
                if (r.getSenderUUID().equals(senderUUID)) {
                    return true;
                }
            }
        }
        return false;
    }

    private List<TPARequest> getSentRequests(UUID senderUUID) {
        List<TPARequest> result = new ArrayList<>();
        for (List<TPARequest> list : this.pendingRequests.values()) {
            for (TPARequest r : list) {
                if (r.getSenderUUID().equals(senderUUID)) {
                    result.add(r);
                }
            }
        }
        return result;
    }

    public void cancelAllRequests() {
        this.warmupTasks.values().forEach(BukkitTask::cancel);
        this.expiryTasks.values().forEach(BukkitTask::cancel);
        this.warmupTasks.clear();
        this.expiryTasks.clear();
        this.pendingRequests.clear();
        this.cooldowns.clear();
    }
}
