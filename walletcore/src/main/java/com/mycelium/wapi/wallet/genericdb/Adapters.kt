package com.mycelium.wapi.wallet.genericdb

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.mycelium.generated.wallet.database.*
import com.mycelium.generated.wallet.database.EthAccountBacking
import com.mycelium.wapi.wallet.coins.*
import com.squareup.sqldelight.ColumnAdapter
import java.math.BigInteger
import java.util.*


object Adapters {

    val uuidAdapter = object : ColumnAdapter<UUID, String> {
        override fun decode(databaseValue: String) = databaseValue.let(UUID::fromString)

        override fun encode(value: UUID) = value.toString()
    }

    val cryptoCurrencyAdapter = object : ColumnAdapter<CryptoCurrency, String> {
        override fun decode(databaseValue: String): CryptoCurrency =
                databaseValue.let(COINS::get)
                        ?: throw IllegalArgumentException("Unknown currency type $databaseValue")

        override fun encode(value: CryptoCurrency): String = value.id
    }

    val assetAdapter = object : ColumnAdapter<AssetInfo, String> {
        override fun decode(databaseValue: String): CryptoCurrency =
                databaseValue.let(COINS::get)
                        ?: throw IllegalArgumentException("Unknown currency type $databaseValue")

        override fun encode(value: AssetInfo): String = value.id
    }

    val balanceAdapter = object : ColumnAdapter<Balance, String> {
        override fun decode(databaseValue: String): Balance {
            val mapper = ObjectMapper()

            val rootNode = mapper.readTree(databaseValue)
            val childNodes = rootNode.get("Balance") as ArrayNode
            val asset = rootNode.get("Asset").asText()

            val confirmed = Value.valueOf(COINS.getValue(asset), childNodes[0].bigIntegerValue())
            val pendingReceiving = Value.valueOf(COINS.getValue(asset), childNodes[1].bigIntegerValue())
            val pendingSending = Value.valueOf(COINS.getValue(asset), childNodes[2].bigIntegerValue())
            val pendingChange = Value.valueOf(COINS.getValue(asset), childNodes[3].bigIntegerValue())

            return Balance(confirmed, pendingReceiving, pendingSending, pendingChange)
        }

        override fun encode(value: Balance): String {
            val mapper = ObjectMapper()

            val rootNode = mapper.createObjectNode()
            val childNodes = mapper.createArrayNode()

            childNodes.add(value.confirmed.value)
            childNodes.add(value.pendingReceiving.value)
            childNodes.add(value.pendingSending.value)
            childNodes.add(value.pendingChange.value)
            rootNode.set("Balance", childNodes)
            rootNode.put("Asset", value.confirmed.type.id)
            return rootNode.toString()
        }
    }

    val valueAdapter = object : ColumnAdapter<Value, String> {
        override fun decode(databaseValue: String): Value {
            val mapper = ObjectMapper()

            val rootNode = mapper.readTree(databaseValue)
            val asset = COINS.getValue(rootNode["Asset"].asText())
            val value = rootNode["Value"].bigIntegerValue()
            return Value.valueOf(asset, value)
        }

        override fun encode(value: Value): String {
            val mapper = ObjectMapper()

            val rootNode = mapper.createObjectNode()
            rootNode.put("Asset", assetAdapter.encode(value.type))
            rootNode.put("Value", value.value)
            return rootNode.toString()
        }
    }

    val bigIntAdapter = object : ColumnAdapter<BigInteger, String> {
        override fun decode(databaseValue: String): BigInteger {
            return BigInteger(databaseValue)
        }

        override fun encode(value: BigInteger): String {
            return value.toString()
        }
    }

    val listAdapter = object : ColumnAdapter<List<String>, String> {
        override fun decode(databaseValue: String): List<String> {
            val mapper = ObjectMapper()
            val type = mapper.typeFactory.constructCollectionType(ArrayList::class.java, String::class.java)
            return mapper.readValue(databaseValue, type)
        }

        override fun encode(value: List<String>): String {
            return ObjectMapper().writeValueAsString(value)
        }
    }
}

val accountBackingAdapter = AccountBacking.Adapter(Adapters.uuidAdapter, Adapters.cryptoCurrencyAdapter,
        Adapters.valueAdapter, Adapters.valueAdapter)

val ethAccountBackingAdapter = EthAccountBacking.Adapter(Adapters.uuidAdapter, Adapters.bigIntAdapter,
        Adapters.bigIntAdapter, Adapters.bigIntAdapter, Adapters.valueAdapter)

val fioAccountBackingAdapter = FioAccountBacking.Adapter(Adapters.uuidAdapter)

val accountContextAdapter = AccountContext.Adapter(Adapters.uuidAdapter, Adapters.cryptoCurrencyAdapter,
        Adapters.balanceAdapter)
val ethContextAdapter = EthContext.Adapter(Adapters.uuidAdapter, Adapters.bigIntAdapter, Adapters.listAdapter)

val erc20ContextAdapter = Erc20Context.Adapter(Adapters.uuidAdapter, Adapters.bigIntAdapter, Adapters.uuidAdapter)

val fioContextAdapter = FioContext.Adapter(Adapters.uuidAdapter)

val feeEstimatorAdapter = FeeEstimation.Adapter(Adapters.assetAdapter,
        Adapters.valueAdapter, Adapters.valueAdapter, Adapters.valueAdapter, Adapters.valueAdapter)