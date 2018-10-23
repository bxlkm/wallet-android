package com.mycelium.wallet.coinapult

import android.util.Log
import com.coinapult.api.httpclient.CoinapultClient
import com.coinapult.api.httpclient.SearchMany
import com.mrd.bitlib.model.Address
import com.mrd.bitlib.util.Sha256Hash
import com.mycelium.wapi.wallet.GenericAddress
import com.mycelium.wapi.wallet.btc.BtcAddress
import com.mycelium.wapi.wallet.btc.BtcLegacyAddress
import com.mycelium.wapi.wallet.coinapult.CoinapultApi
import com.mycelium.wapi.wallet.coinapult.CoinapultTransaction
import com.mycelium.wapi.wallet.coinapult.Currency
import com.mycelium.wapi.wallet.coins.Balance
import com.mycelium.wapi.wallet.coins.Value
import java.math.BigDecimal
import java.net.SocketTimeoutException


class CoinapultApiImpl(val client: CoinapultClient) : CoinapultApi {
    override fun getAddress(currency: Currency, currenctAddress: GenericAddress?): GenericAddress? {
        var address: GenericAddress? = null
        if (currenctAddress == null) {
            address = BtcLegacyAddress(currency, Address.fromString(client.bitcoinAddress.address).allAddressBytes)
        } else {
            val criteria = HashMap<String, String>(1)
            criteria["to"] = address.toString()
            val search = client.search(criteria)
            val alreadyUsed = search.containsKey("transaction_id")
            if (alreadyUsed) {
                // get a new one
                address = BtcLegacyAddress(currency, Address.fromString(client.bitcoinAddress.address).allAddressBytes)
            } else {
                address = currenctAddress
            }
            client.config(address.toString(), currency.name)
        }
        return address
    }

    override fun getBalance(currency: Currency): Balance? {
        try {
            val balance = client.accountInfo().balances
            val filters = balance.filter {
                it.currency == currency.name
            }
            if (filters.isNotEmpty()) {
                return Balance(Value.valueOf(currency
                        , filters[0].amount.multiply(BigDecimal.TEN.pow(currency.getUnitExponent())).toLong())
                        , Value.zeroValue(currency), Value.zeroValue(currency), Value.zeroValue(currency))
            }
        } catch (e: CoinapultClient.CoinapultBackendException) {
            Log.e("CoinapultApiImpl", "error while getting balance", e)
        } catch (e: SocketTimeoutException) {
            Log.e("CoinapultApiImpl", "error while getting balance", e)
        }
        return null
    }

    override fun getTransactions(currency: Currency): List<CoinapultTransaction>? {
        var result: List<CoinapultTransaction>? = null
        //get first page to get pageCount
        try {
            var batch = client.history(1)
            val tmpResult = mutableListOf<CoinapultTransaction>()
            add(currency, batch, tmpResult)
            //get extra pages
            var i = 2
            while (batch.page < batch.pageCount) {
                batch = client.history(i)
                add(currency, batch, tmpResult)
                i++
            }
            result = tmpResult
        } catch (e: CoinapultClient.CoinapultBackendException) {
            Log.e("CoinapultApiImpl", "error while getting history", e)
        } catch (e: SocketTimeoutException) {
            Log.e("CoinapultApiImpl", "error while getting balance", e)
        }
        return result
    }

    private fun add(currency: Currency, batch: SearchMany.Json?, tmpResult: MutableList<CoinapultTransaction>) {
        if (batch?.result != null) {
            batch.result.forEach {
                val isIncoming = it.type != "payment"
                val half = if (isIncoming) it.out else it.`in`
                val txCurrency = Currency.all[half.currency]!!
                if (currency == txCurrency) {
                    val data = it.tid.toByteArray().plus(ByteArray(Sha256Hash.HASH_LENGTH - it.tid.toByteArray().size))
                    tmpResult.add(CoinapultTransaction(Sha256Hash.of(data)
                            , Value.valueOf(currency, half.amount.multiply(BigDecimal.TEN.pow(currency.getUnitExponent())).toLong())
                            , isIncoming, it.completeTime, it.state, it.timestamp))
                }
            }
        }
    }
}