package net.reconburrito.tvbrowser.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import net.reconburrito.tvbrowser.R
import net.reconburrito.tvbrowser.data.Bookmark

class BookmarkGridAdapter(private val ctx: Context) : BaseAdapter() {
    private var items: List<Bookmark> = emptyList()

    fun submit(list: List<Bookmark>) {
        items = list
        notifyDataSetChanged()
    }

    override fun getCount(): Int = items.size
    override fun getItem(position: Int): Bookmark = items[position]
    override fun getItemId(position: Int): Long = items[position].id

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val v = convertView ?: LayoutInflater.from(ctx).inflate(R.layout.item_bookmark, parent, false)
        val b = items[position]
        v.findViewById<TextView>(R.id.item_title).text = b.title
        v.findViewById<TextView>(R.id.item_url).text = b.url.removePrefix("https://").removePrefix("http://")
        return v
    }
}
