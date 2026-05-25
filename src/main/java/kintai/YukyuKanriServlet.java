package kintai;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/YukyuKanriServlet")
public class YukyuKanriServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private LeaveBalanceDao leaveBalanceDao = new LeaveBalanceDao();
    private EmpDao empDao = new EmpDao();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            response.sendRedirect(request.getContextPath() + "/web/login.jsp");
            return;
        }

        UserBean loginUser = (UserBean) session.getAttribute("user");
        String action = request.getParameter("action");
        if (action == null) action = "list";

        // ===== 一覧表示 =====
        if ("list".equals(action) || action.isEmpty()) {
            List<Map<String, Object>> summaryList = leaveBalanceDao.findYukyuSummary();
            request.setAttribute("summaryList", summaryList);
            request.getRequestDispatcher("/web/YukyuKanri.jsp")
                   .forward(request, response);
            return;
        }

        // ===== 個別履歴表示 =====
        if ("detail".equals(action)) {
            String empId = request.getParameter("empId");
            if (empId == null || empId.isEmpty()) {
                response.sendRedirect(request.getContextPath() + "/YukyuKanriServlet");
                return;
            }

            // 付与履歴取得
            List<Map<String, Object>> historyList = leaveBalanceDao.findGrantHistory(empId);
            EmpBean emp = empDao.findByEmpId(empId);

            request.setAttribute("historyList", historyList);
            request.setAttribute("emp", emp);
            request.setAttribute("empId", empId);
            request.getRequestDispatcher("/web/YukyuKanriDetail.jsp")
                   .forward(request, response);
            return;
        }

        // ===== 今年度分付与 =====
        if ("grant".equals(action)) {
            // 管理者のみ
            if (loginUser.getRoleId() != 1) {
                response.sendRedirect(request.getContextPath() + "/YukyuKanriServlet");
                return;
            }

            try {
                // 今年度開始日計算（7月始まり）
                LocalDate today = LocalDate.now();
                LocalDate thisYearStart = today.getMonthValue() >= 7
                    ? LocalDate.of(today.getYear(), 7, 1)
                    : LocalDate.of(today.getYear() - 1, 7, 1);
                LocalDate expireDate = thisYearStart.plusYears(1);

                // 全社員取得
                List<EmpBean> empList = empDao.findAll();

                int grantedCount = 0;
                int skippedCount = 0;

                for (EmpBean emp : empList) {
                    // 今年度分がすでに付与済みかチェック
                    boolean alreadyGranted = leaveBalanceDao.isAlreadyGranted(
                        emp.getEmpId(), 1, thisYearStart);

                    if (!alreadyGranted) {
                        // 付与
                        leaveBalanceDao.grantLeave(
                            emp.getEmpId(), 1, thisYearStart, expireDate, 10, loginUser.getEmpId());
                        grantedCount++;
                    } else {
                        skippedCount++;
                    }
                }

                request.setAttribute("grantedCount", grantedCount);
                request.setAttribute("skippedCount", skippedCount);
                request.setAttribute("message", 
                    grantedCount + "名に付与しました。" + skippedCount + "名はすでに付与済みです。");

            } catch (Exception e) {
                e.printStackTrace();
                request.setAttribute("message", "エラーが発生しました: " + e.getMessage());
            }

            // 付与後に一覧表示
            List<Map<String, Object>> summaryList = leaveBalanceDao.findYukyuSummary();
            request.setAttribute("summaryList", summaryList);
            request.getRequestDispatcher("/web/YukyuKanri.jsp")
                   .forward(request, response);
            return;
        }

        // デフォルト → 一覧
        List<Map<String, Object>> summaryList = leaveBalanceDao.findYukyuSummary();
        request.setAttribute("summaryList", summaryList);
        request.getRequestDispatcher("/web/YukyuKanri.jsp")
               .forward(request, response);
    }
}