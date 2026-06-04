public class RectPlane extends Object3D {
    private static final double EPSILON = 0.000001;

    private Vector3D normal;
    private Vector3D uAxis;
    private Vector3D vAxis;
    private double width;
    private double height;
    private double uScale;
    private double vScale;

    public RectPlane(
        Vector3D center,
        Vector3D uAxis,
        Vector3D vAxis,
        double width,
        double height,
        Material material
    ) {
        this(center, uAxis, vAxis, width, height, material, 1.0, 1.0);
    }

    public RectPlane(
        Vector3D center,
        Vector3D uAxis,
        Vector3D vAxis,
        double width,
        double height,
        Material material,
        double uScale,
        double vScale
    ) {
        super(center, material);
        this.uAxis = uAxis.normalize();
        this.vAxis = vAxis.normalize();
        this.normal = this.uAxis.cross(this.vAxis).normalize();
        this.width = width;
        this.height = height;
        this.uScale = uScale;
        this.vScale = vScale;
    }

    @Override
    public Vector3D getNormal(Vector3D point) {
        return normal;
    }

    @Override
    public Intersection intersect(Ray ray) {
        double denominator = normal.dot(ray.getDirection());

        if (Math.abs(denominator) < EPSILON) {
            return null;
        }

        double t = position.subtract(ray.getOrigin()).dot(normal) / denominator;

        if (t <= EPSILON) {
            return null;
        }

        Vector3D hitPoint = ray.getPoint(t);
        Vector3D local = hitPoint.subtract(position);
        double localU = local.dot(uAxis);
        double localV = local.dot(vAxis);

        if (Math.abs(localU) > width * 0.5 || Math.abs(localV) > height * 0.5) {
            return null;
        }

        double textureU = (localU / width + 0.5) * uScale;
        double textureV = (localV / height + 0.5) * vScale;

        return new Intersection(t, hitPoint, this, normal, textureU, textureV);
    }
}
