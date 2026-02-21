package net.akat.mosaic.strategy;

import java.awt.image.BufferedImage;

public class CropImageStrategy implements ImageProcessingStrategy {
    @Override
    public BufferedImage process(BufferedImage sourceImage, int targetWidth, int targetHeight) {
        int x = (sourceImage.getWidth() - targetWidth) / 2;
        int y = (sourceImage.getHeight() - targetHeight) / 2;
        return sourceImage.getSubimage(
                Math.max(0, x),
                Math.max(0, y),
                Math.min(targetWidth, sourceImage.getWidth()),
                Math.min(targetHeight, sourceImage.getHeight())
        );
    }
}
