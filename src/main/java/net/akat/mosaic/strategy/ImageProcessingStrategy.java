package net.akat.mosaic.strategy;

import java.awt.image.BufferedImage;

public interface ImageProcessingStrategy {
    BufferedImage process(BufferedImage sourceImage, int targetWidth, int targetHeight);
}
