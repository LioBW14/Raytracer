import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class Texture {
    private BufferedImage image;

    public Texture(String fileName) throws IOException {
        image = ImageIO.read(new File(fileName));

        if (image == null) {
            throw new IOException("Could not read texture " + fileName);
        }
    }

    public Color sample(double u, double v) {
        double wrappedU = u - Math.floor(u);
        double wrappedV = v - Math.floor(v);
        int x = Math.min(image.getWidth() - 1, Math.max(0, (int) Math.floor(wrappedU * image.getWidth())));
        int y = Math.min(image.getHeight() - 1, Math.max(0, (int) Math.floor((1.0 - wrappedV) * image.getHeight())));

        return new Color(image.getRGB(x, y), true);
    }
}
