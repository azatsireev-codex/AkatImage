package net.akat.map;

import net.akat.util.MapColorUtil;
import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

import java.awt.*;
import java.awt.image.BufferedImage;

public class LazyMapRenderer extends MapRenderer {
    private static final int MAP_SIZE = 128;
    private static final int MAP_PIXELS = MAP_SIZE * MAP_SIZE;

    private BufferedImage image;
    private boolean rendered;
    private BufferedImage scaledImageCache;
    private byte[] mapPixelsCache;

    static {
        MapColorUtil.loadColors();
    }

    public LazyMapRenderer() {
        this.rendered = false;
    }

    public LazyMapRenderer(BufferedImage image) {
        this.image = image;
        this.rendered = false;
    }

    public void setImage(BufferedImage image) {
        this.image = image;
        this.rendered = false;
        this.scaledImageCache = null;
        this.mapPixelsCache = null;
    }

    @Override
    public void render(MapView mapView, MapCanvas mapCanvas, Player player) {
        if (rendered || image == null) return;

        byte[] mapPixels = getMapPixels();
        for (int x = 0; x < MAP_SIZE; x++) {
            for (int y = 0; y < MAP_SIZE; y++) {
                mapCanvas.setPixel(x, y, mapPixels[y * MAP_SIZE + x]);
            }
        }

        rendered = true;
    }

    private BufferedImage getScaledImage() {
        if (scaledImageCache == null) {
            scaledImageCache = new BufferedImage(MAP_SIZE, MAP_SIZE, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = scaledImageCache.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(image, 0, 0, MAP_SIZE, MAP_SIZE, null);
            g.dispose();
        }
        return scaledImageCache;
    }

    private byte[] getMapPixels() {
        if (mapPixelsCache == null) {
            BufferedImage scaledImage = getScaledImage();
            int[] rgbPixels = scaledImage.getRGB(0, 0, MAP_SIZE, MAP_SIZE, null, 0, MAP_SIZE);
            mapPixelsCache = new byte[MAP_PIXELS];

            for (int i = 0; i < rgbPixels.length; i++) {
                mapPixelsCache[i] = MapColorUtil.getClosestColor(rgbPixels[i]);
            }
        }
        return mapPixelsCache;
    }
}
