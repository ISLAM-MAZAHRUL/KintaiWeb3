package kintai;

import java.io.Serializable;

public class UserBean implements Serializable {
    private static final long serialVersionUID = 1L;

    // --- フィールド定義 ---
    // empテーブルの各列に対応する。新しいER図の定義に合わせる。

    private String empId;       // empテーブルの「EMP_ID」列に対応
    private String name;        // empテーブルの「EMP_NAME」列に対応
    private String deptId;      // empテーブルの「DEPT_ID」列に対応
    private String postId;      // empテーブルの「POST_ID」列に対応
    private int roleId;         // empテーブルの「ROLE_ID」列に対応
    // PASS, MAIL, EMPDATE はUserBeanのログイン情報として必要であれば追加可能だが、
    // 認証には直接使用しないため、ここでは省略。

    public UserBean() {
    }

    // --- 以下、各フィールドのアクセサメソッド (getter/setter) ---

    public String getEmpId() {
        return empId;
    }
    public void setEmpId(String empId) {
        this.empId = empId;
    }
    
    // 互換性のためのメソッド
    public String getEmpno() {
        return empId;
    }
    public void setEmpno(String empno) {
        this.empId = empno;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public String getDeptId() {
        return deptId;
    }
    public void setDeptId(String deptId) {
        this.deptId = deptId;
    }
    
    // 互換性のためのメソッド
    public String getDeptNo() {
        return deptId;
    }
    public void setDeptNo(String deptNo) {
        this.deptId = deptNo;
    }

    public String getPostId() {
        return postId;
    }
    public void setPostId(String postId) {
        this.postId = postId;
    }
    
    // 互換性のためのメソッド
    public String getPostNo() {
        return postId;
    }
    public void setPostNo(String postNo) {
        this.postId = postNo;
    }

    public int getRoleId() {
        return roleId;
    }
    public void setRoleId(int roleId) {
        this.roleId = roleId;
    }

}
