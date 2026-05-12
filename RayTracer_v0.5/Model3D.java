import java.awt.Color;
import java.util.List;

public class Model3D extends Object3D {
    // Stores the triangles that form the mesh.
    private List<Triangle> triangles;

    // Creates a model from a list of triangles and a base color.
    public Model3D(List<Triangle> triangles, Color color) {
        super(new Vector3D(0, 0, 0), color);
        this.triangles = triangles;
    }

    // Returns the mesh triangle list.
    public List<Triangle> getTriangles() {
        return triangles;
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
