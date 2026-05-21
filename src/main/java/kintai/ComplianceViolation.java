package kintai;

import java.time.LocalDate;

/**
 * 合规违反项目数据模型
 */
public class ComplianceViolation {
    private String violationType;    // 違反タイプ（遅刻、残業時間超過など）
    private LocalDate date;          // 違反発生日
    private String severity;         // 重要度（高/中/低）
    private String description;      // 詳細説明
    private String legalBasis;       // 法的根拠（労働基準法第○条、就業規則第○条など）
    private String recommendedAction; // 推奨対応策
    
    public ComplianceViolation() {
    }
    
    public ComplianceViolation(String violationType, LocalDate date, String severity, 
                             String description, String legalBasis) {
        this.violationType = violationType;
        this.date = date;
        this.severity = severity;
        this.description = description;
        this.legalBasis = legalBasis;
    }
    
    // Getters and Setters
    public String getViolationType() {
        return violationType;
    }
    
    public void setViolationType(String violationType) {
        this.violationType = violationType;
    }
    
    public LocalDate getDate() {
        return date;
    }
    
    public void setDate(LocalDate date) {
        this.date = date;
    }
    
    public String getSeverity() {
        return severity;
    }
    
    public void setSeverity(String severity) {
        this.severity = severity;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getLegalBasis() {
        return legalBasis;
    }
    
    public void setLegalBasis(String legalBasis) {
        this.legalBasis = legalBasis;
    }
    
    public String getRecommendedAction() {
        return recommendedAction;
    }
    
    public void setRecommendedAction(String recommendedAction) {
        this.recommendedAction = recommendedAction;
    }
    
    /**
     * 重要度に応じたアイコンを取得
     */
    public String getSeverityIcon() {
        switch (severity) {
            case "高": return "🚨";
            case "中": return "⚠️";
            case "低": return "ℹ️";
            default: return "❓";
        }
    }
    
    /**
     * 重要度に応じたCSSクラスを取得
     */
    public String getSeverityCssClass() {
        switch (severity) {
            case "高": return "severity-high";
            case "中": return "severity-medium";
            case "低": return "severity-low";
            default: return "severity-unknown";
        }
    }
    
    /**
     * 法的根拠の種別を判定
     */
    public String getLegalBasisType() {
        if (legalBasis != null) {
            if (legalBasis.contains("労働基準法")) {
                return "法令";
            } else if (legalBasis.contains("就業規則")) {
                return "会社規則";
            }
        }
        return "その他";
    }
    
    /**
     * 違反タイプに応じた推奨対応策を自動生成
     */
    public void generateRecommendedAction() {
        switch (violationType) {
            case "法定労働時間超過":
                this.recommendedAction = "労働時間の見直し、残業申請の適切な管理、業務効率化の検討";
                break;
            case "月間残業時間超過":
                this.recommendedAction = "月次残業時間の監視強化、36協定の確認、人員配置の見直し";
                break;
            case "連続勤務日数超過":
                this.recommendedAction = "休日出勤の制限、適切な休暇取得の促進、シフト管理の改善";
                break;
            case "休憩時間不足":
                this.recommendedAction = "休憩時間の確実な取得、休憩時間の記録管理強化";
                break;
            case "深夜勤務":
                this.recommendedAction = "深夜勤務の事前承認制、深夜手当の適切な支給確認";
                break;
            case "遅刻":
                this.recommendedAction = "出勤時間の確認、交通手段の見直し、勤務開始時間の調整検討";
                break;
            case "早退":
                this.recommendedAction = "退勤理由の確認、労働時間の適切な管理";
                break;
            case "月間欠勤日数超過":
                this.recommendedAction = "欠勤理由の確認、健康状態のチェック、人事面談の実施";
                break;
            case "頻繁な遅刻":
            case "頻繁な早退":
                this.recommendedAction = "勤務態度の改善指導、個別面談の実施、根本原因の調査";
                break;
            default:
                this.recommendedAction = "管理者との面談、改善計画の策定";
                break;
        }
    }
    
    /**
     * フォーマットされた日付文字列を取得
     */
    public String getFormattedDate() {
        if (date == null) return "";
        return String.format("%d月%d日", date.getMonthValue(), date.getDayOfMonth());
    }
}