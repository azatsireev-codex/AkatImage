package net.akat.command;

import net.akat.ServiceLocator;
import net.akat.painting.PaintManager;
import org.bukkit.command.CommandSender;

import java.util.Optional;

public class UpdateCommand implements Command {
    private final ServiceLocator services;

    public UpdateCommand(ServiceLocator services) {
        this.services = services;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        PaintManager paintManager = services.getService(PaintManager.class);
        paintManager.restoreAllPaints();
        sender.sendMessage("§aВсе картины перерисованы!");
    }

    @Override
    public Optional<String> getPermission() {
        return Optional.of("imageplugin.update");
    }

    @Override
    public String getUsage() {
        return "/image update";
    }
}
