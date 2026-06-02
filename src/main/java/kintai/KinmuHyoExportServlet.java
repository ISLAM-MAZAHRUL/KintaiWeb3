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

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
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

                XSSFWorkbook workbook = new XSSFWorkbook();

                // ===== スタイル定義 =====
                // タイトルスタイル
                XSSFCellStyle titleStyle = workbook.createCellStyle();
                Font titleFont = workbook.createFont();
                titleFont.setBold(true);
                titleFont.setFontHeightInPoints((short) 14);
                titleStyle.setFont(titleFont);
                titleStyle.setAlignment(HorizontalAlignment.CENTER);

                // ヘッダースタイル（オレンジ背景）
                XSSFCellStyle headerStyle = workbook.createCellStyle();
                headerStyle.setFillForegroundColor(new XSSFColor(new byte[]{(byte)255, (byte)152, (byte)0}, null));
                headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                Font headerFont = workbook.createFont();
                headerFont.setBold(true);
                headerFont.setColor(IndexedColors.WHITE.getIndex());
                headerStyle.setFont(headerFont);
                headerStyle.setAlignment(HorizontalAlignment.CENTER);
                headerStyle.setBorderBottom(BorderStyle.THIN);
                headerStyle.setBorderTop(BorderStyle.THIN);
                headerStyle.setBorderLeft(BorderStyle.THIN);
                headerStyle.setBorderRight(BorderStyle.THIN);

                // 通常セルスタイル
                XSSFCellStyle normalStyle = workbook.createCellStyle();
                normalStyle.setBorderBottom(BorderStyle.THIN);
                normalStyle.setBorderTop(BorderStyle.THIN);
                normalStyle.setBorderLeft(BorderStyle.THIN);
                normalStyle.setBorderRight(BorderStyle.THIN);
                normalStyle.setAlignment(HorizontalAlignment.CENTER);

                // 週末スタイル（薄いピンク）
                XSSFCellStyle weekendStyle = workbook.createCellStyle();
                weekendStyle.cloneStyleFrom(normalStyle);
                weekendStyle.setFillForegroundColor(new XSSFColor(new byte[]{(byte)255, (byte)224, (byte)224}, null));
                weekendStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

                // 合計行スタイル
                XSSFCellStyle totalStyle = workbook.createCellStyle();
                totalStyle.cloneStyleFrom(normalStyle);
                Font totalFont = workbook.createFont();
                totalFont.setBold(true);
                totalStyle.setFont(totalFont);
                totalStyle.setFillForegroundColor(new XSSFColor(new byte[]{(byte)241, (byte)243, (byte)245}, null));
                totalStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

                // ラベルスタイル
                XSSFCellStyle labelStyle = workbook.createCellStyle();
                Font labelFont = workbook.createFont();
                labelFont.setBold(true);
                labelStyle.setFont(labelFont);
                labelStyle.setBorderBottom(BorderStyle.THIN);
                labelStyle.setBorderTop(BorderStyle.THIN);
                labelStyle.setBorderLeft(BorderStyle.THIN);
                labelStyle.setBorderRight(BorderStyle.THIN);

                for (YearMonth ym = startYm; !ym.isAfter(endYm); ym = ym.plusMonths(1)) {

                    Sheet sheet = workbook.createSheet(ym.toString());

                    int rowNum = 0;

                    // タイトル行
                    Row titleRow = sheet.createRow(rowNum++);
                    Cell titleCell = titleRow.createCell(0);
                    titleCell.setCellValue("勤　務　表　(" + ym.toString() + ")");
                    titleCell.setCellStyle(titleStyle);
                    titleRow.setHeightInPoints(30);

                    // 社員情報
                    Row infoRow1 = sheet.createRow(rowNum++);
                    Cell lbl1 = infoRow1.createCell(0);
                    lbl1.setCellValue("社員番号");
                    lbl1.setCellStyle(labelStyle);
                    Cell val1 = infoRow1.createCell(1);
                    val1.setCellValue(emp.getEmpId());
                    val1.setCellStyle(normalStyle);

                    Row infoRow2 = sheet.createRow(rowNum++);
                    Cell lbl2 = infoRow2.createCell(0);
                    lbl2.setCellValue("氏名");
                    lbl2.setCellStyle(labelStyle);
                    Cell val2 = infoRow2.createCell(1);
                    val2.setCellValue(emp.getEmpName());
                    val2.setCellStyle(normalStyle);

                    Row infoRow3 = sheet.createRow(rowNum++);
                    Cell lbl3 = infoRow3.createCell(0);
                    lbl3.setCellValue("対象月");
                    lbl3.setCellStyle(labelStyle);
                    Cell val3 = infoRow3.createCell(1);
                    val3.setCellValue(ym.toString());
                    val3.setCellStyle(normalStyle);

                    rowNum++;

                    // ヘッダー行
                    Row header = sheet.createRow(rowNum++);
                    String[] headers = {"日付", "曜日", "出勤状況", "始業時間", "終了時間", "休憩時間", "勤務時間", "残業時間", "プロジェクトコード"};
                    for (int i = 0; i < headers.length; i++) {
                        Cell hCell = header.createCell(i);
                        hCell.setCellValue(headers[i]);
                        hCell.setCellStyle(headerStyle);
                    }
                    header.setHeightInPoints(20);

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
                        boolean isWknd = date.getDayOfWeek().getValue() == 6 || date.getDayOfWeek().getValue() == 7;
                        XSSFCellStyle rowStyle = isWknd ? weekendStyle : normalStyle;

                        Row row = sheet.createRow(rowNum++);
                        row.setHeightInPoints(18);

                        // 日付
                        Cell c0 = row.createCell(0);
                        c0.setCellValue(date.getMonthValue() + "/" + String.format("%02d", date.getDayOfMonth()));
                        c0.setCellStyle(rowStyle);

                        // 曜日
                        Cell c1 = row.createCell(1);
                        c1.setCellValue(weekday);
                        c1.setCellStyle(rowStyle);

                        // 出勤状況
                        String status = "";
                        if (rec != null && rec.getAttendanceStatus() != null) {
                            status = rec.getAttendanceStatus();
                        }
                        if (status == null || status.isEmpty()) {
                            if (isWknd) {
                                status = "休み";
                            } else if (rec != null && rec.getClockIn() != null) {
                                status = "出勤";
                            }
                        }
                        Cell c2 = row.createCell(2);
                        c2.setCellValue(status);
                        c2.setCellStyle(rowStyle);

                        // 始業時間
                        Cell c3 = row.createCell(3);
                        c3.setCellValue(rec != null && rec.getClockIn() != null ? rec.getClockIn().toString().substring(0, 5) : "");
                        c3.setCellStyle(rowStyle);

                        // 終了時間
                        Cell c4 = row.createCell(4);
                        c4.setCellValue(rec != null && rec.getClockOut() != null ? rec.getClockOut().toString().substring(0, 5) : "");
                        c4.setCellStyle(rowStyle);

                        // 休憩時間
                        Cell c5 = row.createCell(5);
                        c5.setCellValue(rec != null && rec.getClockIn() != null ? rec.getTotalBreakMinutes() / 60.0 : 0.0);
                        c5.setCellStyle(rowStyle);

                        // 勤務時間
                        Cell c6 = row.createCell(6);
                        c6.setCellValue(rec != null && rec.getClockIn() != null ? rec.getActualWorkMinutes() / 60.0 : 0.0);
                        c6.setCellStyle(rowStyle);

                        // 残業時間
                        Cell c7 = row.createCell(7);
                        c7.setCellValue(rec != null && rec.getClockIn() != null ? rec.getOvertimeMinutes() / 60.0 : 0.0);
                        c7.setCellStyle(rowStyle);

                        // プロジェクトコード
                        Cell c8 = row.createCell(8);
                        c8.setCellValue(rec != null && rec.getProjectCode() != null ? rec.getProjectCode() : "");
                        c8.setCellStyle(rowStyle);
                       
                        if (rec != null && rec.getClockIn() != null) {

                            totalBreak += rec.getTotalBreakMinutes() / 60.0;

                            totalWork += rec.getActualWorkMinutes() / 60.0;

                            totalOver += rec.getOvertimeMinutes() / 60.0;
                        }
                    }
                    
                 // ===== 合計 =====
                    Row totalRow = sheet.createRow(rowNum++);
                    totalRow.setHeightInPoints(20);

                    Cell tc0 = totalRow.createCell(0);
                    tc0.setCellValue("合計");
                    tc0.setCellStyle(totalStyle);

                    for (int i = 1; i <= 4; i++) {
                        totalRow.createCell(i).setCellStyle(totalStyle);
                    }

                    Cell tc5 = totalRow.createCell(5);
                    tc5.setCellValue(totalBreak);
                    tc5.setCellStyle(totalStyle);

                    Cell tc6 = totalRow.createCell(6);
                    tc6.setCellValue(totalWork);
                    tc6.setCellStyle(totalStyle);

                    Cell tc7 = totalRow.createCell(7);
                    tc7.setCellValue(totalOver);
                    tc7.setCellStyle(totalStyle);

                    totalRow.createCell(8).setCellStyle(totalStyle);

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
