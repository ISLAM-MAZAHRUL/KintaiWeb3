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

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

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

            // アイドルデータの取得（開始から終了までのすべてのデータを統合）
            List<String> empIds = List.of(empId);
            List<KintaiRecBean> records = kintaiRecDao.getKintaiRecords(
                empIds, null, null, monthStart, monthEnd, 0);
            MonthlySummaryBean summary =
                    kintaiRecDao.getPeriodSummary(empId, monthStart, monthEnd);
            if ("preview".equals(action)) {

                request.setAttribute("emp", emp);
                request.setAttribute("records", records);
                request.setAttribute("startMonth", startMonth);
                request.setAttribute("endMonth", endMonth);

                
                request.setAttribute("endYm", endYm);
                request.setAttribute("summary", summary);

                request.getRequestDispatcher("/web/kinmu_hyo_export.jsp")
                       .forward(request, response);

                return;
            }

            // 以下は、CSVファイルをダウンロードするためのロジックです（月初めから月末までループ処理されます）。
            
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
            sb.append("日付,出勤状況,曜日,始業時間,終了時間,休憩時間,勤務時間,残業時間,プロジェクトコード\n");
            
            // 曜日配列
            String[] weekdays = {"日", "月", "火", "水", "木", "金", "土"};

            // 開始日から終了日までの範囲を毎日ループします
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
            
                

                // 残業時間
             // 日付
                sb.append(date.getMonthValue() + "/"
                        + String.format("%02d", date.getDayOfMonth()))
                        .append(",");

                // 出勤状況
                String status = "";

                if (rec != null && rec.getAttendanceStatus() != null) {
                    status = rec.getAttendanceStatus();
                }

                boolean isWeekend =
                        date.getDayOfWeek().getValue() == 6
                        || date.getDayOfWeek().getValue() == 7;

                if (status == null || status.isEmpty()) {
                    if (isWeekend) {
                        status = "休み";
                    } else if (rec != null && rec.getClockIn() != null) {
                        status = "出勤";
                    }
                }

                sb.append(status).append(",");
                sb.append(weekday).append(",");
                sb.append(clockIn).append(",");
                sb.append(clockOut).append(",");
                sb.append(breakTime).append(",");
                sb.append(workTime).append(",");

                // 残業時間
                String overtimeTime = "";

                if (rec != null && rec.getClockIn() != null) {
                    overtimeTime = String.format("%.1f",
                            rec.getOvertimeMinutes() / 60.0);
                }

                sb.append(overtimeTime).append(",");
                sb.append(rec != null && rec.getProjectId() != null
                        ? rec.getProjectId()
                        : "").append("\n");
            }
                
         // 合計行
            sb.append("合計,,,,,");

            sb.append(summary.getTotalBreakHours()).append(",");

            sb.append(summary.getTotalWorkingHours()).append(",");

            sb.append(summary.getTotalOvertimeHours()).append("\n");

            sb.append("\n");
            
            sb.append("集計結果\n");
            sb.append("合計勤務時間," + summary.getTotalWorkingHours() + "\n");
            sb.append("合計残業時間," + summary.getTotalOvertimeHours() + "\n");
            
            sb.append("合計休憩時間," + summary.getTotalBreakHours() + "\n");
            
            
            
            
            
            if ("excel".equals(action)) {

                Workbook workbook = new XSSFWorkbook();

                for (YearMonth ym = startYm; !ym.isAfter(endYm); ym = ym.plusMonths(1)) {

                    Sheet sheet = workbook.createSheet(ym.toString());

                    int rowNum = 0;

                    Row titleRow = sheet.createRow(rowNum++);
                    titleRow.createCell(0).setCellValue("勤務表");

                    Row infoRow1 = sheet.createRow(rowNum++);
                    infoRow1.createCell(0).setCellValue("社員番号");
                    infoRow1.createCell(1).setCellValue(emp.getEmpId());

                    Row infoRow2 = sheet.createRow(rowNum++);
                    infoRow2.createCell(0).setCellValue("氏名");
                    infoRow2.createCell(1).setCellValue(emp.getEmpName());

                    Row infoRow3 = sheet.createRow(rowNum++);
                    infoRow3.createCell(0).setCellValue("対象月");
                    infoRow3.createCell(1).setCellValue(ym.toString());

                    rowNum++;

                    Row header = sheet.createRow(rowNum++);
                    header.createCell(0).setCellValue("日付");
                    header.createCell(1).setCellValue("出勤状況");
                    header.createCell(2).setCellValue("曜日");
                    header.createCell(3).setCellValue("始業時間");
                    header.createCell(4).setCellValue("終了時間");
                    header.createCell(5).setCellValue("休憩時間");
                    header.createCell(6).setCellValue("勤務時間");
                    header.createCell(7).setCellValue("残業時間");
                    header.createCell(8).setCellValue("プロジェクトコード");

                    LocalDate sheetStart = ym.atDay(1);
                    LocalDate sheetEnd = ym.atEndOfMonth();
                    double totalBreak = 0.0;
                    double totalWork = 0.0;
                    double totalOver = 0.0;

                    for (LocalDate date = sheetStart; !date.isAfter(sheetEnd); date = date.plusDays(1)) {

                        KintaiRecBean rec = null;
                        for (KintaiRecBean r : records) {
                            if (r.getKintaiDate().equals(date)) {
                                rec = r;
                                break;
                            }
                        }

                        String weekday = weekdays[date.getDayOfWeek().getValue() % 7];

                        Row row = sheet.createRow(rowNum++);

                        row.createCell(0).setCellValue(
                                date.getMonthValue() + "/" + String.format("%02d", date.getDayOfMonth())
                        );

                        String status = "";

                        if (rec != null && rec.getAttendanceStatus() != null) {
                            status = rec.getAttendanceStatus();
                        }

                        boolean isWeekend =
                                date.getDayOfWeek().getValue() == 6
                                || date.getDayOfWeek().getValue() == 7;

                        if (status == null || status.isEmpty()) {
                            if (isWeekend) {
                                status = "休み";
                            } else if (rec != null && rec.getClockIn() != null) {
                                status = "出勤";
                            }
                        }

                        row.createCell(1).setCellValue(status);
                        row.createCell(2).setCellValue(weekday);

                        row.createCell(3).setCellValue(
                                rec != null && rec.getClockIn() != null
                                        ? rec.getClockIn().toString().substring(0, 5)
                                        : ""
                        );

                        row.createCell(4).setCellValue(
                                rec != null && rec.getClockOut() != null
                                        ? rec.getClockOut().toString().substring(0, 5)
                                        : ""
                        );

                        row.createCell(5).setCellValue(
                                rec != null && rec.getClockIn() != null
                                        ? rec.getTotalBreakMinutes() / 60.0
                                        : 0.0
                        );

                        row.createCell(6).setCellValue(
                                rec != null && rec.getClockIn() != null
                                        ? rec.getActualWorkMinutes() / 60.0
                                        : 0.0
                        );

                        row.createCell(7).setCellValue(
                                rec != null && rec.getClockIn() != null
                                        ? rec.getOvertimeMinutes() / 60.0
                                        : 0.0
                        );
                        row.createCell(8).setCellValue(
                                rec != null && rec.getProjectId() != null
                                        ? Integer.parseInt(rec.getProjectId())
                                        : 0
                        );
                       
                        if (rec != null && rec.getClockIn() != null) {

                            totalBreak += rec.getTotalBreakMinutes() / 60.0;

                            totalWork += rec.getActualWorkMinutes() / 60.0;

                            totalOver += rec.getOvertimeMinutes() / 60.0;
                        }
                    }
                    
                 // ===== 合計 =====
                    Row totalRow = sheet.createRow(rowNum++);

                    totalRow.createCell(0).setCellValue("合計");

                    totalRow.createCell(5).setCellValue(totalBreak);

                    totalRow.createCell(6).setCellValue(totalWork);

                    totalRow.createCell(7).setCellValue(totalOver);

                    // ===== 集計結果 =====
                    rowNum++;

                    Row resultTitle = sheet.createRow(rowNum++);
                    resultTitle.createCell(0).setCellValue("集計結果");

                    Row resultWork = sheet.createRow(rowNum++);
                    resultWork.createCell(0).setCellValue("合計勤務時間：");
                    resultWork.createCell(1).setCellValue(totalWork);

                    Row resultOver = sheet.createRow(rowNum++);
                    resultOver.createCell(0).setCellValue("合計残業時間：");
                    resultOver.createCell(1).setCellValue(totalOver);

                    Row resultBreak = sheet.createRow(rowNum++);
                    resultBreak.createCell(0).setCellValue("合計休憩時間：");
                    resultBreak.createCell(1).setCellValue(totalBreak);
                    for (int i = 0; i <= 20; i++) {
                        sheet.autoSizeColumn(i);
                    }
                }
                

                String filename = "勤務表_" + emp.getEmpName() + "_" +
                        startMonth.replace("-", "") + "_to_" +
                        endMonth.replace("-", "") + ".xlsx";

                String encodedFilename = java.net.URLEncoder.encode(filename, "UTF-8")
                        .replace("+", "%20");

                response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                response.setHeader("Content-Disposition",
                        "attachment; filename*=UTF-8''" + encodedFilename);

                OutputStream out = response.getOutputStream();
                workbook.write(out);
                workbook.close();
                out.flush();
                out.close();

                return;
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

                response.sendError(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "エクスポート中にエラーが発生しました: " + e.getMessage()
                );
            }

            }
}