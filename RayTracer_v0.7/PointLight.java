import java.awt.Color;

public class PointLight extends Light {
    // Stores the point light position in world space.
    private Vector3D position;

    // Stores the constant falloff coefficient.
    private double constantFalloff;

    // Stores the linear falloff coefficient.
    private double linearFalloff;

    // Stores the quadratic falloff coefficient.
    private double quadraticFalloff;

    // Creates a point light with position, color and intensity.
    public PointLight(Vector3D position, Color color, double intensity) {
        this(position, color, intensity, 1.0, 0.09, 0.032);
    }

    // Creates a point light with configurable distance falloff.
    public PointLight(
        Vector3D position,
        Color color,
        double intensity,
        double constantFalloff,
        double linearFalloff,
        double quadraticFalloff
    ) {
        super(new Vector3D(0, 0, 0), color, intensity);
        this.position = position;
        this.constantFalloff = constantFalloff;
        this.linearFalloff = linearFalloff;
        this.quadraticFalloff = quadraticFalloff;
    }

    // Returns the point light position.
    public Vector3D getPosition() {
        return position;
    }

    // Returns the normalized direction from the hit point to the light.
    @Override
    public Vector3D getDirectionToLight(Vector3D point) {
        return position.subtract(point).normalize();
    }

    // Returns a distance falloff factor for the point light.
    @Override
    public double getFalloff(Vector3D point) {
        double distance = position.subtract(point).magnitude();
        return 1.0 / (
            constantFalloff
            + linearFalloff * distance
            + quadraticFalloff * distance * distance
        );
    }

    // Returns the distance from the hit point to the light.
    @Override
    public double getDistanceToLight(Vector3D point) {
        return position.subtract(point).magnitude();
    }
}
