package kintai;

import java.io.Serializable;
import java.time.LocalDate; // LocalDateをインポート

/**
 * 従業員情報（empテーブルのレコード）を保持するJavaBean。
 * 新しいER図のempテーブルの全列に対応し、
 * 部署名、役職名、ロール名、等級名も保持できるように拡張。
 */
public class EmpBean implements Serializable {
    private static final long serialVersionUID = 1L;

    // --- empテーブルの列に対応するフィールド ---
    private String empId;       // empテーブルの「EMP_ID」列に対応
    private String empName;     // empテーブルの「EMP_NAME」列に対応
    private String deptId;      // empテーブルの「DEPT_ID」列に対応
    private String postId;      // empテーブルの「POST_ID」列に対応
    private int roleId;         // empテーブルの「ROLE_ID」列に対応
    private String empType;     // empテーブルの「EMP_TYPE」列に対応
    private String pass;        // empテーブルの「PASS」列に対応
    private String mail;        // empテーブルの「MAIL」列に対応
    private LocalDate empDate;  // empテーブルの「EMP_DATE」列に対応
    private boolean isActive;   // empテーブルの「IS_ACTIVE」列に対応
    private LocalDate leaveDate; // empテーブルの「LEAVE_DATE」列に対応
    
    // --- 表示用の追加フィールド（JOINで取得） ---
    private String deptName;    // 部署名（dept.DEPT_NAME）
    private String postName;    // 役職名（post.POST_NAME）
    private String roleName;    // ロール名（role.ROLE_NAME）

    // --- 休暇付与のための日数表示に使用する追加フィールド ---
    private int grantedDays;
    private boolean canGrant;         // 付与可能か


    /**
     * デフォルトコンストラクタ
     */
    public EmpBean() {
    }

    // --- 以下、各フィールドのアクセサメソッド (getter/setter) ---

    public String getEmpId() {
        return empId;
    }

    public void setEmpId(String empId) {
        this.empId = empId;
    }

    public String getEmpName() {
        return empName;
    }

    public void setEmpName(String empName) {
        this.empName = empName;
    }

    public String getDeptId() {
        return deptId;
    }

    public void setDeptId(String deptId) {
        this.deptId = deptId;
    }

    public String getPostId() {
        return postId;
    }

    public void setPostId(String postId) {
        this.postId = postId;
    }

    // 旧getRole/setRoleからgetRoleId/setRoleIdへ変更
    public int getRoleId() {
        return roleId;
    }

    public void setRoleId(int roleId) {
        this.roleId = roleId;
    }

    public String getEmpType() {
        return empType;
    }

    public void setEmpType(String empType) {
        this.empType = empType;
    }

    public String getPass() {
        return pass;
    }

    public void setPass(String pass) {
        this.pass = pass;
    }

    public String getMail() {
        return mail;
    }

    public void setMail(String mail) {
        this.mail = mail;
    }

    public LocalDate getEmpDate() {
        return empDate;
    }

    public void setEmpDate(LocalDate empDate) {
        this.empDate = empDate;
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

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
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

    public LocalDate getLeaveDate() {
        return leaveDate;
    }

    public void setLeaveDate(LocalDate leaveDate) {
        this.leaveDate = leaveDate;
    }

    // JSPとの互換性のための追加メソッド
    /**
     * 従業員番号の別名ゲッター（empno形式）
     * JSPでの使用を考慮した後方互換性メソッド
     * @return 従業員ID（従業員番号）
     */
    public String getEmpno() {
        return empId;
    }

    /**
     * 従業員番号の別名セッター（empno形式）
     * JSPでの使用を考慮した後方互換性メソッド
     * @param empno 従業員番号
     */
    public void setEmpno(String empno) {
        this.empId = empno;
    }

    /**
     * 部署番号の別名ゲッター（deptNo形式）
     * JSPでの使用を考慮した後方互換性メソッド
     * @return 部署ID（部署番号）
     */
    public String getDeptNo() {
        return deptId;
    }

    /**
     * 部署番号の別名セッター（deptNo形式）
     * JSPでの使用を考慮した後方互換性メソッド
     * @param deptNo 部署番号
     */
    public void setDeptNo(String deptNo) {
        this.deptId = deptNo;
    }

    /**
     * 役職番号の別名ゲッター（postNo形式）
     * JSPでの使用を考慮した後方互換性メソッド
     * @return 役職ID（役職番号）
     */
    public String getPostNo() {
        return postId;
    }

    /**
     * 役職番号の別名セッター（postNo形式）
     * JSPでの使用を考慮した後方互換性メソッド
     * @param postNo 役職番号
     */
    public void setPostNo(String postNo) {
        this.postId = postNo;
    }
    
    /**
     * 休暇付与管理画面の表示用
     */
    public int getGrantedDays() {
        return grantedDays;
    }

    public void setGrantedDays(int grantedDays) {
        this.grantedDays = grantedDays;
    }
    
    public boolean isCanGrant() {
    	return canGrant;
    }
    
    public void setCanGrant(boolean canGrant) {
    	this.canGrant = canGrant;
    }
    
}
