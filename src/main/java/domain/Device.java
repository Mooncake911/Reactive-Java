package domain;

import domain.components.Location;
import domain.components.Status;
import domain.components.Type;

import java.util.List;

public class Device {

    private final long id;
    private final String name;
    private final String manufacturer;
    private final Type type;
    private final List<String> capabilities;

    private Location location;
    private Status status;

    private final DeviceDelays params;
    private transient Status cachedStatus;
    private transient boolean statusCached = false;

    public Device(long id, String name, String manufacturer, Type type, List<String> capabilities,
                  Location location, Status status, DeviceDelays params) {
        this.id = id;
        this.name = name;
        this.manufacturer = manufacturer;
        this.type = type;
        this.capabilities = capabilities;
        this.location = location;
        this.status = status;
        this.params = params;
    }

    // Сеттеры
    public void updateLocation(Location location) {
        this.location = location;
    }

    public void updateStatus(Status status) {
        this.status = status;
    }

    public void addCapability(String capability) {
        if (capability != null && !capability.trim().isEmpty()) {
            this.capabilities.add(capability);
        }
    }

    public void removeCapability(String capability) {
        this.capabilities.remove(capability);
    }

    // Геттеры
    public long getId() {
        applyDelay(params.idDelayMs());
        return id;
    }

    public String getName() {
        applyDelay(params.nameDelayMs());
        return name;
    }

    public String getManufacturer() {
        applyDelay(params.manufacturerDelayMs());
        return manufacturer;
    }

    public Type getType() {
        applyDelay(params.typeDelayMs());
        return type;
    }

    public Location getLocation() {
        applyDelay(params.locationDelayMs());
        return location;
    }

    public List<String> getCapabilities() {
        applyDelay(params.capabilitiesDelayMs());
        return capabilities;
    }

    public Status getStatus() {
        if (statusCached) {
            return cachedStatus;
        }

        applyDelay(params.statusDelayMs());

        cachedStatus = status;
        statusCached = true;

        return cachedStatus;
    }

    // Вспомогательный метод для применения задержки
    private void applyDelay(long delayMs) {
        if (delayMs > 0) {
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Operation interrupted", e);
            }
        }
    }

    public void resetCache() {
        this.statusCached = false;
        this.cachedStatus = null;
    }

    @Override
    public String toString() {
        return String.format("Device{id=%d, name='%s', type=%s, manufacturer='%s', location=%s, status=%s, capabilities=%d}",
                id, name, type, manufacturer, location, status, capabilities.size());
    }
}


