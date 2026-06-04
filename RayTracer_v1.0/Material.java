import java.awt.Color;

public class Material {
    private Color color;
    private Texture texture;
    private double ambient;
    private double diffuse;
    private double specular;
    private double shininess;
    private double reflectivity;
    private double transparency;
    private double refractiveIndex;

    public Material(Color color) {
        this(color, null);
    }

    public Material(Color color, Texture texture) {
        this.color = color;
        this.texture = texture;
        this.ambient = 0.08;
        this.diffuse = 0.85;
        this.specular = 0.25;
        this.shininess = 48.0;
        this.reflectivity = 0.0;
        this.transparency = 0.0;
        this.refractiveIndex = 1.0;
    }

    public static Material matte(Color color) {
        return new Material(color).setSpecular(0.08).setShininess(20.0);
    }

    public Color getColor(double u, double v) {
        if (texture == null) {
            return color;
        }

        Color texel = texture.sample(u, v);
        return new Color(
            clamp((texel.getRed() / 255.0) * color.getRed()),
            clamp((texel.getGreen() / 255.0) * color.getGreen()),
            clamp((texel.getBlue() / 255.0) * color.getBlue())
        );
    }

    public double getAmbient() {
        return ambient;
    }

    public double getDiffuse() {
        return diffuse;
    }

    public double getSpecular() {
        return specular;
    }

    public double getShininess() {
        return shininess;
    }

    public double getReflectivity() {
        return reflectivity;
    }

    public double getTransparency() {
        return transparency;
    }

    public double getRefractiveIndex() {
        return refractiveIndex;
    }

    public Material setAmbient(double ambient) {
        this.ambient = ambient;
        return this;
    }

    public Material setDiffuse(double diffuse) {
        this.diffuse = diffuse;
        return this;
    }

    public Material setSpecular(double specular) {
        this.specular = specular;
        return this;
    }

    public Material setShininess(double shininess) {
        this.shininess = shininess;
        return this;
    }

    public Material setReflectivity(double reflectivity) {
        this.reflectivity = reflectivity;
        return this;
    }

    public Material setTransparency(double transparency) {
        this.transparency = transparency;
        return this;
    }

    public Material setRefractiveIndex(double refractiveIndex) {
        this.refractiveIndex = refractiveIndex;
        return this;
    }

    private static int clamp(double value) {
        return Math.max(0, Math.min(255, (int) Math.round(value)));
    }
}
