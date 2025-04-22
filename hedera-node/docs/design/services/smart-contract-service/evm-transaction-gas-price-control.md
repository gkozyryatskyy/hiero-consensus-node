# EVM transaction gas price control

## Purpose

For private networks we need a way to control `gasPrice` of the `EthereumTransaction`

## Goals

- Provide a code and configuration to control `gasPrice` used by Hedera `EthereumTransaction`
- Understand when original `EthereumTransaction` (on Ethereum) is incrementing/not incrementing the `nonce`
- Understand what original `EthereumTransaction` (on Ethereum) error codes still causes `nonce` increment

## Nonce increment

- `Nonce` is a transactions counter https://ethereum.org/en/glossary/#nonce
- Ethereum account `nonce` will be incremented even if the transaction fails to be included in a block on the blockchain.
  In other words if the transaction is added to a block (recorded), with any status, the `nonce` will be incremented
- Here is some addition comparison of exceptional situations where `nonce` is not incremented on Hedera and Ethereum https://github.com/hiero-ledger/hiero-consensus-node/issues/17696#issuecomment-2792286471

## Architecture

There is a [FeeManager](../../../../hedera-app/src/main/java/com/hedera/node/app/fees/FeeManager.java) That provides
`FunctionalityResourcePrices` with `basePrices.servicedata.gas` to Hedera Smart Contract Service (HSCS). On each 
transaction `HederaEvmContext` is created. `basePrices.servicedata.gas` goes to `HederaEvmContext` as `gasPrice` and 
used in HSCS for gas cost calculations. We should add `evm.ethTransaction.gasPriceOverride.enable` and 
`evm.ethTransaction.gasPriceOverride.tinybar` configurations to override `HederaEvmContext.gasPrice`. There is also few 
places that will not work correctly with `gasPrice=0`, they will be described in Implementation section of this design

## Implementation

- Add `evm.ethTransaction.gasPriceOverride.enable` and `evm.ethTransaction.gasPriceOverride.tinybar` to configs
- Override `HederaEvmContext.gasPrice` if `evm.ethTransaction.gasPriceOverride.enable=true` with `evm.ethTransaction.gasPriceOverride.tinybar`
- Change `CustomGasCharging` code to increment nonce on `context.isNoopGasContext()`

## Open Questions

- should we provide `evm.ethTransaction.gasPriceOverride` to override `gasPrice` from `FeeManager` or other team should introduce changes directly is `FeeManager`?
- `evm.ethTransaction.gasPriceOverride.enable=false` vs `evm.ethTransaction.gasPriceOverride=null`
- `evm.ethTransaction.gasPriceOverride` in tinybars?
- if `evm.ethTransaction.gasPriceOverride=0` should we check `sender.balance>tx.maxGasAllowance`?

## Acceptance Tests

- Verify if we can send `EthereumTransaction`, that will be executed successfully and charges no gas costs
  - tests with different precondition
- Verify if 'free' `EthereumTransaction` is incrementing the `nonce`
  - tests with different precondition
- Check if gas throttle is still working with `gasPrice=0`
- TODO add more

## TODO
- check besu gasPrice=0
- add not `EthereumTransaction` calls to the design
- remove `nonce` parts
