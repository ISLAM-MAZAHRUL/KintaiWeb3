package kintai;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

public class LeaveGrantDao {

    private final DBAccess db = new DBAccess();

    public static final int LEAVE_TYPE_ANNUAL = 1;
    public static final int LEAVE_TYPE_SPECIAL = 2;
    public static final int LEAVE_TYPE_SUBSTITUTE = 3;
    public static final int LEAVE_TYPE_INITIAL_3M = 11;
    public static final int LEAVE_TYPE_INITIAL_6M = 12;

    // 出勤率8割以上か
    public boolean isEligible(EmpBean emp, LocalDate baseDate) {
        LocalDate start = emp.getEmpDate();
        if (start == null || baseDate.isBefore(start)) return false;

        LocalDate periodStart = baseDate.minusMonths(6); // 直近6か月のみ

        try (Connection conn = db.getConnection()) {
            int scheduled = countScheduledWorkDays(periodStart, baseDate, conn);
            int actual = countActualWorkDays(emp.getEmpId(), periodStart, baseDate, conn);

            System.out.println("emp=" + emp.getEmpId() + ", scheduled=" + scheduled + ", actual=" + actual);
            return scheduled > 0 && ((double) actual / scheduled) >= 0.8;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private int countScheduledWorkDays(LocalDate from, LocalDate to, Connection conn) throws SQLException {
        int count = 0;
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            int dow = date.getDayOfWeek().getValue();
            if (dow >= 6 || isHoliday(date, conn)) continue;
            count++;
        }
        return count;
    }

    private boolean isHoliday(LocalDate date, Connection conn) throws SQLException {
        String sql = "SELECT is_work FROM calendar_event WHERE event_date = ? AND is_deleted = FALSE";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(date));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return !rs.getBoolean("is_work"); // 登録があればそれに従う
                } else {
                    // 登録がなければ → 土日なら休日、平日なら出勤日
                    int dow = date.getDayOfWeek().getValue(); // 月=1, 日=7
                    return dow == 6 || dow == 7;
                }
            }
        }
    }

    private int countActualWorkDays(String empId, LocalDate from, LocalDate to, Connection conn) throws SQLException {
        String sql = "SELECT COUNT(DISTINCT kintai_date) FROM kintai WHERE emp_id = ? AND kintai_date BETWEEN ? AND ? AND is_deleted = FALSE";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, empId);
            ps.setDate(2, Date.valueOf(from));
            ps.setDate(3, Date.valueOf(to));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public boolean alreadyGranted(String empId, LocalDate date, int typeId) {
        String sql = "SELECT COUNT(*) FROM leave_balance WHERE emp_id = ? AND leave_type_id = ? AND grant_date = ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, empId);
            ps.setInt(2, typeId);
            ps.setDate(3, Date.valueOf(date));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return true;
        }
    }
    
    public boolean isSubstituteLeaveNotGranted(EmpBean emp, LocalDate baseDate) {
        return getUncompensatedWorkDays(emp.getEmpId(), baseDate) > 0;
    }
    

    public boolean grantAnnualLeave(EmpBean emp, LocalDate date, String user) {
    	System.out.println("grantAnnualLeave: emp=" + emp.getEmpId() + ", date=" + date + ", isEligible=" + isEligible(emp, date) + ", alreadyGranted=" + alreadyGranted(emp.getEmpId(), date, LEAVE_TYPE_ANNUAL));
    	if (!isEligible(emp, date) || alreadyGranted(emp.getEmpId(), date, LEAVE_TYPE_ANNUAL)) return false;
        int days = calcGrantedDays(emp.getEmpDate(), date);
        return insertLeaveBalance(emp.getEmpId(), LEAVE_TYPE_ANNUAL, date, date.plusYears(2), days, "auto", user); // 翌年まで繰り越せる
    }

    public boolean grantInitialAnnualLeave(EmpBean emp, int stage, LocalDate baseDate, String user) {
        int days = 5;
        int type = (stage == 1) ? LEAVE_TYPE_INITIAL_3M : LEAVE_TYPE_INITIAL_6M;
        LocalDate target = (stage == 1) ? emp.getEmpDate().plusMonths(3) : emp.getEmpDate().plusMonths(6);
        if (baseDate.isBefore(target) || alreadyGranted(emp.getEmpId(), target, type) || !isEligible(emp, target)) return false;
        return insertLeaveBalance(emp.getEmpId(), type, target, target.plusYears(1), days, "初回" + stage, user);
    }

    public boolean grantSpecialLeave(EmpBean emp, LocalDate date, String user) {
        if (alreadyGranted(emp.getEmpId(), date, LEAVE_TYPE_SPECIAL)) return false;
        return insertLeaveBalance(emp.getEmpId(), LEAVE_TYPE_SPECIAL, date, date.plusYears(1), 5, "特別休暇", user);
    }

    public boolean grantCompLeave(EmpBean emp, LocalDate workDate, String user) {
        if (alreadyGranted(emp.getEmpId(), workDate, LEAVE_TYPE_SUBSTITUTE)) return false;
        return insertLeaveBalance(emp.getEmpId(), LEAVE_TYPE_SUBSTITUTE, workDate, workDate.plusMonths(1), 1, "代休", user);
    }

    public int grantAllPendingCompLeaves(EmpBean emp, LocalDate baseDate, String user) {
        int granted = 0;
        String sql = """
            SELECT k.kintai_date FROM kintai k
            JOIN calendar_event c ON k.kintai_date = c.event_date
            WHERE k.emp_id = ? AND k.kintai_date <= ? AND c.is_work = FALSE AND k.is_deleted = FALSE
            AND NOT EXISTS (
                SELECT 1 FROM leave_balance lb
                WHERE lb.emp_id = k.emp_id AND lb.leave_type_id = ? AND lb.grant_date = k.kintai_date
            )
        """;

        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, emp.getEmpId());
            ps.setDate(2, Date.valueOf(baseDate));
            ps.setInt(3, LEAVE_TYPE_SUBSTITUTE);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    LocalDate workDate = rs.getDate("kintai_date").toLocalDate();
                    boolean ok = insertLeaveBalance(emp.getEmpId(), LEAVE_TYPE_SUBSTITUTE, workDate, workDate.plusMonths(1), 1, "代休", user);
                    if (ok) granted++;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return granted;
    }
    
    
    public int calcGrantedDays(LocalDate empDate, LocalDate baseDate) {
        int months = (baseDate.getYear() - empDate.getYear()) * 12 + baseDate.getMonthValue() - empDate.getMonthValue();
        int years = months / 12;
        return (years >= 6) ? 20 : 10 + years;
    }

    public int calcGrantedDays(EmpBean emp, int typeId) {
        return switch (typeId) {
            case LEAVE_TYPE_ANNUAL -> calcGrantedDays(emp.getEmpDate(), LocalDate.now());
            case LEAVE_TYPE_INITIAL_3M, LEAVE_TYPE_INITIAL_6M, LEAVE_TYPE_SPECIAL -> 5;
            case LEAVE_TYPE_SUBSTITUTE -> getUncompensatedWorkDays(emp.getEmpId(), LocalDate.now());
            default -> 0;
        };
    }

    private int getUncompensatedWorkDays(String empId, LocalDate until) {
        String sql = """
            SELECT COUNT(*) FROM kintai k
            JOIN calendar_event c ON k.kintai_date = c.event_date
            WHERE k.emp_id = ? AND k.kintai_date <= ? AND c.is_work = FALSE AND k.is_deleted = FALSE
              AND NOT EXISTS (
                SELECT 1 FROM leave_balance lb
                WHERE lb.emp_id = k.emp_id AND lb.leave_type_id = ? AND lb.grant_date = k.kintai_date
              )
        """;
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, empId);
            ps.setDate(2, Date.valueOf(until));
            ps.setInt(3, LEAVE_TYPE_SUBSTITUTE);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    private boolean insertLeaveBalance(String empId, int type, LocalDate grant, LocalDate expire, int days, String source, String user) {
        String sql = "INSERT INTO leave_balance (emp_id, leave_type_id, grant_date, expire_date, granted_days, used_days, source, created_at, created_by, updated_at, updated_by) " +
                     "VALUES (?, ?, ?, ?, ?, 0, ?, CURRENT_TIMESTAMP, ?, CURRENT_TIMESTAMP, ?)";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, empId);
            ps.setInt(2, type);
            ps.setDate(3, Date.valueOf(grant));
            ps.setDate(4, Date.valueOf(expire));
            ps.setInt(5, days);
            ps.setString(6, source);
            ps.setString(7, user);
            ps.setString(8, user);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
