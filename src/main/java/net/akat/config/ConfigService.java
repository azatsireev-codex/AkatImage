package net.akat.config;

import net.akat.mosaic.strategy.ImageProcessingStrategy;

public interface ConfigService {
    int getMaxMosaicSize();
    int getMapSize();
    boolean isDebugMode();
    ImageProcessingStrategy getDefaultStrategy();
    void reload();
}
