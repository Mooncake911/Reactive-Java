package domain.components;

public record Location(int x, int y, int z) {
    public Location(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public String toString() {
        return String.format("Location[x=%d, y=%d, z=%d]", x, y, z);
    }
}


