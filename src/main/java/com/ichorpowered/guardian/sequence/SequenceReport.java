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
package com.ichorpowered.guardian.sequence;

import com.ichorpowered.guardianapi.detection.report.Report;
import com.ichorpowered.guardianapi.event.origin.Origin;

import java.util.HashMap;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class SequenceReport implements Report {

    private final HashMap<String, Object> reportMap = new HashMap<>();
    private final boolean pass;
    private final Origin origin;

    public SequenceReport(boolean pass, @Nonnull Origin origin) {
        this.pass = pass;
        this.origin = origin;
    }

    public boolean isPassed() {
        return this.pass;
    }

    @Override
    public <T> void put(@Nonnull String key, @Nullable T object) {
        this.reportMap.put(key, object);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(@Nonnull String key) throws IllegalArgumentException {
        return (T) this.reportMap.get(key);
    }

    @Nonnull
    @Override
    public Set<String> keySet() {
        return this.reportMap.keySet();
    }

    @Nonnull
    @Override
    public Origin getOrigin() {
        return this.origin;
    }

}
