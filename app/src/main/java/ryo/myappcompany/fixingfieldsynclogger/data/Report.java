package ryo.myappcompany.fixingfieldsynclogger.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
@Entity(tableName = "reports")
public class Report {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String content;

    public boolean isSynced;
}
