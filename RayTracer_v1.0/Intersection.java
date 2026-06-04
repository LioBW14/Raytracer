import java.awt.Color;

public class Intersection {
    // Stores the distance from the ray origin to the hit point.
    private double distance;

    // Stores the exact hit position.
    private Vector3D position;

    // Stores the object that was hit.
    private Object3D object;

    // Stores the surface normal at the hit point.
    private Vector3D normal;

    private double textureU;
    private double textureV;

    // Creates an intersection with distance, position and object
    public Intersection(double distance, Vector3D position, Object3D object) {
        this(distance, position, object, object.getNormal(position));
    }

    // Creates an intersection with distance, position, object and normal.
    public Intersection(double distance, Vector3D position, Object3D object, Vector3D normal) {
        this(distance, position, object, normal, 0.0, 0.0);
    }

    public Intersection(
        double distance,
        Vector3D position,
        Object3D object,
        Vector3D normal,
        double textureU,
        double textureV
    ) {
        this.distance = distance;
        this.position = position;
        this.object = object;
        this.normal = normal;
        this.textureU = textureU;
        this.textureV = textureV;
    }

    // Returns the hit distance.
    public double getDistance() {
        return distance;
    }

    // Returns the hit position.
    public Vector3D getPosition() {
        return position;
    }

    // Returns the object hit by the ray.
    public Object3D getObject() {
        return object;
    }

    // Returns the surface normal at the hit point.
    public Vector3D getNormal() {
        return normal;
    }

    // Returns the base color of the hit object.
    public Color getColor() {
        return object.getColor(textureU, textureV);
    }

    public Material getMaterial() {
        return object.getMaterial();
    }

    public double getTextureU() {
        return textureU;
    }

    public double getTextureV() {
        return textureV;
    }
}
