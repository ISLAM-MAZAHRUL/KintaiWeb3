package kintai;

import java.io.IOException;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * パスワード変更機能を提供するサーブレット。
 * ユーザーが自身のパスワードを変更する処理を制御します。
 */
@WebServlet("/PasswordChangeServlet") // 全体ファイルまとめ.xlsx - Sheet1.pdf の PasswordChangeServlet.java に対応
public class PasswordChangeServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private UserDao userDao = new UserDao(); // UserDaoのインスタンス

    /**
     * GETリクエストの処理メソッド。
     * パスワード変更画面 (password_change.jsp) を表示します。
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

        // パスワード変更画面にフォワード
        RequestDispatcher dispatcher = request.getRequestDispatcher("/web/password_change.jsp");
        dispatcher.forward(request, response);
    }

    /**
     * POSTリクエストの処理メソッド。
     * パスワード変更フォームからのデータ送信を受け付け、パスワード更新処理を実行します。
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

        UserBean user = (UserBean) session.getAttribute("user");
        String empId = user.getEmpId(); // ログイン中の従業員番号

        // フォームからパラメータを取得
        String currentPassword = request.getParameter("currentPassword");
        String newPassword = request.getParameter("newPassword");
        String confirmNewPassword = request.getParameter("confirmNewPassword");

        String successMessage = null;
        String errorMessage = null;

        try {
            // サーバー側での入力チェック
            if (currentPassword == null || currentPassword.trim().isEmpty() ||
                newPassword == null || newPassword.trim().isEmpty() ||
                confirmNewPassword == null || confirmNewPassword.trim().isEmpty()) {
                errorMessage = "すべてのパスワードフィールドを入力してください";
            } else if (!newPassword.equals(confirmNewPassword)) {
                errorMessage = "新しいパスワードと確認用パスワードが一致しません";
            } else if (!isValidPassword(newPassword)) {  // ← 強度チェックをここに移動
            	 errorMessage = "パスワードは12文字以上で、大文字・小文字・数字・記号のうち3種類以上を含めてください。";
            } else if (newPassword.equals(currentPassword)) {
                 errorMessage = "新しいパスワードは現在のパスワードと同じにはできません";
            } else {
                // 現在のパスワードを検証
                boolean isCurrentPasswordCorrect = userDao.verifyCurrentPassword(empId, currentPassword);

                if (isCurrentPasswordCorrect) {
                    // パスワードを更新
                    boolean updateSuccess = userDao.updatePassword(empId, newPassword);

                    if (updateSuccess) {
                        successMessage = "パスワードが正常に変更されました";
                    } else {
                        errorMessage = "パスワードの変更に失敗しました";
                    }
                } else {
                    errorMessage = "現在のパスワードが正しくありません";
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            errorMessage = "パスワード変更中にエラーが発生しました";
        }

        // メッセージをリクエスト属性に設定
        request.setAttribute("successMessage", successMessage);
        request.setAttribute("errorMessage", errorMessage);

        // パスワード変更画面にフォワードして結果を表示
        RequestDispatcher dispatcher = request.getRequestDispatcher("/web/password_change.jsp");
        dispatcher.forward(request, response);
    }
    // パスワード強度チェック用メソッド
    private boolean isValidPassword(String password) {
        if (password == null || password.length() < 12) {
            return false;
        }
        boolean hasUpper = password.matches(".*[A-Z].*");
        boolean hasLower = password.matches(".*[a-z].*");
        boolean hasDigit = password.matches(".*[0-9].*");
        boolean hasSymbol = password.matches(".*[^A-Za-z0-9].*");
        int count = 0;
        if (hasUpper) count++;
        if (hasLower) count++;
        if (hasDigit) count++;
        if (hasSymbol) count++;
        return count >= 3;
    }
}
