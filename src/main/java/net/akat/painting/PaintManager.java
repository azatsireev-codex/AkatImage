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
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.YamlConfiguration;
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
    private final File paintsDirectory;
    private final File legacyPaintFile;
    private final Map<String, PaintData> paints = new HashMap<>();
    private final Map<String, BufferedImage> imageCache = new WeakHashMap<>();

    public PaintManager(ImagePlugin plugin) {
        this.plugin = plugin;
        this.paintsDirectory = new File(plugin.getDataFolder(), "paints");
        this.legacyPaintFile = new File(plugin.getDataFolder(), "paint.yml");
        loadPaints();

        plugin.getServer().getScheduler().runTaskLater(plugin, this::restoreAllPaints, 100L);
    }

    public void loadPaints() {
        paints.clear();
        imageCache.clear();

        if (!paintsDirectory.exists()) {
            paintsDirectory.mkdirs();
        }

        int loaded = loadPaintsFromDirectory();

        if (loaded == 0 && legacyPaintFile.exists()) {
            loaded = loadLegacyPaints();
            if (loaded > 0) {
                saveAllPaints();
                plugin.getLogger().info("§aМиграция картин из paint.yml завершена: " + loaded);
            }
        }

        plugin.getLogger().info("§aЗагружено " + paints.size() + " картин");
    }

    private int loadPaintsFromDirectory() {
        int loaded = 0;

        File[] files = paintsDirectory.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) {
            return 0;
        }

        for (File file : files) {
            try {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                Map<String, Object> map = config.getConfigurationSection("paint") != null
                        ? config.getConfigurationSection("paint").getValues(true)
                        : config.getValues(true);

                PaintData paint = PaintData.deserialize(map);
                paints.put(paint.getId(), paint);
                loaded++;
            } catch (Exception e) {
                plugin.getLogger().warning("§eОшибка чтения " + file.getName() + ": " + e.getMessage());
            }
        }

        return loaded;
    }

    @SuppressWarnings("unchecked")
    private int loadLegacyPaints() {
        int loaded = 0;

        try {
            YamlConfiguration paintConfig = YamlConfiguration.loadConfiguration(legacyPaintFile);
            if (!paintConfig.contains("paints")) {
                return 0;
            }

            List<Map<?, ?>> paintsList = paintConfig.getMapList("paints");
            for (Map<?, ?> map : paintsList) {
                PaintData paint = PaintData.deserialize((Map<String, Object>) map);
                paints.put(paint.getId(), paint);
                loaded++;
            }
        } catch (Exception e) {
            plugin.getLogger().severe("§cОшибка загрузки legacy paint.yml: " + e.getMessage());
        }

        return loaded;
    }

    public void savePaint(PaintData paint) {
        paints.put(paint.getId(), paint);
        savePaintToFile(paint);
    }

    public void saveAllPaints() {
        if (!paintsDirectory.exists()) {
            paintsDirectory.mkdirs();
        }

        for (PaintData paint : paints.values()) {
            savePaintToFile(paint);
        }

        File[] existingFiles = paintsDirectory.listFiles((dir, name) -> name.endsWith(".yml"));
        if (existingFiles != null) {
            for (File file : existingFiles) {
                String id = file.getName().substring(0, file.getName().length() - 4);
                if (!paints.containsKey(id)) {
                    file.delete();
                }
            }
        }

        plugin.getLogger().info("§aСохранено " + paints.size() + " картин");
    }

    private void savePaintToFile(PaintData paint) {
        try {
            YamlConfiguration config = new YamlConfiguration();
            config.options().width(120);
            config.set("paint", paint.serialize());
            config.save(getPaintFile(paint.getId()));
        } catch (IOException e) {
            plugin.getLogger().severe("§cОшибка сохранения картины " + paint.getId() + ": " + e.getMessage());
        }
    }

    private File getPaintFile(String id) {
        return new File(paintsDirectory, id + ".yml");
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

        int mapSize = 128;
        int width = paint.getWidth();
        int height = paint.getHeight();

        String strategyName = paint.getStrategy();
        ImageProcessingStrategy strategy = getStrategyByName(strategyName);

        BufferedImage processedImage = strategy.process(originalImage, width * mapSize, height * mapSize);

        String processedKey = paint.getLocalPath() + "_" + strategyName + "_" + width + "x" + height;
        imageCache.put(processedKey, processedImage);

        BlockFace expectedFace = FrameUtil.getFaceFromChar(paint.getFace());
        boolean verticalWall = isVerticalWall(expectedFace);

        Location areaPos1 = new Location(world, paint.getPos1Array()[0], paint.getPos1Array()[1], paint.getPos1Array()[2]);
        Location areaPos2 = new Location(world, paint.getPos2Array()[0], paint.getPos2Array()[1], paint.getPos2Array()[2]);

        List<ItemFrame> areaFrames = FrameUtil.getItemFramesInArea(areaPos1, areaPos2);
        areaFrames.removeIf(frame -> frame.getAttachedFace() != expectedFace);

        Map<String, ItemFrame> frameByRelativeCoords = new HashMap<>();
        if (!areaFrames.isEmpty()) {
            FrameUtil.FrameGrid grid = FrameUtil.calculateGrid(areaFrames);
            for (ItemFrame frame : areaFrames) {
                FrameUtil.RelativeCoords coords = FrameUtil.calculateRelativeCoords(frame, grid, verticalWall);
                if (coords.relX < 0 || coords.relY < 0 || coords.relX >= width || coords.relY >= height) {
                    continue;
                }
                frameByRelativeCoords.put(coords.relX + ":" + coords.relY, frame);
            }
        }

        int restored = 0;

        for (int[] frameData : paint.getFrames()) {
            int relX = frameData[0];
            int relY = frameData[1];

            ItemFrame frame = frameByRelativeCoords.get(relX + ":" + relY);
            if (frame != null) {
                try {
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
                    continue;
                } catch (Exception e) {
                    plugin.getLogger().warning("§eОшибка рамки: " + e.getMessage());
                }
            }

            Location frameLoc = calculateFrameLocation(paint, relX, relY);
            if (frameLoc != null) {
                plugin.getLogger().warning("§eРамка не найдена: " + frameLoc);
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

    private boolean isVerticalWall(BlockFace face) {
        return face == BlockFace.NORTH || face == BlockFace.SOUTH
                || face == BlockFace.EAST || face == BlockFace.WEST;
    }

    public void deletePaint(String id) {
        paints.remove(id);
        File file = getPaintFile(id);
        if (file.exists()) {
            file.delete();
        }
    }

    public Collection<PaintData> getAllPaints() {
        return paints.values();
    }
}
