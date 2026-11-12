package com.zone.app.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/* ==========================================================================
   Zone — data models
   Mirrors the `zone.v1.*` record shapes from the web app, as Kotlinx
   Serialization records persisted per key on-device.
   ========================================================================== */

@Serializable
data class DiaryEntry(
    val id: String,
    val date: String,          // yyyy-MM-dd
    val mood: String = "good", // low | flat | good | bright | peak
    val title: String = "",
    val body: String = "",
    val tags: List<String> = emptyList()
)

@Serializable
data class ListStep(
    val id: String,
    val text: String,
    val done: Boolean = false
)

@Serializable
data class ListItem(
    val id: String,
    val text: String,
    val done: Boolean = false,
    val children: List<ListStep> = emptyList()
)

@Serializable
data class ZoneList(
    val id: String,
    val title: String,
    val createdAt: String,
    val items: List<ListItem> = emptyList()
)

@Serializable
data class Habit(
    val id: String,
    val name: String,
    val target: Int = 7,               // days per week, 1..7
    val createdAt: String = "",
    val checks: Map<String, Boolean> = emptyMap()
)

@Serializable
data class Note(
    val id: String,
    val title: String = "",
    val body: String = "",
    val color: String = "plain",       // plain | sand | sage | clay
    val pinned: Boolean = false,
    val created: String = "",
    val updated: String = ""
)

@Serializable
data class Txn(
    val id: String,
    val type: String = "expense",      // income | expense
    val amount: Double = 0.0,
    val category: String = "Other",
    val date: String = "",
    val note: String = ""
)

@Serializable
data class MoneyData(
    val budget: Double = 65000.0,
    val transactions: List<Txn> = emptyList()
)

@Serializable
data class Account(
    val id: String,
    val name: String,
    val type: String = "Bank",         // Bank | Savings | Wallet | Card | Cash | Other
    val number: String = "",
    val opening: Double = 0.0
)

@Serializable
data class PassbookEntry(
    val id: String,
    val accountId: String,
    val type: String = "deposit",      // deposit | withdraw
    val amount: Double = 0.0,
    val category: String = "Other",
    val description: String = "",
    val date: String = ""
)

@Serializable
data class PassbookData(
    val accounts: List<Account> = emptyList(),
    val transactions: List<PassbookEntry> = emptyList()
)

@Serializable
data class SplitMember(
    val id: String,
    val name: String
)

@Serializable
data class SplitExpense(
    val id: String,
    val description: String = "",
    val amount: Double = 0.0,
    val paidBy: String = "",
    val date: String = "",
    val sharedWith: List<String> = emptyList(),
    val category: String = "other"     // home | food | travel | bills | fun | other | settlement
)

@Serializable
data class SplitGroup(
    val id: String,
    val name: String,
    val kind: String = "other",        // home | trip | work | other
    val members: List<SplitMember> = emptyList(),
    val expenses: List<SplitExpense> = emptyList()
)

@Serializable
data class Fill(
    val id: String,
    val vehicle: String,
    val date: String,
    val start: Double = 0.0,           // odometer km at start
    val end: Double = 0.0,             // odometer km at end
    val price: Double = 0.0,           // price per litre
    val paid: Double = 0.0,            // total paid
    val note: String = ""
)

@Serializable
data class Vehicle(
    val id: String,
    val name: String,
    val type: String = "car",          // bike | scooter | car | suv | truck
    val fuel: String = "petrol",       // petrol | diesel
    val createdAt: String = ""
)

@Serializable
data class WatchItem(
    val id: String,
    val title: String,
    val type: String = "Film",         // Film | Series
    val year: String = "",
    val genre: String = "",
    val status: String = "Queued",     // Queued | Watching | Watched | Dropped
    val rating: Double = 0.0,          // 0..10, 0 = unrated
    val watched: Int = 0,
    val total: Int = 0,
    val service: String = "",
    val favorite: Boolean = false,
    val note: String = "",
    val added: String = ""
)

@Serializable
data class Subscription(
    val id: String,
    val name: String,
    val amount: Double = 0.0,
    val cycle: String = "Monthly",     // Monthly | Yearly | Weekly
    val next: String = "",             // next charge, yyyy-MM-dd
    val category: String = "Other",
    val status: String = "Active"      // Active | Paused
)

@Serializable
data class LinkItem(
    val id: String,
    val title: String,
    val url: String,
    val tags: List<String> = emptyList(),
    val note: String = "",
    val favorite: Boolean = false,
    val saved: String = ""
)

@Serializable
data class AppSettings(
    val currency: String = "INR",
    val theme: String = "system"       // light | dark | system
)

@Serializable
data class AuditEvent(
    val id: String,
    val tool: String,
    val action: String,                // add | update | delete | restore | system
    val title: String,
    val at: String                     // ISO offset date-time
)

/** Shape used for workspace backup export / restore. */
@Serializable
data class BackupFile(
    val app: String = "zone-android",
    val version: Int = 1,
    val exportedAt: String,
    val data: Map<String, JsonElement> = emptyMap()
)
