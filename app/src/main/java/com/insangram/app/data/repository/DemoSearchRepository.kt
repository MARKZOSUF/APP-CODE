package com.insangram.app.data.repository

import com.insangram.app.core.common.InsangramResult
import com.insangram.app.domain.model.HashtagSummary
import com.insangram.app.domain.model.Post
import com.insangram.app.domain.model.RecentSearch
import com.insangram.app.domain.model.SearchResults
import com.insangram.app.domain.repository.SearchRepository
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/*
 * Demo-flavor implementation.
 *
 * The demo build ships without a backend, so this repository satisfies the
 * domain contract locally. Read paths expose empty-but-valid streams instead of
 * fabricated content, and unsupported write paths return
 * InsangramError.Unsupported so the UI can show an honest message rather than
 * silently pretending the action succeeded.
 */
@Singleton
class DemoSearchRepository @Inject constructor() : SearchRepository {

    /** Recent searches are fully functional in demo mode - they are local data. */
    private val recentSearches = MutableStateFlow<List<RecentSearch>>(emptyList())

    override suspend fun search(query: String): InsangramResult<SearchResults> =
        InsangramResult.Success(SearchResults())

    override fun observeRecentSearches(): Flow<List<RecentSearch>> = recentSearches.asStateFlow()

    override suspend fun recordSearch(term: String, kind: String): InsangramResult<Unit> {
        val trimmed = term.trim()
        if (trimmed.isEmpty()) return InsangramResult.Success(Unit)

        val entry = RecentSearch(
            id = UUID.randomUUID().toString(),
            term = trimmed,
            kind = kind,
            searchedAt = System.currentTimeMillis(),
        )
        // De-duplicate on term, newest first, and cap the history length.
        recentSearches.value = (listOf(entry) + recentSearches.value)
            .distinctBy { it.term.lowercase() }
            .take(MAX_RECENT_SEARCHES)
        return InsangramResult.Success(Unit)
    }

    override suspend fun deleteRecentSearch(id: String): InsangramResult<Unit> {
        recentSearches.value = recentSearches.value.filterNot { it.id == id }
        return InsangramResult.Success(Unit)
    }

    override suspend fun clearSearchHistory(): InsangramResult<Unit> {
        recentSearches.value = emptyList()
        return InsangramResult.Success(Unit)
    }

    override suspend fun trendingHashtags(limit: Int): InsangramResult<List<HashtagSummary>> =
        InsangramResult.Success(emptyList())

    override suspend fun postsForHashtag(tag: String, limit: Int): InsangramResult<List<Post>> =
        InsangramResult.Success(emptyList())

    private companion object {
        const val MAX_RECENT_SEARCHES = 20
    }
}
