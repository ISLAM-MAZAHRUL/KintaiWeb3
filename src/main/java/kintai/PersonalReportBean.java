package kintai;

import java.util.List;
import java.util.ArrayList;

/**
 * 個人別月次報告書用のデータモデル
 */
public class PersonalReportBean {
    private String empno;           // 従業員番号
    private String empName;         // 氏名
    private String deptName;        // 部署名
    private String postName;        // 役職名
    private String targetMonth;     // 対象月 (YYYY-MM)
    private MonthlySummaryBean summary;  // 月度統計
    private List<KintaiRecBean> records; // 詳細記録
    private List<String> complianceWarnings; // 法定時間チェック警告

    public PersonalReportBean() {
        this.records = new ArrayList<>();
        this.complianceWarnings = new ArrayList<>();
    }

    // Getter and Setter methods
    public String getEmpno() {
        return empno;
    }

    public void setEmpno(String empno) {
        this.empno = empno;
    }

    public String getEmpName() {
        return empName;
    }

    public void setEmpName(String empName) {
        this.empName = empName;
    }

    public String getDeptName() {
        return deptName;
    }

    public void setDeptName(String deptName) {
        this.deptName = deptName;
    }

    public String getPostName() {
        return postName;
    }

    public void setPostName(String postName) {
        this.postName = postName;
    }

    public String getTargetMonth() {
        return targetMonth;
    }

    public void setTargetMonth(String targetMonth) {
        this.targetMonth = targetMonth;
    }

    public MonthlySummaryBean getSummary() {
        return summary;
    }

    public void setSummary(MonthlySummaryBean summary) {
        this.summary = summary;
    }

    public List<KintaiRecBean> getRecords() {
        return records;
    }

    public void setRecords(List<KintaiRecBean> records) {
        this.records = records;
    }

    public List<String> getComplianceWarnings() {
        return complianceWarnings;
    }

    public void setComplianceWarnings(List<String> complianceWarnings) {
        this.complianceWarnings = complianceWarnings;
    }

    public void addComplianceWarning(String warning) {
        this.complianceWarnings.add(warning);
    }

    /**
     * 法定時間チェック結果の判定
     */
    public boolean isMonthlyOvertimeCompliant() {
        if (summary == null) return true;
        // 月間残業45時間以内
        return summary.getTotalOvertimeHours().doubleValue() <= 45.0;
    }

    /**
     * 1日8時間超過日数の計算
     */
    public int getExcessWorkDaysCount() {
        if (records == null) return 0;
        return (int) records.stream()
                .filter(r -> r.getActualWorkMinutes() > 8 * 60)
                .count();
    }

    /**
     * 長時間残業日の取得（4時間超）
     */
    public List<KintaiRecBean> getLongOvertimeDays() {
        if (records == null) return new ArrayList<>();
        return records.stream()
                .filter(r -> r.getOvertimeMinutes() > 4 * 60)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 遅刻日数の計算
     */
    public int getLateDaysCount() {
        if (records == null) return 0;
        return (int) records.stream()
                .filter(r -> r.getClockIn() != null && 
                           r.getClockIn().toLocalTime().isAfter(java.time.LocalTime.of(9, 0)))
                .count();
    }

    /**
     * 欠勤日数の計算
     */
    public int getAbsentDaysCount() {
        if (records == null || summary == null) return 0;
        return summary.getTotalWorkDays() - summary.getActualAttendanceDays();
    }

    // JSPとの互換性のための追加メソッド
    /**
     * 従業員IDの別名ゲッター（empId形式）
     * JSPでの使用を考慮した後方互換性メソッド
     * @return 従業員番号
     */
    public String getEmpId() {
        return empno;
    }

    /**
     * 従業員IDの別名セッター（empId形式）
     * JSPでの使用を考慮した後方互換性メソッド
     * @param empId 従業員番号
     */
    public void setEmpId(String empId) {
        this.empno = empId;
    }
}