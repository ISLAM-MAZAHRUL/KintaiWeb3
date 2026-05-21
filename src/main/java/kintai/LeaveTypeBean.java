package kintai;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * leave_type テーブルに対応するJavaBean
 */
public class LeaveTypeBean implements Serializable {
    private static final long serialVersionUID = 1L;

    private int leaveTypeId;           // LEAVE_TYPE_ID
    private String leaveTypeName;      // LEAVE_TYPE_NAME
    private boolean isPaid;            // IS_PAID
    private boolean isDeleted;         // IS_DELETED
    private Timestamp deletedAt;
    private String deletedBy;
    private Timestamp createdAt;       // CREATED_AT
    private String createdBy;          // CREATED_BY
    private Timestamp updatedAt;       // UPDATED_AT
    private String updatedBy;          // UPDATED_BY

    public LeaveTypeBean() {}

    /**
     * 全フィールドを初期化するコンストラクタ
     * @param leaveTypeNo 休日種別番号
     * @param leaveTypeName 休日種別名
     */
    public LeaveTypeBean(int leaveTypeId, String leaveTypeName, boolean isPaid) {
        this.leaveTypeId = leaveTypeId;
        this.leaveTypeName = leaveTypeName;
        this.isPaid = isPaid;
    }

    public int getLeaveTypeId() {
        return leaveTypeId;
    }

    public void setLeaveTypeId(int leaveTypeId) {
        this.leaveTypeId = leaveTypeId;
    }

    public String getLeaveTypeName() {
        return leaveTypeName;
    }

    public void setLeaveTypeName(String leaveTypeName) {
        this.leaveTypeName = leaveTypeName;
    }

    public boolean isPaid() {
        return isPaid;
    }

    public void setPaid(boolean isPaid) {
        this.isPaid = isPaid;
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
    
    public String getDeletedBy() {
        return deletedBy;
    }

    public void setDeletedBy(String deletedBy) {
        this.deletedBy = deletedBy;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }
    
 // 後方互換性メソッド - JSP用
    public int getDeptNo() {
        return this.leaveTypeId;
    }

    public void setDeptNo(int deptNo) {
        this.leaveTypeId = deptNo;
    }
}
