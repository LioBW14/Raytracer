import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class Raytracer {
    // Stores the background color
    private Color backgroundColor;

    // Creates a raytracer with a default background color
    public Raytracer() {
        backgroundColor = Color.WHITE;
    }

    // Renders the scene and returns the final image
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

                // Uses object color if there is a hit, otherwise uses background color
                if (intersection != null) {
                    image.setRGB(x, y, intersection.getColor().getRGB());
                } else {
                    image.setRGB(x, y, backgroundColor.getRGB());
                }
            }
        }

        return image;
    }

    // Saves the rendered image to a file
    public void saveImage(BufferedImage image, String fileName) {
        try {
            ImageIO.write(image, "png", new File(fileName));
            System.out.println("Image saved as " + fileName);
        } catch (Exception e) {
            System.out.println("Error saving image: " + e.getMessage());
        }
    }

    // Creates a simple test scene and renders it
    public static void main(String[] args) {
        // Creates the camera
        Camera camera = new Camera(new Vector3D(0, 0, 0), 800, 600, 60);

        // Creates the scene
        Scene scene = new Scene();
        scene.setCamera(camera);

        // Adds spheres to the scene
        scene.addObject(new Sphere(new Vector3D(-1.5, 0, -5), 1, Color.RED));
        scene.addObject(new Sphere(new Vector3D(1.5, 0, -9), 1, Color.BLUE));
        scene.addObject(new Triangle(
            new Vector3D(-1.2, -1.1, -4),
            new Vector3D(1.2, -1.1, -4),
            new Vector3D(0, 1.1, -4),
            Color.GREEN
        ));

        // Creates the raytracer
        Raytracer raytracer = new Raytracer();

        // Renders the image
        BufferedImage image = raytracer.render(scene);

        // Saves the image
        raytracer.saveImage(image, "render.png");
    }
}
