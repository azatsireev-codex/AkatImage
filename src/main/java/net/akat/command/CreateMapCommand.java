package net.akat.command;

import net.akat.ServiceLocator;
import net.akat.image.ImageRepository;
import net.akat.map.MapFactory;
import net.akat.mosaic.Mosaic;
import net.akat.mosaic.MosaicBuilder;
import net.akat.mosaic.strategy.PreserveAspectRatioStrategy;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.awt.image.BufferedImage;
import java.util.Optional;

public class CreateMapCommand implements Command {
    private final ServiceLocator services;

    public CreateMapCommand(ServiceLocator services) {
        this.services = services;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cТолько игроки могут использовать эту команду!");
            return;
        }

        Player player = (Player) sender;

        if (args.length < 3) {
            player.sendMessage("§cИспользование: " + getUsage());
            return;
        }

        try {
            String[] sizeParts = args[1].toLowerCase().split("x");
            int width = Integer.parseInt(sizeParts[0]);
            int height = Integer.parseInt(sizeParts[1]);
            String imageName = args[2].toLowerCase();

            ImageRepository repository = services.getService(ImageRepository.class);
            Optional<BufferedImage> imageOpt = repository.findByName(imageName);

            if (!imageOpt.isPresent()) {
                player.sendMessage("§cИзображение не найдено!");
                return;
            }

            Mosaic mosaic = new MosaicBuilder()
                    .setSourceImage(imageOpt.get())
                    .setGridSize(width, height)
                    .setImageStrategy(new PreserveAspectRatioStrategy())
                    .setPlayer(player)
                    .setMapFactory(services.getService(MapFactory.class))
                    .build();

            mosaic.giveToPlayer();
            player.sendMessage("§aСоздана мозаика " + width + "x" + height + " из '" + imageName + "'!");

        } catch (Exception e) {
            player.sendMessage("§cОшибка: " + e.getMessage());
        }
    }

    @Override
    public Optional<String> getPermission() {
        return Optional.of("imageplugin.create");
    }

    @Override
    public String getUsage() {
        return "/image create <ширина>x<высота> <изображение>";
    }
}
