package net.akat.mosaic;

import net.akat.map.LazyMapRenderer;
import net.akat.map.MapFactory;
import net.akat.mosaic.strategy.ImageProcessingStrategy;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.map.MapView;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class Mosaic {
    private final BufferedImage sourceImage;
    private final int gridWidth;
    private final int gridHeight;
    private final ImageProcessingStrategy strategy;
    private final Player player;
    private final MapFactory mapFactory;
    private final List<ItemStack> mapItems;

    public Mosaic(BufferedImage sourceImage, int gridWidth, int gridHeight,
                  ImageProcessingStrategy strategy, Player player, MapFactory mapFactory) {
        this.sourceImage = sourceImage;
        this.gridWidth = gridWidth;
        this.gridHeight = gridHeight;
        this.strategy = strategy;
        this.player = player;
        this.mapFactory = mapFactory;
        this.mapItems = new ArrayList<>();
        generate();
    }

    private void generate() {
        int mapSize = 128;
        int totalWidth = mapSize * gridWidth;
        int totalHeight = mapSize * gridHeight;

        BufferedImage processedImage = strategy.process(sourceImage, totalWidth, totalHeight);

        int index = 1;
        int total = gridWidth * gridHeight;

        for (int y = 0; y < gridHeight; y++) {
            for (int x = 0; x < gridWidth; x++) {
                BufferedImage segment = processedImage.getSubimage(
                        x * mapSize, y * mapSize, mapSize, mapSize
                );

                // 1. Создаем карту
                ItemStack mapItem = mapFactory.createMap(player, index++, total);

                // 2. Получаем MapMeta и MapView
                org.bukkit.inventory.meta.MapMeta meta = (org.bukkit.inventory.meta.MapMeta) mapItem.getItemMeta();
                MapView mapView = meta.getMapView();

                if (mapView != null) {
                    // 3. ВАЖНО: Очищаем ВСЕ существующие рендереры
                    mapView.getRenderers().clear();

                    // 4. Создаем НОВЫЙ рендерер с изображением
                    LazyMapRenderer renderer = new LazyMapRenderer(segment);

                    // 5. Добавляем наш рендерер
                    mapView.addRenderer(renderer);

                    // 6. Обновляем мету
                    meta.setMapView(mapView);
                    mapItem.setItemMeta(meta);
                }

                mapItems.add(mapItem);
            }
        }
    }

    public List<ItemStack> getMapItems() {
        return new ArrayList<>(mapItems);
    }

    public void giveToPlayer() {
        mapItems.forEach(item -> {
            player.getInventory().addItem(item);
            // Небольшая задержка для сервера
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }
}
