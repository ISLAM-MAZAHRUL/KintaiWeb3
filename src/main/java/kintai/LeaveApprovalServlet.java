package kintai;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/leaveApproval")
public class LeaveApprovalServlet extends HttpServlet {

    private final LeaveRecDao leaveDao = new LeaveRecDao();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute("user") == null) {
            response.getWriter().write("error:ログインしてください");
            return;
        }

        UserBean loginUser = (UserBean) session.getAttribute("user");

        try {
            int leaveId = Integer.parseInt(request.getParameter("leaveId"));
            String action = request.getParameter("action"); // approve or reject

            LeaveRecBean leave = leaveDao.findById(leaveId);
            if (leave == null) {
                response.getWriter().write("error:対象の休暇申請が存在しません");
                return;
            }

            // 承認者チェック
            if (!loginUser.getEmpId().equals(leave.getApprovedBy())) {
                response.getWriter().write("error:承認権限がありません");
                return;
            }

            // ステータス更新
            if ("approve".equals(action)) {
                leave.setStatus("承認済み");
            } else if ("reject".equals(action)) {
                leave.setStatus("却下");
            } else {
                response.getWriter().write("error:不正な操作です");
                return;
            }

            leave.setUpdatedBy(loginUser.getEmpId());

            boolean updated = leaveDao.updateLeaveStatus(leave);

            if (updated) {

                // 承認された場合、USED_DAYSを更新
                if ("approve".equals(action)) {
                    try {
                        // 申請日数を計算
                        LocalDate start = leave.getStartDate().toLocalDate();
                        LocalDate end = leave.getEndDate().toLocalDate();

                        long days =
                                java.time.temporal.ChronoUnit.DAYS
                                        .between(start, end) + 1;

                        // 有給使用日数更新
                        LeaveBalanceDao leaveBalanceDao = new LeaveBalanceDao();
                        leaveBalanceDao.consumeLeaveDays(
                                leave.getEmpId(),
                                leave.getLeaveTypeId(),
                                (int) days
                        );

                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }

                // セッション内の pendingLeaves から対象を削除
                @SuppressWarnings("unchecked")
                List<LeaveRecBean> pendingLeaves =
                        (List<LeaveRecBean>) session.getAttribute("pendingLeaves");

                if (pendingLeaves != null) {
                    pendingLeaves.removeIf(l -> l.getLeaveId() == leaveId);
                    session.setAttribute("pendingLeaves", pendingLeaves);
                }

                response.getWriter().write("success");

            } else {
                response.getWriter().write("error:更新に失敗しました");
            }

        } catch (Exception e) {
            e.printStackTrace();
            response.getWriter().write("error:" + e.getMessage());
        }
    }
}