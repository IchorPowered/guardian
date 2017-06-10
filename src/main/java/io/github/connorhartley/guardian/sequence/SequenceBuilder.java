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
import io.github.connorhartley.guardian.sequence.action.Action;
import io.github.connorhartley.guardian.sequence.action.ActionBlueprint;
import io.github.connorhartley.guardian.sequence.action.ActionBuilder;
import io.github.connorhartley.guardian.sequence.capture.CaptureContext;
import io.github.connorhartley.guardian.sequence.capture.CaptureHandler;
import io.github.connorhartley.guardian.storage.StorageSupplier;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Event;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Sequence Builder
 *
 * A {@link SequenceBlueprint} builder giving the ability to chain
 * actions for a {@link Sequence}.
 */
public class SequenceBuilder<E, F extends StorageSupplier<File>> {

    private CaptureContext<E, F>[] captureContexts;
    private List<Action> actions = new ArrayList<>();

    @SafeVarargs
    public final SequenceBuilder capture(CaptureContext<E, F>... captureContexts) {
        this.captureContexts = captureContexts;
        return this;
    }

    public <T extends Event> ActionBuilder<T> action(Class<T> clazz) {
        return action(new Action<>(clazz));
    }

    public <T extends Event> ActionBuilder<T> action(ActionBlueprint<T> builder) {
        return action(builder.create());
    }

    public <T extends Event> ActionBuilder<T> action(Action<T> action) {
        this.actions.add(action);

        return new ActionBuilder<>(this, action);
    }

    public SequenceBlueprint build(Check check) {
        return new SequenceBlueprint(check) {
            @Override
            public Sequence create(Player player) {
                return new Sequence(player, this, check, actions, new CaptureHandler<>(player, captureContexts));
            }
        };
    }

}
