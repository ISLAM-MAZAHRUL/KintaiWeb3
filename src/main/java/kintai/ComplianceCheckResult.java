package kintai;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class ComplianceCheckResult {
    private String empno;                           // 従業員番号
    private String empName;                         // 従業員名
    private String deptName;                        // 部署名
    private String postName;                        // 役職名
    private LocalDate checkDate;                   // チェック実行日
    private String checkType;                      // チェック種別（法令遵守/会社規則/総合）
    private List<ComplianceViolation> violations;  // 違反項目リスト
    private int totalViolations;                   // 総違反件数
    private double complianceScore;                // 合規スコア（0-100）
    private String riskLevel;                      // リスクレベル（低/中/高）
    
    public ComplianceCheckResult() {
        this.violations = new ArrayList<>();
        this.checkDate = LocalDate.now();
    }
    
    // Getters and Setters
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
    
    public LocalDate getCheckDate() {
        return checkDate;
    }
    
    public void setCheckDate(LocalDate checkDate) {
        this.checkDate = checkDate;
    }
    
    public String getCheckType() {
        return checkType;
    }
    
    public void setCheckType(String checkType) {
        this.checkType = checkType;
    }
    
    public List<ComplianceViolation> getViolations() {
        return violations;
    }
    
    public void setViolations(List<ComplianceViolation> violations) {
        this.violations = violations;
        updateRiskLevel();
    }
    
    public int getTotalViolations() {
        return totalViolations;
    }
    
    public void setTotalViolations(int totalViolations) {
        this.totalViolations = totalViolations;
    }
    
    public double getComplianceScore() {
        return complianceScore;
    }
    
    public void setComplianceScore(double complianceScore) {
        this.complianceScore = complianceScore;
        updateRiskLevel();
    }
    
    public String getRiskLevel() {
        return riskLevel;
    }
    
    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }
    
    /**
     * リスクレベルの自動更新
     */
    private void updateRiskLevel() {
        if (complianceScore >= 90) {
            this.riskLevel = "低";
        } else if (complianceScore >= 70) {
            this.riskLevel = "中";
        } else {
            this.riskLevel = "高";
        }
    }
    
    /**
     * 高重度違反の件数を取得
     */
    public long getHighSeverityViolationCount() {
        return violations.stream()
            .filter(v -> "高".equals(v.getSeverity()))
            .count();
    }
    
    /**
     * 中重度違反の件数を取得
     */
    public long getMediumSeverityViolationCount() {
        return violations.stream()
            .filter(v -> "中".equals(v.getSeverity()))
            .count();
    }
    
    /**
     * 低重度違反の件数を取得
     */
    public long getLowSeverityViolationCount() {
        return violations.stream()
            .filter(v -> "低".equals(v.getSeverity()))
            .count();
    }
    
    /**
     * 法令違反の件数を取得
     */
    public long getLegalViolationCount() {
        return violations.stream()
            .filter(v -> v.getLegalBasis() != null && v.getLegalBasis().contains("労働基準法"))
            .count();
    }
    
    /**
     * 会社規則違反の件数を取得
     */
    public long getCompanyRuleViolationCount() {
        return violations.stream()
            .filter(v -> v.getLegalBasis() != null && v.getLegalBasis().contains("就業規則"))
            .count();
    }
    
    /**
     * 合規状況のサマリーメッセージを生成
     */
    public String getComplianceSummary() {
        StringBuilder summary = new StringBuilder();
        
        if (complianceScore >= 95) {
            summary.append("✅ 優秀な合規状況です。");
        } else if (complianceScore >= 85) {
            summary.append("🔵 良好な合規状況です。");
        } else if (complianceScore >= 70) {
            summary.append("⚠️ 改善が必要な項目があります。");
        } else {
            summary.append("🚨 重要な合規問題が発見されました。");
        }
        
        if (totalViolations > 0) {
            summary.append(String.format(" 違反件数: %d件", totalViolations));
            
            long highCount = getHighSeverityViolationCount();
            long mediumCount = getMediumSeverityViolationCount();
            long lowCount = getLowSeverityViolationCount();
            
            if (highCount > 0) {
                summary.append(String.format(" (高重度: %d件", highCount));
                if (mediumCount > 0 || lowCount > 0) {
                    summary.append(String.format(", 中重度: %d件, 低重度: %d件)", mediumCount, lowCount));
                } else {
                    summary.append(")");
                }
            } else if (mediumCount > 0) {
                summary.append(String.format(" (中重度: %d件", mediumCount));
                if (lowCount > 0) {
                    summary.append(String.format(", 低重度: %d件)", lowCount));
                } else {
                    summary.append(")");
                }
            } else if (lowCount > 0) {
                summary.append(String.format(" (低重度: %d件)", lowCount));
            }
        }
        
        return summary.toString();
    }
    
    /**
     * リスクレベルに応じたCSSクラスを取得
     */
    public String getRiskLevelCssClass() {
        switch (riskLevel) {
            case "高": return "risk-high";
            case "中": return "risk-medium";
            case "低": return "risk-low";
            default: return "risk-unknown";
        }
    }
    
    /**
     * 合規スコアに応じたCSSクラスを取得
     */
    public String getComplianceScoreCssClass() {
        if (complianceScore >= 90) {
            return "score-excellent";
        } else if (complianceScore >= 80) {
            return "score-good";
        } else if (complianceScore >= 70) {
            return "score-fair";
        } else {
            return "score-poor";
        }
    }
}