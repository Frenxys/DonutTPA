package github.io.Frenxys.models;

import java.util.UUID;

public class PlayerSettings {

    private final UUID uuid;
    private boolean tpaEnabled;
    private boolean tpaHereEnabled;
    private boolean autoAccept;
    private boolean guiEnabled;

    public PlayerSettings(UUID uuid) {
        this.uuid = uuid;
        this.tpaEnabled = true;
        this.tpaHereEnabled = true;
        this.autoAccept = false;
        this.guiEnabled = true;
    }

    public PlayerSettings(UUID uuid, boolean tpaEnabled, boolean tpaHereEnabled, boolean autoAccept, boolean guiEnabled) {
        this.uuid = uuid;
        this.tpaEnabled = tpaEnabled;
        this.tpaHereEnabled = tpaHereEnabled;
        this.autoAccept = autoAccept;
        this.guiEnabled = guiEnabled;
    }

    public UUID getUUID() {
        return uuid;
    }

    public boolean isTpaEnabled() {
        return tpaEnabled;
    }

    public boolean isTpaHereEnabled() {
        return tpaHereEnabled;
    }

    public boolean isAutoAccept() {
        return autoAccept;
    }

    public boolean isGuiEnabled() {
        return guiEnabled;
    }

    public void setTpaEnabled(boolean v) {
        this.tpaEnabled = v;
    }

    public void setTpaHereEnabled(boolean v) {
        this.tpaHereEnabled = v;
    }

    public void setAutoAccept(boolean v) {
        this.autoAccept = v;
    }

    public void setGuiEnabled(boolean v) {
        this.guiEnabled = v;
    }
}
