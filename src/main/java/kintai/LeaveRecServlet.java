package kintai;

import java.io.IOException;
import java.sql.Date;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/leaveRec")
public class LeaveRecServlet extends HttpServlet {

    private final LeaveRecDao dao = new LeaveRecDao();
    private final LeaveBalanceDao balanceDao = new LeaveBalanceDao();
    private final EmpDao empDao = new EmpDao();
    private final DeptDao deptDao = new DeptDao();
    private final PostDao postDao = new PostDao();
    private final LeaveTypeDao typeDao = new LeaveTypeDao();
    
    

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            response.sendRedirect(request.getContextPath() + "/web/login.jsp");
            return;
        }
        
        UserBean loginUser = (UserBean) session.getAttribute("user");

        // --- PRG対応：セッションからメッセージを取得してリクエストに渡す ---
        if (session.getAttribute("message") != null) {
            request.setAttribute("message", session.getAttribute("message"));
            request.setAttribute("success", session.getAttribute("success"));
            session.removeAttribute("message");
            session.removeAttribute("success");
        }

        try {
            String deptId = request.getParameter("dept");
            String postId = request.getParameter("post");
            String empId = request.getParameter("empId");
            
            // 管理者以外は自分のempIdを固定
            if (loginUser.getRoleId() != 1) {
                empId = loginUser.getEmpId();
            }
            
            // 管理者は全社員リスト、一般社員は自分のみ
            if (loginUser.getRoleId() == 1) {
                request.setAttribute("empList", empDao.findByFilters(deptId, postId));
                request.setAttribute("deptList", deptDao.findAll());
                request.setAttribute("postList", postDao.findAll());
            } else {
                EmpBean emp = empDao.findByEmpId(empId);
                if (emp != null) {
                    request.setAttribute("empList", java.util.Collections.singletonList(emp));
                } else {
                    request.setAttribute("empList", java.util.Collections.emptyList());
                }
                request.setAttribute("deptList", java.util.Collections.emptyList());
                request.setAttribute("postList", java.util.Collections.emptyList());
            }

            request.setAttribute("leaveTypeList", typeDao.findAll());
            request.setAttribute("selectedDept", deptId);
            request.setAttribute("selectedPost", postId);
            request.setAttribute("selectedEmpId", empId);

            if (empId != null && !empId.isEmpty()) {
                request.setAttribute("balanceList", balanceDao.getLeaveBalances(empId));
                request.setAttribute("leaveList", dao.getLeaveList(empId));
            }
        } catch (Exception e) {
            request.setAttribute("message", "初期表示に失敗しました:" + e.getMessage());
            request.setAttribute("success", false);
        }

        request.getRequestDispatcher("/web/leave_rec_manage.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            response.sendRedirect(request.getContextPath() + "/web/login.jsp");
            return;
        }
        
        
        UserBean loginUser = (UserBean) session.getAttribute("user");
        

        String mode = request.getParameter("mode");
        String empId = request.getParameter("empId");
        if (empId == null) empId = "";
        
        // 管理者以外は強制的に自分のIDを使う
        if (loginUser.getRoleId() != 1) {
            empId = loginUser.getEmpId();
        }

        try {
            //UserBean loginUser = (UserBean) session.getAttribute("user");

            switch (mode) {
                case "add": {
                    LeaveRecBean addBean = buildBean(request, false);
                    addBean.setEmpId(empId); // 権限に応じて上書き
                    int daysRequested = calcDays(addBean);
                    int remaining = balanceDao.calculateRemainingDays(empId, addBean.getLeaveTypeId());

                    if (daysRequested > remaining) {
                        session.setAttribute("message", "申請日数が残日数を超えています");
                        session.setAttribute("success", false);
                        break;
                    }

                    addBean.setCreatedBy(loginUser.getEmpId());
                    addBean.setUpdatedBy(loginUser.getEmpId());

                    if (dao.insertLeave(addBean)) {
                        balanceDao.consumeLeaveDays(empId, addBean.getLeaveTypeId(), daysRequested);
                        session.setAttribute("message", "休暇申請を登録しました");
                        session.setAttribute("success", true);
                    } else {
                        session.setAttribute("message", "休暇申請の登録に失敗しました");
                        session.setAttribute("success", false);
                    }
                    break;
                }

                case "update": {
                    LeaveRecBean updBean = buildBean(request, true);
                    
                    // 管理者以外は他人の申請を変更できない
                    if (loginUser.getRoleId() != 1 && !updBean.getEmpId().equals(loginUser.getEmpId())) {
                        session.setAttribute("message", "他人の申請は変更できません");
                        session.setAttribute("success", false);
                        break;
                    }
                    
                    updBean.setUpdatedBy(loginUser.getEmpId());
                    
                    if (dao.updateLeave(updBean)) {
                    	dao.recalculateUsedDays(updBean.getEmpId(), updBean.getLeaveTypeId());
                        session.setAttribute("message", "休暇申請を更新しました");
                        session.setAttribute("success", true);
                    } else {
                        session.setAttribute("message", "更新に失敗しました");
                        session.setAttribute("success", false);
                    }
                    break;
                }

                case "delete": {
                    int leaveId = Integer.parseInt(request.getParameter("leaveId"));
                    LeaveRecBean deletedLeave = dao.findById(leaveId);
                    
                    // 管理者以外は自分の申請しか削除できない
                    if (loginUser.getRoleId() != 1 && !deletedLeave.getEmpId().equals(loginUser.getEmpId())) {
                        session.setAttribute("message", "他人の申請は削除できません");
                        session.setAttribute("success", false);
                        break;
                    }

                    if (dao.logicalDeleteLeave(leaveId, loginUser.getEmpId())) {
                        dao.recalculateUsedDays(deletedLeave.getEmpId(), deletedLeave.getLeaveTypeId());
                        session.setAttribute("message", "休暇申請を削除しました");
                        session.setAttribute("success", true);
                        empId = deletedLeave.getEmpId();
                    } else {
                        session.setAttribute("message", "削除に失敗しました");
                        session.setAttribute("success", false);
                    }
                    break;
                }

                default:
                    session.setAttribute("message", "不正な操作が指定されました");
                    session.setAttribute("success", false);
                    break;
            }
        } catch (Exception e) {
            session.setAttribute("message", "処理中にエラーが発生しました: " + e.getMessage());
            session.setAttribute("success", false);
        }

        // --- PRG対応：リダイレクトでGETへ遷移（パラメータで状態を維持） ---
        response.sendRedirect(request.getContextPath() + "/leaveRec?empId=" + empId);
    }

    private LeaveRecBean buildBean(HttpServletRequest req, boolean includeId) {
        LeaveRecBean leave = new LeaveRecBean();
        if (includeId) {
            leave.setLeaveId(Integer.parseInt(req.getParameter("leaveId")));
        }
        leave.setEmpId(req.getParameter("empId"));
        leave.setLeaveTypeId(Integer.parseInt(req.getParameter("leaveTypeId")));
        leave.setStartDate(Date.valueOf(req.getParameter("startDate")));
        leave.setEndDate(Date.valueOf(req.getParameter("endDate")));
        leave.setReason(req.getParameter("reason"));
        
        // ★ 承認者ID（送信先）
        String approverId = req.getParameter("postEmpId");
        leave.setApprovedBy(approverId); 
        
        // 管理者なら、初期ステータスで承認済みにするのもありかもしれない
        // ★ 初期ステータスは「承認待ち」全ての従業員が誰かに承認される前提
        leave.setStatus("承認待ち");
        
        return leave;
    }

    private int calcDays(LeaveRecBean bean) {
        return (int) (bean.getEndDate().toLocalDate().toEpochDay() - bean.getStartDate().toLocalDate().toEpochDay() + 1);
    }
}
