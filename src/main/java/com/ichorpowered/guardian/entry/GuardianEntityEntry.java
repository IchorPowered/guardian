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
package com.ichorpowered.guardian.entry;

import com.google.common.reflect.TypeToken;
import com.ichorpowered.guardian.content.AbstractContentContainer;
import com.ichorpowered.guardianapi.content.transaction.ContentKey;
import com.ichorpowered.guardianapi.entry.entity.PlayerEntry;
import net.kyori.lunar.reflect.Reified;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nonnull;

public class GuardianEntityEntry<T> extends AbstractContentContainer implements PlayerEntry, Reified<T> {

    private final T entity;
    private final UUID uuid;

    public static <E> GuardianEntityEntry<E> of(@Nonnull E entity, @Nonnull UUID uuid) {
        return new GuardianEntityEntry<>(entity, uuid);
    }

    private GuardianEntityEntry(@Nonnull T entity, @Nonnull UUID uuid) {
        this.entity = entity;
        this.uuid = uuid;
    }

    @Nonnull
    @Override
    public UUID getUniqueId() {
        return this.uuid;
    }

    @Nonnull
    @Override
    public <E> Optional<E> getEntity(@Nonnull Class<E> clazz) {
        if (!clazz.isAssignableFrom(this.entity.getClass())) return Optional.empty();
        return Optional.ofNullable((E) this.entity);
    }

    @Nonnull
    @Override
    public TypeToken<T> type() {
        return TypeToken.of((Class<T>) this.entity.getClass());
    }

    @Override
    public Set<ContentKey> getPossibleKeys() {
        return null;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.entity, this.uuid);
    }

    @Override
    public boolean equals(final Object object) {
        if (this == object) return true;
        if (object == null || !(object instanceof PlayerEntry)) return false;
        return Objects.equals(this.entity, ((PlayerEntry) object).getEntity(this.entity.getClass()))
                && Objects.equals(this.uuid, ((PlayerEntry) object).getUniqueId());
    }
}
