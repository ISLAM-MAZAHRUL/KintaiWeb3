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

    private static final String[] WEEKDAYS = {"日", "月", "火", "水", "木", "金", "土"};

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

        String startMonth = request.getParameter("startMonth");
        String endMonth = request.getParameter("endMonth");

        if (startMonth == null || startMonth.isEmpty()) {
            startMonth = YearMonth.now().toString();
        }
        if (endMonth == null || endMonth.isEmpty()) {
            endMonth = YearMonth.now().toString();
        }

        String action = request.getParameter("action");
        if (action == null || action.isEmpty()) {
            action = "download";
        }

        String empId = request.getParameter("empId");
        if (empId == null || empId.trim().isEmpty()) {
            empId = loginUser.getEmpId();
        }
        if (loginUser.getRoleId() != 1) {
            empId = loginUser.getEmpId();
        }

        try {
            YearMonth startYm = YearMonth.parse(startMonth);
            YearMonth endYm = YearMonth.parse(endMonth);

            LocalDate monthStart = startYm.atDay(1);
            LocalDate monthEnd = endYm.atEndOfMonth();

            EmpBean emp = empDao.findByEmpId(empId);
            if (emp == null) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "従業員が見つかりません");
                return;
            }

            List<String> empIds = List.of(empId);
            List<KintaiRecBean> records = kintaiRecDao.getKintaiRecords(
                empIds, null, null, monthStart, monthEnd, 0);

            // ===== プレビュー =====
            if ("preview".equals(action)) {
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

            // ===== Excel出力（POI不要）=====
            if ("excel".equals(action)) {
                String xlsFilename = "勤務表_" + emp.getEmpName() + "_" +
                        startMonth.replace("-", "") + ".xlsx";
                String encodedXls = java.net.URLEncoder.encode(xlsFilename, "UTF-8")
                        .replace("+", "%20");
                response.setContentType(
                	    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                response.setHeader("Content-Disposition",
                        "attachment; filename*=UTF-8''" + encodedXls);

                StringBuilder sbXls = new StringBuilder();
                sbXls.append("日付,曜日,出社,始業時間,終了時間,休憩時間,勤務時間,備考\n");

                for (LocalDate date = monthStart; !date.isAfter(monthEnd); date = date.plusDays(1)) {
                    KintaiRecBean rec = null;
                    for (KintaiRecBean r : records) {
                        if (r.getKintaiDate().equals(date)) { rec = r; break; }
                    }
                    String weekday = WEEKDAYS[date.getDayOfWeek().getValue() % 7];
                    sbXls.append(date.getMonthValue() + "/" + String.format("%02d", date.getDayOfMonth())).append(",");
                    sbXls.append(weekday).append(",");
                    sbXls.append(rec != null && rec.getClockIn() != null ? "○" : "").append(",");
                    sbXls.append(rec != null && rec.getClockIn() != null ? rec.getClockIn().toString().substring(0, 5) : "").append(",");
                    sbXls.append(rec != null && rec.getClockOut() != null ? rec.getClockOut().toString().substring(0, 5) : "").append(",");
                    sbXls.append(rec != null && rec.getClockIn() != null ? String.format("%.1f", rec.getTotalBreakMinutes() / 60.0) : "").append(",");
                    sbXls.append(rec != null && rec.getClockIn() != null ? String.format("%.1f", rec.getActualWorkMinutes() / 60.0) : "").append(",");
                    sbXls.append("\n");
                }

                OutputStream xlsOut = response.getOutputStream();
                xlsOut.write(new byte[]{(byte)0xEF, (byte)0xBB, (byte)0xBF});
                xlsOut.write(sbXls.toString().getBytes("UTF-8"));
                xlsOut.flush();
                xlsOut.close();
                return;
            }

            // ===== CSV出力（デフォルト）=====
            StringBuilder sb = new StringBuilder();
            sb.append("勤務表\n");
            sb.append("対象期間," + startMonth + " 〜 " + endMonth + "\n");
            sb.append("\n");
            sb.append("会社名,株式会社エービーシー\n");
            sb.append("フリガナ,\n");
            sb.append("氏名," + emp.getEmpName() + "\n");
            sb.append("\n");
            sb.append("日付,曜日,出社,始業時間,終了時間,休憩時間,勤務時間,備考\n");

            for (LocalDate date = monthStart; !date.isAfter(monthEnd); date = date.plusDays(1)) {
                String weekday = WEEKDAYS[date.getDayOfWeek().getValue() % 7];

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

                sb.append(date.getMonthValue() + "/" + String.format("%02d", date.getDayOfMonth())).append(",");
                sb.append(weekday).append(",");
                sb.append(shussha).append(",");
                sb.append(clockIn).append(",");
                sb.append(clockOut).append(",");
                sb.append(breakTime).append(",");
                sb.append(workTime).append(",");
                sb.append(remarks).append("\n");
            }

            String filename = "勤務表_" + emp.getEmpName() + "_" +
                    startMonth.replace("-", "") + "_to_" + endMonth.replace("-", "") + ".csv";
            String encodedFilename = java.net.URLEncoder.encode(filename, "UTF-8")
                .replace("+", "%20");

            response.setContentType("text/csv; charset=UTF-8");
            response.setHeader("Content-Disposition",
                "attachment; filename*=UTF-8''" + encodedFilename);

            OutputStream out = response.getOutputStream();
            out.write(new byte[]{(byte)0xEF, (byte)0xBB, (byte)0xBF});
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