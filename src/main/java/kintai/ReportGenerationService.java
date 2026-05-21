package kintai;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 報告書生成サービスクラス
 * 各種報告書をPDF、Excel、CSV形式で生成する
 */
public class ReportGenerationService {
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy年MM月dd日");
    private EmpDao empDao = new EmpDao();
    
    /**
     * 個人別月次報告書生成
     */
    public byte[] generateIndividualReport(MonthlySummaryBean summary, List<KintaiRecBean> records, String format) {
        switch (format.toLowerCase()) {
            case "pdf":
                return generateIndividualPDF(summary, records);
            case "excel":
                return generateIndividualExcel(summary, records);
            case "csv":
                return generateIndividualCSV(summary, records);
            default:
                throw new IllegalArgumentException("Unsupported format: " + format);
        }
    }
    
    /**
     * 部署別集計報告書生成
     */
    public byte[] generateDepartmentReport(List<KintaiRecBean> records, String deptNo, String format) {
        switch (format.toLowerCase()) {
            case "pdf":
                return generateDepartmentPDF(records, deptNo);
            case "excel":
                return generateDepartmentExcel(records, deptNo);
            case "csv":
                return generateDepartmentCSV(records, deptNo);
            default:
                throw new IllegalArgumentException("Unsupported format: " + format);
        }
    }
    
    /**
     * 残業時間分析報告書生成
     */
    public byte[] generateOvertimeReport(List<KintaiRecBean> records, String format) {
        switch (format.toLowerCase()) {
            case "pdf":
                return generateOvertimePDF(records);
            case "excel":
                return generateOvertimeExcel(records);
            case "csv":
                return generateOvertimeCSV(records);
            default:
                throw new IllegalArgumentException("Unsupported format: " + format);
        }
    }
    
    /**
     * 全社員勤怠一覧報告書生成
     */
    public byte[] generateAllEmployeeReport(List<KintaiRecBean> records, String format) {
        switch (format.toLowerCase()) {
            case "pdf":
                return generateAllEmployeePDF(records);
            case "excel":
                return generateAllEmployeeExcel(records);
            case "csv":
                return generateAllEmployeeCSV(records);
            default:
                throw new IllegalArgumentException("Unsupported format: " + format);
        }
    }
    
    /**
     * 期間指定レポート生成
     */
    public byte[] generatePeriodReport(List<KintaiRecBean> records, LocalDate startDate, LocalDate endDate, String format) {
        switch (format.toLowerCase()) {
            case "pdf":
                return generatePeriodPDF(records, startDate, endDate);
            case "excel":
                return generatePeriodExcel(records, startDate, endDate);
            case "csv":
                return generatePeriodCSV(records, startDate, endDate);
            default:
                throw new IllegalArgumentException("Unsupported format: " + format);
        }
    }
    
    /**
     * 法定チェック報告書生成
     */
    public byte[] generateComplianceReport(List<KintaiRecBean> records, String format) {
        switch (format.toLowerCase()) {
            case "pdf":
                return generateCompliancePDF(records);
            case "excel":
                return generateComplianceExcel(records);
            case "csv":
                return generateComplianceCSV(records);
            default:
                throw new IllegalArgumentException("Unsupported format: " + format);
        }
    }
    
    // ===========================================
    // CSV生成メソッド群（簡単な実装）
    // ===========================================
    
    private byte[] generateIndividualCSV(MonthlySummaryBean summary, List<KintaiRecBean> records) {
        StringBuilder csv = new StringBuilder();
        csv.append("個人別月次報告書\n");
        csv.append("対象月,").append(summary.getTargetMonth()).append("\n");
        csv.append("出勤率,").append(summary.getAttendanceRateString()).append("\n");
        csv.append("総残業時間,").append(summary.getTotalOvertimeHoursString()).append("\n");
        csv.append("総実働時間,").append(summary.getTotalWorkingHoursString()).append("\n");
        csv.append("総休憩時間,").append(summary.getTotalBreakHoursString()).append("\n\n");
        
        csv.append("日付,曜日,出勤時刻,退勤時刻,休憩時間,実働時間,残業時間\n");
        for (KintaiRecBean record : records) {
            csv.append(record.getKintaiDate()).append(",");
            csv.append(getJapaneseDayOfWeek(record.getKintaiDate())).append(",");
            csv.append(record.getClockIn() != null ? record.getClockIn().toString().substring(0, 5) : "").append(",");
            csv.append(record.getClockOut() != null ? record.getClockOut().toString().substring(0, 5) : "").append(",");
            csv.append(record.getTotalBreakTimeFormatted()).append(",");
            csv.append(record.getActualWorkTimeFormatted()).append(",");
            csv.append(record.getOvertimeFormatted()).append("\n");
            csv.append(record.getNightovertimeFormatted()).append("\n");
        }
        
        return csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }
    
    private byte[] generateDepartmentCSV(List<KintaiRecBean> records, String deptNo) {
        StringBuilder csv = new StringBuilder();
        csv.append("部署別集計報告書\n");
        if (deptNo != null) {
            csv.append("対象部署,").append(deptNo).append("\n");
        }
        csv.append("生成日,").append(LocalDate.now().format(DATE_FORMATTER)).append("\n\n");
        
        // 部署別集計
        Map<String, List<KintaiRecBean>> deptGroups = records.stream()
            .collect(Collectors.groupingBy(r -> r.getDeptName() != null ? r.getDeptName() : "未設定"));
        
        for (Map.Entry<String, List<KintaiRecBean>> entry : deptGroups.entrySet()) {
            String deptName = entry.getKey();
            List<KintaiRecBean> deptRecords = entry.getValue();
            
            csv.append("部署名,").append(deptName).append("\n");
            csv.append("従業員数,").append(deptRecords.stream().map(KintaiRecBean::getEmpno).distinct().count()).append("名\n");
            csv.append("勤怠記録数,").append(deptRecords.size()).append("件\n");
            
            // 総残業時間計算
            long totalOvertimeMinutes = deptRecords.stream()
                .mapToLong(KintaiRecBean::getOvertimeMinutes)
                .sum();
            csv.append("総残業時間,").append(String.format("%.1f時間", totalOvertimeMinutes / 60.0)).append("\n\n");
        }
        
        return csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }
    
    private byte[] generateOvertimeCSV(List<KintaiRecBean> records) {
        StringBuilder csv = new StringBuilder();
        csv.append("残業時間分析報告書\n");
        csv.append("生成日,").append(LocalDate.now().format(DATE_FORMATTER)).append("\n\n");
        
        // 残業時間でフィルタリング
        List<KintaiRecBean> overtimeRecords = records.stream()
            .filter(r -> r.getOvertimeMinutes() > 0)
            .collect(Collectors.toList());
        
        csv.append("従業員番号,氏名,部署,日付,残業時間,深夜残業時間\n");
        for (KintaiRecBean record : overtimeRecords) {
            csv.append(record.getEmpno()).append(",");
            csv.append(record.getEmpName()).append(",");
            csv.append(record.getDeptName()).append(",");
            csv.append(record.getKintaiDate()).append(",");
            csv.append(record.getOvertimeFormatted()).append("\n");
            csv.append(record.getNightovertimeFormatted()).append("\n");
        }
        
        return csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }
    
    private byte[] generateAllEmployeeCSV(List<KintaiRecBean> records) {
        StringBuilder csv = new StringBuilder();
        csv.append("全社員勤怠一覧\n");
        csv.append("生成日,").append(LocalDate.now().format(DATE_FORMATTER)).append("\n\n");
        
        csv.append("従業員番号,氏名,部署,役職,日付,出勤時刻,退勤時刻,実働時間,残業時間,深夜残業時間\n");
        for (KintaiRecBean record : records) {
            csv.append(record.getEmpno()).append(",");
            csv.append(record.getEmpName()).append(",");
            csv.append(record.getDeptName()).append(",");
            csv.append(record.getPostName()).append(",");
            csv.append(record.getKintaiDate()).append(",");
            csv.append(record.getClockIn() != null ? record.getClockIn().toString().substring(0, 5) : "").append(",");
            csv.append(record.getClockOut() != null ? record.getClockOut().toString().substring(0, 5) : "").append(",");
            csv.append(record.getActualWorkTimeFormatted()).append(",");
            csv.append(record.getOvertimeFormatted()).append("\n");
            csv.append(record.getNightovertimeFormatted()).append("\n");
        }
        
        return csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }
    
    private byte[] generatePeriodCSV(List<KintaiRecBean> records, LocalDate startDate, LocalDate endDate) {
        StringBuilder csv = new StringBuilder();
        csv.append("期間指定レポート\n");
        csv.append("対象期間,").append(startDate).append("～").append(endDate).append("\n");
        csv.append("生成日,").append(LocalDate.now().format(DATE_FORMATTER)).append("\n\n");
        
        return generateAllEmployeeCSV(records); // 基本的に全社員一覧と同じ形式
    }
    
    private byte[] generateComplianceCSV(List<KintaiRecBean> records) {
        StringBuilder csv = new StringBuilder();
        csv.append("法定チェック報告書\n");
        csv.append("生成日,").append(LocalDate.now().format(DATE_FORMATTER)).append("\n\n");
        
        // 法定時間を超える記録をチェック
        csv.append("法定時間超過チェック結果\n");
        csv.append("従業員番号,氏名,日付,実働時間,残業時間,チェック結果\n");
        
        for (KintaiRecBean record : records) {
            String checkResult = "";
            if (record.getActualWorkMinutes() > 8 * 60) { // 8時間超過
                checkResult = "法定労働時間超過";
            }
            if (record.getOvertimeMinutes() > 2 * 60) { // 2時間以上の残業
                checkResult += (checkResult.isEmpty() ? "" : ",") + "長時間残業";
            }
            if (checkResult.isEmpty()) {
                checkResult = "正常";
            }
            
            csv.append(record.getEmpno()).append(",");
            csv.append(record.getEmpName()).append(",");
            csv.append(record.getKintaiDate()).append(",");
            csv.append(record.getActualWorkTimeFormatted()).append(",");
            csv.append(record.getOvertimeFormatted()).append(",");
            csv.append(record.getNightovertimeFormatted()).append(",");
            csv.append(checkResult).append("\n");
        }
        
        return csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }
    
    // ===========================================
    // PDF生成メソッド群（簡易実装）
    // ===========================================
    
    private byte[] generateIndividualPDF(MonthlySummaryBean summary, List<KintaiRecBean> records) {
        // 改良されたHTML形式で詳細な報告書を生成
        StringBuilder html = new StringBuilder();
        
        html.append("<!DOCTYPE html><html><head>");
        html.append("<meta charset='UTF-8'>");
        html.append("<title>個人別月次勤怠報告書</title>");
        html.append("<style>");
        html.append("body { font-family: 'MS Gothic', Arial, sans-serif; margin: 20px; font-size: 12px; }");
        html.append("h1 { text-align: center; color: #333; border-bottom: 2px solid #007bff; padding-bottom: 10px; }");
        html.append("h2 { color: #007bff; border-bottom: 1px solid #ccc; padding-bottom: 5px; margin-top: 25px; }");
        html.append("table { width: 100%; border-collapse: collapse; margin-bottom: 20px; }");
        html.append("th, td { border: 1px solid #000; padding: 8px; text-align: center; font-size: 11px; }");
        html.append("th { background-color: #f0f0f0; font-weight: bold; }");
        html.append(".info-table { margin-bottom: 15px; }");
        html.append(".info-table td { text-align: left; }");
        html.append(".summary-row { background-color: #e8f4fd; font-weight: bold; }");
        html.append("</style>");
        html.append("</head><body>");
        
        // タイトル
        html.append("<h1>個人別月次勤怠報告書</h1>");
        
        // 基本情報
        if (!records.isEmpty()) {
            KintaiRecBean firstRecord = records.get(0);
            html.append("<table class='info-table'>");
            html.append("<tr><td><strong>従業員番号:</strong></td><td>").append(firstRecord.getEmpno() != null ? firstRecord.getEmpno() : "").append("</td>");
            html.append("<td><strong>従業員名:</strong></td><td>").append(firstRecord.getEmpName() != null ? firstRecord.getEmpName() : "").append("</td></tr>");
            html.append("<tr><td><strong>部署:</strong></td><td>").append(firstRecord.getDeptName() != null ? firstRecord.getDeptName() : "").append("</td>");
            html.append("<td><strong>役職:</strong></td><td>").append(firstRecord.getPostName() != null ? firstRecord.getPostName() : "").append("</td></tr>");
            html.append("<tr><td><strong>対象月:</strong></td><td>").append(summary.getTargetMonth()).append("</td>");
            html.append("<td><strong>生成日:</strong></td><td>").append(LocalDate.now().format(DATE_FORMATTER)).append("</td></tr>");
            html.append("</table>");
        }
        
        // 月次統計サマリー
        html.append("<h2>月次統計サマリー</h2>");
        html.append("<table>");
        html.append("<tr><th>出勤日数/出社日</th><th>総実働時間</th><th>総残業時間</th><th>総休憩時間</th></tr>");
        html.append("<tr>");
        html.append("<td>").append(summary.getAttendanceRateString()).append("</td>");
        html.append("<td>").append(summary.getTotalWorkingHoursString()).append("</td>");
        html.append("<td>").append(summary.getTotalOvertimeHoursString()).append("</td>");
        html.append("<td>").append(summary.getTotalBreakHoursString()).append("</td>");
        html.append("</tr>");
        html.append("</table>");
        
        // 詳細勤怠記録
        html.append("<h2>詳細勤怠記録</h2>");
        html.append("<table>");
        html.append("<tr>");
        html.append("<th>日付</th><th>曜日</th><th>出勤時刻</th><th>退勤時刻</th>");
        html.append("<th>休憩時間</th><th>実働時間</th><th>残業時間</th><th>深夜残業時間</th><th>備考</th><th>プロジェクト時間</th>");
        html.append("</tr>");
        
        // 全ての詳細データ
        for (KintaiRecBean record : records) {
            String remarks = "";
            if (record.getClockIn() == null && record.getClockOut() == null) {
                remarks = "欠勤";
//            } else if (record.getClockIn() != null && 
//                      record.getClockIn().toLocalTime().isAfter(java.time.LocalTime.of(9, 0))) {
//                remarks = "遅刻";
            } else if (record.getOvertimeMinutes() > 4 * 60) {
                remarks = "長時間残業";
            } else if (record.getOvertimeMinutes() > 2 * 60) {
                remarks = "残業";
            }
            
            
            html.append("<tr>");
            html.append("<td>").append(record.getKintaiDate() != null ? record.getKintaiDate().toString() : "").append("</td>");
            html.append("<td>").append(getJapaneseDayOfWeek(record.getKintaiDate())).append("</td>");
            html.append("<td>").append(record.getClockIn() != null ? record.getClockIn().toString().substring(0, 5) : "---").append("</td>");
            html.append("<td>").append(record.getClockOut() != null ? record.getClockOut().toString().substring(0, 5) : "---").append("</td>");
            html.append("<td>").append(record.getTotalBreakTimeFormatted()).append("</td>");
            html.append("<td>").append(record.getActualWorkTimeFormatted()).append("</td>");
            html.append("<td>").append(record.getOvertimeFormatted()).append("</td>");
            html.append("<td>").append(record.getNightovertimeFormatted()).append("</td>");
            html.append("<td>").append(remarks).append("</td>");
            html.append("</tr>");
        }
        
        // 合計行
        long totalWorkMinutes = records.stream().mapToLong(KintaiRecBean::getActualWorkMinutes).sum();
        long totalOvertimeMinutes = records.stream().mapToLong(KintaiRecBean::getOvertimeMinutes).sum();
        long totalBreakMinutes = records.stream().mapToLong(KintaiRecBean::getTotalBreakMinutes).sum();
        
        html.append("<tr class='summary-row'>");
        html.append("<td colspan='4'><strong>合計</strong></td>");
        html.append("<td><strong>").append(String.format("%.1f時間", totalBreakMinutes / 60.0)).append("</strong></td>");
        html.append("<td><strong>").append(String.format("%.1f時間", totalWorkMinutes / 60.0)).append("</strong></td>");
        html.append("<td><strong>").append(String.format("%.1f時間", totalOvertimeMinutes / 60.0)).append("</strong></td>");
        html.append("<td></td>");
        html.append("</tr>");
        
        html.append("</table>");
        html.append("</body></html>");
        
        return html.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }
    
    /**
     * 法定時間チェック警告の生成
     */
    private void generateComplianceWarnings(PersonalReportBean report) {
        report.getComplianceWarnings().clear();
        
        // 月間残業時間チェック
        if (!report.isMonthlyOvertimeCompliant()) {
            report.addComplianceWarning("⚠️ 月間残業時間が45時間を超過しています (" + 
                report.getSummary().getTotalOvertimeHoursString() + ")");
        }
        
        // 1日8時間超過チェック
        int excessDays = report.getExcessWorkDaysCount();
        if (excessDays > 0) {
            report.addComplianceWarning("⚠️ 1日8時間超過日数: " + excessDays + "日");
        }
        
        // 長時間残業チェック
        List<KintaiRecBean> longOvertimeDays = report.getLongOvertimeDays();
        if (!longOvertimeDays.isEmpty()) {
            StringBuilder warning = new StringBuilder("⚠️ 4時間超残業日: ");
            for (int i = 0; i < longOvertimeDays.size() && i < 3; i++) {
                if (i > 0) warning.append(", ");
                warning.append(longOvertimeDays.get(i).getKintaiDate().getMonthValue())
                       .append("/").append(longOvertimeDays.get(i).getKintaiDate().getDayOfMonth());
            }
            if (longOvertimeDays.size() > 3) {
                warning.append(" 他").append(longOvertimeDays.size() - 3).append("日");
            }
            report.addComplianceWarning(warning.toString());
        }
        
//        // 遅刻チェック
//        int lateDays = report.getLateDaysCount();
//        if (lateDays > 0) {
//            report.addComplianceWarning("📝 遅刻日数: " + lateDays + "日");
//        }
        
        // 欠勤チェック
        int absentDays = report.getAbsentDaysCount();
        if (absentDays > 0) {
            report.addComplianceWarning("📝 欠勤日数: " + absentDays + "日");
        }
    }
    
    /**
     * 改良版個人レポートHTML生成
     */
    private byte[] generateEnhancedPersonalReportHTML(PersonalReportBean report) {
        StringBuilder html = new StringBuilder();
        
        // HTML基本構造とCSS
        html.append("<!DOCTYPE html><html><head>");
        html.append("<meta charset='UTF-8'>");
        html.append("<title>個人別月次勤怠報告書</title>");
        html.append(generatePrintOptimizedCSS());
        html.append("</head><body>");
        
        // 報告書ヘッダー
        html.append("<div class='report-container'>");
        html.append("<div class='header'>");
        html.append("<h1>個人別月次勤怠報告書</h1>");
        html.append("</div>");
        
        // 基本情報セクション
        html.append(generateBasicInfoSection(report));
        
        // 対象期間と出力日
        html.append(generatePeriodSection(report));
        
        // 月度統計セクション
        html.append(generateSummarySection(report));
        
        // 詳細記録テーブル
        html.append(generateDetailedRecordsTable(report));
        
        // 法定時間チェックセクション
        html.append(generateComplianceSection(report));
        
        html.append("</div>");
        html.append("</body></html>");
        
        return html.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }
    
    private byte[] generateDepartmentPDF(List<KintaiRecBean> records, String deptNo) {
        StringBuilder html = new StringBuilder();
        
        html.append("<!DOCTYPE html><html><head>");
        html.append("<meta charset='UTF-8'>");
        html.append("<title>部署別集計勤怠報告書</title>");
        html.append("<style>");
        html.append("body { font-family: 'MS Gothic', Arial, sans-serif; margin: 20px; font-size: 12px; }");
        html.append("h1 { text-align: center; color: #333; border-bottom: 2px solid #007bff; padding-bottom: 10px; }");
        html.append("h2 { color: #007bff; border-bottom: 1px solid #ccc; padding-bottom: 5px; margin-top: 25px; }");
        html.append("table { width: 100%; border-collapse: collapse; margin-bottom: 20px; }");
        html.append("th, td { border: 1px solid #000; padding: 8px; text-align: center; font-size: 11px; }");
        html.append("th { background-color: #f0f0f0; font-weight: bold; }");
        html.append(".info-table { margin-bottom: 15px; }");
        html.append(".info-table td { text-align: left; }");
        html.append(".summary-row { background-color: #e8f4fd; font-weight: bold; }");
        html.append("</style>");
        html.append("</head><body>");
        
        // タイトル
        html.append("<h1>部署別集計勤怠報告書</h1>");
        
        // 基本情報
        html.append("<table class='info-table'>");
        html.append("<tr><td><strong>対象部署:</strong></td><td>").append(deptNo != null ? deptNo : "全部署").append("</td>");
        html.append("<td><strong>総記録数:</strong></td><td>").append(records.size()).append("件</td></tr>");
        html.append("<tr><td><strong>対象従業員数:</strong></td><td>").append(records.stream().map(KintaiRecBean::getEmpno).distinct().count()).append("名</td>");
        html.append("<td><strong>生成日:</strong></td><td>").append(LocalDate.now().format(DATE_FORMATTER)).append("</td></tr>");
        html.append("</table>");
        
        // 部署別統計サマリー
        Map<String, List<KintaiRecBean>> deptGroups = records.stream()
            .collect(Collectors.groupingBy(r -> r.getDeptName() != null ? r.getDeptName() : "未設定"));
        
        html.append("<h2>部署別統計サマリー</h2>");
        html.append("<table>");
        html.append("<tr>");
        html.append("<th>部署名</th><th>従業員数</th><th>記録数</th>");
        html.append("<th>総労働時間</th><th>総残業時間</th><th>平均残業時間</th>");
        html.append("</tr>");
        
        for (Map.Entry<String, List<KintaiRecBean>> entry : deptGroups.entrySet()) {
            String deptName = entry.getKey();
            List<KintaiRecBean> deptRecords = entry.getValue();
            
            long totalOvertimeMinutes = deptRecords.stream().mapToLong(KintaiRecBean::getOvertimeMinutes).sum();
            long totalWorkMinutes = deptRecords.stream().mapToLong(KintaiRecBean::getActualWorkMinutes).sum();
            long empCount = deptRecords.stream().map(KintaiRecBean::getEmpno).distinct().count();
            
            html.append("<tr>");
            html.append("<td>").append(deptName).append("</td>");
            html.append("<td>").append(empCount).append("名</td>");
            html.append("<td>").append(deptRecords.size()).append("件</td>");
            html.append("<td>").append(String.format("%.1f時間", totalWorkMinutes / 60.0)).append("</td>");
            html.append("<td>").append(String.format("%.1f時間", totalOvertimeMinutes / 60.0)).append("</td>");
            html.append("<td>").append(String.format("%.1f時間", empCount > 0 ? (totalOvertimeMinutes / 60.0) / empCount : 0)).append("</td>");
            html.append("</tr>");
        }
        html.append("</table>");
        
        // 詳細勤怠記録
        html.append("<h2>詳細勤怠記録</h2>");
        html.append("<table>");
        html.append("<tr>");
        html.append("<th>日付</th><th>曜日</th><th>従業員番号</th><th>氏名</th><th>部署</th>");
        html.append("<th>出勤時刻</th><th>退勤時刻</th><th>実働時間</th><th>残業時間</th><th>深夜残業時間</th>");
        html.append("</tr>");
        
        // 全ての詳細データ
        for (KintaiRecBean record : records) {
            // 曜日の日本語表示
            String dayOfWeek = "";
            if (record.getKintaiDate() != null) {
                switch (record.getKintaiDate().getDayOfWeek()) {
                    case MONDAY: dayOfWeek = "月"; break;
                    case TUESDAY: dayOfWeek = "火"; break;
                    case WEDNESDAY: dayOfWeek = "水"; break;
                    case THURSDAY: dayOfWeek = "木"; break;
                    case FRIDAY: dayOfWeek = "金"; break;
                    case SATURDAY: dayOfWeek = "土"; break;
                    case SUNDAY: dayOfWeek = "日"; break;
                }
            }
            
            html.append("<tr>");
            html.append("<td>").append(record.getKintaiDate() != null ? record.getKintaiDate().toString() : "").append("</td>");
            html.append("<td>").append(dayOfWeek).append("</td>");
            html.append("<td>").append(record.getEmpno() != null ? record.getEmpno() : "").append("</td>");
            html.append("<td>").append(record.getEmpName() != null ? record.getEmpName() : "").append("</td>");
            html.append("<td>").append(record.getDeptName() != null ? record.getDeptName() : "").append("</td>");
            html.append("<td>").append(record.getClockIn() != null ? record.getClockIn().toString().substring(0, 5) : "---").append("</td>");
            html.append("<td>").append(record.getClockOut() != null ? record.getClockOut().toString().substring(0, 5) : "---").append("</td>");
            html.append("<td>").append(record.getActualWorkTimeFormatted()).append("</td>");
            html.append("<td>").append(record.getOvertimeFormatted()).append("</td>");
            html.append("<td>").append(record.getNightovertimeFormatted()).append("</td>");
            html.append("</tr>");
        }
        
        // 合計行
        long grandTotalWorkMinutes = records.stream().mapToLong(KintaiRecBean::getActualWorkMinutes).sum();
        long grandTotalOvertimeMinutes = records.stream().mapToLong(KintaiRecBean::getOvertimeMinutes).sum();
        
        html.append("<tr class='summary-row'>");
        html.append("<td colspan='7'><strong>合計</strong></td>");
        html.append("<td><strong>").append(String.format("%.1f時間", grandTotalWorkMinutes / 60.0)).append("</strong></td>");
        html.append("<td><strong>").append(String.format("%.1f時間", grandTotalOvertimeMinutes / 60.0)).append("</strong></td>");
        html.append("</tr>");
        
        html.append("</table>");
        html.append("</body></html>");
        
        return html.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }
    
    private byte[] generateOvertimePDF(List<KintaiRecBean> records) {
        long overtimeCount = records.stream().mapToLong(r -> r.getOvertimeMinutes() > 0 ? 1 : 0).sum();
        return generateSimpleHTMLReport("残業時間分析報告書", 
            "総記録数: " + records.size() + "件<br>" +
            "残業記録数: " + overtimeCount + "件<br>" +
            "残業率: " + String.format("%.1f%%", (double)overtimeCount / records.size() * 100));
    }
    
    private byte[] generateAllEmployeePDF(List<KintaiRecBean> records) {
        return generateSimpleHTMLReport("全社員勤怠一覧", 
            "総記録数: " + records.size() + "件<br>" +
            "対象従業員数: " + records.stream().map(KintaiRecBean::getEmpno).distinct().count() + "名<br>" +
            "期間: " + (records.isEmpty() ? "データなし" : records.get(records.size()-1).getKintaiDate() + "～" + records.get(0).getKintaiDate()));
    }
    
    private byte[] generatePeriodPDF(List<KintaiRecBean> records, LocalDate startDate, LocalDate endDate) {
        return generateSimpleHTMLReport("期間指定レポート", 
            "対象期間: " + startDate + "～" + endDate + "<br>" +
            "記録件数: " + records.size() + "件<br>" +
            "対象従業員数: " + records.stream().map(KintaiRecBean::getEmpno).distinct().count() + "名");
    }
    
    private byte[] generateCompliancePDF(List<KintaiRecBean> records) {
        long violationCount = records.stream().mapToLong(r -> r.getOvertimeMinutes() > 2 * 60 ? 1 : 0).sum();
        long longOvertimeCount = records.stream().mapToLong(r -> r.getOvertimeMinutes() > 4 * 60 ? 1 : 0).sum();
        return generateSimpleHTMLReport("法定チェック報告書", 
            "チェック対象: " + records.size() + "件<br>" +
            "2時間超残業: " + violationCount + "件<br>" +
            "4時間超残業: " + longOvertimeCount + "件<br>" +
            "要注意率: " + String.format("%.1f%%", (double)violationCount / records.size() * 100));
    }
    
    /**
     * 簡易HTMLレポート生成
     */
    private byte[] generateSimpleHTMLReport(String title, String content) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head>");
        html.append("<meta charset='UTF-8'>");
        html.append("<title>").append(title).append("</title>");
        html.append("<style>");
        html.append("body { font-family: 'Arial', sans-serif; margin: 40px; background-color: #f8f9fa; }");
        html.append(".container { background-color: white; padding: 30px; border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }");
        html.append("h1 { color: #333; border-bottom: 3px solid #007bff; padding-bottom: 15px; margin-bottom: 30px; }");
        html.append(".content { font-size: 16px; line-height: 1.8; }");
        html.append(".footer { margin-top: 40px; text-align: right; color: #666; font-size: 14px; border-top: 1px solid #eee; padding-top: 20px; }");
        html.append("</style></head><body>");
        
        html.append("<div class='container'>");
        html.append("<h1>").append(title).append("</h1>");
        html.append("<div class='content'>").append(content).append("</div>");
        html.append("<div class='footer'>生成日: ").append(LocalDate.now().format(DATE_FORMATTER)).append("</div>");
        html.append("</div>");
        html.append("</body></html>");
        
        return html.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }
    
    /**
     * 実用的なPDF生成（HTML to PDF変換方式）
     */
    private byte[] generateSimplePDFContent(String title, String content) {
        // 実際のPDF生成は複雑なため、ここでは暫定的にHTMLを返す
        // 本格的な実装にはiTextやPDFBoxなどのライブラリが必要
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head>");
        html.append("<meta charset='UTF-8'>");
        html.append("<title>").append(title).append("</title>");
        html.append("<style>");
        html.append("@media print { @page { margin: 1cm; } }");
        html.append("body { font-family: 'MS Gothic', Arial, sans-serif; margin: 20px; }");
        html.append("h1 { color: #333; text-align: center; }");
        html.append("</style>");
        html.append("</head><body>");
        html.append("<h1>").append(title).append("</h1>");
        html.append("<div>").append(content).append("</div>");
        html.append("<script>window.print();</script>");
        html.append("</body></html>");
        
        return html.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }
    
    
    // ===========================================
    // ユーティリティメソッド
    // ===========================================
    
    private String getJapaneseDayOfWeek(LocalDate date) {
        if (date == null) return "";
        switch (date.getDayOfWeek()) {
            case MONDAY: return "月";
            case TUESDAY: return "火";
            case WEDNESDAY: return "水";
            case THURSDAY: return "木";
            case FRIDAY: return "金";
            case SATURDAY: return "土";
            case SUNDAY: return "日";
            default: return "";
        }
    }
    
    /**
     * 印刷最適化CSS生成
     */
    private String generatePrintOptimizedCSS() {
        return "<style>" +
            "@page { size: A4; margin: 15mm 20mm; }" +
            "body { font-family: 'MS Gothic', 'Arial', sans-serif; font-size: 12px; line-height: 1.5; margin: 0 auto; color: #000; background-color: #fff; }" +
            ".report-container { max-width: 680px; margin: 0 auto; padding: 15px; background-color: #fff; }" +
            ".header { text-align: center; margin-bottom: 20px; border-bottom: 2px solid #000; padding-bottom: 12px; }" +
            ".header h1 { font-size: 20px; margin: 0; font-weight: bold; letter-spacing: 1px; }" +
            ".info-section { margin-bottom: 15px; }" +
            ".info-table { width: 100%; border-collapse: collapse; margin-bottom: 12px; }" +
            ".info-table td, .info-table th { border: 1px solid #000; padding: 8px 10px; text-align: center; font-size: 12px; line-height: 1.4; }" +
            ".info-table th { background-color: #f0f0f0; font-weight: bold; }" +
            ".period-section { text-align: center; margin-bottom: 15px; padding: 10px; background-color: #f9f9f9; border: 1px solid #ccc; font-size: 12px; }" +
            ".summary-table { width: 100%; border-collapse: collapse; margin-bottom: 15px; }" +
            ".summary-table td, .summary-table th { border: 1px solid #000; padding: 8px 6px; text-align: center; font-size: 12px; line-height: 1.4; }" +
            ".summary-table th { background-color: #e9e9e9; font-weight: bold; }" +
            ".records-table { width: 100%; border-collapse: collapse; margin-bottom: 15px; font-size: 12px; }" +
            ".records-table td, .records-table th { border: 1px solid #000; padding: 6px 4px; text-align: center; line-height: 1.3; }" +
            ".records-table th { background-color: #e9e9e9; font-weight: bold; font-size: 12px; }" +
            ".records-table th:nth-child(1) { width: 8%; }" +
            ".records-table th:nth-child(2) { width: 8%; }" +
            ".records-table th:nth-child(3) { width: 12%; }" +
            ".records-table th:nth-child(4) { width: 12%; }" +
            ".records-table th:nth-child(5) { width: 12%; }" +
            ".records-table th:nth-child(6) { width: 12%; }" +
            ".records-table th:nth-child(7) { width: 12%; }" +
            ".records-table th:nth-child(8) { width: 24%; }" +
            ".compliance-section { margin-top: 20px; }" +
            ".compliance-item { margin-bottom: 8px; font-size: 12px; padding: 6px 10px; line-height: 1.4; }" +
            ".late { background-color: #fff3cd; }" +
            ".absent { background-color: #f8d7da; }" +
            ".overtime { background-color: #d1ecf1; }" +
            ".warning { color: #721c24; background-color: #f8d7da; }" +
            ".success { color: #155724; background-color: #d4edda; }" +
            "h3 { font-size: 14px; margin: 12px 0; text-align: center; font-weight: bold; }" +
            "@media print { " +
            "  body { -webkit-print-color-adjust: exact; margin: 0; }" +
            "  .report-container { max-width: none; width: 100%; margin: 0; padding: 10px; }" +
            "  @page { margin: 15mm 20mm; }" +
            "}" +
            "@media screen { " +
            "  body { background-color: #f5f5f5; padding: 20px; }" +
            "  .report-container { box-shadow: 0 0 10px rgba(0,0,0,0.1); }" +
            "}" +
            "</style>";
    }
    
    /**
     * 基本情報セクション生成
     */
    private String generateBasicInfoSection(PersonalReportBean report) {
        return "<div class='info-section'>" +
            "<table class='info-table'>" +
            "<tr>" +
            "<th>従業員番号</th><th>氏名</th><th>部署</th><th>役職</th>" +
            "</tr>" +
            "<tr>" +
            "<td>" + report.getEmpno() + "</td>" +
            "<td>" + report.getEmpName() + "</td>" +
            "<td>" + report.getDeptName() + "</td>" +
            "<td>" + report.getPostName() + "</td>" +
            "</tr>" +
            "</table>" +
            "</div>";
    }
    
    /**
     * 期間セクション生成
     */
    private String generatePeriodSection(PersonalReportBean report) {
        return "<div class='period-section'>" +
            "<strong>対象期間: " + report.getTargetMonth() + "</strong>" +
            "<span style='float: right;'>出力日: " + LocalDate.now().format(DATE_FORMATTER) + "</span>" +
            "<div style='clear: both;'></div>" +
            "</div>";
    }
    
    /**
     * 統計セクション生成
     */
    private String generateSummarySection(PersonalReportBean report) {
        MonthlySummaryBean summary = report.getSummary();
        return "<div class='info-section'>" +
            "<h3 style='margin: 10px 0; font-size: 14px; text-align: center;'>月度統計</h3>" +
            "<table class='summary-table'>" +
            "<tr>" +
            "<th>出勤日数/出社日</th><th>総実働時間</th><th>総残業時間</th><th>総休憩時間</th>" +
            "</tr>" +
            "<tr>" +
            "<td>" + summary.getAttendanceRateString() + "</td>" +
            "<td>" + summary.getTotalWorkingHoursString() + "</td>" +
            "<td>" + summary.getTotalOvertimeHoursString() + "</td>" +
            "<td>" + summary.getTotalBreakHoursString() + "</td>" +
            "</tr>" +
            "</table>" +
            "</div>";
    }
    
    /**
     * 詳細記録テーブル生成
     */
    private String generateDetailedRecordsTable(PersonalReportBean report) {
        StringBuilder table = new StringBuilder();
        table.append("<div class='info-section'>");
        table.append("<h3 style='margin: 10px 0; font-size: 14px; text-align: center;'>詳細記録</h3>");
        table.append("<table class='records-table'>");
        table.append("<tr>");
        table.append("<th>日期</th><th>曜日</th><th>出勤時刻</th><th>退勤時刻</th>");
        table.append("<th>休憩時間</th><th>実働時間</th><th>残業時間</th><th>深夜残業時間</th><th>備考</th><th>プロジェクト時間</th>");
        table.append("</tr>");
        
        for (KintaiRecBean record : report.getRecords()) {
            String rowClass = "";
            String remarks = "";
            
            // 異常ケースの判定とクラス設定
            if (record.getClockIn() == null && record.getClockOut() == null) {
                rowClass = "absent";
                remarks = "欠勤";
//            } else if (record.getClockIn() != null && 
//                      record.getClockIn().toLocalTime().isAfter(java.time.LocalTime.of(9, 0))) {
//                rowClass = "late";
//                remarks = "遅刻";
            } else if (record.getOvertimeMinutes() > 4 * 60) {
                rowClass = "overtime";
                remarks = "長時間残業";
            } else if (record.getOvertimeMinutes() > 2 * 60) {
                rowClass = "overtime";
                remarks = "残業";
            }
            
            table.append("<tr class='").append(rowClass).append("'>");
            table.append("<td>").append(record.getKintaiDate() != null ? 
                String.format("%02d", record.getKintaiDate().getDayOfMonth()) : "").append("</td>");
            table.append("<td>").append(getJapaneseDayOfWeek(record.getKintaiDate())).append("</td>");
            table.append("<td>").append(record.getClockIn() != null ? 
                record.getClockIn().toString().substring(0, 5) : "─").append("</td>");
            table.append("<td>").append(record.getClockOut() != null ? 
                record.getClockOut().toString().substring(0, 5) : "─").append("</td>");
            table.append("<td>").append(record.getClockIn() != null ? 
                record.getTotalBreakTimeFormatted() : "─").append("</td>");
            table.append("<td>").append(record.getClockIn() != null ? 
                record.getActualWorkTimeFormatted() : "─").append("</td>");
            table.append("<td>").append(record.getClockIn() != null ? 
                record.getOvertimeFormatted() : "─").append("</td>");
            table.append("<td>").append(remarks).append("</td>");
            table.append("</tr>");
        }
        
        table.append("</table>");
        table.append("</div>");
        return table.toString();
    }
    
    /**
     * 法定時間チェックセクション生成
     */
    private String generateComplianceSection(PersonalReportBean report) {
        StringBuilder section = new StringBuilder();
        section.append("<div class='compliance-section'>");
        section.append("<h3 style='margin: 10px 0; font-size: 14px; text-align: center;'>法定時間チェック</h3>");
        
        if (report.getComplianceWarnings().isEmpty()) {
            section.append("<div class='compliance-item success'>");
            section.append("✅ 法定労働時間内で適正な勤務状況です");
            section.append("</div>");
        } else {
            for (String warning : report.getComplianceWarnings()) {
                String itemClass = warning.contains("⚠️") ? "warning" : "compliance-item";
                section.append("<div class='compliance-item ").append(itemClass).append("'>");
                section.append(warning);
                section.append("</div>");
            }
        }
        
        section.append("</div>");
        return section.toString();
    }
    
    // ===========================================
    // Excel生成メソッド群（簡易CSV形式）
    // ===========================================
    
    /**
     * 個人別月次報告Excel生成（Excel互換CSV形式）
     */
    private byte[] generateIndividualExcel(MonthlySummaryBean summary, List<KintaiRecBean> records) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            
            // Excel用のBOM（UTF-8 BOM）
            baos.write(new byte[]{(byte)0xEF, (byte)0xBB, (byte)0xBF});
            
            // Excel XMLスプレッドシート形式の基本構造
            StringBuilder xml = new StringBuilder();
            xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            xml.append("<?mso-application progid=\"Excel.Sheet\"?>\n");
            xml.append("<Workbook xmlns=\"urn:schemas-microsoft-com:office:spreadsheet\"\n");
            xml.append(" xmlns:o=\"urn:schemas-microsoft-com:office:office\"\n");
            xml.append(" xmlns:x=\"urn:schemas-microsoft-com:office:excel\"\n");
            xml.append(" xmlns:ss=\"urn:schemas-microsoft-com:office:spreadsheet\"\n");
            xml.append(" xmlns:html=\"http://www.w3.org/TR/REC-html40\">\n");
            
            // スタイル定義
            xml.append("<Styles>\n");
            xml.append("<Style ss:ID=\"Header\">\n");
            xml.append("<Font ss:Bold=\"1\" ss:Size=\"12\"/>\n");
            xml.append("<Alignment ss:Horizontal=\"Center\"/>\n");
            xml.append("</Style>\n");
            xml.append("<Style ss:ID=\"Summary\">\n");
            xml.append("<Font ss:Bold=\"1\" ss:Size=\"11\"/>\n");
            xml.append("<Interior ss:Color=\"#F0F8FF\" ss:Pattern=\"Solid\"/>\n");
            xml.append("</Style>\n");
            xml.append("</Styles>\n");
            
            // ワークシート開始
            xml.append("<Worksheet ss:Name=\"個人別月次勤怠報告書\">\n");
            xml.append("<Table>\n");
            xml.append("<Column ss:Index=\"1\" ss:Width=\"111\"/>"); // A列 ≒ 14.75
            xml.append("<Column ss:Index=\"3\" ss:Width=\"74\"/>");  // C列 ≒ 9.83
            xml.append("<Column ss:Index=\"8\" ss:Width=\"74\"/>");  // H列 ≒ 9.83
            xml.append("<Column ss:Index=\"9\" ss:Width=\"74\"/>");  // I列 ≒ 9.83
            xml.append("<Column ss:Index=\"10\" ss:Width=\"111\"/>"); // J列 ≒ 14.75
            // タイトル行
            xml.append("<Row>\n");
            xml.append("<Cell ss:MergeAcross=\"7\" ss:StyleID=\"Header\"><Data ss:Type=\"String\">個人別月次勤怠報告書</Data></Cell>\n");
            xml.append("</Row>\n");
            
            // 基本情報
            if (!records.isEmpty()) {
                KintaiRecBean firstRecord = records.get(0);
                xml.append("<Row>\n");
                xml.append("<Cell><Data ss:Type=\"String\">従業員番号</Data></Cell>\n");
                xml.append("<Cell><Data ss:Type=\"String\">").append(firstRecord.getEmpno() != null ? firstRecord.getEmpno() : "").append("</Data></Cell>\n");
                xml.append("<Cell><Data ss:Type=\"String\">従業員名</Data></Cell>\n");
                xml.append("<Cell><Data ss:Type=\"String\">").append(firstRecord.getEmpName() != null ? firstRecord.getEmpName() : "").append("</Data></Cell>\n");
                xml.append("</Row>\n");
                
                xml.append("<Row>\n");
                xml.append("<Cell><Data ss:Type=\"String\">部署</Data></Cell>\n");
                xml.append("<Cell><Data ss:Type=\"String\">").append(firstRecord.getDeptName() != null ? firstRecord.getDeptName() : "").append("</Data></Cell>\n");
                xml.append("<Cell><Data ss:Type=\"String\">役職</Data></Cell>\n");
                xml.append("<Cell><Data ss:Type=\"String\">").append(firstRecord.getPostName() != null ? firstRecord.getPostName() : "").append("</Data></Cell>\n");
                xml.append("</Row>\n");
            }
            
            xml.append("<Row>\n");
            xml.append("<Cell><Data ss:Type=\"String\">対象月</Data></Cell>\n");
            xml.append("<Cell><Data ss:Type=\"String\">").append(summary.getTargetMonth()).append("</Data></Cell>\n");
            xml.append("<Cell><Data ss:Type=\"String\">生成日</Data></Cell>\n");
            xml.append("<Cell><Data ss:Type=\"String\">").append(LocalDate.now().format(DATE_FORMATTER)).append("</Data></Cell>\n");
            xml.append("</Row>\n");
            
            // 空行
            xml.append("<Row></Row>\n");
            
            // 詳細記録セクション
            xml.append("<Row>\n");
            xml.append("<Cell ss:MergeAcross=\"7\" ss:StyleID=\"Summary\"><Data ss:Type=\"String\">詳細勤怠記録</Data></Cell>\n");
            xml.append("</Row>\n");
            
            // 詳細データヘッダー
            xml.append("<Row>\n");
            xml.append("<Cell ss:StyleID=\"Header\"><Data ss:Type=\"String\">日付</Data></Cell>\n");
            xml.append("<Cell ss:StyleID=\"Header\"><Data ss:Type=\"String\">曜日</Data></Cell>\n");
            xml.append("<Cell ss:StyleID=\"Header\"><Data ss:Type=\"String\">出勤時刻</Data></Cell>\n");
            xml.append("<Cell ss:StyleID=\"Header\"><Data ss:Type=\"String\">退勤時刻</Data></Cell>\n");
            xml.append("<Cell ss:StyleID=\"Header\"><Data ss:Type=\"String\">休憩時間</Data></Cell>\n");
            xml.append("<Cell ss:StyleID=\"Header\"><Data ss:Type=\"String\">実働時間</Data></Cell>\n");
            xml.append("<Cell ss:StyleID=\"Header\"><Data ss:Type=\"String\">残業時間</Data></Cell>\n");
            xml.append("<Cell ss:StyleID=\"Header\"><Data ss:Type=\"String\">深夜残業時間</Data></Cell>\n");
            xml.append("<Cell ss:StyleID=\"Header\"><Data ss:Type=\"String\">備考</Data></Cell>\n");
            xml.append("<Cell ss:StyleID=\"Header\"><Data ss:Type=\"String\">プロジェクト時間</Data></Cell>\n");
            xml.append("</Row>\n");
            
            // 全ての詳細データを出力
            for (KintaiRecBean record : records) {
                String remarks = "";
                if (record.getClockIn() == null && record.getClockOut() == null) {
                    remarks = "欠勤";
//                } else if (record.getClockIn() != null && 
//                          record.getClockIn().toLocalTime().isAfter(java.time.LocalTime.of(9, 0))) {
//                    remarks = "遅刻";
                } else if (record.getOvertimeMinutes() > 4 * 60) {
                    remarks = "長時間残業";
                } else if (record.getOvertimeMinutes() > 2 * 60) {
                    remarks = "残業";
                }
                
                xml.append("<Row>\n");
                xml.append("<Cell><Data ss:Type=\"String\">").append(record.getKintaiDate() != null ? record.getKintaiDate().toString() : "").append("</Data></Cell>\n");
                xml.append("<Cell><Data ss:Type=\"String\">").append(getJapaneseDayOfWeek(record.getKintaiDate())).append("</Data></Cell>\n");
                xml.append("<Cell><Data ss:Type=\"String\">").append(record.getClockIn() != null ? record.getClockIn().toString().substring(0, 5) : "---").append("</Data></Cell>\n");
                xml.append("<Cell><Data ss:Type=\"String\">").append(record.getClockOut() != null ? record.getClockOut().toString().substring(0, 5) : "---").append("</Data></Cell>\n");
                xml.append("<Cell><Data ss:Type=\"String\">").append(record.getTotalBreakTimeFormatted()).append("</Data></Cell>\n");
                xml.append("<Cell><Data ss:Type=\"String\">").append(record.getActualWorkTimeFormatted()).append("</Data></Cell>\n");
                xml.append("<Cell><Data ss:Type=\"String\">").append(record.getOvertimeFormatted()).append("</Data></Cell>\n");
                xml.append("<Cell><Data ss:Type=\"String\">").append(record.getNightovertimeFormatted()).append("</Data></Cell>\n");
                xml.append("<Cell><Data ss:Type=\"String\">").append(remarks).append("</Data></Cell>\n");
                xml.append("<Cell><Data ss:Type=\"String\">").append(record.getNightovertimeFormatted()).append("</Data></Cell>\n");	//プロジェクト時間を追加したいがgetメソッド未作成
                xml.append("</Row>\n");
            }
            
            // 合計行
            long totalWorkMinutes = records.stream().mapToLong(KintaiRecBean::getActualWorkMinutes).sum();
            long totalOvertimeMinutes = records.stream().mapToLong(KintaiRecBean::getOvertimeMinutes).sum();
            long totalBreakMinutes = records.stream().mapToLong(KintaiRecBean::getTotalBreakMinutes).sum();
            
            xml.append("<Row>\n");
            xml.append("<Cell ss:StyleID=\"Summary\"><Data ss:Type=\"String\">合計</Data></Cell>\n");
            xml.append("<Cell></Cell>\n");
            xml.append("<Cell></Cell>\n");
            xml.append("<Cell></Cell>\n");
            xml.append("<Cell ss:StyleID=\"Summary\"><Data ss:Type=\"String\">").append(String.format("%.1f時間", totalBreakMinutes / 60.0)).append("</Data></Cell>\n");
            xml.append("<Cell ss:StyleID=\"Summary\"><Data ss:Type=\"String\">").append(String.format("%.1f時間", totalWorkMinutes / 60.0)).append("</Data></Cell>\n");
            xml.append("<Cell ss:StyleID=\"Summary\"><Data ss:Type=\"String\">").append(String.format("%.1f時間", totalOvertimeMinutes / 60.0)).append("</Data></Cell>\n");
            xml.append("<Cell></Cell>\n");
            xml.append("</Row>\n");
            
            // 空行
            xml.append("<Row></Row>\n");
            
            // 月次統計サマリー（詳細記録と合計の後に移動）
            xml.append("<Row>\n");
            xml.append("<Cell ss:MergeAcross=\"7\" ss:StyleID=\"Summary\"><Data ss:Type=\"String\">月次統計サマリー</Data></Cell>\n");
            xml.append("</Row>\n");
            
            xml.append("<Row>\n");
            xml.append("<Cell><Data ss:Type=\"String\">出勤日数/出社日</Data></Cell>\n");
            xml.append("<Cell><Data ss:Type=\"String\">").append(summary.getAttendanceRateString()).append("</Data></Cell>\n");
            xml.append("<Cell><Data ss:Type=\"String\">総稼働時間</Data></Cell>\n");
            xml.append("<Cell><Data ss:Type=\"String\">").append(summary.getTotalWorkingHoursString()).append("</Data></Cell>\n");
            xml.append("</Row>\n");
            
            xml.append("<Row>\n");
            xml.append("<Cell><Data ss:Type=\"String\">総残業時間</Data></Cell>\n");
            xml.append("<Cell><Data ss:Type=\"String\">").append(summary.getTotalOvertimeHoursString()).append("</Data></Cell>\n");
            xml.append("<Cell><Data ss:Type=\"String\">総休憩時間</Data></Cell>\n");
            xml.append("<Cell><Data ss:Type=\"String\">").append(summary.getTotalBreakHoursString()).append("</Data></Cell>\n");
            xml.append("</Row>\n");
            
            // ワークシート終了
            xml.append("</Table>\n");
            xml.append("</Worksheet>\n");
            xml.append("</Workbook>\n");
            
            baos.write(xml.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return baos.toByteArray();
            
        } catch (IOException e) {
            // フォールバック：シンプルなCSV形式
            return generateSimpleExcelCSV(summary, records);
        }
    }
    
    /**
     * フォールバック用シンプルCSV生成
     */
    private byte[] generateSimpleExcelCSV(MonthlySummaryBean summary, List<KintaiRecBean> records) {
        StringBuilder csv = new StringBuilder();
        
        csv.append("個人別月次勤怠報告書\n");
        csv.append("対象月,").append(summary.getTargetMonth()).append("\n");
        csv.append("生成日,").append(LocalDate.now().format(DATE_FORMATTER)).append("\n\n");
        
        // 詳細記録ヘッダー
        csv.append("日付,曜日,出勤時刻,退勤時刻,休憩時間,実働時間,残業時間,深夜残業時間,備考,プロジェクト時間\n");
        
        // 詳細記録データ
        for (KintaiRecBean record : records) {
            String remarks = "";
            if (record.getClockIn() == null && record.getClockOut() == null) {
                remarks = "欠勤";
//            } else if (record.getClockIn() != null && 
//                      record.getClockIn().toLocalTime().isAfter(java.time.LocalTime.of(9, 0))) {
//                remarks = "遅刻";
            } else if (record.getOvertimeMinutes() > 4 * 60) {
                remarks = "長時間残業";
            } else if (record.getOvertimeMinutes() > 2 * 60) {
                remarks = "残業";
            }
            
            csv.append("\"").append(record.getKintaiDate()).append("\",");
            csv.append("\"").append(getJapaneseDayOfWeek(record.getKintaiDate())).append("\",");
            csv.append("\"").append(record.getClockIn() != null ? record.getClockIn().toString().substring(0, 5) : "").append("\",");
            csv.append("\"").append(record.getClockOut() != null ? record.getClockOut().toString().substring(0, 5) : "").append("\",");
            csv.append("\"").append(record.getTotalBreakTimeFormatted()).append("\",");
            csv.append("\"").append(record.getActualWorkTimeFormatted()).append("\",");
            csv.append("\"").append(record.getOvertimeFormatted()).append("\",");
            csv.append("\"").append(remarks).append("\"\n");
            csv.append("\"").append(remarks).append("\"\n");	//プロジェクト時間を追加したいがgetメソッド未作成
        }
        
        // 月次統計サマリー（詳細記録の後に移動）
        csv.append("\n月次統計サマリー\n");
        csv.append("出勤日数/出社日,").append(summary.getAttendanceRateString()).append("\n");
        csv.append("総稼働時間,").append(summary.getTotalWorkingHoursString()).append("\n");
        csv.append("総残業時間,").append(summary.getTotalOvertimeHoursString()).append("\n");
        csv.append("総休憩時間,").append(summary.getTotalBreakHoursString()).append("\n");
        
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            // BOM + CSV内容
            baos.write(new byte[]{(byte)0xEF, (byte)0xBB, (byte)0xBF});
            baos.write(csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return baos.toByteArray();
        } catch (IOException e) {
            return csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        }
    }
    
    /**
     * 部署別集計報告Excel生成（Excel互換XML形式）
     */
    private byte[] generateDepartmentExcel(List<KintaiRecBean> records, String deptNo) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            
            // Excel用のBOM（UTF-8 BOM）
            baos.write(new byte[]{(byte)0xEF, (byte)0xBB, (byte)0xBF});
            
            // Excel XMLスプレッドシート形式の基本構造
            StringBuilder xml = new StringBuilder();
            xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            xml.append("<?mso-application progid=\"Excel.Sheet\"?>\n");
            xml.append("<Workbook xmlns=\"urn:schemas-microsoft-com:office:spreadsheet\"\n");
            xml.append(" xmlns:o=\"urn:schemas-microsoft-com:office:office\"\n");
            xml.append(" xmlns:x=\"urn:schemas-microsoft-com:office:excel\"\n");
            xml.append(" xmlns:ss=\"urn:schemas-microsoft-com:office:spreadsheet\"\n");
            xml.append(" xmlns:html=\"http://www.w3.org/TR/REC-html40\">\n");
            
            // スタイル定義
            xml.append("<Styles>\n");
            xml.append("<Style ss:ID=\"Header\">\n");
            xml.append("<Font ss:Bold=\"1\" ss:Size=\"12\"/>\n");
            xml.append("<Alignment ss:Horizontal=\"Center\"/>\n");
            xml.append("</Style>\n");
            xml.append("<Style ss:ID=\"Summary\">\n");
            xml.append("<Font ss:Bold=\"1\" ss:Size=\"11\"/>\n");
            xml.append("<Interior ss:Color=\"#F0F8FF\" ss:Pattern=\"Solid\"/>\n");
            xml.append("</Style>\n");
            xml.append("</Styles>\n");
            
            // ワークシート開始
            xml.append("<Worksheet ss:Name=\"部署別集計勤怠報告書\">\n");
            xml.append("<Table>\n");
            xml.append("<Column ss:Index=\"1\" ss:Width=\"111\"/>"); // A列 ≒ 14.75
            xml.append("<Column ss:Index=\"3\" ss:Width=\"74\"/>");  // C列 ≒ 9.83
            xml.append("<Column ss:Index=\"4\" ss:Width=\"108\"/>"); // D列 ≒ 14.42
            xml.append("<Column ss:Index=\"5\" ss:Width=\"85\"/>");  // E列 ≒ 11.33
            xml.append("<Column ss:Index=\"6\" ss:Width=\"101\"/>"); // F列 ≒ 13.5
            xml.append("<Column ss:Index=\"10\" ss:Width=\"101\"/>"); // I列 ≒ 13.5
            xml.append("<Column ss:Index=\"11\" ss:Width=\"111\"/>"); // I列 ≒ 13.5

            // タイトル行
            xml.append("<Row>\n");
            xml.append("<Cell ss:MergeAcross=\"8\" ss:StyleID=\"Header\"><Data ss:Type=\"String\">部署別集計勤怠報告書</Data></Cell>\n");
            xml.append("</Row>\n");
            
            // 基本情報
            xml.append("<Row>\n");
            xml.append("<Cell><Data ss:Type=\"String\">対象部署</Data></Cell>\n");
            xml.append("<Cell><Data ss:Type=\"String\">").append(deptNo != null ? deptNo : "全部署").append("</Data></Cell>\n");
            xml.append("<Cell><Data ss:Type=\"String\">生成日</Data></Cell>\n");
            xml.append("<Cell><Data ss:Type=\"String\">").append(LocalDate.now().format(DATE_FORMATTER)).append("</Data></Cell>\n");
            xml.append("</Row>\n");
            
            xml.append("<Row>\n");
            xml.append("<Cell><Data ss:Type=\"String\">総記録数</Data></Cell>\n");
            xml.append("<Cell><Data ss:Type=\"String\">").append(records.size()).append("件</Data></Cell>\n");
            xml.append("<Cell><Data ss:Type=\"String\">対象従業員数</Data></Cell>\n");
            xml.append("<Cell><Data ss:Type=\"String\">").append(records.stream().map(KintaiRecBean::getEmpno).distinct().count()).append("名</Data></Cell>\n");
            xml.append("</Row>\n");
            
            // 空行
            xml.append("<Row></Row>\n");
            
            // 詳細記録セクション
            xml.append("<Row>\n");
            xml.append("<Cell ss:MergeAcross=\"8\" ss:StyleID=\"Summary\"><Data ss:Type=\"String\">詳細勤怠記録</Data></Cell>\n");
            xml.append("</Row>\n");
            
            // 詳細データヘッダー
            xml.append("<Row>\n");
            xml.append("<Cell ss:StyleID=\"Header\"><Data ss:Type=\"String\">日付</Data></Cell>\n");
            xml.append("<Cell ss:StyleID=\"Header\"><Data ss:Type=\"String\">曜日</Data></Cell>\n");
            xml.append("<Cell ss:StyleID=\"Header\"><Data ss:Type=\"String\">従業員番号</Data></Cell>\n");
            xml.append("<Cell ss:StyleID=\"Header\"><Data ss:Type=\"String\">氏名</Data></Cell>\n");
            xml.append("<Cell ss:StyleID=\"Header\"><Data ss:Type=\"String\">部署</Data></Cell>\n");
            xml.append("<Cell ss:StyleID=\"Header\"><Data ss:Type=\"String\">出勤時刻</Data></Cell>\n");
            xml.append("<Cell ss:StyleID=\"Header\"><Data ss:Type=\"String\">退勤時刻</Data></Cell>\n");
            xml.append("<Cell ss:StyleID=\"Header\"><Data ss:Type=\"String\">実働時間</Data></Cell>\n");
            xml.append("<Cell ss:StyleID=\"Header\"><Data ss:Type=\"String\">残業時間</Data></Cell>\n");
            xml.append("<Cell ss:StyleID=\"Header\"><Data ss:Type=\"String\">深夜残業時間</Data></Cell>\n");
            xml.append("<Cell ss:StyleID=\"Header\"><Data ss:Type=\"String\">プロジェクト時間</Data></Cell>\n");
            xml.append("</Row>\n");
            
            // 全ての詳細データを出力
            for (KintaiRecBean record : records) {
                // 曜日の日本語表示
                String dayOfWeek = "";
                if (record.getKintaiDate() != null) {
                    switch (record.getKintaiDate().getDayOfWeek()) {
                        case MONDAY: dayOfWeek = "月"; break;
                        case TUESDAY: dayOfWeek = "火"; break;
                        case WEDNESDAY: dayOfWeek = "水"; break;
                        case THURSDAY: dayOfWeek = "木"; break;
                        case FRIDAY: dayOfWeek = "金"; break;
                        case SATURDAY: dayOfWeek = "土"; break;
                        case SUNDAY: dayOfWeek = "日"; break;
                    }
                }
                
                xml.append("<Row>\n");
                xml.append("<Cell><Data ss:Type=\"String\">").append(record.getKintaiDate() != null ? record.getKintaiDate().toString() : "").append("</Data></Cell>\n");
                xml.append("<Cell><Data ss:Type=\"String\">").append(dayOfWeek).append("</Data></Cell>\n");
                xml.append("<Cell><Data ss:Type=\"String\">").append(record.getEmpno() != null ? record.getEmpno() : "").append("</Data></Cell>\n");
                xml.append("<Cell><Data ss:Type=\"String\">").append(record.getEmpName() != null ? record.getEmpName() : "").append("</Data></Cell>\n");
                xml.append("<Cell><Data ss:Type=\"String\">").append(record.getDeptName() != null ? record.getDeptName() : "").append("</Data></Cell>\n");
                xml.append("<Cell><Data ss:Type=\"String\">").append(record.getClockIn() != null ? record.getClockIn().toString().substring(0, 5) : "---").append("</Data></Cell>\n");
                xml.append("<Cell><Data ss:Type=\"String\">").append(record.getClockOut() != null ? record.getClockOut().toString().substring(0, 5) : "---").append("</Data></Cell>\n");
                xml.append("<Cell><Data ss:Type=\"String\">").append(record.getActualWorkTimeFormatted()).append("</Data></Cell>\n");
                xml.append("<Cell><Data ss:Type=\"String\">").append(record.getOvertimeFormatted()).append("</Data></Cell>\n");
                xml.append("<Cell><Data ss:Type=\"String\">").append(record.getNightovertimeFormatted()).append("</Data></Cell>\n");
                xml.append("<Cell><Data ss:Type=\"String\">").append(record.getNightovertimeFormatted()).append("</Data></Cell>\n");	//プロジェクト時間を入れたいがgetメソッド未作成
                xml.append("</Row>\n");
            }
            
            // 合計行
            long grandTotalWorkMinutes = records.stream().mapToLong(KintaiRecBean::getActualWorkMinutes).sum();
            long grandTotalOvertimeMinutes = records.stream().mapToLong(KintaiRecBean::getOvertimeMinutes).sum();
            
            xml.append("<Row>\n");
            xml.append("<Cell ss:StyleID=\"Summary\"><Data ss:Type=\"String\">合計</Data></Cell>\n");
            xml.append("<Cell></Cell>\n");
            xml.append("<Cell></Cell>\n");
            xml.append("<Cell></Cell>\n");
            xml.append("<Cell></Cell>\n");
            xml.append("<Cell></Cell>\n");
            xml.append("<Cell></Cell>\n");
            xml.append("<Cell ss:StyleID=\"Summary\"><Data ss:Type=\"String\">").append(String.format("%.1f時間", grandTotalWorkMinutes / 60.0)).append("</Data></Cell>\n");
            xml.append("<Cell ss:StyleID=\"Summary\"><Data ss:Type=\"String\">").append(String.format("%.1f時間", grandTotalOvertimeMinutes / 60.0)).append("</Data></Cell>\n");
            xml.append("</Row>\n");
            
            // 空行
            xml.append("<Row></Row>\n");
            
            // 部署別集計サマリー
            Map<String, List<KintaiRecBean>> deptGroups = records.stream()
                .collect(Collectors.groupingBy(r -> r.getDeptName() != null ? r.getDeptName() : "未設定"));
            
            xml.append("<Row>\n");
            xml.append("<Cell ss:MergeAcross=\"8\" ss:StyleID=\"Summary\"><Data ss:Type=\"String\">部署別統計サマリー</Data></Cell>\n");
            xml.append("</Row>\n");
            
            // サマリーヘッダー
            xml.append("<Row>\n");
            xml.append("<Cell ss:StyleID=\"Header\"><Data ss:Type=\"String\">部署名</Data></Cell>\n");
            xml.append("<Cell ss:StyleID=\"Header\"><Data ss:Type=\"String\">従業員数</Data></Cell>\n");
            xml.append("<Cell ss:StyleID=\"Header\"><Data ss:Type=\"String\">記録数</Data></Cell>\n");
            xml.append("<Cell ss:StyleID=\"Header\"><Data ss:Type=\"String\">総労働時間</Data></Cell>\n");
            xml.append("<Cell ss:StyleID=\"Header\"><Data ss:Type=\"String\">総残業時間</Data></Cell>\n");
            xml.append("<Cell ss:StyleID=\"Header\"><Data ss:Type=\"String\">平均残業時間</Data></Cell>\n");
            xml.append("</Row>\n");
            
            // サマリーデータ
            for (Map.Entry<String, List<KintaiRecBean>> entry : deptGroups.entrySet()) {
                String deptName = entry.getKey();
                List<KintaiRecBean> deptRecords = entry.getValue();
                
                long totalOvertimeMinutes = deptRecords.stream()
                    .mapToLong(KintaiRecBean::getOvertimeMinutes)
                    .sum();
                    
                long totalWorkMinutes = deptRecords.stream()
                    .mapToLong(KintaiRecBean::getActualWorkMinutes)
                    .sum();
                    
                long empCount = deptRecords.stream().map(KintaiRecBean::getEmpno).distinct().count();
                
                xml.append("<Row>\n");
                xml.append("<Cell><Data ss:Type=\"String\">").append(deptName).append("</Data></Cell>\n");
                xml.append("<Cell><Data ss:Type=\"Number\">").append(empCount).append("</Data></Cell>\n");
                xml.append("<Cell><Data ss:Type=\"Number\">").append(deptRecords.size()).append("</Data></Cell>\n");
                xml.append("<Cell><Data ss:Type=\"String\">").append(String.format("%.1f時間", totalWorkMinutes / 60.0)).append("</Data></Cell>\n");
                xml.append("<Cell><Data ss:Type=\"String\">").append(String.format("%.1f時間", totalOvertimeMinutes / 60.0)).append("</Data></Cell>\n");
                xml.append("<Cell><Data ss:Type=\"String\">").append(String.format("%.1f時間", empCount > 0 ? (totalOvertimeMinutes / 60.0) / empCount : 0)).append("</Data></Cell>\n");
                xml.append("</Row>\n");
            }
            
            // ワークシート終了
            xml.append("</Table>\n");
            xml.append("</Worksheet>\n");
            xml.append("</Workbook>\n");
            
            baos.write(xml.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return baos.toByteArray();
            
        } catch (IOException e) {
            // フォールバック：シンプルなCSV形式
            return generateDepartmentCSV(records, deptNo);
        }
    }
    
    /**
     * 残業時間分析報告Excel生成（CSV形式で代替）
     */
    private byte[] generateOvertimeExcel(List<KintaiRecBean> records) {
        return generateOvertimeCSV(records); // CSVを代用
    }
    
    /**
     * 全社員勤怠一覧Excel生成（CSV形式で代替）
     */
    private byte[] generateAllEmployeeExcel(List<KintaiRecBean> records) {
        return generateAllEmployeeCSV(records); // CSVを代用
    }
    
    /**
     * 期間指定レポートExcel生成（CSV形式で代替）
     */
    private byte[] generatePeriodExcel(List<KintaiRecBean> records, LocalDate startDate, LocalDate endDate) {
        return generatePeriodCSV(records, startDate, endDate); // CSVを代用
    }
    
    /**
     * 法定チェック報告書Excel生成（CSV形式で代替）
     */
    private byte[] generateComplianceExcel(List<KintaiRecBean> records) {
        return generateComplianceCSV(records); // CSVを代用
    }
}