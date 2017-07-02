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
package io.github.connorhartley.guardian.sequence;

import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Sequence Result
 *
 * Represents a report containing an analysis from a {@link Sequence}.
 */
public class SequenceResult {

    private final String detectionType;
    private final List<String> information = new ArrayList<>();
    private final Location<World> initialLocation;
    private final double severity;
    private final boolean accepted;

    public SequenceResult(Builder builder) {
        this.detectionType = builder.detectionType;
        this.information.addAll(builder.information);
        this.initialLocation = builder.initialLocation;
        this.severity = builder.severity;
        this.accepted = builder.accepted;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Get Detection Type
     *
     * <p>Returns a string of the detection type that was caught.</p>
     *
     * @return The detection type
     */
    public String getDetectionType() {
        return this.detectionType;
    }

    /**
     * Get Information
     *
     * <p>Returns a list of information that was added regarding
     * detection that were caught.</p>
     *
     * @return A list of report information
     */
    public List<String> getInformation() {
        return this.information;
    }

    /**
     * Get Severity
     *
     * <p>Returns the severity of this report.</p>
     *
     * @return Severity of the report
     */
    public double getSeverity() {
        return this.severity;
    }

    public Optional<Location<World>> getInitialLocation() {
        return Optional.ofNullable(this.initialLocation);
    }

    /**
     * Is Accepted
     *
     * <p>Returns true if the report was accepted by the
     * checks reporter and false if it was not.</p>
     *
     * @return True if accepted by the check reporter
     */
    public boolean isAccepted() {
        return this.accepted;
    }

    /**
     * Copy
     *
     * <p>Returns a copy of this sequence report.</p>
     *
     * @return A copy of this
     */
    public SequenceResult copy() {
        return new SequenceResult.Builder().of(this).build(this.accepted);
    }

    public static class Builder {

        private String detectionType;
        private List<String> information = new ArrayList<>();
        private Location<World> initialLocation;
        private double severity = 0.0;
        private boolean accepted = false;

        public Builder() {}

        public Builder of(SequenceResult sequenceResult) {
            this.detectionType = sequenceResult.detectionType;
            this.information = sequenceResult.information;
            this.severity = sequenceResult.severity;
            this.accepted = sequenceResult.accepted;

            if (sequenceResult.initialLocation != null) {
                this.initialLocation = sequenceResult.initialLocation.copy();
            }

            return this;
        }

        public Builder type(String detectionType) {
            this.detectionType = detectionType;
            return this;
        }

        public Builder information(String information) {
            this.information.add(information);
            return this;
        }

        public Builder severity(double severity) {
            this.severity = severity;
            return this;
        }

        public Builder initialLocation(Location<World> location) {
            this.initialLocation = location.copy();
            return this;
        }

        public SequenceResult build(boolean accept) {
            this.accepted = accept;
            return new SequenceResult(this);
        }

    }

}
