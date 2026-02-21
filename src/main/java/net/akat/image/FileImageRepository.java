package net.akat.image;

import net.akat.ImagePlugin;
import net.akat.service.LoggerService;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class FileImageRepository implements ImageRepository {
    private final ImagePlugin plugin;
    private final File imagesFolder;
    private final Map<String, BufferedImage> cache;
    private final LoggerService logger;

    public FileImageRepository(ImagePlugin plugin) {
        this.plugin = plugin;
        this.imagesFolder = new File(plugin.getDataFolder(), "images");
        this.cache = new HashMap<>();
        this.logger = plugin.getServiceLocator().getService(LoggerService.class);

        if (!imagesFolder.exists()) {
            imagesFolder.mkdirs();
        }

        loadAll();
    }

    @Override
    public void loadAll() {
        cache.clear();
        File[] files = imagesFolder.listFiles((dir, name) ->
                name.toLowerCase().endsWith(".png") ||
                        name.toLowerCase().endsWith(".jpg") ||
                        name.toLowerCase().endsWith(".jpeg")
        );

        if (files != null) {
            for (File file : files) {
                try {
                    BufferedImage image = ImageIO.read(file);
                    String name = file.getName().substring(0, file.getName().lastIndexOf('.'));
                    cache.put(name.toLowerCase(), image);
                    logger.info("Загружено изображение: " + name);
                } catch (IOException e) {
                    logger.warning("Не удалось загрузить изображение: " + file.getName());
                }
            }
        }

        logger.info("Загружено изображений: " + cache.size());
    }

    @Override
    public Optional<BufferedImage> findByName(String name) {
        return Optional.ofNullable(cache.get(name.toLowerCase()));
    }

    @Override
    public Set<String> getAllNames() {
        return cache.keySet();
    }

    @Override
    public void save(String name, BufferedImage image) {
        try {
            File outputFile = new File(imagesFolder, name + ".png");
            ImageIO.write(image, "png", outputFile);
            cache.put(name.toLowerCase(), image);
            logger.info("Сохранено изображение: " + name);
        } catch (IOException e) {
            logger.error("Не удалось сохранить изображение: " + name);
        }
    }

    @Override
    public boolean delete(String name) {
        File file = new File(imagesFolder, name + ".png");
        if (file.exists()) {
            boolean deleted = file.delete();
            if (deleted) {
                cache.remove(name.toLowerCase());
                logger.info("Удалено изображение: " + name);
            }
            return deleted;
        }
        return false;
    }
}
