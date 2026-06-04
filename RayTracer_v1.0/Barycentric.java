public class Barycentric {
    // Prevents creating utility instances.
    private Barycentric() {
    }

    // Computes barycentric weights for a point inside a triangle.
    public static double[] calculate(Vector3D point, Triangle triangle) {
        Vector3D vertexA = triangle.getVertexA();
        Vector3D vertexB = triangle.getVertexB();
        Vector3D vertexC = triangle.getVertexC();

        // Builds triangle edges and the vector from A to the point.
        Vector3D edgeAB = vertexB.subtract(vertexA);
        Vector3D edgeAC = vertexC.subtract(vertexA);
        Vector3D pointVector = point.subtract(vertexA);

        // Computes dot products used by the barycentric formula.
        double dotABAB = edgeAB.dot(edgeAB);
        double dotABAC = edgeAB.dot(edgeAC);
        double dotACAC = edgeAC.dot(edgeAC);
        double dotPointAB = pointVector.dot(edgeAB);
        double dotPointAC = pointVector.dot(edgeAC);
        double denominator = dotABAB * dotACAC - dotABAC * dotABAC;

        // Degenerate triangles fall back to vertex A.
        if (Math.abs(denominator) < 0.000001) {
            return new double[]{1.0, 0.0, 0.0};
        }

        double weightB = (dotACAC * dotPointAB - dotABAC * dotPointAC) / denominator;
        double weightC = (dotABAB * dotPointAC - dotABAC * dotPointAB) / denominator;
        double weightA = 1.0 - weightB - weightC;

        return new double[]{weightA, weightB, weightC};
    }
}
