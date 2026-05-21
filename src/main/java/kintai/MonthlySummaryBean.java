package kintai;

import java.math.BigDecimal;

public class MonthlySummaryBean {
    private int totalWorkDays;          // 会社出勤日
    private int actualAttendanceDays;   // 出勤日
    private int paidLeaveDays;          // 有給
    private int absentDays;             // 欠勤
    private int holidayWorkDays;        // 休日出勤
    private int compensatoryLeaveDays;  // 代休

    private BigDecimal totalOvertimeHours;  // 残業時間
    private BigDecimal totalWorkingHours;   // 勤務時間
    private BigDecimal totalBreakHours;     // 休憩時間
    private BigDecimal totalNightHours;     // 深夜時間

    private String targetMonth;

    public MonthlySummaryBean() {
        this.totalOvertimeHours = BigDecimal.ZERO;
        this.totalWorkingHours = BigDecimal.ZERO;
        this.totalBreakHours = BigDecimal.ZERO;
        this.totalNightHours = BigDecimal.ZERO;
    }

    // === Getter ===
    public int getTotalWorkDays() { return totalWorkDays; }
    public int getActualAttendanceDays() { return actualAttendanceDays; }
    public int getPaidLeaveDays() { return paidLeaveDays; }
    public int getAbsentDays() { return absentDays; }
    public int getHolidayWorkDays() { return holidayWorkDays; }
    public int getCompensatoryLeaveDays() { return compensatoryLeaveDays; }

    public BigDecimal getTotalOvertimeHours() { return totalOvertimeHours; }
    public BigDecimal getTotalWorkingHours() { return totalWorkingHours; }
    public BigDecimal getTotalBreakHours() { return totalBreakHours; }
    public BigDecimal getTotalNightHours() { return totalNightHours; }

    public String getTargetMonth() { return targetMonth; }

    // === Setter ===
    public void setTotalWorkDays(int totalWorkDays) { this.totalWorkDays = totalWorkDays; }
    public void setActualAttendanceDays(int actualAttendanceDays) { this.actualAttendanceDays = actualAttendanceDays; }
    public void setPaidLeaveDays(int paidLeaveDays) { this.paidLeaveDays = paidLeaveDays; }
    public void setAbsentDays(int absentDays) { this.absentDays = absentDays; }
    public void setHolidayWorkDays(int holidayWorkDays) { this.holidayWorkDays = holidayWorkDays; }
    public void setCompensatoryLeaveDays(int compensatoryLeaveDays) { this.compensatoryLeaveDays = compensatoryLeaveDays; }

    public void setTotalOvertimeHours(BigDecimal totalOvertimeHours) { this.totalOvertimeHours = totalOvertimeHours; }
    public void setTotalWorkingHours(BigDecimal totalWorkingHours) { this.totalWorkingHours = totalWorkingHours; }
    public void setTotalBreakHours(BigDecimal totalBreakHours) { this.totalBreakHours = totalBreakHours; }
    public void setTotalNightHours(BigDecimal totalNightHours) { this.totalNightHours = totalNightHours; }

    public void setTargetMonth(String targetMonth) { this.targetMonth = targetMonth; }

    // === 便利メソッド ===
    public double getAttendanceRate() {
        if (totalWorkDays == 0) return 0.0;
        return (double) actualAttendanceDays / totalWorkDays * 100.0;
    }

    public String getAttendanceRateString() {
        return String.format("%d/%d", actualAttendanceDays, totalWorkDays);
    }

    public String formatHours(BigDecimal hours) {
        if (hours == null) return "0.0時間";
        return String.format("%.1f時間", hours.doubleValue());
    }

    public String getTotalOvertimeHoursString() { return formatHours(totalOvertimeHours); }
    public String getTotalWorkingHoursString() { return formatHours(totalWorkingHours); }
    public String getTotalBreakHoursString() { return formatHours(totalBreakHours); }
    public String getTotalNightHoursString() { return formatHours(totalNightHours); }
}
