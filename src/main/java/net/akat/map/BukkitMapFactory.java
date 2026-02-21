package net.akat.map;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapView;
import org.bukkit.plugin.java.JavaPlugin;

public class BukkitMapFactory implements MapFactory {
    private final JavaPlugin plugin;
    private final RendererFactory rendererFactory;

    public BukkitMapFactory(JavaPlugin plugin) {
        this.plugin = plugin;
        this.rendererFactory = new CustomMapRendererFactory();
    }

    @Override
    public ItemStack createMap(Player player, int index, int total) {
        MapView mapView = plugin.getServer().createMap(player.getWorld());

        mapView.getRenderers().clear();
        LazyMapRenderer renderer = new LazyMapRenderer();
        mapView.addRenderer(renderer);

        ItemStack mapItem = new ItemStack(org.bukkit.Material.FILLED_MAP);
        MapMeta meta = (MapMeta) mapItem.getItemMeta();
        meta.setMapView(mapView);

        meta.setDisplayName(null);

        mapItem.setItemMeta(meta);

        return mapItem;
    }
}
