package kintai;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * ログアウト処理を行うサーブレット
 * セッションを無効化し、ログイン画面にリダイレクトする
 */
@WebServlet("/logout")
public class LogoutServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    /**
     * GETリクエストの処理
     * ログアウト処理を実行する
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        doPost(request, response);
    }

    /**
     * POSTリクエストの処理
     * セッションを無効化してログイン画面へリダイレクト
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        // 現在のセッションを取得（存在しない場合は新規作成しない）
        HttpSession session = request.getSession(false);
        
        // セッションが存在する場合は無効化
        if (session != null) {
            // セッション内のすべての属性を削除
            session.invalidate();
        }
        
        // ログイン画面へリダイレクト
        response.sendRedirect(request.getContextPath() + "/web/login.jsp");
    }
}