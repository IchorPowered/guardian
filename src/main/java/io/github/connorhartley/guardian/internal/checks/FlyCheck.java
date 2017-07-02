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
package io.github.connorhartley.guardian.internal.checks;

import io.github.connorhartley.guardian.Guardian;
import io.github.connorhartley.guardian.GuardianConfiguration;
import io.github.connorhartley.guardian.detection.Detection;
import io.github.connorhartley.guardian.detection.check.Check;
import io.github.connorhartley.guardian.internal.contexts.player.PlayerLocationContext;
import io.github.connorhartley.guardian.internal.contexts.player.PlayerPositionContext;
import io.github.connorhartley.guardian.internal.contexts.world.MaterialSpeedContext;
import io.github.connorhartley.guardian.sequence.SequenceBlueprint;
import io.github.connorhartley.guardian.sequence.SequenceBuilder;
import io.github.connorhartley.guardian.sequence.SequenceResult;
import io.github.connorhartley.guardian.sequence.condition.ConditionResult;
import io.github.connorhartley.guardian.storage.StorageProvider;
import io.github.connorhartley.guardian.util.check.CommonMovementConditions;
import io.github.connorhartley.guardian.util.check.PermissionCheckCondition;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.event.entity.MoveEntityEvent;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import tech.ferus.util.config.HoconConfigFile;

import java.nio.file.Path;

public class FlyCheck<E, F extends StorageProvider<HoconConfigFile, Path>> implements Check<E, F> {

    private final Detection<E, F> detection;

    private double analysisTime = 40;
    private double minimumAirTime = 1.35;
    private double minimumTickRange = 30;
    private double maximumTickRange = 50;


    public FlyCheck(Detection<E, F> detection) {
        this.detection = detection;

        this.analysisTime = this.detection.getConfiguration().getStorage().getNode("analysis", "sequence-time").getDouble(2d) / 0.05;

        this.minimumAirTime = this.detection.getConfiguration().getStorage().getNode("analysis", "minimum-air-time").getDouble();

        this.minimumTickRange = this.analysisTime * GuardianConfiguration.GLOBAL_TICK_MIN.get(this.detection.getConfiguration().getStorage(), 0.75);

        this.maximumTickRange = this.analysisTime * GuardianConfiguration.GLOBAL_TICK_MAX.get(this.detection.getConfiguration().getStorage(), 1.25);
    }

    @Override
    public Detection<E, F> getDetection() {
        return this.detection;
    }

    @Override
    public SequenceBlueprint getSequence() {
        return new SequenceBuilder<E, F>()

                .capture(
                        new PlayerLocationContext<>((Guardian) this.getDetection().getPlugin(), this.getDetection()),
                        new PlayerPositionContext.Altitude<>((Guardian) this.getDetection().getPlugin(), this.getDetection()),
                        new MaterialSpeedContext<>((Guardian) this.getDetection().getPlugin(), this.getDetection())
                )

                // Trigger : Move Entity Event

                .action(MoveEntityEvent.class)

                // After 2 Seconds : Move Entity Event

                .action(MoveEntityEvent.class)
                        .delay(((Double) this.analysisTime).intValue())
                        .expire(((Double) this.maximumTickRange).intValue())

                        /*
                         * Cancels the sequence if the player being tracked, dies, teleports,
                         * teleports through Nucleus and mounts or dismounts a vehicle. This
                         * is due to the location comparison at the beginning and end of a check
                         * which these events change the behaviour of.
                         */
                        .failure(new CommonMovementConditions.DeathCondition(this.detection))
                        .failure(new CommonMovementConditions.NucleusTeleportCondition(this.detection))
                        .failure(new CommonMovementConditions.VehicleMountCondition(this.detection))
                        .condition(new CommonMovementConditions.TeleportCondition(this.detection))

                        // Does the player have permission?
                        .condition(new PermissionCheckCondition(this.detection))

                        .condition((user, event, contextValuation, sequenceReport, lastAction) -> {
                            SequenceResult.Builder report = SequenceResult.builder().of(sequenceReport);

                            Guardian plugin = (Guardian) this.detection.getPlugin();

                            Location<World> start = null;
                            Location<World> present = null;

                            long currentTime;
                            long playerAltitudeGainTicks = 0;
                            int materialUpdateTicks = 0;

                            int materialGas = 0;
                            double playerAltitudeGain = 0;

                            if (contextValuation.<PlayerLocationContext, Location<World>>get(PlayerLocationContext.class, "start_location").isPresent()) {
                                start = contextValuation.<PlayerLocationContext, Location<World>>get(PlayerLocationContext.class, "start_location").get();
                            }

                            if (contextValuation.<PlayerLocationContext, Location<World>>get(PlayerLocationContext.class, "present_location").isPresent()) {
                                present = contextValuation.<PlayerLocationContext, Location<World>>get(PlayerLocationContext.class, "present_location").get();
                            }

                            if (contextValuation.<PlayerPositionContext.Altitude, Integer>get(PlayerPositionContext.Altitude.class, "update").isPresent()) {
                                playerAltitudeGainTicks = contextValuation.<PlayerPositionContext.Altitude, Integer>get(PlayerPositionContext.Altitude.class, "update").get();
                            }

                            if (contextValuation.<PlayerPositionContext.Altitude, Double>get(PlayerPositionContext.Altitude.class, "position_altitude").isPresent()) {
                                playerAltitudeGain = contextValuation.<PlayerPositionContext.Altitude, Double>get(PlayerPositionContext.Altitude.class, "position_altitude").get();
                            }

                            if (contextValuation.<MaterialSpeedContext, Integer>get(MaterialSpeedContext.class, "amplifier_material_gas").isPresent()) {
                                materialGas = contextValuation.<MaterialSpeedContext, Integer>get(MaterialSpeedContext.class, "amplifier_material_gas").get();
                            }

                            if (contextValuation.<MaterialSpeedContext, Integer>get(MaterialSpeedContext.class, "update").isPresent()) {
                                materialUpdateTicks = contextValuation.<MaterialSpeedContext, Integer>get(MaterialSpeedContext.class, "update").get();
                            }

                            if (playerAltitudeGainTicks < this.minimumTickRange) {
                                plugin.getLogger().warn("The server may be overloaded. A detection check has been skipped as it is less than a second and a half behind.");
                                return new ConditionResult(false, report.build(false));
                            } else if (playerAltitudeGainTicks > this.maximumTickRange) {
                                return new ConditionResult(false, report.build(false));
                            }

                            if (user.getPlayer().isPresent() && start != null && present != null) {

                                currentTime = System.currentTimeMillis();

                                if (user.getPlayer().get().get(Keys.VEHICLE).isPresent()) {
                                    return new ConditionResult(false, report.build(false));
                                }

                                if (user.getPlayer().get().get(Keys.CAN_FLY).isPresent()) {
                                    if (user.getPlayer().get().get(Keys.CAN_FLY).get()) {
                                        return new ConditionResult(false, report.build(false));
                                    }
                                }

                                if (user.getPlayer().get().getLocation().getY() < -1.25 || !user.getPlayer().get().isLoaded()) {
                                    return new ConditionResult(false, report.build(false));
                                }

                                double altitudeDisplacement = ((present.getY() - start.getY()) == 0) ? 3.1 : present.getY() - start.getY();

                                double airTime = materialGas * (((
                                        ((1 / ((playerAltitudeGainTicks + materialUpdateTicks) / 2)) *
                                                ((long) this.analysisTime * 1000)) + (currentTime - lastAction)) / 2) / 1000) * 0.05;

                                double meanAltitude = playerAltitudeGain / ((
                                        ((playerAltitudeGainTicks + this.analysisTime) / 2) +
                                                ((currentTime - lastAction) / 1000)) / 2);

                                if (altitudeDisplacement <= 1 || meanAltitude <= 1 || airTime < this.minimumAirTime
                                        || !user.getPlayer().get().getLocation().sub(0, -1, 0).getBlock().getType().equals(BlockTypes.AIR)
                                        || !user.getPlayer().get().getLocation().sub(0, -2, 0).getBlock().getType().equals(BlockTypes.AIR)) {
                                    return new ConditionResult(false, report.build(false));
                                }

                                if (((altitudeDisplacement / meanAltitude) + meanAltitude) > (3.1 *
                                        (this.analysisTime * 0.05))) {
                                    report
                                            .information("Result of altitude gain was " + ((altitudeDisplacement / meanAltitude) + meanAltitude) + ".")
                                            .type("flying")
                                            .initialLocation(start.copy())
                                            .severity(((altitudeDisplacement / meanAltitude) + meanAltitude));

                                    // TODO : Remove this after testing \/
                                    plugin.getLogger().warn(user.getName() + " has triggered the flight check and overshot " +
                                            "the maximum altitude gain by " + ((altitudeDisplacement / meanAltitude) + meanAltitude) + ".");

                                    return new ConditionResult(true, report.build(true));
                                }
                            }
                            return new ConditionResult(false, sequenceReport);
                        })

                .build(this);
    }
}
