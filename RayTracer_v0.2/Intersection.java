import java.awt.Color;

public class Intersection {
    // Stores the distance from the ray origin to the hit point
    private double distance;

    // Stores the exact hit position
    private Vector3D position;

    // Stores the object that was hit
    private Object3D object;

    // Creates an intersection with distance, position and object
    public Intersection(double distance, Vector3D position, Object3D object) {
        this.distance = distance;
        this.position = position;
        this.object = object;
    }

    // Returns the hit distance
    public double getDistance() {
        return distance;
    }

    // Returns the hit position
    public Vector3D getPosition() {
        return position;
    }

    // Returns the hit object
    public Object3D getObject() {
        return object;
    }

    // Returns the color of the hit object
    public Color getColor() {
        return object.getColor();
    }
}