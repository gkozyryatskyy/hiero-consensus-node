// SPDX-License-Identifier: Apache-2.0
package com.hedera.node.app.spi.workflows;

import static java.util.Objects.requireNonNull;

import com.hedera.hapi.node.base.ResponseCodeEnum;
import com.hedera.node.app.spi.fees.FeeCharging;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.function.Consumer;

/**
 * A runtime exception that wraps a {@link ResponseCodeEnum} status. Thrown by
 * components in the {@code handle} workflow to signal a transaction has reached
 * an unsuccessful outcome.
 *
 * <p>In general, this exception is <i>not</i> appropriate to throw when code
 * detects an internal error. Instead, use {@link IllegalStateException} or
 * {@link IllegalArgumentException} as appropriate.
 */
public class HandleException extends RuntimeException {
    private final ShouldRollbackStack shouldRollbackStack;
    private final ResponseCodeEnum status;

    @Nullable
    private final Consumer<FeeCharging.Context> rollbackFeesCb;

    /**
     * Whether the stack should be rolled back. In case of a ContractCall if it reverts, the gas charged
     * should not be rolled back
     */
    public enum ShouldRollbackStack {
        YES,
        NO
    }

    public HandleException(final ResponseCodeEnum status) {
        this(status, ShouldRollbackStack.YES, null);
    }

    public HandleException(
            @NonNull final ResponseCodeEnum status, @NonNull final ShouldRollbackStack shouldRollbackStack) {
        this(status, shouldRollbackStack, null);
    }

    public HandleException(
            @NonNull final ResponseCodeEnum status, @Nullable final Consumer<FeeCharging.Context> rollbackFeesCb) {
        this(status, ShouldRollbackStack.YES, rollbackFeesCb);
    }

    private HandleException(
            @NonNull final ResponseCodeEnum status,
            @NonNull final ShouldRollbackStack shouldRollbackStack,
            @Nullable final Consumer<FeeCharging.Context> rollbackFeesCb) {
        super(status.protoName());
        this.status = requireNonNull(status);
        this.shouldRollbackStack = requireNonNull(shouldRollbackStack);
        this.rollbackFeesCb = rollbackFeesCb;
    }

    /**
     * Returns whether the stack should be rolled back. In case of a ContractCall if it reverts, the gas charged
     * should not be rolled back
     */
    public boolean shouldRollbackStack() {
        return shouldRollbackStack == ShouldRollbackStack.YES;
    }

    /**
     * If the exception was constructed with rollback fee charging, charges it in the given context.
     * @param context the context in which to charge the rollback fees
     */
    public void maybeReplayFees(@NonNull final FeeCharging.Context context) {
        if (rollbackFeesCb != null) {
            rollbackFeesCb.accept(context);
        }
    }

    /**
     * {@inheritDoc}
     * This implementation prevents initializing a cause.  HandleException is a result code carrier and
     * must not have a cause.  If another {@link Throwable} caused this exception to be thrown, then that other
     * throwable <strong>must</strong> be logged to appropriate diagnostics before the {@code HandleException}
     * is thrown.
     * @throws UnsupportedOperationException always.  This method must not be called.
     */
    @Override
    // Suppressing the warning that this method is not synchronized as its parent.
    // Since the method will only throw an error there is no need for synchronization
    @SuppressWarnings("java:S3551")
    public Throwable initCause(Throwable cause) {
        throw new UnsupportedOperationException("HandleException must not chain a cause");
    }

    public ResponseCodeEnum getStatus() {
        return status;
    }

    public static void validateTrue(final boolean flag, final ResponseCodeEnum errorStatus) {
        if (!flag) {
            throw new HandleException(errorStatus);
        }
    }

    public static void validateFalse(final boolean flag, final ResponseCodeEnum errorStatus) {
        validateTrue(!flag, errorStatus);
    }

    @Override
    public String toString() {
        return "HandleException{" + "status=" + status + '}';
    }

    /**
     * <strong>Disallowed</strong> constructor of {@code HandleException}.
     * This {@link Exception} subclass is used as a form of unconditional jump, rather than a true
     * exception.  If another {@link Throwable} caused this exception to be thrown, then that other
     * throwable <strong>must</strong> be logged to appropriate diagnostics before the {@code HandleException}
     * is thrown.
     *
     * @param responseCode the {@link ResponseCodeEnum responseCode}.  This is ignored.
     * @param cause the {@link Throwable} that caused this exception.  This is ignored.
     * @throws UnsupportedOperationException always.  This constructor must not be called.
     */
    // Suppressing the warning that the constructor and arguments are not used
    @SuppressWarnings({"java:S1144", "java:S1172"})
    private HandleException(@NonNull final ResponseCodeEnum responseCode, @Nullable final Throwable cause) {
        throw new UnsupportedOperationException("HandleException must not chain a cause");
    }
}
