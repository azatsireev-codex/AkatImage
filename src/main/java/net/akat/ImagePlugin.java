package net.akat;

import net.akat.command.ImageCommand;
import net.akat.config.ConfigService;
import net.akat.config.PluginConfigService;
import net.akat.image.FileImageRepository;
import net.akat.image.ImageRepository;
import net.akat.map.BukkitMapFactory;
import net.akat.map.CustomMapRendererFactory;
import net.akat.map.MapFactory;
import net.akat.map.RendererFactory;
import net.akat.painting.PaintManager;
import net.akat.service.BukkitLoggerService;
import net.akat.service.LoggerService;
import org.bukkit.plugin.java.JavaPlugin;

public class ImagePlugin extends JavaPlugin {

    private static ImagePlugin instance;
    private ServiceLocator serviceLocator;
    private PaintManager paintManager;

    @Override
    public void onEnable() {
        instance = this;

        // Инициализация сервисов через Service Locator
        serviceLocator = new ServiceLocator();

        // Регистрация сервисов
        serviceLocator.registerService(ImagePlugin.class, this);
        serviceLocator.registerService(LoggerService.class, new BukkitLoggerService(this));
        serviceLocator.registerService(ConfigService.class, new PluginConfigService(this));
        serviceLocator.registerService(ImageRepository.class, new FileImageRepository(this));
        serviceLocator.registerService(MapFactory.class, new BukkitMapFactory(this));
        serviceLocator.registerService(RendererFactory.class, new CustomMapRendererFactory());

        paintManager = new PaintManager(this);
        serviceLocator.registerService(PaintManager.class, paintManager);

        // Инициализация команды через Command Pattern
        ImageCommand command = new ImageCommand(serviceLocator);
        getCommand("image").setExecutor(command);

        getLogger().info("§aЗагружено картин из paint.yml: " + paintManager.getAllPaints().size());
    }

    @Override
    public void onDisable() {
        if (paintManager != null) {
            paintManager.saveAllPaints();
            getLogger().info("§aСохранено картин в paint.yml");
        }
    }

    public static ImagePlugin getInstance() {
        return instance;
    }

    public ServiceLocator getServiceLocator() {
        return serviceLocator;
    }
}
