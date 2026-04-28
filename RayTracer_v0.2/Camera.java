public class Camera {
    // Stores the camera position
    private Vector3D position;

    // Stores the image width
    private int width;

    // Stores the image height
    private int height;

    // Stores the field of view in degrees  
    private double fov;

    // Stores the near plane distance used to build the camera frustum
    private double nearPlaneDistance;

    // Creates a camera with position, resolution and FOV
    public Camera(Vector3D position, int width, int height, double fov) {
        this(position, width, height, fov, 1.0);
    }

    // Creates a camera with position, resolution, FOV and near plane distance
    public Camera(Vector3D position, int width, int height, double fov, double nearPlaneDistance) {
        this.position = position;
        this.width = width;
        this.height = height;
        this.fov = fov;
        this.nearPlaneDistance = nearPlaneDistance;
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

    // Returns the near plane distance
    public double getNearPlaneDistance() {
        return nearPlaneDistance;
    }

    // Creates a ray that passes through a pixel
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
