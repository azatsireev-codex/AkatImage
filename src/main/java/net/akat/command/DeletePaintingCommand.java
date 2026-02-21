package net.akat.command;

import net.akat.ServiceLocator;
import net.akat.painting.PaintManager;
import org.bukkit.command.CommandSender;

import java.util.Optional;

public class DeletePaintingCommand implements Command {
    private final ServiceLocator services;

    public DeletePaintingCommand(ServiceLocator services) {
        this.services = services;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cИспользование: " + getUsage());
            return;
        }

        String id = args[1];
        PaintManager paintManager = services.getService(PaintManager.class);

        if (!paintManager.deletePaintById(id)) {
            sender.sendMessage("§cКартина с ID " + id + " не найдена!");
            return;
        }

        sender.sendMessage("§aКартина " + id + " удалена.");
    }

    @Override
    public Optional<String> getPermission() {
        return Optional.of("imageplugin.delete");
    }

    @Override
    public String getUsage() {
        return "/image delete <id>";
    }
}
