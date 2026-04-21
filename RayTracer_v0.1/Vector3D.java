public class Vector3D {
    // Stores the x coordinate
    private double x;

    // Stores the y coordinate
    private double y;

    // Stores the z coordinate
    private double z;

    // Creates a 3D vector with x, y and z values
    public Vector3D(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    // Returns the x value
    public double getX() {
        return x;
    }

    // Returns the y value
    public double getY() {
        return y;
    }

    // Returns the z value
    public double getZ() {
        return z;
    }

    // Adds this vector to another vector
    public Vector3D add(Vector3D other) {
        return new Vector3D(
            this.x + other.x,
            this.y + other.y,
            this.z + other.z
        );
    }

    // Subtracts another vector from this vector
    public Vector3D subtract(Vector3D other) {
        return new Vector3D(
            this.x - other.x,
            this.y - other.y,
            this.z - other.z
        );
    }

    // Multiplies the vector by a scalar value
    public Vector3D multiply(double scalar) {
        return new Vector3D(
            this.x * scalar,
            this.y * scalar,
            this.z * scalar
        );
    }

    // Computes the dot product with another vector
    public double dot(Vector3D other) {
        return this.x * other.x
             + this.y * other.y
             + this.z * other.z;
    }

    // Returns the vector magnitude
    public double magnitude() {
        return Math.sqrt(this.dot(this));
    }

    // Returns a normalized version of the vector
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