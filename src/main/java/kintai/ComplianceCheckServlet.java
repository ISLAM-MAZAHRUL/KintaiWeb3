package kintai;

import java.io.IOException;
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
 * 法令遵守チェックと会社規則チェックのリクエスト
 */
@WebServlet("/ComplianceCheckServlet")
public class ComplianceCheckServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    
    private KintaiRecDao kintaiRecDao = new KintaiRecDao();
    private ComplianceChecker complianceChecker = new ComplianceChecker();

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
            response.sendRedirect(request.getContextPath() + "/web/login.jsp");
            return;
        }

        UserBean user = (UserBean) session.getAttribute("user");
        String action = request.getParameter("action");
        
        try {
            switch (action != null ? action : "") {
                case "legalCheck":
                    performLegalComplianceCheck(request, response, user);
                    break;
                case "companyRulesCheck":
                    performCompanyRulesCheck(request, response, user);
                    break;
                case "comprehensiveCheck":
                    performComprehensiveCheck(request, response, user);
                    break;
                case "getViolationDetails":
                    getViolationDetails(request, response, user);
                    break;
                default:
                    showComplianceCheckPage(request, response, user);
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
            request.setAttribute("errorMessage", "合規チェック処理中にエラーが発生しました: " + e.getMessage());
            request.getRequestDispatcher("/web/kintai_rec.jsp").forward(request, response);
        }
    }
    
    /**
     * 合規チェックページの表示
     */
    private void showComplianceCheckPage(HttpServletRequest request, HttpServletResponse response, UserBean user)
            throws ServletException, IOException {
        
        // 基本的な月次データを取得してメイン画面に戻る
        String targetMonth = LocalDate.now().getYear() + "-" + String.format("%02d", LocalDate.now().getMonthValue());
        
        if (user.getRoleId() == 1) {
            // 管理者の場合：全員モード
            request.setAttribute("isSelfMode", false);
        } else {
            // 一般社員の場合：自分モード
            request.setAttribute("isSelfMode", true);
            MonthlySummaryBean monthlySummary = kintaiRecDao.getMonthlySummary(user.getEmpno(), targetMonth);
            request.setAttribute("monthlySummary", monthlySummary);
        }
        
        request.getRequestDispatcher("/web/kintai_rec.jsp").forward(request, response);
    }
    
    /**
     * 法令遵守チェックの実行
     */
    private void performLegalComplianceCheck(HttpServletRequest request, HttpServletResponse response, UserBean user)
            throws ServletException, IOException {
        
        String empno = getTargetEmpno(request, user);
        List<KintaiRecBean> records = getKintaiRecords(request, empno);
        
        ComplianceCheckResult result = complianceChecker.performLegalComplianceCheck(records, empno);
        
        request.setAttribute("complianceResult", result);
        request.setAttribute("checkType", "legal");
        request.setAttribute("successMessage", "法令遵守チェックが完了しました。");
        
        forwardToResultPage(request, response, user, empno);
    }
    
    /**
     * 会社規則チェックの実行
     */
    private void performCompanyRulesCheck(HttpServletRequest request, HttpServletResponse response, UserBean user)
            throws ServletException, IOException {
        
        String empno = getTargetEmpno(request, user);
        List<KintaiRecBean> records = getKintaiRecords(request, empno);
        
        ComplianceCheckResult result = complianceChecker.performCompanyRulesCheck(records, empno);
        
        request.setAttribute("complianceResult", result);
        request.setAttribute("checkType", "company");
        request.setAttribute("successMessage", "会社規則チェックが完了しました。");
        
        forwardToResultPage(request, response, user, empno);
    }
    
    /**
     * 総合合規チェックの実行
     */
    private void performComprehensiveCheck(HttpServletRequest request, HttpServletResponse response, UserBean user)
            throws ServletException, IOException {
        
        String empno = getTargetEmpno(request, user);
        List<KintaiRecBean> records = getKintaiRecords(request, empno);
        
        ComplianceCheckResult result = complianceChecker.performComprehensiveCheck(records, empno);
        
        request.setAttribute("complianceResult", result);
        request.setAttribute("checkType", "comprehensive");
        request.setAttribute("successMessage", "総合合規チェックが完了しました。");
        
        forwardToResultPage(request, response, user, empno);
    }
    
    /**
     * 違反詳細の取得
     */
    private void getViolationDetails(HttpServletRequest request, HttpServletResponse response, UserBean user)
            throws ServletException, IOException {
        
        String empno = getTargetEmpno(request, user);
        String violationType = request.getParameter("violationType");
        
        List<KintaiRecBean> records = getKintaiRecords(request, empno);
        
        // 指定された違反タイプに関連する記録をフィルタリング
        List<KintaiRecBean> filteredRecords = records.stream()
            .filter(record -> isRelatedToViolationType(record, violationType))
            .collect(java.util.stream.Collectors.toList());
        
        request.setAttribute("violationType", violationType);
        request.setAttribute("violationRecords", filteredRecords);
        request.setAttribute("targetEmpno", empno);
        
        request.getRequestDispatcher("/web/compliance_violation_details.jsp").forward(request, response);
    }
    
    /**
     * 対象従業員番号の取得
     */
    private String getTargetEmpno(HttpServletRequest request, UserBean user) {
        String empno = request.getParameter("empno");
        
        // 管理者以外は自分の番号のみ
        if (user.getRoleId() != 1) {
            empno = user.getEmpno();
        } else if (empno == null || empno.trim().isEmpty()) {
            empno = user.getEmpno();
        }
        
        return empno;
    }
    
    /**
     * 勤怠記録の取得
     */
    private List<KintaiRecBean> getKintaiRecords(HttpServletRequest request, String empno) {
        String startDateStr = request.getParameter("startDate");
        String endDateStr = request.getParameter("endDate");
        
        LocalDate startDate;
        LocalDate endDate;
        
        try {
            if (startDateStr != null && !startDateStr.trim().isEmpty()) {
                startDate = LocalDate.parse(startDateStr);
            } else {
                // デフォルトは当月
                startDate = LocalDate.now().withDayOfMonth(1);
            }
            
            if (endDateStr != null && !endDateStr.trim().isEmpty()) {
                endDate = LocalDate.parse(endDateStr);
            } else {
                // デフォルトは当月末
                endDate = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth());
            }
        } catch (DateTimeParseException e) {
            // エラーの場合はデフォルトを使用
            startDate = LocalDate.now().withDayOfMonth(1);
            endDate = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth());
        }
        
        List<String> targetEmpNos = List.of(empno);
        return kintaiRecDao.getKintaiRecords(targetEmpNos, null, null, startDate, endDate, 0);
    }
    
    /**
     * 結果ページへの転送
     */
    private void forwardToResultPage(HttpServletRequest request, HttpServletResponse response, 
                                   UserBean user, String empno) 
            throws ServletException, IOException {
        
        // 基本データの設定
        request.setAttribute("targetEmpno", empno);
        
        if (user.getRoleId() == 1) {
            request.setAttribute("isSelfMode", false);
        } else {
            request.setAttribute("isSelfMode", true);
            String targetMonth = LocalDate.now().getYear() + "-" + String.format("%02d", LocalDate.now().getMonthValue());
            MonthlySummaryBean monthlySummary = kintaiRecDao.getMonthlySummary(user.getEmpno(), targetMonth);
            request.setAttribute("monthlySummary", monthlySummary);
        }
        
        request.getRequestDispatcher("/web/compliance_check_result.jsp").forward(request, response);
    }
    
    /**
     * 記録が指定された違反タイプに関連するかチェック
     */
    private boolean isRelatedToViolationType(KintaiRecBean record, String violationType) {
        if (violationType == null) return false;
        
        switch (violationType) {
//            case "遅刻":
//                return record.getClockIn() != null && 
//                       record.getClockIn().toLocalTime().isAfter(java.time.LocalTime.of(9, 0));
            case "早退":
                return record.getClockOut() != null && 
                       record.getClockOut().toLocalTime().isBefore(java.time.LocalTime.of(17, 30));
            case "法定労働時間超過":
                return record.getActualWorkMinutes() > 8 * 60;
            case "残業時間超過":
                return record.getOvertimeMinutes() > 2 * 60;
            case "休憩時間不足":
                return record.getActualWorkMinutes() > 6 * 60 && record.getTotalBreakMinutes() < 45;
            case "深夜勤務":
                if (record.getClockOut() == null) return false;
                java.time.LocalTime clockOut = record.getClockOut().toLocalTime();
                return clockOut.isAfter(java.time.LocalTime.of(22, 0)) || 
                       clockOut.isBefore(java.time.LocalTime.of(5, 0));
            case "欠勤":
                return record.getClockIn() == null && record.getClockOut() == null;
            default:
                return false;
        }
    }
}