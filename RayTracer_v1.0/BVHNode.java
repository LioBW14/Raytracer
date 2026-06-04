import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class BVHNode {
    private static final int LEAF_SIZE = 8;

    private AxisAlignedBoundingBox bounds;
    private BVHNode left;
    private BVHNode right;
    private List<Triangle> triangles;

    public BVHNode(List<Triangle> sourceTriangles) {
        bounds = calculateBounds(sourceTriangles);

        if (sourceTriangles.size() <= LEAF_SIZE) {
            triangles = new ArrayList<>(sourceTriangles);
            return;
        }

        int axis = largestAxis(bounds);
        List<Triangle> sorted = new ArrayList<>(sourceTriangles);
        sorted.sort(Comparator.comparingDouble(triangle -> coordinate(triangle.getCentroid(), axis)));

        int middle = sorted.size() / 2;
        left = new BVHNode(sorted.subList(0, middle));
        right = new BVHNode(sorted.subList(middle, sorted.size()));
    }

    public Intersection intersect(Ray ray, double closestDistance) {
        if (!bounds.intersects(ray, closestDistance)) {
            return null;
        }

        if (triangles != null) {
            Intersection closest = null;

            for (Triangle triangle : triangles) {
                Intersection hit = triangle.intersect(ray);

                if (hit != null && hit.getDistance() < closestDistance) {
                    closestDistance = hit.getDistance();
                    closest = hit;
                }
            }

            return closest;
        }

        Intersection leftHit = left.intersect(ray, closestDistance);

        if (leftHit != null) {
            closestDistance = leftHit.getDistance();
        }

        Intersection rightHit = right.intersect(ray, closestDistance);

        if (rightHit != null && (leftHit == null || rightHit.getDistance() < leftHit.getDistance())) {
            return rightHit;
        }

        return leftHit;
    }

    private static AxisAlignedBoundingBox calculateBounds(List<Triangle> triangles) {
        AxisAlignedBoundingBox result = triangles.get(0).getBounds();

        for (int i = 1; i < triangles.size(); i++) {
            result = AxisAlignedBoundingBox.union(result, triangles.get(i).getBounds());
        }

        return result;
    }

    private static int largestAxis(AxisAlignedBoundingBox bounds) {
        Vector3D min = bounds.getMin();
        Vector3D max = bounds.getMax();
        double x = max.getX() - min.getX();
        double y = max.getY() - min.getY();
        double z = max.getZ() - min.getZ();

        if (x >= y && x >= z) {
            return 0;
        }

        if (y >= z) {
            return 1;
        }

        return 2;
    }

    private static double coordinate(Vector3D vector, int axis) {
        if (axis == 0) {
            return vector.getX();
        }

        if (axis == 1) {
            return vector.getY();
        }

        return vector.getZ();
    }
}
