package net.akat.config;

import net.akat.mosaic.strategy.CropImageStrategy;
import net.akat.mosaic.strategy.ImageProcessingStrategy;
import net.akat.mosaic.strategy.PreserveAspectRatioStrategy;
import net.akat.mosaic.strategy.ScaleImageStrategy;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class PluginConfigService implements ConfigService {
    private final JavaPlugin plugin;
    private FileConfiguration config;

    public PluginConfigService(JavaPlugin plugin) {
        this.plugin = plugin;
        plugin.saveDefaultConfig();
        this.config = plugin.getConfig();
    }

    @Override
    public int getMaxMosaicSize() {
        return config.getInt("max-mosaic-size", 10);
    }

    @Override
    public int getMapSize() {
        return config.getInt("map-size", 128);
    }

    @Override
    public boolean isDebugMode() {
        return config.getBoolean("debug", false);
    }

    @Override
    public ImageProcessingStrategy getDefaultStrategy() {
        String strategy = config.getString("default-strategy", "scale");
        switch (strategy.toLowerCase()) {
            case "crop":
                return new CropImageStrategy();
            case "aspect":
                return new PreserveAspectRatioStrategy();
            case "scale":
            default:
                return new ScaleImageStrategy();
        }
    }

    @Override
    public void reload() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();
    }
}
