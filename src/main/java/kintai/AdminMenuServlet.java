package kintai;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * 管理者メニュー用のデータを取得するサーブレット
 * 管理者がログイン後に admin_menu.jsp を表示するために必要な情報をすべてここで取得
 */
@WebServlet("/AdminMenuServlet")
public class AdminMenuServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        // --- 1. セッションチェック ---
        HttpSession session = request.getSession();
        UserBean user = (UserBean) session.getAttribute("user");
        
        if (user == null) {
            // 未ログインならログイン画面へ
            response.sendRedirect(request.getContextPath() + "/web/login.jsp");
            return;
        }

        // --- 2. 管理者権限チェック ---
        if (user.getRoleId() != 1) {
            // 管理者でない場合は通常メニューにリダイレクト
            response.sendRedirect(request.getContextPath() + "/web/menu.jsp");
            return;
        }

        try {
            // --- 3. システム概要データを取得 ---
            SystemSummary summary = getSystemSummary();

            request.setAttribute("totalEmployees", summary.getTotalEmployees());
            request.setAttribute("todayAttendance", summary.getTodayAttendance());
            request.setAttribute("totalDepts", summary.getTotalDepts());
            request.setAttribute("monthlyHours", summary.getMonthlyHours());
            request.setAttribute("pendingRequests", summary.getPendingRequests());

            // --- 4. 承認待ち休暇申請を取得 ---
            LeaveRecDao leaveRecDao = new LeaveRecDao();
            List<LeaveRecBean> pendingLeaves = new ArrayList<>();
            try {
                pendingLeaves = leaveRecDao.findPendingByApprover(user.getEmpId());
            } catch (Exception e) {
                e.printStackTrace();
            }

            // JSP で表示するために request 属性にセット
            request.setAttribute("pendingLeaves", pendingLeaves);

        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
            // エラーが発生した場合はデフォルト値を設定
            request.setAttribute("totalEmployees", "-");
            request.setAttribute("todayAttendance", "-");
            request.setAttribute("totalDepts", "-");
            request.setAttribute("monthlyHours", "-");
            request.setAttribute("pendingRequests", "-");
            request.setAttribute("pendingLeaves", new ArrayList<LeaveRecBean>());
        }

        // --- 5. JSP へフォワード ---
        RequestDispatcher dispatcher = request.getRequestDispatcher("/web/admin_menu.jsp");
        dispatcher.forward(request, response);
    }

    /**
     * システム概要データを取得するメソッド
     */
    private SystemSummary getSystemSummary() throws SQLException, ClassNotFoundException {
        SystemSummary summary = new SystemSummary();
        DBAccess db = new DBAccess();
        
        try (Connection conn = db.getConnection()) {
            // 総従業員数
            String empCountSql = "SELECT COUNT(*) FROM emp WHERE IS_ACTIVE = true";
            try (PreparedStatement stmt = conn.prepareStatement(empCountSql);
                 ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    summary.setTotalEmployees(rs.getInt(1));
                }
            }
            
            // 今日の出勤者数
            String todayAttendanceSql = 
                "SELECT COUNT(DISTINCT EMP_ID) FROM kintai " +
                "WHERE KINTAI_DATE = CURDATE() AND CLOCK_IN IS NOT NULL AND IS_DELETED = false";
            try (PreparedStatement stmt = conn.prepareStatement(todayAttendanceSql);
                 ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    summary.setTodayAttendance(rs.getInt(1));
                }
            }
            
            // 部署数
            String deptCountSql = "SELECT COUNT(*) FROM dept WHERE IS_DELETED = false";
            try (PreparedStatement stmt = conn.prepareStatement(deptCountSql);
                 ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    summary.setTotalDepts(rs.getInt(1));
                }
            }
            
            // 今月の総労働時間
            YearMonth currentMonth = YearMonth.now();
            String monthlyHoursSql = 
                "SELECT COALESCE(SUM(WORKING_HOURS), 0) FROM kintai " +
                "WHERE YEAR(KINTAI_DATE) = ? AND MONTH(KINTAI_DATE) = ? AND IS_DELETED = false";
            try (PreparedStatement stmt = conn.prepareStatement(monthlyHoursSql)) {
                stmt.setInt(1, currentMonth.getYear());
                stmt.setInt(2, currentMonth.getMonthValue());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        summary.setMonthlyHours(rs.getDouble(1));
                    }
                }
            }
            
            // 未処理の申請数（ここでは休暇申請数を取得していないため 0 で仮置き）
            summary.setPendingRequests(0);
        }
        
        return summary;
    }

    /**
     * システム概要データを格納するクラス
     */
    private static class SystemSummary {
        private int totalEmployees = 0;
        private int todayAttendance = 0;
        private int totalDepts = 0;
        private double monthlyHours = 0.0;
        private int pendingRequests = 0;

        public int getTotalEmployees() { return totalEmployees; }
        public void setTotalEmployees(int totalEmployees) { this.totalEmployees = totalEmployees; }
        
        public int getTodayAttendance() { return todayAttendance; }
        public void setTodayAttendance(int todayAttendance) { this.todayAttendance = todayAttendance; }
        
        public int getTotalDepts() { return totalDepts; }
        public void setTotalDepts(int totalDepts) { this.totalDepts = totalDepts; }
        
        public double getMonthlyHours() { return monthlyHours; }
        public void setMonthlyHours(double monthlyHours) { this.monthlyHours = monthlyHours; }
        
        public int getPendingRequests() { return pendingRequests; }
        public void setPendingRequests(int pendingRequests) { this.pendingRequests = pendingRequests; }
    }
}
