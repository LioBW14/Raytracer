import java.awt.Color;

public class Triangle extends Object3D {
    private static final double EPSILON = 0.000001;

    // Stores the triangle vertices
    private Vector3D vertexA;
    private Vector3D vertexB;
    private Vector3D vertexC;

    // Creates a triangle from three vertices and a color
    public Triangle(Vector3D vertexA, Vector3D vertexB, Vector3D vertexC, Color color) {
        super(vertexA, color);
        this.vertexA = vertexA;
        this.vertexB = vertexB;
        this.vertexC = vertexC;
    }

    // Returns the first vertex
    public Vector3D getVertexA() {
        return vertexA;
    }

    // Returns the second vertex
    public Vector3D getVertexB() {
        return vertexB;
    }

    // Returns the third vertex
    public Vector3D getVertexC() {
        return vertexC;
    }

    // Computes the intersection between the triangle and a ray
    @Override
    public Intersection intersect(Ray ray) {
        Vector3D edgeAB = vertexB.subtract(vertexA);
        Vector3D edgeAC = vertexC.subtract(vertexA);
        Vector3D pVector = ray.getDirection().cross(edgeAC);
        double determinant = edgeAB.dot(pVector);

        // Parallel rays do not intersect the triangle plane
        if (Math.abs(determinant) < EPSILON) {
            return null;
        }

        double inverseDeterminant = 1.0 / determinant;
        Vector3D tVector = ray.getOrigin().subtract(vertexA);
        double u = tVector.dot(pVector) * inverseDeterminant;

        if (u < 0.0 || u > 1.0) {
            return null;
        }

        Vector3D qVector = tVector.cross(edgeAB);
        double v = ray.getDirection().dot(qVector) * inverseDeterminant;

        if (v < 0.0 || u + v > 1.0) {
            return null;
        }

        double t = edgeAC.dot(qVector) * inverseDeterminant;

        if (t <= EPSILON) {
            return null;
        }

        return new Intersection(t, ray.getPoint(t), this);
    }
}
