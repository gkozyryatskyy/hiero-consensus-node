# EVM transaction gas price control

## Purpose
For private networks we need a way to control `gasPrice` of the `EthereumTransaction`

## Goals
- Provide a code and configuration to control `gasPrice` used by Hedera `EthereumTransaction`
- Understand when original `EthereumTransaction` is incrementing/not incrementing the `nonce`
- Understand what `EthereumTransaction` error codes still causes `nonce` increment

## Architecture

There is a [FeeManager](../../../../hedera-app/src/main/java/com/hedera/node/app/fees/FeeManager.java) That provides 
`FunctionalityResourcePrices` with `basePrices.servicedata.gas` to Hedera Smart Contract Service (HSCS). Then
`basePrices.servicedata.gas` goes to `HederaEvmContext` as `gasPrice` and used in HSCS gor gas cons calculations.
We should add `evm.ethTransaction.gasPriceOverride.enable` and `evm.ethTransaction.gasPriceOverride.tinybar` 
configurations to override `HederaEvmContext.gasPrice`. There is also few places that will not work correctly with 
`gasPrice=0`, they will be described in Implementation section of this design

## Implementation
- Add `evm.ethTransaction.gasPriceOverride.enable` and `evm.ethTransaction.gasPriceOverride.tinybar` to configs
- Override `HederaEvmContext.gasPrice` if `evm.ethTransaction.gasPriceOverride.enable=true` with `evm.ethTransaction.gasPriceOverride.tinybar`
- 

## Open Questions

- should we provide `evm.ethTransaction.gasPriceOverride` to override `gasPrice` from `basePrices.servicedata.gas` or the fees object itself should be changed?
- `evm.ethTransaction.gasPriceOverride` in tinybars?
- if `evm.ethTransaction.gasPriceOverride = 0` should we check `sender.balance > tx.maxGasAllowance`?

## Acceptance Tests