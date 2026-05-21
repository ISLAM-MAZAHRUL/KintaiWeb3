package kintai;

import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * 勤務時間報告書出力機能を提供するサーブレット
 * 各種報告書をPDF、Excel、CSV形式で生成する
 */
@WebServlet("/ReportGenerationServlet")
public class ReportGenerationServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    
    private KintaiRecDao kintaiRecDao = new KintaiRecDao();
    private ReportGenerationService reportService = new ReportGenerationService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doPost(request, response); // GET和POST統一処理
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        // セッションチェック
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "ログインが必要です。");
            return;
        }

        UserBean user = (UserBean) session.getAttribute("user");
        
        // パラメータ取得
        String action = request.getParameter("action");
        String reportType = request.getParameter("reportType");
        String format = request.getParameter("format");

        // 従業員IDは empNoFilter と empno の両方を受け付ける（優先順: empNoFilter → empno → ログインユーザー）
        String empnoParam = request.getParameter("empNoFilter");
        if (empnoParam == null || empnoParam.isEmpty()) {
            empnoParam = request.getParameter("empno");
        }
        if (empnoParam == null || empnoParam.isEmpty()) {
            empnoParam = ((UserBean) session.getAttribute("user")).getEmpno(); // fallback
        }

        String deptNoFilter = request.getParameter("deptNoFilter");
        String postNoFilter = request.getParameter("postNoFilter");
        String startDateStr = request.getParameter("startDate");
        String endDateStr = request.getParameter("endDate");

        
        // アクションに基づく処理分岐
        if ("generateIndividual".equals(action)) {
            // 個人別月次報告のモーダル表示用（HTML返却）
            handleIndividualReportModal(request, response, user);
            return;
        } else if ("downloadIndividual".equals(action)) {
            // 個人別月次報告のダウンロード
            handleIndividualReportDownload(request, response, user);
            return;
        } else if ("generateDepartment".equals(action)) {
            // 部署別集計報告のモーダル表示用（JSON返却）
            handleDepartmentReportModal(request, response, user);
            return;
        } else if ("downloadDepartment".equals(action)) {
            // 部署別集計報告のダウンロード
            handleDepartmentReportDownload(request, response, user);
            return;
        }
        
        // 日付パース
        LocalDate startDate = null;
        LocalDate endDate = null;
        try {
            if (startDateStr != null && !startDateStr.trim().isEmpty()) {
                startDate = LocalDate.parse(startDateStr);
            }
            if (endDateStr != null && !endDateStr.trim().isEmpty()) {
                endDate = LocalDate.parse(endDateStr);
            }
        } catch (DateTimeParseException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "日付形式が不正です。");
            return;
        }
        
        // デフォルト値設定
        if (reportType == null) reportType = "individual";
        if (format == null) format = "pdf";
        if (empnoParam == null) empnoParam = user.getEmpno();
        
        try {
            byte[] reportData = null;
            String filename = "";
            String contentType = "";
            
            switch (reportType) {
	            case "individual":
	                reportData = generateIndividualReport(empnoParam, startDate, endDate, format);
	                filename = "個人別月次勤怠報告書_" + empnoParam + "_" + getCurrentDateString();
	                break;
	            case "department":
	                reportData = generateDepartmentReport(deptNoFilter, startDate, endDate, format);
	                filename = "部署別集計勤怠報告書_" + getCurrentDateString();
	                break;
	            case "overtime":
	                reportData = generateOvertimeReport(empnoParam, deptNoFilter, startDate, endDate, format);
	                filename = "残業時間分析_" + getCurrentDateString();
	                break;
	            case "all":
	                reportData = generateAllEmployeeReport(startDate, endDate, format);
	                filename = "全社員勤怠一覧_" + getCurrentDateString();
	                break;
	            case "period":
	                reportData = generatePeriodReport(empnoParam, deptNoFilter, startDate, endDate, format);
	                filename = "期間指定レポート_" + getCurrentDateString();
	                break;
	            case "compliance":
	                reportData = generateComplianceReport(startDate, endDate, format);
	                filename = "法定チェック_" + getCurrentDateString();
	                break;
	            default:
	                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "不正な報告書タイプです。");
	                return;
	        }

            
            // Content-Typeとファイル拡張子の設定
            switch (format.toLowerCase()) {
                case "pdf":
                    // 現在はHTML形式で代替（印刷可能）
                    contentType = "text/html; charset=UTF-8";
                    filename += ".html";
                    break;
                case "excel":
                    contentType = "application/vnd.ms-excel";
                    filename += ".xls";
                    break;
                case "csv":
                    contentType = "text/csv; charset=UTF-8";
                    filename += ".csv";
                    break;
                default:
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST, "不正な出力形式です。");
                    return;
            }
            
            // レスポンス設定
            response.setContentType(contentType);
            
            // 日本語ファイル名のURL エンコード
            String encodedFilename = java.net.URLEncoder.encode(filename, "UTF-8").replace("+", "%20");
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFilename);
            response.setContentLength(reportData.length);
            
            // データ出力
            OutputStream out = response.getOutputStream();
            out.write(reportData);
            out.flush();
            out.close();
            
        } catch (Exception e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "報告書生成中にエラーが発生しました。");
        }
    }
    
    /**
     * 個人別月次報告書生成
     */
    private byte[] generateIndividualReport(String empno, LocalDate startDate, LocalDate endDate, String format) {
        // 期間が指定されていない場合は当月を設定
        if (startDate == null || endDate == null) {
            LocalDate now = LocalDate.now();
            startDate = now.withDayOfMonth(1);
            endDate = now.withDayOfMonth(now.lengthOfMonth());
        }
        
        // 月度統計取得
        String targetMonth = startDate.getYear() + "-" + String.format("%02d", startDate.getMonthValue());
        MonthlySummaryBean monthlySummary = kintaiRecDao.getMonthlySummary(empno, targetMonth);
        
        // 勤怠記録取得
        List<String> targetEmpNos = List.of(empno);
        List<KintaiRecBean> records = kintaiRecDao.getKintaiRecords(targetEmpNos, null, null, startDate, endDate, 0);
        
        return reportService.generateIndividualReport(monthlySummary, records, format);
    }
    
    /**
     * 部署別集計報告書生成
     */
    private byte[] generateDepartmentReport(String deptNo, LocalDate startDate, LocalDate endDate, String format) {
        if (startDate == null || endDate == null) {
            LocalDate now = LocalDate.now();
            startDate = now.withDayOfMonth(1);
            endDate = now.withDayOfMonth(now.lengthOfMonth());
        }
        
        List<KintaiRecBean> records = kintaiRecDao.getKintaiRecords(null, deptNo, null, startDate, endDate, 1);
        return reportService.generateDepartmentReport(records, deptNo, format);
    }
    
    /**
     * 残業時間分析報告書生成
     */
    private byte[] generateOvertimeReport(String empNo, String deptNo, LocalDate startDate, LocalDate endDate, String format) {
        if (startDate == null || endDate == null) {
            LocalDate now = LocalDate.now();
            startDate = now.withDayOfMonth(1);
            endDate = now.withDayOfMonth(now.lengthOfMonth());
        }
        
        List<String> targetEmpNos = empNo != null ? List.of(empNo) : null;
        List<KintaiRecBean> records = kintaiRecDao.getKintaiRecords(targetEmpNos, deptNo, null, startDate, endDate, 1);
        return reportService.generateOvertimeReport(records, format);
    }
    
    /**
     * 全社員勤怠一覧報告書生成
     */
    private byte[] generateAllEmployeeReport(LocalDate startDate, LocalDate endDate, String format) {
        if (startDate == null || endDate == null) {
            LocalDate now = LocalDate.now();
            startDate = now.withDayOfMonth(1);
            endDate = now.withDayOfMonth(now.lengthOfMonth());
        }
        
        List<KintaiRecBean> records = kintaiRecDao.getKintaiRecords(null, null, null, startDate, endDate, 1);
        return reportService.generateAllEmployeeReport(records, format);
    }
    
    /**
     * 期間指定レポート生成
     */
    private byte[] generatePeriodReport(String empNo, String deptNo, LocalDate startDate, LocalDate endDate, String format) {
        List<String> targetEmpNos = empNo != null ? List.of(empNo) : null;
        List<KintaiRecBean> records = kintaiRecDao.getKintaiRecords(targetEmpNos, deptNo, null, startDate, endDate, 1);
        return reportService.generatePeriodReport(records, startDate, endDate, format);
    }
    
    /**
     * 法定チェック報告書生成
     */
    private byte[] generateComplianceReport(LocalDate startDate, LocalDate endDate, String format) {
        if (startDate == null || endDate == null) {
            LocalDate now = LocalDate.now();
            startDate = now.withDayOfMonth(1);
            endDate = now.withDayOfMonth(now.lengthOfMonth());
        }
        
        List<KintaiRecBean> records = kintaiRecDao.getKintaiRecords(null, null, null, startDate, endDate, 1);
        return reportService.generateComplianceReport(records, format);
    }
    
    /**
     * 個人別月次報告のモーダル表示用処理
     */
    private void handleIndividualReportModal(HttpServletRequest request, HttpServletResponse response, UserBean user) 
            throws ServletException, IOException {

        try {
        	String empNoFilter = request.getParameter("empNoFilter");
        	if (empNoFilter == null || empNoFilter.isEmpty()) {
        	    empNoFilter = user.getEmpId(); // ← ここでログイン中ユーザーにフォールバック
        	}
            String startDateStr = request.getParameter("startDate");
            String endDateStr = request.getParameter("endDate");
            
            // 🔎 デバッグログ
            System.out.println("[DEBUG][ReportGenerationServlet] action=generateIndividual"
                + ", empNoFilter=" + empNoFilter
                + ", startDate=" + startDateStr
                + ", endDate=" + endDateStr
                + ", loggedInUser=" + user.getEmpId());
            
            // 日付パース
            LocalDate startDate = null;
            LocalDate endDate = null;
            if (startDateStr != null && !startDateStr.trim().isEmpty()) {
                startDate = LocalDate.parse(startDateStr);
            }
            if (endDateStr != null && !endDateStr.trim().isEmpty()) {
                endDate = LocalDate.parse(endDateStr);
            }
            
            // デフォルト値設定（当月）
            if (startDate == null || endDate == null) {
                LocalDate now = LocalDate.now();
                startDate = now.withDayOfMonth(1);
                endDate = now.withDayOfMonth(now.lengthOfMonth());
            }
            
         // 月度統計取得
            String targetMonth = startDate.getYear() + "-" + String.format("%02d", startDate.getMonthValue());
            MonthlySummaryBean monthlySummary = kintaiRecDao.getMonthlySummary(empNoFilter, targetMonth);
            System.out.println("[DEBUG] targetMonth=" + targetMonth + ", monthlySummary=" + monthlySummary);

            // 勤怠記録取得
            List<String> targetEmpNos = List.of(empNoFilter);
            List<KintaiRecBean> records = kintaiRecDao.getKintaiRecords(targetEmpNos, null, null, startDate, endDate, 0);
            System.out.println("[DEBUG] records.size=" + records.size());

            // 従業員情報取得
            EmpDao empDao = new EmpDao();
            PersonalReportBean report = empDao.getEmployeeForReport(empNoFilter);
            System.out.println("[DEBUG] report=" + report);
            
            if (report == null) {
                report = new PersonalReportBean();
                report.setEmpno(empNoFilter);
                report.setEmpName("不明");
                report.setDeptName("不明");
                report.setPostName("不明");
            }
            
            // JSON形式でレスポンスを返す
            response.setContentType("application/json; charset=UTF-8");
            StringBuilder json = new StringBuilder();
            json.append("{");
            json.append("\"empno\":\"").append(report.getEmpno()).append("\",");
            json.append("\"empName\":\"").append(report.getEmpName()).append("\",");
            json.append("\"deptName\":\"").append(report.getDeptName()).append("\",");
            json.append("\"postName\":\"").append(report.getPostName()).append("\",");
            json.append("\"targetMonth\":\"").append(targetMonth).append("\",");
            json.append("\"attendanceRate\":\"").append(monthlySummary.getAttendanceRateString()).append("\",");
            json.append("\"totalWorkingHours\":\"").append(monthlySummary.getTotalWorkingHoursString()).append("\",");
            json.append("\"totalOvertimeHours\":\"").append(monthlySummary.getTotalOvertimeHoursString()).append("\",");
            json.append("\"totalBreakHours\":\"").append(monthlySummary.getTotalBreakHoursString()).append("\",");
            json.append("\"records\":[");
            
            for (int i = 0; i < records.size(); i++) {
                KintaiRecBean record = records.get(i);
                if (i > 0) json.append(",");
                
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
                
                json.append("{");
                json.append("\"date\":\"").append(record.getKintaiDate() != null ? record.getKintaiDate().toString() : "").append("\",");
                json.append("\"dayOfWeek\":\"").append(dayOfWeek).append("\",");
                json.append("\"clockIn\":\"").append(record.getClockIn() != null ? record.getClockIn().toString().substring(0, 5) : "---").append("\",");
                json.append("\"clockOut\":\"").append(record.getClockOut() != null ? record.getClockOut().toString().substring(0, 5) : "---").append("\",");
                json.append("\"breakTime\":\"").append(record.getTotalBreakTimeFormatted()).append("\",");
                json.append("\"workTime\":\"").append(record.getActualWorkTimeFormatted()).append("\",");
                json.append("\"overtimeTime\":\"").append(record.getOvertimeFormatted()).append("\",");
                json.append("\"nightovertimeTime\":\"").append(record.getNightovertimeFormatted()).append("\",");
                json.append("\"remarks\":\"").append(remarks).append("\"");
                json.append("}");
            }
            
            json.append("]}");
            
            response.getWriter().write(json.toString());
            
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType("application/json; charset=UTF-8");
            response.getWriter().write("{\"error\":\"データの取得に失敗しました: " + e.getMessage() + "\"}");
        }
    }
    
    /**
     * 部署別集計報告のモーダル表示用処理
     */
    private void handleDepartmentReportModal(HttpServletRequest request, HttpServletResponse response, UserBean user) 
            throws ServletException, IOException {
        try {
            String deptNoFilter = request.getParameter("deptNoFilter");
            String startDateStr = request.getParameter("startDate");
            String endDateStr = request.getParameter("endDate");
            
            // 日付パース
            LocalDate startDate = null;
            LocalDate endDate = null;
            if (startDateStr != null && !startDateStr.trim().isEmpty()) {
                startDate = LocalDate.parse(startDateStr);
            }
            if (endDateStr != null && !endDateStr.trim().isEmpty()) {
                endDate = LocalDate.parse(endDateStr);
            }
            
            // デフォルト値設定（当月）
            if (startDate == null || endDate == null) {
                LocalDate now = LocalDate.now();
                startDate = now.withDayOfMonth(1);
                endDate = now.withDayOfMonth(now.lengthOfMonth());
            }
            
            // 部署別勤怠記録取得
            List<KintaiRecBean> records = kintaiRecDao.getKintaiRecords(null, deptNoFilter, null, startDate, endDate, 1);
            
            // 部署情報取得
            String targetDeptName = "全部署";
            if (deptNoFilter != null && !deptNoFilter.trim().isEmpty()) {
                // 部署名を取得するためのDAO呼び出し（仮実装）
                targetDeptName = deptNoFilter; // 実際は部署名を取得
            }
            
            // 部署別統計計算
            java.util.Map<String, java.util.List<KintaiRecBean>> deptGroups = records.stream()
                .collect(java.util.stream.Collectors.groupingBy(r -> r.getDeptName() != null ? r.getDeptName() : "未設定"));
            
            // JSON形式でレスポンスを返す
            response.setContentType("application/json; charset=UTF-8");
            StringBuilder json = new StringBuilder();
            json.append("{");
            json.append("\"targetDeptName\":\"").append(targetDeptName).append("\",");
            json.append("\"targetPeriod\":\"").append(startDate).append(" ～ ").append(endDate).append("\",");
            json.append("\"totalRecords\":").append(records.size()).append(",");
            json.append("\"totalEmployees\":").append(records.stream().map(KintaiRecBean::getEmpno).distinct().count()).append(",");
            
            // 部署統計
            json.append("\"departments\":[");
            boolean first = true;
            for (java.util.Map.Entry<String, java.util.List<KintaiRecBean>> entry : deptGroups.entrySet()) {
                if (!first) json.append(",");
                first = false;
                
                String deptName = entry.getKey();
                java.util.List<KintaiRecBean> deptRecords = entry.getValue();
                
                long totalOvertimeMinutes = deptRecords.stream()
                    .mapToLong(KintaiRecBean::getOvertimeMinutes)
                    .sum();
                    
                long totalWorkMinutes = deptRecords.stream()
                    .mapToLong(KintaiRecBean::getActualWorkMinutes)
                    .sum();
                    
                long empCount = deptRecords.stream().map(KintaiRecBean::getEmpno).distinct().count();
                
                json.append("{");
                json.append("\"deptName\":\"").append(deptName).append("\",");
                json.append("\"employeeCount\":").append(empCount).append(",");
                json.append("\"recordCount\":").append(deptRecords.size()).append(",");
                json.append("\"totalOvertimeHours\":\"").append(String.format("%.1f", totalOvertimeMinutes / 60.0)).append("\",");
                json.append("\"totalWorkHours\":\"").append(String.format("%.1f", totalWorkMinutes / 60.0)).append("\",");
                json.append("\"avgOvertimeHours\":\"").append(String.format("%.1f", empCount > 0 ? (totalOvertimeMinutes / 60.0) / empCount : 0)).append("\"");
                json.append("}");
            }
            json.append("],");
            
            // 詳細記録
            json.append("\"records\":[");
            for (int i = 0; i < Math.min(records.size(), 50); i++) { // 最大50件まで表示
                KintaiRecBean record = records.get(i);
                if (i > 0) json.append(",");
                
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
                
                json.append("{");
                json.append("\"date\":\"").append(record.getKintaiDate() != null ? record.getKintaiDate().toString() : "").append("\",");
                json.append("\"dayOfWeek\":\"").append(dayOfWeek).append("\",");
                json.append("\"empno\":\"").append(record.getEmpno() != null ? record.getEmpno() : "").append("\",");
                json.append("\"empName\":\"").append(record.getEmpName() != null ? record.getEmpName() : "").append("\",");
                json.append("\"deptName\":\"").append(record.getDeptName() != null ? record.getDeptName() : "").append("\",");
                json.append("\"clockIn\":\"").append(record.getClockIn() != null ? record.getClockIn().toString().substring(0, 5) : "---").append("\",");
                json.append("\"clockOut\":\"").append(record.getClockOut() != null ? record.getClockOut().toString().substring(0, 5) : "---").append("\",");
                json.append("\"workTime\":\"").append(record.getActualWorkTimeFormatted()).append("\",");
                json.append("\"overtimeTime\":\"").append(record.getOvertimeFormatted()).append("\",");
                json.append("\"nightovertimeTime\":\"").append(record.getNightovertimeFormatted()).append("\"");
                json.append("}");
            }
            json.append("]}");
            
            response.getWriter().write(json.toString());
            
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType("application/json; charset=UTF-8");
            response.getWriter().write("{\"error\":\"データの取得に失敗しました: " + e.getMessage() + "\"}");
        }
    }
    
    /**
     * 個人別月次報告のダウンロード処理
     */
    private void handleIndividualReportDownload(HttpServletRequest request, HttpServletResponse response, UserBean user) 
            throws ServletException, IOException {
        try {
            String empNoFilter = request.getParameter("empNoFilter");
            String format = request.getParameter("format");
            String startDateStr = request.getParameter("startDate");
            String endDateStr = request.getParameter("endDate");
            
            // 日付パース
            LocalDate startDate = null;
            LocalDate endDate = null;
            if (startDateStr != null && !startDateStr.trim().isEmpty()) {
                startDate = LocalDate.parse(startDateStr);
            }
            if (endDateStr != null && !endDateStr.trim().isEmpty()) {
                endDate = LocalDate.parse(endDateStr);
            }
            
            // 報告書データ生成
            byte[] reportData = generateIndividualReport(empNoFilter, startDate, endDate, format);
            
            // ファイル名生成
            String filename = "個人別月次勤怠報告書_" + empNoFilter + "_" + getCurrentDateString();
            String contentType;
            
            // Content-Typeとファイル拡張子の設定
            switch (format.toLowerCase()) {
                case "pdf":
                    // 現在はHTML形式で代替（印刷可能）
                    contentType = "text/html; charset=UTF-8";
                    filename += ".html";
                    break;
                case "excel":
                    contentType = "application/vnd.ms-excel";
                    filename += ".xls";
                    break;
                case "csv":
                    contentType = "text/csv; charset=UTF-8";
                    filename += ".csv";
                    break;
                default:
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST, "不正な出力形式です。");
                    return;
            }
            
            // レスポンス設定
            response.setContentType(contentType);
            
            // 日本語ファイル名のURL エンコード
            String encodedFilename = java.net.URLEncoder.encode(filename, "UTF-8").replace("+", "%20");
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFilename);
            response.setContentLength(reportData.length);
            
            // データ出力
            response.getOutputStream().write(reportData);
            response.getOutputStream().flush();
            
        } catch (Exception e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "報告書生成中にエラーが発生しました。");
        }
    }
    
    /**
     * 部署別集計報告のダウンロード処理
     */
    private void handleDepartmentReportDownload(HttpServletRequest request, HttpServletResponse response, UserBean user) 
            throws ServletException, IOException {
        try {
            String deptNoFilter = request.getParameter("deptNoFilter");
            String format = request.getParameter("format");
            String startDateStr = request.getParameter("startDate");
            String endDateStr = request.getParameter("endDate");
            
            // 日付パース
            LocalDate startDate = null;
            LocalDate endDate = null;
            if (startDateStr != null && !startDateStr.trim().isEmpty()) {
                startDate = LocalDate.parse(startDateStr);
            }
            if (endDateStr != null && !endDateStr.trim().isEmpty()) {
                endDate = LocalDate.parse(endDateStr);
            }
            
            // 報告書データ生成
            byte[] reportData = generateDepartmentReport(deptNoFilter, startDate, endDate, format);
            
            // ファイル名生成
            String filename = "部署別集計勤怠報告書_" + getCurrentDateString();
            String contentType;
            
            // Content-Typeとファイル拡張子の設定
            switch (format.toLowerCase()) {
                case "pdf":
                    // 現在はHTML形式で代替（印刷可能）
                    contentType = "text/html; charset=UTF-8";
                    filename += ".html";
                    break;
                case "excel":
                    contentType = "application/vnd.ms-excel";
                    filename += ".xls";
                    break;
                case "csv":
                    contentType = "text/csv; charset=UTF-8";
                    filename += ".csv";
                    break;
                default:
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST, "不正な出力形式です。");
                    return;
            }
            
            // レスポンス設定
            response.setContentType(contentType);
            
            // 日本語ファイル名のURL エンコード
            String encodedFilename = java.net.URLEncoder.encode(filename, "UTF-8").replace("+", "%20");
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFilename);
            response.setContentLength(reportData.length);
            
            // データ出力
            response.getOutputStream().write(reportData);
            response.getOutputStream().flush();
            
        } catch (Exception e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "報告書生成中にエラーが発生しました。");
        }
    }
    
    /**
     * 現在日付の文字列取得
     */
    private String getCurrentDateString() {
        return LocalDate.now().toString().replace("-", "");
    }
}