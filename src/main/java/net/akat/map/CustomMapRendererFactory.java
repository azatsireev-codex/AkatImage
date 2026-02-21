package net.akat.map;

import org.bukkit.map.MapRenderer;

import java.awt.image.BufferedImage;

public class CustomMapRendererFactory implements RendererFactory {
    @Override
    public MapRenderer createRenderer() {
        return new LazyMapRenderer();
    }

    @Override
    public MapRenderer createRenderer(BufferedImage image) {
        return new LazyMapRenderer(image);
    }
}
