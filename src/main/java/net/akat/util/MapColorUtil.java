package net.akat.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MapColorUtil {
    private static final Map<Integer, Integer> PROTOCOL_COLORS = new HashMap<>();
    private static final int RGB_CACHE_LIMIT = 65_536;
    private static final Map<Integer, Byte> RGB_TO_MAP_COLOR = new LinkedHashMap<>(RGB_CACHE_LIMIT, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Integer, Byte> eldest) {
            return size() > RGB_CACHE_LIMIT;
        }
    };

    private static int[] paletteIds = new int[0];
    private static int[] paletteReds = new int[0];
    private static int[] paletteGreens = new int[0];
    private static int[] paletteBlues = new int[0];

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
                List<Integer> ids = new ArrayList<>();
                List<Integer> rgbs = new ArrayList<>();

                for (String key : version.keySet()) {
                    int baseId = Integer.parseInt(key);
                    JsonObject colorObj = version.getAsJsonObject(key);
                    JsonArray colors = colorObj.getAsJsonArray("colors");

                    for (int v = 0; v < colors.size(); v++) {
                        int rgb = colors.get(v).getAsInt();
                        if (rgb != 0) {
                            int id = baseId * 4 + v;
                            PROTOCOL_COLORS.put(id, rgb);
                            ids.add(id);
                            rgbs.add(rgb);
                        }
                    }
                }

                paletteIds = new int[ids.size()];
                paletteReds = new int[ids.size()];
                paletteGreens = new int[ids.size()];
                paletteBlues = new int[ids.size()];

                for (int i = 0; i < ids.size(); i++) {
                    int rgb = rgbs.get(i);
                    paletteIds[i] = ids.get(i);
                    paletteReds[i] = (rgb >> 16) & 0xFF;
                    paletteGreens[i] = (rgb >> 8) & 0xFF;
                    paletteBlues[i] = rgb & 0xFF;
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

        rgb = rgb & 0xFFFFFF;

        Byte cached = RGB_TO_MAP_COLOR.get(rgb);
        if (cached != null) {
            return cached;
        }

        int targetRed = (rgb >> 16) & 0xFF;
        int targetGreen = (rgb >> 8) & 0xFF;
        int targetBlue = rgb & 0xFF;

        int bestId = 4;
        long bestDistance = Long.MAX_VALUE;

        for (int i = 0; i < paletteIds.length; i++) {
            int dr = targetRed - paletteReds[i];
            int dg = targetGreen - paletteGreens[i];
            int db = targetBlue - paletteBlues[i];

            long distance = (long) dr * dr * 299L
                    + (long) dg * dg * 587L
                    + (long) db * db * 114L;

            if (distance < bestDistance) {
                bestDistance = distance;
                bestId = paletteIds[i];
            }
        }

        byte bestColor = (byte) bestId;
        RGB_TO_MAP_COLOR.put(rgb, bestColor);
        return bestColor;
    }
}
