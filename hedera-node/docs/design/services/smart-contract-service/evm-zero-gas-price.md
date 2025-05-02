# EVM zero gas price

## Purpose

For private networks we need a way to control `gasPrice` of the transaction.

## Goals

- Provide a code and configuration to control `gasPrice` used by Hedera transaction.

## Architecture

There is a [FeeManager](../../../../hedera-app/src/main/java/com/hedera/node/app/fees/FeeManager.java) That provides
`FunctionalityResourcePrices` with `basePrices.servicedata.gas` to Hedera Smart Contract Service (HSCS). On each
transaction `HederaEvmContext` is created. `basePrices.servicedata.gas` goes to `HederaEvmContext` as `gasPrice` and
used in HSCS for gas cost calculations. We should add `evm.ethTransaction.gasPriceOverride.enable` and
`evm.ethTransaction.gasPriceOverride.tinybar` configurations to override `HederaEvmContext.gasPrice`. There is also few
places that will not work correctly with `gasPrice=0`, they will be described in Implementation section of this design

### Besu example

- [Configure free gas networks](https://besu.hyperledger.org/stable/private-networks/how-to/configure/free-gas) for Besu
- Config: [min-gas-price](https://besu.hyperledger.org/stable/public-networks/reference/cli/options#min-gas-price)`=0` on node start
- Suggestions
  - [Increase block size](https://besu.hyperledger.org/stable/private-networks/how-to/configure/free-gas#1-set-the-block-size) (in gas)
  - [Set contract size to maximum value](https://besu.hyperledger.org/stable/private-networks/how-to/configure/free-gas#1-set-the-block-size)
  - Enable [zeroBaseFee](https://besu.hyperledger.org/stable/private-networks/how-to/configure/free-gas#4-enable-zero-base-fee-if-using-london-fork-or-later) for London fork
  - [Configure free gas in Truffle](https://besu.hyperledger.org/stable/private-networks/how-to/configure/free-gas#configure-free-gas-in-truffle)

## Implementation

- Add `evm.ethTransaction.gasPriceOverride.enable` and `evm.ethTransaction.gasPriceOverride.tinybar` to configs
- Override `HederaEvmContext.gasPrice` if `evm.ethTransaction.gasPriceOverride.enable=true` with
  `evm.ethTransaction.gasPriceOverride.tinybar`
- Change `CustomGasCharging` code to increment nonce on `context.isNoopGasContext()`

## Open Questions

- should we provide `evm.ethTransaction.gasPriceOverride` to override `gasPrice` from `FeeManager` or other team should
  introduce changes directly is `FeeManager`?
- `evm.ethTransaction.gasPriceOverride.enable=false` vs `evm.ethTransaction.gasPriceOverride=null`
- `evm.ethTransaction.gasPriceOverride` in tinybars?
- if `evm.ethTransaction.gasPriceOverride=0` should we check `sender.balance>tx.maxGasAllowance`?
- check gas estimations work correctly with `gasPrice=0`

## Acceptance Tests

- Verify if we can send `EthereumTransaction`, `ContractCreate`, `ContractCall` , `ContractCallLocal`
  that will be executed successfully and charges no gas costs
  - tests with different precondition
- Verify if 'free' `EthereumTransaction` is incrementing the `nonce`
  - tests with different precondition
- Check if gas throttle is still working with `gasPrice=0`

## TODO

- + check besu gasPrice=0
- + add not `EthereumTransaction` calls to the design
