package org.kalpeshbkundanani.burnmate.integration.fit

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.Bucket
import com.google.android.gms.fitness.data.DataPoint
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.data.Field
import com.google.android.gms.fitness.request.DataReadRequest
import com.google.android.gms.tasks.Task
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.roundToInt
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import org.kalpeshbkundanani.burnmate.integration.model.GoogleAccountSession
import org.kalpeshbkundanani.burnmate.integration.model.GoogleIntegrationAvailability
import org.kalpeshbkundanani.burnmate.integration.model.GoogleIntegrationError
import org.kalpeshbkundanani.burnmate.integration.model.ImportedActivitySample
import org.kalpeshbkundanani.burnmate.platform.GoogleIntegrationConfiguration

class GoogleFitServiceAndroid(
    private val context: Context,
    private val configuration: GoogleIntegrationConfiguration
) : GoogleFitService {
    private val zoneId: ZoneId = ZoneId.systemDefault()
    private val fitnessOptions: FitnessOptions = FitnessOptions.builder()
        .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
        .addDataType(DataType.TYPE_CALORIES_EXPENDED, FitnessOptions.ACCESS_READ)
        .build()

    override fun availability(): GoogleIntegrationAvailability = configuration.availability()

    override suspend fun readDailyActivity(
        session: GoogleAccountSession,
        startDate: LocalDate,
        endDate: LocalDate
    ): Result<List<ImportedActivitySample>> {
        if (availability() != GoogleIntegrationAvailability.Available) {
            return Result.failure(GoogleIntegrationError.Unavailable)
        }
        if (startDate > endDate) {
            return Result.failure(IllegalArgumentException("INVALID_IMPORT_WINDOW"))
        }

        return try {
            val account = GoogleSignIn.getLastSignedInAccount(context)
                ?: GoogleSignIn.getAccountForExtension(context, fitnessOptions)
            if (!account.matches(session)) {
                return Result.failure(
                    GoogleIntegrationError.AccountMismatch("Google Fit access is bound to a different Google account.")
                )
            }
            val response = Fitness.getHistoryClient(context, account)
                .readData(createReadRequest(startDate, endDate))
                .awaitResult()
            Result.success(mapBuckets(startDate, endDate, response.buckets))
        } catch (error: Throwable) {
            Result.failure(GoogleIntegrationError.ImportFailed(error.message ?: "Google Fit read failed"))
        }
    }

    override suspend fun disconnect(session: GoogleAccountSession): Result<Unit> {
        return try {
            GoogleSignIn.getClient(context, GoogleSignInOptions.DEFAULT_SIGN_IN).revokeAccess()
            Result.success(Unit)
        } catch (error: Throwable) {
            Result.failure(error)
        }
    }

    private fun createReadRequest(startDate: LocalDate, endDate: LocalDate): DataReadRequest {
        val startMillis = startDate.toJavaDate().atStartOfDay(zoneId).toInstant().toEpochMilli()
        val endMillis = endDate.toJavaDate().plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        return DataReadRequest.Builder()
            .aggregate(DataType.TYPE_STEP_COUNT_DELTA, DataType.AGGREGATE_STEP_COUNT_DELTA)
            .aggregate(DataType.TYPE_CALORIES_EXPENDED, DataType.AGGREGATE_CALORIES_EXPENDED)
            .bucketByTime(1, TimeUnit.DAYS)
            .setTimeRange(startMillis, endMillis, TimeUnit.MILLISECONDS)
            .enableServerQueries()
            .build()
    }

    private fun mapBuckets(
        startDate: LocalDate,
        endDate: LocalDate,
        buckets: List<Bucket>
    ): List<ImportedActivitySample> {
        val days = linkedMapOf<LocalDate, ImportedActivitySample>()
        var cursor = startDate
        while (cursor <= endDate) {
            days[cursor] = ImportedActivitySample(date = cursor, stepCount = null, activeCalories = null)
            cursor = cursor.plus(1, DateTimeUnit.DAY)
        }

        buckets.forEach { bucket ->
            val bucketDate = java.time.Instant.ofEpochMilli(bucket.getStartTime(TimeUnit.MILLISECONDS))
                .atZone(zoneId)
                .toLocalDate()
                .toKotlinDate()
            var steps: Int? = null
            var calories: Int? = null

            bucket.dataSets.flatMap { it.dataPoints }.forEach { point ->
                when (point.dataType.name) {
                    DataType.AGGREGATE_STEP_COUNT_DELTA.name -> {
                        steps = (steps ?: 0) + readInt(point, Field.FIELD_STEPS)
                    }
                    DataType.AGGREGATE_CALORIES_EXPENDED.name -> {
                        calories = (calories ?: 0) + readFloat(point, Field.FIELD_CALORIES).roundToInt()
                    }
                }
            }

            days[bucketDate] = ImportedActivitySample(
                date = bucketDate,
                stepCount = steps,
                activeCalories = calories
            )
        }

        return days.values.toList()
    }

    private fun readInt(point: DataPoint, field: Field): Int {
        return if (point.dataType.fields.contains(field)) point.getValue(field).asInt() else 0
    }

    private fun readFloat(point: DataPoint, field: Field): Float {
        return if (point.dataType.fields.contains(field)) point.getValue(field).asFloat() else 0f
    }

    private fun LocalDate.toJavaDate(): java.time.LocalDate {
        return java.time.LocalDate.of(year, monthNumber, dayOfMonth)
    }

    private fun java.time.LocalDate.toKotlinDate(): LocalDate {
        return LocalDate(year, monthValue, dayOfMonth)
    }

    private fun GoogleSignInAccount.matches(session: GoogleAccountSession): Boolean {
        val sessionEmail = session.email?.trim()?.lowercase()
        val accountEmail = email?.trim()?.lowercase()
        if (sessionEmail != null && accountEmail != null) {
            return sessionEmail == accountEmail
        }
        return session.subjectId == (id ?: email ?: displayName ?: "")
    }

    private suspend fun <T> Task<T>.awaitResult(): T {
        return suspendCancellableCoroutine { continuation ->
            addOnSuccessListener { continuation.resume(it) }
            addOnFailureListener { continuation.resumeWithException(it) }
        }
    }
}
