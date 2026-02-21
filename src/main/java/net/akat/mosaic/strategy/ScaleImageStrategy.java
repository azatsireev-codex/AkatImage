package net.akat.mosaic.strategy;

import java.awt.image.BufferedImage;

public class ScaleImageStrategy implements ImageProcessingStrategy {
    @Override
    public BufferedImage process(BufferedImage sourceImage, int targetWidth, int targetHeight) {
        BufferedImage scaledImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = scaledImage.createGraphics();
        g.drawImage(sourceImage, 0, 0, targetWidth, targetHeight, null);
        g.dispose();
        return scaledImage;
    }
}
