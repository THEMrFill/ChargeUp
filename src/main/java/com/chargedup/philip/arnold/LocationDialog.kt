package com.chargedup.philip.arnold

import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import androidx.fragment.app.DialogFragment
import android.os.Bundle
import android.view.ViewGroup
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.RadioButton
import android.widget.TableRow
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.textfield.TextInputEditText
import java.io.IOException

class LocationDialog: DialogFragment() {
    private lateinit var dialogView: View
    private lateinit var start: TextInputEditText
    private lateinit var end: TextInputEditText
    private lateinit var locationRadio: RadioButton
    private lateinit var startRadio: RadioButton
    private lateinit var returnInt: DialogReturn
    private var userLocation: Boolean = true

    fun newInstance(title: String): LocationDialog {
        val frag = LocationDialog()
        val args = Bundle()
        args.putString("title", title)
        frag.setArguments(args)
        return frag
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        dialogView = inflater.inflate(R.layout.location_dialog, container, false)

        start = dialogView.findViewById(R.id.start)
        end = dialogView.findViewById(R.id.end)

        if (ActivityCompat.checkSelfPermission(
                context!!,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            dialogView.findViewById<TableRow>(R.id.my_location_row).visibility = View.GONE
            dialogView.findViewById<RadioButton>(R.id.start_location).visibility = View.INVISIBLE
        }

        locationRadio = dialogView.findViewById<RadioButton>(R.id.location)
        locationRadio.setOnClickListener({
            startRadio.setChecked(false)
            start.setEnabled(false)
            userLocation = true
        })
        startRadio = dialogView.findViewById<RadioButton>(R.id.start_location)
        startRadio.setOnClickListener({
            locationRadio.setChecked(false)
            start.setEnabled(true)
            userLocation = false
        })

        dialogView.findViewById<Button>(R.id.cancel_button)!!.setOnClickListener({
            dismiss()
        })
        dialogView.findViewById<Button>(R.id.ok_button)!!.setOnClickListener({
            doChecks()
        })

        return dialogView
    }

    fun setupReturn(ret: DialogReturn) {
        returnInt = ret
    }

    fun doChecks() {
        var startLocation: LatLng? = null
        var endLocation: LatLng? = null

        var checksComplete = true
        if (!userLocation) {
            startLocation = lookupLocation(start.text.toString())
            if (startLocation == null) {
                checksComplete = false
            }
        }
        endLocation = lookupLocation(end.text.toString())
        if (endLocation == null) {
            checksComplete = false
        }

        if (checksComplete) {
            returnInt.onFinishDialog(userLocation, startLocation, endLocation)
            dismiss()
        }
    }

    fun lookupLocation(address: String): LatLng? {
        var geocodeMatches: List<Address>? = null

        try {
            geocodeMatches = Geocoder(context).getFromLocationName(
                address, 1)
        } catch (e: IOException) {
            e.printStackTrace()
        }

        if (geocodeMatches != null && geocodeMatches.size > 0) {
            return LatLng(geocodeMatches[0].latitude, geocodeMatches[0].longitude)
        }
        Toast.makeText(context, String.format("Address for '%s' not found", address),Toast.LENGTH_LONG).show()
        return null
    }
}
