package net.akat.service;

import org.bukkit.plugin.java.JavaPlugin;

public class BukkitLoggerService implements LoggerService {
    private final JavaPlugin plugin;

    public BukkitLoggerService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void info(String message) {
        plugin.getLogger().info(message);
    }

    @Override
    public void warning(String message) {
        plugin.getLogger().warning(message);
    }

    @Override
    public void error(String message) {
        plugin.getLogger().severe(message);
    }

    @Override
    public void debug(String message) {
        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info("[DEBUG] " + message);
        }
    }
}
