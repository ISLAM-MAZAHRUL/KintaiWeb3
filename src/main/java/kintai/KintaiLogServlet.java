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

@WebServlet("/kintaiLog") //URLパターン「/kintaiLog」にマッピング
public class KintaiLogServlet extends HttpServlet{
	private static final long serialVersionUID = 1L;
	
	// 勤怠データアクセス用のDAOインスタンス
    private WorkTimeDao workTimeDao = new WorkTimeDao();
    
    private final EmpDao empDao = new EmpDao();
    private final DeptDao deptDao = new DeptDao();
    private final PostDao postDao = new PostDao();
    
    /**
     * GETリクエストの処理メソッド
     * 勤怠登録画面(dakoku.jsp)を表示する前に、
     * ログイン中のユーザーの今日の勤怠データをデータベースから取得してJSPに渡す。
     *
     * @param request HTTPリクエストオブジェクト
     * @param response HTTPレスポンスオブジェクト
     * @throws ServletException サーブレット例外
     * @throws IOException 入出力例外
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        // セッション情報を取得（既存のセッションのみ、新規作成はしない）
        HttpSession session = request.getSession(false);

        // セッションが存在しない、またはユーザー情報がない場合はログイン画面にリダイレクト
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

            request.setAttribute("empList", empDao.findByFilters(deptId, postId));
            request.setAttribute("deptList", deptDao.findAll());
            request.setAttribute("postList", postDao.findAll());

            request.setAttribute("selectedDept", deptId);
            request.setAttribute("selectedPost", postId);
            request.setAttribute("selectedEmpId", empId);
            
            // --- 新規追加: 勤怠ログを取得 ---
            if (empId != null && !empId.isEmpty()) {
                List<WorkTimeBean> workTimeList = workTimeDao.findWorkTimeLog(empId);
                request.setAttribute("workTime", workTimeList);
            }

           
            
        } catch (Exception e) {
            request.setAttribute("message", "初期表示に失敗しました: + print" );
            request.setAttribute("success", false);
        }

        doPost(request,response);
        
    }
	

    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
    	
    	// menu.jspにフォワード（画面表示）
        RequestDispatcher dispatcher = request.getRequestDispatcher("/web/kintai_log.jsp");
        dispatcher.forward(request, response);
    	
    }
}
