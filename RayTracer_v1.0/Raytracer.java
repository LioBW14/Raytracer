import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.stream.IntStream;
import javax.imageio.ImageIO;

public class Raytracer {
    private static final double SHADOW_BIAS = 0.0008;
    private static final double RAY_BIAS = 0.0008;

    private Color backgroundColor;
    private Texture backgroundTexture;
    private int maxDepth;
    private int samplesPerAxis;

    public Raytracer() {
        backgroundColor = new Color(8, 10, 16);
        maxDepth = 4;
        samplesPerAxis = 1;
    }

    public Raytracer setBackgroundTexture(Texture backgroundTexture) {
        this.backgroundTexture = backgroundTexture;
        return this;
    }

    public Raytracer setSamplesPerAxis(int samplesPerAxis) {
        this.samplesPerAxis = Math.max(1, samplesPerAxis);
        return this;
    }

    public Raytracer setMaxDepth(int maxDepth) {
        this.maxDepth = Math.max(1, maxDepth);
        return this;
    }

    public BufferedImage render(Scene scene) {
        Camera camera = scene.getCamera();
        int width = camera.getWidth();
        int height = camera.getHeight();
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        IntStream.range(0, height).parallel().forEach(y -> {
            for (int x = 0; x < width; x++) {
                image.setRGB(x, y, samplePixel(scene, camera, x, y).getRGB());
            }
        });

        return image;
    }

    private Color samplePixel(Scene scene, Camera camera, int x, int y) {
        double red = 0.0;
        double green = 0.0;
        double blue = 0.0;
        double sampleCount = samplesPerAxis * samplesPerAxis;

        for (int sampleY = 0; sampleY < samplesPerAxis; sampleY++) {
            for (int sampleX = 0; sampleX < samplesPerAxis; sampleX++) {
                double offsetX = (sampleX + 0.5) / samplesPerAxis;
                double offsetY = (sampleY + 0.5) / samplesPerAxis;
                Color sampleColor = traceRay(scene, camera.generateRay(x, y, offsetX, offsetY), 0);

                red += sampleColor.getRed();
                green += sampleColor.getGreen();
                blue += sampleColor.getBlue();
            }
        }

        return new Color(
            clampColor(red / sampleCount),
            clampColor(green / sampleCount),
            clampColor(blue / sampleCount)
        );
    }

    private Color traceRay(Scene scene, Ray ray, int depth) {
        Intersection intersection = scene.raycast(ray);

        if (intersection == null) {
            return sampleBackground(ray);
        }

        Material material = intersection.getMaterial();
        Color localColor = shade(scene, intersection, ray);

        if (depth >= maxDepth) {
            return localColor;
        }

        Vector3D normal = orientNormal(intersection.getNormal(), ray);
        double reflectivity = material.getReflectivity();
        double transparency = material.getTransparency();
        Color reflectionColor = Color.BLACK;
        Color refractionColor = Color.BLACK;
        double reflectionWeight = reflectivity;
        double refractionWeight = 0.0;

        if (reflectivity > 0.0 || transparency > 0.0) {
            Vector3D reflectionDirection = reflect(ray.getDirection(), normal);
            Vector3D reflectionOrigin = intersection.getPosition().add(normal.multiply(RAY_BIAS));
            reflectionColor = traceRay(scene, new Ray(reflectionOrigin, reflectionDirection), depth + 1);
        }

        if (transparency > 0.0) {
            double fresnel = fresnelSchlick(ray.getDirection(), intersection.getNormal(), material.getRefractiveIndex());
            Vector3D refractionDirection = refract(
                ray.getDirection(),
                intersection.getNormal(),
                material.getRefractiveIndex()
            );

            reflectionWeight = Math.max(reflectivity, fresnel);

            if (refractionDirection != null) {
                Vector3D refractionOrigin = intersection.getPosition().add(refractionDirection.multiply(RAY_BIAS));
                refractionColor = traceRay(scene, new Ray(refractionOrigin, refractionDirection), depth + 1);
                refractionWeight = transparency * (1.0 - fresnel);
            }
        }

        double localWeight = Math.max(0.0, 1.0 - reflectionWeight - refractionWeight);

        return combine(localColor, localWeight, reflectionColor, reflectionWeight, refractionColor, refractionWeight);
    }

    private Color shade(Scene scene, Intersection intersection, Ray ray) {
        Material material = intersection.getMaterial();
        Color baseColor = intersection.getColor();
        Vector3D normal = orientNormal(intersection.getNormal(), ray);
        Vector3D viewDirection = ray.getDirection().multiply(-1.0).normalize();

        double red = baseColor.getRed() * material.getAmbient();
        double green = baseColor.getGreen() * material.getAmbient();
        double blue = baseColor.getBlue() * material.getAmbient();

        for (Light light : scene.getLights()) {
            Vector3D lightDirection = light.getDirectionToLight(intersection.getPosition());
            double falloff = light.getFalloff(intersection.getPosition());
            double nDotL = Math.max(0.0, normal.dot(lightDirection));

            if (nDotL <= 0.0 || isInShadow(scene, intersection, normal, light)) {
                continue;
            }

            double diffuse = nDotL * light.getIntensity() * falloff * material.getDiffuse();
            Vector3D halfVector = lightDirection.add(viewDirection).normalize();
            double specular = Math.pow(
                Math.max(0.0, normal.dot(halfVector)),
                material.getShininess()
            ) * material.getSpecular() * light.getIntensity() * falloff;

            red += baseColor.getRed() * (light.getColor().getRed() / 255.0) * diffuse;
            green += baseColor.getGreen() * (light.getColor().getGreen() / 255.0) * diffuse;
            blue += baseColor.getBlue() * (light.getColor().getBlue() / 255.0) * diffuse;

            red += 255.0 * (light.getColor().getRed() / 255.0) * specular;
            green += 255.0 * (light.getColor().getGreen() / 255.0) * specular;
            blue += 255.0 * (light.getColor().getBlue() / 255.0) * specular;
        }

        return new Color(clampColor(red), clampColor(green), clampColor(blue));
    }

    private boolean isInShadow(Scene scene, Intersection intersection, Vector3D normal, Light light) {
        Vector3D point = intersection.getPosition();
        Vector3D lightDirection = light.getDirectionToLight(point);
        Vector3D shadowOrigin = point.add(normal.multiply(SHADOW_BIAS));
        Intersection blocker = scene.raycast(new Ray(shadowOrigin, lightDirection));

        if (blocker == null) {
            return false;
        }

        double blockerDistance = blocker.getDistance();
        double lightDistance = light.getDistanceToLight(point);
        return blockerDistance > SHADOW_BIAS && blockerDistance < lightDistance - SHADOW_BIAS;
    }

    private Color sampleBackground(Ray ray) {
        if (backgroundTexture == null) {
            return backgroundColor;
        }

        Vector3D direction = ray.getDirection().normalize();
        double u = 0.5 + direction.getX() * 0.42;
        double v = 0.34 + direction.getY() * 0.62;
        return backgroundTexture.sample(u, v);
    }

    private Vector3D orientNormal(Vector3D normal, Ray ray) {
        Vector3D normalized = normal.normalize();

        if (normalized.dot(ray.getDirection()) > 0.0) {
            return normalized.multiply(-1.0);
        }

        return normalized;
    }

    private Vector3D reflect(Vector3D incoming, Vector3D normal) {
        return incoming.subtract(normal.multiply(2.0 * incoming.dot(normal))).normalize();
    }

    private Vector3D refract(Vector3D incoming, Vector3D normal, double refractiveIndex) {
        Vector3D n = normal.normalize();
        double cosi = Math.max(-1.0, Math.min(1.0, incoming.dot(n)));
        double etai = 1.0;
        double etat = refractiveIndex;

        if (cosi < 0.0) {
            cosi = -cosi;
        } else {
            double temp = etai;
            etai = etat;
            etat = temp;
            n = n.multiply(-1.0);
        }

        double eta = etai / etat;
        double k = 1.0 - eta * eta * (1.0 - cosi * cosi);

        if (k < 0.0) {
            return null;
        }

        return incoming.multiply(eta).add(n.multiply(eta * cosi - Math.sqrt(k))).normalize();
    }

    private double fresnelSchlick(Vector3D incoming, Vector3D normal, double refractiveIndex) {
        double cosi = Math.max(-1.0, Math.min(1.0, incoming.dot(normal.normalize())));
        double etai = 1.0;
        double etat = refractiveIndex;

        if (cosi > 0.0) {
            double temp = etai;
            etai = etat;
            etat = temp;
        }

        double sint = etai / etat * Math.sqrt(Math.max(0.0, 1.0 - cosi * cosi));

        if (sint >= 1.0) {
            return 1.0;
        }

        double cost = Math.sqrt(Math.max(0.0, 1.0 - sint * sint));
        cosi = Math.abs(cosi);
        double r0 = Math.pow((etat - etai) / (etat + etai), 2.0);
        return r0 + (1.0 - r0) * Math.pow(1.0 - (cosi > 0.0 ? cosi : cost), 5.0);
    }

    private Color combine(
        Color localColor,
        double localWeight,
        Color reflectionColor,
        double reflectionWeight,
        Color refractionColor,
        double refractionWeight
    ) {
        double total = localWeight + reflectionWeight + refractionWeight;

        if (total <= 0.0) {
            return localColor;
        }

        return new Color(
            clampColor(localColor.getRed() * localWeight
                + reflectionColor.getRed() * reflectionWeight
                + refractionColor.getRed() * refractionWeight),
            clampColor(localColor.getGreen() * localWeight
                + reflectionColor.getGreen() * reflectionWeight
                + refractionColor.getGreen() * refractionWeight),
            clampColor(localColor.getBlue() * localWeight
                + reflectionColor.getBlue() * reflectionWeight
                + refractionColor.getBlue() * refractionWeight)
        );
    }

    private int clampColor(double value) {
        return Math.max(0, Math.min(255, (int) Math.round(value)));
    }

    public void saveImage(BufferedImage image, String fileName) {
        try {
            ImageIO.write(image, "png", new File(fileName));
            System.out.println("Image saved as " + fileName);
        } catch (Exception e) {
            System.out.println("Error saving image: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        String mode = args.length > 0 ? args[0].toLowerCase() : "scene3-final";
        boolean renderScene1 = mode.equals("scene1-final") || mode.equals("scene1");
        boolean renderScene2 = mode.equals("scene2-final") || mode.equals("scene2");
        int width = 4096;
        int height = 2160;
        int samples = 2;
        int maxDepth = 5;
        String output = "scene3_final.png";

        // Final render selector. All modes are 4K deliverables.
        if (renderScene2) {
            output = "scene2_final.png";
        } else if (renderScene1) {
            output = "scene1_final.png";
        }

        /*
         * Scene 1 Batman final config kept inactive for reference:
         * java Raytracer scene1-final
         * camera: (0.28, 0.30, 1.55) -> (-0.16, 0.48, -3.20), FOV 56
         * Batman: scale 1.18, position (0.08, 0.0, -4.70), rotation (0, 42, 0)
         * output: scene1_final.png at 4096x2160, 2x2 samples, maxDepth 5
         */

        try {
            Scene scene;

            if (renderScene1) {
                scene = buildScene1(width, height);
            } else if (renderScene2) {
                scene = buildScene2(width, height);
            } else {
                scene = buildScene3(width, height);
            }

            Raytracer raytracer = new Raytracer()
                .setSamplesPerAxis(samples)
                .setMaxDepth(maxDepth);

            if (renderScene1) {
                raytracer.setBackgroundTexture(new Texture("Scene 1/Techo 01.png"));
            }

            raytracer.saveImage(raytracer.render(scene), output);
        } catch (Exception e) {
            System.out.println("Render failed for " + mode + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Scene 3: labor stays in the center while the products of that labor are displayed around it.
    private static Scene buildScene3(int width, int height) throws Exception {
        Scene scene = new Scene();
        scene.setCamera(new Camera(
            new Vector3D(0.18, 1.18, 1.85),
            new Vector3D(-0.10, 1.05, -3.65),
            width,
            height,
            54
        ));

        Texture ironManTexture = new Texture("Scene 3/iron-man/textures/Octane_default_BaseColor.1001.png");
        Texture carTexture = new Texture("Scene 3/Car/uploads_files_20080_nissan_gtr_obj/gt-r.jpg");

        Material floorMaterial = new Material(new Color(42, 45, 49))
            .setAmbient(0.10)
            .setDiffuse(0.42)
            .setSpecular(0.86)
            .setShininess(170.0)
            .setReflectivity(0.38);
        Material backWallMaterial = new Material(new Color(30, 34, 40))
            .setAmbient(0.12)
            .setDiffuse(0.44)
            .setSpecular(0.56)
            .setShininess(120.0)
            .setReflectivity(0.22);
        Material sideWallMaterial = new Material(new Color(25, 29, 36))
            .setAmbient(0.11)
            .setDiffuse(0.42)
            .setSpecular(0.50)
            .setShininess(110.0)
            .setReflectivity(0.20);
        Material ceilingMaterial = new Material(new Color(20, 22, 27))
            .setAmbient(0.08)
            .setDiffuse(0.36)
            .setSpecular(0.38)
            .setShininess(96.0)
            .setReflectivity(0.16);
        Material robotArmMaterial = new Material(new Color(86, 94, 102))
            .setAmbient(0.08)
            .setDiffuse(0.38)
            .setSpecular(0.95)
            .setShininess(180.0)
            .setReflectivity(0.36);
        Material carMaterial = new Material(Color.WHITE, carTexture)
            .setAmbient(0.08)
            .setDiffuse(0.50)
            .setSpecular(0.86)
            .setShininess(170.0)
            .setReflectivity(0.30);
        Material ironManMaterial = new Material(Color.WHITE, ironManTexture)
            .setAmbient(0.08)
            .setDiffuse(0.48)
            .setSpecular(0.92)
            .setShininess(190.0)
            .setReflectivity(0.26);
        // The iMac texture atlas does not map well as a single diffuse texture, so the model uses a clean solid material.
        Material imacMaterial = new Material(new Color(190, 194, 196))
            .setAmbient(0.10)
            .setDiffuse(0.46)
            .setSpecular(0.82)
            .setShininess(140.0)
            .setReflectivity(0.28);
        Material keyboardMaterial = new Material(new Color(170, 174, 176))
            .setAmbient(0.10)
            .setDiffuse(0.48)
            .setSpecular(0.68)
            .setShininess(120.0)
            .setReflectivity(0.18);
        Material keyMaterial = new Material(new Color(228, 230, 228))
            .setAmbient(0.12)
            .setDiffuse(0.62)
            .setSpecular(0.28)
            .setShininess(60.0)
            .setReflectivity(0.04);
        Material mouseMaterial = new Material(new Color(210, 214, 214))
            .setAmbient(0.10)
            .setDiffuse(0.52)
            .setSpecular(0.70)
            .setShininess(130.0)
            .setReflectivity(0.16);
        Material controllerMaterial = new Material(new Color(218, 220, 216))
            .setAmbient(0.10)
            .setDiffuse(0.52)
            .setSpecular(0.62)
            .setShininess(110.0)
            .setReflectivity(0.18);
        Material plinthMaterial = new Material(new Color(38, 41, 45))
            .setAmbient(0.08)
            .setDiffuse(0.38)
            .setSpecular(0.78)
            .setShininess(150.0)
            .setReflectivity(0.30);
        Material glassBarrierMaterial = new Material(new Color(120, 160, 190))
            .setAmbient(0.02)
            .setDiffuse(0.08)
            .setSpecular(0.90)
            .setShininess(190.0)
            .setReflectivity(0.24)
            .setTransparency(0.22)
            .setRefractiveIndex(1.45);

        scene.addObject(new RectPlane(
            new Vector3D(0.0, 0.0, -3.55),
            new Vector3D(1, 0, 0),
            new Vector3D(0, 0, -1),
            8.4,
            7.4,
            floorMaterial,
            1.0,
            1.0
        ));
        scene.addObject(new RectPlane(
            new Vector3D(0.0, 1.95, -7.25),
            new Vector3D(1, 0, 0),
            new Vector3D(0, 1, 0),
            8.4,
            3.9,
            backWallMaterial,
            1.0,
            1.0
        ));
        scene.addObject(new RectPlane(
            new Vector3D(-4.2, 1.95, -3.55),
            new Vector3D(0, 0, -1),
            new Vector3D(0, 1, 0),
            7.4,
            3.9,
            sideWallMaterial,
            1.0,
            1.0
        ));
        scene.addObject(new RectPlane(
            new Vector3D(4.2, 1.95, -3.55),
            new Vector3D(0, 0, 1),
            new Vector3D(0, 1, 0),
            7.4,
            3.9,
            sideWallMaterial,
            1.0,
            1.0
        ));
        scene.addObject(new RectPlane(
            new Vector3D(0.0, 3.90, -3.55),
            new Vector3D(1, 0, 0),
            new Vector3D(0, 0, 1),
            8.4,
            7.4,
            ceilingMaterial,
            1.0,
            1.0
        ));

        scene.addObject(new RectPlane(
            new Vector3D(0.0, 0.025, -3.22),
            new Vector3D(1, 0, 0),
            new Vector3D(0, 0, -1),
            2.55,
            2.20,
            plinthMaterial,
            1.0,
            1.0
        ));
        scene.addObject(new RectPlane(
            new Vector3D(-2.35, 0.040, -5.18),
            new Vector3D(1, 0, 0),
            new Vector3D(0, 0, -1),
            1.15,
            1.00,
            plinthMaterial,
            1.0,
            1.0
        ));
        scene.addObject(new RectPlane(
            new Vector3D(0.0, 1.36, -4.98),
            new Vector3D(1, 0, 0),
            new Vector3D(0, 1, 0),
            1.80,
            1.35,
            glassBarrierMaterial,
            1.0,
            1.0
        ));

        scene.addObject(OBJLoader.loadModel(
            "Scene 3/sk095yah4v7k-ModelRmk3/robotic_arm.obj",
            robotArmMaterial,
            0.0039,
            new Vector3D(-0.84, 0.0054, -3.35),
            0.0,
            -16.0,
            0.0
        ));
        // The GTR OBJ contains several color variants; only the silver group is loaded as the final car.
        scene.addObject(OBJLoader.loadModelGroup(
            "Scene 3/Car/uploads_files_20080_nissan_gtr_obj/nissan_gtr.obj",
            carMaterial,
            "silver",
            0.050,
            new Vector3D(2.28, 0.0027, -5.58),
            0.0,
            -74.0,
            0.0
        ));
        scene.addObject(OBJLoader.loadModel(
            "Scene 3/iron-man/iron_man.obj",
            ironManMaterial,
            0.00075,
            new Vector3D(-2.35, 0.042, -5.20),
            0.0,
            18.0,
            0.0
        ));
        scene.addObject(OBJLoader.loadModel(
            "Scene 3/mac/imac.obj",
            imacMaterial,
            0.018,
            new Vector3D(-2.45, -0.026, -2.16),
            0.0,
            32.0,
            0.0
        ));
        // Keyboard and mouse are added manually because they read better than the full iMac texture atlas.
        scene.addObject(new RectPlane(
            new Vector3D(-2.38, 0.057, -1.76),
            new Vector3D(1, 0, 0),
            new Vector3D(0, 0, -1),
            0.70,
            0.22,
            keyboardMaterial,
            1.0,
            1.0
        ));
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                scene.addObject(new RectPlane(
                    new Vector3D(-2.68 + col * 0.075 + row * 0.010, 0.060, -1.695 - row * 0.055),
                    new Vector3D(1, 0, 0),
                    new Vector3D(0, 0, -1),
                    0.052,
                    0.032,
                    keyMaterial,
                    1.0,
                    1.0
                ));
            }
        }
        scene.addObject(new IrregularPuddle(
            new Vector3D(-1.84, 0.060, -1.78),
            new Vector3D(1, 0, 0),
            new Vector3D(0, 0, -1),
            0.115,
            0.070,
            0.08,
            15.3,
            mouseMaterial
        ));
        scene.addObject(OBJLoader.loadModel(
            "Scene 3/30-xbox-one-controller/Xbox one controller/xbox_controller.obj",
            controllerMaterial,
            0.085,
            new Vector3D(2.03, 0.260, -1.75),
            0.0,
            -24.0,
            0.0
        ));

        scene.addLight(new Light(new Vector3D(-0.30, 0.65, 0.35), new Color(90, 115, 155), 0.14));
        scene.addLight(new PointLight(
            new Vector3D(0.0, 3.20, -2.65),
            new Color(205, 230, 255),
            5.2,
            1.0,
            0.22,
            0.08
        ));
        scene.addLight(new PointLight(
            new Vector3D(-2.70, 1.75, -2.05),
            new Color(120, 170, 255),
            1.85,
            1.0,
            0.18,
            0.09
        ));
        scene.addLight(new PointLight(
            new Vector3D(2.70, 1.35, -5.30),
            new Color(255, 196, 120),
            2.1,
            1.0,
            0.20,
            0.10
        ));

        return scene;
    }

    // Scene 2: a private room where expensive objects reflect emotional emptiness.
    private static Scene buildScene2(int width, int height) throws Exception {
        Scene scene = new Scene();
        scene.setCamera(new Camera(
            new Vector3D(0.22, 0.82, 1.12),
            new Vector3D(0.16, 0.68, -2.62),
            width,
            height,
            48
        ));

        Texture woodTexture = new Texture("Scene 2/Wood Table with glasplatte/textures/Wood_Table_C.jpg");
        Texture windowTexture = new Texture("Scene 2/Window/tex/window.png");
        Texture bottleTexture = new Texture("Scene 2/wine bottle/14042_750 mL_Wine_Bottle_dfinal.jpg");
        Texture focoTexture = new Texture("Scene 2/foco/all_por.jpg");
        Texture tvTexture = new Texture("Scene 2/Samsung_Smart_TV_55_Zoll/textures/col.png");

        Material floorMaterial = new Material(new Color(55, 42, 38))
            .setAmbient(0.11)
            .setDiffuse(0.50)
            .setSpecular(0.40)
            .setShininess(82.0)
            .setReflectivity(0.20);
        Material backWallMaterial = Material.matte(new Color(31, 30, 36))
            .setAmbient(0.17)
            .setDiffuse(0.50)
            .setSpecular(0.40)
            .setShininess(82.0)
            .setReflectivity(0.20);
        Material sideWallMaterial = Material.matte(new Color(27, 30, 39))
            .setAmbient(0.16)
            .setDiffuse(0.50)
            .setSpecular(0.40)
            .setShininess(82.0)
            .setReflectivity(0.20);
        Material ceilingMaterial = Material.matte(new Color(25, 24, 29))
            .setAmbient(0.12)
            .setDiffuse(0.48)
            .setSpecular(0.40)
            .setShininess(82.0)
            .setReflectivity(0.20);
        Material oldManMaterial = new Material(new Color(66, 61, 59))
            .setAmbient(0.08)
            .setDiffuse(0.48)
            .setSpecular(0.55)
            .setShininess(95.0)
            .setReflectivity(0.18);
        Material bedMaterial = new Material(new Color(58, 54, 64))
            .setAmbient(0.075)
            .setDiffuse(0.36)
            .setSpecular(0.34)
            .setShininess(70.0)
            .setReflectivity(0.08)
            .setTransparency(0.18)
            .setRefractiveIndex(1.18);
        Material tableMaterial = new Material(Color.WHITE, woodTexture)
            .setAmbient(0.09)
            .setDiffuse(0.50)
            .setSpecular(0.58)
            .setShininess(96.0)
            .setReflectivity(0.26);
        Material windowMaterial = new Material(Color.WHITE, windowTexture)
            .setAmbient(0.13)
            .setDiffuse(0.42)
            .setSpecular(0.38)
            .setShininess(96.0)
            .setReflectivity(0.10)
            .setTransparency(0.08)
            .setRefractiveIndex(1.45);
        Material bottleMaterial = new Material(new Color(135, 170, 145), bottleTexture)
            .setAmbient(0.04)
            .setDiffuse(0.26)
            .setSpecular(0.88)
            .setShininess(150.0)
            .setReflectivity(0.28)
            .setTransparency(0.32)
            .setRefractiveIndex(1.47);
        Material goldMaterial = new Material(new Color(245, 203, 86))
            .setAmbient(0.06)
            .setDiffuse(0.38)
            .setSpecular(1.0)
            .setShininess(230.0)
            .setReflectivity(0.64);
        Material diamondMaterial = new Material(new Color(210, 238, 255))
            .setAmbient(0.02)
            .setDiffuse(0.12)
            .setSpecular(1.0)
            .setShininess(240.0)
            .setReflectivity(0.34)
            .setTransparency(0.58)
            .setRefractiveIndex(2.42);
        Material saberMaterial = new Material(new Color(28, 32, 38))
            .setAmbient(0.04)
            .setDiffuse(0.36)
            .setSpecular(0.62)
            .setShininess(150.0)
            .setReflectivity(0.18);
        Material focoMaterial = new Material(new Color(210, 202, 190), focoTexture)
            .setAmbient(0.16)
            .setDiffuse(0.42)
            .setSpecular(0.62)
            .setShininess(96.0)
            .setReflectivity(0.10);
        Material socksMaterial = new Material(new Color(65, 62, 68))
            .setAmbient(0.10)
            .setDiffuse(0.66)
            .setSpecular(0.08)
            .setShininess(18.0)
            .setReflectivity(0.01);
        Material tvMaterial = new Material(new Color(32, 32, 36), tvTexture)
            .setAmbient(0.05)
            .setDiffuse(0.30)
            .setSpecular(0.72)
            .setShininess(150.0)
            .setReflectivity(0.30);
        Material wineGlassMaterial = new Material(new Color(205, 220, 225))
            .setAmbient(0.025)
            .setDiffuse(0.05)
            .setSpecular(1.0)
            .setShininess(220.0)
            .setReflectivity(0.10)
            .setTransparency(0.72)
            .setRefractiveIndex(1.47);
        Material winePuddleMaterial = new Material(new Color(76, 15, 96))
            .setAmbient(0.035)
            .setDiffuse(0.18)
            .setSpecular(0.86)
            .setShininess(180.0)
            .setReflectivity(0.32)
            .setTransparency(0.18)
            .setRefractiveIndex(1.36);

        scene.addObject(new RectPlane(
            new Vector3D(0.0, 0.0, -3.0),
            new Vector3D(1, 0, 0),
            new Vector3D(0, 0, -1),
            6.4,
            6.4,
            floorMaterial,
            3.2,
            3.2
        ));
        scene.addObject(new RectPlane(
            new Vector3D(0.0, 1.42, -6.2),
            new Vector3D(1, 0, 0),
            new Vector3D(0, 1, 0),
            6.4,
            2.84,
            backWallMaterial,
            1.0,
            1.0
        ));
        scene.addObject(new RectPlane(
            new Vector3D(-3.2, 1.42, -3.0),
            new Vector3D(0, 0, -1),
            new Vector3D(0, 1, 0),
            6.4,
            2.84,
            sideWallMaterial,
            1.0,
            1.0
        ));
        scene.addObject(new RectPlane(
            new Vector3D(3.2, 1.42, -3.0),
            new Vector3D(0, 0, 1),
            new Vector3D(0, 1, 0),
            6.4,
            2.84,
            sideWallMaterial,
            1.0,
            1.0
        ));
        scene.addObject(new RectPlane(
            new Vector3D(0.0, 2.84, -3.0),
            new Vector3D(1, 0, 0),
            new Vector3D(0, 0, 1),
            6.4,
            6.4,
            ceilingMaterial,
            1.0,
            1.0
        ));

        scene.addObject(OBJLoader.loadModel(
            "Scene 2/Window/window.obj",
            windowMaterial,
            0.035,
            new Vector3D(0.0, 1.40, -6.02),
            0.0,
            90.0,
            0.0
        ));
        scene.addObject(OBJLoader.loadModel(
            "Scene 2/Samsung_Smart_TV_55_Zoll/samsung_smart_tv.obj",
            tvMaterial,
            0.120,
            new Vector3D(2.86, 0.35, -4.78),
            0.0,
            90.0,
            0.0
        ));
        scene.addObject(OBJLoader.loadModel(
            "Scene 2/Bed/bed.obj",
            bedMaterial,
            0.012,
            new Vector3D(-1.65, 0.55, -4.38),
            -90.0,
            0.0,
            0.0
        ));
        scene.addObject(new IrregularPuddle(
            new Vector3D(0.32, 0.013, -1.30),
            new Vector3D(1, 0, 0),
            new Vector3D(0, 0, -1),
            0.28,
            0.12,
            0.92,
            9.1,
            winePuddleMaterial
        ));
        scene.addObject(new IrregularPuddle(
            new Vector3D(1.05, 0.013, -1.58),
            new Vector3D(1, 0, 0),
            new Vector3D(0, 0, -1),
            0.46,
            0.20,
            1.06,
            10.4,
            winePuddleMaterial
        ));
        scene.addObject(new IrregularPuddle(
            new Vector3D(0.64, 0.013, -1.94),
            new Vector3D(1, 0, 0),
            new Vector3D(0, 0, -1),
            0.26,
            0.10,
            1.18,
            11.7,
            winePuddleMaterial
        ));
        scene.addObject(new IrregularPuddle(
            new Vector3D(0.84, 0.642, -2.40),
            new Vector3D(1, 0, 0),
            new Vector3D(0, 0, -1),
            0.22,
            0.08,
            0.98,
            12.6,
            winePuddleMaterial
        ));
        scene.addObject(OBJLoader.loadModel(
            "Scene 2/Old men/old_man_low_resolution.obj",
            oldManMaterial,
            16.5,
            new Vector3D(-0.48, -0.003, -3.42),
            0.0,
            155.0,
            0.0
        ));
        scene.addObject(OBJLoader.loadModel(
            "Scene 2/socks/socks.obj",
            socksMaterial,
            3.8,
            new Vector3D(-1.26, -0.010, -1.74),
            0.0,
            -32.0,
            0.0
        ));
        scene.addObject(OBJLoader.loadModel(
            "Scene 2/socks/socks.obj",
            socksMaterial,
            2.9,
            new Vector3D(-2.05, -0.008, -2.72),
            0.0,
            58.0,
            0.0
        ));
        scene.addObject(OBJLoader.loadModel(
            "Scene 2/Wood Table with glasplatte/wood_table.obj",
            tableMaterial,
            1.35,
            new Vector3D(0.74, -0.006, -2.54),
            0.0,
            -18.0,
            0.0
        ));
        scene.addObject(OBJLoader.loadModel(
            "Scene 2/glass/wine_glass.obj",
            wineGlassMaterial,
            0.060,
            new Vector3D(1.08, 0.635, -2.22),
            0.0,
            12.0,
            0.0
        ));
        scene.addObject(OBJLoader.loadModel(
            "Scene 2/glass/wine_glass.obj",
            wineGlassMaterial,
            0.062,
            new Vector3D(1.04, 0.105, -1.57),
            0.0,
            -32.0,
            88.0
        ));
        scene.addObject(OBJLoader.loadModel(
            "Scene 2/wine bottle/wine_bottle.obj",
            bottleMaterial,
            0.033,
            new Vector3D(0.52, 0.635, -2.35),
            -90.0,
            25.0,
            0.0
        ));
        scene.addObject(OBJLoader.loadModel(
            "Scene 2/91-the_crowned_ring_obj_format/wedding_ring.obj",
            goldMaterial,
            0.125,
            new Vector3D(0.12, 0.040, -0.92),
            84.0,
            -18.0,
            0.0
        ));
        scene.addObject(OBJLoader.loadModel(
            "Scene 2/diamonds1/diamonds.obj",
            diamondMaterial,
            0.045,
            new Vector3D(0.78, 0.626, -2.83),
            0.0,
            18.0,
            0.0
        ));
        scene.addObject(OBJLoader.loadModel(
            "Scene 2/Lightsaber/lightsaber.obj",
            saberMaterial,
            0.085,
            new Vector3D(2.46, 0.008, -4.18),
            0.0,
            -58.0,
            0.0
        ));
        scene.addObject(OBJLoader.loadModel(
            "Scene 2/foco/hanging_light.obj",
            focoMaterial,
            0.0013,
            new Vector3D(0.12, 2.45, -2.70),
            0.0,
            0.0,
            0.0
        ));

        scene.addLight(new Light(new Vector3D(-0.25, 0.45, 0.25), new Color(52, 65, 105), 0.12));
        scene.addLight(new PointLight(
            new Vector3D(0.12, 2.18, -2.70),
            new Color(205, 178, 150),
            3.7,
            1.0,
            0.28,
            0.14
        ));
        scene.addLight(new PointLight(
            new Vector3D(0.0, 1.35, -5.78),
            new Color(52, 78, 150),
            1.85,
            1.0,
            0.22,
            0.11
        ));
        scene.addLight(new PointLight(
            new Vector3D(0.34, 0.48, -1.18),
            new Color(190, 182, 215),
            2.15,
            1.0,
            0.18,
            0.16
        ));

        return scene;
    }
    // Scene 1: Batman origin scene with rain, reflections, batarangs and the fallen pearls.
    private static Scene buildScene1(int width, int height) throws Exception {
        Scene scene = new Scene();
        scene.setCamera(new Camera(
            new Vector3D(0.28, 0.30, 1.55),
            new Vector3D(-0.16, 0.48, -3.20),
            width,
            height,
            56
        ));

        Texture floorTexture = new Texture("Scene 1/Piso 01.png");
        Texture leftWallTexture = new Texture("Scene 1/Pared izq 01.png");
        Texture rightWallTexture = new Texture("Scene 1/Pared der 01.png");
        Texture backWallTexture = new Texture("Scene 1/Pared fondo 01.png");
        Texture batmanTexture = new Texture("Scene 1/the-batman/textures/body_d.tga.png");
        Texture gunTexture = new Texture("Scene 1/Gun _obj/Gun.png");

        Material floorMaterial = new Material(new Color(135, 135, 135), floorTexture)
            .setAmbient(0.22)
            .setDiffuse(0.58)
            .setSpecular(0.68)
            .setShininess(95.0)
            .setReflectivity(0.28);
        Material leftWallMaterial = new Material(new Color(105, 110, 125), leftWallTexture)
            .setAmbient(0.28)
            .setDiffuse(0.72)
            .setSpecular(0.12)
            .setReflectivity(0.03);
        Material rightWallMaterial = new Material(new Color(105, 110, 125), rightWallTexture)
            .setAmbient(0.26)
            .setDiffuse(0.74)
            .setSpecular(0.11)
            .setReflectivity(0.03);
        Material backWallMaterial = new Material(new Color(92, 96, 110), backWallTexture)
            .setAmbient(0.28)
            .setDiffuse(0.70)
            .setSpecular(0.10)
            .setReflectivity(0.02);
        Material puddleMaterial = new Material(new Color(65, 88, 105))
            .setAmbient(0.025)
            .setDiffuse(0.06)
            .setSpecular(1.0)
            .setShininess(220.0)
            .setReflectivity(0.50)
            .setTransparency(0.42)
            .setRefractiveIndex(1.333);
        Material batmanMaterial = new Material(Color.WHITE, batmanTexture)
            .setAmbient(0.06)
            .setDiffuse(0.62)
            .setSpecular(0.42)
            .setShininess(72.0)
            .setReflectivity(0.08);
        Material batarangMaterial = new Material(new Color(10, 11, 13))
            .setAmbient(0.05)
            .setDiffuse(0.38)
            .setSpecular(0.72)
            .setShininess(125.0)
            .setReflectivity(0.20);

        Material pearlMaterial = new Material(new Color(248, 246, 238))
            .setAmbient(0.10)
            .setDiffuse(0.58)
            .setSpecular(1.0)
            .setShininess(170.0)
            .setReflectivity(0.52)
            .setTransparency(0.03)
            .setRefractiveIndex(1.45);
        Material gunMaterial = new Material(new Color(190, 190, 190), gunTexture)
            .setAmbient(0.07)
            .setDiffuse(0.52)
            .setSpecular(0.74)
            .setShininess(105.0)
            .setReflectivity(0.22);

        scene.addObject(new RectPlane(
            new Vector3D(0.0, 0.0, -2.65),
            new Vector3D(1, 0, 0),
            new Vector3D(0, 0, -1),
            7.2,
            9.8,
            floorMaterial,
            3.0,
            4.2
        ));
        scene.addObject(new RectPlane(
            new Vector3D(-3.05, 1.325, -3.35),
            new Vector3D(0, 0, -1),
            new Vector3D(0, 1, 0),
            7.0,
            2.65,
            leftWallMaterial,
            2.4,
            1.2
        ));
        scene.addObject(new RectPlane(
            new Vector3D(3.05, 1.325, -3.35),
            new Vector3D(0, 0, 1),
            new Vector3D(0, 1, 0),
            7.0,
            2.65,
            rightWallMaterial,
            2.4,
            1.2
        ));
        scene.addObject(new RectPlane(
            new Vector3D(0.0, 1.325, -6.65),
            new Vector3D(1, 0, 0),
            new Vector3D(0, 1, 0),
            6.3,
            2.65,
            backWallMaterial,
            2.2,
            1.15
        ));
        scene.addObject(new IrregularPuddle(
            new Vector3D(0.18, 0.012, -2.60),
            new Vector3D(1, 0, 0),
            new Vector3D(0, 0, -1),
            1.08,
            0.48,
            0.95,
            1.2,
            puddleMaterial
        ));
        scene.addObject(new IrregularPuddle(
            new Vector3D(-1.34, 0.011, -1.42),
            new Vector3D(1, 0, 0),
            new Vector3D(0, 0, -1),
            0.36,
            0.18,
            1.05,
            2.3,
            puddleMaterial
        ));
        scene.addObject(new IrregularPuddle(
            new Vector3D(1.32, 0.011, -1.78),
            new Vector3D(1, 0, 0),
            new Vector3D(0, 0, -1),
            0.42,
            0.21,
            0.96,
            3.7,
            puddleMaterial
        ));
        scene.addObject(new IrregularPuddle(
            new Vector3D(-0.42, 0.011, -3.55),
            new Vector3D(1, 0, 0),
            new Vector3D(0, 0, -1),
            0.58,
            0.24,
            0.92,
            4.4,
            puddleMaterial
        ));
        scene.addObject(new IrregularPuddle(
            new Vector3D(0.98, 0.011, -3.12),
            new Vector3D(1, 0, 0),
            new Vector3D(0, 0, -1),
            0.28,
            0.12,
            1.08,
            5.5,
            puddleMaterial
        ));
        scene.addObject(new IrregularPuddle(
            new Vector3D(-1.85, 0.011, -2.55),
            new Vector3D(1, 0, 0),
            new Vector3D(0, 0, -1),
            0.22,
            0.10,
            1.02,
            6.8,
            puddleMaterial
        ));

        scene.addObject(OBJLoader.loadModel(
            "Scene 1/the-batman/source/0/batman.obj",
            batmanMaterial,
            1.18,
            new Vector3D(0.08, 0.0, -4.70),
            0.0,
            42.0,
            0.0
        ));
        addBatarang(scene, batarangMaterial, 0.42, new Vector3D(-0.35, 0.75, -1.24), 12.0, 58.0, -18.0);
        addBatarang(scene, batarangMaterial, 0.64, new Vector3D(1.35, 0.78, -0.86), 16.0, 50.0, 24.0);
        addBatarang(scene, batarangMaterial, 0.52, new Vector3D(-1.55, 0.92, -0.92), 8.0, 62.0, -56.0);
        addBatarang(scene, batarangMaterial, 0.30, new Vector3D(0.62, 0.90, -2.18), 10.0, 66.0, 16.0);
        scene.addObject(OBJLoader.loadModel(
            "Scene 1/pearl_necklace.obj",
            pearlMaterial,
            0.34,
            new Vector3D(-1.40, 0.070, -2.20),
            90.0,
            -28.0,
            0.0
        ));
        scene.addObject(OBJLoader.loadModel(
            "Scene 1/Gun _obj/gun.obj",
            gunMaterial,
            1.15,
            new Vector3D(1.25, 0.084, -1.55),
            88.0,
            -55.0,
            8.0
        ));

        scene.addLight(new Light(new Vector3D(-0.25, 0.7, 0.25), new Color(130, 160, 220), 0.22));
        scene.addLight(new PointLight(
            new Vector3D(-1.65, 2.30, -0.95),
            new Color(255, 205, 145),
            5.7,
            1.0,
            0.18,
            0.09
        ));
        scene.addLight(new PointLight(
            new Vector3D(1.35, 1.60, -5.65),
            new Color(80, 130, 255),
            2.4,
            1.0,
            0.16,
            0.08
        ));

        return scene;
    }

    private static void addBatarang(
        Scene scene,
        Material material,
        double scale,
        Vector3D position,
        double rotationX,
        double rotationY,
        double rotationZ
    ) throws Exception {
        scene.addObject(OBJLoader.loadModel(
            "Scene 1/batarang.obj",
            material,
            scale,
            position,
            rotationX,
            rotationY,
            rotationZ
        ));
    }
}
