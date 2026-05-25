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

    public List<Map<String, Object>> findYukyuSummary() {
        List<Map<String, Object>> list = new ArrayList<>();

        LocalDate today = LocalDate.now();

        LocalDate thisYearStart = today.getMonthValue() >= 7
                ? LocalDate.of(today.getYear(), 7, 1)
                : LocalDate.of(today.getYear() - 1, 7, 1);

        LocalDate lastYearStart = thisYearStart.minusYears(1);

        String sql = """
            SELECT
                e.EMP_ID,
                e.EMP_NAME,
                SUM(CASE
                    WHEN lb.GRANT_DATE >= ? AND lb.LEAVE_TYPE_ID = 1
                    THEN lb.GRANTED_DAYS - lb.USED_DAYS
                    ELSE 0 END) AS THIS_YEAR_REMAINING,

                SUM(CASE
                    WHEN lb.GRANT_DATE >= ? AND lb.GRANT_DATE < ? AND lb.LEAVE_TYPE_ID = 1
                    AND lb.EXPIRE_DATE >= CURDATE()
                    THEN lb.GRANTED_DAYS - lb.USED_DAYS
                    ELSE 0 END) AS LAST_YEAR_REMAINING,

                SUM(CASE
                    WHEN lb.LEAVE_TYPE_ID = 1 AND lb.EXPIRE_DATE >= CURDATE()
                    THEN lb.GRANTED_DAYS - lb.USED_DAYS
                    ELSE 0 END) AS TOTAL_REMAINING,

                SUM(CASE
                    WHEN lb.LEAVE_TYPE_ID = 1
                    THEN lb.USED_DAYS
                    ELSE 0 END) AS USED

            FROM leave_balance lb
            JOIN emp e ON lb.EMP_ID = e.EMP_ID
            WHERE e.IS_ACTIVE = 1
            AND lb.EXPIRE_DATE >= CURDATE()
            AND lb.GRANT_DATE >= ?
            GROUP BY e.EMP_ID, e.EMP_NAME
            ORDER BY e.EMP_ID
        """;

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setDate(1, java.sql.Date.valueOf(thisYearStart));
            ps.setDate(2, java.sql.Date.valueOf(lastYearStart));
            ps.setDate(3, java.sql.Date.valueOf(thisYearStart));
            ps.setDate(4, java.sql.Date.valueOf(lastYearStart));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new java.util.HashMap<>();
                    row.put("empId", rs.getString("EMP_ID"));
                    row.put("empName", rs.getString("EMP_NAME"));
                    row.put("thisYearRemaining", rs.getInt("THIS_YEAR_REMAINING"));
                    row.put("lastYearRemaining", rs.getInt("LAST_YEAR_REMAINING"));
                    row.put("totalRemaining", rs.getInt("TOTAL_REMAINING"));
                    row.put("used", rs.getInt("USED"));
                    list.add(row);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    // 指定従業員・休暇種別の残日数を計算
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

                    if (expireDate != null &&
                            expireDate.toLocalDate().isBefore(today)) {
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

    // 申請分の日数を使用済みに反映
    public boolean consumeLeaveDays(String emp_id,
                                    int leaveTypeId,
                                    int daysToUse) throws Exception {

        if (daysToUse <= 0) return false;

        String selectSql = """
            SELECT grant_date, granted_days, used_days
            FROM leave_balance
            WHERE emp_id = ?
            AND leave_type_id = ?
            AND (expire_date IS NULL OR expire_date >= ?)
            ORDER BY grant_date DESC
        """;

        String updateSql = """
            UPDATE leave_balance
            SET used_days = ?
            WHERE emp_id = ?
            AND leave_type_id = ?
            AND grant_date = ?
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

                    LocalDate grantDate =
                            rs.getDate("grant_date").toLocalDate();

                    int granted = rs.getInt("granted_days");
                    int used = rs.getInt("used_days");
                    int available = granted - used;

                    if (available <= 0) continue;

                    int consume = Math.min(available, daysToUse);
                    int newUsed = used + consume;

                    updatePs.setInt(1, newUsed);
                    updatePs.setString(2, emp_id);
                    updatePs.setInt(3, leaveTypeId);
                    updatePs.setDate(4,
                            java.sql.Date.valueOf(grantDate));
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

    // ===== 付与履歴取得 =====
    public List<Map<String, Object>> findGrantHistory(String empId) {

        List<Map<String, Object>> list = new ArrayList<>();

        String sql = """
            SELECT lb.BALANCE_ID, lb.GRANT_DATE, lb.EXPIRE_DATE,
                   lb.GRANTED_DAYS, lb.USED_DAYS,
                   (lb.GRANTED_DAYS - lb.USED_DAYS) AS REMAINING,
                   lt.LEAVE_TYPE_NAME
            FROM leave_balance lb
            JOIN leave_type lt
            ON lb.LEAVE_TYPE_ID = lt.LEAVE_TYPE_ID
            WHERE lb.EMP_ID = ?
            AND lb.LEAVE_TYPE_ID = 1
            ORDER BY lb.GRANT_DATE DESC
        """;

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, empId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {

                    Map<String, Object> row =
                            new java.util.HashMap<>();

                    row.put("balanceId",
                            rs.getInt("BALANCE_ID"));
                    row.put("grantDate",
                            rs.getDate("GRANT_DATE"));
                    row.put("expireDate",
                            rs.getDate("EXPIRE_DATE"));
                    row.put("grantedDays",
                            rs.getInt("GRANTED_DAYS"));
                    row.put("usedDays",
                            rs.getInt("USED_DAYS"));
                    row.put("remaining",
                            rs.getInt("REMAINING"));
                    row.put("leaveTypeName",
                            rs.getString("LEAVE_TYPE_NAME"));

                    list.add(row);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    // ===== 付与済みチェック =====
    public boolean isAlreadyGranted(String empId,
                                    int leaveTypeId,
                                    LocalDate grantDate) {

        String sql = """
            SELECT COUNT(*) AS CNT
            FROM leave_balance
            WHERE EMP_ID = ?
            AND LEAVE_TYPE_ID = ?
            AND GRANT_DATE = ?
        """;

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, empId);
            ps.setInt(2, leaveTypeId);
            ps.setDate(3,
                    java.sql.Date.valueOf(grantDate));

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("CNT") > 0;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    // ===== 有給付与 =====
    public void grantLeave(String empId,
                           int leaveTypeId,
                           LocalDate grantDate,
                           LocalDate expireDate,
                           int grantedDays,
                           String createdBy) {

        String sql = """
            INSERT INTO leave_balance
            (EMP_ID, LEAVE_TYPE_ID, GRANT_DATE,
             EXPIRE_DATE, GRANTED_DAYS, USED_DAYS,
             SOURCE, CREATED_BY, UPDATED_BY)
            VALUES (?, ?, ?, ?, ?, 0,
                    'auto', ?, ?)
        """;

        try (Connection conn = db.getConnection();
             PreparedStatement ps =
                     conn.prepareStatement(sql)) {

            ps.setString(1, empId);
            ps.setInt(2, leaveTypeId);
            ps.setDate(3,
                    java.sql.Date.valueOf(grantDate));
            ps.setDate(4,
                    java.sql.Date.valueOf(expireDate));
            ps.setInt(5, grantedDays);
            ps.setString(6, createdBy);
            ps.setString(7, createdBy);

            ps.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}