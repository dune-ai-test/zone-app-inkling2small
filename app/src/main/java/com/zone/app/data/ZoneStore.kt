package com.zone.app.data

import android.content.Context
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import java.io.File
import java.time.Instant
import java.util.UUID

/**
 * Zone — local-first key/value store.
 *
 * The web app keeps one localStorage entry per `zone.v1.*` key and mirrors it
 * to Cloudflare D1. The Android build keeps the same contract on-device:
 * one JSON file (`zone.store.json`) holding a map of key -> JSON value, cached
 * in memory, written atomically on every mutation.
 */
object ZoneStore {

    @PublishedApi
    internal val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
    }

    private lateinit var appContext: Context
    private val lock = Any()
    private val cache = linkedMapOf<String, JsonElement>()
    private var loaded = false

    private const val STORE_FILE = "zone.store.json"
    private const val AUDIT_LIMIT = 200

    fun init(context: Context) {
        synchronized(lock) {
            if (!::appContext.isInitialized) {
                appContext = context.applicationContext
            }
            if (!loaded) {
                loadFromDisk()
                loaded = true
            }
        }
    }

    private fun storeFile(): File = File(appContext.filesDir, STORE_FILE)

    private fun loadFromDisk() {
        cache.clear()
        try {
            val file = storeFile()
            if (!file.exists()) return
            val text = file.readText()
            if (text.isBlank()) return
            val obj = json.parseToJsonElement(text).jsonObject
            obj.forEach { (key, value) -> cache[key] = value }
        } catch (_: Exception) {
            // A corrupt store must never crash the desk; keep whatever parsed.
        }
    }

    private fun persistLocked() {
        try {
            val target = storeFile()
            val tmp = File(target.parentFile, STORE_FILE + ".tmp")
            tmp.writeText(JsonObject(cache).toString())
            if (!tmp.renameTo(target)) {
                tmp.copyTo(target, overwrite = true)
                tmp.delete()
            }
        } catch (_: Exception) {
            // Storage full / revoked permission — keep the in-memory cache authoritative.
        }
    }

    /** Raw element for a key, or null when absent. */
    fun getElement(key: String): JsonElement? = synchronized(lock) {
        if (!loaded) loadFromDisk().also { loaded = true }
        cache[key]
    }

    fun <T> read(key: String, default: T, decode: (JsonElement) -> T): T = synchronized(lock) {
        if (!loaded) loadFromDisk().also { loaded = true }
        val element = cache[key] ?: return@synchronized default
        try {
            decode(element)
        } catch (_: Exception) {
            default
        }
    }

    inline fun <reified T> readTyped(key: String, default: T): T =
        read(key, default) { element -> json.decodeFromJsonElement(kotlinx.serialization.serializer<T>(), element) }

    fun <T> write(key: String, value: T, encode: (T) -> JsonElement) = synchronized(lock) {
        if (!loaded) loadFromDisk().also { loaded = true }
        cache[key] = encode(value)
        persistLocked()
    }

    inline fun <reified T> writeTyped(key: String, value: T) =
        write(key, value) { v -> json.encodeToJsonElement(kotlinx.serialization.serializer<T>(), v) }

    /** Remove every key that starts with [prefix]; used by "reset tool" actions. */
    fun removeKey(key: String) = synchronized(lock) {
        if (!loaded) loadFromDisk().also { loaded = true }
        cache.remove(key)
        persistLocked()
    }

    fun allKeys(): List<String> = synchronized(lock) {
        if (!loaded) loadFromDisk().also { loaded = true }
        cache.keys.toList()
    }

    // ---------------- audit trail ------------------------------------

    fun auditEvents(): List<AuditEvent> =
        readTyped(KEY_AUDIT, emptyList())

    fun logAudit(tool: String, action: String, title: String) {
        val events = auditEvents().toMutableList()
        events.add(
            0,
            AuditEvent(
                id = "audit-" + UUID.randomUUID().toString().take(8),
                tool = tool,
                action = action,
                title = title.take(140),
                at = Instant.now().toString()
            )
        )
        writeTyped(KEY_AUDIT, events.take(AUDIT_LIMIT))
    }

    fun clearAudit() = writeTyped(KEY_AUDIT, emptyList<AuditEvent>())

    // ---------------- settings ---------------------------------------

    fun settings(): AppSettings = readTyped(KEY_SETTINGS, AppSettings())

    fun saveSettings(patch: AppSettings) = writeTyped(KEY_SETTINGS, patch)

    // ---------------- backup / restore --------------------------------

    /** Full workspace export as pretty JSON text (share-sheet friendly). */
    fun exportBackup(): String {
        val snapshot: Map<String, JsonElement> = synchronized(lock) {
            if (!loaded) loadFromDisk().also { loaded = true }
            LinkedHashMap(cache)
        }
        val backup = BackupFile(
            exportedAt = Instant.now().toString(),
            data = snapshot
        )
        return Json { prettyPrint = true }.encodeToString(BackupFile.serializer(), backup)
    }

    /**
     * Merge-import a backup produced by [exportBackup] (or a web-app export with the
     * same `data` map shape). Returns the number of keys restored.
     */
    fun importBackup(text: String): Int {
        val file = try {
            json.decodeFromString(BackupFile.serializer(), text)
        } catch (_: Exception) {
            return 0
        }
        synchronized(lock) {
            if (!loaded) loadFromDisk().also { loaded = true }
            file.data.forEach { (key, value) -> cache[key] = value }
            persistLocked()
        }
        logAudit("admin", "restore", "Imported backup from ${file.exportedAt}")
        return file.data.size
    }

    fun clearWorkspace() {
        synchronized(lock) {
            cache.clear()
            persistLocked()
        }
    }

    /** Record counts per tool, for the admin register. */
    inline fun <reified T> countTyped(key: String): Int = readTyped<List<T>>(key, emptyList()).size

    fun recordCounts(): List<Pair<String, Int>> {
        val money = readTyped(KEY_MONEY, MoneyData())
        val passbook = readTyped(KEY_PASSBOOK, PassbookData())
        return listOf(
            "Diary" to countTyped<DiaryEntry>(KEY_DIARY),
            "Lists" to countTyped<ZoneList>(KEY_LISTS),
            "Habits" to countTyped<Habit>(KEY_HABITS),
            "Notes" to countTyped<Note>(KEY_NOTES),
            "Money" to money.transactions.size,
            "Passbook" to passbook.transactions.size,
            "Split" to countTyped<SplitGroup>(KEY_SPLIT),
            "Mileage" to countTyped<Fill>(KEY_TRIPS),
            "Watchlist" to countTyped<WatchItem>(KEY_WATCHLIST),
            "Links" to countTyped<LinkItem>(KEY_LINKS),
            "Subscriptions" to countTyped<Subscription>(KEY_SUBSCRIPTIONS)
        )
    }

    // ---------------- keys (mirrors the web app's zone.v1.* keys) -----

    const val KEY_DIARY = "diary"
    const val KEY_LISTS = "lists"
    const val KEY_HABITS = "habits"
    const val KEY_NOTES = "notes"
    const val KEY_MONEY = "money"
    const val KEY_PASSBOOK = "passbook"
    const val KEY_SPLIT = "split"
    const val KEY_TRIPS = "trips"
    const val KEY_VEHICLES = "vehicles"
    const val KEY_WATCHLIST = "watchlist"
    const val KEY_LINKS = "links"
    const val KEY_SUBSCRIPTIONS = "subscriptions"
    const val KEY_SETTINGS = "settings"
    const val KEY_AUDIT = "audit"
}
