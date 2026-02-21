package net.akat.command;

import net.akat.ImagePlugin;
import net.akat.ServiceLocator;
import net.akat.image.ImageRepository;
import net.akat.map.LazyMapRenderer;
import net.akat.mosaic.strategy.CropImageStrategy;
import net.akat.mosaic.strategy.ImageProcessingStrategy;
import net.akat.mosaic.strategy.PreserveAspectRatioStrategy;
import net.akat.mosaic.strategy.ScaleImageStrategy;
import net.akat.painting.PaintData;
import net.akat.painting.PaintManager;
import net.akat.util.FrameUtil;
import net.akat.util.ImageDownloader;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapView;
import org.bukkit.scheduler.BukkitRunnable;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class CreatePaintingCommand implements Command {
    private final ServiceLocator services;

    public CreatePaintingCommand(ServiceLocator services) {
        this.services = services;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cТолько игроки могут использовать эту команду!");
            return;
        }

        Player player = (Player) sender;

        if (args.length < 8) {
            player.sendMessage("§cИспользование: " + getUsage());
            player.sendMessage("§7Пример: /image paint 100 64 100 107 67 100 https://i.imgur.com/abc123.png preserve");
            player.sendMessage("§7Пример: /image paint ~ ~ ~ ~5 ~3 ~ my_image scale");
            player.sendMessage("§7Пример: /image paint ~ ~ ~ ~5 ~3 ~ my_image crop");
            player.sendMessage("§7Доступные стратегии: preserve (по умолчанию), scale, crop");
            return;
        }

        try {
            // Парсим координаты
            Location pos1 = parseLocation(player, args[1], args[2], args[3]);
            Location pos2 = parseLocation(player, args[4], args[5], args[6]);
            String source = args[7];

            // Получаем стратегию (8-й аргумент, опционально)
            String strategyName = args.length > 8 ? args[8].toLowerCase() : "preserve";

            player.sendMessage("§e⏳ Создание картины от " + formatLocation(pos1) + " до " + formatLocation(pos2));
            player.sendMessage("§e⏳ Источник: " + source);
            player.sendMessage("§e⏳ Стратегия: " + strategyName);

            // Загружаем изображение асинхронно
            CompletableFuture<Optional<BufferedImage>> future;

            if (source.startsWith("http://") || source.startsWith("https://")) {
                future = ImageDownloader.downloadImage(source)
                        .thenApply(Optional::of)
                        .exceptionally(e -> {
                            player.sendMessage("§cОшибка загрузки URL: " + e.getCause().getMessage());
                            return Optional.empty();
                        });
            } else {
                try {
                    ImageRepository repository = services.getService(ImageRepository.class);
                    Optional<BufferedImage> imageOpt = repository.findByName(source);
                    future = CompletableFuture.completedFuture(imageOpt);
                } catch (Exception e) {
                    player.sendMessage("§cОшибка загрузки из репозитория: " + e.getMessage());
                    return;
                }
            }

            future.thenAcceptAsync(imageOpt -> {
                if (!imageOpt.isPresent()) {
                    player.sendMessage("§cНе удалось загрузить изображение: " + source);
                    return;
                }

                BufferedImage image = imageOpt.get();

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        createPainting(player, pos1, pos2, source, image, strategyName);
                    }
                }.runTask(services.getService(ImagePlugin.class));

            }).exceptionally(throwable -> {
                player.sendMessage("§cОшибка: " + throwable.getCause().getMessage());
                throwable.printStackTrace();
                return null;
            });

        } catch (Exception e) {
            player.sendMessage("§cОшибка: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void createPainting(Player player, Location pos1, Location pos2,
                                String source, BufferedImage image, String strategyName) {
        try {
            if (!isFlatArea(pos1, pos2)) {
                player.sendMessage("§cОшибка: Область должна быть плоской!");
                player.sendMessage("§cКартина может быть только на одной стене/полу/потолке.");
                player.sendMessage("§cУбедитесь, что одна из координат (X, Y или Z) одинакова для обеих точек.");
                return;
            }
            List<ItemFrame> frames = FrameUtil.getItemFramesInArea(pos1, pos2);

            if (frames.isEmpty()) {
                player.sendMessage("§cВ указанной области нет рамок!");
                return;
            }

            player.sendMessage("§a✓ Найдено рамок: " + frames.size());

            // Определяем ориентацию всех рамок
            boolean allSameOrientation = true;
            boolean isVerticalWall = false;
            boolean isHorizontal = false;

            for (ItemFrame frame : frames) {
                BlockFace face = frame.getAttachedFace();
                if (face == null) continue;

                boolean wall = (face == BlockFace.NORTH || face == BlockFace.SOUTH ||
                        face == BlockFace.EAST || face == BlockFace.WEST);
                boolean floor = (face == BlockFace.UP || face == BlockFace.DOWN);

                if (frame == frames.get(0)) {
                    isVerticalWall = wall;
                    isHorizontal = floor;
                } else {
                    if (isVerticalWall && !wall) allSameOrientation = false;
                    if (isHorizontal && !floor) allSameOrientation = false;
                }
            }

            if (!allSameOrientation) {
                player.sendMessage("§cОшибка: Все рамки должны быть на одной стороне!");
                return;
            }

            // Вычисляем размеры сетки
            int minX = frames.stream().mapToInt(this::getFrameGridX).min().orElse(0);
            int maxX = frames.stream().mapToInt(this::getFrameGridX).max().orElse(0);
            int minY = frames.stream().mapToInt(this::getFrameGridY).min().orElse(0);
            int maxY = frames.stream().mapToInt(this::getFrameGridY).max().orElse(0);
            int minZ = frames.stream().mapToInt(this::getFrameGridZ).min().orElse(0);
            int maxZ = frames.stream().mapToInt(this::getFrameGridZ).max().orElse(0);

            int width, height;
            char faceChar = 'N';

            if (isVerticalWall) {
                BlockFace face = frames.get(0).getAttachedFace();
                if (face == BlockFace.NORTH) faceChar = 'N';
                else if (face == BlockFace.SOUTH) faceChar = 'S';
                else if (face == BlockFace.EAST) faceChar = 'E';
                else if (face == BlockFace.WEST) faceChar = 'W';

                if (face == BlockFace.NORTH || face == BlockFace.SOUTH) {
                    width = maxX - minX + 1;
                } else {
                    width = maxZ - minZ + 1;
                }
                height = maxY - minY + 1;
            } else {
                BlockFace face = frames.get(0).getAttachedFace();
                if (face == BlockFace.UP) faceChar = 'U';
                else if (face == BlockFace.DOWN) faceChar = 'D';

                width = maxX - minX + 1;
                height = maxZ - minZ + 1;
            }

            player.sendMessage("§a✓ Размер картины: " + width + "x" + height + " рамок");

            PaintManager paintManager = services.getService(PaintManager.class);
            Optional<PaintData> overlapPaint = paintManager.findOverlappingPaint(
                    player.getWorld().getName(),
                    minX, maxX,
                    minY, maxY,
                    minZ, maxZ,
                    faceChar
            );

            if (overlapPaint.isPresent()) {
                player.sendMessage("§cВ этой области уже есть картина! ID: " + overlapPaint.get().getId());
                return;
            }

            // СОРТИРОВКА
            final boolean verticalWall = isVerticalWall;

            frames.sort((f1, f2) -> {
                int x1 = getFrameGridX(f1);
                int y1 = getFrameGridY(f1);
                int z1 = getFrameGridZ(f1);

                int x2 = getFrameGridX(f2);
                int y2 = getFrameGridY(f2);
                int z2 = getFrameGridZ(f2);

                int yCompare = Integer.compare(y2, y1);
                if (yCompare != 0) return yCompare;

                if (verticalWall) {
                    BlockFace face = f1.getAttachedFace();
                    if (face == BlockFace.NORTH || face == BlockFace.SOUTH) {
                        return Integer.compare(x1, x2);
                    } else {
                        return Integer.compare(z1, z2);
                    }
                } else {
                    int zCompare = Integer.compare(z1, z2);
                    if (zCompare != 0) return zCompare;
                    return Integer.compare(x1, x2);
                }
            });

            // ✅ СОЗДАЕМ СТРАТЕГИЮ ПО ИМЕНИ
            ImageProcessingStrategy strategy = getStrategyByName(strategyName);
            player.sendMessage("§a✓ Используется стратегия: " + strategyName);

            // Масштабируем изображение
            int mapSize = 128;
            BufferedImage scaledImage = new BufferedImage(width * mapSize, height * mapSize,
                    BufferedImage.TYPE_INT_RGB);
            Graphics2D g = scaledImage.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            BufferedImage processed = strategy.process(image, width * mapSize, height * mapSize);
            g.drawImage(processed, 0, 0, width * mapSize, height * mapSize, null);
            g.dispose();

            // СОХРАНЯЕМ изображение локально
            String localPath;

            if (source.startsWith("http://") || source.startsWith("https://")) {
                localPath = paintManager.saveImageLocally(scaledImage, source);
                if (localPath == null) {
                    player.sendMessage("§cНе удалось сохранить изображение!");
                    return;
                }
            } else {
                localPath = "images/" + source + ".png";
                File imageFile = new File(ImagePlugin.getInstance().getDataFolder(), localPath);
                if (!imageFile.exists()) {
                    localPath = "images/" + source + ".jpg";
                    imageFile = new File(ImagePlugin.getInstance().getDataFolder(), localPath);
                    if (!imageFile.exists()) {
                        player.sendMessage("§cЛокальный файл не найден!");
                        return;
                    }
                }
            }

            // Создаем данные для рамок
            List<int[]> framesData = new ArrayList<>();
            int frameCount = 0;

            for (ItemFrame frame : frames) {
                try {
                    int frameX = getFrameGridX(frame);
                    int frameY = getFrameGridY(frame);
                    int frameZ = getFrameGridZ(frame);

                    int relX, relY;

                    if (isVerticalWall) {
                        relY = maxY - frameY;
                        BlockFace face = frame.getAttachedFace();
                        if (face == BlockFace.NORTH || face == BlockFace.SOUTH) {
                            relX = frameX - minX;
                        } else {
                            relX = frameZ - minZ;
                        }
                    } else {
                        relX = frameX - minX;
                        relY = maxZ - frameZ;
                    }

                    if (relX >= width || relY >= height) {
                        player.sendMessage("§eПредупреждение: рамка вне сетки (" + relX + ", " + relY + ")");
                        continue;
                    }

                    BufferedImage segment = scaledImage.getSubimage(
                            relX * mapSize, relY * mapSize, mapSize, mapSize
                    );

                    MapView mapView = player.getServer().createMap(player.getWorld());
                    mapView.getRenderers().clear();
                    mapView.addRenderer(new LazyMapRenderer(segment));

                    ItemStack mapItem = new ItemStack(org.bukkit.Material.FILLED_MAP);
                    MapMeta meta = (MapMeta) mapItem.getItemMeta();
                    meta.setMapView(mapView);
                    meta.setDisplayName(null);
                    mapItem.setItemMeta(meta);

                    frame.setItem(mapItem);
                    frameCount++;
                    framesData.add(new int[]{relX, relY, frameCount});

                } catch (Exception e) {
                    player.sendMessage("§cОшибка обработки рамки: " + e.getMessage());
                }
            }

            // Создаем PaintData
            // Важно: для восстановления сохраняем фактическую область РАМОК,
            // а не пользовательские точки ввода. Это исключает смещение,
            // если pos1/pos2 указывались по блоку стены/полу, а не по центрам рамок.
            int[] pos1Array = {minX, minY, minZ};
            int[] pos2Array = {maxX, maxY, maxZ};

            String paintId = paintManager.generatePaintId();
            PaintData paintData = new PaintData(
                    paintId,
                    player.getWorld().getName(),
                    pos1Array,
                    pos2Array,
                    source,
                    localPath,
                    player.getName(),
                    width,
                    height,
                    faceChar,
                    strategyName, // ✅ Сохраняем название стратегии!
                    framesData
            );

            paintManager.savePaint(paintData);

            player.sendMessage("§a✓ Картина создана! ID: " + paintId);
            player.sendMessage("§a✓ Обработано рамок: " + frameCount + "/" + frames.size());
            player.sendMessage("§a✓ Изображение сохранено: " + localPath);
            player.sendMessage("§a✓ Стратегия: " + strategyName);
            player.sendMessage("§a✓ Данные сохранены в отдельный файл картины");

        } catch (Exception e) {
            player.sendMessage("§cОшибка создания картины: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private ImageProcessingStrategy getStrategyByName(String name) {
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

    /**
     * Парсит локацию из строковых координат с поддержкой ~
     */
    private Location parseLocation(Player player, String x, String y, String z) {
        World world = player.getWorld();

        double parsedX = parseCoordinate(x, player.getLocation().getX());
        double parsedY = parseCoordinate(y, player.getLocation().getY());
        double parsedZ = parseCoordinate(z, player.getLocation().getZ());

        return new Location(world, parsedX, parsedY, parsedZ);
    }

    private boolean isFlatArea(Location pos1, Location pos2) {
        int x1 = pos1.getBlockX();
        int y1 = pos1.getBlockY();
        int z1 = pos1.getBlockZ();
        int x2 = pos2.getBlockX();
        int y2 = pos2.getBlockY();
        int z2 = pos2.getBlockZ();

        // Плоскость Y (пол/потолок) - все Y одинаковы
        if (y1 == y2) {
            // X и Z могут меняться - это плоскость
            return true;
        }

        // Плоскость X (стена запад/восток) - все X одинаковы
        if (x1 == x2) {
            // Y и Z могут меняться - это плоскость
            return true;
        }

        // Плоскость Z (стена север/юг) - все Z одинаковы
        if (z1 == z2) {
            // X и Y могут меняться - это плоскость
            return true;
        }

        // Если ни одна координата не совпадает - это объём!
        return false;
    }

    /**
     * Парсит отдельную координату с поддержкой ~
     */
    private double parseCoordinate(String input, double current) {
        if (input.startsWith("~")) {
            if (input.length() == 1) {
                return current;
            } else {
                try {
                    return current + Double.parseDouble(input.substring(1));
                } catch (NumberFormatException e) {
                    return current;
                }
            }
        } else {
            try {
                return Double.parseDouble(input);
            } catch (NumberFormatException e) {
                return current;
            }
        }
    }

    private Block getAttachedBlock(ItemFrame frame) {
        BlockFace facing = frame.getAttachedFace();
        if (facing == null) return null;
        return frame.getLocation().getBlock().getRelative(facing);
    }

    private int getFrameGridX(ItemFrame frame) {
        Block attached = getAttachedBlock(frame);
        return attached != null ? attached.getX() : frame.getLocation().getBlockX();
    }

    private int getFrameGridY(ItemFrame frame) {
        Block attached = getAttachedBlock(frame);
        return attached != null ? attached.getY() : frame.getLocation().getBlockY();
    }

    private int getFrameGridZ(ItemFrame frame) {
        Block attached = getAttachedBlock(frame);
        return attached != null ? attached.getZ() : frame.getLocation().getBlockZ();
    }

    private boolean isOnVerticalWall(ItemFrame frame) {
        BlockFace face = frame.getAttachedFace();
        return face == BlockFace.NORTH || face == BlockFace.SOUTH ||
                face == BlockFace.EAST || face == BlockFace.WEST;
    }

    /**
     * Форматирует локацию для вывода в чат
     */
    private String formatLocation(Location loc) {
        if (loc == null) return "§7(?, ?, ?)";
        return String.format("§7(%d, %d, %d)",
                loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    @Override
    public Optional<String> getPermission() {
        return Optional.of("imageplugin.admin");
    }

    @Override
    public String getUsage() {
        return "/image paint <x1> <y1> <z1> <x2> <y2> <z2> <source>";
    }
}
