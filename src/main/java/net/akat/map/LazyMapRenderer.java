package net.akat.map;

import net.akat.util.MapColorUtil;
import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

import java.awt.*;
import java.awt.image.BufferedImage;

public class LazyMapRenderer extends MapRenderer {
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
        this.scaledImageCache = null; // Сбрасываем кэш при новом изображении
        this.mapPixelsCache = null;
    }

    @Override
    public void render(MapView mapView, MapCanvas mapCanvas, Player player) {
        if (rendered || image == null) return;

        byte[] mapPixels = getMapPixels();

        for (int x = 0; x < 128; x++) {
            for (int y = 0; y < 128; y++) {
                mapCanvas.setPixel(x, y, mapPixels[y * 128 + x]);
            }
        }

        rendered = true;
    }

    private BufferedImage getScaledImage() {
        if (scaledImageCache == null) {
            scaledImageCache = new BufferedImage(128, 128, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = scaledImageCache.createGraphics();

            // Улучшенное масштабирование для лучшего качества
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY);

            g.drawImage(image, 0, 0, 128, 128, null);
            g.dispose();
        }
        return scaledImageCache;
    }

    private byte[] getMapPixels() {
        if (mapPixelsCache == null) {
            BufferedImage scaledImage = getScaledImage();
            mapPixelsCache = new byte[128 * 128];

            for (int x = 0; x < 128; x++) {
                for (int y = 0; y < 128; y++) {
                    mapPixelsCache[y * 128 + x] = MapColorUtil.getClosestColor(scaledImage.getRGB(x, y));
                }
            }
        }
        return mapPixelsCache;
    }
}
