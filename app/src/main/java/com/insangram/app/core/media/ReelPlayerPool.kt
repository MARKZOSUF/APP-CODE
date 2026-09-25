package com.insangram.app.core.media

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.insangram.app.core.common.InsangramConstants
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A small, bounded pool of ExoPlayer instances for the reels pager.
 *
 * Creating one player per page leaks memory and decoders quickly, so at most
 * [InsangramConstants.MAX_CONCURRENT_PLAYERS] players exist and they are reused
 * as pages scroll. Media is cached on disk with an LRU evictor so replaying a
 * reel does not re-download it.
 */
@OptIn(UnstableApi::class)
@Singleton
class ReelPlayerPool @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val players = mutableMapOf<String, ExoPlayer>()
    private val order = ArrayDeque<String>()

    private val cache: SimpleCache by lazy {
        SimpleCache(
            File(context.cacheDir, "insangram_video_cache"),
            LeastRecentlyUsedCacheEvictor(InsangramConstants.VIDEO_CACHE_BYTES),
            StandaloneDatabaseProvider(context),
        )
    }

    private val mediaSourceFactory: DefaultMediaSourceFactory by lazy {
        val upstream = DefaultDataSource.Factory(context)
        val cacheFactory = CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(upstream)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
        DefaultMediaSourceFactory(cacheFactory)
    }

    /** Returns a prepared, paused player for [key], evicting the oldest if needed. */
    fun acquire(key: String, uri: String): ExoPlayer {
        players[key]?.let {
            order.remove(key)
            order.addLast(key)
            return it
        }
        while (order.size >= InsangramConstants.MAX_CONCURRENT_PLAYERS) {
            order.removeFirstOrNull()?.let { release(it) }
        }
        val player = ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()
            .apply {
                repeatMode = Player.REPEAT_MODE_ONE
                setMediaItem(MediaItem.fromUri(uri))
                playWhenReady = false
                prepare()
            }
        players[key] = player
        order.addLast(key)
        return player
    }

    fun play(key: String) {
        players.forEach { (id, player) -> player.playWhenReady = id == key }
    }

    fun pauseAll() {
        players.values.forEach { it.playWhenReady = false }
    }

    fun setMuted(muted: Boolean) {
        players.values.forEach { it.volume = if (muted) 0f else 1f }
    }

    fun release(key: String) {
        players.remove(key)?.release()
        order.remove(key)
    }

    /** Called when the reels screen leaves the composition or app backgrounds. */
    fun releaseAll() {
        players.values.forEach { it.release() }
        players.clear()
        order.clear()
    }
}
