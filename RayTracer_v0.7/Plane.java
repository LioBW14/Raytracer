import java.awt.Color;

public class Plane extends Object3D {
    private static final double EPSILON = 0.000001;

    // Stores the normalized plane normal.
    private Vector3D normal;

    // Creates a plane from one point, a normal, color and reflectivity.
    public Plane(Vector3D position, Vector3D normal, Color color, double reflectivity) {
        super(position, color, reflectivity);
        this.normal = normal.normalize();
    }

    // Returns the same normal for every point on the plane.
    @Override
    public Vector3D getNormal(Vector3D point) {
        return normal;
    }

    // Computes the intersection between the plane and a ray.
    @Override
    public Intersection intersect(Ray ray) {
        double denominator = normal.dot(ray.getDirection());

        // Parallel rays do not intersect the plane.
        if (Math.abs(denominator) < EPSILON) {
            return null;
        }

        double t = position.subtract(ray.getOrigin()).dot(normal) / denominator;

        // Intersections behind the ray origin are ignored.
        if (t <= EPSILON) {
            return null;
        }

        Vector3D hitPoint = ray.getPoint(t);
        return new Intersection(t, hitPoint, this, normal);
    }
}
