package net.akat.mosaic.strategy;

import java.awt.image.BufferedImage;

public class PreserveAspectRatioStrategy implements ImageProcessingStrategy {
    @Override
    public BufferedImage process(BufferedImage sourceImage, int targetWidth, int targetHeight) {
        double ratio = Math.min(
                (double) targetWidth / sourceImage.getWidth(),
                (double) targetHeight / sourceImage.getHeight()
        );

        int newWidth = (int) (sourceImage.getWidth() * ratio);
        int newHeight = (int) (sourceImage.getHeight() * ratio);

        BufferedImage scaledImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = scaledImage.createGraphics();

        int x = (targetWidth - newWidth) / 2;
        int y = (targetHeight - newHeight) / 2;

        g.drawImage(sourceImage, x, y, newWidth, newHeight, null);
        g.dispose();

        return scaledImage;
    }
}
