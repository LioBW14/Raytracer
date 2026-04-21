import java.util.ArrayList;
import java.util.List;

public class Scene {
    // Stores the scene camera
    private Camera camera;

    // Stores all objects in the scene
    private List<Object3D> objects;

    // Creates an empty scene
    public Scene() {
        objects = new ArrayList<>();
    }

    // Sets the scene camera
    public void setCamera(Camera camera) {
        this.camera = camera;
    }

    // Returns the scene camera
    public Camera getCamera() {
        return camera;
    }

    // Adds an object to the scene
    public void addObject(Object3D object) {
        objects.add(object);
    }

    // Returns the list of scene objects
    public List<Object3D> getObjects() {
        return objects;
    }

    // Finds the closest intersection for a ray
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