package local.simpleevents.minigame;

import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;

import java.util.Random;

/**
 * Generates a completely empty (void) world — no blocks, no terrain, no structures.
 * Register this in SimpleEventsPlugin.getDefaultWorldGenerator() so Multiverse
 * can use it via: /mv create <worldname> normal -g SimpleEvents
 */
public class VoidChunkGenerator extends ChunkGenerator {

    @Override
    public void generateSurface(WorldInfo worldInfo, Random random, int chunkX, int chunkZ, ChunkData chunkData) {
        // intentionally empty — all air
    }

    @Override public boolean shouldGenerateNoise()       { return false; }
    @Override public boolean shouldGenerateSurface()     { return false; }
    @Override public boolean shouldGenerateCaves()       { return false; }
    @Override public boolean shouldGenerateDecorations() { return false; }
    @Override public boolean shouldGenerateMobs()        { return false; }
    @Override public boolean shouldGenerateStructures()  { return false; }
}
