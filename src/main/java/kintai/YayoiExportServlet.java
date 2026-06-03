package kintai;

import java.io.IOException;
import java.io.OutputStream;
import java.time.YearMonth;
import java.util.List;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/YayoiExportServlet")
public class YayoiExportServlet extends HttpServlet {
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

        String startMonth = request.getParameter("startMonth");
        String endMonth = request.getParameter("endMonth");

        if (startMonth == null || startMonth.isEmpty()
                || endMonth == null || endMonth.isEmpty()) {
            YearMonth now = YearMonth.now();
            startMonth = now.toString();
            endMonth = now.toString();
        }

        try {
            String action = request.getParameter("action");
            if (action == null) action = "download";

            System.out.println("action = " + action);

            YearMonth startYm = YearMonth.parse(startMonth);
            YearMonth endYm = YearMonth.parse(endMonth);

            // 全社員リスト取得
            List<EmpBean> empList = empDao.findAll();

            // 全社員IDリスト作成
            List<String> empIds = new java.util.ArrayList<>();
            for (EmpBean emp : empList) {
                empIds.add(emp.getEmpId());
            }

            // ===== 1回のDBアクセスで全データ取得！=====
            List<MonthlySummaryBean> allSummaries =
                kintaiRecDao.getAllMonthlySummaries(empIds, startMonth, endMonth);

            // empIdでMapに変換
            java.util.Map<String, MonthlySummaryBean> summaryMap = new java.util.HashMap<>();
            for (MonthlySummaryBean s : allSummaries) {
                summaryMap.put(s.getEmpId(), s);
            }

            // 全社員分のサマリーリスト作成
            List<MonthlySummaryBean> summaryList = new java.util.ArrayList<>();
            for (EmpBean emp : empList) {
                MonthlySummaryBean s = summaryMap.get(emp.getEmpId());
                if (s == null) s = new MonthlySummaryBean();
                summaryList.add(s);
            }

            // ===== プレビュー =====
            if ("preview".equals(action)) {
                request.setAttribute("empList", empList);
                request.setAttribute("summaryList", summaryList);
                request.setAttribute("startMonth", startMonth);
                request.setAttribute("endMonth", endMonth);
                request.getRequestDispatcher("/web/kinmu_yayoi_export.jsp")
                       .forward(request, response);
                return;
            }

            // ===== CSV/Excel共通ヘッダー =====
            StringBuilder sb = new StringBuilder();
            sb.append("弥生給与データ\n");
            sb.append("対象期間：" + startMonth + " 〜 " + endMonth + "\n");
            sb.append("出力日：" + java.time.LocalDate.now().toString() + "\n");
            sb.append("\n");
            sb.append("社員番号,氏名,所定労働日,出勤日数,所定労働時間,");
            sb.append("実働時間,特別休暇日数,有休日数,欠勤回数,");
            sb.append("普通残業時間,深夜残業時間,出勤基礎日数\n");

            for (int i = 0; i < empList.size(); i++) {
                EmpBean emp = empList.get(i);
                MonthlySummaryBean s = summaryList.get(i);

                sb.append(emp.getEmpId()).append(",");
                sb.append(emp.getEmpName()).append(",");
                sb.append(s.getTotalWorkDays()).append(",");
                sb.append(s.getActualAttendanceDays()).append(",");
                sb.append(s.getTotalWorkDays() * 8).append(",");
                sb.append(roundHours(s.getTotalWorkingHours())).append(",");
                sb.append(s.getHolidayWorkDays()).append(",");
                sb.append(s.getPaidLeaveDays()).append(",");
                sb.append(s.getAbsentDays()).append(",");
                sb.append(roundHours(s.getTotalOvertimeHours())).append(",");
                sb.append(roundHours(s.getTotalNightHours())).append(",");
                sb.append(s.getActualAttendanceDays()).append("\n");
            }

            // ===== Excel出力 =====
            if ("excel".equals(action)) {
                try {
                    org.apache.poi.xssf.usermodel.XSSFWorkbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
                    org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("弥生給与データ");

                    // タイトルスタイル
                    org.apache.poi.ss.usermodel.CellStyle titleStyle = workbook.createCellStyle();
                    org.apache.poi.ss.usermodel.Font titleFont = workbook.createFont();
                    titleFont.setBold(true);
                    titleFont.setFontHeightInPoints((short) 14);
                    titleStyle.setFont(titleFont);

                    // 情報スタイル
                    org.apache.poi.ss.usermodel.CellStyle infoStyle = workbook.createCellStyle();
                    org.apache.poi.ss.usermodel.Font infoFont = workbook.createFont();
                    infoFont.setFontHeightInPoints((short) 10);
                    infoStyle.setFont(infoFont);

                    // ヘッダースタイル（オレンジ背景）
                    org.apache.poi.xssf.usermodel.XSSFCellStyle headerStyle = (org.apache.poi.xssf.usermodel.XSSFCellStyle) workbook.createCellStyle();
                    headerStyle.setFillForegroundColor(new org.apache.poi.xssf.usermodel.XSSFColor(new byte[]{(byte)253, (byte)126, (byte)20}, null));
                    headerStyle.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
                    org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
                    headerFont.setBold(true);
                    headerFont.setColor(org.apache.poi.ss.usermodel.IndexedColors.WHITE.getIndex());
                    headerStyle.setFont(headerFont);
                    headerStyle.setAlignment(org.apache.poi.ss.usermodel.HorizontalAlignment.CENTER);
                    headerStyle.setBorderBottom(org.apache.poi.ss.usermodel.BorderStyle.THIN);
                    headerStyle.setBorderTop(org.apache.poi.ss.usermodel.BorderStyle.THIN);
                    headerStyle.setBorderLeft(org.apache.poi.ss.usermodel.BorderStyle.THIN);
                    headerStyle.setBorderRight(org.apache.poi.ss.usermodel.BorderStyle.THIN);

                    // 通常セルスタイル
                    org.apache.poi.ss.usermodel.CellStyle normalStyle = workbook.createCellStyle();
                    normalStyle.setBorderBottom(org.apache.poi.ss.usermodel.BorderStyle.THIN);
                    normalStyle.setBorderTop(org.apache.poi.ss.usermodel.BorderStyle.THIN);
                    normalStyle.setBorderLeft(org.apache.poi.ss.usermodel.BorderStyle.THIN);
                    normalStyle.setBorderRight(org.apache.poi.ss.usermodel.BorderStyle.THIN);
                    normalStyle.setAlignment(org.apache.poi.ss.usermodel.HorizontalAlignment.CENTER);

                    // 偶数行スタイル
                    org.apache.poi.xssf.usermodel.XSSFCellStyle evenStyle = (org.apache.poi.xssf.usermodel.XSSFCellStyle) workbook.createCellStyle();
                    evenStyle.cloneStyleFrom(normalStyle);
                    evenStyle.setFillForegroundColor(new org.apache.poi.xssf.usermodel.XSSFColor(new byte[]{(byte)255, (byte)243, (byte)224}, null));
                    evenStyle.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);

                    int rowNum = 0;

                    // タイトル行
                    org.apache.poi.ss.usermodel.Row titleRow = sheet.createRow(rowNum++);
                    org.apache.poi.ss.usermodel.Cell titleCell = titleRow.createCell(0);
                    titleCell.setCellValue("弥生給与データ");
                    titleCell.setCellStyle(titleStyle);
                    titleRow.setHeightInPoints(25);

                    // 対象期間
                    org.apache.poi.ss.usermodel.Row infoRow1 = sheet.createRow(rowNum++);
                    org.apache.poi.ss.usermodel.Cell infoCell1 = infoRow1.createCell(0);
                    infoCell1.setCellValue("対象期間：" + startMonth + " 〜 " + endMonth);
                    infoCell1.setCellStyle(infoStyle);

                    // 出力日
                    org.apache.poi.ss.usermodel.Row infoRow2 = sheet.createRow(rowNum++);
                    org.apache.poi.ss.usermodel.Cell infoCell2 = infoRow2.createCell(0);
                    infoCell2.setCellValue("出力日：" + java.time.LocalDate.now().toString() + "　対象社員数：" + empList.size() + "名");
                    infoCell2.setCellStyle(infoStyle);

                    rowNum++; // 空行

                    // ヘッダー行
                    String[] headers = {"社員番号", "氏名", "所定労働日", "出勤日数", "所定労働時間",
                                        "実働時間", "特別休暇日数", "有休日数", "欠勤回数",
                                        "普通残業時間", "深夜残業時間", "出勤基礎日数"};
                    org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(rowNum++);
                    headerRow.setHeightInPoints(20);
                    for (int i = 0; i < headers.length; i++) {
                        org.apache.poi.ss.usermodel.Cell hCell = headerRow.createCell(i);
                        hCell.setCellValue(headers[i]);
                        hCell.setCellStyle(headerStyle);
                    }

                    // データ行
                    for (int i = 0; i < empList.size(); i++) {
                        EmpBean emp = empList.get(i);
                        MonthlySummaryBean s = summaryList.get(i);
                        org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowNum++);
                        row.setHeightInPoints(18);
                        org.apache.poi.ss.usermodel.CellStyle rowStyle = (i % 2 == 0) ? normalStyle : evenStyle;

                        org.apache.poi.ss.usermodel.Cell c0 = row.createCell(0); c0.setCellValue(emp.getEmpId()); c0.setCellStyle(rowStyle);
                        org.apache.poi.ss.usermodel.Cell c1 = row.createCell(1); c1.setCellValue(emp.getEmpName()); c1.setCellStyle(rowStyle);
                        org.apache.poi.ss.usermodel.Cell c2 = row.createCell(2); c2.setCellValue(s.getTotalWorkDays()); c2.setCellStyle(rowStyle);
                        org.apache.poi.ss.usermodel.Cell c3 = row.createCell(3); c3.setCellValue(s.getActualAttendanceDays()); c3.setCellStyle(rowStyle);
                        org.apache.poi.ss.usermodel.Cell c4 = row.createCell(4); c4.setCellValue(s.getTotalWorkDays() * 8); c4.setCellStyle(rowStyle);
                        org.apache.poi.ss.usermodel.Cell c5 = row.createCell(5); c5.setCellValue(roundHours(s.getTotalWorkingHours())); c5.setCellStyle(rowStyle);
                        org.apache.poi.ss.usermodel.Cell c6 = row.createCell(6); c6.setCellValue(s.getHolidayWorkDays()); c6.setCellStyle(rowStyle);
                        org.apache.poi.ss.usermodel.Cell c7 = row.createCell(7); c7.setCellValue(s.getPaidLeaveDays()); c7.setCellStyle(rowStyle);
                        org.apache.poi.ss.usermodel.Cell c8 = row.createCell(8); c8.setCellValue(s.getAbsentDays()); c8.setCellStyle(rowStyle);
                        org.apache.poi.ss.usermodel.Cell c9 = row.createCell(9); c9.setCellValue(roundHours(s.getTotalOvertimeHours())); c9.setCellStyle(rowStyle);
                        org.apache.poi.ss.usermodel.Cell c10 = row.createCell(10); c10.setCellValue(roundHours(s.getTotalNightHours())); c10.setCellStyle(rowStyle);
                        org.apache.poi.ss.usermodel.Cell c11 = row.createCell(11); c11.setCellValue(s.getActualAttendanceDays()); c11.setCellStyle(rowStyle);
                    }

                    // 列幅設定
                    for (int i = 0; i < headers.length; i++) {
                        sheet.setColumnWidth(i, 4000);
                    }

                    String filename = "弥生インポート用_" + startMonth + "_to_" + endMonth + ".xlsx";
                    String encodedFilename = java.net.URLEncoder.encode(filename, "UTF-8").replace("+", "%20");
                    response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                    response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFilename);
                    OutputStream out = response.getOutputStream();
                    workbook.write(out);
                    out.flush();
                    workbook.close();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                return;
            }

            // ===== CSV出力（デフォルト）=====
            String filename = "弥生インポート用_" + startMonth + "_to_" + endMonth + ".csv";
            String encodedFilename = java.net.URLEncoder.encode(filename, "UTF-8").replace("+", "%20");
            response.setContentType("text/csv; charset=UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFilename);
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
    

    private double roundHours(java.math.BigDecimal hours) {
        if (hours == null) return 0.0;
        return Math.round(hours.doubleValue() * 100.0) / 100.0;
    }
    
    
}
