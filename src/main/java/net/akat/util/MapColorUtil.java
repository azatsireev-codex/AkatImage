package net.akat.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapColorUtil {
    private static final Map<Integer, Integer> PROTOCOL_COLORS = new HashMap<>();
    private static final String MC_VERSION = "1.21.8";

    // Небоксированный direct-mapped cache (rgb -> map color)
    private static final int RGB_CACHE_SIZE = 1 << 16;
    private static final int RGB_CACHE_MASK = RGB_CACHE_SIZE - 1;
    private static final int[] RGB_CACHE_KEYS = new int[RGB_CACHE_SIZE];
    private static final byte[] RGB_CACHE_VALUES = new byte[RGB_CACHE_SIZE];

    // Предрасчёт весов для dr/dg/db в диапазоне [-255..255]
    private static final int DIFF_OFFSET = 255;
    private static final int[] RED_WEIGHTED_SQUARE = new int[511];
    private static final int[] GREEN_WEIGHTED_SQUARE = new int[511];
    private static final int[] BLUE_WEIGHTED_SQUARE = new int[511];

    private static int[] paletteIds = new int[0];
    private static int[] paletteReds = new int[0];
    private static int[] paletteGreens = new int[0];
    private static int[] paletteBlues = new int[0];

    private static boolean loaded = false;

    static {
        // -1 невалиден как ключ RGB (так как rgb маскируется до 0xFFFFFF)
        java.util.Arrays.fill(RGB_CACHE_KEYS, -1);

        for (int d = -255; d <= 255; d++) {
            int idx = d + DIFF_OFFSET;
            int sq = d * d;
            RED_WEIGHTED_SQUARE[idx] = sq * 299;
            GREEN_WEIGHTED_SQUARE[idx] = sq * 587;
            BLUE_WEIGHTED_SQUARE[idx] = sq * 114;
        }
    }

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

        int cacheIndex = rgb & RGB_CACHE_MASK;
        if (RGB_CACHE_KEYS[cacheIndex] == rgb) {
            return RGB_CACHE_VALUES[cacheIndex];
        }

        int targetRed = (rgb >> 16) & 0xFF;
        int targetGreen = (rgb >> 8) & 0xFF;
        int targetBlue = rgb & 0xFF;

        int bestId = 4;
        int bestDistance = Integer.MAX_VALUE;

        for (int i = 0; i < paletteIds.length; i++) {
            int drIdx = targetRed - paletteReds[i] + DIFF_OFFSET;
            int dgIdx = targetGreen - paletteGreens[i] + DIFF_OFFSET;
            int dbIdx = targetBlue - paletteBlues[i] + DIFF_OFFSET;

            int distance = RED_WEIGHTED_SQUARE[drIdx]
                    + GREEN_WEIGHTED_SQUARE[dgIdx]
                    + BLUE_WEIGHTED_SQUARE[dbIdx];

            if (distance < bestDistance) {
                bestDistance = distance;
                bestId = paletteIds[i];

                if (distance == 0) {
                    break;
                }
            }
        }

        byte bestColor = (byte) bestId;
        RGB_CACHE_KEYS[cacheIndex] = rgb;
        RGB_CACHE_VALUES[cacheIndex] = bestColor;
        return bestColor;
    }
}
