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
 * アナウンス管理機能を提供するサーブレット
 * アナウンスの一覧表示、追加、更新、削除を処理する
 */
@WebServlet("/announcement")
public class AnnouncementServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    
    // DAOインスタンス
    private AnnouncementDao announcementDao = new AnnouncementDao();
    
    /**
     * GETリクエストの処理メソッド。
     * アナウンス一覧を表示する。
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
        
        if ("edit".equals(action)) {
            // 編集画面を表示
            String idStr = request.getParameter("id");
            if (idStr != null && !idStr.isEmpty()) {
                int announcementId = Integer.parseInt(idStr);
                AnnouncementBean announcement = announcementDao.findById(announcementId);
                request.setAttribute("announcement", announcement);
            }
        }
        
        // アナウンス一覧を取得
        List<AnnouncementBean> announcementList = announcementDao.findAll();
        request.setAttribute("announcementList", announcementList);
        
        // アナウンス管理画面にフォワード
        RequestDispatcher dispatcher = request.getRequestDispatcher("/web/announcement_manage.jsp");
        dispatcher.forward(request, response);
    }
    
    /**
     * POSTリクエストの処理メソッド。
     * アナウンスの追加、更新、削除を処理する。
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
        
        // 管理者権限チェック 
        UserBean user = (UserBean) session.getAttribute("user");
        if (user.getRoleId() != 1) { // ROLEIDが1が管理者
            response.sendRedirect(request.getContextPath() + "/web/menu.jsp");
            return;
        }
        
        // アクションを取得
        String action = request.getParameter("action");
        String message = "";
        
        try {
            if ("add".equals(action)) {
                // アナウンス追加
                message = addAnnouncement(request, user);
            } else if ("update".equals(action)) {
                // アナウンス更新
                message = updateAnnouncement(request, user);
            } else if ("delete".equals(action)) {
                // アナウンス削除
                message = deleteAnnouncement(request, user);
            }
        } catch (Exception e) {
            e.printStackTrace();
            message = "エラーが発生しました: " + e.getMessage();
        }
        
        // 結果メッセージをセッションに保存
        session.setAttribute("message", message);
        
        // admin_menu.jspにリダイレクト
        response.sendRedirect(request.getContextPath() + "/web/admin_menu.jsp");
    }
    
    /**
     * アナウンスを追加する
     * @param request HTTPリクエスト
     * @param user ユーザー情報
     * @return 結果メッセージ
     */
    private String addAnnouncement(HttpServletRequest request, UserBean user) {
        String title = request.getParameter("title");
        String content = request.getParameter("content");
        String isActiveStr = request.getParameter("isActive");
        String displayOrderStr = request.getParameter("displayOrder");
        
        // バリデーション
        if (title == null || title.trim().isEmpty()) {
            return "タイトルを入力してください";
        }
        if (content == null || content.trim().isEmpty()) {
            return "内容を入力してください";
        }
        
        AnnouncementBean announcement = new AnnouncementBean();
        announcement.setTitle(title.trim());
        announcement.setContent(content.trim());
        announcement.setActive("true".equals(isActiveStr) || "on".equals(isActiveStr));
        
        // 表示順序の設定
        int displayOrder = 0;
        if (displayOrderStr != null && !displayOrderStr.trim().isEmpty()) {
            try {
                displayOrder = Integer.parseInt(displayOrderStr.trim());
            } catch (NumberFormatException e) {
                // デフォルト値を使用
            }
        }
        announcement.setDisplayOrder(displayOrder);
        
        announcement.setCreatedBy(user.getEmpId());
        announcement.setUpdatedBy(user.getEmpId());
        
        boolean success = announcementDao.insert(announcement);
        return success ? "アナウンスを追加しました" : "アナウンスの追加に失敗しました";
    }
    
    /**
     * アナウンスを更新する
     * @param request HTTPリクエスト
     * @param user ユーザー情報
     * @return 結果メッセージ
     */
    private String updateAnnouncement(HttpServletRequest request, UserBean user) {
        String idStr = request.getParameter("announcementId");
        String title = request.getParameter("title");
        String content = request.getParameter("content");
        String isActiveStr = request.getParameter("isActive");
        String displayOrderStr = request.getParameter("displayOrder");
        
        // バリデーション
        if (idStr == null || idStr.trim().isEmpty()) {
            return "アナウンスIDが指定されていません";
        }
        if (title == null || title.trim().isEmpty()) {
            return "タイトルを入力してください";
        }
        if (content == null || content.trim().isEmpty()) {
            return "内容を入力してください";
        }
        
        try {
            int announcementId = Integer.parseInt(idStr);
            
            AnnouncementBean announcement = new AnnouncementBean();
            announcement.setAnnouncementId(announcementId);
            announcement.setTitle(title.trim());
            announcement.setContent(content.trim());
            announcement.setActive("true".equals(isActiveStr) || "on".equals(isActiveStr));
            
            // 表示順序の設定
            int displayOrder = 0;
            if (displayOrderStr != null && !displayOrderStr.trim().isEmpty()) {
                try {
                    displayOrder = Integer.parseInt(displayOrderStr.trim());
                } catch (NumberFormatException e) {
                    // デフォルト値を使用
                }
            }
            announcement.setDisplayOrder(displayOrder);
            
            announcement.setUpdatedBy(user.getEmpId());
            
            boolean success = announcementDao.update(announcement);
            return success ? "アナウンスを更新しました" : "アナウンスの更新に失敗しました";
            
        } catch (NumberFormatException e) {
            return "無効なアナウンスIDです";
        }
    }
    
    /**
     * アナウンスを削除する
     * @param request HTTPリクエスト
     * @param user ユーザー情報
     * @return 結果メッセージ
     */
    private String deleteAnnouncement(HttpServletRequest request, UserBean user) {
        String idStr = request.getParameter("announcementId");
        
        if (idStr == null || idStr.trim().isEmpty()) {
            return "アナウンスIDが指定されていません";
        }
        
        try {
            int announcementId = Integer.parseInt(idStr);
            boolean success = announcementDao.delete(announcementId, user.getEmpId());
            return success ? "アナウンスを削除しました" : "アナウンスの削除に失敗しました";
            
        } catch (NumberFormatException e) {
            return "無効なアナウンスIDです";
        }
    }
}