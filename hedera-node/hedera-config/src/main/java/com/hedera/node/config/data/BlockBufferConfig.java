// SPDX-License-Identifier: Apache-2.0
package com.hedera.node.config.data;

import com.hedera.node.config.NetworkProperty;
import com.swirlds.config.api.ConfigData;
import com.swirlds.config.api.ConfigProperty;
import com.swirlds.config.api.validation.annotation.Min;
import java.time.Duration;

/**
 * Configuration settings related to the block buffer.
 *
 * @param blockTtl the TTL for entries in the block buffer
 * @param pruneInterval interval to prune block buffer and check for whether backpressure is needed; if set
 *                      to 0 then pruning is effectively disabled
 * @param actionStageThreshold the threshold (as a percentage from 0.0 to 100.0) at which proactive measures are
 *                             taken to attempt faster buffer recovery. This threshold is measured against the
 *                             current saturation level of the buffer. (For example, a value of '20.0' means
 *                             once the buffer saturated reaches 20% or higher, proactive measures will be taken
 *                             such as attempting to connect to a different block node.)
 * @param actionGracePeriod the period between buffer recovery action attempts. After an action has been performed to
 *                          attempt buffer recovery, there will be a grace period before the next recovery attempt is
 *                          triggered. That delay is this configuration property.
 * @param recoveryThreshold the threshold (as a percentage from 0.0 to 100.0) of which the buffer saturation level must
 *                          be decreased by before the buffer is considered "in recovery" and back pressure (if enabled)
 *                          can be removed. (For example, a value of '85.0' means at least 15% of the buffer capacity
 *                          must be available. Said another way: the buffer saturation must be at or below 85% before
 *                          the buffer is considered recovered.)
 */
@ConfigData("blockStream.buffer")
public record BlockBufferConfig(
        @ConfigProperty(defaultValue = "5m") @Min(0) @NetworkProperty Duration blockTtl,
        @ConfigProperty(defaultValue = "1s") @Min(0) @NetworkProperty Duration pruneInterval,
        @ConfigProperty(defaultValue = "50.0") @Min(0) @NetworkProperty double actionStageThreshold,
        @ConfigProperty(defaultValue = "20s") @Min(0) @NetworkProperty Duration actionGracePeriod,
        @ConfigProperty(defaultValue = "85.0") @Min(0) @NetworkProperty double recoveryThreshold) {}
