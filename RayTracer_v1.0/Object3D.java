import java.awt.Color;

public abstract class Object3D {
    // Stores the object position or reference point.
    protected Vector3D position;

    // Stores all shading properties used by the renderer.
    protected Material material;

    // Creates a 3D object with position and color
    public Object3D(Vector3D position, Color color) {
        this(position, color, 0.0);
    }

    // Creates a 3D object with position, color and reflectivity.
    public Object3D(Vector3D position, Color color, double reflectivity) {
        this.position = position;
        this.material = new Material(color).setReflectivity(reflectivity);
    }

    public Object3D(Vector3D position, Material material) {
        this.position = position;
        this.material = material;
    }

    // Returns the object position or reference point.
    public Vector3D getPosition() {
        return position;
    }

    // Returns the base object color.
    public Color getColor() {
        return material.getColor(0.0, 0.0);
    }

    public Color getColor(double u, double v) {
        return material.getColor(u, v);
    }

    public Material getMaterial() {
        return material;
    }

    // Returns the object reflection factor.
    public double getReflectivity() {
        return material.getReflectivity();
    }

    // Returns the surface normal used for lighting at the hit point.
    public abstract Vector3D getNormal(Vector3D point);

    // Computes the intersection between this object and a ray.
    public abstract Intersection intersect(Ray ray);
}
