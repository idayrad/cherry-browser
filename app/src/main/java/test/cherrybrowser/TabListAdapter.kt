package test.cherrybrowser

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/**
 * RecyclerView adapter for the tab list.
 * Shows title and url, and a close button. No thumbnails.
 */
class TabListAdapter(
    private var items: MutableList<BrowserTab>,
    private val onSelect: (BrowserTab) -> Unit,
    private val onClose: (BrowserTab) -> Unit
) : RecyclerView.Adapter<TabListAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.tab_title)
        val url: TextView = view.findViewById(R.id.tab_url)
        val btnClose: ImageButton = view.findViewById(R.id.btn_close)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_tab, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val t = items[position]
        holder.title.text = t.title ?: t.url
        holder.url.text = t.url
        holder.itemView.setOnClickListener { onSelect(t) }
        holder.btnClose.setOnClickListener {
            onClose(t)
        }
    }

    override fun getItemCount(): Int = items.size

    fun update(newItems: List<BrowserTab>) {
        items = newItems.toMutableList()
        notifyDataSetChanged()
    }

    fun remove(tab: BrowserTab) {
        val idx = items.indexOfFirst { it.id == tab.id }
        if (idx >= 0) {
            items.removeAt(idx)
            notifyItemRemoved(idx)
        }
    }
}
