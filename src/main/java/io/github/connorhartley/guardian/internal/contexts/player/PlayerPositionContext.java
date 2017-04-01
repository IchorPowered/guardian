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
package io.github.connorhartley.guardian.internal.contexts.player;

import io.github.connorhartley.guardian.Guardian;
import io.github.connorhartley.guardian.context.Context;
import io.github.connorhartley.guardian.context.ContextTypes;
import io.github.connorhartley.guardian.context.container.ContextContainer;
import io.github.connorhartley.guardian.detection.Detection;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.HashMap;
import java.util.Map;

public class PlayerPositionContext {

    public static class Altitude extends Context {

        private Guardian plugin;
        private Detection detection;
        private Player player;
        private ContextContainer contextContainer;

        private Location<World> depthThreshold;

        private long updateAmount = 0;
        private boolean suspended = false;

        public Altitude(Guardian plugin, Detection detection, Player player) {
            super(plugin, detection, player);
            this.plugin = plugin;
            this.detection = detection;
            this.player = player;
            this.contextContainer = new ContextContainer(this);

            this.contextContainer.set(ContextTypes.GAINED_ALTITUDE);
        }

        @Override
        public void update() {
            Location<World> playerAltitude = null;
            double blockDepth = 0;

            for (int i = 0; i < this.player.getLocation().getY(); i++) {
                if (!this.player.getLocation().sub(0, i, 0).getBlockType().equals(BlockTypes.AIR)) {
                    Location<World> currentDepth = this.player.getLocation().sub(0, i, 0);
                    if (this.depthThreshold != null && this.depthThreshold.getY() == currentDepth.getY()) {
                        playerAltitude = currentDepth.add(0, 1, 0);
                        blockDepth = 1;
                    } else if (this.depthThreshold != null && this.depthThreshold.getY() < currentDepth.getY()) {
                        playerAltitude = currentDepth.add(0, 1, 0);
                        blockDepth = currentDepth.getY() - this.depthThreshold.getY();
                    } else if (this.depthThreshold != null && this.depthThreshold.getY() > currentDepth.getY()) {
                        playerAltitude = currentDepth.add(0, 1, 0);
                        blockDepth = this.depthThreshold.getY() - currentDepth.getY();
                    } else if (this.depthThreshold == null) {
                        this.depthThreshold = currentDepth;
                        playerAltitude = currentDepth.add(0, 1, 0);
                    }
                }
            }

            if (this.depthThreshold == null || playerAltitude == null) {
                playerAltitude = new Location<>(this.player.getWorld(), this.player.getLocation().getX(), 0, this.player.getLocation().getZ());
            }

            double altitude = (this.player.getLocation().getY() - playerAltitude.getY()) - blockDepth;

            if (altitude < 0) {
                this.contextContainer.transform(ContextTypes.GAINED_ALTITUDE, oldValue -> oldValue);
            } else {
                this.contextContainer.transform(ContextTypes.GAINED_ALTITUDE, oldValue -> oldValue + altitude);
            }

            this.updateAmount += 1;
        }

        @Override
        public void suspend() {
            this.suspended = true;
        }

        @Override
        public boolean isSuspended() {
            return this.suspended;
        }

        @Override
        public long updateAmount() {
            return this.updateAmount;
        }

        @Override
        public ContextContainer getContainer() {
            return this.contextContainer;
        }
    }
}
