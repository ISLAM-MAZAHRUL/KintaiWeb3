package kintai;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * ログイン処理を受け持つサーブレット。
 */
@WebServlet("/LoginCheck")
public class LoginCheckServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	// 勤怠データアクセス用のDAOインスタンス
	private WorkTimeDao workTimeDao = new WorkTimeDao();
		

    /**
     * POSTリクエストの処理。
     * ログインフォームからの送信を受け付け、認証を行う。
     * @param request HTTPリクエストオブジェクト
     * @param response HTTPレスポンスオブジェクト
     * @throws ServletException サーブレット例外
     * @throws IOException 入出力例外
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        // 1. JSPから送信されたパラメータを取得
        String empId = request.getParameter("empId");
        String password = request.getParameter("password");
        
        // 入力値の検証（サーバー側でも検証を行う）
        if (empId == null || empId.trim().isEmpty() || 
            password == null || password.trim().isEmpty()) {
            request.setAttribute("errorMessage", "従業員番号とパスワードを入力してください");
            RequestDispatcher dispatcher = request.getRequestDispatcher("/web/login.jsp");
            dispatcher.forward(request, response);
            return;
        }

        // 2. UserDaoをインスタンス化
        UserDao userDao = new UserDao();
        DeptDao deptDao = new DeptDao(); // 既存のDeptDaoを使用
        PostDao postDao = new PostDao(); // 既存のPostDaoを使用

        // 3. UserDaoを使って、データベースにユーザーが存在するか問い合わせる
        UserBean user = userDao.findByLoginInfo(empId, password);

        // 4. 認証結果に応じて処理を分岐
        if (user != null) {
            // --- ログイン成功の処理 ---

            // 新しいセッションを開始し、ユーザー情報を保存する
            HttpSession session = request.getSession();
            session.setAttribute("user", user);

            String deptName = null;
            String postName = null;

            // 部署名を取得 (UserBeanのgetDeptId()使用)
            if (user.getDeptId() != null && !user.getDeptId().isEmpty()) {
                DeptBean dept = deptDao.findByDeptId(user.getDeptId());
                if (dept != null) {
                    deptName = dept.getDeptName();
                }
            }
            // 役職名を取得 (UserBeanのgetPostId()使用)
            if (user.getPostId() != null && !user.getPostId().isEmpty()) {
                PostBean post = postDao.findByPostId(user.getPostId());
                if (post != null) {
                    postName = post.getPostName();
                }
            }
            
            // 取得した部署名と役職名をセッションに保存
            session.setAttribute("deptName", deptName != null ? deptName : "情報なし");
            session.setAttribute("postName", postName != null ? postName : "情報なし");
            
            LocalDate today = LocalDate.now(); // 今日の日付を取得
            
            // データベースから今日の勤怠データと休憩データリストを取得
    		WorkTimeBean workTime = workTimeDao.findWorkTimeByDate(empId, today);
    		
    		// JSPに渡すための勤怠データ用のマップを作成
    		Map<String, String> workTimeData = new HashMap<>();
    		// 時間表示用のフォーマッター（HH:mm形式）
    		DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
    		
    		// 勤怠データが存在する場合、出勤・退勤時刻をフォーマットしてマップに格納
    		if (workTime != null) {
    			// 出勤時刻が記録されている場合
    			if (workTime.getClockIn() != null) {
    				workTimeData.put("clockInTime", workTime.getClockIn().toLocalTime().format(timeFormatter));
    			}
    			// 退勤時刻が記録されている場合
    			if (workTime.getClockOut() != null) {
    				workTimeData.put("clockOutTime", workTime.getClockOut().toLocalTime().format(timeFormatter));
    			}
    		}
    		
    		// 承認待ち休暇申請を取得
    		LeaveRecDao leaveRecDao = new LeaveRecDao();
    		List<LeaveRecBean> pendingLeaves = new ArrayList<>();
    		try {
    		    pendingLeaves = leaveRecDao.findPendingByApprover(user.getEmpId());
    		} catch (Exception e) {
    		    e.printStackTrace();
    		}

    		// セッションに追加
    		session.setAttribute("pendingLeaves", pendingLeaves);

    		
    		// JSPに渡すデータをリクエスト属性として設定
    		session.setAttribute("workTimeData", workTimeData);


            // ユーザーの役割(ROLEID)に応じてリダイレクト先を決定 
            if (user.getRoleId() == 1) { // 管理部の従業員（ROLEID=1）
                // 管理部の場合 -> 管理者用メニューServletにリダイレクト
                response.sendRedirect(request.getContextPath() + "/AdminMenuServlet");
            } else { // 一般従業員（ROLEID=0）および部長（ROLEID=2）
                // 一般従業員および部長の場合 -> 通常メニューにリダイレクト
                response.sendRedirect(request.getContextPath() + "/web/menu.jsp");
            }

        } else {
            // --- ログイン失敗の処理 ---
            request.setAttribute("errorMessage", "従業員番号またはパスワードが正しくありません");
            RequestDispatcher dispatcher = request.getRequestDispatcher("/web/login.jsp");
            dispatcher.forward(request, response);
        }
    }
    
    /**
     * GETリクエストの処理。
     * ログイン画面へリダイレクト。
     * @param request HTTPリクエストオブジェクト
     * @param response HTTPレスポンスオブジェクト
     * @throws ServletException サーブレット例外
     * @throws IOException 入出力例外
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        // GETリクエストの場合はログイン画面へリダイレクト
        response.sendRedirect(request.getContextPath() + "/web/login.jsp");
    }
}
