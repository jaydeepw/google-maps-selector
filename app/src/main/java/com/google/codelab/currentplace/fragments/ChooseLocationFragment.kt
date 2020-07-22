package com.google.codelab.currentplace.fragments

import android.app.Dialog
import android.graphics.Color
import android.util.Log
import android.view.View
import android.widget.ImageView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.FragmentManager
import com.bumptech.glide.Glide
import com.google.android.libraries.places.api.model.Place
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.codelab.currentplace.R

/**
 *
 */
class ChooseLocationFragment : BottomSheetDialogFragment() {

    lateinit var place: Place

    var imageViewMapLocation: ImageView? = null
    var onButtonClickListener: OnButtonClickListener? = null

    interface OnButtonClickListener {
        fun onPositiveClicked(place: Place)
        fun onNegativeClicked()
    }

    override fun setupDialog(dialog: Dialog, style: Int) {
        // set background color of the bottomsheet
        val contentView = View.inflate(context, R.layout.add_new_address, null)
        dialog.setContentView(contentView)
        val params = (contentView.parent as View).layoutParams as CoordinatorLayout.LayoutParams
        (contentView.parent as View).setBackgroundColor(Color.TRANSPARENT)

        // set default state of the bottomsheet to expanded
        val behavior = params.behavior as BottomSheetBehavior
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
        imageViewMapLocation = contentView.findViewById(R.id.imageViewMapLocation)
        showMapPreview(constructUrl())

        val positiveButton = contentView.findViewById<View>(R.id.buttonYes)
        val negativeButton = contentView.findViewById<View>(R.id.buttonNo)
        positiveButton.setOnClickListener {
            onButtonClickListener?.onPositiveClicked(place)
            dismiss()
        }

        negativeButton.setOnClickListener {
            onButtonClickListener?.onNegativeClicked()
            dismiss()
        }
    }

    override fun show(manager: FragmentManager, tag: String?) {
        if (place == null) {
            throw IllegalStateException("place has not be initialized. Please set place by calling .place=value on object of this class.")
        }
        super.show(manager, tag)
    }

    private fun constructUrl(): String {
        val url = "https://maps.googleapis.com/maps/api/staticmap?center=${place?.latLng?.latitude},${place?.latLng?.longitude}&zoom=18&size=400x300&key=${getString(R.string.google_apis_places_and_maps)}"
        Log.d("address ui", "contructurl $url")
        return url
    }

    private fun showMapPreview(url: String) {
        Glide.with(requireActivity())
                .load(url)
                .into(imageViewMapLocation!!)
    }

}