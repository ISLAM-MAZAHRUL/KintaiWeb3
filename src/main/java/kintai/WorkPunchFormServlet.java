package kintai;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;


@WebServlet("/showWorkPunchForm")
public class WorkPunchFormServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	// 勤怠データアクセス用のDAOインスタンス
	private WorkTimeDao workTimeDao = new WorkTimeDao();

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doPost(request, response);
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
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
		List<BreakBean> breaks = workTimeDao.findBreaksByDate(empId, today);

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

		// 休憩データをJSPで扱いやすい形式に変換
		List<Map<String, String>> breakList = new ArrayList<>();
		for (BreakBean breakBean : breaks) {
			// 各休憩データを個別のマップとして作成
			Map<String, String> breakItem = new HashMap<>();
			// 休憩ID（削除処理で使用）
			breakItem.put("breakId", String.valueOf(breakBean.getBreakId()));

			// 休憩開始時刻が記録されている場合
			if (breakBean.getBreakStart() != null) {
				breakItem.put("startTime", breakBean.getBreakStart().toLocalTime().format(timeFormatter));
			}
			// 休憩終了時刻が記録されている場合
			if (breakBean.getBreakEnd() != null) {
				breakItem.put("endTime", breakBean.getBreakEnd().toLocalTime().format(timeFormatter));
			}
			// 休憩データをリストに追加
			breakList.add(breakItem);
		}

		// JSPに渡すデータをリクエスト属性として設定
		request.setAttribute("workTimeData", workTimeData);
		request.setAttribute("breakList", breakList);

		// dakoku.jspにフォワード（画面表示）
		RequestDispatcher dispatcher = request.getRequestDispatcher("/web/dakoku.jsp");
		dispatcher.forward(request, response);
	}

}