<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="kintai.UserBean" %>
<%@ page import="kintai.WorkTimeBean" %>
<%@ page import="kintai.BreakBean" %>
<%@ page import="kintai.ProjectBean" %> <%-- ProjectBean をインポート --%>
<%@ page import="kintai.KinmuManageBean" %> <%-- WorkAlloc 内部クラスを使用するため --%>
<%@ page import="java.time.LocalDate" %>
<%@ page import="java.time.format.DateTimeFormatter" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.HashMap" %>
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

    // サーブレットから渡されたデータを取得
    String targetDateStr = (String) request.getAttribute("targetDate");
    Map<String, String> workTimeData = (Map<String, String>) request.getAttribute("workTimeData");
    List<Map<String, String>> breakList = (List<Map<String, String>>) request.getAttribute("breakList");
    List<KinmuManageBean.WorkAlloc> workAllocs = (List<KinmuManageBean.WorkAlloc>) request.getAttribute("workAllocs"); // workDetails から workAllocs へ変更
    List<ProjectBean> projectList = (List<ProjectBean>) request.getAttribute("projectList"); // gyomuList から projectList へ変更

    String successMessage = (String) request.getAttribute("successMessage");
    String errorMessage = (String) request.getAttribute("errorMessage");

    // nullチェックと初期化
    if (workTimeData == null) workTimeData = new HashMap<>();
    if (breakList == null) breakList = new java.util.ArrayList<>();
    if (workAllocs == null) workAllocs = new java.util.ArrayList<>(); // workDetails から workAllocs へ変更
    if (projectList == null) projectList = new java.util.ArrayList<>(); // gyomuList から projectList へ変更
    
    // 現在表示している日付をLocalDateオブジェクトに変換
    LocalDate targetDate = LocalDate.parse(targetDateStr);
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日");
    String formattedTargetDate = targetDate.format(formatter);

    // メニューへ戻るリンクのURLを権限に応じて設定
    String backUrl = (userRoleId == 1) ? request.getContextPath() + "/AdminMenuServlet" : request.getContextPath() + "/web/menu.jsp";
%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>勤務時間管理</title>
    <style>
    	/* 2025/8/8 米村 中身のモーダル部分を横スクロールして見た目を良くするため修正*/
        body {
            font-family: 'メイリオ', sans-serif;
            background-color: #f0f0f0;
            margin: 0;
            padding: 20px;
            
        }
        .container {
            max-width: 1200px; /* 他ページと同じ幅に変更 */
            margin: 0 auto;
            background-color: white;
            padding: 20px;
            border-radius: 8px;
            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
        }
        .header {
            display: flex;
            justify-content: space-between;
            align-items: flex-start;
            padding: 10px 20px;
            background: #fff;
            border-bottom: 1px solid #ccc;
            margin: -20px -20px 20px -20px;
            border-top-left-radius: 8px;
            border-top-right-radius: 8px;
        }
        .user-info {
            display: flex;
            flex-direction: column;
            line-height: 1.5;
            text-align: left;
            font-size: 13px;
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
            margin-top: 0;
            text-align: center; /* 他ページと同じ中央揃え */
            margin: 10px 0 5px 0;
            padding: 0;
            padding-bottom: 5px;
            font-size: 18px;
        }
        /* メッセージ表示エリア */
        .message {
            padding: 10px;
            margin-bottom: 20px;
            border-radius: 4px;
            text-align: center;
        }
        .success-message {
            background-color: #d4edda;
            color: #155724;
            border: 1px solid #c3e6cb;
        }
        .error-message {
            background-color: #f8d7da;
            color: #721c24;
            border: 1px solid #f5c6cb;
        }

        /* メインコンテンツレイアウト */
        .main-content {
            display: flex;
            gap: 20px;
            margin-top: 20px;
        }
        
        .section {
            flex: 1;
            margin-bottom: 20px;
            padding: 16px;
            border: 1px solid #dee2e6;
            border-radius: 8px;
            background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%);
            box-shadow: 0 2px 4px rgba(0,0,0,0.05);
        }
        .section h2 {
            margin-top: 0;
            color: #495057;
            border-bottom: 2px solid #007bff;
            padding-bottom: 8px;
            margin-bottom: 15px;
            font-size: 16px;
        }
        .form-group {
            margin-bottom: 15px;
            display: flex;
            align-items: center;
        }
        .form-group label {
            width: 120px;
            font-weight: bold;
            flex-shrink: 0; /* ラベルが縮まないように */
        }
        .form-group input[type="date"],
        .form-group input[type="time"],
        .form-group input[type="text"],
        .form-group select {
            flex-grow: 1; /* 入力フィールドが残りのスペースを占める */
            padding: 8px;
            border: 1px solid #ced4da;
            border-radius: 4px;
        }
        .form-group input[type="text"].description-input {
            width: 100%; /* 説明欄は広めに */
        }
        button[type="submit"], .button {
            padding: 6px 12px;
            border: none;
            border-radius: 4px;
            cursor: pointer;
            font-size: 12px;
            margin-left: 8px;
            transition: all 0.2s;
        }
        .btn-primary { background-color: #007bff; color: white; }
        .btn-primary:hover { background-color: #0056b3; }
        .btn-success { background-color: #28a745; color: white; }
        .btn-success:hover { background-color: #218838; }
        .btn-danger { background-color: #dc3545; color: white; }
        .btn-danger:hover { background-color: #c82333; }
        .btn-secondary { background-color: #6c757d; color: white; }
        .btn-secondary:hover { background-color: #545b62; }

        table {
            width: 100%;
            border-collapse: collapse;
            margin-top: 10px;
            border-radius: 8px;
            overflow: hidden;
            box-shadow: 0 2px 8px rgba(0,0,0,0.08);
            background-color: white;
        }
        table th, table td {
            border: 1px solid #dee2e6;
            padding: 8px 6px;
            text-align: center;
            vertical-align: middle;
            font-size: 12px;
        }
        table th {
            background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%);
            font-weight: 600;
            color: #495057;
            border-bottom: 2px solid #dee2e6;
        }
        .action-cell button {
            margin: 0 3px;
        }

        .date-navigation {
            display: flex;
            justify-content: center;
            align-items: center;
            margin-bottom: 20px;
            gap: 15px;
        }
        .date-navigation button {
            background-color: #007bff;
            color: white;
            padding: 10px 15px;
            border-radius: 5px;
            cursor: pointer;
            border: none;
            font-size: 1em;
        }
        .date-navigation button:hover {
            background-color: #0056b3;
        }
        .date-navigation span {
            font-size: 16px;
            font-weight: bold;
            color: #333;
        }

        .back-link {
            display: inline-block;
            margin-top: 30px;
            padding: 8px 16px;
            background-color: #6c757d;
            color: white;
            text-decoration: none;
            border-radius: 4px;
            text-align: center;
            font-size: 14px;
        }
        .back-link:hover {
            background-color: #545b62;
        }
        .scrollable-panel-wrapper {
		    display: flex;
		    flex-wrap: nowrap;
		    overflow-x: auto;
		    gap: 20px;
		    padding: 10px;
		    margin-bottom: 20px;
		    border: 1px solid #dee2e6;
		    border-radius: 8px;
		    width: 100%;
		    background: linear-gradient(135deg, #ffffff 0%, #f8f9fa 100%);
		}
		
		.scrollable-panel {
		    flex: 1;
		    min-width: 300px;
		    height: auto;
		    overflow-y: auto;
		    /*background-color: #ffffff;*/
		    border-radius: 8px;
		    padding: 16px;
		    box-shadow: 0 2px 8px rgba(0,0,0,0.05); /* 少し柔らかい影に */
		}
		
		.scrollable-panel h2 {
		    margin-top: 0;
		    font-size: 15px;
		    color: #343a40;
		    border-bottom: 3px solid #007bff;
		    padding-bottom: 5px;
		    margin-bottom: 10px;
		    gap: 8px; /* アイコンとの余白 */
		}
    </style>
    <script>
        // 日付ナビゲーション用
        function navigateDate(days) {
            const currentDate = new Date('<%= targetDate.toString() %>');
            currentDate.setDate(currentDate.getDate() + days);
            const year = currentDate.getFullYear();
            const month = String(currentDate.getMonth() + 1).padStart(2, '0');
            const day = String(currentDate.getDate()).padStart(2, '0');
            window.location.href = '<%= request.getContextPath() %>/KinmuManageServlet?targetDate=' + year + '-' + month + '-' + day;
        }

        // 確認メッセージ用
        function confirmAction(message) {
            // alert() の代わりに確認ダイアログを作成
            // 実運用では、もっとリッチなモーダルダイアログを実装することが推奨されます
            return confirm(message);
        }

        // 新しい関数: input[type="time"] がフォーカスされたときにピッカーを表示
        document.addEventListener('DOMContentLoaded', (event) => {
            const timeInputs = document.querySelectorAll('input[type="time"]');
            timeInputs.forEach(input => {
                // 'focus' イベントと 'click' イベントの両方で showPicker を呼び出す
                // これにより、クリック（フォーカスと同時に発生）またはタブキーでのフォーカス移動でピッカーが表示される
                input.addEventListener('focus', () => {
                    if (input.showPicker) { // showPicker がサポートされているかチェック
                        input.showPicker();
                    }
                });
                input.addEventListener('click', () => {
                    if (input.showPicker) {
                        input.showPicker();
                    }
                });
            });
        });
        function onDateChange() {
            const selectedDate = document.getElementById("datePicker").value;
            if (selectedDate) {
                window.location.href = '<%= request.getContextPath() %>/KinmuManageServlet?targetDate=' + selectedDate;
            }
        }
    </script>
<!--    FlatpickrのCSS -->
<!--	<link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/flatpickr/dist/themes/material_blue.css">-->
	
<!--	 FlatpickrのJS本体 -->
<!--	<script src="https://cdn.jsdelivr.net/npm/flatpickr"></script>-->
	
<!--	 日本語ローカライズ -->
<!--	<script src="https://cdn.jsdelivr.net/npm/flatpickr/dist/l10n/ja.js"></script>-->
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

        <h1>勤務時間管理</h1>

        <%-- メッセージ表示 --%>
        <% if (successMessage != null) { %>
            <div class="message success-message">
                <%= successMessage %>
            </div>
        <% } %>
        <% if (errorMessage != null) { %>
            <div class="message error-message">
                <%= errorMessage %>
            </div>
        <% } %>

        <%-- 日付ナビゲーション --%>
        <div class="date-navigation">
            <button type="button" onclick="navigateDate(-1)">&lt; 前日</button>
<!--             Flatpickrに対応したinput要素 -->
<!--    		<input type="text" id="calendar" value="<%= formattedTargetDate %>" style="text-align: center; font-size: 14px; width: 160px; padding: 5px;" readonly>-->
			<input type="date" id="datePicker" value="<%= targetDate.toString() %>" onchange="onDateChange()" />
            <button type="button" onclick="navigateDate(1)">翌日 &gt;</button>
        </div>

        <%-- メインコンテンツエリア（一行配置） --%>
        <!-- <div style="display: flex; gap: 20px; margin-bottom: 20px;"> -->
        <div class="scrollable-panel-wrapper">
            <%-- 出退勤・休憩時間修正エリア（左側） --%>
            <div class="scrollable-panel">
                <h2>📋 出退勤時間更新</h2>
                <form action="<%= request.getContextPath() %>/KinmuManageServlet" method="post" onsubmit="return confirmAction('この内容で出退勤時間を更新しますか？');">
                    <input type="hidden" name="action" value="update_work_time">
                    <input type="hidden" name="targetDate" value="<%= targetDateStr %>">
                    <input type="hidden" name="recId" value="<%= workTimeData.getOrDefault("recId", "") %>">
                    
                    <div class="form-group">
                        <label for="clockInTime" style="width: 60px; font-size: 11px;">出勤:</label>
                        <input type="time" id="clockInTime" name="clockInTime" value="<%= workTimeData.getOrDefault("clockInTime", "") %>" style="font-size: 11px; width: 120px;">
                    </div>
                    <div class="form-group">
                        <label for="clockOutTime" style="width: 60px; font-size: 11px;">退勤:</label>
                        <input type="time" id="clockOutTime" name="clockOutTime" value="<%= workTimeData.getOrDefault("clockOutTime", "") %>" style="font-size: 11px; width: 120px;">
                    </div>
                    <div style="text-align: right; margin-top: 10px;">
                        <button type="submit" class="btn-primary" style="font-size: 11px; padding: 4px 8px;">更新</button>
                    </div>
                </form>
            </div>

            <%-- 休憩時間エリア（中央） --%>
            <div class="scrollable-panel">
                <h2>🕐 休憩時間</h2>
                <div style="max-height: 200px; overflow-y: auto;">
                    <table style="width: 100%; border-collapse: collapse; font-size: 10px;">
                        <thead>
                            <tr>
                                <th style="border: 1px solid #dee2e6; padding: 4px; font-size: 10px;">開始</th>
                                <th style="border: 1px solid #dee2e6; padding: 4px; font-size: 10px;">終了</th>
                                <th style="border: 1px solid #dee2e6; padding: 4px; font-size: 10px;">操作</th>
                            </tr>
                        </thead>
                        <tbody>
                            <% if (breakList.isEmpty()) { %>
                                <tr><td colspan="3" style="color: #6c757d; text-align: center; padding: 8px; font-size: 10px;">休憩記録なし</td></tr>
                            <% } else { %>
                                <% for (Map<String, String> breakItem : breakList) { %>
                                    <tr>
                                        <td style="border: 1px solid #dee2e6; padding: 3px; text-align: center;"><%= breakItem.getOrDefault("startTime", "---") %></td>
                                        <td style="border: 1px solid #dee2e6; padding: 3px; text-align: center;"><%= breakItem.getOrDefault("endTime", "---") %></td>
                                        <td style="border: 1px solid #dee2e6; padding: 3px; text-align: center;">
                                            <form action="<%= request.getContextPath() %>/KinmuManageServlet" method="post" onsubmit="return confirmAction('この休憩記録を削除しますか？');" style="display: inline;">
                                                <input type="hidden" name="action" value="delete_break">
                                                <input type="hidden" name="targetDate" value="<%= targetDateStr %>">
                                                <input type="hidden" name="breakId" value="<%= breakItem.get("breakId") %>">
                                                <button type="submit" class="btn-danger" style="font-size: 9px; padding: 2px 4px;">削除</button>
                                            </form>
                                        </td>
                                    </tr>
                                <% } %>
                            <% } %>
                        </tbody>
                    </table>
                </div>
                <form action="<%= request.getContextPath() %>/KinmuManageServlet" method="post" onsubmit="return confirmAction('新しい休憩記録を追加しますか？');" style="margin-top: 10px;">
                    <input type="hidden" name="action" value="add_break">
                    <input type="hidden" name="targetDate" value="<%= targetDateStr %>">
                    <div style="display: flex; gap: 5px; align-items: center;">
                        <input type="time" name="newBreakStartTime" required style="width: 80px; font-size: 10px;">
                        <span style="font-size: 10px;">〜</span>
                        <input type="time" name="newBreakEndTime" required style="width: 80px; font-size: 10px;">
                        <button type="submit" class="btn-success" style="font-size: 10px; padding: 3px 6px;">追加</button>
                    </div>
                </form>
            </div>

            <%-- 工数割り当て（プロジェクト）管理エリア（右側） --%>
            <div class="scrollable-panel">
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
                                                 <input type="hidden" name="sourcePage" value="kinmu_manage">
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
                    <input type="hidden" name="sourcePage" value="kinmu_manage">
                    <input type="hidden" name="targetDate" value="<%= targetDateStr %>">
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
       <!--  </div> -->

        <div style="text-align: center; margin-top: 30px;">
            <a href="<%= backUrl %>" class="back-link">メニューへ戻る</a>
        </div>
        </div>
    </div>
    <input type="hidden" id="debugWorkHours" name="debugWorkHours" value="">
    <script>
    const workTimeInput = document.querySelector('input[name="newWorkHours"]');
    const debugInput = document.getElementById('debugWorkHours');
    workTimeInput.addEventListener('change', () => {
        debugInput.value = workTimeInput.value;
        console.log("workTime:", workTimeInput.value);
    });
</script>
    <script>
    flatpickr("#calendar", {
        locale: "ja",
        dateFormat: "Y年m月d日（D）",
        defaultDate: "<%= formattedTargetDate %>",
        allowInput: false,
        onChange: function(selectedDates, dateStr, instance) {
            const ymd = selectedDates[0].toISOString().slice(0, 10);
            window.location.href = "<%= request.getContextPath() %>/KinmuManageServlet?targetDate=" + ymd;
        }
    });
</script>
    
</body>
</html>
