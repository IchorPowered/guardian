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
package io.github.connorhartley.guardian.internal.detections;

import com.google.common.reflect.TypeToken;
import com.google.inject.Inject;
import com.me4502.modularframework.module.Module;
import com.me4502.modularframework.module.guice.ModuleConfiguration;
import com.me4502.modularframework.module.guice.ModuleContainer;
import com.me4502.precogs.detection.CommonDetectionTypes;
import io.github.connorhartley.guardian.Guardian;
import io.github.connorhartley.guardian.context.ContextProvider;
import io.github.connorhartley.guardian.detection.Detection;
import io.github.connorhartley.guardian.detection.DetectionConfiguration;
import io.github.connorhartley.guardian.detection.DetectionTypes;
import io.github.connorhartley.guardian.detection.check.CheckType;
import io.github.connorhartley.guardian.event.sequence.SequenceFinishEvent;
import io.github.connorhartley.guardian.internal.checks.RelationalFlyCheck;
import io.github.connorhartley.guardian.internal.punishments.WarnPunishment;
import io.github.connorhartley.guardian.punishment.Punishment;
import io.github.connorhartley.guardian.sequence.report.ReportType;
import io.github.connorhartley.guardian.storage.StorageConsumer;
import io.github.connorhartley.guardian.storage.container.StorageKey;
import io.github.connorhartley.guardian.storage.container.StorageValue;
import ninja.leaping.configurate.ConfigurationNode;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.plugin.PluginContainer;

import java.io.File;
import java.time.LocalDateTime;
import java.util.*;

@Module(
        id = "fly",
        name = "Fly Detection",
        authors = { "Connor Hartley (vectrix)" },
        version = "0.0.3",
        onEnable = "onConstruction",
        onDisable = "onDeconstruction"
)
public class FlyDetection extends Detection<Guardian> implements StorageConsumer {

    private static Module moduleAnnotation = FlyDetection.class.getAnnotation(Module.class);

    private Guardian plugin;
    private List<CheckType> checkTypes;
    private Configuration configuration;
    private boolean ready = false;

    @Inject
    @ModuleContainer
    public PluginContainer moduleContainer;

    @Inject
    @ModuleConfiguration
    public ConfigurationNode configurationNode;

    public FlyDetection() {
        super(moduleAnnotation.id(), moduleAnnotation.name());
    }

    @Override
    public void onConstruction() {
        this.moduleContainer.getInstance().ifPresent(plugin -> this.plugin = (Guardian) plugin);

        this.configuration = new Configuration(this);

        if (Configuration.getLocation().exists()) {
            for (StorageValue storageValue : this.getStorageNodes()) {
                storageValue.<ConfigurationNode>loadStorage(this.configurationNode);
            }
        }

        DetectionTypes.FLY_DETECTION = Optional.of(this);

        this.checkTypes = Collections.singletonList(new RelationalFlyCheck.Type(this));

        this.plugin.getPunishmentController().bind(WarnPunishment.class, this);

        this.ready = true;
    }

    @Listener
    public void onSequenceFinish(SequenceFinishEvent event) {
        if (!event.isCancelled()) {
            for (CheckType checkProvider : this.checkTypes) {
                if (checkProvider.getSequence().equals(event.getSequence())) {
                    double lower = this.configuration.configSeverityDistribution.getValue().get("lower");
                    double mean = this.configuration.configSeverityDistribution.getValue().get("mean");
                    double standardDeviation = this.configuration.configSeverityDistribution.getValue().get("standard-deviation");

                    NormalDistribution normalDistribution =
                            new NormalDistribution(mean, standardDeviation);

                    if (event.getResult().getReports().get(ReportType.SEVERITY) != null) {
                        double probability = normalDistribution.probability(lower, (Double) event.getResult()
                                .getReports().get(ReportType.SEVERITY));

                        Punishment punishment = Punishment.builder()
                                .time(LocalDateTime.now())
                                .report(event.getResult())
                                .probability(probability)
                                .build();

                        this.getPlugin().getPunishmentController().execute(this, event.getUser(), punishment);
                    }
                }
            }
        }
    }

    @Override
    public void onDeconstruction() {
        this.ready = false;
    }

    @Override
    public CommonDetectionTypes.Category getCategory() {
        return CommonDetectionTypes.Category.MOVEMENT;
    }

    @Override
    public Guardian getPlugin() {
        return this.plugin;
    }

    @Override
    public List<CheckType> getChecks() {
        return this.checkTypes;
    }

    @Override
    public DetectionConfiguration getConfiguration() {
        return this.configuration;
    }

    @Override
    public ContextProvider getContextProvider() {
        return this.plugin;
    }

    @Override
    public StorageValue<?, ?>[] getStorageNodes() {
        return new StorageValue<?, ?>[] {
                this.configuration.configPunishmentProperties, this.configuration.configPunishmentLevels,
                this.configuration.configSeverityDistribution, this.configuration.configAnalysisTime,
                this.configuration.configTickBounds, this.configuration.configDistanceAmplitude
        };
    }

    @Override
    public boolean isReady() {
        return this.ready;
    }

    public static class Configuration implements DetectionConfiguration {

        private static File configFile;

        private final FlyDetection flyDetection;

        StorageValue<String, Double> configAnalysisTime;
        StorageValue<String, Map<String, Double>> configTickBounds;
        StorageValue<String, Map<String, Double>> configDistanceAmplitude;
        StorageValue<String, Map<String, Double>> configPunishmentLevels;
        StorageValue<String, Map<String, String>> configPunishmentProperties;
        StorageValue<String, Map<String, Double>> configSeverityDistribution;

        private Configuration(FlyDetection flyDetection) {
            this.flyDetection = flyDetection;

            configFile = new File(this.flyDetection.getPlugin().getGlobalConfiguration()
                    .getLocation().getParentFile(), "detection" + File.separator +
                    this.flyDetection.getId() + ".conf");

            initialize();
        }

        private void initialize() {
            this.configAnalysisTime = new StorageValue<>(new StorageKey<>("analysis-time"),
                    "Time taken to analyse the players air time. 2 seconds is recommended!",
                    2.0, new TypeToken<Double>() {
            });

            HashMap<String, Double> tickBounds = new HashMap<>();
            tickBounds.put("min", 0.75);
            tickBounds.put("max", 1.5);

            this.configTickBounds = new StorageValue<>(new StorageKey<>("tick-bounds"),
                    "Percentage of the analysis-time in ticks to compare the check time to ensure accurate reports.",
                    tickBounds, new TypeToken<Map<String, Double>>() {
            });

            HashMap<String, Double> punishmentLevels = new HashMap<>();
            punishmentLevels.put("warn", 0.1);
//            punishmentLevels.put("flag", 0.2);
//            punishmentLevels.put("report", 0.3);
//            punishmentLevels.put("kick", 0.5);

            this.configPunishmentLevels = new StorageValue<>(new StorageKey<>("punishment-levels"),
                    "Punishments that happen when the user reaches the individual severity threshold.",
                    punishmentLevels, new TypeToken<Map<String, Double>>() {
            });

            HashMap<String, String> punishmentProperties = new HashMap<>();
            punishmentProperties.put("channel", "admin");
            punishmentProperties.put("releasetime", "12096000");

            this.configPunishmentProperties = new StorageValue<>(new StorageKey<>("punishment-properties"),
                    "Properties that define certain properties for all the punishments in this detection.",
                    punishmentProperties, new TypeToken<Map<String, String>>() {
            });

            HashMap<String, Double> severityDistribution = new HashMap<>();
            severityDistribution.put("lower", 0d);
            severityDistribution.put("mean", 2.5d);
            severityDistribution.put("standard-deviation", 1.5d);

            this.configSeverityDistribution = new StorageValue<>(new StorageKey<>("severity-distribution"),
                    "Normal distribution properties for calculating the over-shot value from the mean.",
                    severityDistribution, new TypeToken<Map<String, Double>>() {
            });

            HashMap<String, Double> distanceAmplitude = new HashMap<>();
            distanceAmplitude.put("minor", 1.4);
            distanceAmplitude.put("mean", 2.2);
            distanceAmplitude.put("major", 3.0);

            this.configDistanceAmplitude = new StorageValue<>(new StorageKey<>("distance-amplitude"),
                    "Amplitude of distance that can be made between each tick, to ensure legal vertical movement.",
                    distanceAmplitude, new TypeToken<Map<String, Double>>() {
            });
        }

        private static File getLocation() {
            return configFile;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <K, E> Optional<StorageValue<K, E>> get(K name, E defaultElement) {
            if (name instanceof String) {
                if (name.equals("analysis-time")) {
                    return Optional.of((StorageValue<K, E>) this.configAnalysisTime);
                } else if (name.equals("tick-bounds")) {
                    return Optional.of((StorageValue<K, E>) this.configTickBounds);
                } else if (name.equals("distance-amplitude")) {
                    return Optional.of((StorageValue<K, E>) this.configDistanceAmplitude);
                } else if (name.equals("punishment-properties")) {
                    return Optional.of((StorageValue<K, E>) this.configPunishmentProperties);
                } else if (name.equals("punishment-levels")) {
                    return Optional.of((StorageValue<K, E>) this.configPunishmentLevels);
                } else if (name.equals("severity-distribution")) {
                    return Optional.of((StorageValue<K, E>) this.configSeverityDistribution);
                }
            }
            return Optional.empty();
        }
    }

}
