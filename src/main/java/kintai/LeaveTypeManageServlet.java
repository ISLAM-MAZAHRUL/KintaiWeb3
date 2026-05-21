package kintai;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * 休日種別管理機能のサーブレット
 */
@WebServlet("/leaveTypeManage")
public class LeaveTypeManageServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private LeaveTypeDao leaveTypeDao = new LeaveTypeDao();

    // 編集・削除不可ID
    private static final List<Integer> FIXED_IDS = Arrays.asList(1, 2, 3, 11, 12, 20, 21, 22, 23, 30, 40, 41);

    /**
     * GETリクエストの処理メソッド。
     * 部署一覧を表示する。
     * @param request HTTPリクエストオブジェクト
     * @param response HTTPレスポンスオブジェクト
     * @throws ServletException サーブレット例外
     * @throws IOException 入出力例外
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
    	
    	// セッションチェック
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            response.sendRedirect(request.getContextPath() + "/web/login.jsp");
            return;
        }
        
        // 管理者権限チェック 
        UserBean user = (UserBean) session.getAttribute("user");
        if (user.getRoleId() != 1) { // ROLEIDが1が管理者
            response.sendRedirect(request.getContextPath() + "/web/menu.jsp");
            return;
        }
        
     // アクションをチェック
        String action = request.getParameter("action");
        
        if ("history".equals(action)) {
            // 削除履歴一覧を表示
            System.out.println("LeaveTypeManageServlet.doGet - Calling findDeleted()");
            List<LeaveTypeBean> deletedLeaveTypeList = leaveTypeDao.findDeleted();
            System.out.println("LeaveTypeManageServlet.doGet - findDeleted() returned " + deletedLeaveTypeList.size() + " items");
            request.setAttribute("deletedLeaveTypeList", deletedLeaveTypeList);
            
            RequestDispatcher dispatcher = request.getRequestDispatcher("/web/leave_manage_history.jsp");
            dispatcher.forward(request, response);
        } else {
        	// 通常の部署管理画面
        	List<LeaveTypeBean> leaveTypeList = leaveTypeDao.findAll();
        	List<LeaveTypeBean> deletedLeaveTypeList = leaveTypeDao.findDeleted();
        	
            request.setAttribute("leaveTypeList", leaveTypeList);
            request.setAttribute("deletedLeaveTypeList", deletedLeaveTypeList);
            request.setAttribute("protectedIds", FIXED_IDS);

            
            // 休日種別管理画面にフォワード
            RequestDispatcher dispatcher = request.getRequestDispatcher("/web/leave_manage.jsp");
            dispatcher.forward(request, response);
            
        }

        
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String action = request.getParameter("action");
        String message = "";
        boolean success = false;

        // セッションから従業員番号取得
        UserBean user = (UserBean) request.getSession().getAttribute("user");
        String empId = (user != null) ? user.getEmpId() : "unknown";

        
        try {
            switch (action) {
                case "add": {
                	String idStr = request.getParameter("leaveTypeId");
                	if (idStr == null || idStr.isBlank()) {
                	    message = "休日種別IDが入力されていません。";
                	    break;
                	}
                    int id = Integer.parseInt(request.getParameter("leaveTypeId"));
                    String name = request.getParameter("leaveTypeName");
                    boolean isPaid = "true".equals(request.getParameter("isPaid"));

                    if (name == null || name.trim().isEmpty()) {
                        message = "休日種別名は必須です";
                        break;
                    }
                    if (leaveTypeDao.exists(id)) {
                        message = "休日種別ID「" + id + "」は既に存在します";
                        break;
                    }

                    LeaveTypeBean newBean = new LeaveTypeBean(id, name, isPaid);
                    newBean.setCreatedBy(empId);
                    success = leaveTypeDao.insert(newBean);
                    message = success ? "休日種別を追加しました" : "休日種別の追加に失敗しました";
                    break;
                }

                case "update": {
                    int originalId = Integer.parseInt(request.getParameter("originalLeaveTypeId"));
                    int newId = Integer.parseInt(request.getParameter("leaveTypeId"));
                    String name = request.getParameter("leaveTypeName");
                    boolean isPaid = "true".equals(request.getParameter("isPaid"));

                    if (FIXED_IDS.contains(originalId)) {
                        message = "この休日種別は編集できません（ID: " + originalId + "）";
                        break;
                    }

                    if (name == null || name.trim().isEmpty()) {
                        message = "休日種別名は必須です";
                        break;
                    }

                    LeaveTypeBean updatedBean = new LeaveTypeBean(newId, name, isPaid);
                    updatedBean.setUpdatedBy(empId);
                    success = leaveTypeDao.update(originalId, updatedBean);
                    message = success ? "休日種別を更新しました" : "休日種別の更新に失敗しました";
                    break;
                }

                case "delete": {
                    int deleteId = Integer.parseInt(request.getParameter("leaveTypeId"));
                    if (FIXED_IDS.contains(deleteId)) {
                        message = "この休日種別は削除できません（ID: " + deleteId + "）";
                        break;
                    }

                    success = leaveTypeDao.delete(deleteId,empId);
                    message = success ? "休日種別を削除しました" : "休日種別の削除に失敗しました";
                    break;
                }

                default:
                    message = "不正な操作です";
            }
        } catch (Exception e) {
            e.printStackTrace();
            message = "処理中にエラーが発生しました";
        }

        request.setAttribute("message", message);
        request.setAttribute("success", success);

        doGet(request, response);
    }
}
