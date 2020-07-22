package com.google.codelab.currentplace.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.google.codelab.currentplace.R
import com.google.codelab.currentplace.models.PlaceWrapper
import com.google.codelab.currentplace.utils.DebugLog
import kotlinx.android.synthetic.main.list_item_search.view.*
import java.util.*


class SearchListAdapter(var searchList: ArrayList<PlaceWrapper>) :
        RecyclerView.Adapter<SearchListAdapter.BaseViewHolder?>() {

    interface OnItemClickListener {
        fun onItemClicked(place: PlaceWrapper)
    }

    companion object {
        const val VIEW_TYPE_LOADING = 0
        const val VIEW_TYPE_NORMAL = 1
    }

    var onItemClickListener: OnItemClickListener? = null
    private var isLoaderVisible = false

    fun updateItems(places: List<PlaceWrapper>) {
        searchList.addAll(places)
        Log.d("", "--> size: ${searchList.size}")
        notifyDataSetChanged()
    }

    override fun getItemCount() = searchList.size

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == searchList.size - 1 && isLoaderVisible) {
            VIEW_TYPE_LOADING
        } else {
            VIEW_TYPE_NORMAL
        }
    }

    fun addLoading() {
        isLoaderVisible = true
        notifyDataSetChanged()
    }

    fun removeLoading() {
        isLoaderVisible = false
        notifyDataSetChanged()
    }

    fun clear() {
        searchList.clear()
        notifyDataSetChanged()
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        DebugLog.d("--> $viewType")
        return when (viewType) {
            VIEW_TYPE_NORMAL -> SearchItemHolder(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.list_item_search,
                    parent,
                    false
                )
            )
           /* VIEW_TYPE_LOADING -> ProgressHolder(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.item_loading,
                    parent,
                    false
                )
            )*/
            else -> {
                DebugLog.e("Appropriate view not found for the type $viewType. This condition should not be reached.")
                SearchItemHolder(
                    LayoutInflater.from(parent.context).inflate(
                        R.layout.list_item_search,
                        parent,
                        false
                    )
                )
            }
        }
    }

    class ProgressHolder internal constructor(itemView: View?) :
        BaseViewHolder(itemView) {
        override fun bind(position: Int) {
            // for future extensability.
        }
    }

    inner class SearchItemHolder(private val view: View) : BaseViewHolder(view),
        View.OnClickListener {
        init {
            itemView.plankItemView.setOnClickListener(this)
        }

        override fun bind(position: Int) {
            val item = searchList[position]
            itemView.itemName.text = item.placeName
        }

        override fun onClick(v: View) {
            onItemClickListener?.onItemClicked(searchList[adapterPosition])
        }
    }

    abstract class BaseViewHolder(itemView: View?) :
        ViewHolder(itemView!!) {

        open fun bind(position: Int) {

        }

    }

}
