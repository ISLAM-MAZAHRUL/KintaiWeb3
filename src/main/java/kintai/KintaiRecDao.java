package kintai;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 勤怠記録表示（kintai_rec.jsp）に関連するデータアクセスを担当するクラス (DAO)。
 * kintai、break、emp、dept、postテーブルからデータを結合して取得する。
 */
public class KintaiRecDao {

    private DBAccess db = new DBAccess();

    /**
     * 指定された条件に基づいて勤怠記録のリストを取得します。
     * このメソッドは、一般社員、管理者、および将来の主任/リーダーの権限に対応します。
     * * @param targetEmpNos 検索対象の従業員番号リスト (一般社員の場合は自身のempno、管理者の場合は空リストまたは指定されたempno、主任の場合は部下のempnoリスト)
     * このリストが空の場合、empNoFilterがnullまたは空であれば全従業員を対象とし、そうでない場合は指定されたempNoFilterを適用する
     * @param deptNoFilter 部署番号によるフィルター (nullまたは空の場合はフィルターしない)
     * @param postNoFilter 役職番号によるフィルター (nullまたは空の場合はフィルターしない)
     * @param startDate 検索期間の開始日 (nullの場合はフィルターしない)
     * @param endDate 検索期間の終了日 (nullの場合はフィルターしない)
     * @param userRole ログイン中のユーザーの権限 (0:一般社員, 1:管理者, 2:主任/リーダーなど)
     * @return 勤怠記録のリスト（KintaiRecBeanオブジェクト）。見つからない場合は空のリスト。
     */
    public List<KintaiRecBean> getKintaiRecords(
            List<String> targetEmpNos, String deptNoFilter, String postNoFilter,
            LocalDate startDate, LocalDate endDate, int userRole) {

        List<KintaiRecBean> kintaiRecList = new ArrayList<>();
        StringBuilder sql = new StringBuilder();

        sql.append("SELECT ");
        sql.append("  k.KINTAI_REC_ID, ");
        sql.append("  k.KINTAI_DATE, ");
        sql.append("  k.ATTENDANCE_TYPE, ");
        sql.append("  k.EMP_ID, ");
        sql.append("  k.CLOCK_IN, ");
        sql.append("  k.CLOCK_OUT, ");
        sql.append("  k.WORKING_HOURS, ");
        sql.append("  k.OVERTIME_HOURS, ");
        sql.append("  e.EMP_NAME, ");
        sql.append("  e.DEPT_ID, ");
        sql.append("  d.DEPT_NAME, ");
        sql.append("  e.POST_ID, ");
        sql.append("  p.POST_NAME, ");
        sql.append("  w.PROJECT_ID, ");
        sql.append("  pr.PROJECT_CODE ");

        sql.append("FROM kintai k ");

        sql.append("LEFT JOIN emp e ");
        sql.append("ON k.EMP_ID = e.EMP_ID ");

        sql.append("LEFT JOIN dept d ");
        sql.append("ON e.DEPT_ID = d.DEPT_ID ");

        sql.append("LEFT JOIN post p ");
        sql.append("ON e.POST_ID = p.POST_ID ");

        sql.append("LEFT JOIN work_alloc w ");
        sql.append("ON k.EMP_ID = w.EMP_ID ");
        sql.append("AND k.KINTAI_DATE = w.WORK_DATE ");

        sql.append("LEFT JOIN project pr ");
        sql.append("ON w.PROJECT_ID = pr.PROJECT_ID ");

        sql.append("WHERE 1=1 ");

        List<Object> params = new ArrayList<>(); // プリペアドステートメントのパラメータリスト

        // --- 従業員番号によるフィルター（権限に基づく）---
        if (userRole == 0) { // 一般社員の場合、自身の勤怠のみ
            sql.append("AND k.EMP_ID = ? ");
            params.add(targetEmpNos.get(0)); // targetEmpNosには自身のempnoが1つだけ入っている
        } else { // 管理者または主任/リーダーの場合
            if (targetEmpNos != null && !targetEmpNos.isEmpty()) {
                // 特定の従業員リストが指定されている場合 (例: 主任の部下、または管理者による単一従業員検索)
                sql.append("AND k.EMP_ID IN (");
                for (int i = 0; i < targetEmpNos.size(); i++) {
                    sql.append("?");
                    if (i < targetEmpNos.size() - 1) {
                        sql.append(", ");
                    }
                }
                sql.append(") ");
                params.addAll(targetEmpNos);
            }
           
        }


        // --- その他のフィルター条件 ---
        if (deptNoFilter != null && !deptNoFilter.trim().isEmpty()) {
            sql.append("AND e.DEPT_ID = ? ");
            params.add(deptNoFilter);
        }
        if (postNoFilter != null && !postNoFilter.trim().isEmpty()) {
            sql.append("AND e.POST_ID = ? ");
            params.add(postNoFilter);
        }
        if (startDate != null) {
            sql.append("AND k.KINTAI_DATE >= ? ");
            params.add(Date.valueOf(startDate));
        }
        if (endDate != null) {
            sql.append("AND k.KINTAI_DATE <= ? ");
            params.add(Date.valueOf(endDate));
        }

        sql.append("ORDER BY k.KINTAI_DATE ASC, k.EMP_ID ASC"); // 日付の新しい順、従業員番号の昇順でソート

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            // パラメータをセット
            for (int i = 0; i < params.size(); i++) {
                Object param = params.get(i);
                if (param instanceof String) {
                    ps.setString(i + 1, (String) param);
                } else if (param instanceof Date) {
                    ps.setDate(i + 1, (Date) param);
                }
                // 他のデータ型が必要な場合はここに追加
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    KintaiRecBean bean = new KintaiRecBean();
                    bean.setKintaiRecId(rs.getInt("KINTAI_REC_ID"));
                    bean.setKintaiDate(rs.getDate("KINTAI_DATE").toLocalDate());
                    bean.setEmpno(rs.getString("EMP_ID"));
                    bean.setClockIn(rs.getTime("CLOCK_IN"));
                    bean.setClockOut(rs.getTime("CLOCK_OUT"));
                    bean.setEmpName(rs.getString("EMP_NAME"));
                    bean.setProjectId(rs.getString("PROJECT_ID"));
                    bean.setProjectCode(rs.getString("PROJECT_CODE"));
                    bean.setDeptNo(rs.getString("DEPT_ID"));
                    bean.setDeptName(rs.getString("DEPT_NAME"));
                    bean.setPostNo(rs.getString("POST_ID"));
                    bean.setPostName(rs.getString("POST_NAME"));
                    // ★ 勤怠区分を補完
                    String type = rs.getString("ATTENDANCE_TYPE");
                    if (type == null || type.isEmpty()) {
                        if (rs.getTime("CLOCK_IN") != null || rs.getTime("CLOCK_OUT") != null) {
                            type = "出勤";
                        }
                    }
                    bean.setAttendanceType(type);


                    // 各勤怠記録の休憩時間を取得し、合計休憩時間を計算
                    long totalBreakMinutes = calculateTotalBreakMinutes(bean.getKintaiRecId());
                    bean.setTotalBreakMinutes(totalBreakMinutes);

                 // 実働時間を計算
                    long actualWorkMinutes = calculateActualWorkMinutes(bean.getClockIn(), bean.getClockOut(), totalBreakMinutes);

                    // ★ 有給なら 8時間固定
                    if ("有給".equals(bean.getAttendanceType())) {
                        actualWorkMinutes = 8 * 60; // 480分
                    } else if ("無給".equals(bean.getAttendanceType()) || "欠勤".equals(bean.getAttendanceType())) {
                        actualWorkMinutes = 0;
                    }


                    bean.setActualWorkMinutes(actualWorkMinutes);


                    // 残業時間を計算（実働時間が8時間を超えた分）
                    long overtimeMinutes = Math.max(0, actualWorkMinutes - (8 * 60)); // 8時間 = 480分
                    bean.setOvertimeMinutes(overtimeMinutes);

                    kintaiRecList.add(bean);
                    
                   //深夜残業時間を計算（22時から5時まで働いた分）
                   long nightovertimeMinutes = calculateNightOvertimeMinutes(
                  	     bean.getClockIn(),
                  	     bean.getClockOut(),
                  	     bean.getTotalBreakMinutes()
                   );
                   bean.setNightovertimeMinutes(nightovertimeMinutes);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            // エラーハンドリング：必要に応じてログ出力や例外スロー
        }
        return kintaiRecList;
    }

    /**
     * 指定されたRECIDの勤怠記録に関連する全ての休憩時間の合計を計算します。
     * @param recId 勤怠記録ID
     * @return 合計休憩時間（分単位）
     */
    private long calculateTotalBreakMinutes(int recId) {
        long totalMinutes = 0;
        String sql = "SELECT BREAK_START, BREAK_END FROM break WHERE KINTAI_REC_ID = ?";

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, recId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Time breakStart = rs.getTime("BREAK_START");
                    Time breakEnd = rs.getTime("BREAK_END");

                    if (breakStart != null && breakEnd != null) {
                        LocalTime start = breakStart.toLocalTime();
                        LocalTime end = breakEnd.toLocalTime();
                        // 休憩終了が休憩開始より前の場合は、日付を跨いだと見なして24時間を加算
                        if (end.isBefore(start)) {
                            Duration duration = Duration.between(start, LocalTime.MAX).plus(Duration.between(LocalTime.MIDNIGHT, end));
                            totalMinutes += duration.toMinutes();
                        } else {
                            totalMinutes += Duration.between(start, end).toMinutes();
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            // エラーハンドリング
        }
        return totalMinutes;
    }

    /**
     * 出勤時刻、退勤時刻、合計休憩時間から実働時間を計算します。
     * @param clockIn 出勤時刻
     * @param clockOut 退勤時刻
     * @param totalBreakMinutes 合計休憩時間（分単位）
     * @return 実働時間（分単位）
     */
    private long calculateActualWorkMinutes(Time clockIn, Time clockOut, long totalBreakMinutes) {
        if (clockIn == null || clockOut == null) {
            return 0; // 出勤または退勤がない場合は実働時間0
        }

        LocalTime start = clockIn.toLocalTime();
        LocalTime end = clockOut.toLocalTime();

        long workDurationMinutes = 0;
        // 退勤時刻が翌日になる場合（例: 22:00出勤 -> 02:00退勤）を考慮
        if (end.isBefore(start)) {
            Duration duration = Duration.between(start, LocalTime.MAX).plus(Duration.between(LocalTime.MIDNIGHT, end));
            workDurationMinutes = duration.toMinutes();
        } else {
            workDurationMinutes = Duration.between(start, end).toMinutes();
        }
        
        // 実働時間 = (退勤時刻 - 出勤時刻) - 総休憩時間
        long actualMinutes = workDurationMinutes - totalBreakMinutes;
        return Math.max(0, actualMinutes); // マイナスにならないように0以上を保証
    }
    
	  /**
	  * 深夜残業時間を計算（22:00〜翌5:00）
	  */
	 private long calculateNightOvertimeMinutes(Time clockIn, Time clockOut, long totalBreakMinutes) {
	 	if (clockIn == null || clockOut == null) {
	         return 0; // 出勤または退勤がない場合は実働時間0
	     }
	 	
	 	
	 	LocalTime start = clockIn.toLocalTime();
	    LocalTime end = clockOut.toLocalTime();
	     
	 	// 勤務時間の範囲
	     LocalDate today = LocalDate.now();
	     LocalDateTime workStart = LocalDateTime.of(today, start);
	     LocalDateTime workEnd = LocalDateTime.of(today, end);
	
	     // 退勤が翌日の場合に対応
	     if (workEnd.isBefore(workStart)) {
	         workEnd = workEnd.plusDays(1);
	     }
	
	     // 深夜時間帯（22:00〜翌5:00）
	     LocalDateTime nightStart = LocalDateTime.of(today, LocalTime.of(22, 0));
	     LocalDateTime nightEnd = LocalDateTime.of(today.plusDays(1), LocalTime.of(5, 0));
	
	     // 勤務時間と深夜時間帯の重なりを計算
	     LocalDateTime overlapStart = workStart.isAfter(nightStart) ? workStart : nightStart;
	     LocalDateTime overlapEnd = workEnd.isBefore(nightEnd) ? workEnd : nightEnd;
	
	     long minutes = 0;
	     if (overlapEnd.isAfter(overlapStart)) {
	         minutes = Duration.between(overlapStart, overlapEnd).toMinutes();
	
	      // 休憩時間を全勤務時間に比例配分して控除
	         long totalWorkMinutes = Duration.between(workStart, workEnd).toMinutes();
	         if (totalWorkMinutes > 0 && totalBreakMinutes > 0) {
	             double ratio = (double) minutes / totalWorkMinutes;
	             long deducted = Math.round(totalBreakMinutes * ratio);
	             minutes = Math.max(0, minutes - deducted);
	         }
	     }
	
	     return minutes;
	 }

    /**
     * 指定された従業員・月の月度勤怠統計を取得します
     * @param empno 従業員番号
     * @param targetMonth 対象月 (YYYY-MM)
     * @return 月度統計データ
     */
    public MonthlySummaryBean getMonthlySummary(String empno, String targetMonth) {
    	
        MonthlySummaryBean summary = new MonthlySummaryBean();
        summary.setTargetMonth(targetMonth);
        

        try {
            // 対象月の開始日と終了日を計算
            YearMonth ym = YearMonth.parse(targetMonth);
            LocalDate monthStart = ym.atDay(1);
            LocalDate monthEnd = ym.atEndOfMonth();

            // 1. 総出社日数を計算
            int totalWorkDays = calculateTotalWorkDays(monthStart, monthEnd);
            summary.setTotalWorkDays(totalWorkDays);

            // 2. 実際の出勤日数を取得
            int actualAttendanceDays = getActualAttendanceDays(empno, monthStart, monthEnd);
            summary.setActualAttendanceDays(actualAttendanceDays);

            // 3. 月度の勤怠統計を計算
            calculateMonthlyWorkingHours(empno, monthStart, monthEnd, summary);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return summary;
    }
    public MonthlySummaryBean getPeriodSummary(
            String empno,
            LocalDate startDate,
            LocalDate endDate) {

        MonthlySummaryBean summary = new MonthlySummaryBean();

        try {
            summary.setTargetMonth(
                    startDate.toString() + " ～ " + endDate.toString());

            calculateMonthlyWorkingHours(
                    empno,
                    startDate,
                    endDate,
                    summary);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return summary;
    }
    /**
     * 指定期間の総出社日数を計算（週末とカレンダーの休日を除く）
     */
    private int calculateTotalWorkDays(LocalDate monthStart, LocalDate monthEnd) {
        int workDays = 0;
        LocalDate current = monthStart;

        while (!current.isAfter(monthEnd)) {
            // 週末を除外
            if (current.getDayOfWeek() != DayOfWeek.SATURDAY && 
                current.getDayOfWeek() != DayOfWeek.SUNDAY) {
                // カレンダーテーブルの休日をチェック
                if (!isHolidayInCalendar(current)) {
                    workDays++;
                }
            }
            current = current.plusDays(1);
        }

        return workDays;
    }

    /**
     * カレンダーテーブルで指定日が休日かどうかをチェック
     * カレンダーテーブルが存在しない場合は、基本的な休日判定のみ行う
     */
    private boolean isHolidayInCalendar(LocalDate date) {
        // まずカレンダーテーブルの存在をチェック
        try (Connection conn = db.getConnection()) {
            String checkTableSql = "SHOW TABLES LIKE 'calendar'";
            PreparedStatement checkStmt = conn.prepareStatement(checkTableSql);
            ResultSet tableRs = checkStmt.executeQuery();
            
            if (tableRs.next()) {
                // カレンダーテーブルが存在する場合の処理
                String sql = "SELECT COUNT(*) FROM calendar WHERE EVENT_DATE = ? AND IS_HOLIDAY = 1";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setDate(1, Date.valueOf(date));
                    ResultSet rs = stmt.executeQuery();
                    
                    if (rs.next()) {
                        return rs.getInt(1) > 0;
                    }
                }
            } else {
                // カレンダーテーブルが存在しない場合は、基本的な祝日をハードコーディングで判定
                return isBasicHoliday(date);
            }
        } catch (Exception e) {
            e.printStackTrace();
            // エラー時は基本的な祝日判定にフォールバック
            return isBasicHoliday(date);
        }
        
        return false;
    }
    
    /**
     * 基本的な日本の祝日判定（簡易版）
     */
    private boolean isBasicHoliday(LocalDate date) {
        int month = date.getMonthValue();
        int day = date.getDayOfMonth();
        
        // 基本的な固定祝日のみをチェック
        switch (month) {
            case 1: // 元日、成人の日
                return day == 1 || (day >= 8 && day <= 14 && date.getDayOfWeek() == DayOfWeek.MONDAY);
            case 2: // 建国記念の日、天皇誕生日
                return day == 11 || day == 23;
            case 3: // 春分の日（概算：20日または21日）
                return day == 20 || day == 21;
            case 4: // 昭和の日
                return day == 29;
            case 5: // 憲法記念日、みどりの日、こどもの日
                return day == 3 || day == 4 || day == 5;
            case 7: // 海の日（7月第3月曜日）
                return day >= 15 && day <= 21 && date.getDayOfWeek() == DayOfWeek.MONDAY;
            case 8: // 山の日
                return day == 11;
            case 9: // 敬老の日、秋分の日
                return (day >= 15 && day <= 21 && date.getDayOfWeek() == DayOfWeek.MONDAY) || day == 22 || day == 23;
            case 10: // スポーツの日（10月第2月曜日）
                return day >= 8 && day <= 14 && date.getDayOfWeek() == DayOfWeek.MONDAY;
            case 11: // 文化の日、勤労感謝の日
                return day == 3 || day == 23;
            case 12: // なし
                return false;
            default:
                return false;
        }
    }

    /**
     * 指定期間の実際の出勤日数を取得
     */
    private int getActualAttendanceDays(String empno, LocalDate monthStart, LocalDate monthEnd) {
        String sql = "SELECT COUNT(*) FROM kintai WHERE EMP_ID = ? AND KINTAI_DATE >= ? AND KINTAI_DATE <= ? AND CLOCK_IN IS NOT NULL";
        
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, empno);
            stmt.setDate(2, Date.valueOf(monthStart));
            stmt.setDate(3, Date.valueOf(monthEnd));
            
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return 0;
    }

    /**
     * 月度の労働時間統計を計算
     */
    private void calculateMonthlyWorkingHours(String empno, LocalDate monthStart, LocalDate monthEnd, MonthlySummaryBean summary) {
        String sql = "SELECT ATTENDANCE_TYPE, KINTAI_REC_ID, CLOCK_IN, CLOCK_OUT, WORKING_HOURS, OVERTIME_HOURS " +
                     "FROM kintai WHERE EMP_ID = ? AND KINTAI_DATE >= ? AND KINTAI_DATE <= ?";

        BigDecimal totalWorkingHours = BigDecimal.ZERO;
        BigDecimal totalOvertimeHours = BigDecimal.ZERO;
        BigDecimal totalBreakHours = BigDecimal.ZERO;
        BigDecimal totalNightHours = BigDecimal.ZERO;

        int paidLeaveDays = 0;
        int absentDays = 0;
        int holidayWorkDays = 0;
        int compensatoryLeaveDays = 0;

        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, empno);
            stmt.setDate(2, Date.valueOf(monthStart));
            stmt.setDate(3, Date.valueOf(monthEnd));

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String type = rs.getString("ATTENDANCE_TYPE");
                int recId = rs.getInt("KINTAI_REC_ID");
                Time in = rs.getTime("CLOCK_IN");
                Time out = rs.getTime("CLOCK_OUT");

                // --- 勤怠区分ごとのカウント ---
                if ("有給".equals(type)) {
                    paidLeaveDays++;
                } else if ("欠勤".equals(type)) {
                    absentDays++;
                } else if ("休日出勤".equals(type)) {
                    holidayWorkDays++;
                } else if ("代休".equals(type)) {
                    compensatoryLeaveDays++;
                }

                // --- 勤務時間 ---
                BigDecimal workingHours = rs.getBigDecimal("WORKING_HOURS");
                if (workingHours != null) {
                    totalWorkingHours = totalWorkingHours.add(workingHours);
                }

                // --- 残業時間 ---
                BigDecimal overtimeHours = rs.getBigDecimal("OVERTIME_HOURS");
                if (overtimeHours != null) {
                    totalOvertimeHours = totalOvertimeHours.add(overtimeHours);
                }

                // --- 休憩時間 ---
                long breakMinutes = calculateTotalBreakMinutes(recId);
                BigDecimal breakHours = BigDecimal.valueOf(breakMinutes)
                        .divide(BigDecimal.valueOf(60), 2, BigDecimal.ROUND_HALF_UP);
                totalBreakHours = totalBreakHours.add(breakHours);

                // --- 深夜残業時間 (22:00〜翌5:00) ---
                if (in != null && out != null) {
                    long nightMinutes = calculateNightOvertimeMinutes(in, out, breakMinutes);
                    BigDecimal nightHours = BigDecimal.valueOf(nightMinutes)
                            .divide(BigDecimal.valueOf(60), 2, BigDecimal.ROUND_HALF_UP);
                    totalNightHours = totalNightHours.add(nightHours);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        // --- サマリーに格納 ---
        summary.setPaidLeaveDays(paidLeaveDays);
        summary.setAbsentDays(absentDays);
        summary.setHolidayWorkDays(holidayWorkDays);
        summary.setCompensatoryLeaveDays(compensatoryLeaveDays);

        summary.setTotalWorkingHours(totalWorkingHours);
        summary.setTotalOvertimeHours(totalOvertimeHours);
        summary.setTotalBreakHours(totalBreakHours);
        summary.setTotalNightHours(totalNightHours);
    }

    
    /**
     * 指定日の出勤予定者数を取得（全従業員数を返す）
     * @param date 対象日
     * @return 出勤予定者数
     */
    public int getScheduledEmployeeCount(LocalDate date) {
        String sql = "SELECT COUNT(*) FROM emp WHERE IS_ACTIVE = true";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }
    
    /**
     * 指定日の出勤予定者数を取得（部門指定）
     * @param deptId 対象部門ID
     * @param date 対象日（※未使用だが将来拡張のため保持）
     * @return 指定部門の出勤予定者数
     */
    public int getScheduledEmployeeCountByDept(String deptId, LocalDate date) {
        String sql = "SELECT COUNT(*) FROM emp WHERE IS_ACTIVE = true AND DEPT_ID = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, deptId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }
    
    /**
     * 指定日の出勤中者数を取得（CLOCKINがあってCLOCKOUTがないレコード）
     * @param date 対象日
     * @return 出勤中者数
     */
    public int getWorkingEmployeeCount(LocalDate date) {
        String sql = "SELECT COUNT(*) FROM kintai WHERE KINTAI_DATE = ? AND CLOCK_IN IS NOT NULL AND (IS_DELETED = false OR IS_DELETED IS NULL)";
        System.out.println("getWorkingEmployeeCount SQL: " + sql);
        System.out.println("getWorkingEmployeeCount date: " + date);
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(date));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int count = rs.getInt(1);
                    System.out.println("getWorkingEmployeeCount result: " + count);
                    return count;
                }
            }
        } catch (Exception e) {
            System.out.println("getWorkingEmployeeCount error: " + e.getMessage());
            e.printStackTrace();
        }
        return 0;
    }
    
    /**
     * 指定日の出勤中者数を取得（部門指定）
     * CLOCK_IN があり、CLOCK_OUT がないレコードをカウント
     * @param deptId 対象部門ID
     * @param date 対象日
     * @return 出勤中者数（指定部門のみ）
     */
    public int getWorkingEmployeeCountByDept(String deptId, LocalDate date) {
        String sql = "SELECT COUNT(*) FROM kintai k " +
                     "JOIN emp e ON k.EMP_ID = e.EMP_ID " +
                     "WHERE k.KINTAI_DATE = ? " +
                     "AND k.CLOCK_IN IS NOT NULL " +
                     "AND (k.IS_DELETED = false OR k.IS_DELETED IS NULL) " +
                     "AND e.DEPT_ID = ?";
        
        System.out.println("getWorkingEmployeeCountByDept SQL: " + sql);
        System.out.println("date: " + date + ", deptId: " + deptId);

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setDate(1, Date.valueOf(date));
            ps.setString(2, deptId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int count = rs.getInt(1);
                    System.out.println("getWorkingEmployeeCountByDept result: " + count);
                    return count;
                }
            }

        } catch (Exception e) {
            System.out.println("getWorkingEmployeeCountByDept error: " + e.getMessage());
            e.printStackTrace();
        }

        return 0;
    }

    
    /**
     * 指定日の未出勤者数を取得（その日の勤怠記録がない従業員）
     * @param date 対象日
     * @return 未出勤者数
     */
    public int getAbsentEmployeeCount(LocalDate date) {
        // 土日祖日の場合は0を返す
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        System.out.println("getAbsentEmployeeCount date: " + date + ", dayOfWeek: " + dayOfWeek);
        if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
            System.out.println("getAbsentEmployeeCount: Weekend, returning 0");
            return 0;
        }
        
        String sql = "SELECT COUNT(*) FROM emp e WHERE e.IS_ACTIVE = true AND NOT EXISTS " +
                    "(SELECT 1 FROM kintai k WHERE k.EMP_ID = e.EMP_ID AND k.KINTAI_DATE = ? AND (k.IS_DELETED = false OR k.IS_DELETED IS NULL))";
        System.out.println("getAbsentEmployeeCount SQL: " + sql);
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(date));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int count = rs.getInt(1);
                    System.out.println("getAbsentEmployeeCount result: " + count);
                    return count;
                }
            }
        } catch (Exception e) {
            System.out.println("getAbsentEmployeeCount error: " + e.getMessage());
            e.printStackTrace();
        }
        return 0;
    }
    
    /**
     * 指定日の未出勤者数を取得（部門指定）
     * 該当日に勤怠記録が存在しないアクティブな従業員をカウント（部門限定）
     * @param deptId 対象部門ID
     * @param date 対象日
     * @return 未出勤者数（指定部門のみ）
     */
    public int getAbsentEmployeeCountByDept(String deptId, LocalDate date) {
        // 土日祝日の場合は 0 を返す（※祝日判定は含まず）
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        System.out.println("getAbsentEmployeeCountByDept date: " + date + ", dayOfWeek: " + dayOfWeek);
        if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
            System.out.println("getAbsentEmployeeCountByDept: Weekend, returning 0");
            return 0;
        }

        String sql = "SELECT COUNT(*) FROM emp e " +
                     "WHERE e.IS_ACTIVE = true " +
                     "AND e.DEPT_ID = ? " +
                     "AND NOT EXISTS (" +
                     "    SELECT 1 FROM kintai k " +
                     "    WHERE k.EMP_ID = e.EMP_ID " +
                     "    AND k.KINTAI_DATE = ? " +
                     "    AND (k.IS_DELETED = false OR k.IS_DELETED IS NULL))";

        System.out.println("getAbsentEmployeeCountByDept SQL: " + sql);

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, deptId);
            ps.setDate(2, Date.valueOf(date));

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int count = rs.getInt(1);
                    System.out.println("getAbsentEmployeeCountByDept result: " + count);
                    return count;
                }
            }

        } catch (Exception e) {
            System.out.println("getAbsentEmployeeCountByDept error: " + e.getMessage());
            e.printStackTrace();
        }

        return 0;
    }

    
    /**
     * 指定日の休暇申請者数を取得
     * 実装注：現在の段階では休暇管理テーブルがないため、0を返す
     * @param date 対象日
     * @return 休暇申請者数
     */
    public int getVacationEmployeeCount(LocalDate date) {
        String sql = "SELECT COUNT(DISTINCT lr.EMP_ID) FROM leave_rec lr " +
                    "WHERE lr.START_DATE <= ? AND lr.END_DATE >= ? " +
                    "AND lr.STATUS = '承認済み' AND lr.IS_DELETED = 0";
        
        System.out.println("getVacationEmployeeCount SQL: " + sql);
        System.out.println("getVacationEmployeeCount date: " + date);
        
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setDate(1, Date.valueOf(date));
            ps.setDate(2, Date.valueOf(date));
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int count = rs.getInt(1);
                    System.out.println("getVacationEmployeeCount result: " + count);
                    return count;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }
    
    /**
     * 指定日の休暇申請者数を取得（部門指定）
     * @param deptId 対象部門ID
     * @param date 対象日
     * @return 指定部門の休暇申請者数
     */
    public int getVacationEmployeeCountByDept(String deptId, LocalDate date) {
        String sql = "SELECT COUNT(DISTINCT lr.EMP_ID) FROM leave_rec lr " +
                     "JOIN emp e ON lr.EMP_ID = e.EMP_ID " +
                     "WHERE lr.START_DATE <= ? AND lr.END_DATE >= ? " +
                     "AND lr.STATUS = '承認済み' AND lr.IS_DELETED = 0 " +
                     "AND e.DEPT_ID = ? AND e.IS_ACTIVE = true";

        System.out.println("getVacationEmployeeCountByDept SQL: " + sql);
        System.out.println("getVacationEmployeeCountByDept date: " + date + ", deptId: " + deptId);

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setDate(1, Date.valueOf(date));
            ps.setDate(2, Date.valueOf(date));
            ps.setString(3, deptId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int count = rs.getInt(1);
                    System.out.println("getVacationEmployeeCountByDept result: " + count);
                    return count;
                }
            }

        } catch (Exception e) {
            System.out.println("getVacationEmployeeCountByDept error: " + e.getMessage());
            e.printStackTrace();
        }

        return 0;
    }
    
    public Map<LocalDate, LeaveRecBean> getLeaveMap(String empId, YearMonth ym) throws SQLException {
        Map<LocalDate, LeaveRecBean> leaveMap = new HashMap<>();

        String sql = "SELECT lr.LEAVE_ID, lr.EMP_ID, lr.LEAVE_TYPE_ID, lr.START_DATE, lr.END_DATE, " +
                     "lt.LEAVE_TYPE_NAME, lt.IS_PAID " +
                     "FROM leave_rec lr " +
                     "JOIN leave_type lt ON lr.LEAVE_TYPE_ID = lt.LEAVE_TYPE_ID " +
                     "WHERE lr.EMP_ID = ? " +
                     "AND (lr.START_DATE <= ? AND lr.END_DATE >= ?)";

        try (Connection conn = db.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, empId);
            ps.setDate(2, java.sql.Date.valueOf(ym.atEndOfMonth()));
            ps.setDate(3, java.sql.Date.valueOf(ym.atDay(1)));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    LeaveTypeBean type = new LeaveTypeBean();
                    type.setLeaveTypeId(rs.getInt("LEAVE_TYPE_ID"));
                    type.setLeaveTypeName(rs.getString("LEAVE_TYPE_NAME"));
                    type.setPaid(rs.getBoolean("IS_PAID")); // booleanでOK

                    LeaveRecBean rec = new LeaveRecBean();
                    rec.setLeaveId(rs.getInt("LEAVE_ID"));
                    rec.setEmpId(rs.getString("EMP_ID"));
                    rec.setLeaveTypeId(rs.getInt("LEAVE_TYPE_ID"));
                    rec.setStartDate(rs.getDate("START_DATE"));
                    rec.setEndDate(rs.getDate("END_DATE"));
                    rec.setLeaveType(type);  // ★紐付け

                    // 日ごとにMapに登録
                    LocalDate start = rec.getStartDate().toLocalDate();
                    LocalDate end = rec.getEndDate().toLocalDate();
                    for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
                        leaveMap.put(d, rec);
                    }
                }
            }
        } catch (ClassNotFoundException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
        return leaveMap;
    }
    
    // 日付＋従業員IDで勤怠データ取得メソッドを追加
    public KintaiRecBean getRecord(String empId, LocalDate date) {
        KintaiRecBean bean = null;
        String sql = "SELECT * FROM kintai WHERE EMP_ID = ? AND KINTAI_DATE = ? AND (IS_DELETED = false OR IS_DELETED IS NULL)";

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, empId);
            ps.setDate(2, java.sql.Date.valueOf(date));

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    bean = new KintaiRecBean();
                    bean.setKintaiRecId(rs.getInt("KINTAI_REC_ID"));
                    bean.setEmpno(rs.getString("EMP_ID"));
                    bean.setKintaiDate(rs.getDate("KINTAI_DATE").toLocalDate());
                    bean.setClockIn(rs.getTime("CLOCK_IN"));
                    bean.setClockOut(rs.getTime("CLOCK_OUT"));

                    // 勤怠区分補完
                    String type = rs.getString("ATTENDANCE_TYPE");
                    if (type == null || type.isEmpty()) {
                        if (rs.getTime("CLOCK_IN") != null || rs.getTime("CLOCK_OUT") != null) {
                            type = "出勤";
                        }
                    }
                    bean.setAttendanceType(type);

                    // --- 休憩リストを取得 ---
                    List<BreakBean> breakList = new ArrayList<>();
                    String breakSql = "SELECT BREAK_START, BREAK_END FROM break WHERE KINTAI_REC_ID = ?";
                    try (PreparedStatement bps = conn.prepareStatement(breakSql)) {
                        bps.setInt(1, bean.getKintaiRecId());
                        try (ResultSet brs = bps.executeQuery()) {
                            while (brs.next()) {
                                BreakBean bb = new BreakBean();
                                bb.setBreakStart(brs.getTime("BREAK_START"));
                                bb.setBreakEnd(brs.getTime("BREAK_END"));
                                breakList.add(bb);
                            }
                        }
                    }
                    bean.setBreaks(breakList);

                    // --- 合計休憩時間 ---
                    long totalBreakMinutes = calculateTotalBreakMinutes(bean.getKintaiRecId());
                    bean.setTotalBreakMinutes(totalBreakMinutes);

                    // --- 実働時間を計算 ---
                    long actualMinutes = calculateActualWorkMinutes(bean.getClockIn(), bean.getClockOut(), totalBreakMinutes);

                    // 有給/無給対応
                    if ("有給".equals(type)) {
                        actualMinutes = 8 * 60;
                    } else if ("無給".equals(type) || "欠勤".equals(type)) {
                        actualMinutes = 0;
                    }

                    bean.setActualWorkMinutes(actualMinutes);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return bean;
    }
    public List<MonthlySummaryBean> getAllMonthlySummaries(
            List<String> empIds, String startMonth, String endMonth) {

        List<MonthlySummaryBean> resultList = new ArrayList<>();
        if (empIds == null || empIds.isEmpty()) return resultList;

        java.time.YearMonth startYm = java.time.YearMonth.parse(startMonth);
        java.time.YearMonth endYm = java.time.YearMonth.parse(endMonth);
        java.time.LocalDate startDate = startYm.atDay(1);
        java.time.LocalDate endDate = endYm.atEndOfMonth();

        String inClause = empIds.stream()
            .map(id -> "?")
            .collect(java.util.stream.Collectors.joining(","));

        String sql = "SELECT " +
            "k.EMP_ID, " +
            "COUNT(CASE WHEN k.CLOCK_IN IS NOT NULL THEN 1 END) AS ACTUAL_DAYS, " +
            "SUM(CASE WHEN k.WORKING_HOURS IS NOT NULL THEN k.WORKING_HOURS ELSE 0 END) AS TOTAL_WORK_HOURS, " +
            "SUM(CASE WHEN k.OVERTIME_HOURS IS NOT NULL THEN k.OVERTIME_HOURS ELSE 0 END) AS TOTAL_OVERTIME, " +
            "COUNT(CASE WHEN k.ATTENDANCE_TYPE = '有給' THEN 1 END) AS PAID_LEAVE, " +
            "COUNT(CASE WHEN k.ATTENDANCE_TYPE = '欠勤' THEN 1 END) AS ABSENT, " +
            "COUNT(CASE WHEN k.ATTENDANCE_TYPE = '休日出勤' THEN 1 END) AS HOLIDAY_WORK " +
            "FROM kintai k " +
            "WHERE k.EMP_ID IN (" + inClause + ") " +
            "AND k.KINTAI_DATE >= ? " +
            "AND k.KINTAI_DATE <= ? " +
            "GROUP BY k.EMP_ID";

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            int idx = 1;
            for (String empId : empIds) {
                ps.setString(idx++, empId);
            }
            ps.setDate(idx++, java.sql.Date.valueOf(startDate));
            ps.setDate(idx, java.sql.Date.valueOf(endDate));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    MonthlySummaryBean bean = new MonthlySummaryBean();
                    bean.setEmpId(rs.getString("EMP_ID"));
                    bean.setActualAttendanceDays(rs.getInt("ACTUAL_DAYS"));
                    bean.setTotalWorkingHours(rs.getBigDecimal("TOTAL_WORK_HOURS"));
                    bean.setTotalOvertimeHours(rs.getBigDecimal("TOTAL_OVERTIME"));
                    bean.setPaidLeaveDays(rs.getInt("PAID_LEAVE"));
                    bean.setAbsentDays(rs.getInt("ABSENT"));
                    bean.setHolidayWorkDays(rs.getInt("HOLIDAY_WORK"));
                    resultList.add(bean);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return resultList;
    }


    /**
     * 勤怠データ保存（休憩含む）
     * @param rec 保存対象の勤怠Bean
     * @param loginEmpId ログインユーザーの従業員ID（更新者・作成者に使う）
     */
    public void saveOrUpdate(KintaiRecBean rec, String loginEmpId) {
        String selectSql = "SELECT COUNT(*) FROM kintai WHERE KINTAI_REC_ID = ?";
        String insertSql = "INSERT INTO kintai (EMP_ID, KINTAI_DATE, CLOCK_IN, CLOCK_OUT, ATTENDANCE_TYPE) VALUES (?, ?, ?, ?, ?)";
        String updateSql = "UPDATE kintai SET CLOCK_IN = ?, CLOCK_OUT = ?, ATTENDANCE_TYPE = ? WHERE KINTAI_REC_ID = ?";

        try (Connection conn = db.getConnection()) {
            conn.setAutoCommit(false); // トランザクション開始

            // --- 勤怠区分の補完 ---
            if (rec.getAttendanceType() == null || rec.getAttendanceType().isEmpty()) {
                if (rec.getClockIn() != null || rec.getClockOut() != null) {
                    rec.setAttendanceType("出勤");
                }
            }

            boolean exists = false;
            if (rec.getKintaiRecId() != 0) {
                try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                    ps.setInt(1, rec.getKintaiRecId());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next() && rs.getInt(1) > 0) {
                            exists = true;
                        }
                    }
                }
            }

            // --- kintai本体の登録・更新 ---
            if (exists) {
                try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                    ps.setTime(1, rec.getClockIn());
                    ps.setTime(2, rec.getClockOut());
                    ps.setString(3, rec.getAttendanceType());
                    ps.setInt(4, rec.getKintaiRecId());
                    ps.executeUpdate();
                }
            } else {
                try (PreparedStatement ps = conn.prepareStatement(insertSql, PreparedStatement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, rec.getEmpno());
                    ps.setDate(2, java.sql.Date.valueOf(rec.getKintaiDate()));
                    ps.setTime(3, rec.getClockIn());
                    ps.setTime(4, rec.getClockOut());
                    ps.setString(5, rec.getAttendanceType());
                    ps.executeUpdate();

                    // 自動採番された KINTAI_REC_ID を取得
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (rs.next()) {
                            rec.setKintaiRecId(rs.getInt(1));
                        }
                    }
                }
            }

            // --- 休憩の更新処理 ---
            if (rec.getBreaks() != null) {
                // 既存の休憩を論理削除
                try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE break SET IS_DELETED = TRUE, DELETED_AT = NOW(), DELETED_BY = ? " +
                    "WHERE KINTAI_REC_ID = ? AND IS_DELETED = FALSE")) {
                    ps.setString(1, loginEmpId);
                    ps.setInt(2, rec.getKintaiRecId());
                    ps.executeUpdate();
                }

                // 新しい休憩を挿入
                String breakInsert = "INSERT INTO break (KINTAI_REC_ID, EMP_ID, BREAK_START, BREAK_END, IS_DELETED, CREATED_BY, UPDATED_BY) " +
                                     "VALUES (?, ?, ?, ?, FALSE, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(breakInsert)) {
                    for (BreakBean b : rec.getBreaks()) {
                        if (b.getBreakStart() != null && b.getBreakEnd() != null) {
                            ps.setInt(1, rec.getKintaiRecId());
                            ps.setString(2, rec.getEmpno());   // 勤怠の従業員ID
                            ps.setTime(3, b.getBreakStart());
                            ps.setTime(4, b.getBreakEnd());
                            ps.setString(5, loginEmpId);      // 作成者
                            ps.setString(6, loginEmpId);      // 更新者
                            ps.addBatch();
                        }
                    }
                    ps.executeBatch();
                }
            }

            conn.commit(); // コミット

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
