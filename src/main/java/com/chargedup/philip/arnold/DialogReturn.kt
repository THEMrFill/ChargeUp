package com.chargedup.philip.arnold

import android.location.Location
import com.google.android.gms.maps.model.LatLng

interface DialogReturn {
    fun onFinishDialog(userLocation: Boolean, start: LatLng?, end: LatLng?)
}