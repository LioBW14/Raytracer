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

    // Creates a triangle from three vertices and a base color.
    public Triangle(Vector3D vertexA, Vector3D vertexB, Vector3D vertexC, Color color) {
        this(vertexA, vertexB, vertexC, color, null, null, null);
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
        super(vertexA, color);
        this.vertexA = vertexA;
        this.vertexB = vertexB;
        this.vertexC = vertexC;
        this.faceNormal = vertexB.subtract(vertexA).cross(vertexC.subtract(vertexA)).normalize();
        this.normalA = normalA;
        this.normalB = normalB;
        this.normalC = normalC;
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

    // Returns the interpolated smooth normal or the face normal as fallback.
    @Override
    public Vector3D getNormal(Vector3D point) {
        if (normalA == null || normalB == null || normalC == null) {
            return faceNormal;
        }

        // Computes barycentric weights for the hit point.
        Vector3D edgeAB = vertexB.subtract(vertexA);
        Vector3D edgeAC = vertexC.subtract(vertexA);
        Vector3D pointVector = point.subtract(vertexA);
        double dotABAB = edgeAB.dot(edgeAB);
        double dotABAC = edgeAB.dot(edgeAC);
        double dotACAC = edgeAC.dot(edgeAC);
        double dotPointAB = pointVector.dot(edgeAB);
        double dotPointAC = pointVector.dot(edgeAC);
        double denominator = dotABAB * dotACAC - dotABAC * dotABAC;

        // Degenerate triangles use the constant face normal.
        if (Math.abs(denominator) < EPSILON) {
            return faceNormal;
        }

        double v = (dotACAC * dotPointAB - dotABAC * dotPointAC) / denominator;
        double w = (dotABAB * dotPointAC - dotABAC * dotPointAB) / denominator;
        double u = 1.0 - v - w;

        // Interpolates vertex normals for Phong interpolation.
        return normalA.multiply(u)
            .add(normalB.multiply(v))
            .add(normalC.multiply(w))
            .normalize();
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

        // Returns the valid triangle hit.
        return new Intersection(t, ray.getPoint(t), this);
    }
}
