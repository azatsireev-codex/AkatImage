package net.akat.observer;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class ImageLoadNotifier {
    private final List<ImageLoadObserver> observers = new ArrayList<>();

    public void addObserver(ImageLoadObserver observer) {
        observers.add(observer);
    }

    public void removeObserver(ImageLoadObserver observer) {
        observers.remove(observer);
    }

    public void notifyImageLoaded(String name, BufferedImage image) {
        observers.forEach(o -> o.onImageLoaded(name, image));
    }

    public void notifyImageDeleted(String name) {
        observers.forEach(o -> o.onImageDeleted(name));
    }

    public void notifyAllImagesLoaded(int count) {
        observers.forEach(o -> o.onAllImagesLoaded(count));
    }
}
