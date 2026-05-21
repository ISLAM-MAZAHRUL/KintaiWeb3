package kintai;

import java.io.Serializable;
import java.math.BigDecimal; // BigDecimal をインポート
import java.sql.Time;
import java.time.LocalDate;

/**
 * 勤怠情報（kintaiテーブルのレコード）を保持するJavaBean。
 * 新しいER図のkintaiテーブルの列に対応するよう修正。
 */
public class WorkTimeBean implements Serializable {
    private static final long serialVersionUID = 1L;

    private int kintaiRecId;     // kintaiテーブルの「KINTAI_REC_ID」列に対応
    private LocalDate kintaiDate;  // kintaiテーブルの「KINTAI_DATE」列に対応
    private String empId;        // kintaiテーブルの「EMP_ID」列に対応
    private Time clockIn;        // kintaiテーブルの「CLOCK_IN」列に対応
    private Time clockOut;       // kintaiテーブルの「CLOCK_OUT」列に対応
    private BigDecimal workingHours;    // kintaiテーブルの「WORKING_HOURS」列に対応
    private BigDecimal overtimeHours;   // kintaiテーブルの「OVERTIME_HOURS」列に対応
    private BigDecimal nightHours;      // kintaiテーブルの「NIGHT_HOURS」列に対応
    private boolean isDeleted;   // kintaiテーブルの「IS_DELETED」列に対応
    private boolean isFinalized; // kintaiテーブルの「IS_FINALIZED」列に対応
    private java.sql.Timestamp createdAt;
    private java.sql.Timestamp updatedAt;
    private String createdBy;
    private String updatedBy;
    
	 // ★追記: 勤怠区分（出勤/有給/無給/欠勤）
	 // kintaiテーブルの ATTENDANCE_TYPE 列に対応
	 private String attendanceType;
	
	 public String getAttendanceType() { return attendanceType; }
	 public void setAttendanceType(String attendanceType) { this.attendanceType = attendanceType; }




    // --- 以下、各フィールドのアクセサメソッド (getter/setter) ---

    public int getKintaiRecId() {
        return kintaiRecId;
    }

    public void setKintaiRecId(int kintaiRecId) {
        this.kintaiRecId = kintaiRecId;
    }

    // Backward compatibility
    public int getRecId() {
        return kintaiRecId;
    }

    public void setRecId(int recId) {
        this.kintaiRecId = recId;
    }

    public LocalDate getKintaiDate() {
        return kintaiDate;
    }

    public void setKintaiDate(LocalDate kintaiDate) {
        this.kintaiDate = kintaiDate;
    }

    public String getEmpId() {
        return empId;
    }

    public void setEmpId(String empId) {
        this.empId = empId;
    }

    public Time getClockIn() {
        return clockIn;
    }

    public void setClockIn(Time clockIn) {
        this.clockIn = clockIn;
    }

    public Time getClockOut() {
        return clockOut;
    }

    public void setClockOut(Time clockOut) {
        this.clockOut = clockOut;
    }

    public BigDecimal getWorkingHours() {
        return workingHours;
    }

    public void setWorkingHours(BigDecimal workingHours) {
        this.workingHours = workingHours;
    }

    public BigDecimal getOvertimeHours() {
        return overtimeHours;
    }

    public void setOvertimeHours(BigDecimal overtimeHours) {
        this.overtimeHours = overtimeHours;
    }

    public BigDecimal getNightHours() {
        return nightHours;
    }

    public void setNightHours(BigDecimal nightHours) {
        this.nightHours = nightHours;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public void setDeleted(boolean isDeleted) {
        this.isDeleted = isDeleted;
    }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
    
    /**
     * isDeletedのゲッターメソッド（getIsDeleted形式）
     * JSPでの使用を考慮した互換性メソッド
     * @return 削除されているかどうか
     */
    public boolean getIsDeleted() {
        return isDeleted;
    }

    public boolean isFinalized() {
        return isFinalized;
    }

    public void setFinalized(boolean isFinalized) {
        this.isFinalized = isFinalized;
    }

    /**
     * isFinalizedのゲッターメソッド（getIsFinalized形式）
     * JSPでの使用を考慮した互換性メソッド
     * @return 確定されているかどうか
     */
    public boolean getIsFinalized() {
        return isFinalized;
    }

    public java.sql.Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(java.sql.Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public java.sql.Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(java.sql.Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    // JSPとの互換性のための追加メソッド
    /**
     * 従業員番号の別名ゲッター（empno形式）
     * JSPでの使用を考慮した後方互換性メソッド
     * @return 従業員ID（従業員番号）
     */
    public String getEmpno() {
        return empId;
    }

    /**
     * 従業員番号の別名セッター（empno形式）
     * JSPでの使用を考慮した後方互換性メソッド
     * @param empno 従業員番号
     */
    public void setEmpno(String empno) {
        this.empId = empno;
    }
}
