package kintai;

import java.io.Serializable;

/**
 * 部署情報（deptテーブルのレコード）を保持するJavaBean。
 */
public class DeptBean implements Serializable {
    private static final long serialVersionUID = 1L;

    private String deptId;      // deptテーブルの「DEPT_ID」列に対応
    private String deptName;    // deptテーブルの「DEPT_NAME」列に対応
    private boolean isDeleted;
    private java.sql.Timestamp deletedAt;
    private String deletedBy;
    private java.sql.Timestamp createdAt;
    private String createdBy;
    private java.sql.Timestamp updatedAt;
    private String updatedBy;

    /**
     * デフォルトコンストラクタ
     */
    public DeptBean() {
    }

    /**
     * 全フィールドを初期化するコンストラクタ
     * @param deptNo 部署番号
     * @param deptName 部署名
     */
    public DeptBean(String deptId, String deptName) {
        this.deptId = deptId;
        this.deptName = deptName;
    }

    // --- 以下、各フィールドのアクセサメソッド (getter/setter) ---

    public String getDeptId() {
        return deptId;
    }

    public void setDeptId(String deptId) {
        this.deptId = deptId;
    }

    public String getDeptName() {
        return deptName;
    }

    public void setDeptName(String deptName) {
        this.deptName = deptName;
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

    // 後方互換性メソッド - JSP用
    public String getDeptNo() {
        return this.deptId;
    }

    public void setDeptNo(String deptNo) {
        this.deptId = deptNo;
    }
}