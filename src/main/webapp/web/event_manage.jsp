<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="kintai.UserBean" %>
<%@ page import="kintai.CalendarEventBean" %>
<%@ page import="kintai.EventRepeatRuleBean" %> <%-- 新規追加: EventRepeatRuleBeanをインポート --%>
<%@ page import="java.time.LocalDate" %>
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
    List<CalendarEventBean> eventList = (List<CalendarEventBean>) request.getAttribute("eventList");
    List<EventRepeatRuleBean> ruleList = (List<EventRepeatRuleBean>) request.getAttribute("ruleList"); // 新規追加: ルールリストを取得
    String message = (String) request.getAttribute("message");
    Boolean success = (Boolean) request.getAttribute("success");

    // nullチェックと初期化
    if (eventList == null) eventList = new java.util.ArrayList<>();
    if (ruleList == null) ruleList = new java.util.ArrayList<>(); // 新規追加: nullチェック

    // 繰り返しルールをRuleIdで検索しやすいようにMapに変換 (JSP内での検索効率化のため)
    Map<Integer, EventRepeatRuleBean> rulesByRuleId = new HashMap<>();
    for (EventRepeatRuleBean rule : ruleList) {
        rulesByRuleId.put(rule.getRuleId(), rule);
    }

    // メニューへ戻るリンクのURLを権限に応じて設定
    String backUrl = (userRoleId == 1) ? request.getContextPath() + "/AdminMenuServlet" : request.getContextPath() + "/web/menu.jsp";
%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>イベント管理</title>
    <style>
        body {
            font-family: 'メイリオ', sans-serif;
            background-color: #f0f0f0;
            margin: 0;
            padding: 20px;
        }
        
        .container {
            max-width: 1000px; /* 幅を少し広めに調整 */
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
            margin: -20px -20px 20px -20px; /* 親コンテナのパディングを相殺 */
            border-top-left-radius: 8px;
            border-top-right-radius: 8px;
        }
        .user-info {
            display: flex;
            flex-direction: column;
            line-height: 1.5;
            text-align: left;
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

        /* 新規追加フォーム */
        .add-form {
            background-color: #f8f9fa;
            padding: 20px;
            margin-bottom: 30px;
            border-radius: 4px;
            border: 1px solid #dee2e6;
        }
        .add-form h2 {
            margin-top: 0;
            color: #495057;
        }
        .form-group {
            margin-bottom: 15px;
            display: flex;
            align-items: center;
            flex-wrap: wrap; /* 小画面で折り返す */
        }
        .form-group label {
            display: inline-block;
            width: 120px;
            font-weight: bold;
            flex-shrink: 0; /* ラベルが縮まないように */
        }
        .form-group input[type="date"],
        .form-group input[type="text"],
        .form-group select {
            width: 200px;
            padding: 5px;
            border: 1px solid #ced4da;
            border-radius: 4px;
            flex-grow: 1; /* 残りのスペースを占める */
        }
        .form-group .radio-group {
            display: flex;
            align-items: center;
            flex-grow: 1;
        }
        .form-group .radio-group input[type="radio"] {
            width: auto;
            margin-left: 0;
            margin-right: 5px;
        }
        .form-group .radio-group label {
            width: auto;
            margin-right: 15px;
            font-weight: normal; /* ラジオボタンラベルは太字にしない */
        }
        .form-group .checkbox-group { /* 繰り返し曜日のチェックボックス用 */
            display: flex;
            flex-grow: 1;
            flex-wrap: wrap;
            gap: 10px;
            padding-left: 120px; /* ラベルの幅分インデント */
            margin-top: -15px; /* 上のform-groupとの間隔調整 */
            margin-bottom: 15px;
        }
        .form-group .checkbox-group label {
            width: auto;
            margin-right: 5px;
            font-weight: normal;
        }
        .form-group .checkbox-group input[type="checkbox"] {
            width: auto;
            margin-right: 2px;
        }


        /* ボタンのスタイル */
        .btn {
            padding: 6px 12px;
            border: none;
            border-radius: 4px;
            cursor: pointer;
            font-size: 14px;
            margin-right: 5px;
        }
        .btn-primary { background-color: #007bff; color: white; }
        .btn-primary:hover { background-color: #0056b3; }
        .btn-success { background-color: #28a745; color: white; }
        .btn-success:hover { background-color: #218838; }
        .btn-danger { background-color: #dc3545; color: white; }
        .btn-danger:hover { background-color: #c82333; }
        .btn-secondary { background-color: #6c757d; color: white; }
        .btn-secondary:hover { background-color: #545b62; }

        /* テーブルのスタイル */
        .event-table {
            width: 100%;
            border-collapse: collapse;
            margin-top: 20px;
        }
        .event-table th, .event-table td {
            border: 1px solid #dee2e6;
            padding: 10px;
            text-align: left;
            vertical-align: middle;
        }
        .event-table th {
            background-color: #f8f9fa;
            font-weight: bold;
            color: #495057;
        }
        .event-table tr:nth-child(even) {
            background-color: #f8f9fa;
        }
        .event-table tr:hover {
            background-color: #e9ecef;
        }
        /* 編集フォームのスタイル */
        .edit-form {
            display: none; /* 初期は非表示 */
            margin: 0;
            display: flex; /* 行内編集時にフレックスボックスレイアウトを使用 */
            flex-wrap: wrap; /* ラベルと入力フィールドの折り返し */
            align-items: center;
            gap: 5px;
        }
        .edit-form .edit-label { /* 編集フォーム内のラベル */
            width: auto;
            font-weight: bold;
            flex-shrink: 0;
            margin-right: 5px;
        }
        .edit-form input[type="date"],
        .edit-form input[type="text"],
        .edit-form select {
            padding: 3px;
            border: 1px solid #ced4da;
            border-radius: 4px;
            width: auto; /* 自動調整 */
            flex-grow: 1; /* 残りのスペースを占める */
        }
        .edit-form .edit-radio-group {
            display: flex;
            align-items: center;
            flex-grow: 1;
        }
        .edit-form .edit-radio-group input[type="radio"] {
             width: auto;
             margin-left: 0;
             margin-right: 5px;
        }
        .edit-form .edit-radio-group label {
            width: auto;
            margin-right: 15px;
            font-weight: normal;
        }
        .edit-form .edit-checkbox-group {
            display: flex;
            flex-wrap: wrap;
            gap: 10px;
            flex-grow: 1;
            padding-left: 120px; /* ラベルの幅分インデント */
            margin-top: -15px;
        }
        .edit-form .edit-checkbox-group label {
            width: auto;
            margin-right: 5px;
            font-weight: normal;
        }
        .edit-form .edit-checkbox-group input[type="checkbox"] {
            width: auto;
            margin-right: 2px;
        }
        .action-cell {
            white-space: nowrap; /* ボタンが改行されないようにする */
        }
        /* 戻るボタンのスタイル */
        .back-link {
            display: inline-block;
            margin-top: 20px;
            padding: 8px 16px;
            background-color: #6c757d;
            color: white;
            text-decoration: none;
            border-radius: 4px;
        }
        .back-link:hover {
            background-color: #545b62;
        }
    </style>
    <script>
        // DOMContentLoadedイベントで初期化処理を行う
        document.addEventListener('DOMContentLoaded', (event) => {
            // 新規追加フォームの繰り返しタイプ選択時の処理を設定
            document.getElementById('newRepeatType').addEventListener('change', function() {
                toggleRepeatFields('new', this.value);
            });
            // 初期表示時の繰り返しフィールドの表示/非表示を調整
            toggleRepeatFields('new', document.getElementById('newRepeatType').value);

            // 既存イベントの編集フォームごとにイベントリスナーを設定
            document.querySelectorAll('.edit-form').forEach(form => {
                const eventDateId = form.id.replace('edit-', '');
                const repeatTypeSelect = form.querySelector('[name="repeatType"]');
                if (repeatTypeSelect) {
                    repeatTypeSelect.addEventListener('change', function() {
                        toggleRepeatFields(eventDateId, this.value);
                    });
                    // 編集フォームの初期表示時にも調整
                    toggleRepeatFields(eventDateId, repeatTypeSelect.value);
                }
            });

            // input[type="date"] がフォーカスされたとき、またはクリックされたときにピッカーを表示
            const dateInputs = document.querySelectorAll('input[type="date"]');
            dateInputs.forEach(input => {
                input.addEventListener('focus', () => {
                    if (input.showPicker) {
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

        // 繰り返し設定フィールドの表示/非表示を切り替える関数
        function toggleRepeatFields(prefix, repeatType) {
            const isWeeklyOrMonthlyWeekday = (repeatType === 'WEEKLY' || repeatType === 'MONTHLY_WEEKDAY');

            // 繰り返し間隔
            document.getElementById(prefix + 'RepeatIntervalGroup').style.display = (repeatType !== 'NONE' ? 'flex' : 'none');
            // 繰り返し曜日
            document.getElementById(prefix + 'RepeatDaysOfWeekGroup').style.display = (isWeeklyOrMonthlyWeekday ? 'flex' : 'none');
            // 繰り返し終了日
            document.getElementById(prefix + 'RepeatEndDateGroup').style.display = (repeatType !== 'NONE' ? 'flex' : 'none');

            // WEEKLY_DAYでは繰り返し曜日必須
            document.querySelectorAll('#' + prefix + 'RepeatDaysOfWeekGroup input[type="checkbox"]').forEach(checkbox => {
                checkbox.required = isWeeklyOrMonthlyWeekday;
            });
            // 繰り返し間隔のrequiredはrepeatTypeがNONEでなければtrue
            document.getElementById(prefix + 'RepeatInterval').required = (repeatType !== 'NONE');
        }

        // 編集モードの切り替え
        function toggleEdit(eventDate) {
            // 日付をYYYY-MM-DD形式の文字列に変換し、HTML IDの一部として使用
            const formattedDate = eventDate.replace(/-/g, '_'); 
            var displaySpan = document.getElementById('display-' + formattedDate);
            var editForm = document.getElementById('edit-' + formattedDate);
            
            if (editForm.style.display === 'flex') { 
                displaySpan.style.display = 'inline-block';
                editForm.style.display = 'none';
            } else {
                displaySpan.style.display = 'none';
                editForm.style.display = 'flex';
                // 編集フォームが表示されたら、そのフォームの繰り返しフィールドの初期状態を調整
                const repeatTypeSelect = editForm.querySelector('[name="repeatType"]');
                if (repeatTypeSelect) {
                    toggleRepeatFields(formattedDate + '_', repeatTypeSelect.value); // プレフィックスを調整
                }
            }
        }
        
        // 削除確認
        function confirmDelete(eventDate, eventName) {
            if (confirm('イベント「' + eventName + '」（日付：' + eventDate + '）を削除してもよろしいですか？')) {
                // 日付をYYYY-MM-DD形式の文字列に変換し、HTML IDの一部として使用
                const formattedDate = eventDate.replace(/-/g, '_');
                document.getElementById('deleteForm-' + formattedDate).submit();
            }
        }
        
        // 追加確認（入力チェックも強化）
        function confirmAdd(form) {
            var eventDate = form.eventDate.value;
            var eventName = form.eventName.value;
            var isWork = form.isWork.value;
            var repeatType = form.repeatType.value;
            var repeatInterval = form.repeatInterval.value;
            var repeatDaysOfWeek = form.querySelectorAll('input[name="repeatDaysOfWeek"]:checked');
            var repeatEndDate = form.repeatEndDate.value;
            var errorMessageDiv = document.getElementById("errorMessage");

            errorMessageDiv.innerHTML = ""; // エラーメッセージをクリア

            if (eventDate.trim() === '' || eventName.trim() === '' || isWork.trim() === '') {
                errorMessageDiv.innerHTML = "日付、イベント名、種別の指定は必須です。";
                return false;
            }

            if (repeatType !== 'NONE') {
                if (repeatInterval.trim() === '' || isNaN(repeatInterval) || parseInt(repeatInterval) <= 0) {
                    errorMessageDiv.innerHTML = "繰り返し間隔は1以上の半角数字で入力してください。";
                    return false;
                }
                if (repeatType === 'WEEKLY') {
                    if (repeatDaysOfWeek.length === 0) {
                        errorMessageDiv.innerHTML = "毎週の場合、繰り返し曜日を1つ以上選択してください。";
                        return false;
                    }
                }
                // 終了日は必須ではないが、もし入力されていれば形式をチェック
                // DatePickerを使うので形式チェックは不要な場合が多い
            }
            
            return confirm('イベント（日付：「' + eventDate + '」、イベント名：「' + eventName + '」）を追加してもよろしいですか？');
        }
        
        // 更新確認（入力チェックも強化）
        function confirmUpdate(form, eventDate) {
            var eventName = form.eventName.value;
            var isWork = form.isWork.value;
            var repeatType = form.repeatType.value;
            var repeatInterval = form.repeatInterval.value;
            var repeatDaysOfWeek = form.querySelectorAll('input[name="repeatDaysOfWeek"]:checked');
            var repeatEndDate = form.repeatEndDate.value;
            var errorMessageDiv = document.getElementById("errorMessage");

            errorMessageDiv.innerHTML = ""; // エラーメッセージをクリア

            if (eventName.trim() === '' || isWork.trim() === '') {
                errorMessageDiv.innerHTML = "イベント名、種別の指定は必須です。";
                return false;
            }

            if (repeatType !== 'NONE') {
                if (repeatInterval.trim() === '' || isNaN(repeatInterval) || parseInt(repeatInterval) <= 0) {
                    errorMessageDiv.innerHTML = "繰り返し間隔は1以上の半角数字で入力してください。";
                    return false;
                }
                if (repeatType === 'WEEKLY') {
                    if (repeatDaysOfWeek.length === 0) {
                        errorMessageDiv.innerHTML = "毎週の場合、繰り返し曜日を1つ以上選択してください。";
                        return false;
                    }
                }
            }
            
            return confirm('イベント（日付：「' + eventDate + '」）の情報を更新してもよろしいですか？');
        }
    </script>
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

        <h1>イベント管理</h1>
        
        <%-- メッセージ表示 --%>
        <% if (message != null && !message.isEmpty()) { %>
            <div class="message <%= (success != null && success) ? "success-message" : "error-message" %>">
                <%= message %>
            </div>
        <% } %>
        
        <%-- 新規イベント追加フォーム --%>
        <div class="add-form">
            <h2>新規イベント追加</h2>
            <form method="post" action="<%= request.getContextPath() %>/EventManageServlet" onsubmit="return confirmAdd(this)">
                <input type="hidden" name="action" value="add">
                <div class="form-group">
                    <label for="newEventDate">日付：</label>
                    <input type="date" id="newEventDate" name="eventDate" required>
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
                <h3 style="margin-top: 20px; margin-bottom: 15px; border-bottom: 1px dashed #eee; padding-bottom: 5px;">繰り返し設定</h3>
                <div class="form-group">
                    <label for="newRepeatType">繰り返しタイプ：</label>
                    <select id="newRepeatType" name="repeatType" required>
                        <option value="NONE">単発</option>
                        <option value="DAILY">毎日</option>
                        <option value="WEEKLY">毎週</option>
                        <option value="MONTHLY_DAY">毎月（日付指定）</option>
                        <%-- <option value="MONTHLY_WEEKDAY">毎月（第N週目・曜日指定）</option> --%> <%-- 今は実装しない --%>
                        <option value="YEARLY">毎年</option>
                    </select>
                </div>
                
                <div class="form-group" id="newRepeatIntervalGroup">
                    <label for="newRepeatInterval">繰り返し間隔（週/月/年ごと）：</label>
                    <input type="number" id="newRepeatInterval" name="repeatInterval" min="1" value="1">
                </div>

                <div class="form-group checkbox-group" id="newRepeatDaysOfWeekGroup">
                    <label>繰り返し曜日：</label>
                    <input type="checkbox" id="newDayMon" name="repeatDaysOfWeek" value="1"><label for="newDayMon">月</label>
                    <input type="checkbox" id="newDayTue" name="repeatDaysOfWeek" value="2"><label for="newDayTue">火</label>
                    <input type="checkbox" id="newDayWed" name="repeatDaysOfWeek" value="3"><label for="newDayWed">水</label>
                    <input type="checkbox" id="newDayThu" name="repeatDaysOfWeek" value="4"><label for="newDayThu">木</label>
                    <input type="checkbox" id="newDayFri" name="repeatDaysOfWeek" value="5"><label for="newDayFri">金</label>
                    <input type="checkbox" id="newDaySat" name="repeatDaysOfWeek" value="6"><label for="newDaySat">土</label>
                    <input type="checkbox" id="newDaySun" name="repeatDaysOfWeek" value="7"><label for="newDaySun">日</label>
                </div>

                <div class="form-group" id="newRepeatEndDateGroup">
                    <label for="newRepeatEndDate">繰り返し終了日：</label>
                    <input type="date" id="newRepeatEndDate" name="repeatEndDate">
                </div>

                <div style="text-align: right; margin-top: 20px;">
                    <button type="submit" class="btn btn-primary">追加</button>
                </div>
            </form>
        </div>
        
        <%-- イベント一覧テーブル --%>
        <h2>イベント一覧</h2>
        <table class="event-table">
            <thead>
                <tr>
                    <th>日付</th>
                    <th>イベント名</th>
                    <th>種別</th>
                    <th>繰り返し</th> <%-- 新規追加 --%>
                    <th>操作</th>
                </tr>
            </thead>
            <tbody>
                <% if (eventList != null && !eventList.isEmpty()) { %>
                    <% for (CalendarEventBean event : eventList) { 
                        // 日付をYYYY-MM-DD形式の文字列に変換し、HTML IDの一部として使用
                        String formattedEventDateId = event.getEventDate().toString().replace("-", "_");
                        // このイベントに紐づく繰り返しルールを取得
                        EventRepeatRuleBean eventRule = (event.getRepeatRuleId() != null) ? rulesByRuleId.get(event.getRepeatRuleId()) : null;
                    %>
                        <tr>
                            <td>
                                <%-- 表示用 --%>
                                <span id="display-<%= formattedEventDateId %>">
                                    <%= event.getEventDate() %>
                                </span>
                                
                                <%-- 編集フォーム --%>
                                <form id="edit-<%= formattedEventDateId %>" method="post" 
                                      action="<%= request.getContextPath() %>/EventManageServlet" 
                                      class="edit-form" style="display: none;"
                                      onsubmit="return confirmUpdate(this, '<%= event.getEventDate() %>')">
                                    <input type="hidden" name="action" value="update">
                                    <input type="hidden" name="eventDate" value="<%= event.getEventDate() %>">
                                    
                                    <label class="edit-label">イベント名：</label>
                                    <input type="text" name="eventName" value="<%= event.getEventName() %>" 
                                           maxlength="100" required>
                                    
                                    <label class="edit-label">種別：</label>
                                    <select name="isWork" required>
                                        <option value="true" <%= event.isWork() ? "selected" : "" %>>出勤日</option>
                                        <option value="false" <%= !event.isWork() ? "selected" : "" %>>休日</option>
                                    </select>

                                    <%-- 繰り返しイベント設定 (編集用) --%>
                                    <h4 style="width: 100%; margin-top: 10px; margin-bottom: 10px; border-bottom: 1px dotted #eee; padding-bottom: 5px;">繰り返し設定</h4>
                                    <label class="edit-label">繰り返しタイプ：</label>
                                    <select id="<%= formattedEventDateId %>_RepeatType" name="repeatType" required onchange="toggleRepeatFields('<%= formattedEventDateId %>_', this.value)">
                                        <option value="NONE" <%= (eventRule == null || "NONE".equals(eventRule.getRepeatType())) ? "selected" : "" %>>単発</option>
                                        <option value="DAILY" <%= (eventRule != null && "DAILY".equals(eventRule.getRepeatType())) ? "selected" : "" %>>毎日</option>
                                        <option value="WEEKLY" <%= (eventRule != null && "WEEKLY".equals(eventRule.getRepeatType())) ? "selected" : "" %>>毎週</option>
                                        <option value="MONTHLY_DAY" <%= (eventRule != null && "MONTHLY_DAY".equals(eventRule.getRepeatType())) ? "selected" : "" %>>毎月（日付指定）</option>
                                        <%-- <option value="MONTHLY_WEEKDAY" <%= (eventRule != null && "MONTHLY_WEEKDAY".equals(eventRule.getRepeatType())) ? "selected" : "" %>>毎月（第N週目・曜日指定）</option> --%>
                                        <option value="YEARLY" <%= (eventRule != null && "YEARLY".equals(eventRule.getRepeatType())) ? "selected" : "" %>>毎年</option>
                                    </select>
                                    
                                    <label class="edit-label" id="<%= formattedEventDateId %>_RepeatIntervalLabel">繰り返し間隔：</label>
                                    <input type="number" id="<%= formattedEventDateId %>_RepeatInterval" name="repeatInterval" min="1" value="<%= (eventRule != null) ? eventRule.getRepeatInterval() : "1" %>">

                                    <div class="edit-checkbox-group" id="<%= formattedEventDateId %>_RepeatDaysOfWeekGroup">
                                        <label>繰り返し曜日：</label>
                                        <% String[] selectedDays = (eventRule != null && eventRule.getRepeatDaysOfWeek() != null) ? eventRule.getRepeatDaysOfWeek().split(",") : new String[]{}; %>
                                        <input type="checkbox" id="<%= formattedEventDateId %>_DayMon" name="repeatDaysOfWeek" value="1" <%= (java.util.Arrays.asList(selectedDays).contains("1")) ? "checked" : "" %>><label for="<%= formattedEventDateId %>_DayMon">月</label>
                                        <input type="checkbox" id="<%= formattedEventDateId %>_DayTue" name="repeatDaysOfWeek" value="2" <%= (java.util.Arrays.asList(selectedDays).contains("2")) ? "checked" : "" %>><label for="<%= formattedEventDateId %>_DayTue">火</label>
                                        <input type="checkbox" id="<%= formattedEventDateId %>_DayWed" name="repeatDaysOfWeek" value="3" <%= (java.util.Arrays.asList(selectedDays).contains("3")) ? "checked" : "" %>><label for="<%= formattedEventDateId %>_DayWed">水</label>
                                        <input type="checkbox" id="<%= formattedEventDateId %>_DayThu" name="repeatDaysOfWeek" value="4" <%= (java.util.Arrays.asList(selectedDays).contains("4")) ? "checked" : "" %>><label for="<%= formattedEventDateId %>_DayThu">木</label>
                                        <input type="checkbox" id="<%= formattedEventDateId %>_DayFri" name="repeatDaysOfWeek" value="5" <%= (java.util.Arrays.asList(selectedDays).contains("5")) ? "checked" : "" %>><label for="<%= formattedEventDateId %>_DayFri">金</label>
                                        <input type="checkbox" id="<%= formattedEventDateId %>_DaySat" name="repeatDaysOfWeek" value="6" <%= (java.util.Arrays.asList(selectedDays).contains("6")) ? "checked" : "" %>><label for="<%= formattedEventDateId %>_DaySat">土</label>
                                        <input type="checkbox" id="<%= formattedEventDateId %>_DaySun" name="repeatDaysOfWeek" value="7" <%= (java.util.Arrays.asList(selectedDays).contains("7")) ? "checked" : "" %>><label for="<%= formattedEventDateId %>_DaySun">日</label>
                                    </div>

                                    <label class="edit-label" id="<%= formattedEventDateId %>_RepeatEndDateLabel">繰り返し終了日：</label>
                                    <input type="date" id="<%= formattedEventDateId %>_RepeatEndDate" name="repeatEndDate" value="<%= (eventRule != null && eventRule.getRepeatEndDate() != null) ? eventRule.getRepeatEndDate().toString() : "" %>">

                                    <div style="width: 100%; text-align: right; margin-top: 10px;">
                                        <button type="submit" class="btn btn-primary">保存</button>
                                        <button type="button" class="btn btn-secondary" 
                                                onclick="toggleEdit('<%= event.getEventDate() %>')">キャンセル</button>
                                    </div>
                                </form>
                            </td>
                            <td><%= event.getEventName() %></td>
                            <td><%= event.getWorkStatusName() %></td>
                            <td>
                                <%-- 繰り返し表示 --%>
                                <% if (eventRule != null) { %>
                                    <%= eventRule.getRepeatTypeJapanese() %>
                                    <% if (!"NONE".equals(eventRule.getRepeatType())) { %>
                                        <% if (eventRule.getRepeatInterval() > 1) { %>
                                            （<%= eventRule.getRepeatInterval() %>ごと）
                                        <% } %>
                                        <% if (eventRule.getRepeatDaysOfWeek() != null && !eventRule.getRepeatDaysOfWeek().isEmpty()) { %>
                                            （<%= String.join("・", eventRule.getSelectedDaysOfWeekNames()) %>）
                                        <% } %>
                                        <% if (eventRule.getRepeatEndDate() != null) { %>
                                            ～<%= eventRule.getRepeatEndDate() %>
                                        <% } else { %>
                                            （無期限）
                                        <% } %>
                                    <% } %>
                                <% } else { %>
                                    単発
                                <% } %>
                            </td>
                            <td class="action-cell">
                                <button class="btn btn-success" onclick="toggleEdit('<%= event.getEventDate() %>')">編集</button>
                                <button class="btn btn-danger" onclick="confirmDelete('<%= event.getEventDate() %>', '<%= event.getEventName() %>')">削除</button>
                                
                                <%-- 削除用表单（隐藏） --%>
                                <form id="deleteForm-<%= formattedEventDateId %>" method="post" 
                                      action="<%= request.getContextPath() %>/EventManageServlet" style="display: none;">
                                    <input type="hidden" name="action" value="delete">
                                    <input type="hidden" name="eventDate" value="<%= event.getEventDate() %>">
                                </form>
                            </td>
                        </tr>
                    <% } %>
                <% } else { %>
                    <tr>
                        <td colspan="5" style="text-align: center;">イベントデータがありません</td> <%-- 列数を調整 --%>
                    </tr>
                <% } %>
            </tbody>
        </table>
        
        <a href="<%= backUrl %>" class="back-link">メニューへ戻る</a>
    </div>
</body>
</html>
