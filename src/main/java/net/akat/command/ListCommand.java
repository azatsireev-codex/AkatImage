package net.akat.command;

import net.akat.ServiceLocator;
import net.akat.painting.PaintData;
import net.akat.painting.PaintManager;
import org.bukkit.command.CommandSender;

import java.util.Collection;
import java.util.Optional;

public class ListCommand implements Command {
    private final ServiceLocator services;

    public ListCommand(ServiceLocator services) {
        this.services = services;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        PaintManager paintManager = services.getService(PaintManager.class);
        Collection<PaintData> paints = paintManager.getAllPaints();

        sender.sendMessage("§6Картины (" + paints.size() + "):");
        if (paints.isEmpty()) {
            sender.sendMessage("§7Нет сохранённых картин");
            return;
        }

        for (PaintData paint : paints) {
            sender.sendMessage("§7- §f" + paint.getId() + " §8(" + paint.getWidth() + "x" + paint.getHeight() + ", " + paint.getWorld() + ")");
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
