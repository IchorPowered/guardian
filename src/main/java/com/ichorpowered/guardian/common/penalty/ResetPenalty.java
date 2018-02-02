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
package com.ichorpowered.guardian.common.penalty;

import com.google.common.collect.Sets;
import com.ichorpowered.guardian.sequence.SequenceReport;
import com.ichorpowered.guardianapi.detection.penalty.Penalty;
import com.ichorpowered.guardianapi.util.StagePredicate;
import org.spongepowered.api.Game;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.TeleportHelper;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.teleport.TeleportHelperFilters;

import java.util.Set;

import javax.annotation.Nonnull;

public class ResetPenalty implements Penalty {

    @Nonnull
    @Override
    public String getId() {
        return "guardian:reset";
    }

    @Override
    public String getName() {
        return "Reset Penalty";
    }

    @Override
    public Set<String> getTags() {
        return Sets.newHashSet(
                "guardian",
                "internal",
                "aggressivepenalty",
                "reset"
        );
    }

    @Override
    public StagePredicate getPredicate() {
        return (detection, summary, playerEntry) -> {
            if (!playerEntry.getEntity(Player.class).isPresent()) return false;
            final Player player = playerEntry.getEntity(Player.class).get();

            if (summary.view(SequenceReport.class) == null ||
                    summary.view(SequenceReport.class).get("initial_location") == null) return false;

            final Location<World> location = summary.view(SequenceReport.class).get("initial_location");

            return Sponge.getGame().getTeleportHelper().getSafeLocation(location, location.getBlockY(), 8)
                    .map(player::setLocation)
                    .orElse(false);
        };
    }
}
