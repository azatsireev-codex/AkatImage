package net.akat.image;

import java.awt.image.BufferedImage;
import java.util.Optional;
import java.util.Set;

public interface ImageRepository {
    void loadAll();
    Optional<BufferedImage> findByName(String name);
    Set<String> getAllNames();
    void save(String name, BufferedImage image);
    boolean delete(String name);
}
