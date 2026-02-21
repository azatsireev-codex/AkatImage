package net.akat.command;

import net.akat.ServiceLocator;
import net.akat.image.ImageRepository;
import org.bukkit.command.CommandSender;

import java.util.Optional;
import java.util.Set;

public class ListCommand implements Command {
    private final ServiceLocator services;

    public ListCommand(ServiceLocator services) {
        this.services = services;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        ImageRepository repository = services.getService(ImageRepository.class);
        Set<String> images = repository.getAllNames();

        sender.sendMessage("§6Доступные изображения (" + images.size() + "):");
        if (images.isEmpty()) {
            sender.sendMessage("§7Нет загруженных изображений");
        } else {
            sender.sendMessage("§7" + String.join(", ", images));
        }
    }

    @Override
    public Optional<String> getPermission() {
        return Optional.of("imageplugin.list");
    }

    @Override
    public String getUsage() {
        return "/image list";
    }
}
