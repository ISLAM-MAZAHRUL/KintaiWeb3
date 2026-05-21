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
 * 従業員管理機能を提供するサーブレット
 * 従業員の一覧表示、追加、更新、削除を処理する
 */
@WebServlet("/empManage")
public class EmpManageServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    
    // DAOインスタンス
    private EmpDao empDao = new EmpDao();
    private DeptDao deptDao = new DeptDao();
    private PostDao postDao = new PostDao();
    private RoleDao roleDao = new RoleDao(); // 新規追加: RoleDao
    
    /**
     * GETリクエストの処理メソッド。
     * 従業員一覧を表示する。
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
//        UserBean user = (UserBean) session.getAttribute("user");
//        if (user.getRoleId() != 1) { // ROLEIDが1が管理者
//            response.sendRedirect(request.getContextPath() + "/web/menu.jsp");
//            return;
//        }
        
        // アクションをチェック
        String action = request.getParameter("action");
        
        if ("history".equals(action)) {
            // 削除履歴一覧を表示
            List<EmpBean> deletedEmpList = empDao.findDeleted();
            request.setAttribute("deletedEmpList", deletedEmpList);
            
            RequestDispatcher dispatcher = request.getRequestDispatcher("/web/emp_history.jsp");
            dispatcher.forward(request, response);
        } else {
            // 通常の従業員管理画面
            List<EmpBean> empList = empDao.findAll();
            List<DeptBean> deptList = deptDao.findAll();
            List<PostBean> postList = postDao.findAll();
            List<RoleBean> roleList = roleDao.findAll();
            List<EmpBean> deletedEmpList = empDao.findDeleted(); // 削除された従業員一覧も取得
            
            request.setAttribute("empList", empList);
            request.setAttribute("deptList", deptList);
            request.setAttribute("postList", postList);
            request.setAttribute("roleList", roleList);
            request.setAttribute("deletedEmpList", deletedEmpList);
            
            // 従業員管理画面にフォワード
            RequestDispatcher dispatcher = request.getRequestDispatcher("/web/emp_manage.jsp");
            dispatcher.forward(request, response);
        }
    }
    
    /**
     * POSTリクエストの処理メソッド。
     * 従業員の追加、更新、削除を処理する。
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
        
        boolean success = false;
        String message = "";
        
        try {
            switch (action) {
                case "add":
                    // 新規追加処理
                    String newEmpId = request.getParameter("empId");
                    String newEmpName = request.getParameter("empName");
                    String newDeptId = request.getParameter("deptId");
                    String newPostId = request.getParameter("postId");
                    String newRoleIdStr = request.getParameter("roleId"); // 旧roleから変更
                    // String newGradeIdStr = request.getParameter("gradeId"); // Grade機能削除により不要
                    String newPass = request.getParameter("pass");
                    String newMail = request.getParameter("mail"); // 新規追加
                    String newEmpDateStr = request.getParameter("empDate"); // 新規追加

                    // 入力チェック (最低限のチェック、詳細なビジネスロジックはDAOやサービス層で)
                    if (newEmpId == null || newEmpId.trim().isEmpty() || 
                        newEmpName == null || newEmpName.trim().isEmpty() ||
                        newDeptId == null || newDeptId.trim().isEmpty() ||
                        newPostId == null || newPostId.trim().isEmpty() ||
                        newRoleIdStr == null || newRoleIdStr.trim().isEmpty() ||
                        newPass == null || newPass.trim().isEmpty()) {
                        message = "必須項目をすべて入力してください";
                        break;
                    }

                    // 従業員番号の重複チェック
                    if (empDao.exists(newEmpId)) {
                        message = "従業員番号「" + newEmpId + "」は既に存在します";
                        break;
                    }
                    
                    EmpBean newEmp = new EmpBean();
                    newEmp.setEmpId(newEmpId);
                    newEmp.setEmpName(newEmpName);
                    newEmp.setDeptId(newDeptId);
                    newEmp.setPostId(newPostId);
                    newEmp.setRoleId(Integer.parseInt(newRoleIdStr)); // 旧setRoleから変更
                    newEmp.setEmpType("正社員"); // Grade機能削除によりデフォルト値を設定
                    newEmp.setPass(newPass);
                    newEmp.setMail(newMail); // 新規追加
                    newEmp.setActive(true); // 新規追加時はアクティブに設定
                    // EMPDATEはnull許容として、JSPからの入力がない場合はnull
                    if (newEmpDateStr != null && !newEmpDateStr.trim().isEmpty()) {
                        newEmp.setEmpDate(java.time.LocalDate.parse(newEmpDateStr));
                    }
                    
                    success = empDao.insert(newEmp);
                    message = success ? "従業員を追加しました" : "従業員の追加に失敗しました";
                    break;
                    
                case "update":
                    // 更新処理
                    String updateEmpId = request.getParameter("empId");
                    String updateEmpName = request.getParameter("empName");
                    String updateDeptId = request.getParameter("deptId");
                    String updatePostId = request.getParameter("postId");
                    String updateRoleIdStr = request.getParameter("roleId"); // 旧roleから変更
                    // String updateGradeIdStr = request.getParameter("gradeId"); // Grade機能削除により不要
                    String updatePass = request.getParameter("pass"); // パスワードは更新時も入力させる想定
                    String updateMail = request.getParameter("mail"); // 新規追加
                    String updateEmpDateStr = request.getParameter("empDate"); // 新規追加
                    
                    // 入力チェック
                    if (updateEmpName == null || updateEmpName.trim().isEmpty() ||
                        updateDeptId == null || updateDeptId.trim().isEmpty() ||
                        updatePostId == null || updatePostId.trim().isEmpty() ||
                        updateRoleIdStr == null || updateRoleIdStr.trim().isEmpty() ||
                        updatePass == null || updatePass.trim().isEmpty()) { // パスワードも必須
                        message = "必須項目をすべて入力してください";
                        break;
                    }
                    
                    EmpBean updateEmp = new EmpBean();
                    updateEmp.setEmpId(updateEmpId);
                    updateEmp.setEmpName(updateEmpName);
                    updateEmp.setDeptId(updateDeptId);
                    updateEmp.setPostId(updatePostId);
                    updateEmp.setRoleId(Integer.parseInt(updateRoleIdStr)); // 旧setRoleから変更
                    updateEmp.setEmpType("正社員"); // Grade機能削除によりデフォルト値を設定
                    updateEmp.setPass(updatePass);
                    updateEmp.setMail(updateMail); // 新規追加
                    if (updateEmpDateStr != null && !updateEmpDateStr.trim().isEmpty()) {
                        updateEmp.setEmpDate(java.time.LocalDate.parse(updateEmpDateStr));
                    } else {
                        updateEmp.setEmpDate(null); // 入力がない場合はnull
                    }
                    
                    success = empDao.update(updateEmp);
                    message = success ? "従業員情報を更新しました" : "従業員情報の更新に失敗しました";
                    break;
                    
                case "delete":
                    // 削除処理
                    String deleteEmpId = request.getParameter("empId");
                    success = empDao.delete(deleteEmpId);
                    
                    if (success) {
                        message = "従業員を削除しました";
                    } else {
                        message = "従業員の削除に失敗しました。この従業員に関連するデータが存在する可能性があります。"; // メッセージを一般化
                    }
                    break;
                    
                case "restore":
                    // 復元処理
                    String restoreEmpId = request.getParameter("empId");
                    success = empDao.restore(restoreEmpId);
                    
                    if (success) {
                        message = "従業員を復元しました";
                    } else {
                        message = "従業員の復元に失敗しました";
                    }
                    break;
                    
                default:
                    message = "不正な操作です";
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
            message = "入力された数値（ロールID、等級番号）が不正です";
        } catch (java.time.format.DateTimeParseException e) {
            e.printStackTrace();
            message = "入社年月日の形式が不正です。YYYY-MM-DD形式で入力してください。";
        } catch (Exception e) {
            e.printStackTrace();
            message = "処理中にエラーが発生しました";
        }
        
        // 処理結果をリクエスト属性に設定
        request.setAttribute("message", message);
        request.setAttribute("success", success);
        
        // 従業員一覧を再取得して表示
        doGet(request, response);
    }
}
