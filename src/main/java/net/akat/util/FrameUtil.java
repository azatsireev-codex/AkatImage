package net.akat.util;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;

import java.util.ArrayList;
import java.util.List;

public class FrameUtil {

    /**
     * Находит все рамки в заданной области
     */
    public static List<ItemFrame> getItemFramesInArea(Location pos1, Location pos2) {
        List<ItemFrame> frames = new ArrayList<>();
        World world = pos1.getWorld();
        if (world == null) return frames;

        int minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        int maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        int minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
        int maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
        int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        int maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());

        // Определяем, какая координата фиксирована (плоскость)
        boolean fixedX = (minX == maxX);
        boolean fixedY = (minY == maxY);
        boolean fixedZ = (minZ == maxZ);

        // Если область не плоская – сразу возвращаем пустой список (или можно обработать иначе)
        if (!fixedX && !fixedY && !fixedZ) {
            return frames;
        }

        // Формируем список возможных значений для фиксированной координаты
        List<Integer> possibleFixed = new ArrayList<>();
        int fixedCoord;
        if (fixedX) {
            fixedCoord = minX;
            possibleFixed.add(fixedCoord);
            possibleFixed.add(fixedCoord - 1);
            possibleFixed.add(fixedCoord + 1);
        } else if (fixedY) {
            fixedCoord = minY;
            possibleFixed.add(fixedCoord);
            possibleFixed.add(fixedCoord - 1);
            possibleFixed.add(fixedCoord + 1);
        } else {
            fixedCoord = minZ;
            possibleFixed.add(fixedCoord);
            possibleFixed.add(fixedCoord - 1);
            possibleFixed.add(fixedCoord + 1);
        }

        // Расширяем область поиска сущностей, чтобы захватить все рамки в окрестности
        int searchMinX = minX - 1;
        int searchMaxX = maxX + 1;
        int searchMinY = minY - 1;
        int searchMaxY = maxY + 1;
        int searchMinZ = minZ - 1;
        int searchMaxZ = maxZ + 1;

        for (int chunkX = (searchMinX >> 4); chunkX <= (searchMaxX >> 4); chunkX++) {
            for (int chunkZ = (searchMinZ >> 4); chunkZ <= (searchMaxZ >> 4); chunkZ++) {
                if (!world.isChunkLoaded(chunkX, chunkZ)) {
                    world.loadChunk(chunkX, chunkZ);
                }
                Chunk chunk = world.getChunkAt(chunkX, chunkZ);
                for (Entity entity : chunk.getEntities()) {
                    if (entity instanceof ItemFrame) {
                        ItemFrame frame = (ItemFrame) entity;
                        BlockFace attached = frame.getAttachedFace();
                        if (attached == null) continue;
                        // Блок, к которому прикреплена рамка
                        Block attachedBlock = frame.getLocation().getBlock().getRelative(attached);
                        int bx = attachedBlock.getX();
                        int by = attachedBlock.getY();
                        int bz = attachedBlock.getZ();

                        // Проверяем, подходит ли блок под условие
                        boolean match = false;
                        if (fixedX) {
                            // X фиксирован – проверяем все возможные X, а Y и Z – в диапазоне
                            for (int fx : possibleFixed) {
                                if (bx == fx && by >= minY && by <= maxY && bz >= minZ && bz <= maxZ) {
                                    match = true;
                                    break;
                                }
                            }
                        } else if (fixedY) {
                            for (int fy : possibleFixed) {
                                if (by == fy && bx >= minX && bx <= maxX && bz >= minZ && bz <= maxZ) {
                                    match = true;
                                    break;
                                }
                            }
                        } else if (fixedZ) {
                            for (int fz : possibleFixed) {
                                if (bz == fz && bx >= minX && bx <= maxX && by >= minY && by <= maxY) {
                                    match = true;
                                    break;
                                }
                            }
                        }

                        if (match) {
                            frames.add(frame);
                        }
                    }
                }
            }
        }

        return frames;
    }

    /**
     * Вычисляет размеры сетки из списка рамок
     */
    public static FrameGrid calculateGrid(List<ItemFrame> frames) {
        int minX = frames.stream().mapToInt(f -> f.getLocation().getBlockX()).min().orElse(0);
        int maxX = frames.stream().mapToInt(f -> f.getLocation().getBlockX()).max().orElse(0);
        int minY = frames.stream().mapToInt(f -> f.getLocation().getBlockY()).min().orElse(0);
        int maxY = frames.stream().mapToInt(f -> f.getLocation().getBlockY()).max().orElse(0);
        int minZ = frames.stream().mapToInt(f -> f.getLocation().getBlockZ()).min().orElse(0);
        int maxZ = frames.stream().mapToInt(f -> f.getLocation().getBlockZ()).max().orElse(0);

        return new FrameGrid(minX, maxX, minY, maxY, minZ, maxZ);
    }

    /**
     * Класс для хранения данных сетки
     */
    public static class FrameGrid {
        public final int minX, maxX, minY, maxY, minZ, maxZ;
        public final int width, height;

        public FrameGrid(int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
            this.minX = minX;
            this.maxX = maxX;
            this.minY = minY;
            this.maxY = maxY;
            this.minZ = minZ;
            this.maxZ = maxZ;
            this.width = Math.max(maxX - minX + 1, maxZ - minZ + 1);
            this.height = maxY - minY + 1;
        }
    }

    /**
     * Определяет ориентацию рамок
     */
    public static FrameOrientation getOrientation(List<ItemFrame> frames) {
        if (frames.isEmpty()) return null;

        boolean allSameOrientation = true;
        boolean isVerticalWall = false;
        boolean isHorizontal = false;
        BlockFace firstFace = frames.get(0).getAttachedFace();

        for (ItemFrame frame : frames) {
            BlockFace face = frame.getAttachedFace();
            if (face == null) continue;

            boolean wall = (face == BlockFace.NORTH || face == BlockFace.SOUTH ||
                    face == BlockFace.EAST || face == BlockFace.WEST);
            boolean floor = (face == BlockFace.UP || face == BlockFace.DOWN);

            if (frame == frames.get(0)) {
                isVerticalWall = wall;
                isHorizontal = floor;
            } else {
                if (isVerticalWall && !wall) allSameOrientation = false;
                if (isHorizontal && !floor) allSameOrientation = false;
            }
        }

        if (!allSameOrientation) return null;

        char faceChar = getFaceChar(firstFace);
        return new FrameOrientation(isVerticalWall, isHorizontal, firstFace, faceChar);
    }

    /**
     * Конвертирует BlockFace в char
     */
    public static char getFaceChar(BlockFace face) {
        if (face == BlockFace.NORTH) return 'N';
        if (face == BlockFace.SOUTH) return 'S';
        if (face == BlockFace.EAST) return 'E';
        if (face == BlockFace.WEST) return 'W';
        if (face == BlockFace.UP) return 'U';
        if (face == BlockFace.DOWN) return 'D';
        return 'N';
    }

    /**
     * Конвертирует char в BlockFace
     */
    public static BlockFace getFaceFromChar(char c) {
        switch (c) {
            case 'N': return BlockFace.NORTH;
            case 'S': return BlockFace.SOUTH;
            case 'E': return BlockFace.EAST;
            case 'W': return BlockFace.WEST;
            case 'U': return BlockFace.UP;
            case 'D': return BlockFace.DOWN;
            default: return BlockFace.NORTH;
        }
    }

    /**
     * Вычисляет относительные координаты для рамки
     */
    public static RelativeCoords calculateRelativeCoords(ItemFrame frame, FrameGrid grid, boolean isVerticalWall) {
        int frameX = frame.getLocation().getBlockX();
        int frameY = frame.getLocation().getBlockY();
        int frameZ = frame.getLocation().getBlockZ();

        int relX, relY;

        if (isVerticalWall) {
            relY = grid.maxY - frameY;
            BlockFace face = frame.getAttachedFace();
            if (face == BlockFace.NORTH || face == BlockFace.SOUTH) {
                relX = frameX - grid.minX;
            } else {
                relX = frameZ - grid.minZ;
            }
        } else {
            relX = frameX - grid.minX;
            relY = grid.maxZ - frameZ;
        }

        return new RelativeCoords(relX, relY);
    }

    /**
     * Вычисляет точную позицию рамки для восстановления
     */
    public static Location calculateFrameLocation(World world, int[] pos1, int[] pos2,
                                                  char faceChar, int relX, int relY) {
        BlockFace face = getFaceFromChar(faceChar);
        int minX = Math.min(pos1[0], pos2[0]);
        int maxX = Math.max(pos1[0], pos2[0]);
        int minY = Math.min(pos1[1], pos2[1]);
        int maxY = Math.max(pos1[1], pos2[1]);
        int minZ = Math.min(pos1[2], pos2[2]);
        int maxZ = Math.max(pos1[2], pos2[2]);

        int x, y, z;

        // Инвертируем Y для правильного порядка сверху вниз
        y = maxY - relY;

        if (face == BlockFace.NORTH) {
            x = minX + relX;
            z = minZ;  // Северная сторона
        } else if (face == BlockFace.SOUTH) {
            x = minX + relX;
            z = maxZ;  // Южная сторона
        } else if (face == BlockFace.EAST) {
            x = maxX;  // Восточная сторона
            z = minZ + relX;
        } else if (face == BlockFace.WEST) {
            x = minX;  // Западная сторона
            z = minZ + relX;
        } else if (face == BlockFace.UP) {
            x = minX + relX;
            z = maxZ - relY;
            y = maxY + 1;  // На полу
        } else { // DOWN
            x = minX + relX;
            z = maxZ - relY;
            y = minY - 1;  // На потолке
        }

        return new Location(world, x + 0.5, y + 0.5, z + 0.5);
    }

    /**
     * Сортирует рамки в правильном порядке
     */
    public static void sortFrames(List<ItemFrame> frames, boolean isVerticalWall) {
        frames.sort((f1, f2) -> {
            Location loc1 = f1.getLocation();
            Location loc2 = f2.getLocation();

            int yCompare = Integer.compare(loc2.getBlockY(), loc1.getBlockY());
            if (yCompare != 0) return yCompare;

            if (isVerticalWall) {
                BlockFace face = f1.getAttachedFace();
                if (face == BlockFace.NORTH || face == BlockFace.SOUTH) {
                    return Integer.compare(loc1.getBlockX(), loc2.getBlockX());
                } else {
                    return Integer.compare(loc1.getBlockZ(), loc2.getBlockZ());
                }
            } else {
                int zCompare = Integer.compare(loc1.getBlockZ(), loc2.getBlockZ());
                if (zCompare != 0) return zCompare;
                return Integer.compare(loc1.getBlockX(), loc2.getBlockX());
            }
        });
    }

    /**
     * Класс для хранения ориентации
     */
    public static class FrameOrientation {
        public final boolean isVerticalWall;
        public final boolean isHorizontal;
        public final BlockFace face;
        public final char faceChar;

        public FrameOrientation(boolean isVerticalWall, boolean isHorizontal, BlockFace face, char faceChar) {
            this.isVerticalWall = isVerticalWall;
            this.isHorizontal = isHorizontal;
            this.face = face;
            this.faceChar = faceChar;
        }
    }

    /**
     * Класс для хранения относительных координат
     */
    public static class RelativeCoords {
        public final int relX;
        public final int relY;

        public RelativeCoords(int relX, int relY) {
            this.relX = relX;
            this.relY = relY;
        }
    }
}
