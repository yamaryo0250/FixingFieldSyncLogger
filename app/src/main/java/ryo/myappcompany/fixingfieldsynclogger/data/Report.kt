package ryo.myappcompany.fixingfieldsynclogger.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reports")
data class Report (
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0,

    var content: String? = null,

    var isSynced: Boolean = false
)
