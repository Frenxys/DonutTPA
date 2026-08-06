package github.io.Frenxys.config;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.util.logging.Level;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class GuiConfig {

    private final JavaPlugin host;
    private final String resourcePath;
    private final String diskPath;
    private FileConfiguration config;
    private File file;

    public GuiConfig(JavaPlugin host, String resourcePath) {
        this.host = host;
        this.resourcePath = resourcePath;
        this.diskPath = resourcePath;
        this.load();
    }

    public void load() {
        this.file = new File(this.host.getDataFolder(), this.diskPath);
        if (!this.file.exists()) {
            this.file.getParentFile().mkdirs();
            try (InputStream in = this.host.getResource(this.resourcePath)) {
                if (in != null) {
                    try (FileOutputStream out = new FileOutputStream(this.file)) {
                        int len;
                        byte[] buf = new byte[1024];
                        while ((len = in.read(buf)) > 0) {
                            ((OutputStream) out).write(buf, 0, len);
                        }
                    }
                }
            } catch (IOException e) {
                this.host.getLogger().log(Level.WARNING, "Could not save default " + this.resourcePath, e);
            }
        }
        this.config = YamlConfiguration.loadConfiguration(this.file);
        try (InputStream defStream = this.host.getResource(this.resourcePath)) {
            if (defStream != null) {
                YamlConfiguration defaults = YamlConfiguration.loadConfiguration(new InputStreamReader(defStream));
                this.config.setDefaults(defaults);
                this.config.options().copyDefaults(true);
                this.config.save(this.file);
            }
        } catch (IOException e) {
            this.host.getLogger().log(Level.WARNING, "Could not merge defaults for " + this.resourcePath, e);
        }
    }

    public void reload() {
        this.load();
    }

    public FileConfiguration getConfig() {
        return this.config;
    }
}
