import java.awt.Color;

public abstract class Object3D {
    // Stores the object position
    protected Vector3D position;

    // Stores the object color
    protected Color color;

    // Creates a 3D object with position and color
    public Object3D(Vector3D position, Color color) {
        this.position = position;
        this.color = color;
    }

    // Returns the object position
    public Vector3D getPosition() {
        return position;
    }

    // Returns the object color
    public Color getColor() {
        return color;
    }

    // Computes the ray-object intersection
    public abstract Intersection intersect(Ray ray);
}