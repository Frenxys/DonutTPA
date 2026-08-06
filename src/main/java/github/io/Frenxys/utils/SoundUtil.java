package github.io.Frenxys.utils;

import github.io.Frenxys.Main;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public class SoundUtil {

    private final Main plugin;

    public SoundUtil(Main plugin) {
        this.plugin = plugin;
    }

    public void play(Player player, String soundKey) {
        ConfigurationSection section = this.plugin.getSoundsConfig().getConfig().getConfigurationSection("sounds." + soundKey);
        if (section == null) {
            return;
        }
        if (!section.getBoolean("enabled", true)) {
            return;
        }
        String soundName = section.getString("sound", "");
        float volume = (float) section.getDouble("volume", 1.0);
        float pitch = (float) section.getDouble("pitch", 1.0);
        if (soundName.isEmpty()) {
            return;
        }
        try {
            // Registry lookup instead of the deprecated Sound.valueOf (removed in newer Paper)
            Sound sound = Registry.SOUNDS.get(NamespacedKey.minecraft(soundName.toLowerCase()));
            if (sound == null) {
                return;
            }
            player.playSound(player.getLocation(), sound, volume, pitch);
        } catch (IllegalArgumentException ignored) {
        }
    }
}
