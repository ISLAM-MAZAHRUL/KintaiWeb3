package kintai;

import java.io.Serializable;

/**
 * ロール情報（roleテーブルのレコード）を保持するJavaBean。
 * 従業員の権限レベルを定義します。
 */
public class RoleBean implements Serializable {
    private static final long serialVersionUID = 1L;

    private int roleId;         // roleテーブルの「ROLE_ID」列に対応
    private String roleName;    // roleテーブルの「ROLENAME」列に対応

    /**
     * デフォルトコンストラクタ
     */
    public RoleBean() {
    }

    /**
     * 全フィールドを初期化するコンストラクタ
     * @param roleId ロールID
     * @param roleName ロール名
     */
    public RoleBean(int roleId, String roleName) {
        this.roleId = roleId;
        this.roleName = roleName;
    }

    // --- 以下、各フィールドのアクセサメソッド (getter/setter) ---

    public int getRoleId() {
        return roleId;
    }

    public void setRoleId(int roleId) {
        this.roleId = roleId;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }
}
