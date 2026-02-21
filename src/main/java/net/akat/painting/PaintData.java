package net.akat.painting;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.serialization.ConfigurationSerializable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PaintData implements ConfigurationSerializable {
    private final String id;
    private final String world;
    private final int[] pos1;
    private final int[] pos2;
    private final String source;
    private final String localPath;
    private final String owner;
    private final long createdAt;
    private final int[] size;
    private final char face;
    private final String strategy;
    private final List<int[]> frames;

    public PaintData(String id, String world, int[] pos1, int[] pos2,
                     String source, String localPath, String owner,
                     int width, int height, char face, String strategy, List<int[]> frames) {
        this.id = id;
        this.world = world;
        this.pos1 = pos1;
        this.pos2 = pos2;
        this.source = source;
        this.localPath = localPath;
        this.owner = owner;
        this.createdAt = System.currentTimeMillis();
        this.size = new int[]{width, height};
        this.face = face;
        this.strategy = strategy;
        this.frames = frames;
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("w", world);
        map.put("p1", pos1);
        map.put("p2", pos2);
        map.put("s", source);
        map.put("l", localPath);
        map.put("o", owner);
        map.put("t", createdAt);
        map.put("sz", size);
        map.put("f", String.valueOf(face));
        map.put("st", strategy);
        map.put("fr", frames);
        return map;
    }

    public static PaintData deserialize(Map<String, Object> map) {
        List<Integer> p1List = (List<Integer>) map.get("p1");
        List<Integer> p2List = (List<Integer>) map.get("p2");
        List<Integer> szList = (List<Integer>) map.get("sz");

        int[] pos1 = {p1List.get(0), p1List.get(1), p1List.get(2)};
        int[] pos2 = {p2List.get(0), p2List.get(1), p2List.get(2)};
        int[] size = {szList.get(0), szList.get(1)};

        List<int[]> frames = new ArrayList<>();
        List<List<Integer>> framesList = (List<List<Integer>>) map.get("fr");
        for (List<Integer> f : framesList) {
            frames.add(new int[]{f.get(0), f.get(1), f.get(2)});
        }

        return new PaintData(
                (String) map.get("id"),
                (String) map.get("w"),
                pos1,
                pos2,
                (String) map.get("s"),
                (String) map.get("l"),
                (String) map.get("o"),
                size[0],
                size[1],
                ((String) map.get("f")).charAt(0),
                (String) map.getOrDefault("st", "preserve"),
                frames
        );
    }

    public Location getPos1() {
        World w = Bukkit.getWorld(world);
        return new Location(w, pos1[0] + 0.5, pos1[1] + 0.5, pos1[2] + 0.5);
    }

    public Location getPos2() {
        World w = Bukkit.getWorld(world);
        return new Location(w, pos2[0] + 0.5, pos2[1] + 0.5, pos2[2] + 0.5);
    }

    public BlockFace getBlockFace() {
        switch (face) {
            case 'N': return BlockFace.NORTH;
            case 'S': return BlockFace.SOUTH;
            case 'E': return BlockFace.EAST;
            case 'W': return BlockFace.WEST;
            case 'U': return BlockFace.UP;
            case 'D': return BlockFace.DOWN;
            default: return BlockFace.NORTH;
        }
    }

    // Геттеры
    public String getId() { return id; }
    public String getWorld() { return world; }
    public int[] getPos1Array() { return pos1; }
    public int[] getPos2Array() { return pos2; }
    public String getSource() { return source; }
    public String getStrategy() { return strategy; }
    public String getLocalPath() { return localPath; }
    public long getCreatedAt() { return createdAt; }
    public String getOwner() { return owner; }
    public int[] getSize() { return size; }
    public int getWidth() { return size[0]; }
    public int getHeight() { return size[1]; }
    public char getFace() { return face; }
    public List<int[]> getFrames() { return frames; }
}
