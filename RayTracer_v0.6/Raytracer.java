import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class Raytracer {
    private static final double RAY_BIAS = 0.0005;

    // Stores the color used when a ray misses every object.
    private Color backgroundColor;

    // Stores the minimum light applied to every visible surface.
    private double ambientStrength;

    // Stores the intensity of the Phong specular highlight.
    private double specularStrength;

    // Stores the shininess exponent used by the Phong model.
    private double shininess;

    // Stores the maximum number of recursive reflection bounces.
    private int maxReflectionDepth;

    // Stores the number of sub-pixel samples per axis.
    private int samplesPerAxis;

    // Creates a raytracer with a default background color
    public Raytracer() {
        backgroundColor = Color.BLACK;
        ambientStrength = 0.08;
        specularStrength = 0.35;
        shininess = 48.0;
        maxReflectionDepth = 2;
        samplesPerAxis = 2;
    }

    // Renders the full scene into an image.
    public BufferedImage render(Scene scene) {
        Camera camera = scene.getCamera();
        int width = camera.getWidth();
        int height = camera.getHeight();

        // Creates the output image
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        // Loops through every pixel
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                // Averages multiple samples inside the pixel to reduce aliasing.
                image.setRGB(x, y, samplePixel(scene, camera, x, y).getRGB());
            }
        }

        return image;
    }

    // Traces all samples inside one pixel and averages their colors.
    private Color samplePixel(Scene scene, Camera camera, int x, int y) {
        double red = 0.0;
        double green = 0.0;
        double blue = 0.0;
        double sampleCount = samplesPerAxis * samplesPerAxis;

        for (int sampleY = 0; sampleY < samplesPerAxis; sampleY++) {
            for (int sampleX = 0; sampleX < samplesPerAxis; sampleX++) {
                double offsetX = (sampleX + 0.5) / samplesPerAxis;
                double offsetY = (sampleY + 0.5) / samplesPerAxis;
                Ray ray = camera.generateRay(x, y, offsetX, offsetY);
                Color sampleColor = traceRay(scene, ray, 0);

                red += sampleColor.getRed();
                green += sampleColor.getGreen();
                blue += sampleColor.getBlue();
            }
        }

        return new Color(
            clampColor(red / sampleCount),
            clampColor(green / sampleCount),
            clampColor(blue / sampleCount)
        );
    }

    // Traces a ray recursively through the scene.
    private Color traceRay(Scene scene, Ray ray, int depth) {
        Intersection intersection = scene.raycast(ray);

        if (intersection == null) {
            return backgroundColor;
        }

        Color localColor = shade(scene, intersection, ray);
        double reflectivity = intersection.getObject().getReflectivity();

        if (reflectivity <= 0.0 || depth >= maxReflectionDepth) {
            return localColor;
        }

        // Launches a secondary reflection ray from the hit point.
        Vector3D normal = orientNormal(intersection.getNormal(), ray);
        Vector3D reflectionDirection = reflect(ray.getDirection(), normal);
        Vector3D reflectionOrigin = intersection.getPosition().add(normal.multiply(RAY_BIAS));
        Color reflectionColor = traceRay(scene, new Ray(reflectionOrigin, reflectionDirection), depth + 1);

        return mixColors(localColor, reflectionColor, reflectivity);
    }

    // Computes the final local color for a ray-object hit using Phong shading.
    private Color shade(Scene scene, Intersection intersection, Ray ray) {
        // Reads the material color from the hit object.
        Color baseColor = intersection.getColor();

        // Reads the surface normal stored in the intersection.
        Vector3D normal = orientNormal(intersection.getNormal(), ray);

        // Points from the surface hit point back toward the ray origin.
        Vector3D viewDirection = ray.getDirection().multiply(-1.0).normalize();

        // Accumulates each color channel separately.
        double red = baseColor.getRed() * ambientStrength;
        double green = baseColor.getGreen() * ambientStrength;
        double blue = baseColor.getBlue() * ambientStrength;

        // Phong lighting adds diffuse and specular terms for each light.
        for (Light light : scene.getLights()) {
            // Computes the direction and attenuation for the current light.
            Vector3D lightDirection = light.getDirectionToLight(intersection.getPosition());
            double attenuation = light.getAttenuation(intersection.getPosition());
            double nDotL = Math.max(0.0, normal.dot(lightDirection));

            if (nDotL <= 0.0 || isInShadow(scene, intersection, normal, light)) {
                continue;
            }

            // Computes Lambertian diffuse strength using N dot L.
            double diffuse = nDotL * light.getIntensity() * attenuation;

            // Reflects the incoming light direction around the surface normal.
            Vector3D reflectDirection = reflect(lightDirection.multiply(-1.0), normal);

            // Computes the Phong specular strength using R dot V.
            double specular = Math.pow(
                Math.max(0.0, reflectDirection.dot(viewDirection)),
                shininess
            ) * specularStrength * light.getIntensity() * attenuation;

            // Adds the diffuse light contribution tinted by the object color.
            red += baseColor.getRed() * (light.getColor().getRed() / 255.0) * diffuse;
            green += baseColor.getGreen() * (light.getColor().getGreen() / 255.0) * diffuse;
            blue += baseColor.getBlue() * (light.getColor().getBlue() / 255.0) * diffuse;

            // Adds the specular highlight tinted by the light color.
            red += 255.0 * (light.getColor().getRed() / 255.0) * specular;
            green += 255.0 * (light.getColor().getGreen() / 255.0) * specular;
            blue += 255.0 * (light.getColor().getBlue() / 255.0) * specular;
        }

        // Converts the accumulated channels into a valid RGB color.
        return new Color(clampColor(red), clampColor(green), clampColor(blue));
    }

    // Checks whether another object blocks the light from the hit point.
    private boolean isInShadow(Scene scene, Intersection intersection, Vector3D normal, Light light) {
        Vector3D lightDirection = light.getDirectionToLight(intersection.getPosition());
        Vector3D shadowOrigin = intersection.getPosition().add(normal.multiply(RAY_BIAS));
        Ray shadowRay = new Ray(shadowOrigin, lightDirection);
        Intersection blocker = scene.raycast(shadowRay);

        if (blocker == null) {
            return false;
        }

        return blocker.getDistance() < light.getDistanceToLight(intersection.getPosition()) - RAY_BIAS;
    }

    // Orients the normal against the incoming ray.
    private Vector3D orientNormal(Vector3D normal, Ray ray) {
        Vector3D normalized = normal.normalize();

        if (normalized.dot(ray.getDirection()) > 0.0) {
            return normalized.multiply(-1.0);
        }

        return normalized;
    }

    // Reflects a vector around a normal using R = I - 2(N dot I)N.
    private Vector3D reflect(Vector3D incoming, Vector3D normal) {
        return incoming.subtract(normal.multiply(2.0 * incoming.dot(normal))).normalize();
    }

    // Blends a local color with a reflected color.
    private Color mixColors(Color localColor, Color reflectionColor, double reflectivity) {
        double localWeight = 1.0 - reflectivity;
        double reflectedWeight = reflectivity;

        return new Color(
            clampColor(localColor.getRed() * localWeight + reflectionColor.getRed() * reflectedWeight),
            clampColor(localColor.getGreen() * localWeight + reflectionColor.getGreen() * reflectedWeight),
            clampColor(localColor.getBlue() * localWeight + reflectionColor.getBlue() * reflectedWeight)
        );
    }

    // Keeps a color channel inside the 0 to 255 range.
    private int clampColor(double value) {
        return Math.max(0, Math.min(255, (int) Math.round(value)));
    }

    // Saves the rendered image as a PNG file.
    public void saveImage(BufferedImage image, String fileName) {
        try {
            ImageIO.write(image, "png", new File(fileName));
            System.out.println("Image saved as " + fileName);
        } catch (Exception e) {
            System.out.println("Error saving image: " + e.getMessage());
        }
    }

    // Builds a sample scene and renders it.
    public static void main(String[] args) {
        // Creates a camera that looks directly at the model.
        Camera camera = new Camera(
            new Vector3D(0.25, 0.75, 2.30),
            new Vector3D(0.0, 0.42, 0.0),
            560,
            420,
            55
        );

        // Creates the scene container for all renderable objects.
        Scene scene = new Scene();
        scene.setCamera(camera);

        // Adds a soft directional fill light.
        scene.addLight(new Light(new Vector3D(-0.2, 0.4, 1.0), new Color(180, 210, 255), 0.18));

        // Adds a point light near the camera to create local highlights.
        scene.addLight(new PointLight(
            new Vector3D(-0.8, 1.2, 1.4),
            Color.WHITE,
            1.7,
            1.0,
            0.08,
            0.04
        ));

        // Adds a warm point light near the spout to reveal curved details.
        scene.addLight(new PointLight(
            new Vector3D(1.4, 0.6, 0.8),
            new Color(255, 220, 170),
            0.9,
            1.0,
            0.12,
            0.05
        ));

        // Adds a soft overhead point light to reveal the lid silhouette.
        scene.addLight(new PointLight(
            new Vector3D(0.0, 1.4, 0.6),
            new Color(210, 255, 210),
            0.45,
            1.0,
            0.08,
            0.03
        ));

        // Adds a base plane that receives shadows and appears in reflections.
        scene.addObject(new Plane(
            new Vector3D(0, -0.05, 0),
            new Vector3D(0, 1, 0),
            new Color(28, 30, 32),
            0.18
        ));

        // scene.addObject(new Sphere(new Vector3D(-1.5, 0, -5), 1, Color.RED));
        // scene.addObject(new Sphere(new Vector3D(1.5, 0, -9), 1, Color.BLUE));

        // scene.addObject(new Triangle(
        //     new Vector3D(-1.2, -1.1, -4),
        //     new Vector3D(1.2, -1.1, -4),
        //     new Vector3D(0, 1.1, -4),
        //     Color.GREEN
        // ));

        // Loads the OBJ model, triangulates each face, and adds the triangles to the scene.
        try {
            // Loads the teapot model with normals used for smooth Phong interpolation.
            Model3D teapot = OBJLoader.loadModel(
                "teapot.obj",
                new Color(30, 120, 45),
                1,
                new Vector3D(0, 0.0, 0.0),
                0.0,
                0.22
            );

            // Adds the whole mesh as one scene object.
            scene.addObject(teapot);
        } catch (Exception e) {
            System.out.println("Error loading OBJ model: " + e.getMessage());
        }

        // Creates the raytracer
        Raytracer raytracer = new Raytracer();

        // Renders the image
        BufferedImage image = raytracer.render(scene);

        // Saves the image
        raytracer.saveImage(image, "render.png");
    }
}
