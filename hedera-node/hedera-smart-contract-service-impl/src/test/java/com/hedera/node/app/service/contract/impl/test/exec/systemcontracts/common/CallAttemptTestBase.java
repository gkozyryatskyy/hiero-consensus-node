// SPDX-License-Identifier: Apache-2.0
package com.hedera.node.app.service.contract.impl.test.exec.systemcontracts.common;

import static com.hedera.node.app.service.contract.impl.test.TestHelpers.OWNER_BESU_ADDRESS;

import com.hedera.node.app.service.contract.impl.exec.scope.VerificationStrategies;
import com.hedera.node.app.service.contract.impl.exec.systemcontracts.HssSystemContract;
import com.hedera.node.app.service.contract.impl.exec.systemcontracts.common.CallTranslator;
import com.hedera.node.app.service.contract.impl.exec.systemcontracts.hss.HssCallAttempt;
import com.hedera.node.app.service.contract.impl.exec.systemcontracts.hts.AddressIdConverter;
import com.hedera.node.app.service.contract.impl.exec.utils.SystemContractMethodRegistry;
import com.hedera.node.app.service.contract.impl.test.TestHelpers;
import com.hedera.node.app.spi.signatures.SignatureVerifier;
import com.swirlds.config.api.Configuration;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.List;
import org.apache.tuweni.bytes.Bytes;
import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.evm.frame.MessageFrame;
import org.mockito.Mock;

/**
 * The base test class for all unit tests in Smart Contract Service that is using AbstractCallAttempt.
 */
public class CallAttemptTestBase extends CallTestBase {

    // properties for CallAttempt
    @Mock
    protected AddressIdConverter addressIdConverter;

    @Mock
    protected VerificationStrategies verificationStrategies;

    @Mock
    protected SignatureVerifier signatureVerifier;

    protected final SystemContractMethodRegistry systemContractMethodRegistry = new SystemContractMethodRegistry();

    @Mock
    protected MessageFrame frame;

    protected HssCallAttempt createHssCallAttempt(
            @NonNull final Bytes input, @NonNull final CallTranslator<HssCallAttempt> subject) {
        return createHssCallAttempt(input, false, TestHelpers.DEFAULT_CONFIG, List.of(subject));
    }

    protected HssCallAttempt createHssCallAttempt(
            @NonNull final Bytes input,
            final boolean onlyDelegatableContractKeysActive,
            @NonNull final Configuration configuration,
            @NonNull final List<CallTranslator<HssCallAttempt>> callTranslators) {
        return createHssCallAttempt(
                input, OWNER_BESU_ADDRESS, onlyDelegatableContractKeysActive, configuration, callTranslators);
    }

    protected HssCallAttempt createHssCallAttempt(
            @NonNull final Bytes input,
            @NonNull final Address senderAddress,
            final boolean onlyDelegatableContractKeysActive,
            @NonNull final Configuration configuration,
            @NonNull final List<CallTranslator<HssCallAttempt>> callTranslators) {
        return new HssCallAttempt(
                HssSystemContract.HSS_CONTRACT_ID,
                input,
                senderAddress,
                onlyDelegatableContractKeysActive,
                mockEnhancement(),
                configuration,
                addressIdConverter,
                verificationStrategies,
                signatureVerifier,
                gasCalculator,
                callTranslators,
                systemContractMethodRegistry,
                false);
    }
}
