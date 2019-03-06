package one.mixin.android.ui.search

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersAdapter
import one.mixin.android.R
import one.mixin.android.ui.search.holder.AssetHolder
import one.mixin.android.ui.search.holder.ContactHolder
import one.mixin.android.ui.search.holder.GroupHolder
import one.mixin.android.ui.search.holder.HeaderHolder
import one.mixin.android.ui.search.holder.MessageHolder
import one.mixin.android.vo.AssetItem
import one.mixin.android.vo.ConversationItemMinimal
import one.mixin.android.vo.SearchMessageItem
import one.mixin.android.vo.User

class SearchAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>(), StickyRecyclerHeadersAdapter<HeaderHolder> {

    var onItemClickListener: SearchFragment.OnSearchClickListener? = null

    override fun getHeaderId(position: Int): Long = getItemViewType(position).toLong()

    override fun onBindHeaderViewHolder(holder: HeaderHolder, position: Int) {
        val context = holder.itemView.context
        when {
            getItemViewType(position) == 0 -> holder.bind(context.getText(R.string.search_title_assets).toString())
            getItemViewType(position) == 1 -> holder.bind(context.getText(R.string.search_title_contacts).toString())
            getItemViewType(position) == 2 -> holder.bind(context.getText(R.string.search_title_group).toString())
            else -> holder.bind(context.getText(R.string.search_title_messages).toString())
        }
    }

    override fun onCreateHeaderViewHolder(parent: ViewGroup): HeaderHolder {
        val item = LayoutInflater.from(parent.context).inflate(R.layout.item_search_header, parent, false)
        return HeaderHolder(item)
    }

    fun setData(
        assetList: List<AssetItem>?,
        userList: List<User>?,
        groupList: List<ConversationItemMinimal>?,
        messageList: List<SearchMessageItem>?
    ) {
        this.assetList = assetList
        this.userList = userList
        this.groupList = groupList
        this.messageList = messageList
        dataList.clear()
        assetList?.let { dataList.addAll(it) }
        userList?.let { dataList.addAll(it) }
        groupList?.let { dataList.addAll(it) }
        messageList?.let { dataList.addAll(it) }
        notifyDataSetChanged()
    }

    private var dataList = ArrayList<Any>()
    private var assetList: List<AssetItem>? = null
    private var userList: List<User>? = null
    private var groupList: List<ConversationItemMinimal>? = null
    private var messageList: List<SearchMessageItem>? = null

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (getItemViewType(position)) {
            0 -> {
                dataList[position].let {
                    if (position == assetList!!.size - 1) {
                        (holder as AssetHolder).bind(it as AssetItem, onItemClickListener, true)
                    } else {
                        (holder as AssetHolder).bind(it as AssetItem, onItemClickListener)
                    }
                }
            }
            1 -> {
                dataList[position].let {
                    if (position == (assetList?.size ?: 0) + userList!!.size - 1) {
                        (holder as ContactHolder).bind(it as User, onItemClickListener, true)
                    } else {
                        (holder as ContactHolder).bind(it as User, onItemClickListener)
                    }
                }
            }
            2 -> {
                dataList[position].let {
                    (holder as GroupHolder).bind(it as ConversationItemMinimal,
                        onItemClickListener, if (userList != null) {
                        position == (assetList?.size ?: 0) + userList!!.size + groupList!!.size - 1
                    } else {
                        position == (assetList?.size ?: 0) + groupList!!.size - 1
                    })
                }
            }
            3 -> {
                dataList[position].let {
                    (holder as MessageHolder).bind(it as SearchMessageItem, onItemClickListener,
                        !(if (userList != null && groupList != null) {
                            position == (assetList?.size ?: 0) + userList!!.size + groupList!!.size + messageList!!.size - 1
                        } else if (userList != null) {
                            position == (assetList?.size ?: 0) + userList!!.size + messageList!!.size - 1
                        } else if (groupList != null) {
                            position == (assetList?.size ?: 0) + groupList!!.size + messageList!!.size - 1
                        } else {
                            position == (assetList?.size ?: 0) + messageList!!.size - 1
                        }))
                }
            }
        }
    }

    override fun getItemCount(): Int = dataList.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        when (viewType) {
            0 -> {
                val item = LayoutInflater.from(parent.context).inflate(R.layout.item_search_asset, parent, false)
                AssetHolder(item)
            }
            1 -> {
                val item = LayoutInflater.from(parent.context).inflate(R.layout.item_search_contact, parent, false)
                ContactHolder(item)
            }
            2 -> {
                val item = LayoutInflater.from(parent.context).inflate(R.layout.item_search_contact, parent, false)
                GroupHolder(item)
            }
            3 -> {
                val item = LayoutInflater.from(parent.context).inflate(R.layout.item_search_message, parent, false)
                MessageHolder(item)
            }
            else -> {
                val item = LayoutInflater.from(parent.context).inflate(R.layout.item_search_contact, parent, false)
                ContactHolder(item)
            }
        }

    override fun getItemViewType(position: Int): Int = when {
        dataList[position] is AssetItem -> 0
        dataList[position] is User -> 1
        dataList[position] is ConversationItemMinimal -> 2
        else -> 3
    }
}