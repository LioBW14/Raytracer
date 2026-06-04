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

    // Returns the direction from a hit point toward this light.
    public Vector3D getDirectionToLight(Vector3D point) {
        return directionToLight;
    }

    // Returns the distance falloff at a hit point.
    public double getFalloff(Vector3D point) {
        return 1.0;
    }

    // Returns the maximum shadow ray distance for this light.
    public double getDistanceToLight(Vector3D point) {
        return Double.MAX_VALUE;
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
