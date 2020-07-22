// Copyright 2019 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.google.codelab.currentplace

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.volley.RequestQueue
import com.android.volley.toolbox.Volley
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.*
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FetchPlaceResponse
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.codelab.currentplace.adapters.SearchListAdapter
import com.google.codelab.currentplace.fragments.ChooseLocationFragment
import com.google.codelab.currentplace.models.PlaceWrapper
import com.google.codelab.currentplace.utils.DebugLog
import kotlinx.android.synthetic.main.list_with_search_bar.*
import java.util.*

class SearchActivity : AppCompatActivity() {

    private val searchAdapter = SearchListAdapter(ArrayList())
    private val handler: Handler by lazy { Handler() }
    private val placesClient: PlacesClient by lazy { Places.createClient(this) }
    private val queue: RequestQueue by lazy { Volley.newRequestQueue(this) }
    private var sessionToken: AutocompleteSessionToken? = null
    private var isLoading = false

    companion object {
        const val EXTRA_LOCATION = "com.maps.location.extras.place"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.list_with_search_bar)

        recyclerView.adapter = searchAdapter
        val layoutManager = LinearLayoutManager(this)
        val decoration = DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        /*val drawable = ContextCompat.getDrawable(activity!!, R.drawable.divider_search_result)
        if (drawable != null) {
            decoration.setDrawable(drawable)
        }*/
        recyclerView.addItemDecoration(decoration)
        recyclerView.layoutManager = layoutManager

        initSearchView()
        sessionToken = AutocompleteSessionToken.newInstance()
        // showList()
        changeSearchViewTextColor(searchView)

        searchAdapter.onItemClickListener = onItemClickListener
    }

    private val onLocationChosenListener = object : ChooseLocationFragment.OnButtonClickListener {
        override fun onPositiveClicked(place: Place) {
            val intent = Intent()
            intent.putExtra(EXTRA_LOCATION, place)
            setResult(Activity.RESULT_OK, intent)
            finish()
        }

        override fun onNegativeClicked() {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }
    }

    private val onItemClickListener = object : SearchListAdapter.OnItemClickListener {
        override fun onItemClicked(place: PlaceWrapper) {
            Log.d("onItemClicked", "onItemClicked clicked")
            fetchPlace(place.placeId)
        }
    }

    private fun changeSearchViewTextColor(view: View?) {
        if (view != null) {
            if (view is TextView) {
                view.setTextColor(Color.BLACK)
                return
            } else if (view is ViewGroup) {
                for (i in 0 until view.childCount) {
                    changeSearchViewTextColor(view.getChildAt(i))
                }
            }
        }
    }

    private fun initSearchView() {
        searchView?.onActionViewExpanded()
        searchView?.maxWidth = Integer.MAX_VALUE
        searchView?.setIconifiedByDefault(false)
        searchView?.setOnQueryTextListener(observer)
    }

    private val observer = object : SearchView.OnQueryTextListener {
        override fun onQueryTextSubmit(query: String?): Boolean {
            return false
        }

        override fun onQueryTextChange(newText: String?): Boolean {

            // TODO: check internet
            /*if (activity != null &&
                    !NetworkUtils.isInternetAvailable(activity as FragmentActivity)
            ) {
                // TODO: show some message
                showSnackbar(getString(R.string.msg_no_internet))
            }*/
            // clear the adapter data as the query has changed
            searchAdapter.clear()
            emptyText.visibility = View.GONE
            performSearch(newText)
            return true
        }
    }

    private fun performSearch(newText: String?) {
        val searchQuery = newText?.trim()
        if (searchQuery == null || searchQuery.isEmpty()
                || searchQuery.length <= 1
        ) {
            DebugLog.w("search query is null, empty or 1. Returning")
            return
        }

        DebugLog.d("--> searchQuery $searchQuery")
        isLoading = true
        DebugLog.d("--> loading $isLoading")

        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({
            getPlaces(searchQuery)
        }, 300)
    }

    private fun getPlaces(query: String) {
        // The value of 'bias' biases prediction results to the rectangular region provided
        // (currently Kolkata). Modify these values to get results for another area. Make sure to
        // pass in the appropriate value/s for .setCountries() in the
        // FindAutocompletePredictionsRequest.Builder object as well.
        val bias: LocationBias = RectangularBounds.newInstance(
                LatLng(22.458744, 88.208162),  // SW lat, lng
                LatLng(22.730671, 88.524896) // NE lat, lng
        )

        val countries = Locale.getISOCountries()?.toMutableList() ?: mutableListOf()

        // Create a new programmatic Place Autocomplete request in Places SDK for Android
        val newRequest = FindAutocompletePredictionsRequest
                .builder()
                .setSessionToken(sessionToken)
                // .setLocationBias(bias)
                .setTypeFilter(TypeFilter.ESTABLISHMENT)
                .setCountries(countries)
                .setQuery(query)
                .setCountries()
                .build()

        // Perform autocomplete predictions request
        placesClient.findAutocompletePredictions(newRequest).addOnSuccessListener { response ->
            val predictions = response.autocompletePredictions
            val list = mutableListOf<PlaceWrapper>()
            for (place in predictions) {
                list.add(PlaceWrapper(place.placeId, place.getPrimaryText(null).toString()))
            }
            searchAdapter.updateItems(list)
            progressBar.isIndeterminate = false
        }.addOnFailureListener { exception: Exception? ->
            progressBar.isIndeterminate = false
            if (exception is ApiException) {
                DebugLog.e("Place not found: ${exception.statusCode}")
            }
        }
    }

    private fun showDialog(place: Place) {
        val addressShow = ChooseLocationFragment()
        addressShow.place = place
        addressShow.onButtonClickListener = onLocationChosenListener
        addressShow.show(
                supportFragmentManager,
                "add_new_address_dialog"
        )
    }

    private fun fetchPlace(placeId: String) {
        // Specify the fields to return.
        val placeFields: List<Place.Field> = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.WEBSITE_URI)

        // Construct a request object, passing the place ID and fields array.
        val request: FetchPlaceRequest = FetchPlaceRequest.newInstance(placeId, placeFields)

        placesClient.fetchPlace(request).addOnSuccessListener { response: FetchPlaceResponse ->
            val place: Place = response.place
            Log.i("FragmentActivity", "Place found: " + place.name)
            Log.i("FragmentActivity", "Place lat-lang: ${place.latLng?.latitude} ${place.latLng?.longitude}")
            showDialog(place)
        }.addOnFailureListener { exception: java.lang.Exception ->
            if (exception is ApiException) {
                val apiException = exception as ApiException
                Log.i("FragmentActivity", "Place not found: " + exception.message)
                val statusCode = apiException.statusCode
                // TODO: Handle error with given status code.
            }
        }
    }

}