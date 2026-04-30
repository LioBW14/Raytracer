import java.util.ArrayList;
import java.util.List;

public class Scene {
    private Camera camera;
    private List<Object3D> objects;

    // Creates an empty scene
    public Scene() {
        objects = new ArrayList<>();
    }

    public void setCamera(Camera camera) {
        this.camera = camera;
    }

    public Camera getCamera() {
        return camera;
    }

    public void addObject(Object3D object) {
        objects.add(object);
    }

    public List<Object3D> getObjects() {
        return objects;
    }

    public Intersection raycast(Ray ray) {
        Intersection closestIntersection = null;
        double closestDistance = Double.MAX_VALUE;

        // Checks all objects in the scene
        for (Object3D object : objects) {
            Intersection intersection = object.intersect(ray);

            // Keeps the closest valid intersection
            if (intersection != null && intersection.getDistance() < closestDistance) {
                closestDistance = intersection.getDistance();
                closestIntersection = intersection;
            }
        }

        return closestIntersection;
    }
}