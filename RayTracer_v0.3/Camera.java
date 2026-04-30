public class Camera {
    private Vector3D position;
    private int width;
    private int height;
    private double fov;
    private double nearPlaneDistance;

    // Creates a camera with position, resolution and FOV
    public Camera(Vector3D position, int width, int height, double fov) {
        this(position, width, height, fov, 1.0);
    }

    public Camera(Vector3D position, int width, int height, double fov, double nearPlaneDistance) {
        this.position = position;
        this.width = width;
        this.height = height;
        this.fov = fov;
        this.nearPlaneDistance = nearPlaneDistance;
    }

    public Vector3D getPosition() {
        return position;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public double getFov() {
        return fov;
    }

    public double getNearPlaneDistance() {
        return nearPlaneDistance;
    }

    public Ray generateRay(int x, int y) {
        // Converts pixel coordinates to normalized device coordinates
        double ndcX = (x + 0.5) / width;
        double ndcY = (y + 0.5) / height;

        // Computes the near plane size of the camera frustum
        double aspectRatio = (double) width / height;
        double nearPlaneHeight = 2.0 * Math.tan(Math.toRadians(fov * 0.5)) * nearPlaneDistance;
        double nearPlaneWidth = nearPlaneHeight * aspectRatio;

        // Maps the pixel to a point on the near plane of the frustum
        double cameraX = (ndcX - 0.5) * nearPlaneWidth;
        double cameraY = (0.5 - ndcY) * nearPlaneHeight;
        double cameraZ = -nearPlaneDistance;

        // Builds the ray direction from the camera position into the frustum
        Vector3D direction = new Vector3D(cameraX, cameraY, cameraZ).normalize();

        return new Ray(position, direction);
    }
}
