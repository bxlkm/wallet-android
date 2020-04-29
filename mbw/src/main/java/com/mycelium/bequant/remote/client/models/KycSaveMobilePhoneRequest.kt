/**
* Auth API
* Auth API<br> <a href='/changelog'>Changelog</a>
*
* The version of the OpenAPI document: v0.0.50
* 
*
* NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
* https://openapi-generator.tech
* Do not edit the class manually.
*/
package com.mycelium.bequant.remote.client.models



import android.os.Parcelable
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize

/**
 * 
 * @param mobilePhone Mobile phone without country code
 * @param mobilePhoneCountryCode Mobile phone country code
 */
@JsonClass(generateAdapter = true)
@Parcelize
data class KycSaveMobilePhoneRequest (
    @Json(name = "mobile_phone")
    val mobilePhone: kotlin.String,
    @Json(name = "mobile_phone_country_code")
    val mobilePhoneCountryCode: kotlin.String
) : Parcelable

