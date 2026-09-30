package ryo.myappcompany.fixingfieldsynclogger.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reports")
data class Report (
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val content: String? = null,

    val isSynced: Boolean = false
)
