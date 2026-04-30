import java.awt.Color;

public abstract class Object3D {
    protected Vector3D position;
    protected Color color;

    // Creates a 3D object with position and color
    public Object3D(Vector3D position, Color color) {
        this.position = position;
        this.color = color;
    }

    public Vector3D getPosition() {
        return position;
    }

    public Color getColor() {
        return color;
    }

    public abstract Intersection intersect(Ray ray);
}