// SPDX-License-Identifier: Apache-2.0
package com.swirlds.state.spi;

import com.hedera.pbj.runtime.Schema;
import edu.umd.cs.findbugs.annotations.NonNull;

/** Essentially, a map of {@link WritableKVState}s. Each state may be retrieved by key. */
public interface WritableStates extends ReadableStates {
    /**
     * Gets the {@link WritableKVState} associated with the given stateKey. If the state cannot be
     * found, an exception is thrown. This should **never** happen in an application, and represents
     * a fatal bug. Applications must only ask for states that they have previously registered with
     * the {@link Schema}.
     *
     * @param stateKey The key used for looking up state
     * @param <K> The key type in the state.
     * @param <V> The value type in the state.
     * @return The state for that key. This will never be null.
     * @throws NullPointerException if stateKey is null.
     * @throws IllegalArgumentException if the state cannot be found.
     */
    @Override
    @NonNull
    <K, V> WritableKVState<K, V> get(@NonNull String stateKey);

    @Override
    @NonNull
    <T> WritableSingletonState<T> getSingleton(@NonNull String stateKey);

    @Override
    @NonNull
    <E> WritableQueueState<E> getQueue(@NonNull String stateKey);
}
