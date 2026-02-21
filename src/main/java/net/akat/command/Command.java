package net.akat.command;

import org.bukkit.command.CommandSender;
import java.util.Optional;

public interface Command {
    void execute(CommandSender sender, String[] args);
    Optional<String> getPermission();
    String getUsage();
}
