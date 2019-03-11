package one.mixin.android.ui.common.recyclerview

import one.mixin.android.extension.notNullElse

abstract class HeaderFilterAdapter<T> : HeaderAdapter<T>() {

    override fun getItemViewType(position: Int): Int {
        return if (position == TYPE_HEADER && headerView != null && !filtered()) {
            TYPE_HEADER
        } else {
            TYPE_NORMAL
        }
    }

    override fun getItemCount(): Int = notNullElse(data, {
        if (filtered()) it.size else it.size + 1
    }, if (filtered()) 0 else 1)

    override fun getPos(position: Int): Int {
        return if (headerView != null && !filtered()) {
            position - 1
        } else {
            position
        }
    }

    abstract fun filtered(): Boolean
}