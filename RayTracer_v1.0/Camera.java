public class Camera {
    // Stores the camera origin in world space.
    private Vector3D position;

    // Stores the output image width in pixels.
    private int width;

    // Stores the output image height in pixels.
    private int height;

    // Stores the vertical field of view in degrees.
    private double fov;

    // Stores the distance from the camera to the frustum near plane.
    private double nearPlaneDistance;

    // Stores the direction the camera is looking at.
    private Vector3D forward;

    // Stores the camera horizontal axis.
    private Vector3D right;

    // Stores the camera vertical axis.
    private Vector3D up;

    // Creates a camera with position, resolution and FOV
    public Camera(Vector3D position, int width, int height, double fov) {
        this(position, width, height, fov, 1.0);
    }

    // Creates a camera with position, resolution, FOV and near plane distance.
    public Camera(Vector3D position, int width, int height, double fov, double nearPlaneDistance) {
        this.position = position;
        this.width = width;
        this.height = height;
        this.fov = fov;
        this.nearPlaneDistance = nearPlaneDistance;
        setOrientation(new Vector3D(0, 0, -1), new Vector3D(0, 1, 0));
    }

    // Creates a camera that looks at a target point.
    public Camera(Vector3D position, Vector3D target, int width, int height, double fov) {
        this(position, target, width, height, fov, 1.0);
    }

    // Creates a camera that looks at a target point with a custom near plane distance.
    public Camera(
        Vector3D position,
        Vector3D target,
        int width,
        int height,
        double fov,
        double nearPlaneDistance
    ) {
        this.position = position;
        this.width = width;
        this.height = height;
        this.fov = fov;
        this.nearPlaneDistance = nearPlaneDistance;
        setOrientation(target.subtract(position), new Vector3D(0, 1, 0));
    }

    // Returns the camera position.
    public Vector3D getPosition() {
        return position;
    }

    // Returns the image width.
    public int getWidth() {
        return width;
    }

    // Returns the image height.
    public int getHeight() {
        return height;
    }

    // Returns the vertical field of view.
    public double getFov() {
        return fov;
    }

    // Returns the near plane distance.
    public double getNearPlaneDistance() {
        return nearPlaneDistance;
    }

    // Builds the camera basis from a forward direction and a world up vector.
    private void setOrientation(Vector3D forwardDirection, Vector3D worldUp) {
        this.forward = forwardDirection.normalize();
        this.right = forward.cross(worldUp).normalize();

        if (right.magnitude() == 0) {
            this.right = new Vector3D(1, 0, 0);
        }

        this.up = right.cross(forward).normalize();
    }

    // Creates a ray that starts at the camera and passes through one pixel.
    public Ray generateRay(int x, int y) {
        return generateRay(x, y, 0.5, 0.5);
    }

    // Creates a ray through a sub-pixel sample.
    public Ray generateRay(int x, int y, double sampleX, double sampleY) {
        // Converts pixel coordinates to normalized device coordinates
        double ndcX = (x + sampleX) / width;
        double ndcY = (y + sampleY) / height;

        // Computes the near plane size of the camera frustum
        double aspectRatio = (double) width / height;
        double nearPlaneHeight = 2.0 * Math.tan(Math.toRadians(fov * 0.5)) * nearPlaneDistance;
        double nearPlaneWidth = nearPlaneHeight * aspectRatio;

        // Maps the pixel to a point on the near plane of the frustum
        double cameraX = (ndcX - 0.5) * nearPlaneWidth;
        double cameraY = (0.5 - ndcY) * nearPlaneHeight;
        double cameraZ = nearPlaneDistance;

        // Builds the ray direction from the camera position into the frustum
        Vector3D direction = right.multiply(cameraX)
            .add(up.multiply(cameraY))
            .add(forward.multiply(cameraZ))
            .normalize();

        return new Ray(position, direction);
    }
}
