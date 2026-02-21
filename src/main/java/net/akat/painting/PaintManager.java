package net.akat.painting;

import net.akat.ImagePlugin;
import net.akat.map.LazyMapRenderer;
import net.akat.mosaic.strategy.CropImageStrategy;
import net.akat.mosaic.strategy.ImageProcessingStrategy;
import net.akat.mosaic.strategy.PreserveAspectRatioStrategy;
import net.akat.mosaic.strategy.ScaleImageStrategy;
import net.akat.util.FrameUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapView;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class PaintManager {
    private final ImagePlugin plugin;
    private final File paintFile;
    private YamlConfiguration paintConfig;
    private final Map<String, PaintData> paints = new HashMap<>();
    private final Map<String, BufferedImage> imageCache = new WeakHashMap<>();

    public PaintManager(ImagePlugin plugin) {
        this.plugin = plugin;
        this.paintFile = new File(plugin.getDataFolder(), "paint.yml");
        loadPaints();

        plugin.getServer().getScheduler().runTaskLater(plugin, this::restoreAllPaints, 100L);
    }

    public void loadPaints() {
        paints.clear();
        imageCache.clear();

        if (!paintFile.exists()) {
            return;
        }

        try {
            paintConfig = YamlConfiguration.loadConfiguration(paintFile);

            if (paintConfig.contains("paints")) {
                List<Map<?, ?>> paintsList = paintConfig.getMapList("paints");

                for (Map<?, ?> map : paintsList) {
                    PaintData paint = PaintData.deserialize((Map<String, Object>) map);
                    paints.put(paint.getId(), paint);
                }
            }

            plugin.getLogger().info("§aЗагружено " + paints.size() + " картин");

        } catch (Exception e) {
            plugin.getLogger().severe("§cОшибка загрузки paint.yml: " + e.getMessage());
        }
    }

    public void savePaint(PaintData paint) {
        paints.put(paint.getId(), paint);
        saveAllPaints();
    }

    public void saveAllPaints() {
        if (paintConfig == null) {
            paintConfig = new YamlConfiguration();
        }

        paintConfig.options().copyDefaults(true);
        paintConfig.options().width(120);
        paintConfig.options().parseComments(false);
        paintConfig.options().copyHeader(false);

        List<Map<String, Object>> paintsList = new ArrayList<>();
        for (PaintData paint : paints.values()) {
            paintsList.add(paint.serialize());
        }

        paintConfig.set("paints", paintsList);

        try {
            paintConfig.save(paintFile);
            plugin.getLogger().info("§aСохранено " + paints.size() + " картин");
        } catch (IOException e) {
            plugin.getLogger().severe("§cОшибка сохранения paint.yml: " + e.getMessage());
        }
    }

    public String generatePaintId() {
        return "p_" + UUID.randomUUID().toString().substring(0, 6);
    }

    public String saveImageLocally(BufferedImage image, String source) {
        try {
            File paintsFolder = new File(plugin.getDataFolder(), "p");
            if (!paintsFolder.exists()) {
                paintsFolder.mkdirs();
            }

            String fileName = "p_" + UUID.randomUUID().toString().substring(0, 6) + ".png";
            File imageFile = new File(paintsFolder, fileName);

            ImageIO.write(image, "png", imageFile);
            imageCache.put(fileName, image);

            return "p/" + fileName;

        } catch (IOException e) {
            plugin.getLogger().severe("§cОшибка сохранения: " + e.getMessage());
            return null;
        }
    }

    public void restoreAllPaints() {
        int success = 0;
        int failed = 0;

        for (PaintData paint : paints.values()) {
            if (restorePaint(paint)) {
                success++;
            } else {
                failed++;
            }
        }

        plugin.getLogger().info("§aВосстановлено картин: " + success + ", ошибок: " + failed);
    }

    public boolean restorePaint(PaintData paint) {
        World world = Bukkit.getWorld(paint.getWorld());
        if (world == null) {
            plugin.getLogger().warning("§cМир " + paint.getWorld() + " не загружен для картины " + paint.getId());
            return false;
        }

        Location testLoc = calculateFrameLocation(paint, 0, 0);
        if (testLoc != null) {
            int chunkX = testLoc.getBlockX() >> 4;
            int chunkZ = testLoc.getBlockZ() >> 4;
            if (!world.isChunkLoaded(chunkX, chunkZ)) {
                world.loadChunk(chunkX, chunkZ);
            }
        }

        BufferedImage originalImage = getCachedImage(paint.getLocalPath());
        if (originalImage == null) {
            plugin.getLogger().warning("§cИзображение не найдено для картины " + paint.getId());
            return false;
        }

        // ✅ ПОЛУЧАЕМ И ПРИМЕНЯЕМ СТРАТЕГИЮ
        int mapSize = 128;
        int width = paint.getWidth();
        int height = paint.getHeight();

        // Получаем стратегию по имени из PaintData
        String strategyName = paint.getStrategy();
        ImageProcessingStrategy strategy = getStrategyByName(strategyName);

        // Применяем стратегию к оригинальному изображению
        BufferedImage processedImage = strategy.process(originalImage, width * mapSize, height * mapSize);

        // Кэшируем обработанное изображение для следующих восстановлений
        String processedKey = paint.getLocalPath() + "_" + strategyName + "_" + width + "x" + height;
        imageCache.put(processedKey, processedImage);

        int restored = 0;

        for (int[] frameData : paint.getFrames()) {
            int relX = frameData[0];
            int relY = frameData[1];
            int index = frameData[2];

            Location frameLoc = calculateFrameLocation(paint, relX, relY);
            if (frameLoc == null) continue;

            boolean frameFound = false;

            for (Entity entity : world.getNearbyEntities(frameLoc, 0.5, 0.5, 0.5)) {
                if (entity instanceof ItemFrame) {
                    ItemFrame frame = (ItemFrame) entity;

                    if (frame.getLocation().getBlockX() == frameLoc.getBlockX() &&
                            frame.getLocation().getBlockY() == frameLoc.getBlockY() &&
                            frame.getLocation().getBlockZ() == frameLoc.getBlockZ()) {

                        try {
                            // ✅ БЕРЕМ СЕГМЕНТ ИЗ ОБРАБОТАННОГО СТРАТЕГИЕЙ ИЗОБРАЖЕНИЯ
                            BufferedImage segment = processedImage.getSubimage(
                                    relX * mapSize, relY * mapSize, mapSize, mapSize
                            );

                            MapView mapView = Bukkit.createMap(frame.getWorld());
                            mapView.getRenderers().clear();
                            mapView.addRenderer(new LazyMapRenderer(segment));

                            ItemStack mapItem = new ItemStack(org.bukkit.Material.FILLED_MAP);
                            MapMeta meta = (MapMeta) mapItem.getItemMeta();
                            meta.setMapView(mapView);
                            meta.setDisplayName(null);
                            mapItem.setItemMeta(meta);

                            frame.setItem(mapItem);
                            restored++;
                            frameFound = true;
                        } catch (Exception e) {
                            plugin.getLogger().warning("§eОшибка рамки: " + e.getMessage());
                        }
                        break;
                    }
                }
            }

            if (!frameFound) {
                plugin.getLogger().warning("§eРамка не найдена: " + frameLoc.toString());
            }
        }

        if (restored == paint.getFrames().size()) {
            plugin.getLogger().info("§aВосстановлена картина: " + paint.getId() +
                    " (" + restored + " рамок, стратегия: " + strategyName + ")");
            return true;
        } else {
            plugin.getLogger().warning("§eЧастично восстановлена картина " + paint.getId() +
                    ": " + restored + "/" + paint.getFrames().size() + " рамок, стратегия: " + strategyName);
            return false;
        }
    }

    // ✅ МЕТОД ДЛЯ ПОЛУЧЕНИЯ СТРАТЕГИИ ПО ИМЕНИ
    private ImageProcessingStrategy getStrategyByName(String name) {
        if (name == null) return new PreserveAspectRatioStrategy();

        switch (name.toLowerCase()) {
            case "scale":
                return new ScaleImageStrategy();
            case "crop":
                return new CropImageStrategy();
            case "preserve":
            default:
                return new PreserveAspectRatioStrategy();
        }
    }

    private BufferedImage getCachedImage(String path) {
        String fileName = path.substring(path.lastIndexOf('/') + 1);

        if (imageCache.containsKey(fileName)) {
            return imageCache.get(fileName);
        }

        try {
            File imageFile = new File(plugin.getDataFolder(), path);
            if (!imageFile.exists()) {
                plugin.getLogger().warning("§cФайл не найден: " + path);
                return null;
            }

            BufferedImage image = ImageIO.read(imageFile);
            imageCache.put(fileName, image);
            return image;
        } catch (IOException e) {
            plugin.getLogger().warning("§cОшибка загрузки: " + path);
            return null;
        }
    }

    private Location calculateFrameLocation(PaintData paint, int relX, int relY) {
        World world = Bukkit.getWorld(paint.getWorld());
        if (world == null) return null;

        return FrameUtil.calculateFrameLocation(
                world,
                paint.getPos1Array(),
                paint.getPos2Array(),
                paint.getFace(),
                relX,
                relY
        );
    }

    public void deletePaint(String id) {
        paints.remove(id);
        saveAllPaints();
    }

    public Collection<PaintData> getAllPaints() {
        return paints.values();
    }
}
