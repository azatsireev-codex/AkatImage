package net.akat.util;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ImageDownloader {

    private static final int TIMEOUT = 5000; // 5 секунд
    private static final int MAX_SIZE = 1024; // Максимальный размер в пикселях

    public static CompletableFuture<BufferedImage> downloadImage(String urlString) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Проверяем URL
                if (!isValidImageUrl(urlString)) {
                    throw new IllegalArgumentException("Неверный формат URL или неподдерживаемый формат");
                }

                // Открываем соединение
                URL url = new URL(urlString);
                URLConnection connection = url.openConnection();
                connection.setConnectTimeout(TIMEOUT);
                connection.setReadTimeout(TIMEOUT);
                connection.setRequestProperty("User-Agent", "Minecraft-ImagePlugin/1.0");

                // Читаем изображение
                BufferedImage original = ImageIO.read(connection.getInputStream());
                if (original == null) {
                    throw new IOException("Не удалось прочитать изображение");
                }

                // Проверяем размер
                if (original.getWidth() > MAX_SIZE || original.getHeight() > MAX_SIZE) {
                    throw new IllegalArgumentException("Изображение слишком большое! Максимум " + MAX_SIZE + "x" + MAX_SIZE);
                }

                return original;

            } catch (IOException e) {
                throw new RuntimeException("Ошибка загрузки: " + e.getMessage());
            }
        });
    }

    private static boolean isValidImageUrl(String url) {
        String lower = url.toLowerCase();
        return (lower.startsWith("http://") || lower.startsWith("https://")) &&
                (lower.endsWith(".png") || lower.endsWith(".jpg") ||
                        lower.endsWith(".jpeg") || lower.endsWith(".gif") ||
                        lower.contains("imgur.com") || lower.contains("i.imgur.com"));
    }

    public static String generateImageName(String url) {
        // Генерируем имя из URL или UUID
        String[] parts = url.split("/");
        String lastPart = parts[parts.length - 1];
        String name = lastPart.contains(".") ?
                lastPart.substring(0, lastPart.lastIndexOf('.')) :
                "url_" + UUID.randomUUID().toString().substring(0, 8);

        // Очищаем имя от спецсимволов
        return name.replaceAll("[^a-zA-Z0-9_-]", "_").toLowerCase();
    }
}
