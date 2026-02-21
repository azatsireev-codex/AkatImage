package net.akat.command;

import net.akat.image.ImageRepository;
import net.akat.ServiceLocator;
import net.akat.painting.PaintManager;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.*;
import java.util.stream.Collectors;

public class ImageCommand implements CommandExecutor, TabCompleter {
    private final Map<String, Command> commands = new HashMap<>();
    private final ServiceLocator services;

    public ImageCommand(ServiceLocator services) {
        this.services = services;

        registerCommand("create", new CreateMapCommand(services));
        registerCommand("url", new CreateFromUrlCommand(services));
        registerCommand("paint", new CreatePaintingCommand(services));
        registerCommand("reload", new ReloadCommand(services));
        registerCommand("update", new UpdateCommand(services));
        registerCommand("delete", new DeletePaintingCommand(services));
        registerCommand("list", new ListCommand(services));
    }

    private void registerCommand(String name, Command command) {
        commands.put(name.toLowerCase(), command);
    }

    @Override
    public boolean onCommand(CommandSender sender, org.bukkit.command.Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();
        Command cmd = commands.get(subCommand);

        if (cmd == null) {
            sender.sendMessage("§cНеизвестная команда!");
            sendHelp(sender);
            return true;
        }

        // Проверка прав
        if (cmd.getPermission().isPresent() && !sender.hasPermission(cmd.getPermission().get())) {
            sender.sendMessage("§cУ вас нет прав для этой команды!");
            return true;
        }

        cmd.execute(sender, args);
        return true;
    }

    private void sendHelp(CommandSender sender) {
        commands.forEach((name, cmd) -> {
            if (!cmd.getPermission().isPresent() || sender.hasPermission(cmd.getPermission().get())) {
                sender.sendMessage("§7" + cmd.getUsage());
            }
        });
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, org.bukkit.command.Command command, String alias, String[] args) {
        if (args.length == 1) {
            return commands.keySet().stream()
                    .filter(cmd -> {
                        Command c = commands.get(cmd);
                        return !c.getPermission().isPresent() || sender.hasPermission(c.getPermission().get());
                    })
                    .filter(cmd -> cmd.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("create")) {
            return Arrays.asList("1x1", "2x2", "3x3", "4x4", "5x5");
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("url")) {
            return Arrays.asList("1x1", "2x2", "3x3", "4x4", "5x5");
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("paint")) {
            return Arrays.asList("~", "~1", "~-1", "100");
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("create")) {
            ImageRepository repository = services.getService(ImageRepository.class);
            return repository.getAllNames().stream()
                    .filter(name -> name.startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("url")) {
            return Arrays.asList("https://i.imgur.com/", "https://example.com/image.png");
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("paint")) {
            return Arrays.asList("~", "~1", "~-1", "64");
        }

        if (args.length == 4 && args[0].equalsIgnoreCase("paint")) {
            return Arrays.asList("~", "~1", "~-1", "100");
        }

        if (args.length == 5 && args[0].equalsIgnoreCase("paint")) {
            return Arrays.asList("~5", "~10", "107");
        }

        if (args.length == 6 && args[0].equalsIgnoreCase("paint")) {
            return Arrays.asList("~3", "~5", "67");
        }

        if (args.length == 7 && args[0].equalsIgnoreCase("paint")) {
            return Arrays.asList("~", "~-1", "100");
        }

        if (args.length == 8 && args[0].equalsIgnoreCase("paint")) {
            ImageRepository repository = services.getService(ImageRepository.class);
            List<String> suggestions = new ArrayList<>(repository.getAllNames());
            suggestions.add("https://i.imgur.com/");
            suggestions.add("https://example.com/");
            return suggestions.stream()
                    .filter(name -> name.startsWith(args[7].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 9 && args[0].equalsIgnoreCase("paint")) {
            return Arrays.asList("preserve", "scale", "crop").stream()
                    .filter(s -> s.startsWith(args[8].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("update")) {
            PaintManager paintManager = services.getService(PaintManager.class);
            List<String> options = new ArrayList<>();
            options.add("all");
            paintManager.getAllPaints().forEach(paint -> options.add(paint.getId()));
            return options.stream()
                    .filter(option -> option.startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("delete")) {
            PaintManager paintManager = services.getService(PaintManager.class);
            return paintManager.getAllPaints().stream()
                    .map(paint -> paint.getId())
                    .filter(id -> id.startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }
}
