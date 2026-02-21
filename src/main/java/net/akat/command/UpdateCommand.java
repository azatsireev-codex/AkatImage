package net.akat.command;

import net.akat.ServiceLocator;
import net.akat.painting.PaintData;
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

        if (args.length < 2 || args[1].equalsIgnoreCase("all")) {
            paintManager.restoreAllPaints();
            sender.sendMessage("§aВсе картины перерисованы!");
            return;
        }

        String id = args[1];
        Optional<PaintData> paintOpt = paintManager.getPaintById(id);
        if (!paintOpt.isPresent()) {
            sender.sendMessage("§cКартина с ID " + id + " не найдена!");
            return;
        }

        boolean restored = paintManager.restorePaint(paintOpt.get());
        if (restored) {
            sender.sendMessage("§aКартина " + id + " успешно перерисована.");
        } else {
            sender.sendMessage("§eКартина " + id + " перерисована частично. Проверьте логи.");
        }
    }

    @Override
    public Optional<String> getPermission() {
        return Optional.of("imageplugin.admin");
    }

    @Override
    public String getUsage() {
        return "/image update <all|id>";
    }
}
