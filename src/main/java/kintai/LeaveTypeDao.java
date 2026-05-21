package kintai;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * leave_type テーブル（論理削除対応）へのデータアクセスクラス。
 */
public class LeaveTypeDao {

    private DBAccess db = new DBAccess();

    /** 有効な休日種別一覧を取得（IS_DELETED = false） */
    public List<LeaveTypeBean> findAll() {
        List<LeaveTypeBean> list = new ArrayList<>();
        String sql = "SELECT LEAVE_TYPE_ID, LEAVE_TYPE_NAME, IS_PAID FROM leave_type WHERE IS_DELETED = FALSE ORDER BY LEAVE_TYPE_ID";

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                LeaveTypeBean bean = new LeaveTypeBean();
                bean.setLeaveTypeId(rs.getInt("LEAVE_TYPE_ID"));
                bean.setLeaveTypeName(rs.getString("LEAVE_TYPE_NAME"));
                bean.setPaid(rs.getBoolean("IS_PAID"));
                bean.setDeleted(false);
                list.add(bean);
            }

        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }

        return list;
    }

    /** IDで休日種別を取得（削除済みも含む） */
    public LeaveTypeBean findById(int leaveTypeId) {
        String sql = "SELECT LEAVE_TYPE_ID, LEAVE_TYPE_NAME, IS_PAID, IS_DELETED FROM leave_type WHERE LEAVE_TYPE_ID = ?";
        LeaveTypeBean bean = null;

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, leaveTypeId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    bean = new LeaveTypeBean();
                    bean.setLeaveTypeId(rs.getInt("LEAVE_TYPE_ID"));
                    bean.setLeaveTypeName(rs.getString("LEAVE_TYPE_NAME"));
                    bean.setPaid(rs.getBoolean("IS_PAID"));
                    bean.setDeleted(rs.getBoolean("IS_DELETED"));
                }
            }

        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }

        return bean;
    }

    /** 新規登録（IS_DELETED = false で追加） */
    public boolean insert(LeaveTypeBean bean) {
    	String sql = "INSERT INTO leave_type (LEAVE_TYPE_ID, LEAVE_TYPE_NAME, IS_PAID, IS_DELETED, CREATED_BY, UPDATED_BY) VALUES (?, ?, ?, FALSE, ?, ?)";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

        	ps.setInt(1, bean.getLeaveTypeId());
            ps.setString(2, bean.getLeaveTypeName());
            ps.setBoolean(3, bean.isPaid());
            ps.setString(4, bean.getCreatedBy());
            ps.setString(5, bean.getCreatedBy());
            
            return ps.executeUpdate() > 0;

        } catch (SQLException | ClassNotFoundException e) {
            if ("23000".equals(((SQLException) e).getSQLState())) {
                System.err.println("休日種別IDが既に存在しています: " + bean.getLeaveTypeId());
            } else {
                e.printStackTrace();
            }
        }

        return false;
    }

    /** 更新処理（IDは変更可能） */
    public boolean update(int originalId, LeaveTypeBean bean) {
        String sql = "UPDATE leave_type SET LEAVE_TYPE_ID = ?, LEAVE_TYPE_NAME = ?, IS_PAID = ?, UPDATED_BY = ? WHERE LEAVE_TYPE_ID = ?";

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, bean.getLeaveTypeId());
            ps.setString(2, bean.getLeaveTypeName());
            ps.setBoolean(3, bean.isPaid());
            ps.setString(4, bean.getUpdatedBy());
            ps.setInt(5, originalId);

            return ps.executeUpdate() > 0;

        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }

        return false;
    }

    /** 論理削除（IS_DELETED = true に更新） */
    public boolean delete(int leaveTypeId, String updatedBy) {
        String sql = "UPDATE leave_type SET IS_DELETED = TRUE, UPDATED_BY = ? WHERE LEAVE_TYPE_ID = ?";

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, updatedBy);
            ps.setInt(2, leaveTypeId);

            return ps.executeUpdate() > 0;

        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }

        return false;
    }

    /** 復元（論理削除フラグを戻す） */
    public boolean restore(int leaveTypeId) {
        String sql = "UPDATE leave_type SET IS_DELETED = FALSE WHERE LEAVE_TYPE_ID = ?";

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, leaveTypeId);
            return ps.executeUpdate() > 0;

        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }

        return false;
    }

    /** 論理削除済み一覧を取得 */
    public List<LeaveTypeBean> findDeleted() {
        List<LeaveTypeBean> list = new ArrayList<>();
        String sql = "SELECT LEAVE_TYPE_ID, LEAVE_TYPE_NAME, IS_PAID FROM leave_type WHERE IS_DELETED = TRUE ORDER BY LEAVE_TYPE_ID";

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                LeaveTypeBean bean = new LeaveTypeBean();
                bean.setLeaveTypeId(rs.getInt("LEAVE_TYPE_ID"));
                bean.setLeaveTypeName(rs.getString("LEAVE_TYPE_NAME"));
                bean.setPaid(rs.getBoolean("IS_PAID"));
                bean.setDeleted(true);
                list.add(bean);
            }

        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }

        return list;
    }

    /** ID存在チェック（削除済みも含む） */
    public boolean exists(int leaveTypeId) {
        return findById(leaveTypeId) != null;
    }
    
    /**
     * 削除された部署を復元する
     * @param deptId 復元する部署番号
     * @return 復元に成功した場合true、失敗した場合false
     */
    public boolean restore(String deptId) {
        return restore(deptId, "system");
    }
    
    public boolean restore(String leaveTypeId, String updatedBy) {
        String sql = "UPDATE leave_type SET IS_DELETED = false, DELETED_AT = NULL, DELETED_BY = NULL, UPDATED_AT = NOW(), UPDATED_BY = ? WHERE LEAVE_TYPE_ID = ? AND IS_DELETED = true";
        
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, updatedBy);
            ps.setString(2, leaveTypeId);
            
            int count = ps.executeUpdate();
            return count > 0;
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return false;
    }
}
