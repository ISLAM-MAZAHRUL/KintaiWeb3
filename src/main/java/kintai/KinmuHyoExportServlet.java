package kintai;

import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/KinmuHyoExportServlet")
public class KinmuHyoExportServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private KintaiRecDao kintaiRecDao = new KintaiRecDao();
    private EmpDao empDao = new EmpDao();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doPost(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        UserBean loginUser = (UserBean) session.getAttribute("user");

        // 対象月取得
        String startMonth = request.getParameter("startMonth");
        String endMonth = request.getParameter("endMonth");

        if (startMonth == null || startMonth.isEmpty()) {
            startMonth = YearMonth.now().toString();
        }

        if (endMonth == null || endMonth.isEmpty()) {
            endMonth = YearMonth.now().toString();
        }
        
        // アクション取得
        String action = request.getParameter("action");
        if (action == null || action.isEmpty()) {
            action = "download";
        }

        // 対象従業員ID
        String empId = request.getParameter("empId");
        
        // empIdが空の場合はログインユーザーのIDを使う
        if (empId == null || empId.trim().isEmpty()) {
            empId = loginUser.getEmpId();
        }

        // 権限チェック — 一般社員は自分のみ
        if (loginUser.getRoleId() != 1) {
            empId = loginUser.getEmpId();
        }

        try {
            YearMonth startYm = YearMonth.parse(startMonth);
            YearMonth endYm = YearMonth.parse(endMonth);

            LocalDate monthStart = startYm.atDay(1);
            LocalDate monthEnd = endYm.atEndOfMonth();

            // 従業員情報取得
            EmpBean emp = empDao.findByEmpId(empId);
            if (emp == null) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "従業員が見つかりません");
                return;
            }

            // 勤怠データ取得 (শুরু থেকে শেষ তারিখের সব ডেটা একসাথে নিয়ে আসা হচ্ছে)
            List<String> empIds = List.of(empId);
            List<KintaiRecBean> records = kintaiRecDao.getKintaiRecords(
                empIds, null, null, monthStart, monthEnd, 0);

            if ("preview".equals(action)) {
                // JSP-তে ডেটা পাঠিয়ে প্রিভিউ দেখানো হচ্ছে
                request.setAttribute("emp", emp);
                request.setAttribute("records", records);
                request.setAttribute("startMonth", startMonth);
                request.setAttribute("endMonth", endMonth);
                request.setAttribute("startYm", startYm);
                request.setAttribute("endYm", endYm);
                request.getRequestDispatcher("/web/kinmu_hyo_export.jsp")
                       .forward(request, response);
                return;
            }

            // ---------------------------------------------------------
            // এখান থেকে CSV ডাউনলোডের লজিক (যা শুরু থেকে শেষ মাস পর্যন্ত লুপ ঘুরবে)
            // ---------------------------------------------------------
            StringBuilder sb = new StringBuilder();

            // ヘッダー情報
            sb.append("勤務表\n");
            sb.append("対象期間," + startMonth + " 〜 " + endMonth + "\n");
            sb.append("\n");
            sb.append("会社名,株式会社エービーシー\n");
            sb.append("フリガナ,\n");
            sb.append("氏名," + emp.getEmpName() + "\n");
            sb.append("\n");

            // 明細ヘッダー
            sb.append("日付,曜日,出社,始業時間,終了時間,休憩時間,勤務時間,備考\n");

            // 曜日配列
            String[] weekdays = {"日", "月", "火", "水", "木", "金", "土"};

            // রেঞ্জের শুরুর তারিখ থেকে শেষ তারিখ পর্যন্ত প্রতিদিনের লুপ
            for (LocalDate date = monthStart; !date.isAfter(monthEnd); date = date.plusDays(1)) {
                String weekday = weekdays[date.getDayOfWeek().getValue() % 7];

                // その日の勤怠を検索
                KintaiRecBean rec = null;
                for (KintaiRecBean r : records) {
                    if (r.getKintaiDate().equals(date)) {
                        rec = r;
                        break;
                    }
                }

                String shussha = "";
                String clockIn = "";
                String clockOut = "";
                String breakTime = "";
                String workTime = "";
                String remarks = "";

                if (rec != null && rec.getClockIn() != null) {
                    shussha = "○";
                    clockIn = rec.getClockIn().toString().substring(0, 5);
                    clockOut = rec.getClockOut() != null ?
                        rec.getClockOut().toString().substring(0, 5) : "";
                    breakTime = String.format("%.0f", rec.getTotalBreakMinutes() / 60.0);
                    workTime = String.format("%.1f", rec.getActualWorkMinutes() / 60.0);
                }

                // 日付を「MM/dd」か「yyyy/MM/dd」形式で出力
                sb.append(date.getMonthValue() + "/" + String.format("%02d", date.getDayOfMonth())).append(",");
                sb.append(weekday).append(",");
                sb.append(shussha).append(",");
                sb.append(clockIn).append(",");
                sb.append(clockOut).append(",");
                sb.append(breakTime).append(",");
                sb.append(workTime).append(",");
                sb.append(remarks).append("\n");
            }

            // ファイル名設定
            String filename = "勤務表_" + emp.getEmpName() + "_" +
                    startMonth.replace("-", "") + "_to_" + endMonth.replace("-", "") + ".csv";
            String encodedFilename = java.net.URLEncoder.encode(filename, "UTF-8")
                .replace("+", "%20");

            response.setContentType("text/csv; charset=UTF-8");
            response.setHeader("Content-Disposition",
                "attachment; filename*=UTF-8''" + encodedFilename);

            OutputStream out = response.getOutputStream();
            out.write(new byte[]{(byte)0xEF, (byte)0xBB, (byte)0xBF}); // UTF-8 BOM
            out.write(sb.toString().getBytes("UTF-8"));
            out.flush();
            out.close();

        } catch (Exception e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "エクスポート中にエラーが発生しました: " + e.getMessage());
        }
    }
}