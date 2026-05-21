package kintai;
//173行編集したい
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.sql.Time;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * 勤怠登録に関連するリクエストを処理するサーブレット。
 * データベースと連携して勤怠データの表示・登録・更新・削除を行う。
 *
 * 主な機能：
 * - 出勤・退勤の打刻処理
 * - 休憩時間の登録・削除
 * - 当日の勤怠データの表示
 */
@WebServlet("/workPunch") // URLパターン「/workPunch」にマッピング
public class WorkPunchServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    // 勤怠データアクセス用のDAOインスタンス
    private WorkTimeDao workTimeDao = new WorkTimeDao();

    /**
     * GETリクエストの処理メソッド
     * 勤怠登録画面(dakoku.jsp)を表示する前に、
     * ログイン中のユーザーの今日の勤怠データをデータベースから取得してJSPに渡す。
     *
     * @param request HTTPリクエストオブジェクト
     * @param response HTTPレスポンスオブジェクト
     * @throws ServletException サーブレット例外
     * @throws IOException 入出力例外
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        // セッション情報を取得（既存のセッションのみ、新規作成はしない）
        HttpSession session = request.getSession(false);

        // セッションが存在しない、またはユーザー情報がない場合はログイン画面にリダイレクト
        if (session == null || session.getAttribute("user") == null) {
            response.sendRedirect(request.getContextPath() + "/web/login.jsp");
            return;
        }
        
        // セッションからログインユーザーの情報を取得
        UserBean user = (UserBean) session.getAttribute("user");
        String empId = user.getEmpId(); // 従業員番号を取得
        LocalDate today = LocalDate.now(); // 今日の日付を取得

        // データベースから今日の勤怠データと休憩データリストを取得
        WorkTimeBean workTime = workTimeDao.findWorkTimeByDate(empId, today);
//        List<BreakBean> breaks = workTimeDao.findBreaksByDate(empId, today);

        // JSPに渡すための勤怠データ用のマップを作成
        Map<String, String> workTimeData = new HashMap<>();
        // 時間表示用のフォーマッター（HH:mm形式）
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

        // 勤怠データが存在する場合、出勤・退勤時刻をフォーマットしてマップに格納
        if (workTime != null) {
            // 出勤時刻が記録されている場合
            if (workTime.getClockIn() != null) {
                workTimeData.put("clockInTime", workTime.getClockIn().toLocalTime().format(timeFormatter));
            }
            // 退勤時刻が記録されている場合
            if (workTime.getClockOut() != null) {
                workTimeData.put("clockOutTime", workTime.getClockOut().toLocalTime().format(timeFormatter));
            }
        }
        int userRoleId = user.getRoleId();

     // 遷移先リンクのURLを権限に応じて設定
     String redirectUrl = (userRoleId == 1) 
         ? request.getContextPath() + "/AdminMenuServlet" 
         : request.getContextPath() + "/menu";

     // JSPに渡すデータをセッションに設定
     session.setAttribute("workTimeData", workTimeData);

     // POST完了後はリダイレクトして画面を再表示（PRGパターン）
     response.sendRedirect(redirectUrl);
    }

    /**
     * POSTリクエストの処理メソッド
     * menu.jspのボタンに応じた各種処理を実行する：
     * - 出勤打刻（clock_in）
     * - 退勤打刻（clock_out）
     * - 休憩時間追加（add_break）
     * - 休憩時間削除（delete_break）
     * @param request HTTPリクエストオブジェクト
     * @param response HTTPレスポンスオブジェクト
     * @throws ServletException サーブレット例外
     * @throws IOException 入出力例外
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    	System.out.println("★★ doPost 呼ばれたよ！ action=" + request.getParameter("action"));

        // セッション情報を取得（既存のセッションのみ）
        HttpSession session = request.getSession(false);

        // ログインチェック：セッションが存在しない、またはユーザー情報がない場合
        if (session == null || session.getAttribute("user") == null) {
            response.sendRedirect(request.getContextPath() + "/web/login.jsp");
            return;
        }

        // ログインユーザーの情報を取得
        UserBean user = (UserBean) session.getAttribute("user");
        String empId = user.getEmpId(); // 従業員番号
        LocalDate today = LocalDate.now(); // 今日の日付

        // リクエストパラメータから実行するアクション（処理種別）を取得
        String action = request.getParameter("action");
        if (action == null) action = ""; // nullの場合は空文字にする

        // データベースから今日の勤怠データを取得
        WorkTimeBean workTime = workTimeDao.findWorkTimeByDate(empId, today);

        // アクションに応じて処理を分岐
        switch (action) {
            case "clock_in": // 出勤打刻処理
                // まだ出勤記録がない場合のみ新規作成
                if (workTime == null) {
                    // 新しい勤怠データオブジェクトを作成
                    WorkTimeBean newWorkTime = new WorkTimeBean();
                    newWorkTime.setEmpId(empId); // 従業員番号設定
                    newWorkTime.setKintaiDate(today); // 勤怠日付設定
                    newWorkTime.setClockIn(Time.valueOf(LocalTime.now())); // 現在時刻を出勤時刻として設定

                    // データベースに保存
                    workTimeDao.saveWorkTime(newWorkTime);
                    // 成功メッセージを設定
                    request.setAttribute("successMessage", "出勤打刻を記録しました");
                }
                // 既に出勤記録がある場合は何もしない（重複打刻防止）
                break;

            case "clock_out": // 退勤打刻処理  //退勤ボタンを押したときに休憩が登録されていないならデフォルトで12:00-13:00を登録を実装したい
                // 出勤記録があり、かつ退勤記録がまだない場合のみ処理
                if (workTime != null && workTime.getClockOut() == null) {
                	Time now = Time.valueOf(LocalTime.now());
                	workTime.setClockOut(now); // 現在時刻を退勤時刻として設定
                    
                    /* 修正・追加*/
                	WorkTimeBean currentWorkTime = workTimeDao.findWorkTimeByDate(empId, today);
                	List<BreakBean> existingBreaks = workTimeDao.findBreaksByDate(empId, today);
                	if (existingBreaks == null || existingBreaks.isEmpty() ) {
                		BreakBean newBreak = new BreakBean();
                        newBreak.setKintaiRecId(currentWorkTime.getKintaiRecId());
                        
                        // "12:00:00" を Time に変換
                        Time start = Time.valueOf("12:00:00");
                        Time end = Time.valueOf("13:00:00");
                		newBreak.setBreakStart(start);
                        newBreak.setBreakEnd(end);
                        newBreak.setCreatedBy(empId); // 作成者を設定
                        newBreak.setUpdatedBy(empId);
                        
                        try {
							workTimeDao.addBreak(newBreak,empId);
						} catch (ClassNotFoundException | SQLException e) {
							// TODO 自動生成された catch ブロック
							e.printStackTrace();
						} // 休憩を追加
                	}
                    
                    
                	// 退勤処理で再計算
                	workTime.setClockOut(now);
                	List<BreakBean> breaks = workTimeDao.findBreaksByDate(empId, today);
                	recalculateWorkTime(workTime, breaks);
                	workTimeDao.saveWorkTime(workTime);
                    
                    request.setAttribute("successMessage", "退勤打刻を記録しました");
                }
                // 出勤記録がない、または既に退勤済みの場合は何もしない
                break;


        }

        // 処理完了後、画面を再表示するためdoGetメソッドを呼び出し
        doGet(request, response);
    }

//    /**
//     * 時間文字列を java.sql.Time オブジェクトに変換する補助メソッド
//     * "HH:mm"または"H:mm"形式の文字列に対応（例: "09:00" または "9:00"）
//     *
//     * @param timeStr 変換する時間文字列（例: "14:30", "9:15"）
//     * @return 変換後のTimeオブジェクト。変換できない場合はnullを返す
//     */
//    private Time parseTime(String timeStr) {
//        // 入力値チェック：nullまたは空文字の場合
//        if (timeStr == null || timeStr.trim().isEmpty()) {
//            return null;
//        }
//
//        try {
//            // "H:mm"フォーマットでパース（先頭の0を省略した形式にも対応）
//            // 例: "9:00" → LocalTime(09:00), "14:30" → LocalTime(14:30)
//            LocalTime localTime = LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("H:mm"));
//
//            // LocalTimeをjava.sql.Timeに変換して返す
//            return Time.valueOf(localTime);
//        } catch (DateTimeParseException e) {
//            // パースに失敗した場合はnullを返す（エラーメッセージは呼び出し元で処理）
//            return null; // フォーマットが不正な場合はnullを返す
//        }
//    }
    
    /** 追加
     * 稼働時間を計算しDBに保存するメソッド
     * 稼働時間 = 退勤時刻 - 出勤時刻 - 休憩時間
     */
    private void recalculateWorkTime(WorkTimeBean workTime, List<BreakBean> breaks) {
        if (workTime.getClockIn() != null && workTime.getClockOut() != null) {
            // 総勤務時間（分）
            long totalWorkedMinutes = Duration.between(
                workTime.getClockIn().toLocalTime(),
                workTime.getClockOut().toLocalTime()
            ).toMinutes();

            // 総休憩時間（分）
            long totalBreakMinutes = 0;
            for (BreakBean b : breaks) {
                if (b.getBreakStart() != null && b.getBreakEnd() != null) {
                    totalBreakMinutes += Duration.between(
                        b.getBreakStart().toLocalTime(),
                        b.getBreakEnd().toLocalTime()
                    ).toMinutes();
                }
            }

            // 実働時間（分）
            long netWorkedMinutes = totalWorkedMinutes - totalBreakMinutes;
            if (netWorkedMinutes < 0) netWorkedMinutes = 0;

            BigDecimal workingHours = BigDecimal.valueOf(netWorkedMinutes)
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
            workTime.setWorkingHours(workingHours);

            // 残業時間（8時間超過分）
            BigDecimal overtime = workingHours.subtract(BigDecimal.valueOf(8));
            if (overtime.compareTo(BigDecimal.ZERO) < 0) {
                overtime = BigDecimal.ZERO;
            }
            workTime.setOvertimeHours(overtime);

            // 深夜時間（22:00～翌5:00）
            BigDecimal nightHours = BigDecimal.ZERO;
            LocalTime clockIn = workTime.getClockIn().toLocalTime();
            LocalTime clockOut = workTime.getClockOut().toLocalTime();

            // 夜間1: 22:00～24:00
            if (!clockOut.isBefore(LocalTime.of(22, 0))) {
                LocalTime start = clockIn.isAfter(LocalTime.of(22, 0)) ? clockIn : LocalTime.of(22, 0);
                LocalTime end = clockOut.isAfter(LocalTime.MIDNIGHT) ? LocalTime.MIDNIGHT : clockOut;
                long night1 = Duration.between(start, end).toMinutes();
                nightHours = nightHours.add(BigDecimal.valueOf(Math.max(night1, 0)));
            }

            // 夜間2: 0:00～5:00
            if (clockOut.isBefore(LocalTime.of(5, 0)) || clockIn.isBefore(LocalTime.of(5, 0))) {
                LocalTime start = clockIn.isAfter(LocalTime.MIDNIGHT) ? clockIn : LocalTime.MIDNIGHT;
                LocalTime end = clockOut.isBefore(LocalTime.of(5, 0)) ? clockOut : LocalTime.of(5, 0);
                long night2 = Duration.between(start, end).toMinutes();
                nightHours = nightHours.add(BigDecimal.valueOf(Math.max(night2, 0)));
            }

            workTime.setNightHours(nightHours.divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP));

        } else {
            // 出退勤不完全 → 全て 0 にリセット
            workTime.setWorkingHours(BigDecimal.ZERO);
            workTime.setOvertimeHours(BigDecimal.ZERO);
            workTime.setNightHours(BigDecimal.ZERO);
        }
    }
    
    
}