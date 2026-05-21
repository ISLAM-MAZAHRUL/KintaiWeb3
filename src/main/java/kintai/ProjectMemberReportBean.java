package kintai;

import java.math.BigDecimal;

public class ProjectMemberReportBean {
    private String empNo;                      // 従業員番号
    private String empName;                    // 従業員名
    private BigDecimal totalHours;             // 月間総工作時間
    private BigDecimal totalProjectHours;      // プロジェクト全期間の総工作時間
    private BigDecimal hourlyRate;             // 時給
    private BigDecimal actualAmount;           // 当月実績額
    private BigDecimal personalBudget;         // 個人予算（全期間時給×全期間時間）
    private BigDecimal personalBudgetVariance; // 個人予算実績差異
    
    public ProjectMemberReportBean() {}
    
    public String getEmpNo() {
        return empNo;
    }
    
    public void setEmpNo(String empNo) {
        this.empNo = empNo;
    }
    
    public String getEmpName() {
        return empName;
    }
    
    public void setEmpName(String empName) {
        this.empName = empName;
    }
    
    public BigDecimal getTotalHours() {
        return totalHours;
    }
    
    public void setTotalHours(BigDecimal totalHours) {
        this.totalHours = totalHours;
    }
    
    public BigDecimal getTotalProjectHours() {
        return totalProjectHours;
    }
    
    public void setTotalProjectHours(BigDecimal totalProjectHours) {
        this.totalProjectHours = totalProjectHours;
    }
    
    public BigDecimal getHourlyRate() {
        return hourlyRate;
    }
    
    public void setHourlyRate(BigDecimal hourlyRate) {
        this.hourlyRate = hourlyRate;
    }
    
    public BigDecimal getActualAmount() {
        return actualAmount;
    }
    
    public void setActualAmount(BigDecimal actualAmount) {
        this.actualAmount = actualAmount;
    }
    
    public BigDecimal getPersonalBudget() {
        return personalBudget;
    }
    
    public void setPersonalBudget(BigDecimal personalBudget) {
        this.personalBudget = personalBudget;
    }
    
    public BigDecimal getPersonalBudgetVariance() {
        return personalBudgetVariance;
    }
    
    public void setPersonalBudgetVariance(BigDecimal personalBudgetVariance) {
        this.personalBudgetVariance = personalBudgetVariance;
    }

    // JSPとの互換性のための追加メソッド
    /**
     * 従業員番号の別名ゲッター（empno形式）
     * JSPでの使用を考慮した後方互換性メソッド
     * @return 従業員番号
     */
    public String getEmpno() {
        return empNo;
    }

    /**
     * 従業員番号の別名セッター（empno形式）
     * JSPでの使用を考慮した後方互換性メソッド
     * @param empno 従業員番号
     */
    public void setEmpno(String empno) {
        this.empNo = empno;
    }

    /**
     * 従業員IDの別名ゲッター（empId形式）
     * JSPでの使用を考慮した後方互換性メソッド
     * @return 従業員番号
     */
    public String getEmpId() {
        return empNo;
    }

    /**
     * 従業員IDの別名セッター（empId形式）
     * JSPでの使用を考慮した後方互換性メソッド
     * @param empId 従業員番号
     */
    public void setEmpId(String empId) {
        this.empNo = empId;
    }
}