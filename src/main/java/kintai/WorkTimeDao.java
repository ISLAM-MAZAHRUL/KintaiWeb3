package kintai;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * kintaiテーブルおよびbreakテーブル、work_allocテーブルへのデータアクセスを担当するクラス (DAO)。
 * 勤怠データ、休憩データ、工数割り当てデータの検索、追加、更新、削除を行う。
 * （旧work_time_detailテーブル関連のメソッドは削除されました）
 */
public class WorkTimeDao {

    private DBAccess db = new DBAccess();

    /**
     * 指定された従業員と日付の出退勤情報をデータベースから検索する。
     * @param empId 従業員番号
     * @param date 検索する日付
     * @return 見つかった場合はWorkTimeBeanオブジェクト、見つからない場合はnull
     */
    public WorkTimeBean findWorkTimeByDate(String empId, LocalDate date) {
        WorkTimeBean workTime = null;
        String sql = "SELECT KINTAI_REC_ID, CLOCK_IN, CLOCK_OUT, WORKING_HOURS, OVERTIME_HOURS, NIGHT_HOURS, ATTENDANCE_TYPE "
                + "FROM kintai WHERE EMP_ID = ? AND KINTAI_DATE = ?";


        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, empId);
            ps.setDate(2, Date.valueOf(date));

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    workTime = new WorkTimeBean();
                    workTime.setKintaiRecId(rs.getInt("KINTAI_REC_ID"));
                    workTime.setEmpId(empId);
                    workTime.setKintaiDate(date);
                    workTime.setClockIn(rs.getTime("CLOCK_IN"));
                    workTime.setClockOut(rs.getTime("CLOCK_OUT"));
                    // 新しいkintaiテーブルのWORKING_HOURS, OVERTIME_HOURS, NIGHT_HOURS も取得
                    // 修正箇所: setWorkingHours(BigDecimal) が WorkTimeBean で定義されたため、ここでのエラーが解消される
                    workTime.setWorkingHours(rs.getBigDecimal("WORKING_HOURS"));
                    workTime.setOvertimeHours(rs.getBigDecimal("OVERTIME_HOURS"));
                    workTime.setNightHours(rs.getBigDecimal("NIGHT_HOURS"));
                    // ★ 勤怠区分をセット
                    workTime.setAttendanceType(rs.getString("ATTENDANCE_TYPE"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return workTime;
    }

    /**
     * 指定された従業員と日付の休憩情報をすべてデータベースから検索する。
     * @param empId 従業員番号
     * @param date 検索する日付
     * @return 休憩情報のリスト。見つからない場合は空のリスト
     */
    public List<BreakBean> findBreaksByDate(String empId, LocalDate date) {
        List<BreakBean> breakList = new ArrayList<>();
        // KINTAI_REC_IDを取得し、そのKINTAI_REC_IDに紐づく休憩データを取得するようにSQLを修正
        String sql = "SELECT b.BREAK_ID, b.KINTAI_REC_ID, b.BREAK_START, b.BREAK_END " +
                     "FROM break b " +
                     "JOIN kintai k ON b.KINTAI_REC_ID = k.KINTAI_REC_ID " +
                     "WHERE k.EMP_ID = ? AND k.KINTAI_DATE = ? " +
                     "ORDER BY b.BREAK_START"; // 休憩開始時刻でソートして表示順を制御

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, empId);
            ps.setDate(2, Date.valueOf(date));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    BreakBean breakBean = new BreakBean();
                    breakBean.setBreakId(rs.getInt("BREAK_ID"));
                    breakBean.setKintaiRecId(rs.getInt("KINTAI_REC_ID"));
                    breakBean.setBreakStart(rs.getTime("BREAK_START"));
                    breakBean.setBreakEnd(rs.getTime("BREAK_END"));
                    breakList.add(breakBean);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return breakList;
    }

    /**
     * 勤怠情報（出勤・退勤時間、および計算済み工数）を保存または更新する。
     * レコードが存在しない場合はINSERT、存在する場合はUPDATEを実行する。
     * @param workTime 保存する勤怠情報
     * @return 更新されたWorkTimeBean（新しいKINTAI_REC_IDを含む）
     */
    public WorkTimeBean saveWorkTime(WorkTimeBean workTime) {
        // 先にレコードが存在するか確認
        WorkTimeBean existingWorkTime = findWorkTimeByDate(workTime.getEmpId(), workTime.getKintaiDate());

        String sql;
        if (existingWorkTime == null) {
            // INSERT処理 (WORKING_HOURSなども初期値として含める)
        	sql = "INSERT INTO kintai (KINTAI_DATE, EMP_ID, CLOCK_IN, CLOCK_OUT, WORKING_HOURS, OVERTIME_HOURS, NIGHT_HOURS, ATTENDANCE_TYPE, IS_DELETED, IS_FINALIZED) "
        		    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, FALSE, FALSE)";
        } else {
            // UPDATE処理
            workTime.setKintaiRecId(existingWorkTime.getKintaiRecId()); // 既存のIDをセット
            sql = "UPDATE kintai SET CLOCK_IN=?, CLOCK_OUT=?, WORKING_HOURS=?, OVERTIME_HOURS=?, NIGHT_HOURS=?, ATTENDANCE_TYPE=? "
            	    + "WHERE KINTAI_REC_ID=?";
        }

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {

            if (existingWorkTime == null) { // INSERTの場合
            	ps.setDate(1, Date.valueOf(workTime.getKintaiDate()));
            	ps.setString(2, workTime.getEmpId());
            	ps.setTime(3, workTime.getClockIn());
            	ps.setTime(4, workTime.getClockOut());
            	ps.setBigDecimal(5, workTime.getWorkingHours() != null ? workTime.getWorkingHours() : BigDecimal.ZERO);
            	ps.setBigDecimal(6, workTime.getOvertimeHours() != null ? workTime.getOvertimeHours() : BigDecimal.ZERO);
            	ps.setBigDecimal(7, workTime.getNightHours() != null ? workTime.getNightHours() : BigDecimal.ZERO);
            	ps.setString(8, workTime.getAttendanceType()); // ★追加
                ps.executeUpdate();
                // 新しく生成されたKINTAI_REC_IDを取得
                try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        workTime.setKintaiRecId(generatedKeys.getInt(1));
                    }
                }
            } else { // UPDATEの場合
                ps.setTime(1, workTime.getClockIn());
                ps.setTime(2, workTime.getClockOut());
                ps.setBigDecimal(3, workTime.getWorkingHours() != null ? workTime.getWorkingHours() : BigDecimal.ZERO);
                ps.setBigDecimal(4, workTime.getOvertimeHours() != null ? workTime.getOvertimeHours() : BigDecimal.ZERO);
                ps.setBigDecimal(5, workTime.getNightHours() != null ? workTime.getNightHours() : BigDecimal.ZERO);
                ps.setString(6, workTime.getAttendanceType());
                ps.setInt(7, workTime.getKintaiRecId());
                ps.executeUpdate();

            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return workTime;
    }

    /**
     * 新しい休憩記録をデータベースに追加する。
     * @param breakBean 追加する休憩情報（KINTAI_REC_ID, 開始時間, 終了時間を含む）
     * @throws SQLException 
     * @throws ClassNotFoundException 
     */
    public void addBreak(BreakBean b, String loginEmpId) throws ClassNotFoundException, SQLException {
        String sql = "INSERT INTO break (KINTAI_REC_ID, EMP_ID, BREAK_START, BREAK_END, IS_DELETED, CREATED_BY, UPDATED_BY) "
                   + "VALUES (?, ?, ?, ?, FALSE, ?, ?)";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, b.getKintaiRecId());
            ps.setString(2, b.getEmpId());   // 休憩対象者
            ps.setTime(3, b.getBreakStart());
            ps.setTime(4, b.getBreakEnd());
            ps.setString(5, loginEmpId);     // 登録者
            ps.setString(6, loginEmpId);     // 更新者
            ps.executeUpdate();
        }
    }



    /**
     * 休憩記録をデータベースから削除する。
     * @param breakId 削除する休憩記録のID
     */
    public void deleteBreak(int breakId) {
        String sql = "DELETE FROM break WHERE BREAK_ID = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, breakId);
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
 // ★修正版: 指定日付の休憩を一括削除（置換保存のため）
    public void deleteBreaksByEmpIdAndDate(String empId, LocalDate date) {
        String sql = "DELETE b FROM break b " +
                     "JOIN kintai k ON b.KINTAI_REC_ID = k.KINTAI_REC_ID " +
                     "WHERE k.EMP_ID = ? AND k.KINTAI_DATE = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, empId);
            ps.setDate(2, Date.valueOf(date));
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    
    // ★追記: 指定日の工数割当を一括削除（置換保存のため）
    public void deleteWorkAllocsByEmpIdAndDate(String empId, LocalDate date) {
        String sql = "DELETE FROM work_alloc WHERE EMP_ID = ? AND WORK_DATE = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, empId);
            ps.setDate(2, Date.valueOf(date));
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    // --- 工数割り当て（work_alloc）関連メソッド ---

    /**
     * 指定された従業員と日付の工数割り当てリストを取得します。
     * （旧findWorkDetailsByRecIdメソッドの代わり）
     * @param empId 従業員番号
     * @param workDate 作業日
     * @return 工数割り当てのリスト。見つからない場合は空のリスト。
     */
    public List<KinmuManageBean.WorkAlloc> findWorkAllocsByEmpNoAndDate(String empId, LocalDate workDate) {
        List<KinmuManageBean.WorkAlloc> workAllocList = new ArrayList<>();
        // SQLを修正: work_allocテーブルとprojectテーブルをJOIN
        String sql = "SELECT wa.ALLOCATION_ID, wa.EMP_ID, wa.PROJECT_ID, wa.WORK_DATE, wa.WORK_HOURS, " +
                "p.PROJECT_NAME " +
                "FROM work_alloc wa " +
                "LEFT JOIN project p ON wa.PROJECT_ID = p.PROJECT_ID " +
                "WHERE wa.EMP_ID = ? AND wa.WORK_DATE = ? AND (wa.IS_DELETED = FALSE OR wa.IS_DELETED IS NULL) " + // 修正
                "ORDER BY wa.ALLOCATION_ID"; // 割り当てIDでソート

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, empId);
            ps.setDate(2, Date.valueOf(workDate));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    KinmuManageBean.WorkAlloc alloc = new KinmuManageBean.WorkAlloc();
                    alloc.setAllocationId(rs.getInt("ALLOCATION_ID"));
                    alloc.setEmpId(rs.getString("EMP_ID"));
                    alloc.setProjectId(rs.getInt("PROJECT_ID"));
                    alloc.setWorkDate(rs.getDate("WORK_DATE").toLocalDate());
                    alloc.setWorkHours(rs.getDouble("WORK_HOURS")); // doubleで取得
                    alloc.setProjectName(rs.getString("PROJECT_NAME")); // プロジェクト名も設定
                    workAllocList.add(alloc);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return workAllocList;
    }

    /**
     * 指定日の工数割り当てを一括置換保存します。
     * 既存の (EMP_ID, WORK_DATE) の工数割り当ては削除してから新規追加します。
     *
     * @param empId 従業員ID
     * @param workDate 勤務日
     * @param workAllocs 保存する工数割り当てリスト
     * @param loginEmpId ログインユーザー（作成者・更新者）
     * @throws ClassNotFoundException 
     */
    public void replaceWorkAllocs(String empId, LocalDate workDate, List<KinmuManageBean.WorkAlloc> workAllocs, String loginEmpId) throws ClassNotFoundException {
        // まず既存データを削除
        deleteWorkAllocsByEmpIdAndDate(empId, workDate);

        String sql = "INSERT INTO work_alloc (EMP_ID, PROJECT_ID, WORK_DATE, WORK_HOURS, " +
                     "IS_DELETED, CREATED_AT, CREATED_BY, UPDATED_AT, UPDATED_BY) " +
                     "VALUES (?, ?, ?, ?, FALSE, NOW(), ?, NOW(), ?)";

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            for (KinmuManageBean.WorkAlloc alloc : workAllocs) {
                ps.setString(1, empId);
                ps.setInt(2, alloc.getProjectId());
                ps.setDate(3, Date.valueOf(workDate));
                ps.setDouble(4, alloc.getWorkHours());
                ps.setString(5, loginEmpId);
                ps.setString(6, loginEmpId);
                ps.addBatch();
            }
            ps.executeBatch();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * 指定された従業員の指定期間の工数割り当てをまとめて取得する
     * 勤怠表（月表示）用
     */
    public List<KinmuManageBean.WorkAlloc> findWorkAllocsByEmpAndRange(String empId, LocalDate start, LocalDate end) {
        List<KinmuManageBean.WorkAlloc> list = new ArrayList<>();
        String sql = "SELECT wa.ALLOCATION_ID, wa.EMP_ID, wa.PROJECT_ID, wa.WORK_DATE, wa.WORK_HOURS, " +
                     "p.PROJECT_NAME " +
                     "FROM work_alloc wa " +
                     "LEFT JOIN project p ON wa.PROJECT_ID = p.PROJECT_ID " +
                     "WHERE wa.EMP_ID = ? " +
                     "AND wa.WORK_DATE BETWEEN ? AND ? " +
                     "AND (wa.IS_DELETED = FALSE OR wa.IS_DELETED IS NULL) " +
                     "ORDER BY wa.WORK_DATE, wa.ALLOCATION_ID";

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, empId);
            ps.setDate(2, Date.valueOf(start));
            ps.setDate(3, Date.valueOf(end));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    KinmuManageBean.WorkAlloc alloc = new KinmuManageBean.WorkAlloc();
                    alloc.setAllocationId(rs.getInt("ALLOCATION_ID"));
                    alloc.setEmpId(rs.getString("EMP_ID"));
                    alloc.setProjectId(rs.getInt("PROJECT_ID"));
                    alloc.setWorkDate(rs.getDate("WORK_DATE").toLocalDate());
                    alloc.setWorkHours(rs.getDouble("WORK_HOURS"));
                    alloc.setProjectName(rs.getString("PROJECT_NAME"));
                    list.add(alloc);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }
    
    public List<KinmuManageBean.WorkAlloc> findProjectTotalsByEmpAndRange(String empId, LocalDate start, LocalDate end) {
        List<KinmuManageBean.WorkAlloc> list = new ArrayList<>();
        String sql = "SELECT wa.PROJECT_ID, p.PROJECT_NAME, SUM(wa.WORK_HOURS) AS TOTAL_HOURS " +
                     "FROM work_alloc wa " +
                     "LEFT JOIN project p ON wa.PROJECT_ID = p.PROJECT_ID " +
                     "WHERE wa.EMP_ID = ? " +
                     "AND wa.WORK_DATE BETWEEN ? AND ? " +
                     "AND (wa.IS_DELETED = FALSE OR wa.IS_DELETED IS NULL) " +
                     "GROUP BY wa.PROJECT_ID, p.PROJECT_NAME " +
                     "ORDER BY TOTAL_HOURS DESC";

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, empId);
            ps.setDate(2, Date.valueOf(start));
            ps.setDate(3, Date.valueOf(end));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    KinmuManageBean.WorkAlloc alloc = new KinmuManageBean.WorkAlloc();
                    alloc.setEmpId(empId);
                    alloc.setProjectId(rs.getInt("PROJECT_ID"));
                    alloc.setProjectName(rs.getString("PROJECT_NAME"));
                    alloc.setWorkHours(rs.getDouble("TOTAL_HOURS")); // 合計時間をセット
                    list.add(alloc);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }




    /**
     * 工数割り当てをデータベースから削除します。
     * （旧deleteWorkDetailメソッドの代わり）
     * @param allocationId 削除する工数割り当てのID
     */
    public void deleteWorkAlloc(int allocationId) {
        String sql = "DELETE FROM work_alloc WHERE ALLOCATION_ID = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, allocationId);
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 指定されたKINTAI_REC_IDの勤怠記録情報をデータベースから検索する補助メソッド。
     * @param recId 勤怠記録ID
     * @return WorkTimeBeanオブジェクト。見つからない場合はnull
     */
    private WorkTimeBean findWorkTimeByRecId(int recId) {
        WorkTimeBean workTime = null;
        String sql = "SELECT KINTAI_REC_ID, EMP_ID, KINTAI_DATE, CLOCK_IN, CLOCK_OUT, " +
                "WORKING_HOURS, OVERTIME_HOURS, NIGHT_HOURS, ATTENDANCE_TYPE " +
                "FROM kintai WHERE KINTAI_REC_ID = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, recId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    workTime = new WorkTimeBean();
                    workTime.setKintaiRecId(rs.getInt("KINTAI_REC_ID"));
                    workTime.setEmpId(rs.getString("EMP_ID"));
                    workTime.setKintaiDate(rs.getDate("KINTAI_DATE").toLocalDate());
                    workTime.setClockIn(rs.getTime("CLOCK_IN"));
                    workTime.setClockOut(rs.getTime("CLOCK_OUT"));
                    // 新しいkintaiテーブルのWORKING_HOURS, OVERTIME_HOURS, NIGHT_HOURSも取得
                    // 修正箇所: setWorkingHours(BigDecimal) が WorkTimeBean で定義されたため、ここでのエラーが解消される
                    workTime.setWorkingHours(rs.getBigDecimal("WORKING_HOURS"));
                    workTime.setOvertimeHours(rs.getBigDecimal("OVERTIME_HOURS"));
                    workTime.setNightHours(rs.getBigDecimal("NIGHT_HOURS"));
                    workTime.setAttendanceType(rs.getString("ATTENDANCE_TYPE"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return workTime;
    }
    
    /**
     * 指定されたEMP_IDの勤怠記録情報をデータベースから検索する補助メソッド。
     * @param empID 従業員ID
     * @return WorkTimeBeanオブジェクト。見つからない場合はnull
     */
    public List<WorkTimeBean> findWorkTimeLog(String empId) {
    	List<WorkTimeBean> workTimeList = new ArrayList<>();
    	String sql = "SELECT EMP_ID, KINTAI_DATE, CLOCK_IN, CLOCK_OUT, " +
                "CREATED_AT, UPDATED_AT, ATTENDANCE_TYPE " +
                "FROM kintai WHERE EMP_ID = ? ORDER BY KINTAI_DATE DESC";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, empId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    WorkTimeBean workTime = new WorkTimeBean();
                    workTime.setEmpId(rs.getString("EMP_ID"));
                    workTime.setKintaiDate(rs.getDate("KINTAI_DATE").toLocalDate());
                    workTime.setClockIn(rs.getTime("CLOCK_IN"));
                    workTime.setClockOut(rs.getTime("CLOCK_OUT"));
                    workTime.setCreatedAt(rs.getTimestamp("CREATED_AT"));
                    workTime.setUpdatedAt(rs.getTimestamp("UPDATED_AT"));
                    workTime.setAttendanceType(rs.getString("ATTENDANCE_TYPE"));
                    
                    workTimeList.add(workTime);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return workTimeList;
    }
    
    /**
     * 工数割り当てを1件追加する
     * @throws ClassNotFoundException 
     */
    public void addWorkAlloc(KinmuManageBean.WorkAlloc alloc) throws ClassNotFoundException {
        String sql = "INSERT INTO work_alloc " +
                     "(EMP_ID, PROJECT_ID, WORK_DATE, WORK_HOURS, IS_DELETED, CREATED_AT, CREATED_BY, UPDATED_AT, UPDATED_BY) " +
                     "VALUES (?, ?, ?, ?, FALSE, NOW(), ?, NOW(), ?)";

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, alloc.getEmpId());
            ps.setInt(2, alloc.getProjectId());
            ps.setDate(3, Date.valueOf(alloc.getWorkDate()));
            ps.setDouble(4, alloc.getWorkHours());
            ps.setString(5, alloc.getCreatedBy());
            ps.setString(6, alloc.getUpdatedBy());

            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}
