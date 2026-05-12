import java.awt.Color;

public class PointLight extends Light {
    // Stores the point light position in world space.
    private Vector3D position;

    // Stores the constant attenuation coefficient.
    private double constantAttenuation;

    // Stores the linear attenuation coefficient.
    private double linearAttenuation;

    // Stores the quadratic attenuation coefficient.
    private double quadraticAttenuation;

    // Creates a point light with position, color and intensity.
    public PointLight(Vector3D position, Color color, double intensity) {
        this(position, color, intensity, 1.0, 0.09, 0.032);
    }

    // Creates a point light with configurable attenuation.
    public PointLight(
        Vector3D position,
        Color color,
        double intensity,
        double constantAttenuation,
        double linearAttenuation,
        double quadraticAttenuation
    ) {
        super(new Vector3D(0, 0, 0), color, intensity);
        this.position = position;
        this.constantAttenuation = constantAttenuation;
        this.linearAttenuation = linearAttenuation;
        this.quadraticAttenuation = quadraticAttenuation;
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

    // Returns a distance attenuation factor for the point light.
    @Override
    public double getAttenuation(Vector3D point) {
        double distance = position.subtract(point).magnitude();
        return 1.0 / (
            constantAttenuation
            + linearAttenuation * distance
            + quadraticAttenuation * distance * distance
        );
    }

    // Returns the distance from the hit point to the light.
    @Override
    public double getDistanceToLight(Vector3D point) {
        return position.subtract(point).magnitude();
    }
}
