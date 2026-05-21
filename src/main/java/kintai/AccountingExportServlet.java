package kintai;

import java.io.IOException;
import java.io.OutputStream;
import java.time.YearMonth;
import java.util.List;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/AccountingExportServlet")
public class AccountingExportServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private KinmuManageDao kinmuDao = new KinmuManageDao();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        doPost(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // セッションチェック
        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute("user") == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        // ログインユーザー取得
        UserBean loginUser = (UserBean) session.getAttribute("user");

        // 対象月取得
        String startMonth = request.getParameter("startMonth");
        String endMonth = request.getParameter("endMonth");

        // 確認ログ
        System.out.println("startMonth = " + startMonth);
        System.out.println("endMonth = " + endMonth);

        // デフォルト値設定
        if (startMonth == null || startMonth.isEmpty()) {
            startMonth = YearMonth.now().toString();
        }

        if (endMonth == null || endMonth.isEmpty()) {
            endMonth = startMonth;
        }

        // 補完後ログ
        System.out.println("final startMonth = " + startMonth);
        System.out.println("final endMonth = " + endMonth);

        try {

            // データ取得
        	// action取得
        	String action = request.getParameter("action");
        	if (action == null) action = "download";

        	// データ取得
        	List<KinmuManageBean.WorkAlloc> list;

        	if (loginUser.getRoleId() == 1 || loginUser.getRoleId() == 2) {

        	    list = kinmuDao.findMonthlyByAllEmpRange(startMonth, endMonth);

        	} else {

        	    list = kinmuDao.findMonthlyByEmpRange(
        	            loginUser.getEmpId(),
        	            startMonth,
        	            endMonth
        	    );
        	}

        	// preview mode
        	if ("preview".equals(action)) {

        	    request.setAttribute("results", list);
        	    request.setAttribute("startMonth", startMonth);
        	    request.setAttribute("endMonth", endMonth);

        	    request.getRequestDispatcher("/web/kinmu_acc_export.jsp")
        	           .forward(request, response);

        	    return;
        	}
        	// Excel download
        	if ("excel".equals(action)) {
        	    // ExcelはCSVと同じデータ、拡張子だけ変える
        	    String filename = "会計用勤怠_" + startMonth + "_" + endMonth + ".xlsx";
        	    String encodedFilename = java.net.URLEncoder.encode(filename, "UTF-8")
        	                                .replace("+", "%20");
        	    response.setContentType("application/vnd.ms-excel; charset=UTF-8");
        	    response.setHeader("Content-Disposition",
        	        "attachment; filename*=UTF-8''" + encodedFilename);

        	    OutputStream out = response.getOutputStream();
        	    out.write(new byte[]{(byte)0xEF, (byte)0xBB, (byte)0xBF});
        	    
        	    StringBuilder sb = new StringBuilder();
        	    sb.append("社員番号,社員氏名,年月,プロジェクト名,勤務時間\n");
        	    for (KinmuManageBean.WorkAlloc wa : list) {
        	        sb.append(wa.getEmpId()).append(",");
        	        sb.append(wa.getEmpName()).append(",");
        	        sb.append(wa.getYearMonth()).append(",");
        	        sb.append(wa.getProjectName()).append(",");
        	        sb.append(String.format("%.1f", wa.getWorkHours())).append("\n");
        	    }
        	    out.write(sb.toString().getBytes("UTF-8"));
        	    out.flush();
        	    out.close();
        	    return;
        	}

            // 件数確認
            System.out.println("list size = " + list.size());

            // CSV生成
            StringBuilder sb = new StringBuilder();

            sb.append("社員番号,社員氏名,年月,プロジェクトID,勤務時間\n");

            for (KinmuManageBean.WorkAlloc wa : list) {

                System.out.println(
                        wa.getEmpId() + " / "
                        + wa.getYearMonth() + " / "
                        + wa.getProjectName()
                );

                sb.append(wa.getEmpId()).append(",");
                sb.append(wa.getEmpName()).append(",");
                sb.append(wa.getYearMonth()).append(",");
                sb.append(wa.getProjectName()).append(",");
                sb.append(String.format("%.1f", wa.getWorkHours())).append("\n");
            }

            // ファイル名
            String filename =
                    "会計用勤怠_" + startMonth + "_" + endMonth + ".csv";

            String encodedFilename =
                    java.net.URLEncoder.encode(filename, "UTF-8")
                            .replace("+", "%20");

            // レスポンス設定
            response.setContentType("text/csv; charset=UTF-8");

            response.setHeader(
                    "Content-Disposition",
                    "attachment; filename*=UTF-8''" + encodedFilename
            );

            // CSV出力
            OutputStream out = response.getOutputStream();

            // BOM
            out.write(new byte[]{
                    (byte) 0xEF,
                    (byte) 0xBB,
                    (byte) 0xBF
            });

            out.write(sb.toString().getBytes("UTF-8"));

            out.flush();
            out.close();

        } catch (Exception e) {

            e.printStackTrace();

            response.sendError(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "エクスポート中にエラーが発生しました: " + e.getMessage()
            );
        }
    }
}