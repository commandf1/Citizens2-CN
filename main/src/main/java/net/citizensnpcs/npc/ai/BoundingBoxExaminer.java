package net.citizensnpcs.npc.ai;

import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

import net.citizensnpcs.api.astar.pathfinder.BlockExaminer;
import net.citizensnpcs.api.astar.pathfinder.BlockSource;
import net.citizensnpcs.api.astar.pathfinder.MinecraftBlockExaminer;
import net.citizensnpcs.api.astar.pathfinder.PathPoint;
import net.citizensnpcs.api.util.BoundingBox;

public class BoundingBoxExaminer implements BlockExaminer {
    private double height;
    private double width;

    public BoundingBoxExaminer(Entity entity) {
        if (entity != null) {
            this.height = entity.getHeight();
            this.width = entity.getWidth();
        }
    }

    @Override
    public float getCost(BlockSource source, PathPoint point) {
        return 0;
    }

    @Override
    public PassableState isPassable(BlockSource source, PathPoint point) {
        Vector pos = point.getVector();
        Material up = source.getMaterialAt(pos.getBlockX(), pos.getBlockY() + 2, pos.getBlockZ());
        Material down = source.getMaterialAt(pos.getBlockX(), pos.getBlockY() - 1, pos.getBlockZ());
        if (!MinecraftBlockExaminer.canStandIn(up) && MinecraftBlockExaminer.canStandOn(down)) {
            BoundingBox above = source.getCollisionBox(pos.getBlockX(), pos.getBlockY() + 2, pos.getBlockZ());
            BoundingBox below = source.getCollisionBox(pos.getBlockX(), pos.getBlockY() - 1, pos.getBlockZ());
            float height = (float) (above.minY - below.maxY);
            if (height < this.height) {
                return PassableState.UNPASSABLE;
            }
        }
        return PassableState.IGNORE;
    }
}
