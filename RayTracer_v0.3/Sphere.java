import java.awt.Color;

public class Sphere extends Object3D {
    private double radius;

    // Creates a sphere with position, radius and color
    public Sphere(Vector3D position, double radius, Color color) {
        super(position, color);
        this.radius = radius;
    }

    public double getRadius() {
        return radius;
    }

    @Override
    public Intersection intersect(Ray ray) {
        // Vector from sphere center to ray origin
        Vector3D oc = ray.getOrigin().subtract(position);

        // Quadratic equation coefficients
        double a = ray.getDirection().dot(ray.getDirection());
        double b = 2.0 * ray.getDirection().dot(oc);
        double c = oc.dot(oc) - (radius * radius);

        // Discriminant value
        double discriminant = (b * b) - (4 * a * c);

        // No real solutions means no intersection
        if (discriminant < 0) {
            return null;
        }

        // Compute both possible intersection distances
        double sqrtDiscriminant = Math.sqrt(discriminant);
        double t1 = (-b - sqrtDiscriminant) / (2 * a);
        double t2 = (-b + sqrtDiscriminant) / (2 * a);

        // Choose the closest positive intersection
        double t = -1;

        if (t1 > 0 && t2 > 0) {
            t = Math.min(t1, t2);
        } else if (t1 > 0) {
            t = t1;
        } else if (t2 > 0) {
            t = t2;
        }

        // If both intersections are behind the ray, ignore them
        if (t < 0) {
            return null;
        }

        // Compute the exact hit point
        Vector3D hitPoint = ray.getPoint(t);

        return new Intersection(t, hitPoint, this);
    }
}