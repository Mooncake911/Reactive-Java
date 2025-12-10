package domain.components;

import java.time.LocalDateTime;

public record Status(boolean isOnline, int batteryLevel, int signalStrength, LocalDateTime lastHeartbeat) {
    public Status(boolean isOnline, int batteryLevel, int signalStrength, LocalDateTime lastHeartbeat) {
        this.isOnline = isOnline;
        this.batteryLevel = Math.max(0, Math.min(100, batteryLevel));
        this.signalStrength = Math.max(0, Math.min(100, signalStrength));
        this.lastHeartbeat = lastHeartbeat;
    }

    @Override
    public String toString() {
        return String.format("Status[online=%s, battery=%d%%, signal=%d%%, lastSeen=%s]",
                isOnline, batteryLevel, signalStrength, lastHeartbeat);
    }
}


