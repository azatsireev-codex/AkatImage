package net.akat.mosaic;

import net.akat.mosaic.strategy.ImageProcessingStrategy;
import net.akat.map.MapFactory;
import net.akat.mosaic.strategy.ScaleImageStrategy;
import org.bukkit.entity.Player;

import java.awt.image.BufferedImage;

public class MosaicBuilder {
    private BufferedImage sourceImage;
    private int gridWidth;
    private int gridHeight;
    private ImageProcessingStrategy strategy;
    private Player player;
    private MapFactory mapFactory;

    public MosaicBuilder setSourceImage(BufferedImage image) {
        this.sourceImage = image;
        return this;
    }

    public MosaicBuilder setGridSize(int width, int height) {
        this.gridWidth = width;
        this.gridHeight = height;
        return this;
    }

    public MosaicBuilder setImageStrategy(ImageProcessingStrategy strategy) {
        this.strategy = strategy;
        return this;
    }

    public MosaicBuilder setPlayer(Player player) {
        this.player = player;
        return this;
    }

    public MosaicBuilder setMapFactory(MapFactory factory) {
        this.mapFactory = factory;
        return this;
    }

    public Mosaic build() {
        validate();
        return new Mosaic(sourceImage, gridWidth, gridHeight, strategy, player, mapFactory);
    }

    private void validate() {
        if (sourceImage == null) throw new IllegalStateException("Source image is required");
        if (gridWidth <= 0 || gridHeight <= 0) throw new IllegalStateException("Invalid grid size");
        if (player == null) throw new IllegalStateException("Player is required");
        if (mapFactory == null) throw new IllegalStateException("Map factory is required");
        if (strategy == null) strategy = new ScaleImageStrategy();
    }
}
