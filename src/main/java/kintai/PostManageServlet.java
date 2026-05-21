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
 * 役職管理機能を提供するサーブレット
 * 役職の一覧表示、追加、更新、削除を処理する
 */
@WebServlet("/postManage")
public class PostManageServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    
    private PostDao postDao = new PostDao(); // 役職DAOのインスタンス
    
    /**
     * GETリクエストの処理メソッド。
     * 役職一覧を表示する。
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
        
        // 管理者権限チェック (UserBeanのgetRole()からgetRoleId()へ変更)
        UserBean user = (UserBean) session.getAttribute("user");
        if (user.getRoleId() != 1) { // ROLEIDが1が管理者
            response.sendRedirect(request.getContextPath() + "/web/menu.jsp");
            return;
        }
        
        // アクションをチェック
        String action = request.getParameter("action");
        
        if ("history".equals(action)) {
            // 削除履歴一覧を表示
            List<PostBean> deletedPostList = postDao.findDeleted();
            request.setAttribute("deletedPostList", deletedPostList);
            
            RequestDispatcher dispatcher = request.getRequestDispatcher("/web/post_history.jsp");
            dispatcher.forward(request, response);
        } else {
            // 通常の役職管理画面
            List<PostBean> postList = postDao.findAll();
            List<PostBean> deletedPostList = postDao.findDeleted(); // 削除された役職一覧も取得
            
            request.setAttribute("postList", postList);
            request.setAttribute("deletedPostList", deletedPostList);
            
            // 役職管理画面にフォワード
            RequestDispatcher dispatcher = request.getRequestDispatcher("/web/post_manage.jsp");
            dispatcher.forward(request, response);
        }
    }
    
    /**
     * POSTリクエストの処理メソッド。
     * 役職の追加、更新、削除を処理する。
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
        System.out.println("PostManageServlet.doPost - action: " + action);
        
        boolean success = false;
        String message = "";
        
        try {
            switch (action) {
                case "add":
                    // 新規追加処理
                    String newPostId = request.getParameter("postId");
                    String newPostName = request.getParameter("postName");
                    System.out.println("PostManageServlet.doPost - add: postId=" + newPostId + ", postName=" + newPostName);
                    
                    // 入力チェック
                    if (newPostId == null || newPostId.trim().isEmpty() || 
                        newPostName == null || newPostName.trim().isEmpty()) {
                        message = "役職番号と役職名は必須入力です";
                        break;
                    }
                    // 新しいER図のPOST_IDの長さはVARCHAR(5)
                    if (newPostId.length() > 5) {
                        message = "役職番号は5文字以内で入力してください";
                        break;
                    }
                    
                    // 役職番号の重複チェック
                    if (postDao.exists(newPostId)) {
                        message = "役職番号「" + newPostId + "」は既に存在します";
                        break;
                    }
                    
                    PostBean newPost = new PostBean(newPostId, newPostName);
                    success = postDao.insert(newPost, user.getEmpId());
                    message = success ? "役職を追加しました" : "役職の追加に失敗しました";
                    System.out.println("PostManageServlet.doPost - add result: success=" + success + ", message=" + message);
                    break;
                    
                case "update":
                    // 更新処理
                    String updatePostId = request.getParameter("postId");
                    String updatePostName = request.getParameter("postName");
                    
                    // 入力チェック
                    if (updatePostName == null || updatePostName.trim().isEmpty()) {
                        message = "役職名は必須入力です";
                        break;
                    }
                    // POST_IDの長さチェックは不要（主キーなので変更されないため）
                    
                    PostBean updatePost = new PostBean(updatePostId, updatePostName);
                    success = postDao.update(updatePost, user.getEmpId());
                    message = success ? "役職を更新しました" : "役職の更新に失敗しました";
                    break;
                    
                case "delete":
                    // 削除処理
                    String deletePostId = request.getParameter("postId");
                    success = postDao.delete(deletePostId, user.getEmpId());
                    
                    if (success) {
                        message = "役職を削除しました";
                    } else {
                        message = "役職の削除に失敗しました。この役職に所属する社員が存在する可能性があります";
                    }
                    break;
                    
                case "restore":
                    // 復元処理
                    String restorePostId = request.getParameter("postId");
                    success = postDao.restore(restorePostId, user.getEmpId());
                    
                    if (success) {
                        message = "役職を復元しました";
                    } else {
                        message = "役職の復元に失敗しました";
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
        
        // 役職一覧を再取得して表示
        doGet(request, response);
    }
}
