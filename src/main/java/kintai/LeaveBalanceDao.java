package kintai;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LeaveBalanceDao {

    private final DBAccess db = new DBAccess();

    // 休暇残数一覧を取得（付与日順で新しい順）
    public List<LeaveBalanceBean> getLeaveBalances(String emp_id) {
        String sql = """
            SELECT lb.emp_id, lb.leave_type_id, lt.leave_type_name, lb.grant_date, lb.expire_date,
                   lb.granted_days, lb.used_days
            FROM leave_balance lb
            JOIN leave_type lt ON lb.leave_type_id = lt.leave_type_id
            WHERE lb.emp_id = ?
            ORDER BY lb.leave_type_id, lb.grant_date DESC
        """;

        List<LeaveBalanceBean> list = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, emp_id);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    LeaveBalanceBean bean = new LeaveBalanceBean();
                    bean.setEmpId(rs.getString("emp_id"));
                    bean.setLeaveTypeId(rs.getInt("leave_type_id"));
                    bean.setLeaveTypeName(rs.getString("leave_type_name"));
                    bean.setGrantedDate(rs.getDate("grant_date").toLocalDate());
                    if (rs.getDate("expire_date") != null) {
                        bean.setExpirationDate(rs.getDate("expire_date").toLocalDate());
                    }
                    bean.setGrantedDays(rs.getInt("granted_days"));
                    bean.setUsedDays(rs.getInt("used_days"));
                    list.add(bean);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }
    public List<Map<String, Object>> findAllSummary() {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT e.EMP_ID, e.EMP_NAME, " +
                     "SUM(lb.GRANTED_DAYS) AS GRANTED, " +
                     "SUM(lb.USED_DAYS) AS USED, " +
                     "SUM(lb.GRANTED_DAYS - lb.USED_DAYS) AS REMAINING " +
                     "FROM leave_balance lb " +
                     "JOIN emp e ON lb.EMP_ID = e.EMP_ID " +
                     "GROUP BY e.EMP_ID, e.EMP_NAME " +
                     "ORDER BY e.EMP_ID";

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Map<String, Object> row = new java.util.HashMap<>();
                row.put("empId", rs.getString("EMP_ID"));
                row.put("empName", rs.getString("EMP_NAME"));
                row.put("granted", rs.getInt("GRANTED"));
                row.put("used", rs.getInt("USED"));
                row.put("remaining", rs.getInt("REMAINING"));
                list.add(row);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    // 指定従業員・休暇種別の残日数を計算（期限切れ分は除外）
    public int calculateRemainingDays(String emp_id, int leaveTypeId) {
        String sql = """
            SELECT granted_days, used_days, expire_date
            FROM leave_balance
            WHERE emp_id = ? AND leave_type_id = ?
        """;
        int remaining = 0;
        LocalDate today = LocalDate.now();

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, emp_id);
            ps.setInt(2, leaveTypeId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int granted = rs.getInt("granted_days");
                    int used = rs.getInt("used_days");
                    java.sql.Date expireDate = rs.getDate("expire_date");

                    if (expireDate != null && expireDate.toLocalDate().isBefore(today)) {
                        // 期限切れなので残数に含めない
                        continue;
                    }
                    remaining += (granted - used);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return remaining;
    }

    // 申請分の日数を使用済みに反映（複数付与分の残数から優先消化）
    public boolean consumeLeaveDays(String emp_id, int leaveTypeId, int daysToUse) throws Exception {
        if (daysToUse <= 0) return false;
        String selectSql = """
            SELECT grant_date, granted_days, used_days
            FROM leave_balance
            WHERE emp_id = ? AND leave_type_id = ? AND (expire_date IS NULL OR expire_date >= ?)
            ORDER BY grant_date DESC
        """;

        String updateSql = """
            UPDATE leave_balance
            SET used_days = ?
            WHERE emp_id = ? AND leave_type_id = ? AND grant_date = ?
        """;

        LocalDate today = LocalDate.now();

        try (Connection conn = db.getConnection();
             PreparedStatement selectPs = conn.prepareStatement(selectSql);
             PreparedStatement updatePs = conn.prepareStatement(updateSql)) {

            conn.setAutoCommit(false);
            selectPs.setString(1, emp_id);
            selectPs.setInt(2, leaveTypeId);
            selectPs.setDate(3, java.sql.Date.valueOf(today));

            try (ResultSet rs = selectPs.executeQuery()) {
                while (rs.next() && daysToUse > 0) {
                    LocalDate grantDate = rs.getDate("grant_date").toLocalDate();
                    int granted = rs.getInt("granted_days");
                    int used = rs.getInt("used_days");
                    int available = granted - used;

                    if (available <= 0) continue;

                    int consume = Math.min(available, daysToUse);
                    int newUsed = used + consume;

                    updatePs.setInt(1, newUsed);
                    updatePs.setString(2, emp_id);
                    updatePs.setInt(3, leaveTypeId);
                    updatePs.setDate(4, java.sql.Date.valueOf(grantDate));
                    updatePs.executeUpdate();

                    daysToUse -= consume;
                }
            }

            if (daysToUse > 0) {
                conn.rollback();
                throw new Exception("残日数不足");
            }

            conn.commit();
            return true;
        } catch (Exception e) {
            throw e;
        }
    }
}