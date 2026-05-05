import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import javax.imageio.ImageIO;

public class Raytracer {
    // Stores the color used when a ray misses every object.
    private Color backgroundColor;

    // Creates a raytracer with a default background color
    public Raytracer() {
        backgroundColor = Color.WHITE;
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
                    image.setRGB(x, y, shade(scene, intersection).getRGB());
                } else {
                    image.setRGB(x, y, backgroundColor.getRGB());
                }
            }
        }

        return image;
    }

    // Computes the final color for a ray-object hit.
    private Color shade(Scene scene, Intersection intersection) {
        // Reads the material color from the hit object.
        Color baseColor = intersection.getColor();

        // Reads the surface normal used by the lighting model.
        Vector3D normal = intersection.getObject().getNormal(intersection.getPosition()).normalize();

        // Accumulates each color channel separately.
        double red = 0.0;
        double green = 0.0;
        double blue = 0.0;

        // Lambertian diffuse lighting: each light contributes baseColor * lightColor * max(0, N dot L).
        for (Light light : scene.getLights()) {
            double diffuse = Math.max(0.0, normal.dot(light.getDirectionToLight())) * light.getIntensity();

            red += baseColor.getRed() * (light.getColor().getRed() / 255.0) * diffuse;
            green += baseColor.getGreen() * (light.getColor().getGreen() / 255.0) * diffuse;
            blue += baseColor.getBlue() * (light.getColor().getBlue() / 255.0) * diffuse;
        }

        // Converts the accumulated channels into a valid RGB color.
        return new Color(clampColor(red), clampColor(green), clampColor(blue));
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

        // Adds directional lights used by the Lambertian flat shader.
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
