package net.akat.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.awt.Color;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class MapColorUtil {
    private static final Map<Integer, Color> PROTOCOL_COLORS = new HashMap<>();
    private static boolean loaded = false;
    private static final String MC_VERSION = "1.21.8";

    public static void loadColors() {
        if (loaded) return;

        try {
            ClassLoader classLoader = MapColorUtil.class.getClassLoader();
            InputStream in = classLoader.getResourceAsStream("colors.json");

            if (in == null) {
                System.err.println("§c[ImagePlugin] colors.json не найден!");
                return;
            }

            try (InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                JsonObject version = root.getAsJsonObject(MC_VERSION);

                for (String key : version.keySet()) {
                    int baseId = Integer.parseInt(key);
                    JsonObject colorObj = version.getAsJsonObject(key);
                    JsonArray colors = colorObj.getAsJsonArray("colors");

                    for (int v = 0; v < colors.size(); v++) {
                        int rgb = colors.get(v).getAsInt();
                        if (rgb != 0) {
                            PROTOCOL_COLORS.put(baseId * 4 + v, new Color(rgb, false));
                        }
                    }
                }
                loaded = true;
                System.out.println("§a[ImagePlugin] Загружено " + PROTOCOL_COLORS.size() + " цветов");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static byte getClosestColor(int rgb) {
        if (!loaded) loadColors();
        if (PROTOCOL_COLORS.isEmpty()) return 12;

        Color target = new Color(rgb);
        int bestId = 4;
        double bestDistance = Double.MAX_VALUE;

        for (Map.Entry<Integer, Color> entry : PROTOCOL_COLORS.entrySet()) {
            Color c = entry.getValue();
            double distance = Math.pow(target.getRed() - c.getRed(), 2) * 0.299 +
                    Math.pow(target.getGreen() - c.getGreen(), 2) * 0.587 +
                    Math.pow(target.getBlue() - c.getBlue(), 2) * 0.114;

            if (distance < bestDistance) {
                bestDistance = distance;
                bestId = entry.getKey();
            }
        }

        return (byte) bestId;
    }
}
