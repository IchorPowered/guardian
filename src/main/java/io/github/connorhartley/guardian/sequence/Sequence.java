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

import io.github.connorhartley.guardian.detection.check.Check;
import io.github.connorhartley.guardian.event.sequence.SequenceFailEvent;
import io.github.connorhartley.guardian.event.sequence.SequenceSucceedEvent;
import io.github.connorhartley.guardian.sequence.action.Action;
import io.github.connorhartley.guardian.sequence.capture.CaptureContainer;
import io.github.connorhartley.guardian.sequence.capture.CaptureContext;
import io.github.connorhartley.guardian.sequence.capture.CaptureHandler;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.Event;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.NamedCause;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Sequence
 *
 * Represents a chain of actions and capture
 * that get run in order, supplying conditions with
 * heuristic reporting.
 */
public class Sequence {

    private final Player player;
    private final Check check;
    private final CaptureHandler captureHandler;
    private final List<Action> actions = new ArrayList<>();
    private final List<Event> completeEvents = new ArrayList<>();
    private final List<Event> incompleteEvents = new ArrayList<>();

    private SequenceBlueprint sequenceBlueprint;
    private SequenceResult sequenceResult = SequenceResult.builder().build(false);
    private Location<World> initialLocation;
    private int queue = 0;
    private long last = System.currentTimeMillis();
    private boolean started = false;
    private boolean cancelled = false;
    private boolean finished = false;

    public Sequence(Player player, SequenceBlueprint sequenceBlueprint, Check check, List<Action> actions,
                    CaptureHandler captureHandler) {
        this.player = player;
        this.check = check;
        this.captureHandler = captureHandler;
        this.sequenceBlueprint = sequenceBlueprint;

        this.actions.addAll(actions);
        this.captureHandler.setContainer(new CaptureContainer());
    }

    /**
     * Check
     *
     * <p>Runs through the list of {@link Action}s and {@link CaptureContext}s and
     * carries the {@link SequenceResult} through each {@link Action} allowing it to be
     * updated through the chain. {@link CaptureContext}s follow a similar run and each tick
     * get passed a {@link CaptureContainer} to update values inside the chain.
     * {@link Action}s that fail will fire the {@link SequenceFailEvent}. {@link Action}s
     * that succeed will fire the {@link SequenceSucceedEvent}.</p>
     *
     * @param player The player in the sequence
     * @param event The event that triggered the sequence
     * @param <T> The event type
     * @return True if the sequence should continue, false if the sequence should skip
     */
    @SuppressWarnings("unchecked")
    <T extends Event> boolean check(Player player, T event) {
        Iterator<Action> iterator = this.actions.iterator();

        if (iterator.hasNext()) {
            Action action = iterator.next();

            this.queue += 1;
            long now = System.currentTimeMillis();

            action.updateReport(this.sequenceResult);

            if (!action.getEvent().isAssignableFrom(event.getClass())) {
                return fail(player, event, action, Cause.of(NamedCause.of("INVALID", this.check.getSequence())));
            }

            if (!this.started) {
                this.captureHandler.start();
                this.started = true;

                this.initialLocation = player.getLocation();
            }

            Action<T> typeAction = (Action<T>) action;

            if (this.queue > 1 && this.last + ((action.getDelay() / 20) * 1000) > now) {
                return fail(player, event, typeAction, Cause.of(NamedCause.of("DELAY", action.getDelay())));
            }

            if (this.queue > 1 && this.last + ((action.getExpire() / 20) * 1000) < now) {
                return fail(player, event, typeAction, Cause.of(NamedCause.of("EXPIRE", action.getExpire())));
            }

            action.updateCaptureContainer(this.captureHandler.getContainer());

            if (!typeAction.testConditions(player, event, this.last)) {
                return this.fail(player, event, typeAction, Cause.of(NamedCause.of("CONDITION", action.getConditions())));
            }

            this.sequenceResult = action.getSequenceResult();

            SequenceSucceedEvent attempt = new SequenceSucceedEvent(this, player, event, Cause.of(NamedCause.of("ACTION", this.sequenceResult)));
            Sponge.getEventManager().post(attempt);

            this.completeEvents.add(event);
            iterator.remove();
            typeAction.updateReport(this.sequenceResult);
            typeAction.updateCaptureContainer(this.captureHandler.getContainer());
            typeAction.succeed(player, event, this.last);
            this.sequenceResult = action.getSequenceResult();

            this.last = System.currentTimeMillis();

            if (!iterator.hasNext()) {
                if (this.started) {
                    this.captureHandler.stop();
                }
                this.finished = true;
            }
        }

        return true;
    }

    // Called when the player does not meet the requirements.
    <T extends Event> boolean fail(User user, T event, Action<T> action, Cause cause) {
        action.updateReport(this.sequenceResult);
        action.updateCaptureContainer(this.captureHandler.getContainer());

        this.cancelled = action.fail(user, event, this.last);
        this.sequenceResult = action.getSequenceResult();

        SequenceFailEvent attempt = new SequenceFailEvent(this, user, event, cause);
        Sponge.getEventManager().post(attempt);

        this.incompleteEvents.add(event);
        return false;
    }

    public Location<World> getInitialLocation() {
        return this.initialLocation;
    }

    /**
     * Get Capture Handler
     *
     * <p>Returns the {@link CaptureHandler} for this {@link Sequence}.</p>
     *
     * @return This capture handler
     */
    public CaptureHandler getCaptureHandler() {
        return this.captureHandler;
    }

    /**
     * Get Capture Container
     *
     * <p>Returns a {@link CaptureContainer} of data that have been analysed.</p>
     *
     * @return A list of capture values
     */
    public CaptureContainer getCaptureContainer() {
        return this.captureHandler.getContainer();
    }

    /**
     * Get Sequence Result
     *
     * <p>Returns the current {@link SequenceResult} for this {@link Sequence}.</p>
     *
     * @return This sequence report
     */
    public SequenceResult getSequenceResult() {
        return this.sequenceResult;
    }

    /**
     * Get Sequence Blueprint
     *
     * @return The sequence blueprint
     */
    public SequenceBlueprint getSequenceBlueprint() {
        return this.sequenceBlueprint;
    }

    /**
     * Has Started
     *
     * <p>Returns true if the {@link Sequence} has started.</p>
     *
     * @return True if the sequence has started
     */
    public boolean hasStarted() {
        return this.started;
    }

    /**
     * Has Expired
     *
     * <p>Returns true if the {@link Sequence} had an {@link Action} expire. False if
     * it has not.</p>
     *
     * @return True if the sequence has expired
     */
    boolean hasExpired() {
        if (this.actions.isEmpty()) {
            return false;
        }

        Action action = this.actions.get(0);
        long now = System.currentTimeMillis();

        if (action != null && this.last + ((action.getExpire() / 20) * 1000) < now) {
            if (this.started) {
                this.captureHandler.stop();
            }
            return true;
        }

        return false;
    }

    /**
     * Is Cancelled
     *
     * <p>Returns true if the {@link Sequence} had an {@link Action} event cancel. False
     * if it has not.</p>
     *
     * @return True if the sequence is cancelled
     */
    boolean isCancelled() {
        return this.cancelled;
    }

    /**
     * Is Finished
     *
     * <p>Returns true of the {@link Sequence} has completed all it's {@link Action}s successfully.
     * False if it has not.</p>
     *
     * @return True if the sequence is completed successfully
     */
    boolean isFinished() {
        return this.finished;
    }

    /**
     * Get User
     *
     * <p>Returns the {@link Player} in the sequence.</p>
     *
     * @return The player in the sequence
     */
    public Player getPlayer() {
        return this.player;
    }

    /**
     * Get Check
     *
     * <p>Returns the {@link Check} containing this
     * {@link Sequence}.</p>
     *
     * @return This sequences check type
     */
    public Check getCheck() {
        return this.check;
    }

    /**
     * Get Complete Events
     *
     * <p>Returns a {@link List} of {@link Event}s that have been successfully completed.</p>
     *
     * @return A list of successful events
     */
    public List<Event> getCompleteEvents() {
        return this.completeEvents;
    }

    /**
     * Get Incomplete Events
     *
     * <p>Returns a {@link List} of {@link Event}s that have failed to be completed.</p>
     *
     * @return A list of failed events
     */
    public List<Event> getIncompleteEvents() {
        return this.incompleteEvents;
    }

}
