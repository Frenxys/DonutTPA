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

public class BaseConfig {

    private final JavaPlugin host;
    private final String fileName;
    private FileConfiguration config;
    private File file;

    public BaseConfig(JavaPlugin host, String fileName) {
        this.host = host;
        this.fileName = fileName;
        this.load();
    }

    public void load() {
        this.file = new File(this.host.getDataFolder(), this.fileName);
        if (!this.file.exists()) {
            this.host.getDataFolder().mkdirs();
            try (InputStream in = this.host.getResource(this.fileName)) {
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
                this.host.getLogger().log(Level.WARNING, "Could not save default " + this.fileName, e);
            }
        }
        this.config = YamlConfiguration.loadConfiguration(this.file);
        try (InputStream defStream = this.host.getResource(this.fileName)) {
            if (defStream != null) {
                YamlConfiguration defaults = YamlConfiguration.loadConfiguration(new InputStreamReader(defStream));
                this.config.setDefaults(defaults);
                this.config.options().copyDefaults(true);
                this.config.save(this.file);
            }
        } catch (IOException e) {
            this.host.getLogger().log(Level.WARNING, "Could not merge defaults for " + this.fileName, e);
        }
    }

    public void reload() {
        this.load();
    }

    public void save() {
        try {
            this.config.save(this.file);
        } catch (IOException e) {
            this.host.getLogger().log(Level.WARNING, "Could not save " + this.fileName, e);
        }
    }

    public FileConfiguration getConfig() {
        return this.config;
    }

    public File getFile() {
        return this.file;
    }
}
