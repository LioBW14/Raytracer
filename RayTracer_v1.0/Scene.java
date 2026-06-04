import java.util.ArrayList;
import java.util.List;

public class Scene {
    // Stores the active camera.
    private Camera camera;

    // Stores all renderable objects.
    private List<Object3D> objects;

    // Stores all lights used for shading.
    private List<Light> lights;

    // Creates an empty scene
    public Scene() {
        objects = new ArrayList<>();
        lights = new ArrayList<>();
    }

    // Sets the active scene camera.
    public void setCamera(Camera camera) {
        this.camera = camera;
    }

    // Returns the active scene camera.
    public Camera getCamera() {
        return camera;
    }

    // Adds one renderable object to the scene.
    public void addObject(Object3D object) {
        objects.add(object);
    }

    // Returns the scene object list.
    public List<Object3D> getObjects() {
        return objects;
    }

    // Adds one light to the scene.
    public void addLight(Light light) {
        lights.add(light);
    }

    // Returns the scene light list.
    public List<Light> getLights() {
        return lights;
    }

    // Finds the closest object intersection for a ray.
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
