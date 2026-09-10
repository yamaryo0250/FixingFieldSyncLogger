package ryo.myappcompany.fixingfieldsynclogger.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface ReportDao {
    @Insert
    void insert(Report report);

    @Query("SELECT * FROM reports")
    List<Report> getAllReports();

    @Query("SELECT * FROM reports WHERE isSynced = 0")
    List<Report> getUnsyncedReports();

    @Update
    void update(Report report);
}
