import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import javax.imageio.ImageIO;

public class Raytracer {
    // Stores the color used when a ray misses every object.
    private Color backgroundColor;

    // Stores the minimum light applied to every visible surface.
    private double ambientStrength;

    // Stores the intensity of the Phong specular highlight.
    private double specularStrength;

    // Stores the shininess exponent used by the Phong model.
    private double shininess;

    // Creates a raytracer with a default background color
    public Raytracer() {
        backgroundColor = Color.WHITE;
        ambientStrength = 0.12;
        specularStrength = 0.45;
        shininess = 32.0;
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
                // Generates a ray for the current pixel
                Ray ray = camera.generateRay(x, y);

                // Finds the closest object hit by the ray
                Intersection intersection = scene.raycast(ray);

                // Shades the hit point if there is a hit, otherwise uses the background color.
                if (intersection != null) {
                    image.setRGB(x, y, shade(scene, intersection, camera).getRGB());
                } else {
                    image.setRGB(x, y, backgroundColor.getRGB());
                }
            }
        }

        return image;
    }

    // Computes the final color for a ray-object hit using Phong shading.
    private Color shade(Scene scene, Intersection intersection, Camera camera) {
        // Reads the material color from the hit object.
        Color baseColor = intersection.getColor();

        // Reads the surface normal used by the lighting model.
        Vector3D normal = intersection.getObject().getNormal(intersection.getPosition()).normalize();

        // Points from the surface hit point back toward the camera.
        Vector3D viewDirection = camera.getPosition().subtract(intersection.getPosition()).normalize();

        // Accumulates each color channel separately.
        double red = baseColor.getRed() * ambientStrength;
        double green = baseColor.getGreen() * ambientStrength;
        double blue = baseColor.getBlue() * ambientStrength;

        // Phong lighting adds diffuse and specular terms for each light.
        for (Light light : scene.getLights()) {
            // Computes Lambertian diffuse strength using N dot L.
            double diffuse = Math.max(0.0, normal.dot(light.getDirectionToLight())) * light.getIntensity();

            // Reflects the incoming light direction around the surface normal.
            Vector3D reflectDirection = reflect(light.getDirectionToLight().multiply(-1.0), normal);

            // Computes the Phong specular strength using R dot V.
            double specular = Math.pow(
                Math.max(0.0, reflectDirection.dot(viewDirection)),
                shininess
            ) * specularStrength * light.getIntensity();

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

    // Reflects a vector around a normal using R = I - 2(N dot I)N.
    private Vector3D reflect(Vector3D incoming, Vector3D normal) {
        return incoming.subtract(normal.multiply(2.0 * incoming.dot(normal))).normalize();
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
        // Creates the camera that shoots rays through its frustum.
        Camera camera = new Camera(new Vector3D(0, 0, 0), 800, 600, 60);

        // Creates the scene container for all renderable objects.
        Scene scene = new Scene();
        scene.setCamera(camera);

        // Adds directional lights used by the Phong shader.
        scene.addLight(new Light(new Vector3D(-0.4, 0.8, 1.0), Color.WHITE, 0.85));
        scene.addLight(new Light(new Vector3D(0.7, 0.2, 0.4), Color.WHITE, 0.35));

        // scene.addObject(new Sphere(new Vector3D(-1.5, 0, -5), 1, Color.RED));
        // scene.addObject(new Sphere(new Vector3D(1.5, 0, -9), 1, Color.BLUE));

        // scene.addObject(new Triangle(
        //     new Vector3D(-1.2, -1.1, -4),
        //     new Vector3D(1.2, -1.1, -4),
        //     new Vector3D(0, 1.1, -4),
        //     Color.GREEN
        // ));

        // Loads the OBJ tree, triangulates each face, and adds the triangles to the scene.
        try {
            // Loads the tree model with a scale and translation that fit the camera.
            List<Triangle> objTriangles = OBJLoader.load(
                "Tree low.obj",
                new Color(34, 139, 34),
                0.035,
                new Vector3D(0, -2.0, -7.0)
            );

            // Adds every generated triangle to the scene.
            for (Triangle triangle : objTriangles) {
                scene.addObject(triangle);
            }
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
