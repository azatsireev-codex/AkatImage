package net.akat.command;

import net.akat.ServiceLocator;
import net.akat.image.ImageRepository;
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

        ImageRepository repository = services.getService(ImageRepository.class);
        repository.loadAll();
        sender.sendMessage("§aИзображения перезагружены!");
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
