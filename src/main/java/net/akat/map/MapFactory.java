package net.akat.map;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public interface MapFactory {
    ItemStack createMap(Player player, int index, int total);
}
