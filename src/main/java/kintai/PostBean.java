package kintai;

import java.io.Serializable;

/**
 * 役職情報（postテーブルのレコード）を保持するJavaBean。
 */
public class PostBean implements Serializable {
    private static final long serialVersionUID = 1L;

    private String postId;      // postテーブルの「POST_ID」列に対応
    private String postName;    // postテーブルの「POST_NAME」列に対応
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
    public PostBean() {
    }

    /**
     * 全フィールドを初期化するコンストラクタ
     * @param postNo 役職番号
     * @param postName 役職名
     */
    public PostBean(String postId, String postName) {
        this.postId = postId;
        this.postName = postName;
    }

    // --- 以下、各フィールドのアクセサメソッド (getter/setter) ---

    public String getPostId() {
        return postId;
    }

    public void setPostId(String postId) {
        this.postId = postId;
    }

    public String getPostName() {
        return postName;
    }

    public void setPostName(String postName) {
        this.postName = postName;
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
    public String getPostNo() {
        return this.postId;
    }

    public void setPostNo(String postNo) {
        this.postId = postNo;
    }
}