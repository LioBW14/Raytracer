import java.awt.Color;
import java.util.List;

public class Model3D extends Object3D {
    // Stores the triangles that form the mesh.
    private List<Triangle> triangles;

    // Stores the model scale.
    private double scale;

    // Stores the model rotation around the Y axis in degrees.
    private double rotationY;

    // Creates a model from a list of triangles and a base color.
    public Model3D(List<Triangle> triangles, Color color) {
        this(triangles, color, 0.0);
    }

    // Creates a model from a list of triangles, a base color and reflectivity.
    public Model3D(List<Triangle> triangles, Color color, double reflectivity) {
        this(triangles, color, new Vector3D(0, 0, 0), 1.0, 0.0, reflectivity);
    }

    // Creates a transformed model from raw triangles.
    public Model3D(
        List<Triangle> triangles,
        Color color,
        Vector3D position,
        double scale,
        double rotationY,
        double reflectivity
    ) {
        super(position, color, reflectivity);
        this.scale = scale;
        this.rotationY = rotationY;
        this.triangles = transformTriangles(triangles);
    }

    // Returns the mesh triangle list.
    public List<Triangle> getTriangles() {
        return triangles;
    }

    // Applies scale, rotation and translation to every triangle.
    private List<Triangle> transformTriangles(List<Triangle> sourceTriangles) {
        java.util.ArrayList<Triangle> transformed = new java.util.ArrayList<>();

        for (Triangle triangle : sourceTriangles) {
            Vector3D[] normals = triangle.getVertexNormals();

            transformed.add(new Triangle(
                transformVertex(triangle.getVertexA()),
                transformVertex(triangle.getVertexB()),
                transformVertex(triangle.getVertexC()),
                color,
                transformNormal(normals[0]),
                transformNormal(normals[1]),
                transformNormal(normals[2])
            ));
        }

        return transformed;
    }

    // Applies model transform to one vertex.
    private Vector3D transformVertex(Vector3D vertex) {
        return vertex.multiply(scale).rotateY(rotationY).add(position);
    }

    // Rotates a normal without scaling or translating it.
    private Vector3D transformNormal(Vector3D normal) {
        if (normal == null) {
            return null;
        }

        return normal.rotateY(rotationY).normalize();
    }

    // Returns a fallback normal when no triangle hit is available.
    @Override
    public Vector3D getNormal(Vector3D point) {
        return new Vector3D(0, 1, 0);
    }

    // Finds the closest triangle hit inside the model.
    @Override
    public Intersection intersect(Ray ray) {
        Intersection closestIntersection = null;
        double closestDistance = Double.MAX_VALUE;

        // Checks every triangle in the mesh.
        for (Triangle triangle : triangles) {
            Intersection intersection = triangle.intersect(ray);

            // Keeps the closest valid triangle hit.
            if (intersection != null && intersection.getDistance() < closestDistance) {
                closestDistance = intersection.getDistance();
                closestIntersection = intersection;
            }
        }

        if (closestIntersection == null) {
            return null;
        }

        // Returns the model as the hit object while preserving the triangle normal.
        return new Intersection(
            closestIntersection.getDistance(),
            closestIntersection.getPosition(),
            this,
            closestIntersection.getNormal()
        );
    }
}
