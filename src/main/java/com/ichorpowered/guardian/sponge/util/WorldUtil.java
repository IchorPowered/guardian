/*
 * MIT License
 *
 * Copyright (c) 2017 Connor Hartley
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.ichorpowered.guardian.sponge.util;

import com.flowpowered.math.vector.Vector3d;
import com.google.common.collect.Lists;
import com.ichorpowered.guardian.sponge.util.entity.BoundingBox;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.data.property.block.MatterProperty;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.List;
import java.util.stream.Stream;

public class WorldUtil {

    public static BoundingBox getBoundingBox(double width, double height) {
        double width2 = width / 2;

        Vector3d a1 = new Vector3d(-width2, 0, width2);
        Vector3d b2 = new Vector3d(width2, 0, -width2);

        return new BoundingBox(new BoundingBox.Bound(a1, b2), new BoundingBox.Bound(a1.add(0, height, 0), b2.add(0, height, 0)));
    }

    public static Stream<Location<World>> getIntersectingLocations(final Location<World> location, final BoundingBox.Bound bound, final double depth) {
        return Lists.newArrayList(location.sub(bound.getFirst().add(0, depth, 0)),
                location.sub(bound.getSecond().add(0, depth, 0)),
                location.sub(bound.getThird().add(0, depth, 0)),
                location.sub(bound.getFourth().add(0, depth, 0)),
                location.sub(0, depth, 0)).stream();
    }

    public static boolean isEmptyAtDepth(final Location<World> location, final BoundingBox boundingBox, final double depth) {
        if (boundingBox.getLowerBounds().isPresent()) {
            BoundingBox.Bound bound = boundingBox.getLowerBounds().get();

            return getIntersectingLocations(location, bound, depth).allMatch(intersect -> intersect.getBlock().getType().equals(BlockTypes.AIR));
        }

        return false;
    }

    public static boolean isLiquidAtDepth(final Location<World> location, final BoundingBox boundingBox, final double depth) {
        if (boundingBox.getLowerBounds().isPresent()) {
            BoundingBox.Bound bound = boundingBox.getLowerBounds().get();

            return getIntersectingLocations(location, bound, depth).allMatch(intersect -> intersect.getBlock().getType().getProperty(MatterProperty.class)
                    .map(matterProperty -> matterProperty.getValue().equals(MatterProperty.Matter.LIQUID)).orElse(false));
        }

        return false;
    }

    public static boolean anyLiquidAtDepth(final Location<World> location, final BoundingBox boundingBox, final double depth) {
        if (boundingBox.getLowerBounds().isPresent()) {
            BoundingBox.Bound bound = boundingBox.getLowerBounds().get();

            return getIntersectingLocations(location, bound, depth).anyMatch(intersect -> intersect.getBlock().getType().getProperty(MatterProperty.class)
                    .map(matterProperty -> matterProperty.getValue().equals(MatterProperty.Matter.LIQUID)).orElse(false));
        }

        return false;
    }

    public static boolean containsBlocksUnder(final Location location, final BoundingBox boundingBox, final double maxDepth) {
        double depthPortion = 0.25;

        for (int n = 0; n < location.getY(); n++) {
            double i = depthPortion * n;

            if (i >= maxDepth) break;

            if (!getBlocksAtDepth(location, boundingBox, i).isEmpty()) return true;
        }

        return false;
    }

    public static List<BlockType> getBlocksUnder(final Location location, final BoundingBox boundingBox, final double maxDepth) {
        double depthPortion = 0.25;

        for (int n = 0; n < location.getY(); n++) {
            double i = depthPortion * n;

            if (i >= maxDepth) break;

            final List<BlockType> blocks = getBlocksAtDepth(location, boundingBox, i);
            if (blocks.isEmpty()) continue;

            return blocks;
        }

        return Lists.newArrayList();
    }

    public static List<BlockType> getBlocksAtDepth(final Location<World> location, final BoundingBox boundingBox, final double depth) {
        final List<BlockType> blockTypes = Lists.newArrayList();

        if (boundingBox.getLowerBounds().isPresent()) {
            final List<BlockType> blacklistBlocks = Lists.newArrayList(BlockTypes.AIR);
            final BoundingBox.Bound bound = boundingBox.getLowerBounds().get();

            getIntersectingLocations(location, bound, depth)
                    .map(at -> at.getBlock().getType())
                    .filter(blockType -> !blacklistBlocks.contains(blockType))
                    .filter(blockType -> !blockTypes.contains(blockType))
                    .forEach(blockTypes::add);
        }

        return blockTypes;
    }

}
