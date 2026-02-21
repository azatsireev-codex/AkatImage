package net.akat.command;

import net.akat.ServiceLocator;
import net.akat.map.MapFactory;
import net.akat.mosaic.Mosaic;
import net.akat.mosaic.MosaicBuilder;
import net.akat.mosaic.strategy.PreserveAspectRatioStrategy;
import net.akat.util.ImageDownloader;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.awt.image.BufferedImage;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class CreateFromUrlCommand implements Command {
    private final ServiceLocator services;

    public CreateFromUrlCommand(ServiceLocator services) {
        this.services = services;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cТолько игроки могут использовать эту команду!");
            return;
        }

        Player player = (Player) sender;

        if (args.length < 3) {  // Изменено: минимально 3 аргумента
            player.sendMessage("§cИспользование: " + getUsage());
            return;
        }

        try {
            String[] sizeParts = args[1].toLowerCase().split("x");
            int width = Integer.parseInt(sizeParts[0]);
            int height = Integer.parseInt(sizeParts[1]);
            String url = args[2];

            player.sendMessage("§e⏳ Загрузка изображения с URL: " + url);

            // Асинхронная загрузка - БЕЗ СОХРАНЕНИЯ
            CompletableFuture<BufferedImage> future = ImageDownloader.downloadImage(url);

            future.thenAcceptAsync(image -> {
                try {
                    // СОЗДАЕМ мозаику БЕЗ сохранения в репозиторий
                    Mosaic mosaic = new MosaicBuilder()
                            .setSourceImage(image)
                            .setGridSize(width, height)
                            .setImageStrategy(new PreserveAspectRatioStrategy())
                            .setPlayer(player)
                            .setMapFactory(services.getService(MapFactory.class))
                            .build();

                    mosaic.giveToPlayer();
                    player.sendMessage("§a✓ Создана мозаика " + width + "x" + height + " из URL!");
                    player.sendMessage("§7└ Изображение НЕ сохранено на диск (одноразовое использование)");

                } catch (Exception e) {
                    player.sendMessage("§cОшибка создания мозаики: " + e.getMessage());
                }
            }).exceptionally(throwable -> {
                player.sendMessage("§cОшибка загрузки: " + throwable.getCause().getMessage());
                return null;
            });

        } catch (Exception e) {
            player.sendMessage("§cОшибка: " + e.getMessage());
        }
    }

    @Override
    public Optional<String> getPermission() {
        return Optional.of("imageplugin.admin");
    }

    @Override
    public String getUsage() {
        return "/image url <ширина>x<высота> <url>";  // Убрано [имя_для_сохранения]
    }
}
