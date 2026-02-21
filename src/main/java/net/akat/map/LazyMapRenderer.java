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
    }

    @Override
    public void render(MapView mapView, MapCanvas mapCanvas, Player player) {
        if (rendered || image == null) return;

        BufferedImage scaledImage = getScaledImage();

        for (int x = 0; x < 128; x++) {
            for (int y = 0; y < 128; y++) {
                // ИСПОЛЬЗУЕМ MapColorUtil ВМЕСТО MapPalette!
                byte mapColor = MapColorUtil.getClosestColor(scaledImage.getRGB(x, y));
                mapCanvas.setPixel(x, y, mapColor);
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
}
