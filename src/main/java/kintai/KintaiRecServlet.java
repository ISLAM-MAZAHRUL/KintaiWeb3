package kintai;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
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



class KintaiRecDto {
    String kintaiDate;
    String dayOfWeek;   // ★ 曜日追加
    String attendanceType;
    String clockIn;
    String clockOut;
    String totalBreakTimeFormatted;
    String actualWorkTimeFormatted;
    String overtimeFormatted;
    String nightovertimeFormatted;

    // イベント情報
    String workStatus;  
    String eventName;   

    // プロジェクト工数合計
    String projectTotalHours; 
}



/**
 * 勤怠記録表示・取得・保存をまとめて提供するサーブレット。
 */
@WebServlet("/KintaiRecServlet")
public class KintaiRecServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private final KintaiRecDao kintaiRecDao = new KintaiRecDao();
    private final DeptDao deptDao = new DeptDao();
    private final PostDao postDao = new PostDao();
    private final EmpDao empDao = new EmpDao();

    // ======================
    // GET リクエスト処理
    // ======================
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

    	String action = request.getParameter("action");
    	System.out.println("[DEBUG] action=" + action);

    	if ("getAttendance".equals(action)) {
    	    handleGetAttendance(request, response);
    	    return;
    	} else if ("projectAnalysis".equals(action)) {
    	    handleProjectAnalysis(request, response);
    	    return;
    	} else if ("list".equals(action)) {   // ★ 月次一覧の JSON 返却を追加
    	    handleListAttendance(request, response);
    	    return;
    	}


        // 一覧画面表示
        handleDisplayPage(request, response);
    }

    /**
     * 勤怠1日分を取得して JSON 返却
     */
    private void handleGetAttendance(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        String dateStr = request.getParameter("date");
        String empId = request.getParameter("empId");

        response.setContentType("application/json; charset=UTF-8");
        PrintWriter out = response.getWriter();

        try {
            LocalDate date = LocalDate.parse(dateStr);

            // DAOで勤怠本体・休憩・プロジェクトを取得
            KintaiRecBean rec = kintaiRecDao.getRecord(empId, date);

            if (rec != null) {
                out.write(rec.toJson());
            } else {
                out.write("{}");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.write("{\"success\":false,\"message\":\"取得に失敗しました\"}");
        }
    }

    // ======================
    // POST リクエスト処理
    // ======================
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String action = request.getParameter("action");
        if ("saveAttendance".equals(action)) {
            handleSaveAttendance(request, response);
            return;
        }

        // それ以外は一覧表示に委譲
        doGet(request, response);
    }

    /**
     * 勤怠1日分を保存（JSON受信）
     */
    private void handleSaveAttendance(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        response.setContentType("application/json; charset=UTF-8");
        PrintWriter out = response.getWriter();

        try (BufferedReader br = request.getReader()) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);

            // JSON → Bean
            KintaiRecBean rec = KintaiRecBean.fromJson(sb.toString());

            // ログインユーザー情報を取得
            UserBean user = (UserBean) session.getAttribute("user");
            String loginEmpId = user.getEmpId();
            
            // DAOで保存（勤怠本体＋休憩＋プロジェクト）
            kintaiRecDao.saveOrUpdate(rec, loginEmpId);

            out.write("{\"success\":true}");
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.write("{\"success\":false,\"message\":\"保存に失敗しました\"}");
        }
    }

    // ======================
    // 一覧画面表示処理
    // ======================
    private void handleDisplayPage(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            response.sendRedirect(request.getContextPath() + "/web/login.jsp");
            return;
        }

        UserBean user = (UserBean) session.getAttribute("user");
        String loggedInEmpId = user.getEmpId();
        int userRoleId = user.getRoleId();

        // モード判定
        String viewMode = request.getParameter("mode");
        boolean isSelfMode = "self".equals(viewMode);
        if (userRoleId == 0) { // 一般社員は強制自分モード
            isSelfMode = true;
        }

        // フィルター条件
        String empIdFilter = request.getParameter("empNoFilter");
        String deptIdFilter = request.getParameter("deptNoFilter");
        String postIdFilter = request.getParameter("postNoFilter");
        String startDateStr = request.getParameter("startDate");
        String endDateStr = request.getParameter("endDate");
        
        LocalDate startDate = null;
        LocalDate endDate = null;

        try {
            if (startDateStr != null && !startDateStr.trim().isEmpty()) {
                startDate = LocalDate.parse(startDateStr);
            }
            if (endDateStr != null && !endDateStr.trim().isEmpty()) {
                endDate = LocalDate.parse(endDateStr);
            }
        } catch (DateTimeParseException e) {
            request.setAttribute("errorMessage", "日付の形式が不正です。YYYY-MM-DD形式で入力してください。");
        }

        if (startDate == null && endDate == null) {
            startDate = LocalDate.now().withDayOfMonth(1);
            endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
        }

        // === 検索対象従業員 ===
        List<String> targetEmpIds = new ArrayList<>();
        String selectedEmpId;

        if (userRoleId == 0) {
            // 一般社員 → 自分のみ
            targetEmpIds.add(loggedInEmpId);
            selectedEmpId = loggedInEmpId;
        } else {
        	// 管理者 / 部長
        	if (empIdFilter != null && !empIdFilter.isEmpty()) {

        	    targetEmpIds.add(empIdFilter);
        	    selectedEmpId = empIdFilter;

        	} else {

        	    // Admin → 全社員
        	    if (userRoleId == 1) {

        	        targetEmpIds = null;

        	    }
        	    // 部長 → 自部署のみ
        	    else if (userRoleId == 2) {

        	        List<EmpBean> deptEmployees =
        	                empDao.findByFilters(user.getDeptId(), null);

        	        for (EmpBean emp : deptEmployees) {
        	            targetEmpIds.add(emp.getEmpId());
        	        }
        	    }

        	    selectedEmpId = loggedInEmpId;
        	}

        // 勤怠データ取得
        List<KintaiRecBean> kintaiRecords;
        try {
            kintaiRecords = kintaiRecDao.getKintaiRecords(
                    targetEmpIds, deptIdFilter, postIdFilter, startDate, endDate, userRoleId);
        } catch (Exception e) {
            e.printStackTrace();
            kintaiRecords = new ArrayList<>();
            request.setAttribute("errorMessage", "勤怠記録の取得中にエラーが発生しました。");
        }

        // Map 化（empId→date→bean）
        Map<String, Map<LocalDate, KintaiRecBean>> recMapByEmp = new HashMap<>();
        for (KintaiRecBean rec : kintaiRecords) {
            recMapByEmp
                .computeIfAbsent(rec.getEmpId(), k -> new HashMap<>())
                .put(rec.getKintaiDate(), rec);
        }
        
        // 統計データを取得
        String currentMonth = java.time.YearMonth.now().toString();
        MonthlySummaryBean monthlySummary = null;
        try {
            monthlySummary = kintaiRecDao.getMonthlySummary(user.getEmpId(), currentMonth);
        } catch (Exception e) {
            e.printStackTrace();
        }
        request.setAttribute("monthlySummary", monthlySummary);

        // === 今日の勤怠状況を取得（出勤予定・欠勤・休暇など） ===
        try {
            LocalDate today = LocalDate.now();
            int scheduledCount = kintaiRecDao.getScheduledEmployeeCount(today);
            int absentCount    = kintaiRecDao.getAbsentEmployeeCount(today);
            int vacationCount  = kintaiRecDao.getVacationEmployeeCount(today);

            request.setAttribute("scheduledCount", scheduledCount);
            request.setAttribute("absentCount", absentCount);
            request.setAttribute("vacationCount", vacationCount);

            System.out.println("[DEBUG] 今日の勤怠状況: 出勤予定=" + scheduledCount +
                    ", 欠勤=" + absentCount + ", 休暇=" + vacationCount);

        } catch (Exception e) {
            e.printStackTrace();
            request.setAttribute("scheduledCount", 0);
            request.setAttribute("absentCount", 0);
            request.setAttribute("vacationCount", 0);
        }
        
     // === コンプライアンスチェック（自分モード or 一般社員） ===
        ComplianceChecker complianceChecker = new ComplianceChecker();
        ComplianceCheckResult complianceResult = complianceChecker.performComprehensiveCheck(kintaiRecords, loggedInEmpId);

        // ユーザー情報を補完
        complianceResult.setEmpName(user.getName());

        // 部署名・役職名を DAO から解決
        DeptBean dept = deptDao.findByDeptId(user.getDeptId());
        PostBean post = postDao.findByPostId(user.getPostId());

        complianceResult.setDeptName(dept != null ? dept.getDeptName() : "不明");
        complianceResult.setPostName(post != null ? post.getPostName() : "不明");

        request.setAttribute("complianceResult", complianceResult);


        // === 法令遵守違反者リスト（管理者モード） ===
        List<String> violationEmployees = null;
        Map<String, ComplianceCheckResult> violationDetails = null;

        if (userRoleId == 1 && !isSelfMode) {
            try {
                ComplianceChecker checker = new ComplianceChecker();
                violationEmployees = new ArrayList<>();
                violationDetails = new HashMap<>();

                List<EmpBean> allEmployees = empDao.findAll();
                LocalDate monthStart = LocalDate.now().withDayOfMonth(1);
                LocalDate monthEnd   = monthStart.withDayOfMonth(monthStart.lengthOfMonth());

                for (EmpBean emp : allEmployees) {
                    List<KintaiRecBean> empRecords = kintaiRecDao.getKintaiRecords(
                        List.of(emp.getEmpId()), null, null,
                        monthStart, monthEnd, userRoleId);

                    ComplianceCheckResult result = checker.performComprehensiveCheck(empRecords, emp.getEmpId());

                    if (result.getTotalViolations() > 0) {
                        violationEmployees.add(emp.getEmpName());
                        result.setEmpName(emp.getEmpName());

                        // 部署名・役職名を DAO から解決
                        DeptBean empDept = deptDao.findByDeptId(emp.getDeptId());
                        PostBean empPost = postDao.findByPostId(emp.getPostId());

                        result.setDeptName(empDept != null ? empDept.getDeptName() : "不明");
                        result.setPostName(empPost != null ? empPost.getPostName() : "不明");

                        violationDetails.put(emp.getEmpName(), result);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                violationEmployees = new ArrayList<>();
                violationDetails = new HashMap<>();
            }
        }
        request.setAttribute("violationEmployees", violationEmployees);
        request.setAttribute("violationDetails", violationDetails);




        // プロジェクト一覧を取得
        ProjectDao projectDao = new ProjectDao();
        List<ProjectBean> projects = projectDao.findAll();  // IS_DELETED=false のみ取得される想定
        request.setAttribute("projectList", projects);
        
        // プロジェクト工数割り当て一覧（日別合計用）
        WorkTimeDao workTimeDao = new WorkTimeDao();
        List<KinmuManageBean.WorkAlloc> kinmuList =
                workTimeDao.findWorkAllocsByEmpAndRange(selectedEmpId, startDate, endDate);
        request.setAttribute("kinmuList", kinmuList);

        // プロジェクト別合計時間（月単位の円グラフ用）
        List<KinmuManageBean.WorkAlloc> projectTotals =
                workTimeDao.findProjectTotalsByEmpAndRange(selectedEmpId, startDate, endDate);
        request.setAttribute("projectTotals", projectTotals);

        // JSPに渡す
        request.setAttribute("selectedEmpId", selectedEmpId);
        request.setAttribute("recMapByEmp", recMapByEmp);
        request.setAttribute("kintaiRecords", kintaiRecords);
        request.setAttribute("empIdFilter", empIdFilter);
        request.setAttribute("deptIdFilter", deptIdFilter);
        request.setAttribute("postIdFilter", postIdFilter);
        request.setAttribute("startDate", startDateStr);
        request.setAttribute("endDate", endDateStr);
        request.setAttribute("user", user);
        request.setAttribute("userRoleId", userRoleId);
        request.setAttribute("isSelfMode", isSelfMode);

        // 部署・役職リスト
        request.setAttribute("deptList", deptDao.findAll());
        request.setAttribute("postList", postDao.findAll());

        // 従業員リスト（管理者・部長用）
        if ((userRoleId == 1 || userRoleId == 2) && !isSelfMode) {
            String selectedDeptId = deptIdFilter != null ? deptIdFilter : (userRoleId == 2 ? user.getDeptId() : null);
            String selectedPostId = postIdFilter;
            request.setAttribute("empList", empDao.findByFilters(selectedDeptId, selectedPostId));
        }

        // JSP にフォワード
        RequestDispatcher dispatcher = request.getRequestDispatcher("/web/kintai_rec.jsp");
        dispatcher.forward(request, response);}}
        
   
    
    /**
     * 月単位のプロジェクト工数合計を取得して JSON 返却
     */
    /**
     * 月単位のプロジェクト工数合計を取得して JSON 返却
     */
    /**
     * 月単位のプロジェクト工数合計を取得して JSON 返却
     */
    private void handleProjectAnalysis(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        response.setContentType("application/json; charset=UTF-8");
        PrintWriter out = response.getWriter();

        try {
            String empId = request.getParameter("empId");
            LocalDate start = LocalDate.parse(request.getParameter("start"));
            LocalDate end   = LocalDate.parse(request.getParameter("end"));

            // DAOからプロジェクト別工数を取得
            KinmuManageDao kinmuDao = new KinmuManageDao();
            List<KinmuManageBean.WorkAlloc> kinmuList = kinmuDao.findByEmpAndRange(empId, start, end);

            // プロジェクトIDごとに時間集計
            Map<Integer, Double> projectHoursMap = new HashMap<>();
            for (KinmuManageBean.WorkAlloc k : kinmuList) {
                projectHoursMap.merge(k.getProjectId(), k.getWorkHours(), Double::sum);
            }

            // プロジェクト名に変換してDTO化
            ProjectDao projectDao = new ProjectDao();
            List<Map<String, Object>> dtos = new ArrayList<>();
            for (Map.Entry<Integer, Double> e : projectHoursMap.entrySet()) {
                String projectName = projectDao.getProjectNameById(e.getKey()); // ← ここで名前解決
                Map<String, Object> dto = new HashMap<>();
                dto.put("projectId", e.getKey());
                dto.put("projectName", projectName != null ? projectName : "不明(" + e.getKey() + ")");
                dto.put("workHours", e.getValue());
                dtos.add(dto);
            }

        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.write("{\"success\":false,\"message\":\"プロジェクト分析に失敗しました\"}");
        }
    }

    
    /**
     * 勤怠 月単位一覧を JSON 返却
     */
    /**
     * 勤怠 月単位一覧を JSON 返却（DTO変換）
     */
    /**
     * 勤怠 月単位一覧を JSON 返却
     */
    private void handleListAttendance(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        response.setContentType("application/json; charset=UTF-8");
        PrintWriter out = response.getWriter();

        try {
            String empId = request.getParameter("empId");
            int year = Integer.parseInt(request.getParameter("year"));
            int month = Integer.parseInt(request.getParameter("month"));

            LocalDate start = LocalDate.of(year, month, 1);
            LocalDate end = start.withDayOfMonth(start.lengthOfMonth());

            // 勤怠データ
            List<KintaiRecBean> records = kintaiRecDao.getKintaiRecords(
                    List.of(empId), null, null, start, end, 0);

            // イベントデータ
            CalendarEventDao eventDao = new CalendarEventDao();
            List<CalendarEventBean> events = eventDao.findByRange(start, end);

            Map<LocalDate, CalendarEventBean> eventMap = new HashMap<>();
            for (CalendarEventBean e : events) {
                eventMap.put(e.getEventDate(), e);
            }

            // プロジェクト工数データ
            KinmuManageDao kinmuDao = new KinmuManageDao();
            List<KinmuManageBean.WorkAlloc> kinmuList = kinmuDao.findByEmpAndRange(empId, start, end);

            Map<LocalDate, Double> projectTotalMap = new HashMap<>();
            for (KinmuManageBean.WorkAlloc k : kinmuList) {
                projectTotalMap.merge(k.getWorkDate(), k.getWorkHours(), Double::sum);
            }

            // DTOに変換
            List<KintaiRecDto> dtos = new ArrayList<>();
            for (KintaiRecBean r : records) {
                KintaiRecDto dto = new KintaiRecDto();
                dto.kintaiDate = (r.getKintaiDate() != null) ? r.getKintaiDate().toString() : "";

                // ★ 曜日を追加
                if (r.getKintaiDate() != null) {
                    dto.dayOfWeek = r.getKintaiDate()
                            .getDayOfWeek()
                            .getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.JAPANESE);
                } else {
                    dto.dayOfWeek = "";
                }

                dto.attendanceType = (r.getAttendanceType() != null) ? r.getAttendanceType() : "";
                dto.clockIn = (r.getClockIn() != null) ? r.getClockIn().toLocalTime().toString().substring(0,5) : "";
                dto.clockOut = (r.getClockOut() != null) ? r.getClockOut().toLocalTime().toString().substring(0,5) : "";
                dto.totalBreakTimeFormatted = (r.getTotalBreakTimeFormatted() != null) ? r.getTotalBreakTimeFormatted() : "";
                dto.actualWorkTimeFormatted = (r.getActualWorkTimeFormatted() != null) ? r.getActualWorkTimeFormatted() : "";
                dto.overtimeFormatted = (r.getOvertimeFormatted() != null) ? r.getOvertimeFormatted() : "";
                dto.nightovertimeFormatted = (r.getNightovertimeFormatted() != null) ? r.getNightovertimeFormatted() : "";

                // イベント
                CalendarEventBean ev = eventMap.get(r.getKintaiDate());
                if (ev != null) {
                    dto.workStatus = ev.isWork() ? "出勤日" : "休日";
                    dto.eventName = (ev.getEventName() != null) ? ev.getEventName() : "";
                } else {
                    dto.workStatus = "";
                    dto.eventName = "";
                }

                // プロジェクト工数
                Double projHours = projectTotalMap.get(r.getKintaiDate());
                if (projHours != null) {
                    int h = projHours.intValue();
                    int m = (int) Math.round((projHours - h) * 60);
                    dto.projectTotalHours = String.format("%02d:%02d", h, m);
                } else {
                    dto.projectTotalHours = "";
                }

                dtos.add(dto);
            }


        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.write("{\"success\":false,\"message\":\"一覧取得に失敗しました\"}");
        }
    }



}
