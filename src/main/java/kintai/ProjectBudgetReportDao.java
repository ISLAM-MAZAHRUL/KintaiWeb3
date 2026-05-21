package kintai;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
public class ProjectBudgetReportDao {
    
    private DBAccess db = new DBAccess();
    
    /**
     * 指定されたプロジェクトと月の参加メンバーの実績レポートを取得
     * 新規入力時は前月の時給を初期値として表示（前月データがない場合は空白）
     */
    public List<ProjectMemberReportBean> getProjectMemberReports(int projectId, String month) {
        List<ProjectMemberReportBean> reports = new ArrayList<>();
        
        // 当月の工数を取得し、時給は当月優先、なければ前月のデータを取得するSQL
        String sql = "SELECT " +
                    "    e.EMP_ID, " +
                    "    e.EMP_NAME, " +
                    "    SUM(CASE WHEN DATE_FORMAT(wa.WORK_DATE, '%Y-%m') = ? THEN wa.WORK_HOURS ELSE 0 END) as MONTHLY_HOURS, " +
                    "    SUM(wa.WORK_HOURS) as TOTAL_PROJECT_HOURS, " +
                    "    COALESCE(current_month.HOURLY_RATE, prev_month.HOURLY_RATE, 0) as HOURLY_RATE, " +
                    "    NULL as ACTUAL_AMOUNT " +  // 初期表示では実績額を計算しない
                    "FROM work_alloc wa " +
                    "INNER JOIN emp e ON wa.EMP_ID = e.EMP_ID " +
                    "LEFT JOIN hourly_rate_monthly current_month ON e.EMP_ID = current_month.EMP_ID " +
                    "    AND wa.PROJECT_ID = current_month.PROJECT_ID " +
                    "    AND DATE_FORMAT(current_month.TARGET_MONTH, '%Y-%m') = ? " +
                    "LEFT JOIN hourly_rate_monthly prev_month ON e.EMP_ID = prev_month.EMP_ID " +
                    "    AND wa.PROJECT_ID = prev_month.PROJECT_ID " +
                    "    AND DATE_FORMAT(prev_month.TARGET_MONTH, '%Y-%m') = DATE_FORMAT(DATE_SUB(STR_TO_DATE(CONCAT(?, '-01'), '%Y-%m-%d'), INTERVAL 1 MONTH), '%Y-%m') " +
                    "WHERE wa.PROJECT_ID = ? " +
                    "GROUP BY e.EMP_ID, e.EMP_NAME, current_month.HOURLY_RATE, prev_month.HOURLY_RATE " +
                    "HAVING SUM(CASE WHEN DATE_FORMAT(wa.WORK_DATE, '%Y-%m') = ? THEN wa.WORK_HOURS ELSE 0 END) > 0 " +
                    "ORDER BY e.EMP_ID";
        
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, month);    // 当月の工数計算用
            stmt.setString(2, month);    // 当月の時給取得用
            stmt.setString(3, month);    // 前月の時給取得用（前月計算に使用）
            stmt.setInt(4, projectId);   // プロジェクトID
            stmt.setString(5, month);    // 当月の工数フィルタ用
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    ProjectMemberReportBean report = new ProjectMemberReportBean();
                    report.setEmpNo(rs.getString("EMP_ID"));
                    report.setEmpName(rs.getString("EMP_NAME"));
                    report.setTotalHours(rs.getBigDecimal("MONTHLY_HOURS"));
                    report.setTotalProjectHours(rs.getBigDecimal("TOTAL_PROJECT_HOURS"));
                    
                    // 時給：当月データがあれば当月、なければ前月、どちらもなければ0
                    BigDecimal hourlyRate = rs.getBigDecimal("HOURLY_RATE");
                    if (hourlyRate != null && hourlyRate.compareTo(BigDecimal.ZERO) > 0) {
                        report.setHourlyRate(hourlyRate);
                    } else {
                        report.setHourlyRate(null); // 0の場合は空白表示
                    }
                    
                    report.setActualAmount(null); // 初期表示では null
                    report.setPersonalBudget(null); // 初期表示では null
                    report.setPersonalBudgetVariance(null); // 初期表示では null
                    
                    reports.add(report);
                }
            }
            
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return reports;
    }
    
    /**
     * 既に保存された時給データを使用してプロジェクトメンバーレポートを取得（集計後表示用）
     */
    public List<ProjectMemberReportBean> getProjectMemberReportsWithCalculation(int projectId, String month) {
        List<ProjectMemberReportBean> reports = new ArrayList<>();
        
        // 保存された時給データを使用して実績額を計算するSQL（当月の工数のみ使用）
        String sql = "SELECT " +
                    "    e.EMP_ID, " +
                    "    e.EMP_NAME, " +
                    "    SUM(CASE WHEN DATE_FORMAT(wa.WORK_DATE, '%Y-%m') = ? THEN wa.WORK_HOURS ELSE 0 END) as MONTHLY_HOURS, " +
                    "    SUM(wa.WORK_HOURS) as TOTAL_PROJECT_HOURS, " +
                    "    COALESCE(hrm.HOURLY_RATE, 0) as HOURLY_RATE, " +
                    "    (SUM(CASE WHEN DATE_FORMAT(wa.WORK_DATE, '%Y-%m') = ? THEN wa.WORK_HOURS ELSE 0 END) * COALESCE(hrm.HOURLY_RATE, 0)) as ACTUAL_AMOUNT " +
                    "FROM work_alloc wa " +
                    "INNER JOIN emp e ON wa.EMP_ID = e.EMP_ID " +
                    "LEFT JOIN hourly_rate_monthly hrm ON e.EMP_ID = hrm.EMP_ID " +
                    "    AND wa.PROJECT_ID = hrm.PROJECT_ID " +
                    "    AND DATE_FORMAT(hrm.TARGET_MONTH, '%Y-%m') = ? " +
                    "    AND (hrm.IS_DELETED IS NULL OR hrm.IS_DELETED = FALSE) " +
                    "WHERE wa.PROJECT_ID = ? " +
                    "GROUP BY e.EMP_ID, e.EMP_NAME, hrm.HOURLY_RATE " +
                    "HAVING SUM(CASE WHEN DATE_FORMAT(wa.WORK_DATE, '%Y-%m') = ? THEN wa.WORK_HOURS ELSE 0 END) > 0 " +
                    "ORDER BY e.EMP_ID";
        
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, month);
            stmt.setString(2, month);
            stmt.setString(3, month);
            stmt.setInt(4, projectId);
            stmt.setString(5, month);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    ProjectMemberReportBean report = new ProjectMemberReportBean();
                    report.setEmpNo(rs.getString("EMP_ID"));
                    report.setEmpName(rs.getString("EMP_NAME"));
                    report.setTotalHours(rs.getBigDecimal("MONTHLY_HOURS"));
                    report.setTotalProjectHours(rs.getBigDecimal("TOTAL_PROJECT_HOURS"));
                    report.setHourlyRate(rs.getBigDecimal("HOURLY_RATE"));
                    report.setActualAmount(rs.getBigDecimal("ACTUAL_AMOUNT"));
                    report.setPersonalBudget(null); // 当月実績額のみ計算
                    report.setPersonalBudgetVariance(null); // 個人予算差異は計算しない
                    
                    // デバッグ出力を追加
                    System.out.println("Employee: " + report.getEmpNo() + 
                                     " | Hours: " + report.getTotalHours() + 
                                     " | HourlyRate: " + report.getHourlyRate() + 
                                     " | ActualAmount: " + report.getActualAmount());
                    
                    reports.add(report);
                }
            }
            
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return reports;
    }
    
    /**
     * 指定された月とプロジェクトで保存済みの時給データがあるかチェック
     */
    public boolean hasCalculatedData(int projectId, String month) {
        String sql = "SELECT COUNT(*) as cnt FROM hourly_rate_monthly hrm " +
                    "WHERE hrm.PROJECT_ID = ? " +
                    "  AND DATE_FORMAT(hrm.TARGET_MONTH, '%Y-%m') = ? " +
                    "  AND hrm.HOURLY_RATE > 0 " +
                    "  AND (hrm.IS_DELETED IS NULL OR hrm.IS_DELETED = FALSE)";
        
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, projectId);
            stmt.setString(2, month);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("cnt") > 0;
                }
            }
            
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return false;
    }
    
    /**
     * 指定された従業員、プロジェクト、月の時給を更新または挿入
     */
    public boolean updateHourlyRate(String empId, int projectId, String month, BigDecimal hourlyRate) {
        System.out.println("updateHourlyRate called with: empId=" + empId + ", projectId=" + projectId + ", month=" + month + ", hourlyRate=" + hourlyRate);
        // 従業員が実際に存在するかチェック
        String checkEmpSql = "SELECT COUNT(*) as cnt FROM emp WHERE EMP_ID = ? AND IS_ACTIVE = TRUE";
        
        // 既存レコードの確認
        String checkExistingSql = "SELECT RATE_ID FROM hourly_rate_monthly " +
                                "WHERE EMP_ID = ? AND PROJECT_ID = ? AND DATE_FORMAT(TARGET_MONTH, '%Y-%m') = ? " +
                                "  AND (IS_DELETED IS NULL OR IS_DELETED = FALSE)";
        
        // 更新SQL
        String updateSql = "UPDATE hourly_rate_monthly SET HOURLY_RATE = ?, UPDATED_AT = NOW(), UPDATED_BY = ? " +
                          "WHERE EMP_ID = ? AND PROJECT_ID = ? AND DATE_FORMAT(TARGET_MONTH, '%Y-%m') = ? " +
                          "  AND (IS_DELETED IS NULL OR IS_DELETED = FALSE)";
        
        // 挿入SQL
        String insertSql = "INSERT INTO hourly_rate_monthly (EMP_ID, PROJECT_ID, TARGET_MONTH, HOURLY_RATE, IS_DELETED, CREATED_AT, CREATED_BY, UPDATED_AT, UPDATED_BY) " +
                          "VALUES (?, ?, STR_TO_DATE(?, '%Y-%m-%d'), ?, FALSE, NOW(), ?, NOW(), ?)";
        
        try (Connection conn = db.getConnection()) {
            conn.setAutoCommit(false);
            
            try {
                // 従業員の存在確認
                try (PreparedStatement checkEmpStmt = conn.prepareStatement(checkEmpSql)) {
                    checkEmpStmt.setString(1, empId);
                    try (ResultSet rs = checkEmpStmt.executeQuery()) {
                        if (rs.next() && rs.getInt("cnt") == 0) {
                            System.out.println("Employee not found: " + empId);
                            return false; // 従業員が見つからない
                        }
                    }
                }
                
                // 既存レコードの確認
                boolean recordExists = false;
                try (PreparedStatement checkStmt = conn.prepareStatement(checkExistingSql)) {
                    checkStmt.setString(1, empId);
                    checkStmt.setInt(2, projectId);
                    checkStmt.setString(3, month);
                    try (ResultSet rs = checkStmt.executeQuery()) {
                        recordExists = rs.next();
                        System.out.println("Record exists check for empId=" + empId + ", projectId=" + projectId + ", month=" + month + ": " + recordExists);
                    }
                }
                
                int updateCount = 0;
                if (recordExists) {
                    // 既存レコードを更新
                    System.out.println("Updating existing record...");
                    try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                        updateStmt.setBigDecimal(1, hourlyRate);
                        updateStmt.setString(2, empId); // UPDATED_BY
                        updateStmt.setString(3, empId);
                        updateStmt.setInt(4, projectId);
                        updateStmt.setString(5, month);
                        updateCount = updateStmt.executeUpdate();
                        System.out.println("Update count: " + updateCount);
                    }
                } else {
                    // 新規レコードを挿入
                    System.out.println("Inserting new record...");
                    try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                        insertStmt.setString(1, empId);
                        insertStmt.setInt(2, projectId);
                        insertStmt.setString(3, month + "-01"); // YYYY-MM-01形式で保存
                        insertStmt.setBigDecimal(4, hourlyRate);
                        insertStmt.setString(5, empId); // CREATED_BY
                        insertStmt.setString(6, empId); // UPDATED_BY
                        updateCount = insertStmt.executeUpdate();
                        System.out.println("Insert count: " + updateCount);
                    }
                }
                
                if (updateCount > 0) {
                    conn.commit();
                    System.out.println("Transaction committed successfully");
                    return true;
                } else {
                    conn.rollback();
                    System.out.println("Transaction rolled back - updateCount was 0");
                    return false;
                }
                
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
            
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 複数の従業員の時給を一括で更新または挿入
     */
    public boolean batchUpdateHourlyRates(List<ProjectMemberReportBean> reports, int projectId, String month) {
        if (reports == null || reports.isEmpty()) {
            return false;
        }
        
        try (Connection conn = db.getConnection()) {
            conn.setAutoCommit(false);
            
            try {
                for (ProjectMemberReportBean report : reports) {
                    if (report.getHourlyRate() != null && report.getHourlyRate().compareTo(BigDecimal.ZERO) > 0) {
                        boolean success = updateHourlyRateWithConnection(conn, report.getEmpNo(), projectId, month, report.getHourlyRate());
                        if (!success) {
                            conn.rollback();
                            return false;
                        }
                    }
                }
                
                conn.commit();
                return true;
                
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
            
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 接続を指定して時給を更新（バッチ処理用）
     */
    private boolean updateHourlyRateWithConnection(Connection conn, String empId, int projectId, String month, BigDecimal hourlyRate) throws SQLException {
        // 既存レコードの確認
        String checkExistingSql = "SELECT RATE_ID FROM hourly_rate_monthly " +
                                "WHERE EMP_ID = ? AND PROJECT_ID = ? AND DATE_FORMAT(TARGET_MONTH, '%Y-%m') = ? " +
                                "  AND (IS_DELETED IS NULL OR IS_DELETED = FALSE)";
        
        // 更新SQL
        String updateSql = "UPDATE hourly_rate_monthly SET HOURLY_RATE = ?, UPDATED_AT = NOW(), UPDATED_BY = ? " +
                          "WHERE EMP_ID = ? AND PROJECT_ID = ? AND DATE_FORMAT(TARGET_MONTH, '%Y-%m') = ? " +
                          "  AND (IS_DELETED IS NULL OR IS_DELETED = FALSE)";
        
        // 挿入SQL
        String insertSql = "INSERT INTO hourly_rate_monthly (EMP_ID, PROJECT_ID, TARGET_MONTH, HOURLY_RATE, IS_DELETED, CREATED_AT, CREATED_BY, UPDATED_AT, UPDATED_BY) " +
                          "VALUES (?, ?, STR_TO_DATE(?, '%Y-%m-%d'), ?, FALSE, NOW(), ?, NOW(), ?)";
        
        // 既存レコードの確認
        boolean recordExists = false;
        try (PreparedStatement checkStmt = conn.prepareStatement(checkExistingSql)) {
            checkStmt.setString(1, empId);
            checkStmt.setInt(2, projectId);
            checkStmt.setString(3, month);
            try (ResultSet rs = checkStmt.executeQuery()) {
                recordExists = rs.next();
            }
        }
        
        int updateCount = 0;
        if (recordExists) {
            // 既存レコードを更新
            try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                updateStmt.setBigDecimal(1, hourlyRate);
                updateStmt.setString(2, empId); // UPDATED_BY
                updateStmt.setString(3, empId);
                updateStmt.setInt(4, projectId);
                updateStmt.setString(5, month);
                updateCount = updateStmt.executeUpdate();
            }
        } else {
            // 新規レコードを挿入
            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                insertStmt.setString(1, empId);
                insertStmt.setInt(2, projectId);
                insertStmt.setString(3, month + "-01"); // YYYY-MM-01形式で保存
                insertStmt.setBigDecimal(4, hourlyRate);
                insertStmt.setString(5, empId); // CREATED_BY
                insertStmt.setString(6, empId); // UPDATED_BY
                updateCount = insertStmt.executeUpdate();
            }
        }
        
        return updateCount > 0;
    }
}