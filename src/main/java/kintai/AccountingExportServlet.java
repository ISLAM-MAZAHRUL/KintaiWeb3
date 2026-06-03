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

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

@WebServlet("/AccountingExportServlet")
public class AccountingExportServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private KinmuManageDao kinmuDao = new KinmuManageDao();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doPost(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // セッションチェック
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        UserBean loginUser = (UserBean) session.getAttribute("user");

        String startMonth = request.getParameter("startMonth");
        String endMonth   = request.getParameter("endMonth");
        if (startMonth == null || startMonth.isEmpty()) startMonth = YearMonth.now().toString();
        if (endMonth   == null || endMonth.isEmpty())   endMonth   = startMonth;

        try {
            String action = request.getParameter("action");
            if (action == null) action = "preview";

            // データ取得
            List<KinmuManageBean.WorkAlloc> list;
            if (loginUser.getRoleId() == 1 || loginUser.getRoleId() == 2) {
                list = kinmuDao.findMonthlyByAllEmpRange(startMonth, endMonth);
            } else {
                list = kinmuDao.findMonthlyByEmpRange(loginUser.getEmpId(), startMonth, endMonth);
            }

            // プレビュー
            if ("preview".equals(action)) {
                request.setAttribute("results", list);
                request.setAttribute("startMonth", startMonth);
                request.setAttribute("endMonth", endMonth);
                request.getRequestDispatcher("/web/kinmu_acc_export.jsp").forward(request, response);
                return;
            }

            // Excelダウンロード
            if ("excel".equals(action)) {
                try {
                    XSSFWorkbook workbook = new XSSFWorkbook();
                    Sheet sheet = workbook.createSheet("プロジェクト別勤怠");

                    // ===== スタイル定義 =====
                    // タイトルスタイル
                    CellStyle titleStyle = workbook.createCellStyle();
                    Font titleFont = workbook.createFont();
                    titleFont.setBold(true);
                    titleFont.setFontHeightInPoints((short) 14);
                    titleStyle.setFont(titleFont);

                    // 情報スタイル
                    CellStyle infoStyle = workbook.createCellStyle();
                    Font infoFont = workbook.createFont();
                    infoFont.setFontHeightInPoints((short) 10);
                    infoStyle.setFont(infoFont);

                    // ヘッダースタイル（緑背景）
                    XSSFCellStyle headerStyle = (XSSFCellStyle) workbook.createCellStyle();
                    headerStyle.setFillForegroundColor(new XSSFColor(new byte[]{(byte)40, (byte)167, (byte)69}, null));
                    headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                    Font headerFont = workbook.createFont();
                    headerFont.setBold(true);
                    headerFont.setColor(org.apache.poi.ss.usermodel.IndexedColors.WHITE.getIndex());
                    headerStyle.setFont(headerFont);
                    headerStyle.setAlignment(HorizontalAlignment.CENTER);
                    headerStyle.setBorderBottom(BorderStyle.THIN);
                    headerStyle.setBorderTop(BorderStyle.THIN);
                    headerStyle.setBorderLeft(BorderStyle.THIN);
                    headerStyle.setBorderRight(BorderStyle.THIN);

                    // 通常セルスタイル
                    CellStyle normalStyle = workbook.createCellStyle();
                    normalStyle.setBorderBottom(BorderStyle.THIN);
                    normalStyle.setBorderTop(BorderStyle.THIN);
                    normalStyle.setBorderLeft(BorderStyle.THIN);
                    normalStyle.setBorderRight(BorderStyle.THIN);
                    normalStyle.setAlignment(HorizontalAlignment.CENTER);

                    // 偶数行スタイル
                    XSSFCellStyle evenStyle = (XSSFCellStyle) workbook.createCellStyle();
                    evenStyle.cloneStyleFrom(normalStyle);
                    evenStyle.setFillForegroundColor(new XSSFColor(new byte[]{(byte)240, (byte)255, (byte)244}, null));
                    evenStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

                    int rowNum = 0;

                    // タイトル行
                    Row titleRow = sheet.createRow(rowNum++);
                    Cell titleCell = titleRow.createCell(0);
                    titleCell.setCellValue("プロジェクト別勤怠");
                    titleCell.setCellStyle(titleStyle);
                    titleRow.setHeightInPoints(25);

                    // 対象期間
                    Row infoRow1 = sheet.createRow(rowNum++);
                    Cell infoCell1 = infoRow1.createCell(0);
                    infoCell1.setCellValue("対象期間：" + startMonth + " 〜 " + endMonth);
                    infoCell1.setCellStyle(infoStyle);

                    // 出力日・件数
                    Row infoRow2 = sheet.createRow(rowNum++);
                    Cell infoCell2 = infoRow2.createCell(0);
                    infoCell2.setCellValue("出力日：" + java.time.LocalDate.now().toString() + "　件数：" + list.size() + "件");
                    infoCell2.setCellStyle(infoStyle);

                    rowNum++; // 空行

                    // ヘッダー行
                    String[] headers = {"社員番号", "社員氏名", "年月", "プロジェクト名", "プロジェクトコード", "勤務時間"};
                    Row headerRow = sheet.createRow(rowNum++);
                    headerRow.setHeightInPoints(20);
                    for (int i = 0; i < headers.length; i++) {
                        Cell hCell = headerRow.createCell(i);
                        hCell.setCellValue(headers[i]);
                        hCell.setCellStyle(headerStyle);
                    }

                    // データ行
                    int dataRowNum = 0;
                    for (KinmuManageBean.WorkAlloc wa : list) {
                        Row row = sheet.createRow(rowNum++);
                        row.setHeightInPoints(18);
                        CellStyle rowStyle = (dataRowNum % 2 == 0) ? normalStyle : evenStyle;

                        Cell c0 = row.createCell(0); c0.setCellValue(wa.getEmpId()); c0.setCellStyle(rowStyle);
                        Cell c1 = row.createCell(1); c1.setCellValue(wa.getEmpName()); c1.setCellStyle(rowStyle);
                        Cell c2 = row.createCell(2); c2.setCellValue(wa.getYearMonth()); c2.setCellStyle(rowStyle);
                        Cell c3 = row.createCell(3); c3.setCellValue(wa.getProjectName()); c3.setCellStyle(rowStyle);
                        Cell c4 = row.createCell(4); c4.setCellValue(wa.getProjectCode() != null ? wa.getProjectCode() : ""); c4.setCellStyle(rowStyle);
                        Cell c5 = row.createCell(5); c5.setCellValue(wa.getWorkHours()); c5.setCellStyle(rowStyle);
                        dataRowNum++;
                    }

                    // 列幅自動調整
                    for (int i = 0; i < headers.length; i++) {
                        sheet.setColumnWidth(i, 5000);
                    }

                    String filename = "プロジェクト別勤怠_" + startMonth + "_" + endMonth + ".xlsx";
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

            // CSVダウンロード
            String filename = "プロジェクト別勤怠_" + startMonth + "_" + endMonth + ".csv";
            String encodedFilename = java.net.URLEncoder.encode(filename, "UTF-8").replace("+", "%20");
            response.setContentType("text/csv; charset=UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFilename);

            StringBuilder sb = new StringBuilder();
            sb.append("プロジェクト別勤怠\n");
            sb.append("対象期間：" + startMonth + " 〜 " + endMonth + "\n");
            sb.append("出力日：" + java.time.LocalDate.now().toString() + "\n");
            sb.append("\n");
            sb.append("社員番号,社員氏名,年月,プロジェクト名,プロジェクトコード,勤務時間\n");
            for (KinmuManageBean.WorkAlloc wa : list) {
                sb.append(wa.getEmpId()).append(",");
                sb.append(wa.getEmpName()).append(",");
                sb.append(wa.getYearMonth()).append(",");
                sb.append(wa.getProjectName()).append(",");
                sb.append(wa.getProjectCode() != null ? wa.getProjectCode() : "").append(",");
                sb.append(String.format("%.1f", wa.getWorkHours())).append("\n");
            }

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
