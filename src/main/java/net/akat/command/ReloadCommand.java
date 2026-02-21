package net.akat.command;

import net.akat.ServiceLocator;
import net.akat.config.ConfigService;
import net.akat.image.ImageRepository;
import net.akat.painting.PaintManager;
import org.bukkit.command.CommandSender;

import java.util.Optional;

public class ReloadCommand implements Command {
    private final ServiceLocator services;

    public ReloadCommand(ServiceLocator services) {
        this.services = services;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("imageplugin.reload")) {
            sender.sendMessage("§cУ вас нет прав!");
            return;
        }

        ConfigService configService = services.getService(ConfigService.class);
        ImageRepository repository = services.getService(ImageRepository.class);
        PaintManager paintManager = services.getService(PaintManager.class);

        configService.reload();
        repository.loadAll();
        paintManager.loadPaints();

        sender.sendMessage("§aКонфигурации плагина перезагружены!");
    }

    @Override
    public Optional<String> getPermission() {
        return Optional.of("imageplugin.reload");
    }

    @Override
    public String getUsage() {
        return "/image reload";
    }
}
