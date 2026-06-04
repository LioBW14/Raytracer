public class IrregularPuddle extends Object3D {
    private static final double EPSILON = 0.000001;

    private Vector3D normal;
    private Vector3D uAxis;
    private Vector3D vAxis;
    private double radiusU;
    private double radiusV;
    private double roughness;
    private double seed;

    public IrregularPuddle(
        Vector3D center,
        Vector3D uAxis,
        Vector3D vAxis,
        double radiusU,
        double radiusV,
        double roughness,
        double seed,
        Material material
    ) {
        super(center, material);
        this.uAxis = uAxis.normalize();
        this.vAxis = vAxis.normalize();
        this.normal = this.uAxis.cross(this.vAxis).normalize();
        this.radiusU = radiusU;
        this.radiusV = radiusV;
        this.roughness = roughness;
        this.seed = seed;
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
        double normalizedU = localU / radiusU;
        double normalizedV = localV / radiusV;
        double radius = Math.sqrt(normalizedU * normalizedU + normalizedV * normalizedV);
        double angle = Math.atan2(normalizedV, normalizedU);
        double boundary = 1.0
            + roughness * 0.38 * Math.sin(3.0 * angle + seed)
            + roughness * 0.26 * Math.sin(5.0 * angle + seed * 1.7)
            + roughness * 0.16 * Math.sin(9.0 * angle + seed * 0.6);

        if (radius > boundary) {
            return null;
        }

        double textureU = localU / (radiusU * 2.0) + 0.5;
        double textureV = localV / (radiusV * 2.0) + 0.5;

        return new Intersection(t, hitPoint, this, normal, textureU, textureV);
    }
}
