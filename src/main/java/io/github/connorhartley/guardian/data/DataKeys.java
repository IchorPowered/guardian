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
package io.github.connorhartley.guardian.data;

import com.google.common.reflect.TypeToken;
import io.github.connorhartley.guardian.detection.Offense;
import io.github.connorhartley.guardian.punishment.PunishmentType;
import org.spongepowered.api.data.key.Key;
import org.spongepowered.api.data.key.KeyFactory;
import org.spongepowered.api.data.value.mutable.ListValue;
import org.spongepowered.api.data.value.mutable.Value;

import java.util.List;

import static org.spongepowered.api.data.DataQuery.of;

public class DataKeys {

    /* Tags : Attached to the player to represent some "type" data. */

    public static Key<Value<Offense>> GUARDIAN_OFFENSE_TAG = KeyFactory.makeSingleKey(
            TypeToken.of(Offense.class),
            new TypeToken<Value<Offense>>() {
            },
            of("GuardianOffenseTag"), "guardian:offensetag", "GuardianOffenseTag"
    );

    public static Key<ListValue<PunishmentType>> GUARDIAN_PUNISHMENT_TAG = KeyFactory.makeListKey(
            new TypeToken<List<PunishmentType>>() {
            },
            new TypeToken<ListValue<PunishmentType>>() {
            },
            of("GuardianPunishmentTag"), "guardian:punishmenttag", "GuardianPunishmentTag"
    );

}
