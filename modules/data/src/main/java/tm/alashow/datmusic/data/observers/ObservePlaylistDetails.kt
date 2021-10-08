/*
 * Copyright (C) 2021, Alashov Berkeli
 * All rights reserved.
 */
package tm.alashow.datmusic.data.observers

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import tm.alashow.data.SubjectInteractor
import tm.alashow.datmusic.data.repos.playlist.PlaylistsRepo
import tm.alashow.datmusic.domain.entities.Playlist
import tm.alashow.datmusic.domain.entities.PlaylistId
import tm.alashow.datmusic.domain.entities.PlaylistWithAudios
import tm.alashow.domain.models.Async
import tm.alashow.domain.models.Fail
import tm.alashow.domain.models.Loading
import tm.alashow.domain.models.Success

class ObservePlaylist @Inject constructor(
    private val playlistsRepo: PlaylistsRepo
) : SubjectInteractor<PlaylistId, Playlist>() {
    override fun createObservable(params: PlaylistId): Flow<Playlist> = playlistsRepo.playlist(params)
}

class ObservePlaylistDetails @Inject constructor(
    private val playlistsRepo: PlaylistsRepo
) : SubjectInteractor<PlaylistId, Async<PlaylistWithAudios>>() {

    override fun createObservable(params: PlaylistId) = flow {
        emit(Loading())
        playlistsRepo.playlistWithAudios(params)
            .catch { error -> emit(Fail<PlaylistWithAudios>(error)) }
            .collect { emit(Success(it)) }
    }
}
