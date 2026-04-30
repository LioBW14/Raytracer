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
        List<Triangle> triangles = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            String line;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                line = line.trim();

                // Empty lines and comments do not affect geometry.
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                String[] parts = line.split("\\s+");

                if (parts[0].equals("v")) {
                    vertices.add(parseVertex(parts, lineNumber, scale, translation));
                } else if (parts[0].equals("f")) {
                    addFaceTriangles(parts, vertices, triangles, color, lineNumber);
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

    // Converts one OBJ face into triangles using fan triangulation.
    private static void addFaceTriangles(
        String[] parts,
        List<Vector3D> vertices,
        List<Triangle> triangles,
        Color color,
        int lineNumber
    ) throws IOException {
        if (parts.length < 4) {
            throw new IOException("Invalid face at line " + lineNumber);
        }

        List<Vector3D> faceVertices = new ArrayList<>();

        for (int i = 1; i < parts.length; i++) {
            int vertexIndex = parseVertexIndex(parts[i], vertices.size(), lineNumber);
            faceVertices.add(vertices.get(vertexIndex));
        }

        // A face with N vertices becomes N - 2 triangles.
        for (int i = 1; i < faceVertices.size() - 1; i++) {
            triangles.add(new Triangle(
                faceVertices.get(0),
                faceVertices.get(i),
                faceVertices.get(i + 1),
                color
            ));
        }
    }

    // Parses the vertex index from OBJ face tokens like v, v/vt or v/vt/vn.
    private static int parseVertexIndex(String token, int vertexCount, int lineNumber) throws IOException {
        String[] tokenParts = token.split("/");

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
}
