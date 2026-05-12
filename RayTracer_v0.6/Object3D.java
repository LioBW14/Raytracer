import java.awt.Color;

public abstract class Object3D {
    // Stores the object position or reference point.
    protected Vector3D position;

    // Stores the base object color.
    protected Color color;

    // Stores how much mirror reflection the object contributes.
    protected double reflectivity;

    // Creates a 3D object with position and color
    public Object3D(Vector3D position, Color color) {
        this(position, color, 0.0);
    }

    // Creates a 3D object with position, color and reflectivity.
    public Object3D(Vector3D position, Color color, double reflectivity) {
        this.position = position;
        this.color = color;
        this.reflectivity = reflectivity;
    }

    // Returns the object position or reference point.
    public Vector3D getPosition() {
        return position;
    }

    // Returns the base object color.
    public Color getColor() {
        return color;
    }

    // Returns the object reflection factor.
    public double getReflectivity() {
        return reflectivity;
    }

    // Returns the surface normal used for lighting at the hit point.
    public abstract Vector3D getNormal(Vector3D point);

    // Computes the intersection between this object and a ray.
    public abstract Intersection intersect(Ray ray);
}
