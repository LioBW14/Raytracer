public class Vector3D {
    private double x;
    private double y;
    private double z;

    // Creates a 3D vector with x, y and z values
    public Vector3D(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public Vector3D add(Vector3D other) {
        return new Vector3D(
            this.x + other.x,
            this.y + other.y,
            this.z + other.z
        );
    }

    public Vector3D subtract(Vector3D other) {
        return new Vector3D(
            this.x - other.x,
            this.y - other.y,
            this.z - other.z
        );
    }

    public Vector3D multiply(double scalar) {
        return new Vector3D(
            this.x * scalar,
            this.y * scalar,
            this.z * scalar
        );
    }

    public double dot(Vector3D other) {
        return this.x * other.x
             + this.y * other.y
             + this.z * other.z;
    }

    public Vector3D cross(Vector3D other) {
        return new Vector3D(
            this.y * other.z - this.z * other.y,
            this.z * other.x - this.x * other.z,
            this.x * other.y - this.y * other.x
        );
    }

    public double magnitude() {
        return Math.sqrt(this.dot(this));
    }

    public Vector3D normalize() {
        double mag = magnitude();

        // Avoids division by zero
        if (mag == 0) {
            return new Vector3D(0, 0, 0);
        }

        return new Vector3D(
            this.x / mag,
            this.y / mag,
            this.z / mag
        );
    }
}
