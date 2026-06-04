public class AxisAlignedBoundingBox {
    private Vector3D min;
    private Vector3D max;

    public AxisAlignedBoundingBox(Vector3D min, Vector3D max) {
        this.min = min;
        this.max = max;
    }

    public Vector3D getMin() {
        return min;
    }

    public Vector3D getMax() {
        return max;
    }

    public boolean intersects(Ray ray, double maxDistance) {
        double tMin = 0.000001;
        double tMax = maxDistance;

        double[] origin = new double[]{ray.getOrigin().getX(), ray.getOrigin().getY(), ray.getOrigin().getZ()};
        double[] direction = new double[]{ray.getDirection().getX(), ray.getDirection().getY(), ray.getDirection().getZ()};
        double[] minimum = new double[]{min.getX(), min.getY(), min.getZ()};
        double[] maximum = new double[]{max.getX(), max.getY(), max.getZ()};

        for (int axis = 0; axis < 3; axis++) {
            if (Math.abs(direction[axis]) < 0.0000001) {
                if (origin[axis] < minimum[axis] || origin[axis] > maximum[axis]) {
                    return false;
                }
                continue;
            }

            double inverseDirection = 1.0 / direction[axis];
            double t0 = (minimum[axis] - origin[axis]) * inverseDirection;
            double t1 = (maximum[axis] - origin[axis]) * inverseDirection;

            if (t0 > t1) {
                double temp = t0;
                t0 = t1;
                t1 = temp;
            }

            tMin = Math.max(tMin, t0);
            tMax = Math.min(tMax, t1);

            if (tMax < tMin) {
                return false;
            }
        }

        return true;
    }

    public static AxisAlignedBoundingBox union(AxisAlignedBoundingBox a, AxisAlignedBoundingBox b) {
        return new AxisAlignedBoundingBox(
            new Vector3D(
                Math.min(a.min.getX(), b.min.getX()),
                Math.min(a.min.getY(), b.min.getY()),
                Math.min(a.min.getZ(), b.min.getZ())
            ),
            new Vector3D(
                Math.max(a.max.getX(), b.max.getX()),
                Math.max(a.max.getY(), b.max.getY()),
                Math.max(a.max.getZ(), b.max.getZ())
            )
        );
    }
}
