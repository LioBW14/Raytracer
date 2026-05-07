import java.awt.Color;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class OBJLoader {
    // Loads an OBJ file and converts every face into one or more triangles.
    public static List<Triangle> load(String fileName, Color color) throws IOException {
        return load(fileName, color, 1.0, new Vector3D(0, 0, 0));
    }

    // Loads an OBJ file and applies a simple scale and translation to each vertex.
    public static List<Triangle> load(
        String fileName,
        Color color,
        double scale,
        Vector3D translation
    ) throws IOException {
        List<Vector3D> vertices = new ArrayList<>();
        List<Vector3D> normals = new ArrayList<>();
        List<Triangle> triangles = new ArrayList<>();

        // Opens the OBJ file and closes it automatically when loading finishes.
        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            String line;
            int lineNumber = 0;

            // Reads the OBJ file one line at a time.
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                line = line.trim();

                // Empty lines and comments do not affect geometry.
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                String[] parts = line.split("\\s+");

                // Vertex lines define positions. Normal lines define smooth surface directions.
                if (parts[0].equals("v")) {
                    vertices.add(parseVertex(parts, lineNumber, scale, translation));
                } else if (parts[0].equals("vn")) {
                    normals.add(parseNormal(parts, lineNumber));
                } else if (parts[0].equals("f")) {
                    addFaceTriangles(parts, vertices, normals, triangles, color, lineNumber);
                }
            }
        }

        return triangles;
    }

    // Parses a vertex line with the form: v x y z.
    private static Vector3D parseVertex(
        String[] parts,
        int lineNumber,
        double scale,
        Vector3D translation
    ) throws IOException {
        if (parts.length < 4) {
            throw new IOException("Invalid vertex at line " + lineNumber);
        }

        try {
            double x = Double.parseDouble(parts[1]);
            double y = Double.parseDouble(parts[2]);
            double z = Double.parseDouble(parts[3]);

            // Scale first, then translate so large OBJ files fit the camera view.
            return new Vector3D(x, y, z).multiply(scale).add(translation);
        } catch (NumberFormatException e) {
            throw new IOException("Invalid vertex number at line " + lineNumber, e);
        }
    }

    // Parses a vertex normal line with the form: vn x y z.
    private static Vector3D parseNormal(String[] parts, int lineNumber) throws IOException {
        if (parts.length < 4) {
            throw new IOException("Invalid normal at line " + lineNumber);
        }

        try {
            double x = Double.parseDouble(parts[1]);
            double y = Double.parseDouble(parts[2]);
            double z = Double.parseDouble(parts[3]);

            return new Vector3D(x, y, z).normalize();
        } catch (NumberFormatException e) {
            throw new IOException("Invalid normal number at line " + lineNumber, e);
        }
    }

    // Converts one OBJ face into triangles using fan triangulation.
    private static void addFaceTriangles(
        String[] parts,
        List<Vector3D> vertices,
        List<Vector3D> normals,
        List<Triangle> triangles,
        Color color,
        int lineNumber
    ) throws IOException {
        if (parts.length < 4) {
            throw new IOException("Invalid face at line " + lineNumber);
        }

        List<Vector3D> faceVertices = new ArrayList<>();
        List<Vector3D> faceNormals = new ArrayList<>();

        // Resolves every OBJ face index into a vertex position and optional normal.
        for (int i = 1; i < parts.length; i++) {
            int vertexIndex = parseVertexIndex(parts[i], vertices.size(), lineNumber);
            faceVertices.add(vertices.get(vertexIndex));

            int normalIndex = parseNormalIndex(parts[i], normals.size(), lineNumber);
            faceNormals.add(normalIndex >= 0 ? normals.get(normalIndex) : null);
        }

        // A face with N vertices becomes N - 2 triangles.
        for (int i = 1; i < faceVertices.size() - 1; i++) {
            triangles.add(new Triangle(
                faceVertices.get(0),
                faceVertices.get(i),
                faceVertices.get(i + 1),
                color,
                faceNormals.get(0),
                faceNormals.get(i),
                faceNormals.get(i + 1)
            ));
        }
    }

    // Parses the vertex index from OBJ face tokens like v, v/vt or v/vt/vn.
    private static int parseVertexIndex(String token, int vertexCount, int lineNumber) throws IOException {
        // Only the vertex position index is needed for this raytracer.
        String[] tokenParts = token.split("/", -1);

        if (tokenParts.length == 0 || tokenParts[0].isEmpty()) {
            throw new IOException("Invalid face index at line " + lineNumber);
        }

        try {
            int objIndex = Integer.parseInt(tokenParts[0]);
            int zeroBasedIndex;

            // Positive OBJ indices are 1-based. Negative indices are relative to the current vertex list.
            if (objIndex > 0) {
                zeroBasedIndex = objIndex - 1;
            } else if (objIndex < 0) {
                zeroBasedIndex = vertexCount + objIndex;
            } else {
                throw new IOException("OBJ indices start at 1 at line " + lineNumber);
            }

            if (zeroBasedIndex < 0 || zeroBasedIndex >= vertexCount) {
                throw new IOException("Face index out of range at line " + lineNumber);
            }

            return zeroBasedIndex;
        } catch (NumberFormatException e) {
            throw new IOException("Invalid face index number at line " + lineNumber, e);
        }
    }

    // Parses the normal index from OBJ face tokens like v//vn or v/vt/vn.
    private static int parseNormalIndex(String token, int normalCount, int lineNumber) throws IOException {
        String[] tokenParts = token.split("/", -1);

        if (tokenParts.length < 3 || tokenParts[2].isEmpty()) {
            return -1;
        }

        try {
            int objIndex = Integer.parseInt(tokenParts[2]);
            int zeroBasedIndex;

            // Positive OBJ indices are 1-based. Negative indices are relative to the current normal list.
            if (objIndex > 0) {
                zeroBasedIndex = objIndex - 1;
            } else if (objIndex < 0) {
                zeroBasedIndex = normalCount + objIndex;
            } else {
                throw new IOException("OBJ normal indices start at 1 at line " + lineNumber);
            }

            if (zeroBasedIndex < 0 || zeroBasedIndex >= normalCount) {
                throw new IOException("Normal index out of range at line " + lineNumber);
            }

            return zeroBasedIndex;
        } catch (NumberFormatException e) {
            throw new IOException("Invalid normal index number at line " + lineNumber, e);
        }
    }
}
