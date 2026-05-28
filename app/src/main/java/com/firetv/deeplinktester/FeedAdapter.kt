package com.firetv.deeplinktester

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class FeedAdapter(
    private val overrides: DeeplinkOverrides,
    private val launcher: DeeplinkLauncher,
    private val onEdit: (FeedItem, String, Int) -> Unit,
) : RecyclerView.Adapter<FeedAdapter.FeedViewHolder>() {

    private val items = mutableListOf<FeedItem>()

    fun submitList(feedItems: List<FeedItem>) {
        items.clear()
        items.addAll(feedItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeedViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_feed_row, parent, false)
        return FeedViewHolder(view)
    }

    override fun onBindViewHolder(holder: FeedViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class FeedViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val title: TextView = itemView.findViewById(R.id.feedTitle)
        private val urlPreview: TextView = itemView.findViewById(R.id.feedUrlPreview)
        private val openButton: Button = itemView.findViewById(R.id.openButton)
        private val editButton: Button = itemView.findViewById(R.id.editButton)

        fun bind(item: FeedItem) {
            val effectiveUrl = overrides.getUrl(item.id, item.firetvUrl)
            title.text = item.title
            urlPreview.text = effectiveUrl

            listOf(title, openButton, editButton, urlPreview).forEach(::bindFocusHighlight)

            openButton.setOnClickListener {
                val result = launcher.open(effectiveUrl)
                launcher.showResultToast(result)
            }

            editButton.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onEdit(item, effectiveUrl, position)
                }
            }

            openButton.nextFocusUpId = title.id
            editButton.nextFocusUpId = title.id
            title.nextFocusDownId = openButton.id
            urlPreview.nextFocusDownId = openButton.id
            openButton.nextFocusRightId = editButton.id
            editButton.nextFocusLeftId = openButton.id
        }

        private fun bindFocusHighlight(view: View) {
            view.setOnFocusChangeListener { v, hasFocus -> v.isSelected = hasFocus }
        }
    }
}
