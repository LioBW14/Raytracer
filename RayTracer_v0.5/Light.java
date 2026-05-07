import java.awt.Color;

public class Light {
    // Stores the normalized direction from a surface point toward the light.
    private Vector3D directionToLight;

    // Stores the light color.
    private Color color;

    // Stores the brightness multiplier.
    private double intensity;

    // Creates a directional light. The direction points from the surface toward the light.
    public Light(Vector3D directionToLight, Color color, double intensity) {
        this.directionToLight = directionToLight.normalize();
        this.color = color;
        this.intensity = intensity;
    }

    // Returns the direction used in the N dot L lighting term.
    public Vector3D getDirectionToLight() {
        return directionToLight;
    }

    // Returns the light color.
    public Color getColor() {
        return color;
    }

    // Returns the light intensity.
    public double getIntensity() {
        return intensity;
    }
}
