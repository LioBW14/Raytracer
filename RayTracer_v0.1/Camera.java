public class Camera {
    // Stores the camera position
    private Vector3D position;

    // Stores the image width
    private int width;

    // Stores the image height
    private int height;

    // Stores the field of view in degrees
    private double fov;

    // Creates a camera with position, resolution and FOV
    public Camera(Vector3D position, int width, int height, double fov) {
        this.position = position;
        this.width = width;
        this.height = height;
        this.fov = fov;
    }

    // Returns the camera position
    public Vector3D getPosition() {
        return position;
    }

    // Returns the image width
    public int getWidth() {
        return width;
    }

    // Returns the image height
    public int getHeight() {
        return height;
    }

    // Returns the field of view
    public double getFov() {
        return fov;
    }

    // Creates a ray that passes through a pixel
    public Ray generateRay(int x, int y) {
        // Converts pixel coordinates to normalized device coordinates
        double ndcX = (x + 0.5) / width;
        double ndcY = (y + 0.5) / height;

        // Converts normalized coordinates to screen space
        double screenX = 2.0 * ndcX - 1.0;
        double screenY = 1.0 - 2.0 * ndcY;

        // Computes aspect ratio and camera scale
        double aspectRatio = (double) width / height;
        double scale = Math.tan(Math.toRadians(fov * 0.5));

        // Maps the pixel to camera space
        double cameraX = screenX * aspectRatio * scale;
        double cameraY = screenY * scale;
        double cameraZ = -1.0;

        // Builds the ray direction
        Vector3D direction = new Vector3D(cameraX, cameraY, cameraZ).normalize();

        return new Ray(position, direction);
    }
}