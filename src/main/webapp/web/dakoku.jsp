<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.time.LocalDate" %>
<%@ page import="java.time.format.DateTimeFormatter" %>
<%@ page import="java.util.List, java.util.Map" %>
<%@ page import="kintai.UserBean" %>
<%@ page import="kintai.KinmuManageBean" %>
<%@ page import="kintai.ProjectBean" %>


<%
    UserBean user = (UserBean) session.getAttribute("user");
    if (user == null) {
        response.sendRedirect(request.getContextPath() + "/web/login.jsp");
        return;
    }
    // 修正箇所: user.getRole() を user.getRoleId() に変更
    String backUrl = (user.getRoleId() == 1) ? request.getContextPath() + "/AdminMenuServlet" : request.getContextPath() + "/web/menu.jsp";

    String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日"));
    Map<String, String> workTimeData = (Map<String, String>) request.getAttribute("workTimeData");
    List<Map<String, String>> breakList = (List<Map<String, String>>) request.getAttribute("breakList");
    String successMessage = (String) request.getAttribute("successMessage");
    String errorMessage = (String) request.getAttribute("errorMessage");

    if (workTimeData == null) workTimeData = new java.util.HashMap<>();
    if (breakList == null) breakList = new java.util.ArrayList<>();

    boolean hasClockedIn = workTimeData.get("clockInTime") != null;
    boolean hasClockedOut = workTimeData.get("clockOutTime") != null;

    // ★★★ ここで最初の休憩かどうかを判断 ★★★
    boolean isFirstBreak = breakList.isEmpty(); 
   
    
    List<KinmuManageBean.WorkAlloc> workAllocs = 
        (List<KinmuManageBean.WorkAlloc>) request.getAttribute("workAllocs");
    if (workAllocs == null) workAllocs = new java.util.ArrayList<>();

    List<ProjectBean> projectList = 
        (List<ProjectBean>) request.getAttribute("projectList");
    if (projectList == null) projectList = new java.util.ArrayList<>();

    String targetDateStr = (String) request.getAttribute("targetDateStr");

%>
<html>
<head>
	<%-- レスポンシブ対応 --%>
	<meta name="viewport" content="width=device-width, initial-scale=1">
    
    <title>勤怠打刻・登録</title>
    <style>
        body { font-family: 'メイリオ', sans-serif; background-color: #f0f0f0; margin: 0; padding: 20px; }
        .overflow-x=hidden; 
        .container { max-width: 1200px; margin: 0 auto; background-color: white; padding: 20px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
        .header { display: flex; justify-content: space-between; align-items: center; padding: 10px 20px; background: #fff; border-bottom: 1px solid #ccc; margin: -20px -20px 20px -20px; border-top-left-radius: 8px; border-top-right-radius: 8px; }
        .user-info { font-size: 13px; }
        h1 { color: #333; border-bottom: 2px solid #007bff; padding-bottom: 5px; margin: 10px 0; text-align: center; font-size: 18px; }
        .main-layout { display: flex; gap: 20px; flex-wrap: wrap; }
        .left-section { flex: 1; display: flex; flex-direction: column; gap: 15px; min-width: 300px; }
        .right-section { flex: 1; min-width: 300px; }
        .section { margin-bottom: 15px; padding: 16px; border: 1px solid #dee2e6; border-radius: 8px; background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%); box-shadow: 0 2px 4px rgba(0,0,0,0.05); }
        .section h2 { margin-top: 0; color: #495057; border-bottom: 2px solid #007bff; padding-bottom: 8px; margin-bottom: 15px; font-size: 16px; }
        .punch-panel { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; margin-bottom: 15px; }
        .punch-button { font-size: 14px; padding: 12px; cursor: pointer; border-radius: 6px; border: 1px solid; }
        .punch-button:disabled { background-color: #e9ecef; color: #6c757d; cursor: not-allowed; }
        .clock-in { background-color: #28a745; border-color: #28a745; color: white; }
        .clock-out { background-color: #dc3545; border-color: #dc3545; color: white; }
        
        .form-group { margin-bottom: 12px; display: flex; align-items: center; }
        .form-group label { font-weight: bold; width: 100px; font-size: 12px; }
        .form-group input[type="text"] { flex-grow: 1; padding: 6px; border: 1px solid #ccc; border-radius: 4px; font-size: 12px; }
        .add-break-button { background-color: #5cb85c; border-color: #4cae4c; color: white; padding: 6px 12px; font-size: 12px; }

        .status-table { width: 100%; border-collapse: collapse; margin-top: 10px; font-size: 12px; }
        .status-table th, .status-table td { border: 1px solid #dee2e6; padding: 8px; text-align: center; }
        .status-table th { background-color: #e9ecef; font-weight: 600; color: #495057; }
        .delete-form button { background: #dc3545; border-color: #dc3545; color: white; padding: 4px 8px; border-radius: 3px; cursor: pointer; font-size: 11px; }
        
        .message { padding: 10px; margin-bottom: 20px; border-radius: 4px; text-align: center; }
        .success-message { background-color: #d4edda; color: #155724; border: 1px solid #c3e6cb; }
        .error-message { background-color: #f8d7da; color: #721c24; border: 1px solid #f5c6cb; }
        .status-display { text-align: center; margin-bottom: 12px; font-size: 13px; background-color: #f8f9fa; padding: 8px; border-radius: 4px; }
        .back-link { display: inline-block; margin-top: 20px; padding: 8px 16px; background-color: #6c757d; color: white; text-decoration: none; border-radius: 4px; text-align: center; font-size: 14px; }
        .back-link:hover { background-color: #545b62; }
        
        @media (max-width: 767px) {
		    .main-layout { flex-direction: column; }
		    .left-section, .right-section { width: 100%; }
		 
		
		
		
    </style>
    <script>
	    // 確認メッセージ用
	    function confirmAction(message) {
	        // alert() の代わりに確認ダイアログを作成
	        // 実運用では、もっとリッチなモーダルダイアログを実装することが推奨されます
	        return confirm(message);
	    }
    </script>
</head>
<body>
    <div class="container">
        <div class="header">
            <div class="user-info">
                <p>部署：<%= session.getAttribute("deptName") != null ? session.getAttribute("deptName") : "情報なし" %></p>
                <p>氏名：<%= user.getName() %></p>
            </div>
            <form method="post" action="<%= request.getContextPath() %>/logout" style="margin: 0;">
                <input type="submit" value="ログアウト" style="background-color: #dc3545; color: white; border: 1px solid #dc3545; border-radius: 5px; padding: 8px 16px; cursor: pointer; font-size: 13px;">
            </form>
        </div>

        <h1><%= today %> の勤怠打刻・登録</h1>

        <% if (successMessage != null) { %><div class="message success-message"><%= successMessage %></div><% } %>
        <% if (errorMessage != null) { %><div class="message error-message"><%= errorMessage %></div><% } %>

        <div class="main-layout">
            <div class="left-section">
                <div class="section">
                    <h2>📋 出退勤打刻</h2>
                    <div class="status-display">
                        <strong>出勤:</strong> <%= workTimeData.getOrDefault("clockInTime", "---") %> | 
                        <strong>退勤:</strong> <%= workTimeData.getOrDefault("clockOutTime", "---") %>
                    </div>
                    <form action="workPunch" method="post">
                        <div class="punch-panel">
                            <button type="submit" name="action" value="clock_in" class="punch-button clock-in" <%= (hasClockedIn) ? "disabled" : "" %>>出勤</button>
                            <button type="submit" name="action" value="clock_out" class="punch-button clock-out" <%= (!hasClockedIn || hasClockedOut) ? "disabled" : "" %>>退勤</button>
                        </div>
                    </form>
                </div>
                <!-- ★ここに工数割り当てセクションを追加★ -->
		    <div class="section">
		        <h2>📊 工数割り当て</h2>
                <div style="max-height: 200px; overflow-y: auto;">
                    <table style="width: 100%; border-collapse: collapse; font-size: 10px;">
                        <thead>
                            <tr>
                                <th style="border: 1px solid #dee2e6; padding: 4px; font-size: 10px;">プロジェクト</th>
                                <th style="border: 1px solid #dee2e6; padding: 4px; font-size: 10px;">時間</th>
                                <th style="border: 1px solid #dee2e6; padding: 4px; font-size: 10px;">操作</th>
                            </tr>
                        </thead>
                        <tbody>
                            <% if (workAllocs.isEmpty()) { %>
                                <tr><td colspan="3" style="color: #6c757d; text-align: center; padding: 8px; font-size: 10px;">工数割り当てなし</td></tr>
                            <% } else { %>
                                <% for (KinmuManageBean.WorkAlloc alloc : workAllocs) { %>
                                    <tr>
                                        <td style="border: 1px solid #dee2e6; padding: 3px; font-size: 9px;"><%= alloc.getProjectName() != null ? alloc.getProjectName() : "---" %></td>
                                        <td style="border: 1px solid #dee2e6; padding: 3px; text-align: center;"><%= alloc.getWorkHoursFormatted() %></td>
                                        <td style="border: 1px solid #dee2e6; padding: 3px; text-align: center;">
                                            <form action="<%= request.getContextPath() %>/KinmuManageServlet" method="post" onsubmit="return confirmAction('この工数割り当てを削除しますか？');" style="display: inline;">
                                                <input type="hidden" name="action" value="delete_work_alloc">
                                                <input type="hidden" name="sourcePage" value="dakoku">
                                                <input type="hidden" name="targetDate" value="<%= targetDateStr %>">
                                                <input type="hidden" name="allocationId" value="<%= alloc.getAllocationId() %>">
                                                <button type="submit" class="btn-danger" style="font-size: 9px; padding: 2px 4px;">削除</button>
                                            </form>
                                        </td>
                                    </tr>
                                <% } %>
                            <% } %>
                        </tbody>
                    </table>
                </div>
                <form action="<%= request.getContextPath() %>/KinmuManageServlet" method="post" onsubmit="return confirmAction('新しい工数割り当てを追加しますか？');" style="margin-top: 10px;">
                    <input type="hidden" name="action" value="add_work_alloc">
                    <input type="hidden" name="sourcePage" value="dakoku">
                    <input type="hidden" name="targetDate" value="<%= request.getAttribute("targetDate") %>">
                    <div style="display: flex; flex-direction: column; gap: 5px;">
                        <select name="newProjectId" required style="width: 100%; font-size: 10px; padding: 3px;">
                            <option value="">プロジェクト選択</option>
                            <% for (ProjectBean project : projectList) { %>
                                <option value="<%= project.getProjectId() %>"><%= project.getProjectName() %></option>
                            <% } %>
                        </select>
                        <div style="display: flex; gap: 5px; align-items: center;">
						    <label>工数時間:</label>
						        <input type="time" name="newWorkHours" step="60" value="01:00" placeholder="例: 01:00">
        						<%-- step="60" は1分刻み --%>
						    <button type="submit" class="btn-success" style="font-size: 10px; padding: 3px 6px;">追加</button>
						</div>
                    </div>
                </form>
            </div>
		    </div>
            
            
            

            <div class="right-section">
                <div class="section">
                    <h2>🕐 休憩の登録・管理</h2>
                    <form action="workPunch" method="post">
                        <input type="hidden" name="action" value="add_break">
                        <div class="form-group">
                            <label>休憩開始:</label>
                            <input type="time" name="breakStartTime" value="<%= isFirstBreak ? "12:00" : "" %>" placeholder="例: 12:00">
                        </div>
                        <div class="form-group">
                            <label>休憩終了:</label>
                            <input type="time" name="breakEndTime" value="<%= isFirstBreak ? "13:00" : "" %>" placeholder="例: 13:00">
                        </div>
                        <div style="text-align:right;">
                            <button type="submit" class="add-break-button" <%= (!hasClockedIn || hasClockedOut) ? "disabled" : "" %>>+ 休憩を追加</button>
                        </div>
                    </form>
                    
                    <table class="status-table" style="margin-top:15px;">
                        <thead><tr><th>休憩開始</th><th>休憩終了</th><th>操作</th></tr></thead>
                        <tbody>
                            <% if (breakList.isEmpty()) { %>
                                <tr><td colspan="3" style="color: #6c757d;">休憩記録はありません</td></tr>
                            <% } else { for (Map<String, String> breakItem : breakList) { %>
                                <tr>
                                    <td><%= breakItem.get("startTime") %></td>
                                    <td><%= breakItem.getOrDefault("endTime", "---") %></td>
                                    <td>
                                        <form class="delete-form" action="workPunch" method="post" style="display: inline;">
                                            <input type="hidden" name="action" value="delete_break">
                                            <input type="hidden" name="breakId" value="<%= breakItem.get("breakId") %>">
                                            <button type="submit" <%= (hasClockedOut) ? "disabled" : "" %>>削除</button>
                                        </form>
                                    </td>
                                </tr>
                            <% }} %>
                        </tbody>
                    </table>
                </div>
            </div>
            
        </div>
        
        <div style="text-align: center; margin-top: 30px;">
            <a href="<%= backUrl %>" class="back-link">メニューへ戻る</a>
        </div>
    </div>
</body>
</html>