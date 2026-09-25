package com.insangram.app.core.common

/**
 * A result wrapper used across every layer boundary. Repository methods never
 * throw; they map failures to [InsangramResult.Failure] with a domain-level
 * [InsangramError] produced by [ErrorMapper].
 */
sealed interface InsangramResult<out T> {
    data class Success<out T>(val data: T) : InsangramResult<T>
    data class Failure(val error: InsangramError) : InsangramResult<Nothing>

    val dataOrNull: T? get() = (this as? Success)?.data
    val isSuccess: Boolean get() = this is Success

    fun <R> map(transform: (T) -> R): InsangramResult<R> = when (this) {
        is Success -> Success(transform(data))
        is Failure -> this
    }

    fun onSuccess(action: (T) -> Unit): InsangramResult<T> {
        if (this is Success) action(data)
        return this
    }

    fun onFailure(action: (InsangramError) -> Unit): InsangramResult<T> {
        if (this is Failure) action(error)
        return this
    }
}

fun <T> T.asSuccess(): InsangramResult<T> = InsangramResult.Success(this)
fun InsangramError.asFailure(): InsangramResult<Nothing> = InsangramResult.Failure(this)

/**
 * Runs [block], mapping any thrown exception through [ErrorMapper] instead of
 * letting it escape into the ViewModel layer.
 */
suspend inline fun <T> runCatchingInsangram(
    mapper: ErrorMapper,
    crossinline block: suspend () -> T,
): InsangramResult<T> = try {
    InsangramResult.Success(block())
} catch (cancellation: kotlinx.coroutines.CancellationException) {
    throw cancellation
} catch (throwable: Throwable) {
    InsangramResult.Failure(mapper.map(throwable))
}
