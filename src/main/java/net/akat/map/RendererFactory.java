package net.akat.map;

import org.bukkit.map.MapRenderer;

import java.awt.image.BufferedImage;

public interface RendererFactory {
    MapRenderer createRenderer();
    MapRenderer createRenderer(BufferedImage image);
}
