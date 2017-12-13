package io.ichorpowered.guardian.util.tuple;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A quartet of elements.
 *
 * @param <A> the first element
 * @param <B> the second element
 * @param <C> the third element
 * @param <D> the fourth element
 */
public class Quartet<A, B, C, D> {

    /**
     * Creates a new {@link Quartet} with the desired {@code first},
     * {@code second}, {@code third} and {@code fourth} elements.
     *
     * @param first the first element
     * @param second the second element
     * @param third the third element
     * @param fourth the fourth element
     * @param <Z> the type of the first element
     * @param <Y> the type of the second element
     * @param <X> the type of the third element
     * @param <W> the type of the fourth element
     * @return the new quartet
     */
    public static <Z, Y, X, W> Quartet<Z, Y, X, W> of(final Z first, final Y second,
                                                      final X third, final W fourth) {
        return new Quartet<>(first, second, third, fourth);
    }

    private final A first;
    private final B second;
    private final C third;
    private final D fourth;

    /**
     * Creates a new {@link Quartet}.
     *
     * @param first the first element
     * @param second the second element
     * @param third the third element
     * @param fourth the fourth element
     */
    public Quartet(final A first, final B second,
                   final C third, final D fourth) {
        this.first = checkNotNull(first);
        this.second = checkNotNull(second);
        this.third = checkNotNull(third);
        this.fourth = checkNotNull(fourth);
    }

    /**
     * Returns the first element.
     *
     * @return the first element
     */
    public final A getFirst() {
        return this.first;
    }

    /**
     * Returns the second element.
     *
     * @return the second element
     */
    public final B getSecond() {
        return this.second;
    }

    /**
     * Returns the third element.
     *
     * @return the third element
     */
    public final C getThird() {
        return this.third;
    }

    /**
     * Returns the fourth element.
     *
     * @return the fourth element
     */
    public final D getFourth() {
        return this.fourth;
    }

    @Override
    public final String toString() {
        return MoreObjects.toStringHelper(this)
                .add("first", this.first)
                .add("second", this.second)
                .add("third", this.third)
                .add("fourth", this.fourth)
                .toString();
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.first, this.second,
                this.third, this.fourth);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final Quartet other = (Quartet) obj;
        return Objects.equal(this.first, other.first)
                && Objects.equal(this.second, other.second)
                && Objects.equal(this.third, other.third)
                && Objects.equal(this.fourth, other.fourth);
    }
}
