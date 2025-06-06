// SPDX-License-Identifier: Apache-2.0
package org.hiero.consensus.event.creator.impl.stale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import com.hedera.hapi.node.state.roster.Roster;
import com.hedera.hapi.platform.state.ConsensusSnapshot;
import com.swirlds.common.metrics.noop.NoOpMetrics;
import com.swirlds.common.test.fixtures.Randotron;
import com.swirlds.component.framework.transformers.RoutableData;
import com.swirlds.config.api.Configuration;
import com.swirlds.config.extensions.test.fixtures.TestConfigBuilder;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.hiero.consensus.model.event.PlatformEvent;
import org.hiero.consensus.model.event.StaleEventDetectorOutput;
import org.hiero.consensus.model.hashgraph.ConsensusRound;
import org.hiero.consensus.model.hashgraph.EventWindow;
import org.hiero.consensus.model.node.NodeId;
import org.hiero.consensus.model.test.fixtures.event.TestingEventBuilder;
import org.hiero.consensus.model.test.fixtures.hashgraph.EventWindowBuilder;
import org.junit.jupiter.api.Test;

class StaleEventDetectorTests {

    /**
     * Extract self events from a stream containing both self events and stale self events. Corresponds to data tagged
     * with {@link StaleEventDetectorOutput#SELF_EVENT}.
     */
    private List<PlatformEvent> getSelfEvents(@NonNull final List<RoutableData<StaleEventDetectorOutput>> data) {
        final List<PlatformEvent> output = new ArrayList<>();
        for (final RoutableData<StaleEventDetectorOutput> datum : data) {
            if (datum.address() == StaleEventDetectorOutput.SELF_EVENT) {
                output.add((PlatformEvent) datum.data());
            }
        }
        return output;
    }

    /**
     * Validate that the correct stale event was returned as part of the output.
     *
     * @param data      the output data
     * @param selfEvent the self event that should have been returned
     */
    private void assertSelfEventReturned(
            @NonNull final List<RoutableData<StaleEventDetectorOutput>> data, @NonNull final PlatformEvent selfEvent) {

        final List<PlatformEvent> selfEvents = getSelfEvents(data);
        assertEquals(1, selfEvents.size());
        assertSame(selfEvent, selfEvents.getFirst());
    }

    /**
     * Validate that no self events were returned as part of the output. (Not to be confused with "stale self events"
     * events.) Essentially, we don't want to see data tagged with {@link StaleEventDetectorOutput#SELF_EVENT} unless we
     * are adding a self event and want to see it pass through.
     *
     * @param data the output data
     */
    private void assertNoSelfEventReturned(@NonNull final List<RoutableData<StaleEventDetectorOutput>> data) {
        final List<PlatformEvent> selfEvents = getSelfEvents(data);
        assertEquals(0, selfEvents.size());
    }

    /**
     * Extract stale self events from a stream containing both self events and stale self events. Corresponds to data
     * tagged with {@link StaleEventDetectorOutput#STALE_SELF_EVENT}.
     */
    private List<PlatformEvent> getStaleSelfEvents(@NonNull final List<RoutableData<StaleEventDetectorOutput>> data) {
        final List<PlatformEvent> output = new ArrayList<>();
        for (final RoutableData<StaleEventDetectorOutput> datum : data) {
            if (datum.address() == StaleEventDetectorOutput.STALE_SELF_EVENT) {
                output.add((PlatformEvent) datum.data());
            }
        }
        return output;
    }

    @Test
    void throwIfInitialEventWindowNotSetTest() {
        final Randotron randotron = Randotron.create();
        final NodeId selfId = NodeId.of(randotron.nextPositiveLong());

        final Configuration configuration = new TestConfigBuilder().getOrCreateConfig();
        final StaleEventDetector detector = new DefaultStaleEventDetector(configuration, new NoOpMetrics(), selfId);

        final PlatformEvent event = new TestingEventBuilder(randotron).build();

        assertThrows(IllegalStateException.class, () -> detector.addSelfEvent(event));
    }

    @Test
    void eventIsStaleBeforeAddedTest() {
        final Randotron randotron = Randotron.create();
        final NodeId selfId = NodeId.of(randotron.nextPositiveLong());

        final Configuration configuration = new TestConfigBuilder().getOrCreateConfig();
        final StaleEventDetector detector = new DefaultStaleEventDetector(configuration, new NoOpMetrics(), selfId);

        final long ancientThreshold = randotron.nextPositiveLong() + 100;
        final long eventBirthRound = ancientThreshold
                - (randotron.nextLong(100)
                        + 1); // +1, because birthRound==ancientThreshold is not yet considered ancient

        final PlatformEvent event = new TestingEventBuilder(randotron)
                .setCreatorId(selfId)
                .setBirthRound(eventBirthRound)
                .build();

        detector.setInitialEventWindow(EventWindowBuilder.builder()
                .setLatestConsensusRound(randotron.nextPositiveInt())
                .setAncientThreshold(ancientThreshold)
                .setExpiredThreshold(randotron.nextPositiveLong())
                .build());

        final List<RoutableData<StaleEventDetectorOutput>> output = detector.addSelfEvent(event);

        final List<PlatformEvent> platformEvents = getSelfEvents(output);
        final List<PlatformEvent> staleEvents = getStaleSelfEvents(output);

        assertEquals(1, staleEvents.size());
        assertSame(event, staleEvents.getFirst());

        assertSelfEventReturned(output, event);
    }

    /**
     * Construct a consensus round.
     *
     * @param randotron        a source of randomness
     * @param events           events that will reach consensus in this round
     * @param ancientThreshold the ancient threshold for this round
     * @return a consensus round
     */
    @NonNull
    private ConsensusRound createConsensusRound(
            @NonNull final Randotron randotron,
            @NonNull final List<PlatformEvent> events,
            final long ancientThreshold) {
        final EventWindow eventWindow = EventWindowBuilder.builder()
                .setLatestConsensusRound(randotron.nextPositiveLong())
                .setAncientThreshold(ancientThreshold)
                .setExpiredThreshold(randotron.nextPositiveLong())
                .build();

        return new ConsensusRound(
                mock(Roster.class), events, eventWindow, mock(ConsensusSnapshot.class), false, Instant.now());
    }

    @Test
    void randomEventsTest() {
        final Randotron randotron = Randotron.create();
        final NodeId selfId = NodeId.of(randotron.nextPositiveLong());

        final Configuration configuration = new TestConfigBuilder().getOrCreateConfig();
        final StaleEventDetector detector = new DefaultStaleEventDetector(configuration, new NoOpMetrics(), selfId);

        final Set<PlatformEvent> detectedStaleEvents = new HashSet<>();
        final Set<PlatformEvent> expectedStaleEvents = new HashSet<>();
        final List<PlatformEvent> consensusEvents = new ArrayList<>();

        long currentAncientThreshold = randotron.nextLong(100, 1_000);
        detector.setInitialEventWindow(EventWindowBuilder.builder()
                .setLatestConsensusRound(randotron.nextPositiveLong())
                .setAncientThreshold(currentAncientThreshold)
                .setExpiredThreshold(randotron.nextPositiveLong())
                .build());

        for (int i = 0; i < 10_000; i++) {
            final boolean selfEvent = randotron.nextBoolean(0.25);
            final NodeId eventCreator = selfEvent ? selfId : NodeId.of(randotron.nextPositiveLong());

            final TestingEventBuilder eventBuilder = new TestingEventBuilder(randotron).setCreatorId(eventCreator);

            final boolean eventIsAncientBeforeAdded = randotron.nextBoolean(0.01);
            if (eventIsAncientBeforeAdded) {
                eventBuilder.setBirthRound(currentAncientThreshold - randotron.nextLong(1, 100));
            } else {
                eventBuilder.setBirthRound(currentAncientThreshold + randotron.nextLong(3));
            }
            final PlatformEvent event = eventBuilder.build();

            final boolean willReachConsensus = !eventIsAncientBeforeAdded && randotron.nextBoolean(0.8);

            if (willReachConsensus) {
                consensusEvents.add(event);
            }

            if (selfEvent && (eventIsAncientBeforeAdded || !willReachConsensus)) {
                expectedStaleEvents.add(event);
            }

            if (selfEvent) {
                final List<RoutableData<StaleEventDetectorOutput>> output = detector.addSelfEvent(event);
                detectedStaleEvents.addAll(getStaleSelfEvents(output));
                assertSelfEventReturned(output, event);
            }

            // Once in a while, permit a round to "reach consensus"
            if (randotron.nextBoolean(0.01)) {
                currentAncientThreshold += randotron.nextLong(3);

                final ConsensusRound consensusRound =
                        createConsensusRound(randotron, consensusEvents, currentAncientThreshold);

                final List<RoutableData<StaleEventDetectorOutput>> output = detector.addConsensusRound(consensusRound);
                detectedStaleEvents.addAll(getStaleSelfEvents(output));
                assertNoSelfEventReturned(output);
                consensusEvents.clear();
            }
        }

        // Create a final round with all remaining consensus events. Move ancient threshold far enough forward
        // to flush out all events we expect to eventually become stale.
        currentAncientThreshold += randotron.nextLong(1_000, 10_000);
        final ConsensusRound consensusRound = createConsensusRound(randotron, consensusEvents, currentAncientThreshold);
        final List<RoutableData<StaleEventDetectorOutput>> output = detector.addConsensusRound(consensusRound);
        detectedStaleEvents.addAll(getStaleSelfEvents(output));
        assertNoSelfEventReturned(output);

        assertEquals(expectedStaleEvents.size(), detectedStaleEvents.size());
    }

    @Test
    void clearTest() {
        final Randotron randotron = Randotron.create();
        final NodeId selfId = NodeId.of(randotron.nextPositiveLong());

        final Configuration configuration = new TestConfigBuilder().getOrCreateConfig();
        final StaleEventDetector detector = new DefaultStaleEventDetector(configuration, new NoOpMetrics(), selfId);

        final long ancientThreshold1 = randotron.nextPositiveInt() + 100;
        final long eventBirthRound1 = ancientThreshold1 + randotron.nextPositiveInt(10);

        final PlatformEvent event1 = new TestingEventBuilder(randotron)
                .setCreatorId(selfId)
                .setBirthRound(eventBirthRound1)
                .build();

        detector.setInitialEventWindow(EventWindowBuilder.builder()
                .setLatestConsensusRound(randotron.nextPositiveInt())
                .setAncientThreshold(ancientThreshold1)
                .setExpiredThreshold(randotron.nextPositiveLong())
                .build());

        final List<RoutableData<StaleEventDetectorOutput>> output1 = detector.addSelfEvent(event1);
        assertSelfEventReturned(output1, event1);
        assertEquals(0, getStaleSelfEvents(output1).size());

        detector.clear();

        // Adding an event again before setting the event window should throw.
        assertThrows(IllegalStateException.class, () -> detector.addSelfEvent(event1));

        // Setting the ancient threshold after the original event should not cause it to come back as stale.
        final long ancientThreshold2 = eventBirthRound1 + randotron.nextPositiveInt();
        detector.setInitialEventWindow(EventWindowBuilder.builder()
                .setLatestConsensusRound(randotron.nextPositiveInt())
                .setAncientThreshold(ancientThreshold2)
                .setExpiredThreshold(randotron.nextPositiveLong())
                .build());

        // Verify that we get otherwise normal behavior after the clear.

        final long eventBirthRound2 = ancientThreshold2 + randotron.nextPositiveInt(10);
        final PlatformEvent event2 = new TestingEventBuilder(randotron)
                .setCreatorId(selfId)
                .setBirthRound(eventBirthRound2)
                .build();

        final List<RoutableData<StaleEventDetectorOutput>> output2 = detector.addSelfEvent(event2);
        assertSelfEventReturned(output2, event2);
        assertEquals(0, getStaleSelfEvents(output2).size());

        final long ancientThreshold3 = eventBirthRound2 + randotron.nextPositiveInt(10);
        final ConsensusRound consensusRound = createConsensusRound(randotron, List.of(), ancientThreshold3);
        final List<RoutableData<StaleEventDetectorOutput>> output3 = detector.addConsensusRound(consensusRound);
        assertNoSelfEventReturned(output3);
        final List<PlatformEvent> staleEvents = getStaleSelfEvents(output3);
        assertEquals(1, staleEvents.size());
        assertSame(event2, staleEvents.getFirst());
    }
}
