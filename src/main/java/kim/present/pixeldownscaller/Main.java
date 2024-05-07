package kim.present.pixeldownscaller;

import com.sksamuel.scrimage.ImmutableImage;
import com.sksamuel.scrimage.color.RGBColor;
import com.sksamuel.scrimage.nio.PngWriter;
import com.sksamuel.scrimage.pixels.Pixel;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        String targetDir;
        if (args == null || args.length < 1) {
            targetDir = System.getProperty("user.dir");
        } else {
            targetDir = args[0];
        }

        File dir = new File(targetDir);
        if (!dir.exists()) {
            throw new IOException("Directory not found: " + targetDir);
        }

        if (dir.isFile()) {
            System.out.println("Processing: " + dir);
            processFile(dir);
            return;
        }

        File[] files = dir.listFiles();
        if (files == null) {
            throw new IOException("Failed to list files in directory: " + targetDir);
        }

        for (File file : files) {
            processFile(file);
        }
    }

    private static void processFile(File file) {
        if (!file.isFile() || !file.getName().endsWith(".png")) {
            return;
        }
        System.out.println("Processing: " + file);
        try {
            ImmutableImage png = ImmutableImage.loader().fromFile(file);
            png = scaleDown(png);
            png = removeBackground(png);
            png.output(PngWriter.NoCompression, file);
        } catch (Exception e) {
            System.err.println("Failed to process: " + file + " - " + e.getMessage());
        }
    }

    private static ImmutableImage scaleDown(ImmutableImage png) {
        if (png.width <= 2 || png.height <= 2) {
            return png;
        }

        ImmutableImage scaled = ImmutableImage.create(png.width / 2, png.height / 2);
        for (int x = 0; x < png.width; x += 2) {
            for (int y = 0; y < png.height; y += 2) {
                Pixel p = png.pixel(x, y);
                scaled.setColor(x / 2, y / 2, p.toColor());
                for (int i = 0; i < 2; i++) {
                    for (int j = 0; j < 2; j++) {
                        if (!pixelCompare(p, png.pixel(x + i, y + j), 30)) {
                            return png;
                        }
                    }
                }
            }
        }
        return scaleDown(scaled);
    }

    private static ImmutableImage removeBackground(ImmutableImage png) {
        Pixel[] backgrounds = new Pixel[]{
                new Pixel(0, 0, 245, 245, 245, 0),
                new Pixel(0, 0, 10, 10, 10, 0),
                new Pixel(0, 0, 10, 245, 10, 0),
                new Pixel(0, 0, 10, 225, 10, 0),
        };

        Pixel b = null;
        Pixel firstPixel = png.pixel(0, 0);
        Pixel lastPixel = png.pixel(png.width - 1, png.height - 1);
        for (Pixel background : backgrounds) {
            if (pixelCompare(firstPixel, background, 20) || pixelCompare(lastPixel, background, 20)) {
                b = background;
                break;
            }
        }

        if (b == null) {
            return png;
        }

        ImmutableImage result = ImmutableImage.create(png.width, png.height, BufferedImage.TYPE_INT_ARGB);
        for (int x = 0; x < png.width; x++) {
            for (int y = 0; y < png.height; y++) {
                Pixel p = png.pixel(x, y);
                if (pixelCompare(p, b, 20)) {
                    result.setColor(x, y, new RGBColor(0, 0, 0, 0));
                } else {
                    result.setColor(x, y, p.toColor());
                }
            }
        }
        return result;
    }

    private static boolean pixelCompare(Pixel p1, Pixel p2, int tolerance) {
        int r = Math.abs(p1.red() - p2.red());
        int g = Math.abs(p1.green() - p2.green());
        int b = Math.abs(p1.blue() - p2.blue());
        return r <= tolerance && g <= tolerance && b <= tolerance;
    }
}