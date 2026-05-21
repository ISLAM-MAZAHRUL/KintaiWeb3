package kintai;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * プロジェクト情報（projectテーブルのレコード）を保持するJavaBean。
 * 新しいER図のprojectテーブルに準拠。
 */
public class ProjectBean implements Serializable {
    private static final long serialVersionUID = 1L;

    private int projectId;      // projectテーブルの「PROJECT_ID」列に対応
    private String projectName; // projectテーブルの「PROJECT_NAME」列に対応
    private Integer budgetAmount;   // projectテーブルの「BUDGET_AMOUNT」列に対応
    private Integer actualCost;     // projectテーブルの「ACTUAL_COST」列に対応
    private Integer actualLaborCost; // projectテーブルの「ACTUAL_LABOR_COST」列に対応
    private LocalDate startDate;    // projectテーブルの「START_DATE」列に対応
    private LocalDate endDate;      // projectテーブルの「END_DATE」列に対応
    private boolean isActive;       // projectテーブルの「IS_ACTIVE」列に対応
    private boolean isDeleted;      // projectテーブルの「IS_DELETED」列に対応
    private java.sql.Timestamp deletedAt;
    private String deletedBy;
    private java.sql.Timestamp createdAt;
    private String createdBy;
    private java.sql.Timestamp updatedAt;
    private String updatedBy;

    /**
     * デフォルトコンストラクタ
     */
    public ProjectBean() {
    }

    /**
     * 全フィールドを初期化するコンストラクタ
     * @param projectId プロジェクトID
     * @param projectName プロジェクト名
     */
    public ProjectBean(int projectId, String projectName, Integer budgetAmount, LocalDate startDate, LocalDate endDate) {
        this.projectId = projectId;
        this.projectName = projectName;
        this.budgetAmount = budgetAmount;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    // --- 以下、各フィールドのアクセサメソッド (getter/setter) --

    public int getProjectId() {
        return projectId;
    }

    public void setProjectId(int projectId) {
        this.projectId = projectId;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }
    public Integer getBudgetAmount() {
        return budgetAmount;
    }

    public void setBudgetAmount(Integer budgetAmount) {
        this.budgetAmount = budgetAmount;
    }

    public Integer getActualCost() {
        return actualCost;
    }

    public void setActualCost(Integer actualCost) {
        this.actualCost = actualCost;
    }

    public Integer getActualLaborCost() {
        return actualLaborCost;
    }

    public void setActualLaborCost(Integer actualLaborCost) {
        this.actualLaborCost = actualLaborCost;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean isActive) {
        this.isActive = isActive;
    }

    /**
     * isActiveのゲッターメソッド（getIsActive形式）
     * JSPでの使用を考慮した互換性メソッド
     * @return アクティブかどうか
     */
    public boolean getIsActive() {
        return isActive;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public void setDeleted(boolean isDeleted) {
        this.isDeleted = isDeleted;
    }

    /**
     * isDeletedのゲッターメソッド（getIsDeleted形式）
     * JSPでの使用を考慮した互換性メソッド
     * @return 削除されているかどうか
     */
    public boolean getIsDeleted() {
        return isDeleted;
    }

    public java.sql.Timestamp getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(java.sql.Timestamp deletedAt) {
        this.deletedAt = deletedAt;
    }

    public String getDeletedBy() {
        return deletedBy;
    }

    public void setDeletedBy(String deletedBy) {
        this.deletedBy = deletedBy;
    }

    public java.sql.Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(java.sql.Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public java.sql.Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(java.sql.Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }
    
    // Budget用のエイリアスメソッド（JSPとの互換性のため）
    public Integer getBudget() {
        return budgetAmount;
    }
    
    public void setBudget(Integer budget) {
        this.budgetAmount = budget;
    }
}
