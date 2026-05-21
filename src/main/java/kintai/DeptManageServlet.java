package kintai;

import java.io.IOException;
import java.util.List;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * 部署管理機能を提供するサーブレット
 * 部署の一覧表示、追加、更新、削除を処理する
 */
@WebServlet("/deptManage")
public class DeptManageServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    
    private DeptDao deptDao = new DeptDao(); // 部署DAOのインスタンス

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
            System.out.println("DeptManageServlet.doGet - Calling findDeleted()");
            List<DeptBean> deletedDeptList = deptDao.findDeleted();
            System.out.println("DeptManageServlet.doGet - findDeleted() returned " + deletedDeptList.size() + " items");
            request.setAttribute("deletedDeptList", deletedDeptList);
            
            RequestDispatcher dispatcher = request.getRequestDispatcher("/web/dept_history.jsp");
            dispatcher.forward(request, response);
        } else {
            // 通常の部署管理画面
            List<DeptBean> deptList = deptDao.findAll();
            List<DeptBean> deletedDeptList = deptDao.findDeleted(); // 削除された部署一覧も取得
            
            request.setAttribute("deptList", deptList);
            request.setAttribute("deletedDeptList", deletedDeptList);
            
            // 部署管理画面にフォワード
            RequestDispatcher dispatcher = request.getRequestDispatcher("/web/dept_manage.jsp");
            dispatcher.forward(request, response);
        }
    }
    
    /**
     * POSTリクエストの処理メソッド。
     * 部署の追加、更新、削除を処理する。
     * @param request HTTPリクエストオブジェクト
     * @param response HTTPレスポンスオブジェクト
     * @throws ServletException サーブレット例外
     * @throws IOException 入出力例外
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        // セッションチェック
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            response.sendRedirect(request.getContextPath() + "/web/login.jsp");
            return;
        }
        
        // 管理者権限チェック (UserBeanのgetRole()からgetRoleId()へ変更)
        UserBean user = (UserBean) session.getAttribute("user");
        if (user.getRoleId() != 1) { // ROLEIDが1が管理者
            response.sendRedirect(request.getContextPath() + "/web/menu.jsp");
            return;
        }
        
        // アクションを取得
        String action = request.getParameter("action");
        System.out.println("DeptManageServlet.doPost - action: " + action);
        
        boolean success = false;
        String message = "";
        
        try {
            switch (action) {
                case "add":
                    // 新規追加処理
                    String newDeptId = request.getParameter("deptId");
                    String newDeptName = request.getParameter("deptName");
                    System.out.println("DeptManageServlet.doPost - add: deptId=" + newDeptId + ", deptName=" + newDeptName);
                    
                    // 入力チェック
                    if (newDeptId == null || newDeptId.trim().isEmpty() || 
                        newDeptName == null || newDeptName.trim().isEmpty()) {
                        message = "部署番号と部署名は必須入力です";
                        break;
                    }
                    // 新しいER図のDEPT_IDの長さはVARCHAR(5)
                    if (newDeptId.length() > 5) {
                        message = "部署番号は5文字以内で入力してください";
                        break;
                    }

                    // 部署番号の重複チェック
                    if (deptDao.exists(newDeptId)) {
                        message = "部署番号「" + newDeptId + "」は既に存在します";
                        break;
                    }
                    
                    DeptBean newDept = new DeptBean(newDeptId, newDeptName);
                    success = deptDao.insert(newDept, user.getEmpId());
                    message = success ? "部署を追加しました" : "部署の追加に失敗しました";
                    break;
                    
                case "update":
                    // 更新処理
                    String updateDeptId = request.getParameter("deptId");
                    String updateDeptName = request.getParameter("deptName");
                    
                    // 入力チェック
                    if (updateDeptName == null || updateDeptName.trim().isEmpty()) {
                        message = "部署名は必須入力です";
                        break;
                    }
                    // DEPT_IDの長さチェックは不要（主キーなので変更されないため）
                    
                    DeptBean updateDept = new DeptBean(updateDeptId, updateDeptName);
                    success = deptDao.update(updateDept, user.getEmpId());
                    message = success ? "部署を更新しました" : "部署の更新に失敗しました";
                    break;
                    
                case "delete":
                    // 削除処理
                    String deleteDeptId = request.getParameter("deptId");
                    System.out.println("DeptManageServlet.doPost - delete: deptId=" + deleteDeptId + ", user=" + user.getEmpId());
                    success = deptDao.delete(deleteDeptId, user.getEmpId());
                    System.out.println("DeptManageServlet.doPost - delete result: " + success);
                    
                    if (success) {
                        message = "部署を削除しました";
                    } else {
                        message = "部署の削除に失敗しました。この部署に所属する社員が存在する可能性があります";
                    }
                    break;
                    
                case "restore":
                    // 復元処理
                    String restoreDeptId = request.getParameter("deptId");
                    success = deptDao.restore(restoreDeptId, user.getEmpId());
                    
                    if (success) {
                        message = "部署を復元しました";
                    } else {
                        message = "部署の復元に失敗しました";
                    }
                    break;
                    
                default:
                    message = "不正な操作です";
            }
        } catch (Exception e) {
            e.printStackTrace();
            message = "処理中にエラーが発生しました";
        }
        
        // 処理結果をリクエスト属性に設定
        request.setAttribute("message", message);
        request.setAttribute("success", success);
        
        // 部署一覧を再取得して表示
        doGet(request, response);
    }
}
