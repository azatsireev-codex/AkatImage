package net.akat.observer;

import java.awt.image.BufferedImage;

public interface ImageLoadObserver {
    void onImageLoaded(String name, BufferedImage image);
    void onImageDeleted(String name);
    void onAllImagesLoaded(int count);
}
