package kintai;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * empテーブルへのデータアクセスを担当するクラス (DAO)。
 * ※重要：このコードは、最新の「ER図」に準拠しています。
 */
public class UserDao {

    private DBAccess db = new DBAccess();

    /**
     * 従業員番号とパスワードを基にデータベースを検索し、ユーザー情報を取得する。
     * 新しいempテーブルの構造に合わせて修正。
     * @param empno ログイン画面で入力された従業員番号
     * @param password ログイン画面で入力されたパスワード
     * @return ユーザーが見つかった場合はUserBeanオブジェクト、見つからない、またはエラーの場合はnull
     */
    public UserBean findByLoginInfo(String empno, String password) {
        UserBean user = null;

        // --- SQL文 ---
        // テーブル名: emp
        // 検索列: EMPNO
        // 取得列: EMPNO, EMPNAME, DEPTNO, POSTNO, ROLEID, GRADENO, PASS
        // ※PASS列は認証のためだけに取得し、UserBeanには格納しない（セキュリティのため）。
        String sql = "SELECT EMP_ID, EMP_NAME, DEPT_ID, POST_ID, ROLE_ID, PASS FROM emp WHERE EMP_ID = ? AND IS_ACTIVE = true";
        
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            // プレースホルダに値をセット
            ps.setString(1, empno); // 1番目の「?」は EMPNO に対応

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    // データベースから取得したパスワード
                    String storedPassword = rs.getString("PASS");

                    // パスワードの直接比較 (ハッシュ化なし)
                    // !!! 注意: この直接比較はテスト目的のみで、本番環境ではハッシュ化されたパスワードを比較してください !!!
                    if (storedPassword != null && storedPassword.equals(password)) {
                        user = new UserBean();
                        // --- ResultSetからUserBeanへのマッピング ---
                        user.setEmpId(rs.getString("EMP_ID"));
                        user.setName(rs.getString("EMP_NAME"));
                        user.setDeptId(rs.getString("DEPT_ID"));
                        user.setPostId(rs.getString("POST_ID"));
                        user.setRoleId(rs.getInt("ROLE_ID"));
                    }
                }
            }
        } catch (SQLException e) { // SQLExceptionを明示的にキャッチ
            e.printStackTrace();
            // データベースエラーの場合
        } catch (Exception e) {
            e.printStackTrace();
            // その他の予期せぬエラーの場合
        }
        return user;
    }

    /**
     * 従業員のパスワードを更新する。
     * @param empNo 従業員番号
     * @param newPassword 新しいパスワード（テスト目的で明文、本番ではハッシュ値）
     * @return 更新に成功した場合true、失敗した場合false
     */
    public boolean updatePassword(String empNo, String newPassword) {
        String sql = "UPDATE emp SET PASS = ?, UPDATED_AT = NOW(), UPDATED_BY = 'system' WHERE EMP_ID = ? AND IS_ACTIVE = true";
        
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            // !!! 注意: ここでnewPasswordをハッシュ化してセットすべきですが、
            //            テスト目的のため、現在のシンプルなパスワード形式に合わせて明文をセットします !!!
            ps.setString(1, newPassword); 
            ps.setString(2, empNo);
            
            int count = ps.executeUpdate();
            return count > 0;
            
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("パスワードの更新に失敗しました: " + empNo);
        }
        return false;
    }

    /**
     * 従業員の現在のパスワードを検証する（パスワード変更機能用）。
     * @param empNo 従業員番号
     * @param currentPassword 現在入力されたパスワード（テスト目的で明文、本番ではハッシュ値）
     * @return 現在のパスワードが正しい場合true、そうでない場合false
     */
    public boolean verifyCurrentPassword(String empNo, String currentPassword) {
        String sql = "SELECT PASS FROM emp WHERE EMP_ID = ? AND IS_ACTIVE = true";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, empNo);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String storedPassword = rs.getString("PASS");
                    // パスワードの直接比較 (テスト目的のみ)
                    return storedPassword != null && storedPassword.equals(currentPassword);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("現在のパスワードの検証中にエラーが発生しました: " + empNo);
        }
        return false;
    }
    
    /**
     * 従業員IDから従業員情報を取得する
     * @param empId 従業員ID
     * @return UserBean（存在しない場合はnull）
     */
    public UserBean findByEmpId(String empId) {
        UserBean user = null;
        String sql = "SELECT EMP_ID, EMP_NAME, DEPT_ID, POST_ID, ROLE_ID FROM emp WHERE EMP_ID = ? AND IS_ACTIVE = true";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, empId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    user = new UserBean();
                    user.setEmpId(rs.getString("EMP_ID"));
                    user.setName(rs.getString("EMP_NAME"));
                    user.setDeptId(rs.getString("DEPT_ID"));
                    user.setPostId(rs.getString("POST_ID"));
                    user.setRoleId(rs.getInt("ROLE_ID"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return user;
    }

}
