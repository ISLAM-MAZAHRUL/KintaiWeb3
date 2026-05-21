package kintai;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * roleテーブルへのデータアクセスを担当するクラス (DAO)。
 * ロール情報の検索を行う。
 */
public class RoleDao {

    private DBAccess db = new DBAccess();

    /**
     * すべてのロール情報を取得する
     * @return ロール情報のリスト
     */
    public List<RoleBean> findAll() {
        List<RoleBean> roleList = new ArrayList<>();
        String sql = "SELECT ROLE_ID, ROLE_NAME FROM role ORDER BY ROLE_ID";

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                RoleBean role = new RoleBean();
                role.setRoleId(rs.getInt("ROLE_ID"));
                role.setRoleName(rs.getString("ROLE_NAME"));
                roleList.add(role);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return roleList;
    }

    /**
     * ロールIDでロール情報を検索する
     * @param roleId ロールID
     * @return ロール情報。見つからない場合はnull
     */
    public RoleBean findByRoleId(int roleId) {
        RoleBean role = null;
        String sql = "SELECT ROLE_ID, ROLE_NAME FROM role WHERE ROLE_ID = ?";

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, roleId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    role = new RoleBean();
                    role.setRoleId(rs.getInt("ROLE_ID"));
                    role.setRoleName(rs.getString("ROLE_NAME"));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return role;
    }
}
