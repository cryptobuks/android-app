package one.mixin.android.ui.conversation

import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import kotlinx.android.synthetic.main.fragment_gallery_album.*
import one.mixin.android.R
import one.mixin.android.ui.conversation.adapter.GalleryAlbumAdapter
import one.mixin.android.ui.conversation.adapter.GalleryCallback
import one.mixin.android.widget.gallery.internal.entity.Album
import one.mixin.android.widget.gallery.internal.model.AlbumCollection

class GalleryAlbumFragment: Fragment(), AlbumCollection.AlbumCallbacks {

    companion object {
        const val TAG = "GalleryAlbumFragment"

        const val POS_CONTENT = 0
        const val POS_LOADING = 1

        fun newInstance() = GalleryAlbumFragment()
    }

    var callback: GalleryCallback? = null

    private val albumCollection = AlbumCollection()

    private val albumAdapter: GalleryAlbumAdapter by lazy {
        GalleryAlbumAdapter(requireContext(), childFragmentManager)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        layoutInflater.inflate(R.layout.fragment_gallery_album, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        view_pager.adapter = albumAdapter
        album_tl.setupWithViewPager(view_pager)
        album_tl.tabMode = TabLayout.MODE_SCROLLABLE
        view_pager.currentItem = 0
        va.displayedChild = POS_LOADING
        albumAdapter.callback = object : GalleryCallback {
            override fun onItemClick(pos: Int, uri: Uri) {
                callback?.onItemClick(pos, uri)
            }

            override fun onCameraClick() {
                callback?.onCameraClick()
            }
        }
        view_pager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
            }

            override fun onPageSelected(position: Int) {
            }

            override fun onPageScrollStateChanged(state: Int) {
                if (state != ViewPager.SCROLL_STATE_IDLE) {
                    albumAdapter.getFragment(view_pager.currentItem)?.hideBlur()
                }
            }
        })

        albumCollection.onCreate(this, this)
        albumCollection.onRestoreInstanceState(savedInstanceState)
        albumCollection.loadAlbums()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        albumCollection.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        super.onDestroy()
        albumCollection.onDestroy()
    }

    override fun onAlbumLoad(cursor: Cursor) {
        va?.post {
            val albums = arrayListOf<Album>()
            va.displayedChild = POS_CONTENT
            while (cursor.moveToNext()) {
                val album = Album.valueOf(cursor)
                albums.add(album)
                album_tl.addTab(album_tl.newTab().setText(album.getDisplayName(requireContext())))
            }
            albumAdapter.albums = albums
        }

    }

    override fun onAlbumReset() {
    }
}