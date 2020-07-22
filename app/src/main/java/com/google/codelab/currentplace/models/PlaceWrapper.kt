package com.google.codelab.currentplace.models

import android.os.Parcel
import android.os.Parcelable

data class PlaceWrapper(
        val placeId: String,
        val placeName: String
) : Parcelable {

    companion object {
        @JvmField
        val CREATOR = object : Parcelable.Creator<PlaceWrapper> {
            override fun createFromParcel(parcel: Parcel) = PlaceWrapper(parcel)
            override fun newArray(size: Int) = arrayOfNulls<PlaceWrapper>(size)
        }
    }

    private constructor(parcel: Parcel) : this(
            placeId = parcel.readString(),
            placeName = parcel.readString()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(placeId)
        parcel.writeString(placeName)
    }

    override fun describeContents() = 0
}