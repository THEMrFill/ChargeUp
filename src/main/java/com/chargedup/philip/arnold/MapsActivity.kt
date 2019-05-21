package com.chargedup.philip.arnold

import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.maps.android.PolyUtil
import org.json.JSONObject

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, DialogReturn {
    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var lastLocation: Location
    private lateinit var fab: FloatingActionButton

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
        private const val ROUTE_LOOKUP_URL = "https://maps.googleapis.com/maps/api/directions/json?origin=%s&destination=%s&key=%s"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        findViewById<FloatingActionButton>(R.id.fab).setOnClickListener({
            searchDialog()
        })
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        // check if we're returning from the location permissions,
        // this if() is for future expansion
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            // loop through the permissions, just in case multiple are returned
            for (i in 0 until permissions.size - 1) {
                // is current permission for location?
                if (permissions.get(i).equals(android.Manifest.permission.ACCESS_FINE_LOCATION)) {
                    // if it's granted, then we go to the permissions setup again
                    if (grantResults[i] == 0) {
                        requestPermissions()
                    }
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        // setup the map location to be London and move the camera there
        val london = LatLng(51.5, -0.15)
        map.moveCamera(CameraUpdateFactory.newLatLng(london))
        map.getUiSettings().setZoomControlsEnabled(true)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(london, 12.0f))
        // check if the user has given location permissions
        requestPermissions()
    }

    // permissions & location setup on the map
    private fun requestPermissions() {
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE
            )
            return
        }
        // if we have permission, turn on user location & move the camera to the user's location (if available)
        map.isMyLocationEnabled = true
        fusedLocationClient.lastLocation.addOnSuccessListener(this) { location ->
            // Got last known location. This can occasionally be null.
            // Note that "last known" could be wildly incorrect, but it's the fastest to get if we're using the map.
            if (location != null) {
                lastLocation = location
                val currentLatLng = LatLng(location.latitude, location.longitude)
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 12f))
            }
        }
    }

    private fun searchDialog() {
        val fm = supportFragmentManager
        val dialogFragment = LocationDialog().newInstance("title")
        dialogFragment.setupReturn(this)
        dialogFragment.show(fm, "dialog")
    }

    override fun onFinishDialog(userLocation: Boolean, start: LatLng?, end: LatLng?) {
        val path: MutableList<List<LatLng>> = ArrayList()
        var startPos = start
        if (userLocation) {
            startPos = LatLng(lastLocation.latitude, lastLocation.longitude)
        }
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(startPos, 12f))
        val urlDirections = String.format(ROUTE_LOOKUP_URL, locationFormat(startPos), locationFormat(end), getString(R.string.google_maps_key))
        val directionsRequest = object : StringRequest(Request.Method.GET, urlDirections, Response.Listener<String> {
                response ->
            val jsonResponse = JSONObject(response)
            // Get routes
            val routes = jsonResponse.getJSONArray("routes")
            if (routes.length() > 0) {
                this.map.clear()

                val legs = routes.getJSONObject(0).getJSONArray("legs")
                val steps = legs.getJSONObject(0).getJSONArray("steps")
                for (i in 0 until steps.length()) {
                    val points = steps.getJSONObject(i).getJSONObject("polyline").getString("points")
                    path.add(PolyUtil.decode(points))
                }
                for (i in 0 until path.size) {
                    this.map.addPolyline(PolylineOptions().addAll(path[i]).color(Color.RED))
                }
            } else {
                val builder = AlertDialog.Builder(this@MapsActivity)
                builder.setMessage(R.string.no_routes_found)
                builder.setNeutralButton(R.string.ok){_,_ -> }// do nothing so the dialog is dismissed
                val dialog: AlertDialog = builder.create()
                dialog.show()
            }
        }, Response.ErrorListener {
                _ ->
        }){}
        val requestQueue = Volley.newRequestQueue(this)
        requestQueue.add(directionsRequest)
    }

    fun locationFormat(latlng: LatLng?): String {
        return String.format("%f,%f", latlng!!.latitude, latlng.longitude)
    }
}
