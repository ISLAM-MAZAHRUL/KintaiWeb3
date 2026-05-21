<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="kintai.UserBean" %>
<%@ page import="kintai.CalendarEventBean" %>
<%@ page import="kintai.EventRepeatRuleBean" %>
<%@ page import="java.time.LocalDate" %>
<%@ page import="java.time.format.DateTimeFormatter" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.HashMap" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="com.google.gson.Gson" %>
<%
    // ログインチェック
    UserBean user = (UserBean) session.getAttribute("user");
    if (user == null) {
        response.sendRedirect(request.getContextPath() + "/web/login.jsp");
        return;
    }

    // セッションからユーザー情報を取得
    String loggedInUserName = user.getName();
    String loggedInDeptName = (String) session.getAttribute("deptName");
    int userRoleId = user.getRoleId(); 

    // サーブレットから渡されたデータ (FullCalendar用JSON文字列、メッセージ、イベントリスト、ルールリスト)
    String eventsJson = (String) request.getAttribute("eventsJson"); // FullCalendar用イベントJSON文字列
    List<CalendarEventBean> eventList = (List<CalendarEventBean>) request.getAttribute("eventList"); // サイドパネルのリスト表示用
    List<EventRepeatRuleBean> allRules = (List<EventRepeatRuleBean>) request.getAttribute("ruleList"); // ルールリスト
    String rulesByRuleIdJson = (String) request.getAttribute("rulesByRuleIdJson"); // JavaScript用のJSON文字列

    String message = (String) request.getAttribute("message");
    Boolean success = (Boolean) request.getAttribute("success");

    // nullチェックと初期化
    if (eventsJson == null) eventsJson = "[]"; // デフォルトで空のJSON配列
    if (eventList == null) eventList = new java.util.ArrayList<>();
    if (allRules == null) allRules = new java.util.ArrayList<>();
    if (rulesByRuleIdJson == null) rulesByRuleIdJson = "{}"; // デフォルトで空のJSONオブジェクト

    // 繰り返しルールをRuleIdで検索しやすいようにMapに変換 (JavaScriptでの検索効率化のため)
    Map<Integer, EventRepeatRuleBean> rulesByRuleId = new HashMap<>();
    for (EventRepeatRuleBean rule : allRules) {
        rulesByRuleId.put(rule.getRuleId(), rule);
    } 

    // メニューへ戻るリンクのURLを権限に応じて設定
    String backUrl = (userRoleId == 1) ? request.getContextPath() + "/AdminMenuServlet" : request.getContextPath() + "/web/menu.jsp";
    
    Boolean isAdmin = (Boolean) request.getAttribute("isAdmin");
%>
<!DOCTYPE html>
<html>
<head>
<link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/flatpickr/dist/flatpickr.min.css">
<script src="https://cdn.jsdelivr.net/npm/flatpickr"></script>
<script>
    var isAdmin = <%= isAdmin %>;
</script>
    <meta charset="UTF-8">
    <title>カレンダー・イベント管理</title>
    
    <!-- FullCalendar CSS -->
    <link href='https://cdn.jsdelivr.net/npm/fullcalendar@6.1.11/main.min.css' rel='stylesheet' />
    
    <style>
        body {
            font-family: 'メイリオ', sans-serif;
            background-color: #f0f0f0;
            margin: 0;
            padding: 10px;
            font-size: 14px;
        }
        /*2025/8/6 米村 サイドバーのイベント一覧が最後までスクロールできないため変更*/
        .container {
            max-width: 1200px;
            margin: 0 auto;
            background-color: white;
            padding: 20px;
            border-radius: 8px;
            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
            display: flex;
            flex-direction: column;
            gap: 5px;
            height: 85vh;
            overflow: hidden;	/* ← 2025/8/6 米村 ここをhiddenからautoに変更 */
        }
        .header {
            display: flex;
            justify-content: space-between;
            align-items: flex-start;
            padding: 8px 20px;
            background: #fff;
            border-bottom: 1px solid #ccc;
            margin: -20px -20px 0px -20px;
            border-top-left-radius: 8px;
            border-top-right-radius: 8px;
        }
        .user-info {
            display: flex;
            flex-direction: column;
            line-height: 1.4;
            text-align: left;
            font-size: 12px;
        }
        .logout-button {
            background-color: #dc3545;
            color: white;
            border: 1px solid #dc3545;
            border-radius: 5px;
            padding: 8px 16px;
            cursor: pointer;
            font-size: 1em;
            text-decoration: none;
            align-self: center;
        }
        .logout-button:hover {
            background-color: #c82333;
            border-color: #bd2130;
        }
        h1 {
            color: #333;
            border-bottom: 2px solid #007bff;
            padding-bottom: 10px;
            margin: 0 0 15px 0;
            font-size: 1.5em;
            text-align: center;
        }
        /* メッセージ表示エリア */
        .message {
            padding: 6px;
            margin-bottom: 8px;
            border-radius: 4px;
            text-align: center;
            font-size: 0.9em;
        }
        .success-message {
            background-color: #d4edda;
            color: #155724;
            border: 1px solid #c3e6cb;
        }
        .error-message {
            background-color: #f8d7da;
            color: #721c24;
            border: 1px solid #f5c6fb;
        }

        /* メインコンテンツ（カレンダーとサイドパネル）のレイアウト */
        .main-content-wrapper {
            display: flex;
            flex-grow: 1;
            gap: 20px;
            align-items: flex-start;
            height: calc(95vh - 180px);
            position: relative;
        }

        /* カレンダーコンテナ */
        #calendar-container {
            flex-grow: 1;
            height: 100%;
            overflow: hidden;
            transition: margin-left 0.3s ease, margin-right 0.3s ease;
        }

        /* 侧边栏展开时不再移动主布局 */
        #calendar-container.list-panel-expanded {
            /* margin-left: 420px; */ /* 削除マージン移動 */
        }
        
        #calendar-container.add-panel-expanded {
            /* margin-right: 420px; */ /* 削除マージン移動 */
        }

        /* FullCalendarコンテナのスタイル */
        #calendar {
            border: 1px solid #ddd;
            border-radius: 5px;
            height: 100%;
        }

        /* FullCalendarの要素の調整 */
        .fc .fc-toolbar {
            flex-wrap: wrap; /* ツールバーが小さい画面で折り返すように */
        }
        .fc .fc-toolbar-title {
            font-size: 1.5em;
        }
        .fc .fc-button {
            padding: 4px 8px;
            font-size: 0.9em;
        }
        .fc .fc-daygrid-day-number {
            font-size: 1em;
            padding: 2px;
        }
        .fc .fc-daygrid-day-top { /* 追加 */
            padding: 2px;
            margin: 0px;
        }
        .fc .fc-col-header-cell {
            font-size: 0.9em;
        }
        .fc .fc-day {
            padding: 2px;
        }
        
        .fc-daygrid-day-events { /* 追加 */
    margin-top: 2px !important;
}
		.fc-header-toolbar {
			margin:0px;
			padding: 0px;
			max-weight: 1px;
		}
		
		.fc-header-toolbar.fc-toolbar.fc-toolbar-ltr{
			margin:0px;
			padding: 0px;
		}

        .fc-event { /* イベント表示のスタイル */
            cursor: pointer;
            border-radius: 3px;
            padding: 2px 4px;
            font-size: 0.85em;
            white-space: nowrap; /* イベント名が改行されないように */
            overflow: hidden;
            text-overflow: ellipsis; /* はみ出した場合に...表示 */
            margin-bottom: 1px; /* イベント間の隙間 */
        }
        /* カレンダー凡例 - title与日历中间横向排列 */
        .legend-container {
            display: flex;
            justify-content: center;
            margin: 15px 0;
        }
        .calendar-legend {
            display: flex;
            align-items: center;
            gap: 20px;
            padding: 8px 16px;
            border: 1px solid #ddd;
            border-radius: 5px;
            background-color: rgba(248, 249, 250, 0.95);
            backdrop-filter: blur(5px);
            box-shadow: 0 2px 8px rgba(0,0,0,0.1);
        }
        .legend-title {
            font-weight: bold;
            font-size: 0.9em;
            color: #495057;
            margin-right: 10px;
        }
        .legend-items {
            display: flex;
            gap: 15px;
        }
        .legend-title {
            font-weight: bold;
            font-size: 0.85em;
            margin-bottom: 6px;
            color: #495057;
            text-align: center;
        }
        .legend-item {
            display: flex;
            align-items: center;
            gap: 5px;
        }
        .legend-color {
            width: 12px;
            height: 12px;
            border-radius: 2px;
            flex-shrink: 0;
        }
        .legend-label {
            font-size: 0.8em;
            color: #495057;
            white-space: nowrap;
        }
        .holiday-color { background-color: #ffa500; }
        .weekend-color { background-color: #ff4444; }
        .work-color { background-color: #007bff; }

        /* カレンダーコンテナに相対位置を設定 */
        #calendar-container {
            position: relative;
        }

        /* 侧边栏打开时的背景遮罩 */
        .sidebar-overlay {
            position: fixed;
            top: 0;
            left: 0;
            width: 100vw;
            height: 100vh;
            background-color: rgba(0, 0, 0, 0.3);
            z-index: 998;
            opacity: 0;
            visibility: hidden;
            transition: opacity 0.3s ease, visibility 0.3s ease;
        }
        .sidebar-overlay.active {
            opacity: 1;
            visibility: visible;
        }

        /* 収縮ボタンのスタイル */
        /* 按钮和凡例的容器样式 */
        .button-container {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 10px;
            position: relative;
            z-index: 900; /* 低于侧边栏的层级，让侧边栏浮现在凡例上面 */
        }
        .button-group {
            display: flex;
            gap: 10px;
        }
        
        /* 侧边栏关闭按钮样式 */
        .panel-close-btn {
            position: absolute;
            top: 10px;
            right: 10px;
            background: #dc3545;
            color: white;
            border: none;
            border-radius: 50%;
            width: 30px;
            height: 30px;
            cursor: pointer;
            font-size: 16px;
            font-weight: bold;
            display: flex;
            align-items: center;
            justify-content: center;
            z-index: 1001;
        }
        .panel-close-btn:hover {
            background: #c82333;
        }

        .panel-toggle-btn {
            background-color: #007bff;
            color: white;
            border: none;
            border-radius: 5px;
            padding: 8px 16px;
            cursor: pointer;
            font-size: 14px;
            box-shadow: 0 2px 8px rgba(0,0,0,0.2);
            transition: all 0.3s ease;
            position: relative;
            z-index: 1001; /* 确保按钮在侧边栏之上 */
        }

        .panel-toggle-btn:hover {
            background-color: #0056b3;
            transform: translateY(-2px);
        }

        /* 侧边栏打开时的按钮位置调整 */
        .button-container.list-panel-expanded {
            margin-left: 420px; /* 左侧パネル分のマージンを追加 */
            transition: margin-left 0.3s ease;
        }
        
        .button-container.add-panel-expanded {
            margin-right: 420px; /* 右侧パネル分のマージンを追加 */
            transition: margin-right 0.3s ease;
        }
        


        /*2025/8/6 米村 サイドバーのイベント一覧が最後までスクロールできないため変更*/
        /* 左侧边栏（イベント一覧）のスタイル */
        #event-list-panel {
            position: fixed;
            left: -450px; /* 初期は隠す */
            top: 0;
            width: 400px;
            height: calc(100vh - 40px);		/* ← 2025/8/6 米村 height: 100vhから変更 */
            background-color: #f8f9fa;
            border-right: 1px solid #dee2e6;
            box-shadow: 5px 0 15px rgba(0,0,0,0.1);
            transition: left 0.3s ease;
            z-index: 1000; /* 提高层级，让其浮现在凡例上面 */
            padding: 20px;
            overflow-y: auto;
            display: flex;
            flex-direction: column;
        }

        #event-list-panel.panel-open {
            left: 0;
        }
        
        /* 右侧边栏（新規イベント追加）のスタイル */
        #event-add-panel {
            position: fixed;
            right: -450px; /* 初期は隠す */
            top: 0;
            width: 400px;
            height: 100vh;
            background-color: #f8f9fa;
            border-left: 1px solid #dee2e6;
            box-shadow: -5px 0 15px rgba(0,0,0,0.1);
            transition: right 0.3s ease;
            z-index: 1000; /* 提高层级 */
            padding: 20px;
            overflow-y: auto;
            display: flex;
            flex-direction: column;
        }

        #event-add-panel.panel-open {
            right: 0;
        }

        #event-list-panel h2, #event-add-panel h2 {
            margin-top: 0;
            color: #495057;
            border-bottom: 1px solid #ddd;
            padding-bottom: 10px;
            margin-bottom: 20px;
            font-size: 1.3em;
        }

        /* フォームグループの共通スタイル */
        .form-group {
            margin-bottom: 15px;
            display: flex;
            align-items: center;
            flex-wrap: wrap;
        }
        .form-group label {
            display: inline-block;
            width: 120px;
            font-weight: bold;
            flex-shrink: 0;
        }
        .form-group input[type="date"],
        .form-group input[type="text"],
        .form-group input[type="number"],
        .form-group select {
            width: calc(100% - 120px);
            padding: 8px;
            border: 1px solid #ced4da;
            border-radius: 4px;
            flex-grow: 1;
        }
        .form-group .radio-group,
        .form-group .checkbox-group {
            display: flex;
            align-items: center;
            flex-grow: 1;
            flex-wrap: wrap;
            gap: 10px;
        }
        .form-group .radio-group input[type="radio"],
        .form-group .checkbox-group input[type="checkbox"] {
            width: auto;
            margin-right: 5px;
            margin-left: 0;
        }
        .form-group .radio-group label,
        .form-group .checkbox-group label {
            width: auto;
            font-weight: normal;
            margin-right: 15px;
        }
        .form-group .checkbox-group {
            padding-left: 120px; /* ラベルの幅分インデント */
            margin-top: -10px; /* 上のform-groupとの間隔調整 */
        }
        .form-group.repeat-h3 {
            width: 100%;
            text-align: left;
            margin-bottom: 10px;
            padding-bottom: 5px;
            border-bottom: 1px dashed #eee;
            color: #495057;
            font-size: 1.1em;
            font-weight: bold;
        }

        /* ボタンのスタイル */
        .btn {
            padding: 8px 16px;
            border: none;
            border-radius: 4px;
            cursor: pointer;
            font-size: 1em;
            margin-left: 5px;
            min-width: 80px;
        }
        .btn-primary { background-color: #007bff; color: white; }
        .btn-primary:hover { background-color: #0056b3; }
        .btn-success { background-color: #28a745; color: white; }
        .btn-success:hover { background-color: #218838; }
        .btn-danger { background-color: #dc3545; color: white; }
        .btn-danger:hover { background-color: #c82333; }
        .btn-secondary { background-color: #6c757d; color: white; }
        .btn-secondary:hover { background-color: #545b62; }
        .text-right {
            text-align: right;
        }

        /* イベントリストテーブルのスタイル（サイドパネル内） */
        .event-list-table {
            width: 100%;
            border-collapse: collapse;
            margin-top: 20px;
        }
        .event-list-table th, .event-list-table td {
            border: 1px solid #dee2e6;
            padding: 8px;
            text-align: left;
            font-size: 0.9em;
        }
        .event-list-table th {
            background-color: #f0f3f5;
            font-weight: bold;
            color: #495057;
            position: sticky; /* ヘッダーを固定 */
            top: 0;
            z-index: 10;
        }
        .event-list-table tr:nth-child(even) { background-color: #fcfcfc; }
        .event-list-table tr:hover { background-color: #e9ecef; }
        .event-list-table .action-cell { white-space: nowrap; text-align: center;}
        .event-list-table .action-cell .btn {
            padding: 4px 8px;
            font-size: 0.8em;
            min-width: 50px;
        }


        /* モーダルダイアログのスタイル（今回使用しないが、以前のスタイルを保持） */
        .modal {
            display: none; 
            position: fixed; 
            z-index: 1000; 
            left: 0;
            top: 0;
            width: 100%;
            height: 100%;
            overflow: auto;
            background-color: rgba(0,0,0,0.4); 
        }
        .modal-content {
            background-color: #fefefe;
            margin: 5% auto; 
            padding: 30px;
            border: 1px solid #888;
            border-radius: 10px;
            width: 80%; 
            max-width: 600px; 
            box-shadow: 0 4px 8px rgba(0,0,0,0.2);
            position: relative;
        }
        .close-button {
            color: #aaa;
            position: absolute;
            right: 15px;
            top: 10px;
            font-size: 28px;
            font-weight: bold;
            cursor: pointer;
        }
        .close-button:hover,
        .close-button:focus {
            color: black;
            text-decoration: none;
            cursor: pointer;
        }
        .modal-content h2 {
            margin-top: 0;
            padding-bottom: 10px;
            border-bottom: 1px solid #eee;
        }
        .modal-form-group {
            margin-bottom: 15px;
            display: flex;
            align-items: center;
            flex-wrap: wrap;
        }
        .modal-form-group label {
            width: 150px;
            font-weight: bold;
            flex-shrink: 0;
        }
        .modal-form-group input[type="date"],
        .modal-form-group input[type="text"],
        .modal-form-group input[type="number"],
        .modal-form-group select {
            flex-grow: 1;
            padding: 8px;
            border: 1px solid #ced4da;
            border-radius: 4px;
        }
        .modal-form-group .radio-group,
        .modal-form-group .checkbox-group {
            display: flex;
            align-items: center;
            flex-grow: 1;
            flex-wrap: wrap;
            gap: 10px;
        }
        .modal-form-group .radio-group label,
        .modal-form-group .checkbox-group label {
            width: auto;
            font-weight: normal;
            margin-right: 10px;
        }
        .modal-form-group .checkbox-group {
            padding-left: 150px; 
            margin-top: -10px; 
        }
        .modal-buttons {
            text-align: right;
            margin-top: 20px;
        }
        .modal-buttons button {
            padding: 10px 20px;
            border: none;
            border-radius: 5px;
            cursor: pointer;
            font-size: 1em;
            margin-left: 10px;
        }
        .modal-buttons .btn-save { background-color: #28a745; color: white; }
        .modal-buttons .btn-delete { background-color: #dc3545; color: white; }
        .modal-buttons .btn-cancel { background-color: #6c757d; color: white; }
        .modal-buttons .btn-save:hover { background-color: #218838; }
        .modal-buttons .btn-delete:hover { background-color: #c82333; }
        .modal-buttons .btn-cancel:hover { background-color: #545b62; }


        /* 戻るボタン */
        .back-link {
            display: inline-block;
            margin-top: 10px;
            padding: 6px 12px;
            background-color: #6c757d;
            color: white;
            text-decoration: none;
            border-radius: 4px;
            font-size: 0.9em;
        }
        .back-link:hover {
            background-color: #545b62;
        }
        
        /* 日曜（日 index 0） */
		.fc-day-sun {
		    background-color: #ffeaea; /* 薄い赤 */
		}
		
		/* 土曜（土 index 6） */
		.fc-day-sat {
		    background-color: #eaf4ff; /* 薄い水色 */
		}
        
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <div class="user-info">
                <p>部署：<%= loggedInDeptName != null ? loggedInDeptName : "情報なし" %></p>
                <p>氏名：<%= loggedInUserName %></p>
            </div>
            <form method="post" action="<%= request.getContextPath() %>/logout" style="margin: 0;">
                <input type="submit" value="ログアウト" class="logout-button">
            </form>
        </div>

        <h1>カレンダー・イベント管理</h1>
        
        <%-- メッセージ表示 --%>
        <% if (message != null && !message.isEmpty()) { %>
            <div class="message <%= (success != null && success) ? "success-message" : "error-message" %>">
                <%= message %>
            </div>
        <% } %>

        <!-- 按钮和凡例同一行 -->
        <div class="button-container">
            <!-- 左侧凡例 -->
            <div class="calendar-legend">
                <div class="legend-items">
                    <div class="legend-item">
                        <span class="legend-color holiday-color"></span>
                        <span class="legend-label">祝日</span>
                    </div>
                    <div class="legend-item">
                        <span class="legend-color weekend-color"></span>
                        <span class="legend-label">休日</span>
                    </div>
                    <div class="legend-item">
                        <span class="legend-color work-color"></span>
                        <span class="legend-label">出勤日</span>
                    </div>
                </div>
            </div>
            
            <!-- 右侧按钮组 -->
            <div class="button-group">
            	<% if (isAdmin) { %>
                <button class="panel-toggle-btn" id="addToggleBtn" onclick="toggleAddPanel()">新規追加</button>
                <% } %>
                <button class="panel-toggle-btn" id="listToggleBtn" onclick="toggleListPanel()">一覧</button>
            </div>
        </div>

        <!-- 背景遮罩 -->
        <div class="sidebar-overlay" id="sidebarOverlay"></div>

        <div class="main-content-wrapper">
            <!-- 左侧边栏（イベント一覧） -->
            <div id="event-list-panel">
                <button class="panel-close-btn" onclick="toggleListPanel()" title="關閉">×</button>
                <h2>イベント一覧</h2>
                <div style="max-height: 700px; overflow-y: auto; border: 1px solid #eee; border-radius: 5px;"> <%-- リスト表示エリアのスクロール --%>
                    <table class="event-list-table">
                        <thead>
                        <tr>
                            <th>日付</th>
                            <th>イベント名</th>
                            <th>種別</th>
                            <% if (isAdmin) { %>
                            <th>操作</th>
                            <% } %>
                        </tr>
                        </thead>
                        <tbody>
                        <% if (eventList != null && !eventList.isEmpty()) { %>
                            <% for (CalendarEventBean event : eventList) { %>
                                <tr>
                                    <td><%= event.getEventDate() %></td>
                                    <td><%= event.getEventName() %></td>
                                    <td><%= event.isWork() ? "出勤日" : "休日" %></td>
                                    <% if (isAdmin) { %>
                                    <td class="action-cell">
                                    
                                        <button type="button" class="btn btn-success" onclick="openEditModal('<%= event.getEventDate() %>', '<%= event.getEventName() %>', <%= event.isWork() %>, <%= event.getRepeatRuleId() != null ? event.getRepeatRuleId() : "null" %>)">編集</button>
                                        <button type="button" class="btn btn-danger" onclick="confirmDelete('<%= event.getEventDate() %>', '<%= event.getEventName() %>')">削除</button>
                                        <%-- 削除用フォーム（非表示） --%>
                                        <form id="deleteForm-<%= event.getEventDate().toString().replace("-", "_") %>" method="post" 
                                            action="<%= request.getContextPath() %>/CalendarManageServlet" style="display: none;">
                                            <input type="hidden" name="action" value="delete">
                                            <input type="hidden" name="eventDate" value="<%= event.getEventDate() %>">
                                        </form>
                                    </td>
                                    <% } %>
                                </tr>
                            <% } %>
                        <% } else { %>
                            <tr><td colspan="4" style="text-align: center; color: #6c757d; font-style: italic;">イベントなし</td></tr>
                        <% } %>
                        </tbody>
                    </table>
                </div>
            </div>
            
            <div id='calendar-container'>
                <div id='calendar'></div>
            </div>
        </div>

        <!-- 右侧边栏（新規イベント追加） -->
        <div id="event-add-panel">
                <button class="panel-close-btn" onclick="toggleAddPanel()" title="關閉">×</button>
                <h2>新規イベント追加</h2>
                <form id="addEventForm" method="post" action="<%= request.getContextPath() %>/CalendarManageServlet" onsubmit="return confirmAdd(this)">
                    <input type="hidden" name="action" value="add">
                    <div class="form-group">
                        <label for="newEventDate">日付：</label>
                        <!--<input type="date" id="newEventDate" name="eventDate" required> -->
						<input type="text" id="newEventDate" name="eventDate" placeholder="年/月/日" required>     
					</div>
                    <div class="form-group">
                        <label for="newEventName">イベント名：</label>
                        <input type="text" id="newEventName" name="eventName" maxlength="100" required>
                    </div>
                    <div class="form-group">
                        <label>種別：</label>
                        <div class="radio-group">
                            <input type="radio" id="newIsWorkTrue" name="isWork" value="true" required checked>
                            <label for="newIsWorkTrue">出勤日</label>
                            <input type="radio" id="newIsWorkFalse" name="isWork" value="false">
                            <label for="newIsWorkFalse">休日</label>
                        </div>
                    </div>
                   

                    <%-- 繰り返しイベント設定 --%>
                    <div class="form-group">
                        <label for="newRepeatType">繰り返しタイプ：</label>
                        <select id="newRepeatType" name="repeatType" required>
                            <option value="NONE">単発</option>
                            <option value="DAILY">毎日</option>
                            <option value="WEEKLY">毎週</option>
                            <option value="MONTHLY_DAY">毎月（日付指定）</option>
                            <%-- <option value="MONTHLY_WEEKDAY">毎月（第N週目・曜日指定）</option> --%>
                            <option value="YEARLY">毎年</option>
                        </select>
                    </div>
                    
                    <div class="form-group" id="newRepeatIntervalGroup">
                        <label for="newRepeatInterval">繰り返し間隔（週/月/年ごと）：</label>
                        <input type="number" id="newRepeatInterval" name="repeatInterval" min="1" value="1" style="width: 60px; flex-grow: 0;">
                    </div>

                    <div class="form-group" id="newRepeatDaysOfWeekGroup">
                        <label>繰り返し曜日：</label>
                        <div style="display: grid; grid-template-columns: repeat(3, auto); gap: 10px; margin-top: 5px; justify-content: start;">
                            <label style="white-space: nowrap; margin-right: 0 !important;"><input type="checkbox" id="newDayMon" name="repeatDaysOfWeek" value="1" style="margin-right: 3px;">月</label>
                            <label style="white-space: nowrap; margin-right: 0 !important;"><input type="checkbox" id="newDayTue" name="repeatDaysOfWeek" value="2" style="margin-right: 3px;">火</label>
                            <label style="white-space: nowrap; margin-right: 0 !important;"><input type="checkbox" id="newDayWed" name="repeatDaysOfWeek" value="3" style="margin-right: 3px;">水</label>
                        </div>
                        <div style="display: grid; grid-template-columns: repeat(3, auto); gap: 10px; margin-top: 5px; justify-content: start;">
                            <label style="white-space: nowrap; margin-right: 0 !important;"><input type="checkbox" id="newDayThu" name="repeatDaysOfWeek" value="4" style="margin-right: 3px;">木</label>
                            <label style="white-space: nowrap; margin-right: 0 !important;"><input type="checkbox" id="newDayFri" name="repeatDaysOfWeek" value="5" style="margin-right: 3px;">金</label>
                            <label style="white-space: nowrap; margin-right: 0 !important;"><input type="checkbox" id="newDaySat" name="repeatDaysOfWeek" value="6" style="margin-right: 3px;">土</label>
                        </div>
                        <div style="display: grid; grid-template-columns: repeat(1, auto); gap: 10px; margin-top: 5px; justify-content: start;">
                            <label style="white-space: nowrap; margin-right: 0 !important;"><input type="checkbox" id="newDaySun" name="repeatDaysOfWeek" value="7" style="margin-right: 3px;">日</label>
                        </div>
                    </div>

                    <div class="form-group" id="newRepeatEndDateGroup">
                        <label for="newRepeatEndDate">繰り返し終了日：</label>
                        <input type="date" id="newRepeatEndDate" name="repeatEndDate">
                    </div>

                    <div class="text-right">
                        <button type="submit" class="btn btn-primary">追加</button>
                    </div>
                </form>

            </div>
        </div>

        <div style="text-align: center; margin-top: 10px;">
            <a href="<%= backUrl %>" class="back-link">メニューへ戻る</a>
        </div>
    </div>

    <!-- イベント追加/編集モーダル -->
    <div id="eventModal" class="modal">
        <div class="modal-content">
            <span class="close-button">&times;</span>
            <h2 id="modalTitle">イベント追加</h2>
            <form id="eventForm" action="<%= request.getContextPath() %>/CalendarManageServlet" method="post">
                <input type="hidden" id="action" name="action" value="add">
                <input type="hidden" id="originalEventDate" name="originalEventDate"> <%-- 更新時に元の主キーを保持 --%>

                <div class="modal-form-group">
                    <label for="modalEventDate">日付：</label>
                    <!--  <input type="date" id="modalEventDate" name="eventDate" required> -->
                    <input type="text" id="modalEventDate" name="eventDate" placeholder="年/月/日" required> <%-- 日付編集可能 --%>
                </div>
                <div class="modal-form-group">
                    <label for="modalEventName">イベント名：</label>
                    <input type="text" id="modalEventName" name="eventName" maxlength="100" required>
                </div>
                <div class="modal-form-group">
                    <label>種別：</label>
                    <div class="radio-group">
                        <input type="radio" id="modalIsWorkTrue" name="isWork" value="true" required checked>
                        <label for="modalIsWorkTrue">出勤日</label>
                        <input type="radio" id="modalIsWorkFalse" name="isWork" value="false">
                        <label for="modalIsWorkFalse">休日</label>
                    </div>
                </div>

                <%-- 繰り返しイベント設定 --%>
                <input type="hidden" id="modalRepeatRuleId" name="repeatRuleId"> <%-- 繰り返しルールIDを保持 --%>
                <div class="modal-form-group">
                    <label for="modalRepeatType">繰り返しタイプ：</label>
                    <select id="modalRepeatType" name="repeatType" required>
                        <option value="NONE">単発</option>
                        <option value="DAILY">毎日</option>
                        <option value="WEEKLY">毎週</option>
                        <option value="MONTHLY_DAY">毎月（日付指定）</option>
                        <%-- <option value="MONTHLY_WEEKDAY">毎月（第N週目・曜日指定）</option> --%>
                        <option value="YEARLY">毎年</option>
                    </select>
                </div>
                
                <div class="modal-form-group" id="modalRepeatIntervalGroup">
                    <label for="modalRepeatInterval">繰り返し間隔（週/月/年ごと）：</label>
                    <input type="number" id="modalRepeatInterval" name="repeatInterval" min="1" value="1" style="width: 60px; flex-grow: 0;">
                </div>

                <div class="modal-form-group" id="modalRepeatDaysOfWeekGroup">
                    <label>繰り返し曜日：</label>
                    <div style="display: grid; grid-template-columns: repeat(3, auto); gap: 10px; margin-top: 5px; justify-content: start;">
                        <label style="white-space: nowrap; margin-right: 0 !important;"><input type="checkbox" id="modalDayMon" name="repeatDaysOfWeek" value="1" style="margin-right: 3px;">月</label>
                        <label style="white-space: nowrap; margin-right: 0 !important;"><input type="checkbox" id="modalDayTue" name="repeatDaysOfWeek" value="2" style="margin-right: 3px;">火</label>
                        <label style="white-space: nowrap; margin-right: 0 !important;"><input type="checkbox" id="modalDayWed" name="repeatDaysOfWeek" value="3" style="margin-right: 3px;">水</label>
                    </div>
                    <div style="display: grid; grid-template-columns: repeat(3, auto); gap: 10px; margin-top: 5px; justify-content: start;">
                        <label style="white-space: nowrap; margin-right: 0 !important;"><input type="checkbox" id="modalDayThu" name="repeatDaysOfWeek" value="4" style="margin-right: 3px;">木</label>
                        <label style="white-space: nowrap; margin-right: 0 !important;"><input type="checkbox" id="modalDayFri" name="repeatDaysOfWeek" value="5" style="margin-right: 3px;">金</label>
                        <label style="white-space: nowrap; margin-right: 0 !important;"><input type="checkbox" id="modalDaySat" name="repeatDaysOfWeek" value="6" style="margin-right: 3px;">土</label>
                    </div>
                    <div style="display: grid; grid-template-columns: repeat(1, auto); gap: 10px; margin-top: 5px; justify-content: start;">
                        <label style="white-space: nowrap; margin-right: 0 !important;"><input type="checkbox" id="modalDaySun" name="repeatDaysOfWeek" value="7" style="margin-right: 3px;">日</label>
                    </div>
                </div>

                <div class="modal-form-group" id="modalRepeatEndDateGroup">
                    <label for="modalRepeatEndDate">繰り返し終了日：</label>
                    <input type="date" id="modalRepeatEndDate" name="repeatEndDate">
                </div>

                <div class="modal-buttons">
                    <button type="submit" class="btn-save">保存</button>
                    <button type="button" id="modalDeleteButton" class="btn-delete" style="display: none;">削除</button> <%-- 編集時のみ表示 --%>
                    <button type="button" class="btn-cancel">キャンセル</button>
                </div>
            </form>
        </div>
    </div>

    <!-- FullCalendar JavaScript -->
    <script src='https://cdn.jsdelivr.net/npm/fullcalendar@6.1.11/index.global.min.js'></script>
    <script>
        // 左侧边栏（イベント一覧）の切り替え機能
        function toggleListPanel() {
            var listPanel = document.getElementById('event-list-panel');
            var overlay = document.getElementById('sidebarOverlay');
            var listToggleBtn = document.getElementById('listToggleBtn');
            
            // 左侧パネルを開く前に右侧パネルを閉じる
            var addPanel = document.getElementById('event-add-panel');
            if (addPanel.classList.contains('panel-open')) {
                toggleAddPanel();
            }
            
            listPanel.classList.toggle('panel-open');
            overlay.classList.toggle('active');
            
            if (listPanel.classList.contains('panel-open')) {
                listToggleBtn.textContent = '閉じる';
            } else {
                listToggleBtn.textContent = '一覧';
            }
        }
        
        // 右侧边栏（新規イベント追加）の切り替え機能
        function toggleAddPanel() {
            var addPanel = document.getElementById('event-add-panel');
            var overlay = document.getElementById('sidebarOverlay');
            var addToggleBtn = document.getElementById('addToggleBtn');
            
            // 右侧パネルを開く前に左侧パネルを閉じる
            var listPanel = document.getElementById('event-list-panel');
            if (listPanel.classList.contains('panel-open')) {
                toggleListPanel();
            }
            
            addPanel.classList.toggle('panel-open');
            overlay.classList.toggle('active');
            
            if (addPanel.classList.contains('panel-open')) {
                addToggleBtn.textContent = '閉じる';
            } else {
                addToggleBtn.textContent = '新規追加';
            }
        }

        var calendar; // グローバル変数として定義
        var modal, modalTitle, modalDeleteButton, modalRepeatTypeSelect, modalRepeatIntervalInput, modalRepeatDaysOfWeekCheckboxes, rulesByRuleId; // グローバル変数として定義
        var toggleRepeatFields, resetModalForm; // 関数もグローバルに

        document.addEventListener('DOMContentLoaded', function() {
            var calendarEl = document.getElementById('calendar');
            var eventListPanel = document.getElementById('event-list-panel');
            var eventAddPanel = document.getElementById('event-add-panel');
            
            // モーダル関連のDOM要素
            modal = document.getElementById('eventModal');
            modalTitle = document.getElementById('modalTitle');
            var closeButton = document.querySelector('.close-button');
            var cancelButton = document.querySelector('.btn-cancel');
            modalDeleteButton = document.getElementById('modalDeleteButton');
            var eventForm = document.getElementById('eventForm');

            // モーダル内の繰り返し設定フィールドのDOM要素
            modalRepeatTypeSelect = document.getElementById('modalRepeatType');
            var modalRepeatIntervalGroup = document.getElementById('modalRepeatIntervalGroup');
            modalRepeatIntervalInput = document.getElementById('modalRepeatInterval');
            var modalRepeatDaysOfWeekGroup = document.getElementById('modalRepeatDaysOfWeekGroup');
            modalRepeatDaysOfWeekCheckboxes = document.querySelectorAll('#modalRepeatDaysOfWeekGroup input[type="checkbox"]');
            var modalRepeatEndDateGroup = document.getElementById('modalRepeatEndDateGroup');

            // JSPから渡された繰り返しルールをJavaScriptで利用できるよう変換
            rulesByRuleId = JSON.parse('<%= rulesByRuleIdJson %>');
            // サーブレットで事前にJSON変換されたデータを使用

            // FullCalendar初期化
            calendar = new FullCalendar.Calendar(calendarEl, {
                initialView: 'dayGridMonth', // 初期表示は月ビュー
                locale: 'ja', // 日本語化
                headerToolbar: {
                    left: 'prev,next today',
                    center: 'title',
                    right: 'dayGridMonth' // 月ビューのみ
                },
                height: '100%', // コンテナの高さを100%使用
                expandRows: true, // 行の高さを均等に広げる
                navLinks: false, // 日付クリック無効
                <% if (isAdmin) { %>
                editable: true, // ドラッグ＆ドロップでイベント移動（今回の要件では不要だが、汎用的に）
                selectable: true, // 日付範囲選択
                dayMaxEvents: 3, // 最大3個のイベントを表示、それ以上は「+N more」表示

                events: JSON.parse('<%= eventsJson %>'), // サーブレットから渡されたイベントデータ

                // 日付セルクリック時の処理
                dateClick: function(info) {
                    openModalForAdd(info.dateStr); // 新規追加用にモーダルを開く
                },
                // イベントクリック時の処理
                eventClick: function(info) {
                    openModalForEdit(info.event); // 編集用にモーダルを開く
                }
                <% } else { %>
                editable: false,
                selectable: false,
                dayMaxEvents: 3,
                events: JSON.parse('<%= eventsJson %>'),
                dateClick: function(info) {
                    // 何もしない（閲覧専用）
                },
                eventClick: function(info) {
                    // 何もしない（閲覧専用）
                }
                <% } %>
            });
            calendar.render(); // カレンダーを描画

            // --- モーダル関連関数 ---
            function openModalForAdd(dateStr) {
                resetModalForm(); // フォームをリセット
                modalTitle.textContent = 'イベント追加';
                document.getElementById('action').value = 'add';
                document.getElementById('modalEventDate').value = dateStr; // クリックした日付をセット
                modalDeleteButton.style.display = 'none'; // 削除ボタンを非表示
                document.getElementById('originalEventDate').value = ''; // 元の日付をクリア
                toggleRepeatFields(modalRepeatTypeSelect.value); // 繰り返しフィールドの初期状態を調整
                modal.style.display = 'block'; // モーダル表示
            }

            function openModalForEdit(eventInfo) {
                resetModalForm(); // フォームをリセット
                modalTitle.textContent = 'イベント編集';
                document.getElementById('action').value = 'update';
                modalDeleteButton.style.display = 'inline-block'; // 削除ボタンを表示

                // 既存イベントデータをフォームにセット
                document.getElementById('modalEventDate').value = eventInfo.startStr;
                document.getElementById('originalEventDate').value = eventInfo.startStr; // 元の日付を保持
                document.getElementById('modalEventName').value = eventInfo.title;
                document.getElementById(eventInfo.extendedProps.isWork ? 'modalIsWorkTrue' : 'modalIsWorkFalse').checked = true;

                // 繰り返しルールをフォームにセット
                const repeatRuleId = eventInfo.extendedProps.repeatRuleId;
                if (repeatRuleId) {
                    const rule = rulesByRuleId[repeatRuleId];
                    if (rule) {
                        modalRepeatTypeSelect.value = rule.repeatType;
                        modalRepeatIntervalInput.value = rule.repeatInterval;
                        
                        // 繰り返し曜日チェックボックスのセット
                        modalRepeatDaysOfWeekCheckboxes.forEach(checkbox => {
                            checkbox.checked = rule.repeatDaysOfWeek.split(',').includes(checkbox.value);
                        });

                        document.getElementById('modalRepeatEndDate').value = rule.repeatEndDate;
                        document.getElementById('modalRepeatRuleId').value = rule.ruleId; // ルールIDを保持
                    }
                } else {
                    // 単発イベントの場合、繰り返し設定を「単発」にリセット
                    modalRepeatTypeSelect.value = 'NONE';
                }
                toggleRepeatFields(modalRepeatTypeSelect.value); // 繰り返し設定フィールドの表示/非表示を調整

                modal.style.display = 'block'; // モーダル表示
            }

            resetModalForm = function() {
                eventForm.reset(); 
                document.getElementById('action').value = 'add';
                document.getElementById('modalRepeatRuleId').value = '';
                document.getElementById('modalIsWorkTrue').checked = true;
                modalRepeatTypeSelect.value = 'NONE';
                modalRepeatIntervalInput.value = '1';
                modalRepeatDaysOfWeekCheckboxes.forEach(checkbox => checkbox.checked = false);
                document.getElementById('modalRepeatEndDate').value = '';
                toggleRepeatFields(modalRepeatTypeSelect.value);
            }

            // 繰り返し設定フィールドの表示/非表示を切り替える関数
            toggleRepeatFields = function(repeatType) {
                const isRepeating = (repeatType !== 'NONE');
                const isWeeklyOrMonthlyWeekday = (repeatType === 'WEEKLY' || repeatType === 'MONTHLY_WEEKDAY');

                // 繰り返し間隔の表示/非表示とラベル変更
                const modalIntervalLabel = modalRepeatIntervalGroup.querySelector('label');
                modalRepeatIntervalGroup.style.display = (isRepeating ? 'flex' : 'none');
                modalRepeatIntervalInput.required = isRepeating;
                
                // ラベルを繰り返しタイプに応じて変更
                switch(repeatType) {
                    case 'DAILY':
                        modalIntervalLabel.textContent = '繰り返し間隔（日ごと）：';
                        break;
                    case 'WEEKLY':
                        modalIntervalLabel.textContent = '繰り返し間隔（週ごと）：';
                        break;
                    case 'MONTHLY_DAY':
                        modalIntervalLabel.textContent = '繰り返し間隔（月ごと）：';
                        break;
                    case 'YEARLY':
                        modalIntervalLabel.textContent = '繰り返し間隔（年ごと）：';
                        break;
                    default:
                        modalIntervalLabel.textContent = '繰り返し間隔（週/月/年ごと）：';
                }

                // 繰り返し曜日の表示/非表示と無効化
                const modalDaysOfWeekLabels = document.querySelectorAll('#modalRepeatDaysOfWeekGroup label');
                modalRepeatDaysOfWeekGroup.style.display = (isWeeklyOrMonthlyWeekday ? 'flex' : 'none');
                modalRepeatDaysOfWeekCheckboxes.forEach(checkbox => {
                    checkbox.required = false; // 曜日選択を任意とする
                    // DAILYの場合は無効化して灰色に
                    if (repeatType === 'DAILY') {
                        checkbox.disabled = true;
                        checkbox.checked = false;
                    } else {
                        checkbox.disabled = false;
                    }
                });
                
                // ラベルも灰色に
                modalDaysOfWeekLabels.forEach(label => {
                    if (repeatType === 'DAILY') {
                        label.style.color = '#ccc';
                    } else {
                        label.style.color = '';
                    }
                });

                // 繰り返し終了日の表示/非表示
                modalRepeatEndDateGroup.style.display = (isRepeating ? 'flex' : 'none');
            }

            // モーダルを閉じるボタン
            closeButton.onclick = function() {
                modal.style.display = 'none';
            };
            cancelButton.onclick = function() {
                modal.style.display = 'none';
            };
            // モーダルの外をクリックで閉じる
            window.onclick = function(event) {
                if (event.target == modal) {
                    modal.style.display = 'none';
                }
            };

            // 削除ボタンクリック時の処理
            modalDeleteButton.onclick = function() {
                if (confirm('このイベントを削除してもよろしいですか？')) {
                    document.getElementById('action').value = 'delete';
                    eventForm.submit();
                }
            };

            // input[type="date"] がフォーカスされたとき、またはクリックされたときにピッカーを表示
            document.querySelectorAll('input[type="date"]').forEach(input => {
                input.addEventListener('focus', () => { if (input.showPicker) input.showPicker(); });
                input.addEventListener('click', () => { if (input.showPicker) input.showPicker(); });
            });

            // サイドパネルの新規追加フォームの繰り返しタイプ選択時の処理
            document.getElementById('newRepeatType').addEventListener('change', function() {
                toggleSidePanelRepeatFields(this.value);
            });
            // 初期表示時の繰り返しフィールドの表示/非表示を調整
            toggleSidePanelRepeatFields(document.getElementById('newRepeatType').value);

            // モーダルの繰り返しタイプ選択時の処理
            modalRepeatTypeSelect.addEventListener('change', function() {
                toggleRepeatFields(this.value);
            });

            // サイドパネルの繰り返し設定フィールドの表示/非表示を切り替える関数
            function toggleSidePanelRepeatFields(repeatType) {
                const isRepeating = (repeatType !== 'NONE');
                const isWeeklyOrMonthlyWeekday = (repeatType === 'WEEKLY' || repeatType === 'MONTHLY_WEEKDAY');

                // 繰り返し間隔の表示/非表示とラベル変更
                const intervalGroup = document.getElementById('newRepeatIntervalGroup');
                const intervalLabel = intervalGroup.querySelector('label');
                intervalGroup.style.display = (isRepeating ? 'flex' : 'none');
                document.getElementById('newRepeatInterval').required = isRepeating;
                
                // ラベルを繰り返しタイプに応じて変更
                switch(repeatType) {
                    case 'DAILY':
                        intervalLabel.textContent = '繰り返し間隔（日ごと）：';
                        break;
                    case 'WEEKLY':
                        intervalLabel.textContent = '繰り返し間隔（週ごと）：';
                        break;
                    case 'MONTHLY_DAY':
                        intervalLabel.textContent = '繰り返し間隔（月ごと）：';
                        break;
                    case 'YEARLY':
                        intervalLabel.textContent = '繰り返し間隔（年ごと）：';
                        break;
                    default:
                        intervalLabel.textContent = '繰り返し間隔（週/月/年ごと）：';
                }

                // 繰り返し曜日の表示/非表示と無効化
                const daysOfWeekGroup = document.getElementById('newRepeatDaysOfWeekGroup');
                const daysOfWeekCheckboxes = document.querySelectorAll('#newRepeatDaysOfWeekGroup input[type="checkbox"]');
                const daysOfWeekLabels = document.querySelectorAll('#newRepeatDaysOfWeekGroup label');
                
                daysOfWeekGroup.style.display = (isWeeklyOrMonthlyWeekday ? 'flex' : 'none');
                daysOfWeekCheckboxes.forEach(checkbox => {
                    checkbox.required = false; // 曜日選択を任意とする
                    // DAILYの場合は無効化して灰色に（表示はするが操作不可）
                    if (repeatType === 'DAILY') {
                        checkbox.disabled = true;
                        checkbox.checked = false;
                    } else {
                        checkbox.disabled = false;
                    }
                });
                
                // ラベルも灰色に
                daysOfWeekLabels.forEach(label => {
                    if (repeatType === 'DAILY') {
                        label.style.color = '#ccc';
                    } else {
                        label.style.color = '';
                    }
                });

                // 繰り返し終了日の表示/非表示
                document.getElementById('newRepeatEndDateGroup').style.display = (isRepeating ? 'flex' : 'none');
            }
        });

        // テーブルの編集ボタンからモーダルを開く関数
        function openEditModal(eventDate, eventName, isWork, repeatRuleId) {
            // フォームをリセット
            resetModalForm();
            
            // モーダルのタイトルと操作を設定
            modalTitle.textContent = 'イベント編集';
            document.getElementById('action').value = 'update';
            modalDeleteButton.style.display = 'inline-block';

            // モーダルフォームに値を設定
            document.getElementById('modalEventDate').value = eventDate;
            document.getElementById('originalEventDate').value = eventDate;
            document.getElementById('modalEventName').value = eventName;
            document.getElementById(isWork ? 'modalIsWorkTrue' : 'modalIsWorkFalse').checked = true;

            // 繰り返しルール情報をセット
            if (repeatRuleId && repeatRuleId !== 'null') {
                const rule = rulesByRuleId[repeatRuleId];
                if (rule) {
                    modalRepeatTypeSelect.value = rule.repeatType;
                    modalRepeatIntervalInput.value = rule.repeatInterval;
                    modalRepeatDaysOfWeekCheckboxes.forEach(checkbox => {
                        checkbox.checked = rule.repeatDaysOfWeek && rule.repeatDaysOfWeek.split(',').includes(checkbox.value);
                    });
                    document.getElementById('modalRepeatEndDate').value = rule.repeatEndDate || '';
                    document.getElementById('modalRepeatRuleId').value = rule.ruleId;
                }
            } else {
                modalRepeatTypeSelect.value = 'NONE';
                document.getElementById('modalRepeatRuleId').value = '';
            }
            
            // 繰り返しフィールドの表示/非表示を調整
            toggleRepeatFields(modalRepeatTypeSelect.value);

            // モーダルを表示
            modal.style.display = 'block';
        }
        
        // 削除確認関数
        function confirmDelete(eventDate, eventName) {
            if (confirm('イベント（日付：「' + eventDate + '」、イベント名：「' + eventName + '」）を削除してもよろしいですか？')) {
                var formattedEventDateId = eventDate.replace(/-/g, '_');
                document.getElementById('deleteForm-' + formattedEventDateId).submit();
            }
        }

        // フォーム送信時の確認（EventManageServletのconfirmAdd/confirmUpdateと連携）
        // このJSPではJSのconfirmのみで、Servlet側でバリデーションとメッセージ表示を行う
        // サイドパネルの追加フォームとモーダルフォームで共用
        // 2025/8/7 米村　カレンダー未入力の修正のためconfirmAddとconfirmEditを追加
        function confirmAdd(form) {
		    const date = document.getElementById("newEventDate").value;
		    const eventName = document.getElementById("newEventName").value;
		
		    if (!date) {
		        alert("日付を入力してください。");
		        return false;
		    }
		
		    if (!eventName) {
		        alert("イベント名を入力してください。");
		        return false;
		    }
		
		    return true;
		}
        function confirmEdit(form) {
            const date = document.getElementById("modalEventDate").value;
            const eventName = document.getElementById("modalEventName").value;

            if (!date) {
                alert("日付を入力してください。");
                return false;
            }

            if (!eventName) {
                alert("イベント名を入力してください。");
                return false;
            }

            return true;
        }
        function confirmAdd(form) {
            var eventDate = form.eventDate.value;
            var eventName = form.eventName.value;
            var isWork = form.isWork.value;
            var repeatType = form.repeatType.value;
            var repeatInterval = form.repeatInterval.value;
            var repeatDaysOfWeek = form.querySelectorAll('input[name="repeatDaysOfWeek"]:checked');
            var repeatEndDate = form.repeatEndDate.value;
            // var errorMessageDiv = document.getElementById("errorMessage"); // ページ上部のメッセージエリア

            // errorMessageDiv.innerHTML = ""; // エラーメッセージをクリア

            if (eventDate.trim() === '' || eventName.trim() === '' || isWork.trim() === '') {
                alert("日付、イベント名、種別の指定は必須です。");
                return false;
            }

            if (repeatType !== 'NONE') {
                if (repeatInterval.trim() === '' || isNaN(repeatInterval) || parseInt(repeatInterval) <= 0) {
                    alert("繰り返し間隔は1以上の半角数字で入力してください。");
                    return false;
                }
                // 毎週の場合でも曜日選択は任意とする（単発日のみの追加も可能）
            }
            return confirm('イベント（日付：「' + eventDate + '」、イベント名：「' + eventName + '」）を追加してもよろしいですか？');
        }
        
        function confirmUpdate(form, eventDate) {
            var eventName = form.eventName.value;
            var isWork = form.isWork.value;
            var repeatType = form.repeatType.value;
            var repeatInterval = form.repeatInterval.value;
            var repeatDaysOfWeek = form.querySelectorAll('input[name="repeatDaysOfWeek"]:checked');
            var repeatEndDate = form.repeatEndDate.value;
            // var errorMessageDiv = document.getElementById("errorMessage"); // ページ上部のメッセージエリア

            // errorMessageDiv.innerHTML = ""; // エラーメッセージをクリア

            if (eventName.trim() === '' || isWork.trim() === '') {
                alert("イベント名、種別の指定は必須です。");
                return false;
            }

            if (repeatType !== 'NONE') {
                if (repeatInterval.trim() === '' || isNaN(repeatInterval) || parseInt(repeatInterval) <= 0) {
                    alert("繰り返し間隔は1以上の半角数字で入力してください。");
                    return false;
                }
                // 毎週の場合でも曜日選択は任意とする（単発日のみの追加も可能）
            }
            return confirm('イベント（日付：「' + eventDate + '」）の情報を更新してもよろしいですか？');
        }

        // イベントクリックから呼び出される openModalForEdit 用のダミー関数 (FullCalendarのinfo.eventを変換)
        function openModalForEdit(eventInfo) {
            resetModalForm(); // フォームをリセット
            modalTitle.textContent = 'イベント編集';
            document.getElementById('action').value = 'update';
            modalDeleteButton.style.display = 'inline-block'; // 削除ボタンを表示

            // 既存イベントデータをフォームにセット
            document.getElementById('modalEventDate').value = eventInfo.startStr;
            document.getElementById('originalEventDate').value = eventInfo.startStr; // 元の日付を保持
            document.getElementById('modalEventName').value = eventInfo.title;
            document.getElementById(eventInfo.extendedProps.isWork ? 'modalIsWorkTrue' : 'modalIsWorkFalse').checked = true;

            // 繰り返しルールをフォームにセット
            const repeatRuleId = eventInfo.extendedProps.repeatRuleId;
            if (repeatRuleId) {
                const rule = rulesByRuleId[repeatRuleId];
                if (rule) {
                    document.getElementById('modalRepeatType').value = rule.repeatType;
                    document.getElementById('modalRepeatInterval').value = rule.repeatInterval;
                    
                    // 繰り返し曜日チェックボックスのセット
                    modalRepeatDaysOfWeekCheckboxes.forEach(checkbox => {
                        checkbox.checked = rule.repeatDaysOfWeek.split(',').includes(checkbox.value);
                    });

                    document.getElementById('modalRepeatEndDate').value = rule.repeatEndDate;
                    document.getElementById('modalRepeatRuleId').value = rule.ruleId; // ルールIDを保持
                }
            } else {
                // 単発イベントの場合、繰り返し設定を「単発」にリセット
                document.getElementById('modalRepeatType').value = 'NONE';
            }
            toggleRepeatFields(document.getElementById('modalRepeatType').value); // 繰り返し設定フィールドの表示/非表示を調整

            modal.style.display = 'block'; // モーダル表示
        }
        // 2025/8/7 米村　カレンダー未入力修正のため flatpickr("#newEventDate", とflatpickr("#modalEventDate",実装
        flatpickr("#newEventDate", {
        	  dateFormat: "Y-m-d",
        	  allowInput: true
        	});
        flatpickr("#modalEventDate", {
            dateFormat: "Y-m-d",
            allowInput: true
        });

    </script>
</body>
</html>
