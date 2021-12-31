/*
 * Copyright (C) 2021, Alashov Berkeli
 * All rights reserved.
 */
package tm.alashow.datmusic.downloader.observers

import com.tonyodev.fetch2.Fetch
import com.tonyodev.fetch2.Status
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import tm.alashow.data.SubjectInteractor
import tm.alashow.datmusic.data.db.daos.AudiosFtsDao
import tm.alashow.datmusic.data.db.daos.DownloadRequestsDao
import tm.alashow.datmusic.domain.entities.AudioDownloadItem
import tm.alashow.datmusic.domain.entities.DownloadRequest
import tm.alashow.datmusic.downloader.DownloadItems
import tm.alashow.datmusic.downloader.Downloader
import tm.alashow.datmusic.downloader.downloads
import tm.alashow.domain.models.Async
import tm.alashow.domain.models.Fail
import tm.alashow.domain.models.Success

class ObserveDownloads @Inject constructor(
    private val fetcher: Fetch,
    private val dao: DownloadRequestsDao,
    private val audiosFtsDao: AudiosFtsDao,
) : SubjectInteractor<ObserveDownloads.Params, DownloadItems>() {

    data class Params(
        val query: String = "",
        val audiosSortOptions: List<DownloadAudioItemSortOption> = DownloadAudioItemSortOptions.ALL,
        val defaultSortOption: DownloadAudioItemSortOption = DownloadAudioItemSortOptions.ALL.first(),
        val audiosSortOption: DownloadAudioItemSortOption = defaultSortOption,
        val defaultStatusFilters: Set<DownloadStatusFilter> = setOf(DownloadStatusFilter.All),
        val statusFilters: Set<DownloadStatusFilter> = defaultStatusFilters,
    ) {
        val hasQuery get() = query.isNotBlank()
        val hasSortingOption get() = audiosSortOption != defaultSortOption
        val hasStatusFilter get() = statusFilters != defaultStatusFilters
        val hasNoFilters get() = !hasQuery && !hasSortingOption && !hasStatusFilter

        val statuses get() = statusFilters.map { it.statuses }.flatten()
    }

    private fun fetcherDownloads(downloadRequests: List<DownloadRequest> = emptyList(), statuses: List<Status>) = flow {
        val requestsById = downloadRequests.associateBy { it.requestId }
        while (true) {
            fetcher.downloads(ids = requestsById.keys, statuses = statuses)
                .map { requestsById.getValue(it.id) to it }
                .also { emit(it) }
            delay(Downloader.DOWNLOADS_STATUS_REFRESH_INTERVAL)
        }
    }.distinctUntilChanged()

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun createObservable(params: Params): Flow<DownloadItems> {
        val downloadsRequestsFlow = when {
            params.hasQuery -> audiosFtsDao.searchDownloads("*${params.query}*")
            else -> dao.entries()
        }

        return downloadsRequestsFlow.flatMapLatest { fetcherDownloads(it, params.statuses) }
            .map {
                val audioDownloads = it.filter { pair -> pair.first.entityType == DownloadRequest.Type.Audio }
                    .map { (request, info) ->
                        AudioDownloadItem.from(request, request.audio, info)
                    }.let { items ->
                        val comparator = params.audiosSortOption.comparator
                        if (comparator != null) items.sortedWith(comparator)
                        else items
                    }
                DownloadItems(audioDownloads)
            }
    }
}

fun Async<DownloadItems>.failWithNoResultsIfEmpty(params: ObserveDownloads.Params) = let {
    if (it is Success) {
        if (it().audios.isEmpty() && !params.hasNoFilters)
            Fail(NoResultsForDownloadsFilter(params))
        else it
    } else it
}
