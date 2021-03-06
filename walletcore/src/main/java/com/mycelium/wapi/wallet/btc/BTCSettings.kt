package com.mycelium.wapi.wallet.btc

import com.mrd.bitlib.model.AddressType
import com.mycelium.wapi.wallet.CurrencySettings

data class BTCSettings(
        var defaultAddressType: AddressType,
        var changeAddressModeReference: Reference<ChangeAddressMode>
) : CurrencySettings {
    fun setChangeAddressMode(changeAddressMode: ChangeAddressMode) =
        changeAddressModeReference.set(changeAddressMode)
}


class Reference<T>(private var referent: T?) {
    fun set(newVal: T) {
        referent = newVal
    }

    fun get(): T? {
        return referent
    }
}