package github.io.Frenxys.storage;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import java.util.logging.Level;
import github.io.Frenxys.Main;
import github.io.Frenxys.models.PlayerSettings;

public class StorageManager {

    private final Main plugin;
    private Connection connection;

    public StorageManager(Main plugin) {
        this.plugin = plugin;
    }

    public void init() {
        try {
            File dataFolder = this.plugin.getDataFolder();
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }
            String url = "jdbc:sqlite:" + new File(dataFolder, "data.db").getAbsolutePath();
            this.connection = DriverManager.getConnection(url);
            try (Statement stmt = this.connection.createStatement()) {
                stmt.execute("    CREATE TABLE IF NOT EXISTS player_settings (\n"
                        + "        uuid TEXT PRIMARY KEY,\n"
                        + "        tpa_enabled INTEGER NOT NULL DEFAULT 1,\n"
                        + "        tpahere_enabled INTEGER NOT NULL DEFAULT 1,\n"
                        + "        auto_accept INTEGER NOT NULL DEFAULT 0,\n"
                        + "        gui_enabled INTEGER NOT NULL DEFAULT 1\n"
                        + "    )\n");
            }
            this.plugin.getLogger().info("Storage initialized (SQLite).");
        } catch (SQLException e) {
            this.plugin.getLogger().log(Level.SEVERE, "Failed to initialize SQLite storage!", e);
        }
    }

    public PlayerSettings loadSettings(UUID uuid) {
        String sql = "SELECT * FROM player_settings WHERE uuid = ?";
        try (PreparedStatement ps = this.connection.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return new PlayerSettings(uuid);
                }
                return new PlayerSettings(uuid,
                        rs.getInt("tpa_enabled") == 1,
                        rs.getInt("tpahere_enabled") == 1,
                        rs.getInt("auto_accept") == 1,
                        rs.getInt("gui_enabled") == 1);
            }
        } catch (SQLException e) {
            this.plugin.getLogger().log(Level.WARNING, "Failed to load settings for " + uuid, e);
        }
        return new PlayerSettings(uuid);
    }

    public void saveSettings(PlayerSettings settings) {
        String sql = "    INSERT INTO player_settings (uuid, tpa_enabled, tpahere_enabled, auto_accept, gui_enabled)\n"
                + "    VALUES (?, ?, ?, ?, ?)\n"
                + "    ON CONFLICT(uuid) DO UPDATE SET\n"
                + "        tpa_enabled = excluded.tpa_enabled,\n"
                + "        tpahere_enabled = excluded.tpahere_enabled,\n"
                + "        auto_accept = excluded.auto_accept,\n"
                + "        gui_enabled = excluded.gui_enabled\n";
        try (PreparedStatement ps = this.connection.prepareStatement(sql)) {
            ps.setString(1, settings.getUUID().toString());
            ps.setInt(2, settings.isTpaEnabled() ? 1 : 0);
            ps.setInt(3, settings.isTpaHereEnabled() ? 1 : 0);
            ps.setInt(4, settings.isAutoAccept() ? 1 : 0);
            ps.setInt(5, settings.isGuiEnabled() ? 1 : 0);
            ps.executeUpdate();
        } catch (SQLException e) {
            this.plugin.getLogger().log(Level.WARNING, "Failed to save settings for " + settings.getUUID(), e);
        }
    }

    public void close() {
        try {
            if (this.connection != null && !this.connection.isClosed()) {
                this.connection.close();
                this.plugin.getLogger().info("Storage connection closed.");
            }
        } catch (SQLException e) {
            this.plugin.getLogger().log(Level.WARNING, "Failed to close storage connection", e);
        }
    }
}
