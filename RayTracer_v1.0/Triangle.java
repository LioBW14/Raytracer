import java.awt.Color;

public class Triangle extends Object3D {
    // Defines a small threshold for floating point comparisons.
    private static final double EPSILON = 0.000001;

    // Stores the first triangle vertex.
    private Vector3D vertexA;

    // Stores the second triangle vertex.
    private Vector3D vertexB;

    // Stores the third triangle vertex.
    private Vector3D vertexC;

    // Stores the constant normal used for flat shading.
    private Vector3D faceNormal;

    // Stores the first vertex normal used for smooth shading.
    private Vector3D normalA;

    // Stores the second vertex normal used for smooth shading.
    private Vector3D normalB;

    // Stores the third vertex normal used for smooth shading.
    private Vector3D normalC;

    private Vector2D uvA;
    private Vector2D uvB;
    private Vector2D uvC;

    // Creates a triangle from three vertices and a base color.
    public Triangle(Vector3D vertexA, Vector3D vertexB, Vector3D vertexC, Color color) {
        this(vertexA, vertexB, vertexC, color, null, null, null);
    }

    public Triangle(Vector3D vertexA, Vector3D vertexB, Vector3D vertexC, Material material) {
        this(vertexA, vertexB, vertexC, material, null, null, null, null, null, null);
    }

    // Creates a triangle from three vertices, a base color and vertex normals.
    public Triangle(
        Vector3D vertexA,
        Vector3D vertexB,
        Vector3D vertexC,
        Color color,
        Vector3D normalA,
        Vector3D normalB,
        Vector3D normalC
    ) {
        this(vertexA, vertexB, vertexC, new Material(color), normalA, normalB, normalC, null, null, null);
    }

    public Triangle(
        Vector3D vertexA,
        Vector3D vertexB,
        Vector3D vertexC,
        Material material,
        Vector3D normalA,
        Vector3D normalB,
        Vector3D normalC,
        Vector2D uvA,
        Vector2D uvB,
        Vector2D uvC
    ) {
        super(vertexA, material);
        this.vertexA = vertexA;
        this.vertexB = vertexB;
        this.vertexC = vertexC;
        this.faceNormal = vertexB.subtract(vertexA).cross(vertexC.subtract(vertexA)).normalize();
        this.normalA = normalA;
        this.normalB = normalB;
        this.normalC = normalC;
        this.uvA = uvA;
        this.uvB = uvB;
        this.uvC = uvC;
    }

    // Returns the first triangle vertex.
    public Vector3D getVertexA() {
        return vertexA;
    }

    // Returns the second triangle vertex.
    public Vector3D getVertexB() {
        return vertexB;
    }

    // Returns the third triangle vertex.
    public Vector3D getVertexC() {
        return vertexC;
    }

    // Returns the constant face normal.
    public Vector3D getFaceNormal() {
        return faceNormal;
    }

    // Returns the vertex normals used for smooth shading.
    public Vector3D[] getVertexNormals() {
        return new Vector3D[]{normalA, normalB, normalC};
    }

    public Vector2D[] getVertexUVs() {
        return new Vector2D[]{uvA, uvB, uvC};
    }

    public Vector3D getCentroid() {
        return vertexA.add(vertexB).add(vertexC).divide(3.0);
    }

    public AxisAlignedBoundingBox getBounds() {
        double minX = Math.min(vertexA.getX(), Math.min(vertexB.getX(), vertexC.getX()));
        double minY = Math.min(vertexA.getY(), Math.min(vertexB.getY(), vertexC.getY()));
        double minZ = Math.min(vertexA.getZ(), Math.min(vertexB.getZ(), vertexC.getZ()));
        double maxX = Math.max(vertexA.getX(), Math.max(vertexB.getX(), vertexC.getX()));
        double maxY = Math.max(vertexA.getY(), Math.max(vertexB.getY(), vertexC.getY()));
        double maxZ = Math.max(vertexA.getZ(), Math.max(vertexB.getZ(), vertexC.getZ()));
        double padding = 0.00001;

        return new AxisAlignedBoundingBox(
            new Vector3D(minX - padding, minY - padding, minZ - padding),
            new Vector3D(maxX + padding, maxY + padding, maxZ + padding)
        );
    }

    // Updates the vertex normals used by smooth shading.
    public void setVertexNormals(Vector3D normalA, Vector3D normalB, Vector3D normalC) {
        this.normalA = normalA;
        this.normalB = normalB;
        this.normalC = normalC;
    }

    // Returns the interpolated smooth normal or the face normal as fallback.
    @Override
    public Vector3D getNormal(Vector3D point) {
        if (normalA == null || normalB == null || normalC == null) {
            return faceNormal;
        }

        // Computes barycentric weights for the hit point.
        double[] weights = Barycentric.calculate(point, this);

        // Interpolates vertex normals for Phong interpolation.
        return interpolateNormal(weights[0], weights[1], weights[2]);
    }

    // Interpolates vertex normals from barycentric weights.
    private Vector3D interpolateNormal(double weightA, double weightB, double weightC) {
        if (normalA == null || normalB == null || normalC == null) {
            return faceNormal;
        }

        return normalA.multiply(weightA)
            .add(normalB.multiply(weightB))
            .add(normalC.multiply(weightC))
            .normalize();
    }

    private Vector2D interpolateUV(double weightA, double weightB, double weightC) {
        if (uvA == null || uvB == null || uvC == null) {
            return new Vector2D(0.0, 0.0);
        }

        return uvA.multiply(weightA)
            .add(uvB.multiply(weightB))
            .add(uvC.multiply(weightC));
    }

    // Computes the intersection between the triangle and a ray.
    @Override
    public Intersection intersect(Ray ray) {
        // Builds two triangle edges from the stored vertices.
        Vector3D edgeAB = vertexB.subtract(vertexA);
        Vector3D edgeAC = vertexC.subtract(vertexA);

        // Starts the Moller-Trumbore intersection test.
        Vector3D pVector = ray.getDirection().cross(edgeAC);
        double determinant = edgeAB.dot(pVector);

        // Parallel rays do not intersect the triangle plane
        if (Math.abs(determinant) < EPSILON) {
            return null;
        }

        double inverseDeterminant = 1.0 / determinant;
        Vector3D tVector = ray.getOrigin().subtract(vertexA);

        // Computes the first barycentric coordinate.
        double u = tVector.dot(pVector) * inverseDeterminant;

        // Rejects hits outside the triangle.
        if (u < 0.0 || u > 1.0) {
            return null;
        }

        Vector3D qVector = tVector.cross(edgeAB);

        // Computes the second barycentric coordinate.
        double v = ray.getDirection().dot(qVector) * inverseDeterminant;

        // Rejects hits outside the triangle.
        if (v < 0.0 || u + v > 1.0) {
            return null;
        }

        // Computes the distance from the ray origin to the triangle.
        double t = edgeAC.dot(qVector) * inverseDeterminant;

        // Rejects intersections behind the ray origin.
        if (t <= EPSILON) {
            return null;
        }

        // Uses the barycentric values already computed by the intersection test.
        Vector3D hitPoint = ray.getPoint(t);
        double weightA = 1.0 - u - v;
        Vector3D hitNormal = interpolateNormal(weightA, u, v);
        Vector2D hitUV = interpolateUV(weightA, u, v);

        // Returns the valid triangle hit with its interpolated normal.
        return new Intersection(t, hitPoint, this, hitNormal, hitUV.getX(), hitUV.getY());
    }
}
