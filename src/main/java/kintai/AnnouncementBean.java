package kintai;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * アナウンス情報（announcementテーブルのレコード）を保持するJavaBean。
 */
public class AnnouncementBean implements Serializable {
    private static final long serialVersionUID = 1L;

    // --- announcementテーブルの列に対応するフィールド ---
    private int announcementId;        // announcementテーブルの「ANNOUNCEMENT_ID」列に対応
    private String title;              // announcementテーブルの「TITLE」列に対応
    private String content;            // announcementテーブルの「CONTENT」列に対応
    private boolean isActive;          // announcementテーブルの「IS_ACTIVE」列に対応
    private int displayOrder;          // announcementテーブルの「DISPLAY_ORDER」列に対応
    private boolean isDeleted;         // announcementテーブルの「IS_DELETED」列に対応
    private LocalDateTime deletedAt;   // announcementテーブルの「DELETED_AT」列に対応
    private String deletedBy;          // announcementテーブルの「DELETED_BY」列に対応
    private LocalDateTime createdAt;   // announcementテーブルの「CREATED_AT」列に対応
    private String createdBy;          // announcementテーブルの「CREATED_BY」列に対応
    private LocalDateTime updatedAt;   // announcementテーブルの「UPDATED_AT」列に対応
    private String updatedBy;          // announcementテーブルの「UPDATED_BY」列に対応

    /**
     * デフォルトコンストラクタ
     */
    public AnnouncementBean() {
    }

    // --- 以下、各フィールドのアクセサメソッド (getter/setter) ---

    public int getAnnouncementId() {
        return announcementId;
    }

    public void setAnnouncementId(int announcementId) {
        this.announcementId = announcementId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public void setDeleted(boolean deleted) {
        isDeleted = deleted;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    public String getDeletedBy() {
        return deletedBy;
    }

    public void setDeletedBy(String deletedBy) {
        this.deletedBy = deletedBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }
}