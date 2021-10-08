/*
 * Copyright (C) 2021, Alashov Berkeli
 * All rights reserved.
 */
package tm.alashow.datmusic.ui.playlists.detail

import tm.alashow.datmusic.domain.entities.Playlist
import tm.alashow.datmusic.domain.entities.PlaylistWithAudios
import tm.alashow.domain.models.Async
import tm.alashow.domain.models.Uninitialized

data class PlaylistDetailViewState(
    val playlist: Playlist? = null,
    val playlistDetails: Async<PlaylistWithAudios> = Uninitialized
) {
    companion object {
        val Empty = PlaylistDetailViewState()
    }
}
