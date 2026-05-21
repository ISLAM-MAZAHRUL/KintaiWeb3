package kintai;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LeaveRecDao {

    public static final int LEAVE_TYPE_PAID = 1;
    public static final int LEAVE_TYPE_SPECIAL = 2;
    public static final int LEAVE_TYPE_COMP = 3;

    private final DBAccess db = new DBAccess();

    // 休暇申請登録
    public boolean insertLeave(LeaveRecBean bean) throws Exception {
        // 期間重複チェック
        if (isOverlapping(bean.getEmpId(), bean.getLeaveTypeId(), bean.getStartDate(), bean.getEndDate(), null)) {
            throw new Exception("申請期間が重複しています");
        }

        String sql = """
            INSERT INTO leave_rec 
            (emp_id, leave_type_id, start_date, end_date, reason, approved_by, status, created_by, updated_by)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, bean.getEmpId());
            ps.setInt(2, bean.getLeaveTypeId());
            ps.setDate(3, bean.getStartDate());
            ps.setDate(4, bean.getEndDate());
            ps.setString(5, bean.getReason());
            ps.setString(6, bean.getApprovedBy());    // 承認者
            ps.setString(7, bean.getStatus());        // ステータス
            ps.setString(8, bean.getCreatedBy());
            ps.setString(9, bean.getUpdatedBy());

            int result = ps.executeUpdate();
            if (result > 0) {
                recalculateUsedDays(bean.getEmpId(), bean.getLeaveTypeId());
                return true;
            }
        }
        return false;
    }


    // 休暇申請更新
    public boolean updateLeave(LeaveRecBean bean) throws Exception {
        if (isOverlapping(bean.getEmpId(), bean.getLeaveTypeId(), bean.getStartDate(), bean.getEndDate(), bean.getLeaveId())) {
            throw new Exception("申請期間が重複しています");
        }

        String sql = """
            UPDATE leave_rec
            SET start_date = ?, end_date = ?, reason = ?, updated_by = ?, updated_at = CURRENT_TIMESTAMP
            WHERE leave_id = ?
        """;

        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, bean.getStartDate());
            ps.setDate(2, bean.getEndDate());
            ps.setString(3, bean.getReason());
            ps.setString(4, bean.getUpdatedBy());
            ps.setInt(5, bean.getLeaveId());
            int result = ps.executeUpdate();
            if (result > 0) {
                recalculateUsedDays(bean.getEmpId(), bean.getLeaveTypeId());
                return true;
            }
        }
        return false;
    }

    // 論理削除
    public boolean logicalDeleteLeave(int leaveId, String updatedBy) throws Exception {
        LeaveRecBean bean = findById(leaveId);
        if (bean == null) throw new Exception("指定された休暇申請が存在しません");

        String sql = """
            UPDATE leave_rec
            SET is_deleted = TRUE, updated_by = ?, updated_at = CURRENT_TIMESTAMP
            WHERE leave_id = ?
        """;

        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, updatedBy);
            ps.setInt(2, leaveId);
            int result = ps.executeUpdate();
            if (result > 0) {
                recalculateUsedDays(bean.getEmpId(), bean.getLeaveTypeId());
                return true;
            }
        }
        return false;
    }

    // 使用日数再計算（消化優先：grant_dateの新しい順）
    public void recalculateUsedDays(String empId, int leaveTypeId) throws Exception {
        String sumSql = """
            SELECT SUM(DATEDIFF(end_date, start_date) + 1) AS total_days
            FROM leave_rec
            WHERE emp_id = ? AND leave_type_id = ? AND (is_deleted IS NULL OR is_deleted = FALSE)
        """;

        
        
        int remaining = 0;
        try (Connection conn = db.getConnection()) {
        	
        	String resetSql = "UPDATE leave_balance SET used_days = 0 WHERE emp_id = ? AND leave_type_id = ?";
        	try (PreparedStatement resetPs = conn.prepareStatement(resetSql)) {
        	    resetPs.setString(1, empId);
        	    resetPs.setInt(2, leaveTypeId);
        	    resetPs.executeUpdate();
        	}
        	
            try (PreparedStatement sumPs = conn.prepareStatement(sumSql)) {
                sumPs.setString(1, empId);
                sumPs.setInt(2, leaveTypeId);
                try (ResultSet rs = sumPs.executeQuery()) {
                    if (rs.next()) {
                        remaining = rs.getInt("total_days");
                    }
                }
            }

            String getBalances = """
                SELECT balance_id, granted_days
                FROM leave_balance
                WHERE emp_id = ? AND leave_type_id = ? AND grant_date <= CURRENT_DATE AND expire_date >= CURRENT_DATE
                ORDER BY grant_date DESC
            """;

            try (PreparedStatement balPs = conn.prepareStatement(getBalances)) {
                balPs.setString(1, empId);
                balPs.setInt(2, leaveTypeId);
                try (ResultSet rs = balPs.executeQuery()) {
                    while (rs.next()) {
                        int balanceId = rs.getInt("balance_id");
                        int granted = rs.getInt("granted_days");
                        int used = Math.min(granted, remaining);

                        String upd = "UPDATE leave_balance SET used_days = ? WHERE balance_id = ?";
                        try (PreparedStatement updPs = conn.prepareStatement(upd)) {
                            updPs.setInt(1, used);
                            updPs.setInt(2, balanceId);
                            updPs.executeUpdate();
                        }
                        remaining -= used;
                    }
                }
            }
        }
    }

    // 期間重複チェック
    public boolean isOverlapping(String empId, int leaveTypeId, Date startDate, Date endDate, Integer excludeId) throws Exception {
    	String sql = """
    		    SELECT COUNT(*) FROM leave_rec
    		    WHERE emp_id = ? 
    		      AND leave_type_id = ? 
    		      AND (is_deleted IS NULL OR is_deleted = FALSE)
    		      AND ((start_date <= ? AND end_date >= ?) OR (start_date <= ? AND end_date >= ?))
    		""" + (excludeId != null ? " AND leave_id <> ?" : "");
    	
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            int idx = 1;
            ps.setString(idx++, empId);
            ps.setInt(idx++, leaveTypeId);
            ps.setDate(idx++, endDate);
            ps.setDate(idx++, startDate);
            ps.setDate(idx++, startDate);
            ps.setDate(idx++, endDate);
            if (excludeId != null) ps.setInt(idx, excludeId);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    // 単一取得
    public LeaveRecBean findById(int leaveId) throws Exception {
        String sql = "SELECT * FROM leave_rec WHERE leave_id = ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, leaveId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    LeaveRecBean bean = new LeaveRecBean();
                    bean.setLeaveId(rs.getInt("leave_id"));
                    bean.setEmpId(rs.getString("emp_id"));
                    bean.setLeaveTypeId(rs.getInt("leave_type_id"));
                    bean.setStartDate(rs.getDate("start_date"));
                    bean.setEndDate(rs.getDate("end_date"));
                    bean.setReason(rs.getString("reason"));
                    bean.setStatus(rs.getString("status"));
                    bean.setApprovedBy(rs.getString("approved_by")); // ←追加
                    return bean;
                }
            }
        }
        return null;
    }

    // 一覧取得
    public List<LeaveRecBean> getLeaveList(String empId) throws Exception {
        String sql = "SELECT * FROM leave_rec WHERE emp_id = ? AND (is_deleted IS NULL OR is_deleted = FALSE) ORDER BY start_date DESC";
        List<LeaveRecBean> list = new ArrayList<>();

        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, empId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    LeaveRecBean bean = new LeaveRecBean();
                    bean.setLeaveId(rs.getInt("leave_id"));
                    bean.setEmpId(rs.getString("emp_id"));
                    bean.setLeaveTypeId(rs.getInt("leave_type_id"));
                    bean.setStartDate(rs.getDate("start_date"));
                    bean.setEndDate(rs.getDate("end_date"));
                    bean.setReason(rs.getString("reason"));
                    bean.setStatus(rs.getString("status"));  // ← ここを追加
                    list.add(bean);
                }
            }
        }
        return list;
    }

    // 残日数取得（全未消化分の合計）
    public int fetchRemainingLeave(String empId, int leaveTypeId) throws Exception {
        String sql = """
            SELECT SUM(granted_days - used_days) AS remaining
            FROM leave_balance
            WHERE emp_id = ? AND leave_type_id = ? AND expire_date >= CURRENT_DATE
        """;

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, empId);
            ps.setInt(2, leaveTypeId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("remaining");
                }
            }
        }
        return 0;
    }
    
    // 承認待ち取得用メソッド追加
    public List<LeaveRecBean> findPendingByApprover(String approverId) throws Exception {
        String sql = """
            SELECT * FROM leave_rec
            WHERE approved_by = ? AND status = '承認待ち'
                  AND (is_deleted IS NULL OR is_deleted = FALSE)
            ORDER BY start_date DESC
        """;

        List<LeaveRecBean> list = new ArrayList<>();
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, approverId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    LeaveRecBean bean = new LeaveRecBean();
                    bean.setLeaveId(rs.getInt("leave_id"));
                    bean.setEmpId(rs.getString("emp_id"));
                    bean.setLeaveTypeId(rs.getInt("leave_type_id"));
                    bean.setStartDate(rs.getDate("start_date"));
                    bean.setEndDate(rs.getDate("end_date"));
                    bean.setReason(rs.getString("reason"));
                    bean.setApprovedBy(rs.getString("approved_by"));
                    bean.setStatus(rs.getString("status"));
                    
                    // 作成日を追加
                    bean.setCreatedAt(rs.getTimestamp("created_at"));
                    
                    list.add(bean);
                }
            }
        }
        return list;
    }
    
    // 承認済み更新メソッド
    public boolean updateLeaveStatus(LeaveRecBean bean) throws Exception {
        String sql = """
            UPDATE leave_rec
            SET status = ?, approved_by = ?, updated_by = ?, updated_at = CURRENT_TIMESTAMP
            WHERE leave_id = ?
        """;

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, bean.getStatus());
            ps.setString(2, bean.getApprovedBy());
            ps.setString(3, bean.getUpdatedBy());
            ps.setInt(4, bean.getLeaveId());

            // デバッグ: PreparedStatement の値確認
            System.out.println(">> SQL 実行前: status=" + bean.getStatus() +
                               ", approved_by=" + bean.getApprovedBy() +
                               ", updated_by=" + bean.getUpdatedBy() +
                               ", leaveId=" + bean.getLeaveId());

            int cnt = ps.executeUpdate();

            // デバッグ: 更新件数
            System.out.println(">> executeUpdate 件数=" + cnt);

            return cnt > 0;
        }
    }
    
    public Map<LocalDate, LeaveRecBean> getLeaveMap(String empId, YearMonth ym) throws Exception {
        Map<LocalDate, LeaveRecBean> leaveMap = new HashMap<>();

        LocalDate startOfMonth = ym.atDay(1);
        LocalDate endOfMonth = ym.atEndOfMonth();

        String sql = """
            SELECT lr.leave_id, lr.emp_id, lr.leave_type_id, lr.start_date, lr.end_date,
                   lt.leave_type_name, lt.is_paid
            FROM leave_rec lr
            JOIN leave_type lt ON lr.leave_type_id = lt.leave_type_id
            WHERE lr.emp_id = ?
              AND lr.status = '承認済み'
              AND (lr.is_deleted IS NULL OR lr.is_deleted = FALSE)
              AND lr.start_date <= ? AND lr.end_date >= ?
        """;

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, empId);
            ps.setDate(2, Date.valueOf(endOfMonth));
            ps.setDate(3, Date.valueOf(startOfMonth));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    // LeaveTypeBean を作成
                    LeaveTypeBean type = new LeaveTypeBean();
                    type.setLeaveTypeId(rs.getInt("leave_type_id"));
                    type.setLeaveTypeName(rs.getString("leave_type_name"));
                    type.setPaid(rs.getBoolean("is_paid"));

                    // LeaveRecBean を作成
                    LeaveRecBean rec = new LeaveRecBean();
                    rec.setLeaveId(rs.getInt("leave_id"));
                    rec.setEmpId(rs.getString("emp_id"));
                    rec.setLeaveTypeId(rs.getInt("leave_type_id"));
                    rec.setStartDate(rs.getDate("start_date"));
                    rec.setEndDate(rs.getDate("end_date"));
                    rec.setLeaveType(type); // ここが重要

                    // 日ごとに Map に登録（JSP で LocalDate で取得できるように）
                    LocalDate start = rec.getStartDate().toLocalDate();
                    LocalDate end = rec.getEndDate().toLocalDate();

                    // 月の範囲内のみ登録
                    for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
                        if (!d.isBefore(startOfMonth) && !d.isAfter(endOfMonth)) {
                            leaveMap.put(d, rec);
                        }
                    }
                }
            }
        }
        return leaveMap;
    }





} 
