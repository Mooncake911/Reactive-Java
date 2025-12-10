package domain;

public final class DeviceDelays {
    private final long idDelayMs;
    private final long nameDelayMs;
    private final long manufacturerDelayMs;
    private final long typeDelayMs;
    private final long locationDelayMs;
    private final long capabilitiesDelayMs;
    private final long statusDelayMs;

    private DeviceDelays(Builder b) {
        this.idDelayMs = Math.max(0, b.idDelayMs);
        this.nameDelayMs = Math.max(0, b.nameDelayMs);
        this.manufacturerDelayMs = Math.max(0, b.manufacturerDelayMs);
        this.typeDelayMs = Math.max(0, b.typeDelayMs);
        this.locationDelayMs = Math.max(0, b.locationDelayMs);
        this.capabilitiesDelayMs = Math.max(0, b.capabilitiesDelayMs);
        this.statusDelayMs = Math.max(0, b.statusDelayMs);
    }

    public long idDelayMs() {
        return idDelayMs;
    }

    public long nameDelayMs() {
        return nameDelayMs;
    }

    public long manufacturerDelayMs() {
        return manufacturerDelayMs;
    }

    public long typeDelayMs() {
        return typeDelayMs;
    }

    public long locationDelayMs() {
        return locationDelayMs;
    }

    public long capabilitiesDelayMs() {
        return capabilitiesDelayMs;
    }

    public long statusDelayMs() {
        return statusDelayMs;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static DeviceDelays empty() {
        return builder().build();
    }

    public static final class Builder {
        private long idDelayMs = 0;
        private long nameDelayMs = 0;
        private long manufacturerDelayMs = 0;
        private long typeDelayMs = 0;
        private long locationDelayMs = 0;
        private long capabilitiesDelayMs = 0;
        private long statusDelayMs = 0;

        public Builder idDelayMs(long v) {
            this.idDelayMs = v;
            return this;
        }

        public Builder nameDelayMs(long v) {
            this.nameDelayMs = v;
            return this;
        }

        public Builder manufacturerDelayMs(long v) {
            this.manufacturerDelayMs = v;
            return this;
        }

        public Builder typeDelayMs(long v) {
            this.typeDelayMs = v;
            return this;
        }

        public Builder locationDelayMs(long v) {
            this.locationDelayMs = v;
            return this;
        }

        public Builder capabilitiesDelayMs(long v) {
            this.capabilitiesDelayMs = v;
            return this;
        }

        public Builder statusDelayMs(long v) {
            this.statusDelayMs = v;
            return this;
        }

        public DeviceDelays build() {
            return new DeviceDelays(this);
        }
    }
}


