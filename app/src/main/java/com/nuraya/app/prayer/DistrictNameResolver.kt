package com.nuraya.app.prayer

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.coroutines.resume

/** Android/Indonesia commonly supplies kecamatan in locality and kelurahan in subLocality. */
internal fun districtLabel(address: Address): String? {
    val prefix=Regex("^(?:kecamatan|kec\\.?)\\s+",RegexOption.IGNORE_CASE)
    val fields=listOfNotNull(address.locality,address.subLocality,address.subAdminArea)
    val explicit=(fields+(0..address.maxAddressLineIndex.coerceAtMost(5)).flatMap { address.getAddressLine(it).orEmpty().split(',') })
        .map(String::trim).firstOrNull { prefix.containsMatchIn(it) }
    return (explicit?.replace(prefix,"") ?: address.locality ?: address.subAdminArea ?: address.subLocality)
        ?.trim()?.takeIf { it.isNotEmpty() && it.length<=80 }
}

class DistrictNameResolver(context:Context) {
    private val context=context.applicationContext
    private val preferences=this.context.getSharedPreferences("nuraya_place_names",Context.MODE_PRIVATE)
    suspend fun resolve(location:PrayerLocation):String? {
        val key=String.format(Locale.ROOT,"%.5f,%.5f",location.latitude,location.longitude)
        if(preferences.getString("coordinate",null)==key)preferences.getString("name",null)?.let{return it}
        if(!Geocoder.isPresent())return null
        val geocoder=Geocoder(context,Locale.forLanguageTag("id-ID"))
        val addresses=withTimeoutOrNull(6_000) {
            suspendCancellableCoroutine<List<Address>> { continuation ->
                fun finish(value:List<Address>) { if(continuation.isActive)continuation.resume(value) }
                if(Build.VERSION.SDK_INT>=33) {
                    try{geocoder.getFromLocation(location.latitude,location.longitude,3,object:Geocoder.GeocodeListener {
                        override fun onGeocode(addresses:MutableList<Address>)=finish(addresses)
                        override fun onError(errorMessage:String?)=finish(emptyList())
                    })}catch(_:Exception){finish(emptyList())}
                } else {
                    val task=legacyExecutor.submit {
                        @Suppress("DEPRECATION")
                        val result=runCatching{geocoder.getFromLocation(location.latitude,location.longitude,3).orEmpty()}.getOrDefault(emptyList())
                        finish(result)
                    }
                    continuation.invokeOnCancellation { task.cancel(true) }
                }
            }
        }.orEmpty()
        val name=addresses.firstNotNullOfOrNull(::districtLabel)
        if(name!=null)preferences.edit().putString("coordinate",key).putString("name",name).apply()
        return name
    }
    companion object {
        private val legacyExecutor=Executors.newFixedThreadPool(2){r->Thread(r,"Nuraya-place-name").apply{isDaemon=true}}
    }
}
