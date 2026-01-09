package com.example.apopulis.simulation

import android.util.Log
import com.example.apopulis.model.CreateCommentRequest
import com.example.apopulis.network.RetrofitInstance
import kotlinx.coroutines.*

class CommentSimulationManager {

    private var job: Job? = null

    fun isRunning(): Boolean = job?.isActive == true

    fun stop() {
        job?.cancel()
        job = null
    }

    fun startRegionRandom(
        periodMinutes: Int,
        commentsPerPeriod: Int,
        burstSpacingMs: Long = 800L,
        getCandidateNewsIds: () -> List<String>,
        onEvent: ((Event) -> Unit)? = null
    ) {
        stop()

        val safePeriodMs = (periodMinutes.coerceAtLeast(1) * 60_000L)
        val safeComments = commentsPerPeriod.coerceAtLeast(1)
        val safeSpacing = burstSpacingMs.coerceAtLeast(100L)

        val simulationId = "sim_android_${System.currentTimeMillis()}"

        job = CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            Log.d("SIM", "START simId=$simulationId every=${safePeriodMs}ms count=$safeComments")
            onEvent?.invoke(Event.Started(simulationId))

            while (isActive) {
                val pool = getCandidateNewsIds().distinct()

                if (pool.isEmpty()) {
                    Log.w("SIM", "No candidate news in selected region -> waiting...")
                    onEvent?.invoke(Event.NoCandidates)
                    delay(safePeriodMs)
                    continue
                }

                onEvent?.invoke(Event.BurstStarted(pool.size))

                // Ako ima dovoljno vesti - random bez ponavljanja
                // Ako nema dovoljno -  dozvoli ponavljanje
                val chosenNewsIds: List<String> =
                    if (safeComments <= pool.size) pool.shuffled().take(safeComments)
                    else List(safeComments) { pool.random() }

                for (i in 1..safeComments) {
                    if (!isActive) break

                    val newsId = chosenNewsIds[i - 1]

                    try {
                        onEvent?.invoke(Event.CommentSending(newsId, i, safeComments))

                        val body = CreateCommentRequest(
                            content = CommentTextGenerator.generate(),
                            isSimulated = true,
                            simulationId = simulationId
                        )

                        val resp = RetrofitInstance.commentsApi.createComment(newsId, body)

                        if (!resp.isSuccessful) {
                            val err = resp.errorBody()?.string()
                            Log.e("SIM", "HTTP ${resp.code()} err=$err")
                            onEvent?.invoke(Event.Error(RuntimeException("HTTP ${resp.code()} $err")))
                        } else {
                            onEvent?.invoke(Event.CommentSent(newsId, i, safeComments))
                            Log.d("SIM", "OK sent $i/$safeComments -> news=$newsId simId=$simulationId")
                        }

                    } catch (e: Exception) {
                        onEvent?.invoke(Event.Error(e))
                        Log.e("SIM", "send failed $i/$safeComments: ${e.message}", e)
                    }

                    delay(safeSpacing)
                }

                onEvent?.invoke(Event.BurstFinished)
                delay(safePeriodMs)
            }

            onEvent?.invoke(Event.Stopped)
            Log.d("SIM", "STOP simId=$simulationId")
        }
    }

    sealed class Event {
        data class Started(val simulationId: String) : Event()
        data class BurstStarted(val candidateCount: Int) : Event()
        data class CommentSending(val newsItemId: String, val index: Int, val total: Int) : Event()
        data class CommentSent(val newsItemId: String, val index: Int, val total: Int) : Event()
        data object BurstFinished : Event()
        data object NoCandidates : Event()
        data class Error(val error: Throwable) : Event()
        data object Stopped : Event()
    }
}
