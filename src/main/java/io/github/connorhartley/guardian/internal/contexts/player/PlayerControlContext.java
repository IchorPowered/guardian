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
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.Player;

import java.util.HashMap;
import java.util.Map;

public class PlayerControlContext {

    public static class VerticalSpeed extends Context {

        private Guardian plugin;
        private Detection detection;
        private Player player;
        private ContextContainer contextContainer;

        private double flySpeedControl = 1.065;

        private long updateAmount = 0;
        private boolean suspended = false;

        public VerticalSpeed(Guardian plugin, Detection detection, Player player) {
            super(plugin, detection, player);
            this.plugin = plugin;
            this.detection = detection;
            this.player = player;
            this.contextContainer = new ContextContainer(this);

            if (this.detection.getConfiguration().get("control-values", new HashMap<String, Double>()).isPresent()) {
                Map<String, Double> storageValueMap = this.detection.getConfiguration().get("control-values",
                        new HashMap<String, Double>()).get().getValue();


                this.flySpeedControl = storageValueMap.get("fly");
            }

            this.contextContainer.set(ContextTypes.VERTICAL_CONTROL_SPEED);
        }

        @Override
        public void update() {
            if (this.player.get(Keys.IS_FLYING).isPresent()) {
                if (this.player.get(Keys.IS_FLYING).get()) {
                    this.contextContainer.transform(ContextTypes.VERTICAL_CONTROL_SPEED, oldValue -> oldValue * this.flySpeedControl);
                }
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

    public static class HorizontalSpeed extends Context {

        private Guardian plugin;
        private Detection detection;
        private Player player;
        private ContextContainer contextContainer;

        private double sneakSpeedControl = 1.015;
        private double walkSpeedControl = 1.035;
        private double sprintSpeedControl = 1.065;
        private double flySpeedControl = 1.065;

        private long updateAmount = 0;
        private boolean suspended = false;

        public HorizontalSpeed(Guardian plugin, Detection detection, Player player) {
            super(plugin, detection, player);
            this.plugin = plugin;
            this.detection = detection;
            this.player = player;
            this.contextContainer = new ContextContainer(this);

            if (this.detection.getConfiguration().get("control-values", new HashMap<String, Double>()).isPresent()) {
                Map<String, Double> storageValueMap = this.detection.getConfiguration().get("control-values",
                        new HashMap<String, Double>()).get().getValue();

                this.sneakSpeedControl = storageValueMap.get("sneak");
                this.walkSpeedControl = storageValueMap.get("walk");
                this.sprintSpeedControl = storageValueMap.get("sprint");
                this.flySpeedControl = storageValueMap.get("fly");
            }

            this.contextContainer.set(ContextTypes.HORIZONTAL_CONTROL_SPEED);
            this.contextContainer.set(ContextTypes.CONTROL_SPEED_STATE);
        }

        @Override
        public void update() {
            if (this.player.get(Keys.IS_SPRINTING).isPresent() && this.player.get(Keys.IS_SNEAKING).isPresent() &&
                    this.player.get(Keys.IS_FLYING).isPresent()) {
                if (this.player.get(Keys.IS_FLYING).get()) {
                    this.contextContainer.transform(ContextTypes.HORIZONTAL_CONTROL_SPEED, oldValue -> oldValue * this.flySpeedControl);
                    this.contextContainer.set(ContextTypes.CONTROL_SPEED_STATE, State.FLYING);
                } else if (this.player.get(Keys.IS_SPRINTING).get()) {
                    this.contextContainer.transform(ContextTypes.HORIZONTAL_CONTROL_SPEED, oldValue -> oldValue * this.sprintSpeedControl);
                    this.contextContainer.set(ContextTypes.CONTROL_SPEED_STATE, State.SPRINTING);
                } else if (this.player.get(Keys.IS_SNEAKING).get()) {
                    this.contextContainer.transform(ContextTypes.HORIZONTAL_CONTROL_SPEED, oldValue -> oldValue * this.sneakSpeedControl);
                    this.contextContainer.set(ContextTypes.CONTROL_SPEED_STATE, State.SNEAKING);
                } else {
                    this.contextContainer.transform(ContextTypes.HORIZONTAL_CONTROL_SPEED, oldValue -> oldValue * this.walkSpeedControl);
                    this.contextContainer.set(ContextTypes.CONTROL_SPEED_STATE, State.WALKING);
                }
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

        public enum State {
            SNEAKING,
            WALKING,
            FLYING,
            SPRINTING
        }
    }

}
