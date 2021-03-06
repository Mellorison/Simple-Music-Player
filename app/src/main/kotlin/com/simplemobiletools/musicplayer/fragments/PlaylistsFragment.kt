package com.simplemobiletools.musicplayer.fragments

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import com.google.gson.Gson
import com.simplemobiletools.commons.adapters.MyRecyclerViewAdapter
import com.simplemobiletools.commons.extensions.beGoneIf
import com.simplemobiletools.commons.extensions.beVisibleIf
import com.simplemobiletools.commons.helpers.ensureBackgroundThread
import com.simplemobiletools.musicplayer.activities.SimpleActivity
import com.simplemobiletools.musicplayer.activities.TracksActivity
import com.simplemobiletools.musicplayer.adapters.PlaylistsAdapter
import com.simplemobiletools.musicplayer.dialogs.ChangeSortingDialog
import com.simplemobiletools.musicplayer.extensions.config
import com.simplemobiletools.musicplayer.extensions.playlistDAO
import com.simplemobiletools.musicplayer.extensions.tracksDAO
import com.simplemobiletools.musicplayer.helpers.PLAYLIST
import com.simplemobiletools.musicplayer.helpers.TAB_PLAYLISTS
import com.simplemobiletools.musicplayer.models.Playlist
import kotlinx.android.synthetic.main.fragment_playlists.view.*

class PlaylistsFragment(context: Context, attributeSet: AttributeSet) : MyViewPagerFragment(context, attributeSet) {
    private var playlistsIgnoringSearch = ArrayList<Playlist>()

    override fun setupFragment(activity: SimpleActivity) {
        ensureBackgroundThread {
            var playlists = activity.playlistDAO.getAll() as ArrayList<Playlist>
            playlists.forEach {
                it.trackCnt = activity.tracksDAO.getTracksCountFromPlaylist(it.id)
            }
            playlists = playlists.filter { it.trackCnt != 0 }.toMutableList() as ArrayList<Playlist>
            Playlist.sorting = activity.config.playlistSorting
            playlists.sort()

            activity.runOnUiThread {
                playlists_placeholder.beVisibleIf(playlists.isEmpty())
                playlists_placeholder_2.beVisibleIf(playlists.isEmpty())
                val adapter = PlaylistsAdapter(activity, playlists, playlists_list, playlists_fastscroller) {
                    Intent(activity, TracksActivity::class.java).apply {
                        putExtra(PLAYLIST, Gson().toJson(it))
                        activity.startActivity(this)
                    }
                }.apply {
                    playlists_list.adapter = this
                }

                playlists_fastscroller.setViews(playlists_list) {
                    val playlist = adapter.playlists.getOrNull(it)
                    playlists_fastscroller.updateBubbleText(playlist?.getBubbleText() ?: "")
                }
            }
        }

        playlists_fastscroller.updatePrimaryColor()
        playlists_fastscroller.updateBubbleColors()
    }

    override fun finishActMode() {
        (playlists_list.adapter as? MyRecyclerViewAdapter)?.finishActMode()
    }

    override fun onSearchQueryChanged(text: String) {
        val filtered = playlistsIgnoringSearch.filter { it.title.contains(text, true) }.toMutableList() as ArrayList<Playlist>
        (playlists_list.adapter as? PlaylistsAdapter)?.updateItems(filtered, text)
        playlists_placeholder.beVisibleIf(filtered.isEmpty())
        playlists_placeholder_2.beVisibleIf(filtered.isEmpty())
    }

    override fun onSearchOpened() {
        playlistsIgnoringSearch = (playlists_list?.adapter as? PlaylistsAdapter)?.playlists ?: ArrayList()
    }

    override fun onSearchClosed() {
        (playlists_list.adapter as? PlaylistsAdapter)?.updateItems(playlistsIgnoringSearch)
        playlists_placeholder.beGoneIf(playlistsIgnoringSearch.isNotEmpty())
        playlists_placeholder_2.beGoneIf(playlistsIgnoringSearch.isNotEmpty())
    }

    override fun onSortOpen(activity: SimpleActivity) {
        ChangeSortingDialog(activity, TAB_PLAYLISTS) {
            val adapter = playlists_list.adapter as? PlaylistsAdapter ?: return@ChangeSortingDialog
            val playlists = adapter.playlists
            Playlist.sorting = activity.config.playlistSorting
            playlists.sort()
            adapter.updateItems(playlists, forceUpdate = true)
        }
    }
}
