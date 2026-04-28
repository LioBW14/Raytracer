public class Ray {
    // Stores the ray origin
    private Vector3D origin;

    // Stores the ray direction
    private Vector3D direction;

    // Creates a ray with origin and direction
    public Ray(Vector3D origin, Vector3D direction) {
        this.origin = origin;
        this.direction = direction.normalize();
    }

    // Returns the ray origin
    public Vector3D getOrigin() {
        return origin;
    }

    // Returns the ray direction
    public Vector3D getDirection() {
        return direction;
    }

    // Returns a point on the ray using P = O + tD
    public Vector3D getPoint(double t) {
        return origin.add(direction.multiply(t));
    }
}