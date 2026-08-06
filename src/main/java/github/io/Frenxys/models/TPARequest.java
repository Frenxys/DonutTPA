package github.io.Frenxys.models;

import java.util.UUID;

public class TPARequest {

    public enum Type {
        TPA,
        TPAHERE
    }

    private final UUID senderUUID;
    private final String senderName;
    private final UUID targetUUID;
    private final String targetName;
    private final Type type;
    private final long createdAt;

    public TPARequest(UUID senderUUID, String senderName, UUID targetUUID, String targetName, Type type) {
        this.senderUUID = senderUUID;
        this.senderName = senderName;
        this.targetUUID = targetUUID;
        this.targetName = targetName;
        this.type = type;
        this.createdAt = System.currentTimeMillis();
    }

    public UUID getSenderUUID() {
        return senderUUID;
    }

    public String getSenderName() {
        return senderName;
    }

    public UUID getTargetUUID() {
        return targetUUID;
    }

    public String getTargetName() {
        return targetName;
    }

    public Type getType() {
        return type;
    }

    public long getCreatedAt() {
        return createdAt;
    }
}
