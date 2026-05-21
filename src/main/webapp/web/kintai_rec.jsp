<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="kintai.UserBean" %>
<%@ page import="kintai.KintaiRecBean" %>
<%@ page import="kintai.DeptBean" %>
<%@ page import="kintai.PostBean" %>
<%@ page import="kintai.EmpBean" %>
<%@ page import="kintai.MonthlySummaryBean" %>
<%@ page import="kintai.KinmuManageBean" %>
<%@ page import="kintai.ComplianceCheckResult" %>
<%@ page import="kintai.ComplianceViolation" %>
<%@ page import="java.util.List" %>
<%@ page import="java.time.LocalDate" %>
<%@ page import="java.time.DayOfWeek, java.time.format.TextStyle, java.util.Locale" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.HashMap" %>
<%@ page import="kintai.CalendarEventBean" %>
<%@ page import="java.time.*, java.util.*, kintai.LeaveRecBean, kintai.LeaveRecDao, kintai.UserBean, kintai.CalendarEventBean, kintai.KintaiRecBean, kintai.KinmuManageBean" %>
<%@ page import="java.time.YearMonth" %>
<%@ page import="kintai.ProjectBean" %>
<%@ page import="kintai.ProjectDao" %>

<%
    // ログインチェック
    UserBean user = (UserBean) session.getAttribute("user");
    if (user == null) {
        response.sendRedirect(request.getContextPath() + "/web/login.jsp");
        return;
    }

    String empno = user.getEmpno();
    

    // セッションからユーザー情報を取得
    String loggedInUserName = user.getName();
    String loggedInDeptName = (String) session.getAttribute("deptName");
    // UserBeanのロールIDを取得
    int userRoleId = user.getRoleId(); 

    // サーブレットから渡されたデータを取得
    List<KintaiRecBean> kintaiRecords = (List<KintaiRecBean>) request.getAttribute("kintaiRecords");
    String empNoFilter = (String) request.getAttribute("empNoFilter");
    String deptNoFilter = (String) request.getAttribute("deptNoFilter");
    String postNoFilter = (String) request.getAttribute("postNoFilter");
    String startDate = (String) request.getAttribute("startDate");
    String endDate = (String) request.getAttribute("endDate");
    Integer retrievedUserRoleId = (Integer) request.getAttribute("userRoleId"); 
    Boolean isSelfMode = (Boolean) request.getAttribute("isSelfMode"); 
    if (isSelfMode == null) isSelfMode = false;
    
    MonthlySummaryBean monthlySummary = (MonthlySummaryBean) request.getAttribute("monthlySummary"); // 月度統計データ
    ComplianceCheckResult complianceResult = (ComplianceCheckResult) request.getAttribute("complianceResult"); // 法令遵守チェック結果
    List<String> violationEmployees = (List<String>) request.getAttribute("violationEmployees"); // 法令遵守違反者リスト
    java.util.Map<String, ComplianceCheckResult> violationDetails = (java.util.Map<String, ComplianceCheckResult>) request.getAttribute("violationDetails"); // 法令遵守違反詳細情報

    // nullチェックと初期化
    if (kintaiRecords == null) kintaiRecords = new java.util.ArrayList<>();
    if (violationEmployees == null) violationEmployees = new java.util.ArrayList<>();
    if (violationDetails == null) violationDetails = new java.util.HashMap<>();
    List<DeptBean> deptList = (List<DeptBean>) request.getAttribute("deptList");
    List<PostBean> postList = (List<PostBean>) request.getAttribute("postList");
    List<EmpBean> empList = (List<EmpBean>) request.getAttribute("empList");
    
    String selectedDept = (String) request.getAttribute("selectedDept");
    String selectedPost = (String) request.getAttribute("selectedPost");
    String selectedEmpId = (String) request.getAttribute("selectedEmpId");

    String successMessage = (String) request.getAttribute("successMessage");
    String errorMessage = (String) request.getAttribute("errorMessage");

    if (deptList == null) deptList = new java.util.ArrayList<>();
    if (postList == null) postList = new java.util.ArrayList<>();
    if (empList == null) empList = new java.util.ArrayList<>();

    // 曜日の日本語表示用マップ
    Map<DayOfWeek, String> dayOfWeekMap = new HashMap<>();
    dayOfWeekMap.put(DayOfWeek.MONDAY, "月");
    dayOfWeekMap.put(DayOfWeek.TUESDAY, "火");
    dayOfWeekMap.put(DayOfWeek.WEDNESDAY, "水");
    dayOfWeekMap.put(DayOfWeek.THURSDAY, "木");
    dayOfWeekMap.put(DayOfWeek.FRIDAY, "金");
    dayOfWeekMap.put(DayOfWeek.SATURDAY, "土");
    dayOfWeekMap.put(DayOfWeek.SUNDAY, "日");

    // メニューへ戻るリンクのURLを権限に応じて設定
    String backUrl;
    if (userRoleId == 1) {
        backUrl = request.getContextPath() + "/AdminMenuServlet";
    } else {
        backUrl = request.getContextPath() + "/web/menu.jsp";
    }
    
    
    //リストを作るため追加
    int year = request.getParameter("year") != null ? Integer.parseInt(request.getParameter("year")) : LocalDate.now().getYear();
    int month = request.getParameter("month") != null ? Integer.parseInt(request.getParameter("month")) : LocalDate.now().getMonthValue();

    LocalDate firstDay = LocalDate.of(year, month, 1);
    LocalDate lastDay = firstDay.withDayOfMonth(firstDay.lengthOfMonth());

    LocalDate prevMonth = firstDay.minusMonths(1);
    LocalDate nextMonth = firstDay.plusMonths(1);
    
    // 有効なプロジェクトリストを取得
    ProjectDao projectDao = new ProjectDao();
    List<ProjectBean> projects = projectDao.findAll(); // ここでIS_DELETED=falseのみ取得される
    request.setAttribute("projectList", projects);
    

    List<ProjectBean> projectList = (List<ProjectBean>) request.getAttribute("projectList");


    
%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title><% if (userRoleId == 1) { %>従業員別勤怠記録表示<% } else if (userRoleId == 2) { %>勤怠記録表示（自分/部下）<% } else { %>勤怠記録表示<% } %></title>
    <style>
        body {
            font-family: 'メイリオ', sans-serif;
            background-color: #f0f0f0;
            margin: 0;
            padding: 20px;
        }
        .container {
            max-width: 1200px;
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
            font-size: 13px;
        }
        .logout-button {
            background-color: #dc3545;
            color: white;
            border: 1px solid #dc3545;
            border-radius: 5px;
            padding: 8px 16px;
            cursor: pointer;
            font-size: 13px;
            text-decoration: none;
            align-self: center; /* 垂直方向中央寄せ */
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
            font-size: 13px;
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

        /* フィルターフォーム */
        .filter-form {
            background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%);
            padding: 15px;
            margin-bottom: 20px;
            border-radius: 8px;
            border: 1px solid #dee2e6;
            box-shadow: 0 2px 4px rgba(0,0,0,0.05);
            display: flex;
            flex-wrap: wrap;
            gap: 12px;
            align-items: flex-end;
        }
        .filter-group {
            display: flex;
            flex-direction: column;
            min-width: 160px;
        }
        .filter-group label {
            font-weight: 600;
            margin-bottom: 4px;
            color: #495057;
            font-size: 12px;
        }
        .filter-form input[type="text"],
        .filter-form input[type="date"],
        .filter-form select {
            padding: 8px 10px;
            border: 1px solid #ced4da;
            border-radius: 6px;
            font-size: 13px;
            transition: border-color 0.2s, box-shadow 0.2s;
            background-color: white;
        }
        .filter-form input[type="text"]:focus,
        .filter-form input[type="date"]:focus,
        .filter-form select:focus {
            outline: none;
            border-color: #007bff;
            box-shadow: 0 0 0 2px rgba(0,123,255,0.1);
        }
        .filter-form button {
            padding: 8px 16px;
            background: linear-gradient(135deg, #007bff 0%, #0056b3 100%);
            color: white;
            border: none;
            border-radius: 6px;
            cursor: pointer;
            font-size: 13px;
            font-weight: 600;
            align-self: flex-end;
            transition: all 0.2s;
            box-shadow: 0 2px 4px rgba(0,123,255,0.2);
        }
        .filter-form button:hover {
            background: linear-gradient(135deg, #0056b3 0%, #004085 100%);
            transform: translateY(-1px);
            box-shadow: 0 3px 6px rgba(0,123,255,0.3);
        }
        
        /* 日付入力グループ */
        .date-group {
            display: flex;
            gap: 12px;
            flex-basis: 100%;
            justify-content: flex-start;
        }

        /* テーブルのスタイル */
        .kintai-table {
            width: 100%;
            border-collapse: collapse;
            margin-top: 10px;
            border-radius: 8px;
            overflow: hidden;
            box-shadow: 0 2px 8px rgba(0,0,0,0.08);
            background-color: white;
        }
        .kintai-table th, .kintai-table td {
            border: 1px solid #dee2e6;
            padding: 10px 8px;
            text-align: center;
            vertical-align: middle;
            font-size: 12px;
        }
        .kintai-table th {
            background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%);
            font-weight: 600;
            color: #495057;
            border-bottom: 2px solid #dee2e6;
            position: relative;
        }
        .kintai-table th::after {
            content: '';
            position: absolute;
            bottom: 0;
            left: 0;
            right: 0;
            height: 2px;
            background: linear-gradient(90deg, #007bff, #28a745, #fd7e14);
        }
        .kintai-table tr:nth-child(even) {
            background-color: #fbfcfd;
        }
        .kintai-table tr:hover {
            background-color: #f0f4ff;
            transform: scale(1.01);
            transition: all 0.2s;
        }
        .kintai-table td {
            transition: background-color 0.2s;
        }
        /* 曜日によって色を変える */
        .kintai-table .saturday {
            color: blue;
        }
        .kintai-table .sunday {
            color: red;
        }

        /* 戻るボタン */
        .back-link {
            display: inline-block;
            margin-top: 30px;
            padding: 10px 20px;
            background-color: #6c757d;
            color: white;
            text-decoration: none;
            border-radius: 4px;
            text-align: center;
            font-size: 13px;
        }
        .back-link:hover {
            background-color: #545b62;
        }

        /* メインコンテンツのレイアウト */
        .main-content {
        	height: auto;
            display: flex;
            flex-direction: column; /* 垂直方向に並べる */
            gap: 20px;
            overflow-x: auto; 		/*  ← 2025/8/8 米村 横スクロールを出来るように追加*/
            margin-top: 20px;
            padding: 20px;			/*  ← 2025/8/8 米村 下側に余白を追加*/
        }
        
        /* 検索フォームとステータス表示の行レイアウト */
        .search-status-row {
            display: flex;
            gap: 16px;
            align-items: flex-start;
            flex-wrap: wrap; /* レスポンシブ対応で必要に応じて折り返す */
        }
        .search-column {
            flex: 2; /* 検索フォームにより広いスペースを割り当てる */
            min-width: 450px; /* 最小幅を設定して狭くなりすぎないようにする */
        }
        .status-column {
            flex: 1; /* ステータスカードとレポートパネルに残りスペースを割り当てる */
            min-width: 300px;
            display: flex;
            flex-direction: column;
            gap: 16px;
        }
        
        /* 今日の勤怠状況のスタイル */
        .daily-status {
            background: linear-gradient(135deg, #e8f5e8 0%, #f0f8ff 100%);
            border: 1px solid #c3e6cb;
            border-radius: 6px;
            padding: 10px 12px;
            font-size: 11px;
            color: #155724;
            flex-shrink: 0;
        }
        .daily-status .status-title {
            font-weight: bold;
            margin-bottom: 6px;
            font-size: 11px;
        }
        .daily-status .status-items {
            display: flex;
            gap: 6px;
            flex-wrap: wrap;
            font-size: 10px;
        }
        
        /* 月度統計カード（個人モード）のスタイル */
        .monthly-summary {
            display: flex;
            justify-content: space-around; /* カードを均等に配置 */
            flex-wrap: wrap; /* レスポンシブ対応でカードが折り返すようにする */
            gap: 15px; /* カード間の間隔 */
            margin-bottom: 20px; /* 下のテーブルとの間隔 */
        }
        
        /* 管理者統計カード（管理者モード）のスタイル */
        .manager-summary {
            display: flex;
            flex-direction: column;
            gap: 12px;
            width: 220px;
            flex-shrink: 0;
        }
        
        /* 管理者レイアウト用のスタイル */
        .admin-top-layout {
            display: flex;
            gap: 20px;
            align-items: flex-start;
        }
        
        .middle-section-admin {
            flex: 1;
            display: flex;
            flex-direction: column;
            gap: 15px;
        }
        
        .right-section-admin {
            width: 250px;
            flex-shrink: 0;
            display: flex;
            flex-direction: column;
            gap: 12px;
        }
        
        .summary-card {
            background-color: #f8f9fa;
            border: 1px solid #dee2e6;
            border-radius: 8px;
            padding: 12px;
            text-align: center;
            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
            min-width: 160px; /* カードの最小幅 */
            flex: 1; /* 柔軟に伸縮 */
            height: 85px; /* 高さを固定 */
            display: flex;
            flex-direction: column;
            justify-content: space-between;
        }
        .summary-card h3 {
            margin: 0;
            font-size: 13px;
            color: #495057;
            border-bottom: none;
            flex-shrink: 0;
        }
        .summary-card .value {
            font-size: 20px; /* 値を大きく表示 */
            font-weight: bold;
            color: #212529;
            margin: 2px 0;
            flex-shrink: 0;
        }
        .summary-card .detail {
            font-size: 12px;
            color: #6c757d;
            margin: 0;
        }
        .summary-card.attendance {
            border-left: 4px solid #28a745;
        }
        .summary-card.overtime {
            border-left: 4px solid #fd7e14;
        }
        .summary-card.working {
            border-left: 4px solid #007bff;
        }
        .summary-card.break {
            border-left: 4px solid #6c757d;
        }
        
        /* 管理者統計カード専用スタイル */
        .summary-card.holiday-work {
            border-left: 4px solid #fd7e14;
        }
        .summary-card.absent {
            border-left: 4px solid #dc3545;
        }
        .summary-card.leave {
            border-left: 4px solid #007bff;
        }
        
        /* 従業員リストのスタイル */
        .employee-list {
            font-size: 12px;
            color: #495057;
            text-align: left;
            display: flex;
            flex-wrap: wrap;
            gap: 4px;
            justify-content: center;
            max-height: 38px;
            overflow: hidden;
            line-height: 18px;
        }
        .employee-list .employee-name {
            background-color: #e9ecef;
            padding: 2px 5px;
            border-radius: 3px;
            cursor: pointer;
            transition: background-color 0.2s;
            white-space: nowrap;
        }
        .employee-list .employee-name:hover {
            background-color: #dee2e6;
        }
        .employee-list .more-indicator {
            background-color: #007bff;
            color: white;
            padding: 1px 4px;
            border-radius: 3px;
            cursor: pointer;
            font-weight: bold;
        }
        .employee-list .more-indicator:hover {
            background-color: #0056b3;
        }
        
        /* ページタイトルのスタイル */
        .page-title {
            text-align: center;
            margin: 10px 0 5px 0;
            padding: 0;
            border-bottom: 2px solid #007bff;
            padding-bottom: 5px;
            font-size: 18px;
        }
        
        /* モーダルダイアログのスタイル */
        .modal {
            display: none;
            position: fixed;
            z-index: 1000;
            left: 0; top: 0;
            width: 100%; height: 100%;
            background-color: rgba(0,0,0,0.5);
        }
        .modal-content {
            background-color: #fff;
            margin: 10% auto;
            padding: 20px;
            border-radius: 8px;
            width: 50%;
            position: relative;
        }
        
        /* 勤務時間一覧モーダル専用スタイル */
        .kintai-modal {
            display: none;
            position: fixed;
            z-index: 1001;
            left: 0;
            top: 0;
            width: 100%;
            height: 100%;
            background-color: rgba(0,0,0,0.6);
        }
        .kintai-modal-content {
            background-color: white;
            margin: 3% auto;
            padding: 15px;
            border-radius: 8px;
            width: 85%;
            max-width: 900px;
            max-height: 85%;
            overflow-y: auto;
            box-shadow: 0 4px 20px rgba(0,0,0,0.3);
            font-size: 12px;
        }
        .kintai-modal-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 20px;
            padding-bottom: 10px;
            border-bottom: 2px solid #007bff;
        }
        .kintai-modal-close {
            color: #aaa;
            font-size: 24px;
            font-weight: bold;
            cursor: pointer;
            transition: color 0.2s;
        }
        .kintai-modal-close:hover {
            color: #000;
        }
        .modal-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 15px;
        }
        .modal-close {
            position: absolute;
            top: 10px; right: 15px;
            font-size: 24px;
            cursor: pointer;
        }
        .modal-close:hover {
            color: #000;
        }

        /* 報告書出力パネルのスタイル */
        .report-panel {
            background-color: #f8f9fa;
            border: 1px solid #dee2e6;
            border-radius: 8px;
            padding: 16px;
            flex-grow: 1;
            min-height: fit-content;
        }
        .report-panel .panel-title {
            font-size: 13px;
            font-weight: bold;
            color: #495057;
            margin-bottom: 10px;
            border-bottom: 1px solid #dee2e6;
            padding-bottom: 6px;
        }
        .report-buttons {
            display: flex;
            flex-wrap: wrap;
            gap: 8px;
            margin-bottom: 10px;
        }
        .report-btn {
            background-color: #28a745;
            color: white;
            border: none;
            padding: 7px 12px;
            border-radius: 4px;
            cursor: pointer;
            font-size: 12px;
            transition: background-color 0.2s;
            display: flex;
            align-items: center;
            gap: 3px;
        }
        .report-btn:hover {
            background-color: #218838;
        }
        .report-btn.secondary {
            background-color: #6c757d;
        }
        .report-btn.secondary:hover {
            background-color: #545b62;
        }
        .report-btn.warning {
            background-color: #fd7e14;
        }
        .report-btn.warning:hover {
            background-color: #e96b00;
        }
        .format-options {
            display: flex;
            gap: 8px;
            align-items: center;
            font-size: 13px;
            color: #495057;
        }
        .format-btn {
            background-color: #007bff;
            color: white;
            border: none;
            padding: 5px 10px;
            border-radius: 3px;
            cursor: pointer;
            font-size: 12px;
            transition: background-color 0.2s;
        }
        .format-btn:hover {
            background-color: #0056b3;
        }
        .compliance-check-buttons {
            display: flex;
            flex-wrap: wrap;
            gap: 8px;
            margin-bottom: 10px;
        }
        .compliance-btn {
            background-color: #17a2b8; /* Info Blue */
            color: white;
            border: none;
            padding: 7px 12px;
            border-radius: 4px;
            cursor: pointer;
            font-size: 12px;
            transition: background-color 0.2s;
            display: flex;
            align-items: center;
            gap: 3px;
        }
        .compliance-btn:hover {
            background-color: #138496;
        }
        .compliance-btn.comprehensive {
            background-color: #007bff; /* Primary Blue */
        }
        .compliance-btn.comprehensive:hover {
            background-color: #0056b3;
        }
        
        .enter-button {
		    display: inline-block;
		    background-color: #e74c3c; /* 赤 */
		    color: #fff; /* 文字色は白 */
		    font-weight: bold;
		    padding: 10px 20px;
		    text-decoration: none;
		    border-radius: 5px;
		    transition: background-color 0.3s;
		}
		
		.enter-button:hover {
		    background-color: #c0392b; /* ホバー時に濃い赤 */
		}
		
		/* 日付セルをリンク風にする */
		/* 日付セルをリンク風にする */
		.attendance-cell {
		    color: #007bff;             /* 青色 */
		    text-decoration: underline; /* 下線 */
		    cursor: pointer;            /* ポインター */
		    font-weight: bold;
		}
		
		.attendance-cell:hover {
		    color: #0056b3;             /* ホバーで濃い青に */
		}

		
		.kintai-cell {
		  border: 1px solid #dee2e6;
		  padding: 4px;
		  text-align: center;
		  vertical-align: middle; /* 縦方向も中央に */
		}
		
		.kintai-status {
		  white-space: nowrap; /* 出勤/有給/欠勤など折り返さない */
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

        <h1 class="page-title">
            <% if (userRoleId == 1) { %>
                <% if (isSelfMode) { %>
                    勤怠記録表示
                <% } else { %>
                    従業員別勤怠記録表示
                <% } %>
            <% } else if (userRoleId == 2) { %>
                <% if (isSelfMode) { %>
                    勤怠記録表示
                <% } else { %>
                    勤怠記録表示（自分/部下）
                <% } %>
            <% } else { %>
                勤怠記録表示
            <% } %>
        </h1>

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

        <div class="main-content">
            <%-- 管理者（全員モード）のレイアウト --%>
            <% if ((userRoleId == 1 || userRoleId == 2) && !isSelfMode) { %>
                <div class="admin-top-layout"> <%-- 新しいコンテナで横並びを実現 --%>
                    <%-- 管理者統計カード (左側) --%>
                    <div class="manager-summary">
<!--                        <div class="summary-card lateness" style="border-left: 4px solid #dc3545;">-->
<!--                            <h3>🕒 遅刻者</h3>-->
<!--                            <div class="value" style="color: #dc3545;">-->
<!--                                <%= request.getAttribute("lateCount") != null ? request.getAttribute("lateCount") : 0 %>名-->
<!--                            </div>-->
<!--                            <div class="employee-list">-->
<!--                                <span style="font-size: 10px; color: #666;">今日のデータ</span>-->
<!--                            </div>-->
<!--                        </div>-->
                        <div class="summary-card holiday-work">
                            <h3>出勤予定者</h3>
                            <div class="value"><%= request.getAttribute("scheduledCount") != null ? request.getAttribute("scheduledCount") : 0 %>名</div>
                            <div class="employee-list">
                                <!-- 真実のデータに基づいて動的に表示される予定 -->
                                <span style="font-size: 10px; color: #666;">今日のデータ</span>
                            </div>
                        </div>
                        <div class="summary-card absent">
                            <h3>欠勤者</h3>
                            <div class="value"><%= request.getAttribute("absentCount") != null ? request.getAttribute("absentCount") : 0 %>名</div>
                            <div class="employee-list">
                                <span style="font-size: 10px; color: #666;">今日のデータ</span>
                            </div>
                        </div>
                        <div class="summary-card leave">
                            <h3>休暇申請者</h3>
                            <div class="value"><%= request.getAttribute("vacationCount") != null ? request.getAttribute("vacationCount") : 0 %>名</div>
                            <div class="employee-list">
                                <span style="font-size: 10px; color: #666;">今日のデータ</span>
                            </div>
                        </div>
                    </div>

                    <div class="middle-section-admin"> <%-- 中間の検索フォーム --%>
                        <%-- 管理者フィルターフォーム --%>
                        <div class="filter-form" style="padding: 12px; background-color: #f8f9fa; border-radius: 6px;">
                            <form id="adminSearchForm" onsubmit="searchKintaiRecords(event)">
                                <%-- 第一行：従業員番号/氏名、部署、役職 --%>
                                <div style="display: flex; gap: 12px; margin-bottom: 10px; align-items: center;">
                                	<div class="filter-group" style="flex: 0 0 150px;">
                                        <label for="deptNoFilter" style="font-size: 12px; margin-right: 5px;">部署:</label>
                                        <select id="deptNoFilter" name="deptNoFilter" style="font-size: 11px; padding: 4px;">
                                            <% if (userRoleId == 1) { %>
                                                <option value="">全ての部署</option>
                                                <% for (DeptBean dept : deptList) { %>
                                                    <option value="<%= dept.getDeptNo() %>" <%= dept.getDeptNo().equals(deptNoFilter != null ? deptNoFilter : "") ? "selected" : "" %>>
                                                        <%= dept.getDeptName() %>
                                                    </option>
                                                <% } %>
                                            <% } else if (userRoleId == 2) { %>
                                                <%-- 部長の場合は自分の部署のみ表示 --%>
                                                <% 
                                                    String userDeptNo = user.getDeptNo();
                                                    String userDeptName = ""; 
                                                    for (DeptBean dept : deptList) {
                                                        if (dept.getDeptNo().equals(userDeptNo)) {
                                                            userDeptName = dept.getDeptName();
                                                            break;
                                                        }
                                                    }
                                                %>
                                                <option value="<%= userDeptNo %>" selected><%= userDeptName %></option>
                                            <% } %>
                                        </select>
                                    </div>
                                	<div class="filter-group" style="flex: 0 0 150px;">
                                        <label for="postNoFilter" style="font-size: 12px; margin-right: 5px;">役職:</label>
                                        <select id="postNoFilter" name="postNoFilter" style="font-size: 11px; padding: 4px;">
                                            <option value="">役職</option>
                                            <% for (PostBean post : postList) { %>
                                                <option value="<%= post.getPostNo() %>" <%= post.getPostNo().equals(postNoFilter != null ? postNoFilter : "") ? "selected" : "" %>>
                                                    <%= post.getPostName() %>
                                                </option>
                                            <% } %>
                                        </select>
                                    </div>
                                    <div class="filter-group" style="flex: 1;">
									    <label for="empNoFilter" style="font-size: 12px; margin-right: 5px;">従業員番号/氏名:</label>
									    <select id="empNoFilter" name="empNoFilter" 
										        style="font-size: 11px; padding: 4px;">
										    <option value="">ログインユーザー</option>
										    <% for (EmpBean emp : empList) { %>
										        <option value="<%= emp.getEmpId() %>" 
										                data-dept="<%= emp.getDeptNo() %>" 
										                data-post="<%= emp.getPostNo() %>"
										                <%= emp.getEmpId().equals(selectedEmpId) ? "selected" : "" %>>
										            <%= emp.getEmpId() %> <%= emp.getEmpName() %>
										        </option>
										    <% } %>
										</select>

									</div>

									                                    
                                </div>
                    			
                            </form>
                        </div>
                        
                        <%-- 法令遵守違反者リスト表示 --%>
                        <div style="background: linear-gradient(135deg, #fff3cd 0%, #ffeaa7 100%); border: 1px solid #ffeeba; border-radius: 8px; padding: 15px; margin-top: 15px;">
                            <div style="font-weight: bold; margin-bottom: 10px; color: #856404; font-size: 14px;">
                                ⚠️ 今月の要確認者
                            </div>
                            
                            <% if (violationEmployees != null && !violationEmployees.isEmpty()) { %>
                                <div style="display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 10px;">
                                    <% 
                                    int displayCount = 0;
                                    for (String empName : violationEmployees) { 
                                        if (displayCount >= 6) break; // 最大6名まで表示
                                    %>
                                        <span class="employee-name" onclick="showViolationEmployeeDetail('<%= empName %>')" 
                                              style="background-color: #f8d7da; color: #721c24; padding: 4px 8px; border-radius: 4px; cursor: pointer; font-size: 12px; transition: background-color 0.2s;">
                                            <%= empName %>
                                        </span>
                                    <% 
                                        displayCount++;
                                    } 
                                    %>
                                    <% if (violationEmployees.size() > 6) { %>
                                        <span style="color: #856404; font-size: 12px; padding: 4px 8px;">
                                            ... 他<%= violationEmployees.size() - 6 %>名
                                        </span>
                                    <% } %>
                                </div>
                                
                                <!-- More按钮 -->
                                <div style="text-align: center;">
                                    <button onclick="showAllViolationEmployees()" 
                                            style="background-color: #fd7e14; color: white; border: none; padding: 6px 16px; border-radius: 4px; cursor: pointer; font-size: 12px; transition: background-color 0.2s;">
                                        詳細表示
                                    </button>
                                </div>
                            <% } else { %>
                                <div style="text-align: center; color: #155724; font-size: 13px; padding: 10px;">
                                    ✅ 今月は要確認者はいません
                                </div>
                            <% } %>
                        </div>
                    </div>

                    <div class="right-section-admin"> <%-- 右側の今日の勤怠状況と報告書出力 --%>
                        <div class="daily-status">
                            <div class="status-title">📈 今日の勤怠状況 (<%= java.time.LocalDate.now().toString() %>)</div>
                            <div class="status-items">
                                <div style="display: flex; gap: 12px; margin-bottom: 8px;">
                                    <span>出勤予定: <%= request.getAttribute("scheduledCount") != null ? request.getAttribute("scheduledCount") : 0 %>名</span>
                                    <span>出勤中: <%= request.getAttribute("workingCount") != null ? request.getAttribute("workingCount") : 0 %>名</span>
                                    <span>未出勤: <%= request.getAttribute("absentCount") != null ? request.getAttribute("absentCount") : 0 %>名</span>
                                </div>
                                <div style="display: flex; gap: 12px;">
                                    <span>休暇予定: <%= request.getAttribute("vacationCount") != null ? request.getAttribute("vacationCount") : 0 %>名</span>
                                </div>
                            </div>
                        </div>
                        <%-- 勤務時間報告書出力パネル --%>
                        <div class="report-panel">
                            <div class="panel-title">📄 勤務時間報告書出力</div>
                            <%-- 期間選択（縦並び） --%>
						    <div style="display: flex; flex-direction: column; gap: 8px; margin-bottom: 12px;">
						        <div style="display: flex; flex-direction: column;">
						            <label for="startDate">開始日:</label>
						            <input type="date" id="startDate" name="startDate">
						        </div>
						        <div style="display: flex; flex-direction: column;">
						            <label for="endDate">終了日:</label>
						            <input type="date" id="endDate" name="endDate">
						        </div>
						    </div>
                            <div class="report-buttons">
                                <button class="report-btn" onclick="generateIndividualReport()">📋 個人別月次報告</button>
								<button class="report-btn" onclick="generateDepartmentReport()">📊 部署別集計報告</button>
                            </div>
                            <div class="format-options">
                                <span>出力形式: 📊 Excel</span>
                            </div>
                        </div>
                    </div>
                </div>
            <% } else { %>
                <%-- 一般社員（または管理者・自分モード）のレイアウト --%>
                <%-- 新しいレイアウト：左側にカード、右側にはセレクトボックス、チェック結果、勤務時間一覧を配置 --%>
                <div style="display: flex; gap: 20px; align-items: flex-start; margin-bottom: 20px;">
                    <%-- 四枚のカード（左側、縦に並べる） --%>
                    <div style="flex: 0 0 180px; display: flex; flex-direction: column; gap: 12px;">
                        <div class="summary-card attendance" style="height: 70px; min-width: 160px; padding: 8px;">
                            <h3 style="font-size: 11px; margin: 0 0 4px 0;">自分の出勤日/会社の出社日</h3>
                            <div class="value" style="font-size: 14px; margin: 2px 0;"><%= monthlySummary.getAttendanceRateString() %></div>
                            <div class="detail" style="font-size: 9px; margin: 0;">今月の出勤状況</div>
                        </div>
                        <div class="summary-card overtime" style="height: 70px; min-width: 160px; padding: 8px;">
                            <h3 style="font-size: 11px; margin: 0 0 4px 0;">残業時間</h3>
                            <div class="value" style="font-size: 14px; margin: 2px 0;"><%= monthlySummary.getTotalOvertimeHoursString() %></div>
                            <div class="detail" style="font-size: 9px; margin: 0;">今月の総残業時間</div>
                        </div>
                        <div class="summary-card working" style="height: 70px; min-width: 160px; padding: 8px;">
                            <h3 style="font-size: 11px; margin: 0 0 4px 0;">総実働時間</h3>
                            <div class="value" style="font-size: 14px; margin: 2px 0;"><%= monthlySummary.getTotalWorkingHoursString() %></div>
                            <div class="detail" style="font-size: 9px; margin: 0;">今月の総実働時間</div>
                        </div>
                        <div class="summary-card break" style="height: 70px; min-width: 160px; padding: 8px;">
                            <h3 style="font-size: 11px; margin: 0 0 4px 0;">総休憩時間</h3>
                            <div class="value" style="font-size: 14px; margin: 2px 0;"><%= monthlySummary.getTotalBreakHoursString() %></div>
                            <div class="detail" style="font-size: 9px; margin: 0;">今月の総休憩時間</div>
                        </div>
                    </div>
                    
                    <%-- 右側のコンテナ：セレクトボックス、チェック結果、勤務時間一覧を含む --%>
                    <div style="flex: 1; display: flex; flex-direction: column; gap: 20px;">
                        <%-- 上半分：期間選択と法令遵守チェック結果を横並び --%>
                        <div style="display: flex; gap: 20px;">
                            <%-- 期間選択用のコンテナ --%>
<!--                            <div style="flex: 0 0 350px;">-->
<!--                                <div class="filter-form" style="min-width: 350px;">-->
<!--                                    <form id="selfSearchForm" onsubmit="searchSelfKintaiRecords(event)" style="display: flex; flex-wrap: wrap; gap: 12px;">-->
<!--                                        <div class="date-group">-->
<!--                                            <div class="filter-group">-->
<!--                                                <label for="startDateSelf">期間(開始):</label>-->
<!--                                                <input type="date" id="startDateSelf" name="startDate" value="<%= startDate != null ? startDate : "" %>">-->
<!--                                            </div>-->
<!--                                            <div class="filter-group">-->
<!--                                                <label for="endDateSelf">期間(終了):</label>-->
<!--                                                <input type="date" id="endDateSelf" name="endDate" value="<%= endDate != null ? endDate : "" %>">-->
<!--                                            </div>-->
<!--                                        </div>-->
<!--                                        <button type="submit">検索</button>-->
<!--                                        <input type="hidden" name="mode" value="self">-->
<!--                                    </form>-->
<!--                                </div>-->
<!--                            </div>-->
                            
                            <%-- 法令遵守チェック結果 --%>
                            <div style="flex: 1; min-width: 280px;">
                                <% if (complianceResult != null) { %>
                                    <div style="background: linear-gradient(135deg, #e8f4fd 0%, #f0f8ff 100%); border: 1px solid #bee5eb; border-radius: 8px; padding: 12px;">
                                        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px;">
                                            <div style="font-weight: bold; color: #0c5460; font-size: 12px;">
                                                📋 法令及び会社規則遵守チェック結果（今月）
                                            </div>
                                            <button onclick="showCheckItems()" style="background-color: #17a2b8; color: white; border: none; padding: 3px 6px; border-radius: 3px; cursor: pointer; font-size: 10px;">
                                                チェック内容
                                            </button>
                                        </div>
                                        
                                        <div style="display: flex; align-items: center; gap: 6px; margin-bottom: 8px;">
                                            <span style="font-size: 16px; font-weight: bold; color: <%= complianceResult.getTotalViolations() == 0 ? "#28a745" : "#dc3545" %>;">
                                                <%= complianceResult.getTotalViolations() %>件
                                            </span>
                                            <span style="font-size: 11px; color: #666;">違反項目</span>
                                        </div>
                                        
                                        <% if (complianceResult.getViolations() != null && !complianceResult.getViolations().isEmpty()) { %>
                                            <div style="font-size: 10px; color: #721c24; background-color: #f8d7da; border: 1px solid #f5c6cb; border-radius: 3px; padding: 6px;">
                                                <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 4px;">
                                                    <strong>主な違反項目:</strong>
                                                    <button onclick="showViolationDetails()" style="background-color: #007bff; color: white; border: none; padding: 2px 6px; border-radius: 3px; cursor: pointer; font-size: 9px;">
                                                        詳細表示
                                                    </button>
                                                </div>
                                                <% 
                                                int displayCount = 0;
                                                for (ComplianceViolation violation : complianceResult.getViolations()) { 
                                                    if (displayCount >= 2) break; // 最大2件まで表示
                                                %>
                                                    • <%= violation.getViolationType() %><br>
                                                <% 
                                                    displayCount++;
                                                } 
                                                if (complianceResult.getViolations().size() > 2) {
                                                %>
                                                    ... 他<%= complianceResult.getViolations().size() - 2 %>件
                                                <% } %>
                                            </div>
                                        <% } else { %>
                                            <div style="font-size: 10px; color: #155724; background-color: #d4edda; border: 1px solid #c3e6cb; border-radius: 3px; padding: 6px;">
                                                ✅ 違反項目はありません
                                            </div>
                                        <% } %>
                                    </div>
                                <% } else { %>
                                    <div style="background-color: #f8f9fa; border: 1px solid #dee2e6; border-radius: 8px; padding: 12px; text-align: center; color: #6c757d; font-size: 11px;">
                                        法令遵守チェック結果がありません
                                    </div>
                                <% } %>
                            </div>
                        </div>
                        

                    </div>
                </div>
            <% } %>

        </div>

        <%-- 隠された勤務時間一覧テーブル（AJAX用データソース） --%>
        <div id="hiddenTableContainer" style="display: none;">
            <table class="kintai-table">
                <thead>
                    <tr>
                        <th>日付</th>
                        <th>曜日</th>
                        <%-- 自分モードでは従業員情報列を表示しない --%>
                        <% if ((userRoleId == 1 || userRoleId == 2) && !isSelfMode) { %>
                            <th>従業員番号</th>
                            <th>氏名</th>
                            <th>部署</th>
                            <th>役職</th>
                        <% } %>
                        <th>出勤時刻</th>
                        <th>退勤時刻</th>
                        <th>休憩時間合計</th>
                        <th>実働時間</th>
                        <th>残業時間</th>
                        <th>深夜残業時間</th>
                        <th>プロジェクト状況</th>
                    </tr>
                </thead>
                <tbody>
                    <% if (kintaiRecords != null && !kintaiRecords.isEmpty()) { %>
                        <% for (KintaiRecBean record : kintaiRecords) { %>
                            <%
                                String dayClass = "";
                                if (record.getKintaiDate() != null) {
                                    DayOfWeek dayOfWeek = record.getKintaiDate().getDayOfWeek();
                                    if (dayOfWeek == DayOfWeek.SATURDAY) {
                                        dayClass = "saturday";
                                    } else if (dayOfWeek == DayOfWeek.SUNDAY) {
                                        dayClass = "sunday";
                                    }
                                }
                            %>
                            <tr class="<%= dayClass %>">
                                <td><%= record.getKintaiDate() != null ? record.getKintaiDate().toString() : "---" %></td>
                                <td><%= record.getKintaiDate() != null ? dayOfWeekMap.get(record.getKintaiDate().getDayOfWeek()) : "---" %></td>
                                <%-- 自分モードでは従業員情報列を表示しない --%>
                                <% if ((userRoleId == 1 || userRoleId == 2) && !isSelfMode) { %>
                                    <td><%= record.getEmpno() != null ? record.getEmpno() : "---" %></td>
                                    <td><%= record.getEmpName() != null ? record.getEmpName() : "---" %></td>
                                    <td><%= record.getDeptName() != null ? record.getDeptName() : "---" %></td>
                                    <td><%= record.getPostName() != null ? record.getPostName() : "---" %></td>
                                <% } %>
                                <td><%= record.getClockIn() != null ? record.getClockIn().toString().substring(0, 5) : "---" %></td>
                                <td><%= record.getClockOut() != null ? record.getClockOut().toString().substring(0, 5) : "---" %></td>
                                <td><%= record.getTotalBreakTimeFormatted() %></td>
                                <td><%= record.getActualWorkTimeFormatted() %></td>
                                <td><%= record.getOvertimeFormatted() %></td>
                                <td><%= record.getNightovertimeFormatted() %></td>
                                <td style="border: 1px solid #dee2e6; padding: 4px; text-align: center;">
                                <%
                                	List<KinmuManageBean.WorkAlloc> kinmuList = (List<KinmuManageBean.WorkAlloc>)request.getAttribute("kinmuList");
							        // 当日のプロジェクト合計時間を計算
							        double totalWorkHours = 0.0;
							        if (kinmuList != null) {
							            for (KinmuManageBean.WorkAlloc k : kinmuList) {
							                if (record.getKintaiDate().equals(k.getWorkDate()) && record.getEmpId().equals(k.getEmpId())) {
							                    totalWorkHours += k.getWorkHours();
							                }
							            }
							        }
							        String totalHHMM = KinmuManageBean.WorkAlloc.formatHoursToHHMM(totalWorkHours);
							    %>
							    <%= totalHHMM %>
							    <button class="btn-detail" data-date="<%= record.getKintaiDate() %>" data-empid="<%= record.getEmpId() %>">詳細</button>
							    </td>
                            </tr>
                        <% } %>
                    <% } else { %>
                        <tr>
                            <%-- 自分モードかどうかでcolspan数を調整（残業時間列追加により+1）--%>
                            <td colspan="<%= ((userRoleId == 1 || userRoleId == 2) && !isSelfMode) ? 11 : 7 %>" style="text-align: center;">
                                勤怠記録がありません
                            </td>
                        </tr>
                    <% } %>
                </tbody>
            </table>
        </div>
        
        <!-- 新たにリスト型での勤怠記録一覧の表示 -->
        <!-- 月切り替えボタン -->
		<div style="display:flex; justify-content: center; align-items: center; margin-bottom: 20px; gap: 15px;">
		    <!-- 前月ボタン -->
		    <a href="?year=<%=prevMonth.getYear()%>&month=<%=prevMonth.getMonthValue()%>"
		       style="padding: 6px 12px; background-color: #f8f9fa; border: 1px solid #dee2e6; border-radius: 8px; text-decoration:none; color:#007bff;">
		       &lt; 前月
		    </a>
		
		    <!-- 現在の年月表示 -->
		    <span style="font-weight:bold; font-size:14px;"><%=year%>年<%=month%>月</span>
		
		    <!-- 翌月ボタン -->
		    <a href="?year=<%=nextMonth.getYear()%>&month=<%=nextMonth.getMonthValue()%>"
		       style="padding: 6px 12px; background-color: #f8f9fa; border: 1px solid #dee2e6; border-radius: 8px; text-decoration:none; color:#007bff;">
		       翌月 &gt;
		    </a>
		</div>
				
		

		<!-- 勤怠表 -->
		<div id="kintaiTableContainer" style="width: 100%;">
		    <div style="background-color: #f8f9fa; border: 1px solid #dee2e6; border-radius: 8px; padding: 16px;">
		        <div style="max-height: 500px; overflow-y: auto;">
		            <table style="width:100%; border-collapse: collapse; font-size: 11px;">
		                <thead style="background-color: #e9ecef; position: sticky; top: 0;">
		                    <tr>
		                        <th style="border:1px solid #dee2e6; padding:6px; text-align:center;">日付</th>
		                        <th style="border:1px solid #dee2e6; padding:6px; text-align:center;">曜日</th>
		                        <th style="border:1px solid #dee2e6; padding:6px; text-align:center;">勤怠種別</th>
		                        <th style="border:1px solid #dee2e6; padding:6px; text-align:center;">イベント名</th>
		                        <th style="border:1px solid #dee2e6; padding:6px; text-align:center;">勤怠</th>
		                        <th style="border:1px solid #dee2e6; padding:6px; text-align:center;">出勤時間</th>
		                        <th style="border:1px solid #dee2e6; padding:6px; text-align:center;">退勤時間</th>
		                        <th style="border:1px solid #dee2e6; padding:6px; text-align:center;">休憩時間</th>
		                        <th style="border:1px solid #dee2e6; padding:6px; text-align:center;">勤務時間</th>
		                        <th style="border:1px solid #dee2e6; padding:6px; text-align:center;">残業</th>
		                        <th style="border:1px solid #dee2e6; padding:6px; text-align:center;">深夜残業</th>
		                        <th style="border:1px solid #dee2e6; padding:6px; text-align:center;">プロジェクト状況</th>
		                    </tr>
		                </thead>
		                <tbody id="kintaiTableBody">
		                <%
		                    // 表示対象の従業員IDを決定（選択中があればそれ、なければログインユーザ）
		                    String displayEmpId = (selectedEmpId != null) 
		                        ? selectedEmpId 
		                        : ((UserBean) session.getAttribute("user")).getEmpno();
		
		                    // 表示対象の年月
		                    YearMonth displayMonth = YearMonth.of(year, month);
		                    LocalDate monthStart = displayMonth.atDay(1);
		                    LocalDate monthEnd = displayMonth.atEndOfMonth();
		
		                    // リクエスト属性から勤怠・イベント・休暇データを取得
		                    List<CalendarEventBean> eventList = (List<CalendarEventBean>) request.getAttribute("eventList");
		                    List<KinmuManageBean.WorkAlloc> kinmuList = (List<KinmuManageBean.WorkAlloc>) request.getAttribute("kinmuList");
		                    Map<String, Map<LocalDate, KintaiRecBean>> recMapByEmp =
		                        (Map<String, Map<LocalDate, KintaiRecBean>>) request.getAttribute("recMapByEmp");
		                    Map<LocalDate, KintaiRecBean> empRecMap = (recMapByEmp != null) ? recMapByEmp.get(displayEmpId) : new HashMap<>();
		
		                    Map<String, Map<LocalDate, LeaveRecBean>> leaveMapByEmp =
		                        (Map<String, Map<LocalDate, LeaveRecBean>>) request.getAttribute("leaveMap");
		                    Map<LocalDate, LeaveRecBean> empLeaveMap = (leaveMapByEmp != null) ? leaveMapByEmp.get(displayEmpId) : new HashMap<>();
		                    
		                 	// --- 合計用変数 ---
		                    long totalBreakMinutes = 0;
		                    long totalWorkMinutes = 0;
		                    long totalOvertimeMinutes = 0;
		                    long totalNightMinutes = 0;
		                    double totalProjectHours = 0.0;
		
		                    // 月の日付ごとのループ
		                    for (LocalDate date = monthStart; !date.isAfter(monthEnd); date = date.plusDays(1)) {
		                        // 曜日を取得（日本語短縮形）
		                        String dayOfWeek = date.getDayOfWeek().getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.JAPANESE);
		
		                        // 行の背景色設定（土曜・日曜）
		                        String rowStyle = "";
		                        if(date.getDayOfWeek() == java.time.DayOfWeek.SATURDAY){
		                            rowStyle = "background-color: rgba(0, 0, 255, 0.1);"; // 土曜
		                        } else if(date.getDayOfWeek() == java.time.DayOfWeek.SUNDAY){
		                            rowStyle = "background-color: rgba(255, 0, 0, 0.1);"; // 日曜
		                        }
		
		                        // 当日勤怠データ・イベント・休暇を取得
		                        KintaiRecBean kintaiRec = (empRecMap != null) ? empRecMap.get(date) : null;
		
		                        CalendarEventBean event = null;
		                        if(eventList != null){
		                            for(CalendarEventBean e : eventList){
		                                if(date.equals(e.getEventDate())){
		                                    event = e; break;
		                                }
		                            }
		                        }
		
		                        LeaveRecBean leave = (empLeaveMap != null) ? empLeaveMap.get(date) : null;
		
		                        // 休日イベントがあれば背景色を優先
		                        if(event != null && "休日".equals(event.getWorkStatusName())){
		                            rowStyle = "background-color: rgba(255, 0, 0, 0.1);";
		                        }
		
		                        // 当日のプロジェクト勤務時間合計を計算
		                        double totalWorkHours = 0.0;
		                        if (kinmuList != null) {
		                            for (KinmuManageBean.WorkAlloc k : kinmuList) {
		                                if (date.equals(k.getWorkDate()) && displayEmpId.equals(k.getEmpId())) {
		                                    totalWorkHours += k.getWorkHours();
		                                }
		                            }
		                        }
		
		                        // 表示用文字列初期化
		                        String leaveTypeName = "&nbsp;", leaveHours = "&nbsp;", clockIn = "&nbsp;", clockOut = "&nbsp;";
		                        String breakTime = "&nbsp;", actualWork = "&nbsp;", overtime = "&nbsp;", nightOvertime = "&nbsp;";
		
		                        // 休暇判定
		                        if(leave != null){
		                            switch(leave.getLeaveTypeId()){
		                                case LeaveRecDao.LEAVE_TYPE_PAID: leaveTypeName="有給"; leaveHours="8:00"; break;
		                                case LeaveRecDao.LEAVE_TYPE_SPECIAL: leaveTypeName="特別休暇"; break;
		                                case LeaveRecDao.LEAVE_TYPE_COMP: leaveTypeName="代休"; leaveHours="8:00"; break;
		                                default: leaveTypeName="無給"; break;
		                            }
		                        } 
		                        // 出勤判定
		                        else if(kintaiRec != null){
		                            leaveTypeName = "出勤";
		                            clockIn = (kintaiRec.getClockIn() != null) ? kintaiRec.getClockIn().toLocalTime().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")) : "&nbsp;";
		                            clockOut = (kintaiRec.getClockOut() != null) ? kintaiRec.getClockOut().toLocalTime().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")) : "&nbsp;";
		                            breakTime = kintaiRec.getTotalBreakTimeFormatted();
		                            actualWork = kintaiRec.getActualWorkTimeFormatted();
		                            overtime = kintaiRec.getOvertimeFormatted();
		                            nightOvertime = kintaiRec.getNightovertimeFormatted();
		                        }
		                        
		                     	// 各日のデータ処理
		                        if (kintaiRec != null) {
		                            totalBreakMinutes += kintaiRec.getTotalBreakMinutes();
		                            totalWorkMinutes += kintaiRec.getActualWorkMinutes();
		                            totalOvertimeMinutes += kintaiRec.getOvertimeMinutes();
		                            totalNightMinutes += kintaiRec.getNightovertimeMinutes();
		                        }
		                        totalProjectHours += totalWorkHours;
		                %>
		                <tr style="<%=rowStyle%>">
						    <!-- 日付セルに data 属性追加 -->
						    <!-- 例: 日付セル -->
							<td class="attendance-cell"
							    id="cell-<%= displayEmpId %>-<%= date %>"
							    data-empid="<%= displayEmpId %>"
							    data-date="<%= date %>"
							    style="border:1px solid #dee2e6; padding:4px; text-align:center;">
							  <%= date.getDayOfMonth() %> 
							</td>



		                    <td style="border:1px solid #dee2e6; padding:4px; text-align:center;"><%=dayOfWeek%></td>
		                    <td style="border:1px solid #dee2e6; padding:4px; text-align:center;"><%= (event != null) ? event.getWorkStatusName() : "" %></td>
		                    <td style="border:1px solid #dee2e6; padding:4px; text-align:center;"><%= (event != null) ? event.getEventName() : "" %></td>
		                   <td
							  id="cell-<%= displayEmpId %>-<%= date.toString() %>"
							  data-empid="<%= displayEmpId %>"
							  data-date="<%= date.toString() %>"
							  class="kintai-cell kintai-status">
							  <%= (kintaiRec != null && kintaiRec.getAttendanceType() != null) 
							        ? kintaiRec.getAttendanceType() 
							        : "" %>
							</td>


		                    <td style="border:1px solid #dee2e6; padding:4px; text-align:center;"><%=clockIn %></td>
		                    <td style="border:1px solid #dee2e6; padding:4px; text-align:center;"><%=clockOut %></td>
		                    <td style="border:1px solid #dee2e6; padding:4px; text-align:center;"><%=breakTime %></td>
		                    <!-- 勤務時間は出勤データがなければ休暇時間を表示 -->
		                    <td style="border:1px solid #dee2e6; padding:4px; text-align:center;"><%=actualWork.equals("&nbsp;") ? leaveHours : actualWork %></td>
		                    <td style="border:1px solid #dee2e6; padding:4px; text-align:center;"><%=overtime %></td>
		                    <td style="border:1px solid #dee2e6; padding:4px; text-align:center;"><%=nightOvertime %></td>
		                    <td id="proj-<%= (kintaiRec != null ? kintaiRec.getEmpId() : displayEmpId) %>-<%= date.toString() %>"
							    style="border:1px solid #dee2e6; padding:4px; text-align:center;">
							    <%= KinmuManageBean.WorkAlloc.formatHoursToHHMM(totalWorkHours) %>
							</td>


		                </tr>
		                <% } %>
		                <!-- ▼ 合計行を追加 -->
						<tr style="background-color:#f1f3f5; font-weight:bold;">
						    <!-- 日付セルと曜日セルを結合して「合計」と表示 -->
						    <td colspan="2" style="border:1px solid #dee2e6; padding:6px; text-align:center;">合計</td>
						    <!-- 勤怠種別～イベントは空 -->
						    <td style="border:1px solid #dee2e6;" colspan="3">&nbsp;</td>
						    <!-- 出勤/退勤時間は空 -->
						    <td style="border:1px solid #dee2e6;">&nbsp;</td>
						    <td style="border:1px solid #dee2e6;">&nbsp;</td>
						    <!-- 合計値を表示 -->
						    <td style="border:1px solid #dee2e6; text-align:center;">
						        <%= String.format("%d:%02d", totalBreakMinutes/60, totalBreakMinutes%60) %>
						    </td>
						    <td style="border:1px solid #dee2e6; text-align:center;">
						        <%= String.format("%d:%02d", totalWorkMinutes/60, totalWorkMinutes%60) %>
						    </td>
						    <td style="border:1px solid #dee2e6; text-align:center;">
						        <%= String.format("%d:%02d", totalOvertimeMinutes/60, totalOvertimeMinutes%60) %>
						    </td>
						    <td style="border:1px solid #dee2e6; text-align:center;">
						        <%= String.format("%d:%02d", totalNightMinutes/60, totalNightMinutes%60) %>
						    </td>
						    <td style="border:1px solid #dee2e6; text-align:center;">
						        <%= KinmuManageBean.WorkAlloc.formatHoursToHHMM(totalProjectHours) %>
						    </td>
						</tr>
		                </tbody>
		            </table>
		            	            <!-- 分析ボタン -->
					<div style="text-align:right; margin-top: 10px;">
					    <button type="button" id="btn-project-analysis" 
						        data-empid="<%= displayEmpId %>" 
						        data-start="<%= monthStart %>" 
						        data-end="<%= monthEnd %>"
						        data-title="<%= monthStart.getYear() %>年<%= monthStart.getMonthValue() %>月">
						    📊 プロジェクト分析
						</button>

					</div>
		            <%-- ▼ 勤怠表サマリー ▼ --%>
					<% if (monthlySummary != null) { %>
					<div style="margin-top: 30px; padding: 15px; border: 1px solid #ccc; border-radius: 6px;">
					    <h3>月次サマリー（<%= monthlySummary.getTargetMonth() %>）</h3>
					    <div style="display: flex; gap: 40px; margin-top: 15px;">
					
					        <!-- 出勤状況 -->
					        <table border="1" style="border-collapse: collapse; text-align:center; min-width:150px;">
					            <tr><th colspan="2">出勤状況</th></tr>
					            <tr><td>会社出勤日</td><td><%= monthlySummary.getTotalWorkDays() %></td></tr>
					            <tr><td>出勤日</td><td><%= monthlySummary.getActualAttendanceDays() %></td></tr>
					            <tr><td>有給</td><td><%= monthlySummary.getPaidLeaveDays() %></td></tr>
					            <tr><td>欠勤</td><td><%= monthlySummary.getAbsentDays() %></td></tr>
					        </table>
					
					        <!-- 特殊勤務 -->
					        <table border="1" style="border-collapse: collapse; text-align:center; min-width:150px;">
					            <tr><th colspan="2">特殊勤務</th></tr>
					            <tr><td>休日出勤</td><td><%= monthlySummary.getHolidayWorkDays() %></td></tr>
					            <tr><td>代休</td><td><%= monthlySummary.getCompensatoryLeaveDays() %></td></tr>
					        </table>
					
					        <!-- 勤務時間 -->
					        <table border="1" style="border-collapse: collapse; text-align:center; min-width:200px;">
					            <tr><th colspan="2">勤務時間</th></tr>
					            <tr><td>勤務時間</td><td><%= monthlySummary.getTotalWorkingHoursString() %></td></tr>
					            <tr><td>うち残業時間</td><td><%= monthlySummary.getTotalOvertimeHoursString() %></td></tr>
					            <tr><td>うち深夜時間</td><td><%= monthlySummary.getTotalNightHoursString() %></td></tr>
					            <tr><td>休憩時間</td><td><%= monthlySummary.getTotalBreakHoursString() %></td></tr>
					        </table>
					
					    </div>
					</div>
					<% } %>
		            
	
					
					<!-- モーダル -->
					<div id="projectModal" style="display:none; position:fixed; z-index:1000; left:0; top:0; width:100%; height:100%; background:rgba(0,0,0,0.5);">
					  <div style="background:#fff; margin:10% auto; padding:20px; width:600px; border-radius:8px; position:relative;">
					    <span id="modalClose" style="position:absolute; top:10px; right:15px; font-size:20px; cursor:pointer;">&times;</span>
					    <h3 style="margin-top:0; text-align:center;">月間プロジェクト別工数分析</h3>
					    <div id="projectPieChart" style="width:100%; height:400px;"></div>
					  </div>
					</div>
		        </div>
		    </div>
		</div>
<!-- JS が使う hidden -->
<input type="hidden" id="yearHidden" value="<%= displayMonth.getYear() %>">
<input type="hidden" id="monthHidden" value="<%= displayMonth.getMonthValue() %>">
</body>
</html>
		
<!--   <div style="text-align: center; margin-top: 10px;">-->
<!--		<a href="<%= backUrl %>" class="enter-button">今月の勤怠を確定</a>-->
<!--	</div>-->
    <div style="text-align: center; margin-top: 10px;">
		<a href="<%= backUrl %>" class="back-link">メニューへ戻る</a>
	</div>
	
	
	<!-- モーダルHTML -->
	<!-- ★勤怠入力モーダル -->
	<div id="kintaiModal" style="display:none; position:fixed; inset:0; background:rgba(0,0,0,.35); z-index:9999;">
	  <div style="position:absolute; top:10%; left:50%; transform:translateX(-50%); width:720px; background:#fff; border-radius:8px; padding:16px;">
	    <div style="display:flex; justify-content:space-between; align-items:center;">
	      <h3 id="modalTitle" style="margin:0;">-</h3>
	      <button type="button" onclick="closeKintaiModal()">✕</button>
	    </div>
	
	    <input type="hidden" id="modalEmpId">
	    <input type="hidden" id="modalDate">
	
	    <!-- 勤怠区分 -->
	    <div style="margin-top:10px;">
	      <label>勤怠区分：</label>
	      <select id="kintaiType">
	        <option value="出勤">出勤</option>
	        <option value="有給">有給</option>
	        <option value="無給">無給</option>
	        <option value="欠勤">欠勤</option>
	      </select>
	    </div>
	
	    <!-- 出勤時間（単一）+ 合計 -->
	    <div id="workTimeSection" style="margin-top:10px;">
	      <label>出勤時間：</label>
	      <input type="time" id="clockIn"> 〜
	      <input type="time" id="clockOut">
	      <span id="workTotal" style="margin-left:8px; font-weight:bold;">合計: 0:00</span>
	    </div>
	
	    <!-- 休憩（複数） -->
	    <div id="breakSection" style="margin-top:10px;">
	      <label>休憩：</label>
	      <div id="breakList"></div>
	      <button type="button" id="addBreak">＋休憩追加</button>
	    </div>
	
	    <!-- プロジェクト状況（複数行） -->
	    <div id="projectSection" style="margin-top:10px;">
	      <label>プロジェクト状況：</label>
	
	      <!-- 🔽 非表示テンプレート（表示されない） -->
	      <template id="projectTemplate">
	        <div class="project-row" style="margin:6px 0;">
	          <select class="projectSelect">
	            <% if (projectList != null) {
	                 for (ProjectBean p : projectList) { %>
	               <option value="<%= p.getProjectId() %>"><%= p.getProjectName() %></option>
	            <% } } %>
	          </select>
	          <input type="number" class="projectHours" min="0" step="0.5" style="width:80px;"> 時間
	          <button type="button" class="removeProject">－</button>
	        </div>
	      </template>
	
	      <!-- 実際に行を追加するリスト -->
	      <div id="projectList"></div>
	
	      <button type="button" id="addProject">＋プロジェクト追加</button>
	    </div>
	
	    <div style="margin-top:14px; display:flex; gap:12px; align-items:center;">
	      <button type="button" id="saveKintaiBtn">保存</button>
	      <button type="button" onclick="closeKintaiModal()">閉じる</button>
	      <label><input type="checkbox" id="continueInput"> 続けて入力</label>
	    </div>
	  </div>
	</div>

	
	
	
	<!-- 背景暗くする -->
	<div id="modalBg" style="display:none; position:fixed; top:0; left:0; width:100%; height:100%; background:rgba(0,0,0,0.4); z-index:900;"></div>
	
	<%-- 隠されたプロジェクト別作業時間割合モーダル --%>
	<div id="projectModal" class="modal" style="display:none;">
    <div class="modal-content">
        <span id="modalClose" class="modal-close">&times;</span>
        <h3>プロジェクト別作業時間割合</h3>
        <div id="projectPieChart" style="width:600px; height:400px;"></div>
    </div>
	</div>


    <%-- 個人詳細情報モーダル --%>
    <div id="employeeDetailModal" class="modal">
        <div class="modal-content">
            <div class="modal-header">
                <h2 id="modalEmployeeName">従業員詳細情報</h2>
                <span class="modal-close" onclick="closeModal()">&times;</span>
            </div>
            <div id="modalEmployeeDetails">
                <%-- 個人詳細情報がここに動的に読み込まれます --%>
                <p>読み込み中...</p>
            </div>
        </div>
    </div>

    <%-- 全員表示モーダル --%>
    <div id="moreEmployeesModal" class="modal">
        <div class="modal-content">
            <div class="modal-header">
                <h2 id="moreModalTitle">従業員一覧</h2>
                <span class="modal-close" onclick="closeMoreModal()">&times;</span>
            </div>
            <div id="moreEmployeeList">
                <%-- 全員表示がここに表示されます --%>
            </div>
        </div>
    </div>

    <%-- 勤務時間一覧モーダル --%>
    <div id="kintaiTableModal" class="kintai-modal">
        <div class="kintai-modal-content">
            <div class="kintai-modal-header">
                <h2 id="kintaiModalTitle">勤務時間一覧</h2>
                <span class="kintai-modal-close" onclick="closeKintaiModal()">&times;</span>
            </div>
            <div id="kintaiModalContainer">
                <%-- 勤務時間一覧テーブルがここに動的に表示されます --%>
            </div>
        </div>
    </div>

    <%-- 違反詳細モーダル --%>
    <div id="violationDetailsModal" class="modal">
        <div class="modal-content" style="max-width: 600px; max-height: 70%;">
            <div class="modal-header">
                <h2>法令及び会社規則遵守違反詳細</h2>
                <span class="modal-close" onclick="closeViolationDetailsModal()">&times;</span>
            </div>
            <div id="violationDetailsContent" style="max-height: 400px; overflow-y: auto;">
                <%-- 違反詳細がここに表示されます --%>
            </div>
        </div>
    </div>
    
    <%-- チェック内容モーダル --%>
<div id="checkItemsModal" class="modal">
    <div class="modal-content" style="max-width: 600px; max-height: 70%;">
        <div class="modal-header">
            <h2>法令及び会社規則遵守チェック内容</h2>
            <span class="modal-close" onclick="closeCheckItemsModal()">&times;</span>
        </div>
        <div id="checkItemsContent" style="max-height: 400px; overflow-y: auto;">
            <%-- JSから内容を差し込む --%>
        </div>
    </div>
</div>
<!-- 部署別集計報告モーダル -->
<div id="departmentReportModal" class="modal" style="display:none;">
  <div class="modal-content" style="max-width: 800px; max-height: 80%;">
    <div class="modal-header">
      <h2>部署別集計報告</h2>
      <span class="modal-close" onclick="closeDepartmentReportModal()">&times;</span>
    </div>
    <div id="departmentReportContent" style="max-height: 600px; overflow-y: auto;">
      <!-- JSで中身を差し込む -->
    </div>
  </div>
</div>

<!-- 勤怠モーダル -->
<div id="kintaiTableModal" class="modal">
  <div class="modal-content" style="max-width: 900px; max-height: 85%;">
    <div class="modal-header">
      <h2 id="kintaiModalTitle"></h2>
      <!-- ここが閉じるボタン -->
      <span class="modal-close" onclick="closeKintaiTableModal()">&times;</span>
    </div>
    <div id="kintaiModalContainer">
      <!-- showIndividualReportModalWithData がここへ差し込む -->
    </div>
  </div>
</div>



    
	
    <script>
    function closeCheckItemsModal() {
        const modal = document.getElementById("checkItemsModal");
        if (modal) {
            modal.style.display = "none";
        }
    }

    function closeKintaiTableModal() {
        document.getElementById("kintaiTableModal").style.display = "none";
    }
        

        
    const ctx = "<%= request.getContextPath() %>";
    (function() {
      const ctx = "<%= request.getContextPath() %>";

      function openKintaiModal(empId, ymd) {
    	  document.getElementById("modalEmpId").value = empId;
    	  document.getElementById("modalDate").value = ymd;
    	  document.getElementById("modalTitle").textContent = ymd + " の勤怠";



    	  // 出退勤時間・休憩は「とりあえず空」にする（新規判定は fetch 後に行う）
    	  document.getElementById("clockIn").value = "";
    	  document.getElementById("clockOut").value = "";
    	  document.getElementById("workTotal").textContent = "合計: 0:00";


    	  // DBからデータ取得
    	  fetch(ctx + "/kintaiData?empId=" + encodeURIComponent(empId) + "&date=" + encodeURIComponent(ymd), {
    	      headers: { "Accept": "application/json" }
    	    })
    	    .then(async (res) => {
    	      const ct = res.headers.get("content-type") || "";
    	      if (!ct.includes("application/json")) {
    	        const t = await res.text();
    	        throw new Error("JSON以外のレスポンス: " + t.substring(0, 200));
    	      }
    	      if (!res.ok) throw new Error("HTTP " + res.status);
    	      return res.json();
    	    })
    	   .then(data => {
			  if (!data.success) throw new Error(data.message || "取得エラー");
			
			  // 勤怠区分
			  const sel = document.getElementById("kintaiType");
			  [...sel.options].forEach(opt => {
			    opt.selected = (opt.value === (data.attendanceType || "出勤"));
			  });
			
			  if (data.attendanceType === "出勤" || !data.attendanceType) {
			    document.getElementById("clockIn").value  = data.clockIn  || "09:00";
			    document.getElementById("clockOut").value = data.clockOut || "18:00";
			
			    if (Array.isArray(data.breaks) && data.breaks.length > 0) {
			      setBreakList(data.breaks);
			    } else {
			      setBreakList([{ start: "12:00", end: "13:00" }]);
			    }
			
			    if (Array.isArray(data.projects) && data.projects.length > 0) {
			      setProjectList(data.projects);
			    }
			
			    // ★ ここでフロント計算を実行
			    calcTotal();
			
			    toggleSections(true);
			  } else {
			    toggleSections(false);
			  }
			})




    	    .catch(err => {
    	      console.error("取得失敗:", err);

    	      // 🔽 DBエラーやデータなしでもデフォルトをセット
    	      document.getElementById("clockIn").value = "09:00";
    	      document.getElementById("clockOut").value = "18:00";
    	      setBreakList([{ start: "12:00", end: "13:00" }]);
    	      calcTotal();

    	      toggleSections(true);
    	    });

    	  document.getElementById("kintaiModal").style.display = "block";
    	}





      function closeKintaiModal() {
        document.getElementById("kintaiModal").style.display = "none";
      }
      window.closeKintaiModal = closeKintaiModal;

      // セクション表示制御
      function toggleSections(isWork) {
        document.getElementById("workTimeSection").style.display = isWork ? "block" : "none";
        document.getElementById("breakSection").style.display   = isWork ? "block" : "none";
        document.getElementById("projectSection").style.display = isWork ? "block" : "none";
      }

      // 勤怠区分切替
      document.getElementById("kintaiType").addEventListener("change", function() {
        toggleSections(this.value === "出勤");
      });

      // 合計計算
      function calcTotal() {
		  const type = document.getElementById("kintaiType").value; // ★ 勤怠区分を確認
		  if (type === "有給") {
		    document.getElementById("workTotal").textContent = "合計: 8:00"; // ★ 強制表示
		    return;
		  }
		
		  const cin = document.getElementById("clockIn").value;
		  const cout = document.getElementById("clockOut").value;
		  if (!cin || !cout) {
		    document.getElementById("workTotal").textContent = "合計: 0:00";
		    return;
		  }
		
		  const toMin = (hhmm) => { const [h,m] = hhmm.split(":").map(Number); return h*60+m; }
		  let diff = toMin(cout) - toMin(cin);
		  document.querySelectorAll(".break-row").forEach(row => {
		    const bs = row.querySelector(".breakStart").value;
		    const be = row.querySelector(".breakEnd").value;
		    if (bs && be) diff -= (toMin(be) - toMin(bs));
		  });
		  if (diff < 0) diff = 0;
		  const h = Math.floor(diff/60), m = diff%60;
		  document.getElementById("workTotal").textContent = `合計: ${h}:${m.toString().padStart(2,"0")}`;
		}

      document.getElementById("clockIn").addEventListener("change", calcTotal);
      document.getElementById("clockOut").addEventListener("change", calcTotal);
      document.getElementById("breakList").addEventListener("change", (e) => {
        if (e.target.classList.contains("breakStart") || e.target.classList.contains("breakEnd")) calcTotal();
      });

   	// ★ 勤怠区分（出勤 / 有給 / 欠勤 ...）が変わった時も合計を再計算
      document.getElementById("kintaiType").addEventListener("change", calcTotal);

      // 休憩 行操作
	 function setBreakList(list) {
	  const wrap = document.getElementById("breakList");
	  wrap.innerHTML = "";
	
	  if (!list || list.length === 0) {
	    list = [{ start: "12:00", end: "13:00" }];
	  }
	
	  const norm = (val, def) => {
	    if (!val || val === "false") return def;
	    return val.length === 8 ? val.substring(0, 5) : val;
	  };
	
	  list.forEach(b => {
	    const startVal = norm(b.start, "12:00");
	    const endVal = norm(b.end, "13:00");
	
	    const div = document.createElement("div");
	    div.className = "break-row";
	    div.style.margin = "6px 0";
	
	    const inputStart = document.createElement("input");
	    inputStart.type = "time";
	    inputStart.className = "breakStart";
	    inputStart.value = startVal;
	
	    const inputEnd = document.createElement("input");
	    inputEnd.type = "time";
	    inputEnd.className = "breakEnd";
	    inputEnd.value = endVal;
	
	    const btn = document.createElement("button");
	    btn.type = "button";
	    btn.className = "removeBreak";
	    btn.textContent = "－";
	
	    div.appendChild(inputStart);
	    div.append(" 〜 ");
	    div.appendChild(inputEnd);
	    div.appendChild(btn);
	
	    wrap.appendChild(div);
	  });
	
	  // ★ 追加：休憩をセットしたら合計も再計算
	  calcTotal();
	}




	  document.getElementById("addBreak").addEventListener("click", function() {
		  const wrap = document.getElementById("breakList");

		  const div = document.createElement("div");
		  div.className = "break-row";
		  div.style.margin = "6px 0";

		  const inputStart = document.createElement("input");
		  inputStart.type = "time";
		  inputStart.className = "breakStart";
		  inputStart.value = ""; // 新規追加時は空欄

		  const inputEnd = document.createElement("input");
		  inputEnd.type = "time";
		  inputEnd.className = "breakEnd";
		  inputEnd.value = ""; // 新規追加時は空欄

		  const btn = document.createElement("button");
		  btn.type = "button";
		  btn.className = "removeBreak";
		  btn.textContent = "－";

		  div.appendChild(inputStart);
		  div.append(" 〜 ");
		  div.appendChild(inputEnd);
		  div.appendChild(btn);

		  wrap.appendChild(div);
		});

      document.getElementById("breakList").addEventListener("click", function(e) {
        if (e.target.classList.contains("removeBreak")) {
          e.target.closest(".break-row").remove();
          calcTotal();
        }
      });

      // プロジェクト 行操作
      function setProjectList(list) {
  const wrap = document.getElementById("projectList");
  wrap.innerHTML = "";

  const tmpl = document.getElementById("projectTemplate");
  if (!tmpl) {
    console.error("projectTemplate が見つかりません");
    return;
  }

  if (!list || list.length === 0) {
    return; // ← 空なら何も作らない
  }

  list.forEach(p => {
    const div = document.createElement("div");
    div.className = "project-row";
    div.style.margin = "6px 0";
    div.innerHTML = tmpl.innerHTML;

    wrap.appendChild(div);

    const sel = div.querySelector(".projectSelect");
    const hours = div.querySelector(".projectHours");
    if (sel) sel.value = String(p.projectId);
    if (hours) hours.value = p.hours;
  });
}




      document.getElementById("addProject").addEventListener("click", function() {
        const wrap = document.getElementById("projectList");
        const tmpl = document.getElementById("projectTemplate"); // ← ★ここを修正
        const div = document.createElement("div");
        div.className = "project-row";
        div.style.margin = "6px 0";
        div.innerHTML = tmpl.innerHTML;
        wrap.appendChild(div);
      });
      document.getElementById("projectList").addEventListener("click", function(e) {
        if (e.target.classList.contains("removeProject")) {
          e.target.closest(".project-row").remove();
        }
      });

   // 保存
      document.getElementById("saveKintaiBtn").addEventListener("click", function() {
    	    const empId = document.getElementById("modalEmpId").value;
    	    const date  = document.getElementById("modalDate").value;
    	    const type  = document.getElementById("kintaiType").value;

    	    // ★ kintaiType → attendanceType に統一
    	    const payload = { empId, date, attendanceType: type };

    	    if (type === "出勤") {
    	      payload.clockIn = document.getElementById("clockIn").value || null;
    	      payload.clockOut = document.getElementById("clockOut").value || null;

    	      // 休憩
    	      payload.breaks = [];
    	      document.querySelectorAll(".break-row").forEach(row => {
    	        const s = row.querySelector(".breakStart").value;
    	        const e = row.querySelector(".breakEnd").value;
    	        if (s && e) payload.breaks.push({ start: s, end: e });
    	      });

    	      // プロジェクト
    	      payload.projects = [];
    	      document.querySelectorAll(".project-row").forEach(row => {
    	        const pid = row.querySelector(".projectSelect").value;
    	        const hrs = row.querySelector(".projectHours").value;
    	        if (pid && hrs) payload.projects.push({ projectId: Number(pid), hours: Number(hrs) });
    	      });
    	    }


    	    fetch(ctx + "/kintaiData", {
    	      method: "POST",
    	      headers: { "Content-Type": "application/json", "Accept": "application/json" },
    	      body: JSON.stringify(payload)
    	    })
    	    .then(async (res) => {
    	      const ct = res.headers.get("content-type") || "";
    	      if (!ct.includes("application/json")) {
    	        const t = await res.text();
    	        throw new Error("JSON以外のレスポンス: " + t.substring(0, 200));
    	      }
    	      const data = await res.json();
    	      if (!res.ok || !data.success) throw new Error(data.message || ("HTTP " + res.status));
    	      return data;
    	    })
    	    .then(data => {
    	    	 console.log("save result:", data); // ← 保存APIの結果だけログ
    	      if (document.getElementById("continueInput").checked) {
    	        const next = addDays(date, 1);
    	        openKintaiModal(empId, next);
    	      } else {
    	        alert("保存しました");
    	        closeKintaiModal();

    	        // ★ 勤怠表を全体再描画
    	        refreshKintaiTable(empId);
    	      }
    	    })
    	    .catch(err => {
    	      console.error("保存エラー:", err);
    	      alert("保存に失敗しました");
    	    });
    	  });

    	  // 勤怠表の日付クリック → モーダル表示（イベント委譲）
    	  document.addEventListener("click", function(e) {
    	    const td = e.target.closest(".attendance-cell");
    	    if (td) {
    	      const empId = td.dataset.empid;
    	      const date  = td.dataset.date;
    	      if (!empId || !date) {
    	        console.error("empId/dateが空");
    	        return;
    	      }
    	      openKintaiModal(empId, date);
    	    }
    	  });

    	  // ユーティリティ
    	  function addDays(ymd, d) {
    	    // ローカルタイムで安定的に+1日
    	    const [y,m,dd] = ymd.split("-").map(Number);
    	    const dt = new Date(y, m-1, dd + d, 9, 0, 0); // JST朝9時固定で時差の影響を排除
    	    const yy = dt.getFullYear();
    	    const mm = String(dt.getMonth()+1).padStart(2,"0");
    	    const dd2 = String(dt.getDate()).padStart(2,"0");
    	    return `${yy}-${mm}-${dd2}`;
    	  }

    	  // ★ 勤怠表を全体再描画
    	  function refreshKintaiTable(empId) {
    		  const year  = document.getElementById("yearHidden").value;
    		  const month = document.getElementById("monthHidden").value;

    		  fetch(ctx + "/KintaiRecServlet?action=list"
    		        + "&empId=" + encodeURIComponent(empId)
    		        + "&year=" + year
    		        + "&month=" + month,
    		        { headers: { "Accept": "application/json" } })
    		    .then(res => res.json())
    		    .then(data => {
    		      const tbody = document.getElementById("kintaiTableBody");
    		      tbody.innerHTML = "";

    		      tbody.innerHTML = "";

    		      data.forEach(rec => {
    		    	  const tr = document.createElement("tr");
    		    	  tr.innerHTML =
    		    	    "<td style='text-align:center;'>" + (rec.kintaiDate || "") + "</td>" +
    		    	    "<td style='text-align:center;'>" + (rec.dayOfWeek || "") + "</td>" +
    		    	    "<td style='text-align:center;'>" + (rec.workStatus || "") + "</td>" +
    		    	    "<td style='text-align:center;'>" + (rec.eventName || "") + "</td>" +
    		    	    "<td style='text-align:center;'>" + (rec.attendanceType || "") + "</td>" +
    		    	    "<td style='text-align:center;'>" + (rec.clockIn || "") + "</td>" +
    		    	    "<td style='text-align:center;'>" + (rec.clockOut || "") + "</td>" +
    		    	    "<td style='text-align:center;'>" + (rec.totalBreakTimeFormatted || "") + "</td>" +
    		    	    "<td style='text-align:center;'>" + (rec.actualWorkTimeFormatted || rec.leaveHours || "") + "</td>" +
    		    	    "<td style='text-align:center;'>" + (rec.overtimeFormatted || "") + "</td>" +
    		    	    "<td style='text-align:center;'>" + (rec.nightovertimeFormatted || "") + "</td>" +
    		    	    "<td style='text-align:center;'>" + (rec.projectTotalHours || "") + "</td>";
    		    	  tbody.appendChild(tr);

    		      });


    		      // 従業員選択を保持
    		      const empSelect = document.getElementById("empNoFilter");
    		      if (empSelect) empSelect.value = empId;
    		    })
    		    .catch(err => console.error("勤怠表更新失敗:", err));
    		}



    		
    	})();
    	

			 



    
    
    document.addEventListener('DOMContentLoaded', function() {
        const deptSelect = document.getElementById('deptNoFilter');
        const postSelect = document.getElementById('postNoFilter');
        const empSelect = document.getElementById('empNoFilter');

        const allEmpOptions = Array.from(empSelect.querySelectorAll('option'));

        function filterEmployees() {
            const selectedDept = deptSelect.value;
            const selectedPost = postSelect.value;

            empSelect.innerHTML = '<option value="">ログインユーザー</option>';

            allEmpOptions.forEach(opt => {
                if (opt.value === "") return;

                const empDept = opt.dataset.dept;
                const empPost = opt.dataset.post;

                const matchDept = !selectedDept || empDept === selectedDept;
                const matchPost = !selectedPost || empPost === selectedPost;

                if (matchDept && matchPost) {
                    empSelect.appendChild(opt.cloneNode(true));
                }
            });
        }

        deptSelect.addEventListener('change', filterEmployees);
        postSelect.addEventListener('change', filterEmployees);

     // 🔥 従業員変更時に勤怠表を更新
        empSelect.addEventListener('change', () => {
            if (!empSelect.value) return;

            const params = new URLSearchParams({
                mode: 'admin',   // ★ 追加：管理者・部長用で従業員を切替えるモード
                empNoFilter: empSelect.value,
                deptNoFilter: deptSelect.value || '',
                postNoFilter: postSelect.value || '',
                startDate: document.getElementById('startDate')?.value || '',
                endDate: document.getElementById('endDate')?.value || ''
            });

            fetch('<%= request.getContextPath() %>/KintaiRecServlet?' + params.toString(), {
                method: 'GET'
            })
            .then(response => response.text())
            .then(html => {
                const parser = new DOMParser();
                const doc = parser.parseFromString(html, 'text/html');

                // 勤怠表部分だけ入れ替える
                const newContainer = doc.querySelector('#kintaiTableContainer');
                const currentContainer = document.querySelector('#kintaiTableContainer');
                if (newContainer && currentContainer) {
                    currentContainer.innerHTML = newContainer.innerHTML;
                }
            })
            .catch(err => console.error("勤怠表更新エラー:", err));
        });

        // 初期ロード時に一度フィルタ適用
        filterEmployees();

    });





    
    document.addEventListener('DOMContentLoaded', () => {
        const deptSelect = document.getElementById('deptNoFilter');
        const postSelect = document.getElementById('postNoFilter');
        const empSelect = document.getElementById('empNoFilter');

        // 初期状態で全オプションを保持
        const allEmpOptions = Array.from(empSelect.querySelectorAll('option'));

        function filterEmployees() {
            const selectedDept = deptSelect.value;
            const selectedPost = postSelect.value;

            // 従業員プルダウンをリセット
            empSelect.innerHTML = '<option value="">従業員</option>';

            allEmpOptions.forEach(opt => {
                if (opt.value === "") return; // 「従業員」ラベルはスキップ

                // 両方の条件を満たす場合のみ追加
                const matchDept = selectedDept === "" || opt.dataset.dept === selectedDept;
                const matchPost = selectedPost === "" || opt.dataset.post === selectedPost;

                if (matchDept && matchPost) {
                    empSelect.appendChild(opt.cloneNode(true));
                }
            });
        }

        // 部署・役職の変更イベントでフィルタ実行
        deptSelect.addEventListener('change', filterEmployees);
        postSelect.addEventListener('change', filterEmployees);

        // 初期ロード時にも適用
        filterEmployees();
    });
        // 従業員リストデータ（サーバーから取得）
        let currentEmployeeList = [];
        <% List<EmpBean> employeeList = (List<EmpBean>) request.getAttribute("employeeList"); %>
        <% if (employeeList != null && !employeeList.isEmpty()) { %>
            currentEmployeeList = [
                <% for (int i = 0; i < employeeList.size(); i++) { 
                    EmpBean emp = employeeList.get(i); %>
                {
                    empno: '<%= emp.getEmpId() %>',
                    empname: '<%= emp.getEmpName() %>',
                    deptname: '<%= emp.getDeptName() != null ? emp.getDeptName() : "" %>',
                    postname: '<%= emp.getPostName() != null ? emp.getPostName() : "" %>'
                }<%= i < employeeList.size() - 1 ? "," : "" %>
                <% } %>
            ];
        <% } %>
        
        // 報告書生成関連の変数（保持）
        let selectedFormat = 'pdf'; // デフォルトはPDF
        
     // 個人別月次報告をモーダル形式で生成
        function generateIndividualReport() {
            let empNoFilter = "";
            const empSelect = document.getElementById('empNoFilter');
            
            // 管理者・部長モードなら従業員選択チェック
            if (empSelect) {
                empNoFilter = empSelect.value;
                if (!empNoFilter || empNoFilter.trim() === '') {
                    alert('個人別月次報告を生成するには、まず対象従業員を選択してください。');
                    return;
                }
            }
            // 一般社員モードでは empNoFilter を空で送る（サーバー側でログインユーザーに置き換え）

            const startDate = document.getElementById('startDate')?.value || '';
            const endDate = document.getElementById('endDate')?.value || '';

            // パラメータを準備
            const params = new URLSearchParams({
                action: 'generateIndividual',
                empNoFilter: empNoFilter,
                startDate: startDate,
                endDate: endDate
            });

            // 報告書データを取得
            fetch('<%= request.getContextPath() %>/ReportGenerationServlet', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                },
                body: params.toString()
            })
            .then(response => {
                if (!response.ok) {
                    throw new Error('Network response was not ok');
                }
                return response.json();
            })
           .then(data => {
			    console.log("[DEBUG][generateIndividualReport] response data:", data);
			
			    if (data.error) {
			        throw new Error(data.error);
			    }
			
			    // 個人別月次報告の結果をモーダルで表示
			    showIndividualReportModalWithData(data);
			})

        
            .catch(error => {
                console.error('Error:', error);
                showKintaiTable(
                    'エラー',
                    '<div style="text-align: center; padding: 50px; color: red;">報告書の生成に失敗しました: ' + error.message + '</div>'
                );
            });
        }

        
        // 部署別集計報告を生成（モーダル形式）
    // 部署別集計報告を生成（モーダル形式）
	function generateDepartmentReport() {
	    const deptNoFilter = document.getElementById('deptNoFilter').value;
	    const startDate = document.getElementById('startDate').value;
	    const endDate = document.getElementById('endDate').value;
	    
	    const params = new URLSearchParams({
	        action: 'generateDepartment',
	        deptNoFilter: deptNoFilter || '',
	        startDate: startDate || '',
	        endDate: endDate || ''
	    });
	    
	    fetch('<%= request.getContextPath() %>/ReportGenerationServlet', {
	        method: 'POST',
	        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
	        body: params.toString()
	    })
	    .then(response => {
	        if (!response.ok) {
	            throw new Error('Network response was not ok');
	        }
	        return response.json();
	    })
	    .then(data => {
	        if (data.error) {
	            throw new Error(data.error);
	        }
	        // ✅ モーダルで表示
	        showDepartmentReportModalWithData(data);
	    })
	    .catch(error => {
	        console.error('Error:', error);
	        document.getElementById("departmentReportContent").innerHTML =
	            `<div style="text-align:center; padding:50px; color:red;">
	                部署別集計報告の生成に失敗しました:<br>${error.message}
	            </div>`;
	        document.getElementById("departmentReportModal").style.display = "block";
	    });
	}

        
        // 出力形式設定関数（保持）
        function setFormat(format) {
            selectedFormat = format;
            
            // すべてのformat-btnから選択状態を削除
            document.querySelectorAll('.format-btn').forEach(btn => {
                btn.style.backgroundColor = '#007bff';
            });
            
            // 選択されたボタンをハイライト
            event.target.style.backgroundColor = '#28a745';
        }

        // 従業員詳細情報モーダルを表示
        function showEmployeeDetail(employeeName) {
            document.getElementById('modalEmployeeName').textContent = employeeName + ' の勤怠詳細';
            document.getElementById('employeeDetailModal').style.display = 'block';
            
            // AJAX呼び出しで従業員詳細情報を取得
            loadEmployeeDetails(employeeName);
        }

        // より多くの従業員リストモーダルを表示
        function showMoreEmployees(category) {
            let title = '';
            let employees = [];
            
            switch(category) {
                case 'holiday-work':
                    title = '休日出勤者一覧';
                    // 真実のデータが必要な場合は、サーバーサイドから取得する必要があります
                    employees = [];
                    break;
                case 'absent':
                    title = '出社日欠勤者一覧';
                    employees = [];
                    break;
                case 'leave':
                    title = '休暇申請者一覧';
                    employees = [];
                    break;
            }
            
            document.getElementById('moreModalTitle').textContent = title;
            
            let html = '<div style="padding: 20px; text-align: center;">';
            if (employees.length > 0) {
                html += '<div style="display: flex; flex-wrap: wrap; gap: 10px;">';
                employees.forEach(name => {
                    html += '<span class="employee-name" onclick="showEmployeeDetail(\'' + name + '\')" style="cursor: pointer; background-color: #e9ecef; padding: 5px 10px; border-radius: 5px;">' + name + '</span>';
                });
                html += '</div>';
            } else {
                html += '<p style="color: #666; font-size: 14px;">現在、該当する従業員はいません。</p>';
                html += '<p style="color: #999; font-size: 12px;">詳細な従業員リストは今後の機能拡張で提供予定です。</p>';
            }
            html += '</div>';
            
            document.getElementById('moreEmployeeList').innerHTML = html;
            document.getElementById('moreEmployeesModal').style.display = 'block';
        }

        // モーダルを閉じる
        function closeModal() {
            document.getElementById('employeeDetailModal').style.display = 'none';
        }

        function closeMoreModal() {
            document.getElementById('moreEmployeesModal').style.display = 'none';
        }

        // モーダルの外をクリックで閉じる
        window.onclick = function(event) {
            const employeeModal = document.getElementById('employeeDetailModal');
            const moreModal = document.getElementById('moreEmployeesModal');
            const kintaiModal = document.getElementById('kintaiTableModal');
            
            if (event.target == employeeModal) {
                employeeModal.style.display = 'none';
            }
            if (event.target == moreModal) {
                moreModal.style.display = 'none';
            }
            if (event.target == kintaiModal) {
                kintaiModal.style.display = 'none';
            }
        }

        // 勤務時間一覧モーダルを閉じる
        function closeKintaiModal() {
            document.getElementById('kintaiTableModal').style.display = 'none';
        }

        // 勤務時間一覧モーダルを表示
        function showKintaiTable(title, tableHtml) {
            document.getElementById('kintaiModalTitle').textContent = title;
            document.getElementById('kintaiTableContainer').innerHTML = tableHtml;
            document.getElementById('kintaiTableModal').style.display = 'block';
        }

        // ページ内の勤務時間一覧を更新
        function updateMainKintaiTable(tableHtml, empName) {
            // 管理者モードの場合のみ更新する
            const isAdminMode = document.getElementById('adminSearchForm') !== null;
            if (!isAdminMode) {
                return; // 一般ユーザーモードでは更新しない
            }
            
            // 勤務時間一覧のタイトルを更新
            const titleElement = document.querySelector('h3[style*="📊"]');
            if (titleElement) {
                titleElement.innerHTML = '📊 ' + empName + 'の勤務時間一覧';
            }
            
            // 勤務時間一覧の表格部分を更新
            const tableContainer = document.querySelector('div[style*="max-height: 300px; overflow-y: auto;"]');
            if (tableContainer) {
                // テーブルのスタイルを調整
                let adjustedTableHtml = tableHtml;
                if (tableHtml.includes('<table')) {
                    adjustedTableHtml = tableHtml.replace(
                        /<table[^>]*>/,
                        '<table style="width: 100%; border-collapse: collapse; font-size: 11px;">'
                    );
                }
                tableContainer.innerHTML = adjustedTableHtml;
            }
        }

        // 管理者モードの検索処理
       function searchKintaiRecords(event) {
		    event.preventDefault();
		
		    const form = document.getElementById('adminSearchForm');
		    const formData = new FormData(form);
		    const params = new URLSearchParams(formData);
		
		    fetch('<%= request.getContextPath() %>/KintaiRecServlet?' + params.toString())
		        .then(response => response.text())
		        .then(html => {
		            const parser = new DOMParser();
		            const doc = parser.parseFromString(html, 'text/html');
		            const table = doc.querySelector('.kintai-table');
		
		            if (table) {
		                let title = '勤務時間一覧';
		                const empSelect = document.getElementById('empNoFilter');
		                if (empSelect.value && empSelect.selectedOptions[0]) {
		                    title += ' - ' + empSelect.selectedOptions[0].text;
		                }
		                showKintaiTable(title, table.outerHTML);
		                updateMainKintaiTable(table.outerHTML, empSelect.selectedOptions[0] ? empSelect.selectedOptions[0].text : '選択された従業員');
		            } else {
		                showKintaiTable('勤務時間一覧', '<p style="text-align: center; padding: 50px;">検索結果がありません。</p>');
		                updateMainKintaiTable('<p style="text-align: center; padding: 50px;">検索結果がありません。</p>', '検索結果');
		            }
		        })
		        .catch(error => {
		            console.error('Error:', error);
		            showKintaiTable('エラー', '<p style="text-align: center; padding: 50px; color: red;">データの取得に失敗しました。</p>');
		            updateMainKintaiTable('<p style="text-align: center; padding: 50px; color: red;">データの取得に失敗しました。</p>', 'エラー');
		        });
		}
        
        // レスポンスから従業員リストデータを更新
        function updateEmployeeListFromResponse(html) {
            // HTMLレスポンスから従業員リストのJavaScriptデータを抽出
            const parser = new DOMParser();
            const doc = parser.parseFromString(html, 'text/html');
            const scripts = doc.querySelectorAll('script');
            
            scripts.forEach(script => {
                const scriptContent = script.textContent || script.innerText;
                if (scriptContent.includes('currentEmployeeList')) {
                    // JavaScriptコードを実行して従業員リストを更新
                    try {
                        eval(scriptContent);
                    } catch (e) {
                        console.error('Failed to update employee list:', e);
                    }
                }
            });
        }
        
        // 従業員一覧モーダルを表示
        function showEmployeeListModal(html) {
            let employeeListHtml = '<div style="display: grid; grid-template-columns: repeat(auto-fill, minmax(250px, 1fr)); gap: 15px; padding: 20px;">';
            
            // サーバーから取得した従業員リストを使用
            if (currentEmployeeList && currentEmployeeList.length > 0) {
                currentEmployeeList.forEach(emp => {
                    employeeListHtml += '<div class="employee-card" style="border: 1px solid #ddd; border-radius: 8px; padding: 15px; background: white; cursor: pointer; transition: all 0.2s; box-shadow: 0 2px 4px rgba(0,0,0,0.1);" onclick="selectEmployee(\'' + emp.empno + '\', \'' + emp.empname + '\')" onmouseover="this.style.transform=\'translateY(-2px)\'; this.style.boxShadow=\'0 4px 8px rgba(0,0,0,0.15)\';" onmouseout="this.style.transform=\'translateY(0)\'; this.style.boxShadow=\'0 2px 4px rgba(0,0,0,0.1)\';">';
                    employeeListHtml += '<h4 style="margin: 0 0 8px 0; color: #333; font-size: 16px;">' + emp.empname + '</h4>';
                    employeeListHtml += '<p style="margin: 4px 0; color: #666; font-size: 13px;"><strong>従業員番号:</strong> ' + emp.empno + '</p>';
                    if (emp.deptname) {
                        employeeListHtml += '<p style="margin: 4px 0; color: #666; font-size: 13px;"><strong>部署:</strong> ' + emp.deptname + '</p>';
                    }
                    if (emp.postname) {
                        employeeListHtml += '<p style="margin: 4px 0; color: #666; font-size: 13px;"><strong>役職:</strong> ' + emp.postname + '</p>';
                    }
                    employeeListHtml += '<div style="text-align: center; margin-top: 10px; color: #007bff; font-size: 12px;">クリックして勤怠記録を表示</div>';
                    employeeListHtml += '</div>';
                });
            } else {
                employeeListHtml += '<div style="grid-column: 1 / -1; text-align: center; padding: 50px; color: #666;">検索条件に一致する従業員が見つかりませんでした。</div>';
            }
            
            employeeListHtml += '</div>';
            
            let title = '従業員一覧';
            const deptSelect = document.getElementById('deptNoFilter');
            const postSelect = document.getElementById('postNoFilter');
            if (deptSelect.value && deptSelect.selectedOptions[0]) {
                title += ' - ' + deptSelect.selectedOptions[0].text;
            }
            if (postSelect.value && postSelect.selectedOptions[0]) {
                title += (deptSelect.value ? ' / ' : ' - ') + postSelect.selectedOptions[0].text;
            }
            title += ' (クリックして詳細を表示)';
            
            showKintaiTable(title, employeeListHtml);
        }
        
        // 従業員を選択した時の処理
        function selectEmployee(empno, empname) {
            // 選択された従業員の勤怠記録を取得
            const params = new URLSearchParams({
                empNoFilter: empno,
                startDate: document.getElementById('startDate').value || '',
                endDate: document.getElementById('endDate').value || ''
            });
            
            fetch('<%= request.getContextPath() %>/KintaiRecServlet?' + params.toString())
                .then(response => response.text())
                .then(html => {
                    const parser = new DOMParser();
                    const doc = parser.parseFromString(html, 'text/html');
                    const table = doc.querySelector('.kintai-table');
                    
                    if (table) {
                        showKintaiTable('勤務時間一覧 - ' + empname, table.outerHTML);
                    } else {
                        showKintaiTable('勤務時間一覧 - ' + empname, '<p style="text-align: center; padding: 50px;">この従業員の勤怠記録がありません</p>');
                    }
                })
                .catch(error => {
                    console.error('Error:', error);
                    showKintaiTable('エラー', '<p style="text-align: center; padding: 50px; color: red;">データの取得に失敗しました。</p>');
                });
        }

        // 自分モードの検索処理  
        function searchSelfKintaiRecords(event) {
            event.preventDefault();
            
            const form = document.getElementById('selfSearchForm');
            const formData = new FormData(form);
            const params = new URLSearchParams(formData);
            
            // AJAX リクエストでデータを取得
            fetch('<%= request.getContextPath() %>/KintaiRecServlet?' + params.toString())
                .then(response => response.text())
                .then(html => {
                    // レスポンスから表格部分を抽出して表示
                    const parser = new DOMParser();
                    const doc = parser.parseFromString(html, 'text/html');
                    const table = doc.querySelector('.kintai-table');
                    
                    if (table) {
                        showKintaiTable('自分の勤務時間一覧', table.outerHTML);
                    } else {
                        showKintaiTable('自分の勤務時間一覧', '<p style="text-align: center; padding: 50px;">検索結果がありません。</p>');
                    }
                })
                .catch(error => {
                    console.error('Error:', error);
                    showKintaiTable('エラー', '<p style="text-align: center; padding: 50px; color: red;">データの取得に失敗しました。</p>');
                });
        }

        function showCheckItems() {
            let checkItemsHtml = `
                <div style="padding: 15px;">
                    <h3 style="margin-bottom: 12px; color: #495057; font-size: 14px;">📋 チェック項目一覧</h3>
                    <div style="background-color: #f8f9fa; border: 1px solid #dee2e6; border-radius: 8px; padding: 12px;">
                        <div style="font-size: 11px; color: #495057; line-height: 1.6;">
                            <strong>以下の項目について法令や会社規則遵守をチェックしています：</strong><br><br>
                            • <strong>休憩時間の適切性</strong> - 6-8時間勤務時に45分以上、8時間超勤務時に60分以上の休憩を取得しているか<br>
                            • <strong>深夜勤務の確認</strong> - 22:00～翌5:00の深夜時間帯での勤務状況<br>
                            • <strong>連続勤務日数の確認</strong> - 10日以内の連続勤務制限を遵守しているか<br>
                            • <strong>2週間時間外労働の確認</strong> - 2週間で40時間を超過し80時間超過防止アラート<br>
                        </div>
                    </div>
                </div>
            `;

            // ✅ モーダルに差し込んで表示
            document.getElementById("checkItemsContent").innerHTML = checkItemsHtml;
            document.getElementById("checkItemsModal").style.display = "block";
        }



        // 法令遵守違反者の詳細表示
        function showViolationEmployeeDetail(employeeName) {
            // 全員一覧ウィンドウが開いている場合は閉じる
            const moreModal = document.getElementById('moreEmployeesModal');
            if (moreModal && moreModal.style.display === 'block') {
                moreModal.style.display = 'none';
            }
            
            // 違反詳細情報をモーダル形式で表示
            document.getElementById('modalEmployeeName').textContent = employeeName + ' の法令及び会社規則遵守違反詳細';
            document.getElementById('employeeDetailModal').style.display = 'block';
            loadViolationEmployeeDetails(employeeName);
        }

        // すべての違反者を表示するモーダル
        function showAllViolationEmployees() {
            let violationList = [
                <% if (violationEmployees != null && !violationEmployees.isEmpty()) { %>
                    <% for (int i = 0; i < violationEmployees.size(); i++) { %>
                        '<%= violationEmployees.get(i) %>'<%= i < violationEmployees.size() - 1 ? "," : "" %>
                    <% } %>
                <% } %>
            ];
            
            document.getElementById('moreModalTitle').textContent = '今月の要確認者一覧（全 ' + violationList.length + ' 名）';
            
            let html = '<div style="background-color: #f8f9fa; border: 1px solid #dee2e6; border-radius: 8px; padding: 15px; margin-bottom: 15px;">';
            html += '<h4 style="margin: 0 0 10px 0; color: #495057;">チェック項目:</h4>';
            html += '<div style="font-size: 12px; color: #6c757d; line-height: 1.5;">';
            html += '• <strong>休憩時間の適切性</strong> - 6-8時間勤務時に45分以上、8時間超勤務時に60分以上の休憩を取得しているか<br>';
            html += '• <strong>深夜勤務の確認</strong> - 22:00～翌5:00の深夜時間帯での勤務状況<br>';
            html += '• <strong>連続勤務日数の確認</strong> - 10日以内の連続勤務制限を遵守しているか<br>';
            html += '• <strong>2週間時間外労働の確認</strong> - 2週間で40時間を超過し80時間超過防止アラート<br>';
            //html += '• <strong>遅刻の確認</strong> - 始業時刻9:00からの遅刻状況';
            html += '</div>';
            html += '</div>';
            
            html += '<div style="display: grid; grid-template-columns: repeat(auto-fill, minmax(200px, 1fr)); gap: 10px;">';
            violationList.forEach(name => {
                html += '<span class="employee-name" onclick="showViolationEmployeeDetail(\'' + name + '\')" ';
                html += 'style="cursor: pointer; background-color: #f8d7da; color: #721c24; padding: 8px 12px; border-radius: 5px; text-align: center; transition: background-color 0.2s; border: 1px solid #f5c6cb;">';
                html += name + '</span>';
            });
            html += '</div>';
            
            if (violationList.length === 0) {
                html = '<div style="text-align: center; padding: 50px; color: #155724; background-color: #d4edda; border: 1px solid #c3e6cb; border-radius: 8px;">✅ 今月は要確認者はいません</div>';
            }
            
            document.getElementById('moreEmployeeList').innerHTML = html;
            document.getElementById('moreEmployeesModal').style.display = 'block';
        }

        // 違反者の詳細情報を読み込む
        function loadViolationEmployeeDetails(employeeName) {
            // サーバーから取得した真実のデータを使用
            const violationDetailsMap = {
                <% if (violationDetails != null && !violationDetails.isEmpty()) { %>
                    <% for (java.util.Map.Entry<String, ComplianceCheckResult> entry : violationDetails.entrySet()) { 
                        String empName = entry.getKey();
                        ComplianceCheckResult detail = entry.getValue();
                    %>
                        '<%= empName %>': {
                            empno: '<%= detail.getEmpno() != null ? detail.getEmpno() : "不明" %>',
                            dept: '<%= detail.getDeptName() != null ? detail.getDeptName() : "不明" %>',
                            post: '<%= detail.getPostName() != null ? detail.getPostName() : "不明" %>',
                            violations: [
                                <% if (detail.getViolations() != null && !detail.getViolations().isEmpty()) { %>
                                    <% for (int i = 0; i < detail.getViolations().size(); i++) { 
                                        ComplianceViolation violation = detail.getViolations().get(i);
                                    %>
                                        {
                                            type: '<%= violation.getViolationType() != null ? violation.getViolationType().replace("'", "\\'") : "不明" %>',
                                            severity: '<%= violation.getSeverity() != null ? violation.getSeverity() : "低" %>',
                                            description: '<%= violation.getDescription() != null ? violation.getDescription().replace("'", "\\'").replace("\n", " ") : "詳細情報なし" %>',
                                            date: '<%= violation.getDate() != null ? violation.getDate().toString() : "不明" %>'
                                        }<%= i < detail.getViolations().size() - 1 ? "," : "" %>
                                    <% } %>
                                <% } %>
                            ]
                        },
                    <% } %>
                <% } %>
            };
            
            const employee = violationDetailsMap[employeeName] || {
                empno: '不明',
                dept: '不明',
                post: '不明',
                violations: [
                    {type: '情報不足', severity: '低', description: '詳細な違反情報が取得できませんでした', date: new Date().toISOString().split('T')[0]}
                ]
            };
            
            let html = '<div style="margin-bottom: 20px;">';
            html += '<p><strong>従業員番号:</strong> ' + employee.empno + '</p>';
            html += '<p><strong>部署:</strong> ' + employee.dept + '</p>';
            html += '<p><strong>役職:</strong> ' + employee.post + '</p>';
            html += '</div>';
            
            html += '<h3>法令及び会社規則遵守違反詳細</h3>';
            html += '<table style="width: 100%; border-collapse: collapse; margin-bottom: 20px;">';
            html += '<tr style="background-color: #f8f9fa;"><th style="border: 1px solid #dee2e6; padding: 8px;">違反項目</th><th style="border: 1px solid #dee2e6; padding: 8px;">詳細</th><th style="border: 1px solid #dee2e6; padding: 8px;">日付</th></tr>';
            
            employee.violations.forEach(violation => {
                html += '<tr>';
                html += '<td style="border: 1px solid #dee2e6; padding: 8px; text-align: center; font-weight: bold;">' + violation.type + '</td>';
                html += '<td style="border: 1px solid #dee2e6; padding: 8px;">' + violation.description + '</td>';
                html += '<td style="border: 1px solid #dee2e6; padding: 8px; text-align: center;">' + violation.date + '</td>';
                html += '</tr>';
            });
            
            html += '</table>';
            
            // チェック項目の説明
            html += '<div style="background-color: #e9ecef; border: 1px solid #dee2e6; border-radius: 6px; padding: 12px;">';
            html += '<h4 style="margin: 0 0 8px 0;">チェック項目:</h4>';
            html += '<div style="font-size: 12px; color: #495057; line-height: 1.6;">';
            html += '• 休憩時間の適切性（6-8時間勤務で45分以上、8時間超で60分以上）<br>';
            html += '• 深夜勤務の確認（22:00～翌5:00）<br>';
            html += '• 連続勤務日数の確認（10日以内）<br>';
            html += '• 2週間時間外労働の確認（40時間超過で80時間超過防止アラート）<br>';
            //html += '• 遅刻の確認（始業時刻9:00から）';
            html += '</div>';
            html += '</div>';
            
            document.getElementById('modalEmployeeDetails').innerHTML = html;
        }

        // 従業員詳細情報を読み込む
        function loadEmployeeDetails(employeeName) {
            // 従業員詳細情報を模擬的に読み込む
            const mockData = {
                '田中太郎': {
                    empno: 'E001',
                    dept: '営業部',
                    post: '主任',
                    thisMonth: [
                        {date: '2024-12-01', clockIn: '09:00', clockOut: '18:30', break: '1:00', work: '8:30', overtime: '0:30'},
                        {date: '2024-12-02', clockIn: '08:45', clockOut: '19:00', break: '1:15', work: '9:00', overtime: '1:00'},
                        {date: '2024-12-03', clockIn: '09:15', clockOut: '17:45', break: '0:45', work: '7:15', overtime: '0:00'}
                    ]
                }
            };
            
            const employee = mockData[employeeName] || {
                empno: 'E999',
                dept: '不明',
                post: '不明',
                thisMonth: []
            };
            
            let html = '<div style="margin-bottom: 20px;">';
            html += '<p><strong>従業員番号:</strong> ' + employee.empno + '</p>';
            html += '<p><strong>部署:</strong> ' + employee.dept + '</p>';
            html += '<p><strong>役職:</strong> ' + employee.post + '</p>';
            html += '</div>';
            
            html += '<h3>今月の勤怠記録</h3>';
            html += '<table style="width: 100%; border-collapse: collapse;">';
            html += '<tr style="background-color: #f8f9fa;"><th style="border: 1px solid #dee2e6; padding: 8px;">日付</th><th style="border: 1px solid #dee2e6; padding: 8px;">出勤</th><th style="border: 1px solid #dee2e6; padding: 8px;">退勤</th><th style="border: 1px solid #dee2e6; padding: 8px;">休憩</th><th style="border: 1px solid #dee2e6; padding: 8px;">実働</th><th style="border: 1px solid #dee2e6; padding: 8px;">残業</th></tr>';
            
            if (employee.thisMonth.length > 0) {
                employee.thisMonth.forEach(record => {
                    html += '<tr>';
                    html += '<td style="border: 1px solid #dee2e6; padding: 8px; text-align: center;">' + record.date + '</td>';
                    html += '<td style="border: 1px solid #dee2e6; padding: 8px; text-align: center;">' + record.clockIn + '</td>';
                    html += '<td style="border: 1px solid #dee2e6; padding: 8px; text-align: center;">' + record.clockOut + '</td>';
                    html += '<td style="border: 1px solid #dee2e6; padding: 8px; text-align: center;">' + record.break + '</td>';
                    html += '<td style="border: 1px solid #dee2e6; padding: 8px; text-align: center;">' + record.work + '</td>';
                    html += '<td style="border: 1px solid #dee2e6; padding: 8px; text-align: center;">' + record.overtime + '</td>';
                    html += '</tr>';
                });
            } else {
                html += '<tr><td colspan="6" style="border: 1px solid #dee2e6; padding: 20px; text-align: center;">勤怠記録がありません</td></tr>';
            }
            
            html += '</table>';
            
            document.getElementById('modalEmployeeDetails').innerHTML = html;
        }

        // 合規チェック実行関数
        function performComplianceCheck(checkType) {
            // 現在選択されているフィルター条件を取得
            const empNoFilter = document.getElementById('empNoFilter').value;
            const deptNoFilter = document.getElementById('deptNoFilter').value;
            const postNoFilter = document.getElementById('postNoFilter').value;
            const startDate = document.getElementById('startDate').value;
            const endDate = document.getElementById('endDate').value;
            
            // チェック対象の従業員が選択されているかチェック
            if (!empNoFilter || empNoFilter.trim() === '') {
                alert('合規チェックを実行するには、まず対象従業員を選択してください。');
                return;
            }
            
            // パラメータを準備
            const params = new URLSearchParams({
                action: 'legalCheck', // 法令遵守チェックのみ
                empNoFilter: empNoFilter,
                startDate: startDate || '',
                endDate: endDate || ''
            });
            
            // 合規チェックを実行
            fetch('<%= request.getContextPath() %>/ComplianceCheckServlet', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                },
                body: params.toString()
            })
            .then(response => response.text())
            .then(html => {
                // 結果を解析してモーダルで表示
                const parser = new DOMParser();
                const doc = parser.parseFromString(html, 'text/html');
                
                // 合規チェック結果を抽出（実際の実装に応じて調整が必要）
                let resultHtml = '<div style="padding: 15px;">';
                resultHtml += '<h3 style="font-size: 14px; margin-bottom: 10px;">📋 法令遵守チェック結果</h3>';
                resultHtml += '<p style="font-size: 11px;">対象従業員: ' + document.getElementById('empNoFilter').selectedOptions[0].text + '</p>';
                resultHtml += '<div style="margin-top: 12px;">';
                resultHtml += '<div style="background-color: #d4edda; border: 1px solid #c3e6cb; border-radius: 4px; padding: 8px; margin-bottom: 8px; font-size: 11px;">';
                resultHtml += '<strong>✅ チェック完了</strong><br>';
                resultHtml += '労働基準法および会社規程に基づく合規性をチェックしました。';
                resultHtml += '</div>';
                resultHtml += '<div style="background-color: #f8f9fa; border: 1px solid #dee2e6; border-radius: 4px; padding: 8px; font-size: 11px;">';
                resultHtml += '<strong>チェック項目:</strong><br>';
                resultHtml += '• 休憩時間の適切性（6-8時間勤務で45分以上、8時間超で60分以上）<br>';
                resultHtml += '• 深夜勤務の確認（22:00～翌5:00）<br>';
                resultHtml += '• 連続勤務日数の確認（10日以内）<br>';
                resultHtml += '• 2週間時間外労働の確認（40時間超過で80時間超過防止アラート）<br>';
                //resultHtml += '• 遅刻の確認（始業時刻9:00から）';
                resultHtml += '</div>';
                resultHtml += '</div>';
                resultHtml += '</div>';
                
                showKintaiTable('法令遵守チェック結果', resultHtml);
            })
            .catch(error => {
                console.error('Error:', error);
                showKintaiTable('エラー', '<div style="text-align: center; padding: 50px; color: red;">合規チェックの実行に失敗しました。</div>');
            });
        }

     	// 小数時間 → HH:MM 形式に変換
        function hoursToHHMM(hours) {
            const h = Math.floor(hours);
            const m = Math.round((hours - h) * 60);
            return h + ':' + (m < 10 ? '0' + m : m);
        }

        

        
        // 個人別月次報告モーダルを表示（真実データ版）
        function showIndividualReportModalWithData(data) {
        	console.log("[DEBUG] showIndividualReportModalWithData called, data=", data);
        	window.currentReportData = data; // CSV出力用に保持
            const empSelect = document.getElementById('empNoFilter');
            const empName = empSelect.selectedOptions[0] ? empSelect.selectedOptions[0].text : data.empName;
            
            let reportHtml = '<div style="padding: 15px;">';
            reportHtml += '<div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 15px; border-bottom: 2px solid #007bff; padding-bottom: 8px;">';
            reportHtml += '<h3 style="font-size: 14px; margin: 0;">📋 個人別月次報告</h3>';
            reportHtml += '<div style="display: flex; gap: 10px;">';
            reportHtml += '<button class="format-btn" onclick="downloadIndividualReport(\'csv\')" style="background-color: #ffc107;">📄 CSV出力</button>'; // 追加
            reportHtml += '<button class="format-btn" onclick="downloadIndividualReport(\'excel\')" style="background-color: #28a745;">📊 Excel出力</button>';
            reportHtml += '</div>';
            reportHtml += '</div>';
            
            reportHtml += '<div style="background-color: #f8f9fa; border: 1px solid #dee2e6; border-radius: 8px; padding: 12px; margin-bottom: 15px;">';
            reportHtml += '<h4 style="margin: 0 0 8px 0; color: #495057; font-size: 13px;">対象従業員情報</h4>';
            reportHtml += '<div style="display: grid; grid-template-columns: 1fr 1fr 1fr 1fr; gap: 15px; margin-bottom: 8px;">';
            reportHtml += '<p style="margin: 0; font-size: 11px;"><strong>従業員番号:</strong> ' + data.empno + '</p>';
            reportHtml += '<p style="margin: 0; font-size: 11px;"><strong>従業員名:</strong> ' + data.empName + '</p>';
            reportHtml += '<p style="margin: 0; font-size: 11px;"><strong>部署:</strong> ' + data.deptName + '</p>';
            reportHtml += '<p style="margin: 0; font-size: 11px;"><strong>役職:</strong> ' + data.postName + '</p>';
            reportHtml += '</div>';
            reportHtml += '<div style="display: grid; grid-template-columns: 1fr 1fr; gap: 15px;">';
            reportHtml += '<p style="margin: 0; font-size: 11px;"><strong>対象期間:</strong> ' + data.targetMonth + '</p>';
            reportHtml += '<p style="margin: 0; font-size: 11px;"><strong>生成日時:</strong> ' + new Date().toLocaleString('ja-JP') + '</p>';
            reportHtml += '</div>';
            reportHtml += '</div>';
            
            // 月次統計サマリー（弹跳窗口用）
            reportHtml += '<div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 15px; margin-bottom: 20px;">';
            
            reportHtml += '<div style="background-color: #e8f5e8; border: 1px solid #c3e6cb; border-radius: 8px; padding: 15px; text-align: center;">';
            reportHtml += '<h5 style="margin: 0 0 10px 0; color: #155724;">出勤日数</h5>';
            reportHtml += '<div style="font-size: 24px; font-weight: bold; color: #155724;">' + data.attendanceRate + '</div>';
            reportHtml += '<div style="font-size: 12px; color: #666;">実際の出勤状況</div>';
            reportHtml += '</div>';
            
            reportHtml += '<div style="background-color: #fff3cd; border: 1px solid #ffeeba; border-radius: 8px; padding: 15px; text-align: center;">';
            reportHtml += '<h5 style="margin: 0 0 10px 0; color: #856404;">総稼働時間</h5>';
            reportHtml += '<div style="font-size: 24px; font-weight: bold; color: #856404;">' + data.totalWorkingHours + '</div>';
            reportHtml += '<div style="font-size: 12px; color: #666;">実際の労働時間</div>';
            reportHtml += '</div>';
            
            reportHtml += '<div style="background-color: #f8d7da; border: 1px solid #f5c6cb; border-radius: 8px; padding: 15px; text-align: center;">';
            reportHtml += '<h5 style="margin: 0 0 10px 0; color: #721c24;">残業時間</h5>';
            reportHtml += '<div style="font-size: 24px; font-weight: bold; color: #721c24;">' + data.totalOvertimeHours + '</div>';
            reportHtml += '<div style="font-size: 12px; color: #666;">（45h 上限）</div>';
            reportHtml += '</div>';
            
            reportHtml += '<div style="background-color: #d1ecf1; border: 1px solid #bee5eb; border-radius: 8px; padding: 15px; text-align: center;">';
            reportHtml += '<h5 style="margin: 0 0 10px 0; color: #0c5460;">休憩時間</h5>';
            reportHtml += '<div style="font-size: 24px; font-weight: bold; color: #0c5460;">' + data.totalBreakHours + '</div>';
            reportHtml += '<div style="font-size: 12px; color: #666;">実際の休憩時間</div>';
            reportHtml += '</div>';
            
            reportHtml += '</div>';
            
            // 勤怠詳細テーブル（真実データ）
            reportHtml += '<div style="background-color: white; border: 1px solid #dee2e6; border-radius: 8px; overflow: hidden;">';
            reportHtml += '<h4 style="background-color: #f8f9fa; margin: 0; padding: 15px; border-bottom: 1px solid #dee2e6;">勤怠詳細記録</h4>';
            reportHtml += '<div style="overflow-x: auto; max-height: 300px;">';
            reportHtml += '<table style="width: 100%; border-collapse: collapse; font-size: 12px;">';
            reportHtml += '<thead style="background-color: #f8f9fa; position: sticky; top: 0;">';
            reportHtml += '<tr>';
            reportHtml += '<th style="border: 1px solid #dee2e6; padding: 8px;">日付</th>';
            reportHtml += '<th style="border: 1px solid #dee2e6; padding: 8px;">曜日</th>';
            reportHtml += '<th style="border: 1px solid #dee2e6; padding: 8px;">出勤</th>';
            reportHtml += '<th style="border: 1px solid #dee2e6; padding: 8px;">退勤</th>';
            reportHtml += '<th style="border: 1px solid #dee2e6; padding: 8px;">休憩</th>';
            reportHtml += '<th style="border: 1px solid #dee2e6; padding: 8px;">実働</th>';
            reportHtml += '<th style="border: 1px solid #dee2e6; padding: 8px;">残業</th>';
            reportHtml += '<th style="border: 1px solid #dee2e6; padding: 8px;">深夜残業</th>';		//深夜残業追加
            reportHtml += '<th style="border: 1px solid #dee2e6; padding: 8px;">プロジェクト状況</th>';		//追加
            reportHtml += '</tr>';
            reportHtml += '</thead>';
            reportHtml += '<tbody>';
            
            // 真実のデータベースデータを使用
            if (data.records && data.records.length > 0) {
            	// ★ デバッグ追加
                console.log("[DEBUG] 個人別月次報告 records:", data.records);
                data.records.forEach(record => {
                    let rowClass = '';
                    if (record.dayOfWeek === '土' || record.dayOfWeek === '日') {
                        rowClass = 'style="background-color: #f8f9fa;"';
                    }
                    
                    reportHtml += '<tr ' + rowClass + '>';
                    reportHtml += '<td style="border: 1px solid #dee2e6; padding: 8px; text-align: center;">' + record.date + '</td>';
                    reportHtml += '<td style="border: 1px solid #dee2e6; padding: 8px; text-align: center;">' + record.dayOfWeek + '</td>';
                    reportHtml += '<td style="border: 1px solid #dee2e6; padding: 8px; text-align: center;">' + record.clockIn + '</td>';
                    reportHtml += '<td style="border: 1px solid #dee2e6; padding: 8px; text-align: center;">' + record.clockOut + '</td>';
                    reportHtml += '<td style="border: 1px solid #dee2e6; padding: 8px; text-align: center;">' + record.breakTime + '</td>';
                    reportHtml += '<td style="border: 1px solid #dee2e6; padding: 8px; text-align: center;">' + record.workTime + '</td>';
                    reportHtml += '<td style="border: 1px solid #dee2e6; padding: 8px; text-align: center;">' + record.overtimeTime + '</td>';
                    reportHtml += '<td style="border: 1px solid #dee2e6; padding: 8px; text-align: center;">' + record.nightovertimeTime + '</td>';
                 	// プロジェクト合計時間 + 詳細ボタン列
                    let totalWorkHours = 0;
                    if (record.projects && record.projects.length > 0) {
                        record.projects.forEach(p => {
                            totalWorkHours += p.workHours;
                        });
                    }

                    const totalHHMM = hoursToHHMM(totalWorkHours);

                    reportHtml += '<td style="border: 1px solid #dee2e6; padding: 8px; text-align: center;">'
                                + totalHHMM
                                + ' <button class="btn-detail" data-date="' + record.date + '" data-empid="' + data.empno + '">詳細</button>'
                                + '</td>';

                    reportHtml += '</tr>';
                });
            } else {
                reportHtml += '<tr><td colspan="9" style="border: 1px solid #dee2e6; padding: 20px; text-align: center; color: #666;">指定期間内に勤怠記録がありません</td></tr>';
            }
            
            reportHtml += '</tbody>';
            reportHtml += '</table>';
            reportHtml += '</div>';
            reportHtml += '</div>';
            reportHtml += '</div>';
            
         	// 勤怠表を上書きせず、モーダルに流し込む
            document.getElementById("kintaiModalTitle").textContent = "個人別月次報告 - " + empName;
            document.getElementById("kintaiModalContainer").innerHTML = reportHtml; // ← モーダル専用
            document.getElementById("kintaiTableModal").style.display = "block";

        }

                
        
        // 部署別集計報告モーダルを表示（真実データ版）
        function showDepartmentReportModalWithData(data) {
            const deptSelect = document.getElementById('deptNoFilter');
            const deptName = deptSelect.selectedOptions[0] ? deptSelect.selectedOptions[0].text : data.targetDeptName;
            
            let reportHtml = '<div style="padding: 15px;">';
            reportHtml += '<div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 15px; border-bottom: 2px solid #007bff; padding-bottom: 8px;">';
            reportHtml += '<h3 style="font-size: 14px; margin: 0;">📊 部署別集計報告</h3>';
            reportHtml += '<div style="display: flex; gap: 10px;">';
            reportHtml += '<button class="format-btn" onclick="downloadDepartmentReport(\'excel\')" style="background-color: #28a745;">📊 Excel出力</button>';
            reportHtml += '</div>';
            reportHtml += '</div>';
            
            reportHtml += '<div style="background-color: #f8f9fa; border: 1px solid #dee2e6; border-radius: 8px; padding: 12px; margin-bottom: 15px;">';
            reportHtml += '<h4 style="margin: 0 0 8px 0; color: #495057; font-size: 13px;">対象部署情報</h4>';
            reportHtml += '<div style="display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 15px; margin-bottom: 8px;">';
            reportHtml += '<p style="margin: 0; font-size: 11px;"><strong>対象部署:</strong> ' + deptName + '</p>';
            reportHtml += '<p style="margin: 0; font-size: 11px;"><strong>総記録数:</strong> ' + data.totalRecords + '件</p>';
            reportHtml += '<p style="margin: 0; font-size: 11px;"><strong>対象従業員数:</strong> ' + data.totalEmployees + '名</p>';
            reportHtml += '</div>';
            reportHtml += '<div style="display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 15px;">';
            reportHtml += '<p style="margin: 0; font-size: 11px;"><strong>対象期間:</strong> ' + data.targetPeriod + '</p>';
            reportHtml += '<p style="margin: 0; font-size: 11px;"><strong>生成日時:</strong> ' + new Date().toLocaleString('ja-JP') + '</p>';
            reportHtml += '<p style="margin: 0; font-size: 11px;"></p>'; // 空の要素で3列目を埋める
            reportHtml += '</div>';
            reportHtml += '</div>';
            
            // 部署別統計サマリー
            if (data.departments && data.departments.length > 0) {
                reportHtml += '<div style="background-color: white; border: 1px solid #dee2e6; border-radius: 8px; overflow: hidden; margin-bottom: 20px;">';
                reportHtml += '<h4 style="background-color: #f8f9fa; margin: 0; padding: 15px; border-bottom: 1px solid #dee2e6;">部署別統計サマリー</h4>';
                reportHtml += '<div style="overflow-x: auto;">';
                reportHtml += '<table style="width: 100%; border-collapse: collapse; font-size: 12px;">';
                reportHtml += '<thead style="background-color: #f8f9fa;">';
                reportHtml += '<tr>';
                reportHtml += '<th style="border: 1px solid #dee2e6; padding: 10px;">部署名</th>';
                reportHtml += '<th style="border: 1px solid #dee2e6; padding: 10px;">従業員数</th>';
                reportHtml += '<th style="border: 1px solid #dee2e6; padding: 10px;">記録数</th>';
                reportHtml += '<th style="border: 1px solid #dee2e6; padding: 10px;">総労働時間</th>';
                reportHtml += '<th style="border: 1px solid #dee2e6; padding: 10px;">総残業時間</th>';
                reportHtml += '<th style="border: 1px solid #dee2e6; padding: 10px;">平均残業時間</th>';
                reportHtml += '</tr>';
                reportHtml += '</thead>';
                reportHtml += '<tbody>';
                
                data.departments.forEach(dept => {
                    reportHtml += '<tr>';
                    reportHtml += '<td style="border: 1px solid #dee2e6; padding: 8px; text-align: center; font-weight: bold;">' + dept.deptName + '</td>';
                    reportHtml += '<td style="border: 1px solid #dee2e6; padding: 8px; text-align: center;">' + dept.employeeCount + '名</td>';
                    reportHtml += '<td style="border: 1px solid #dee2e6; padding: 8px; text-align: center;">' + dept.recordCount + '件</td>';
                    reportHtml += '<td style="border: 1px solid #dee2e6; padding: 8px; text-align: center;">' + dept.totalWorkHours + '時間</td>';
                    reportHtml += '<td style="border: 1px solid #dee2e6; padding: 8px; text-align: center;">' + dept.totalOvertimeHours + '時間</td>';
                    reportHtml += '<td style="border: 1px solid #dee2e6; padding: 8px; text-align: center;">' + dept.avgOvertimeHours + '時間</td>';
                    reportHtml += '</tr>';
                });
                
                reportHtml += '</tbody>';
                reportHtml += '</table>';
                reportHtml += '</div>';
                reportHtml += '</div>';
            }
            
            // 詳細記録テーブル（真実データ）
            reportHtml += '<div style="background-color: white; border: 1px solid #dee2e6; border-radius: 8px; overflow: hidden;">';
            reportHtml += '<h4 style="background-color: #f8f9fa; margin: 0; padding: 15px; border-bottom: 1px solid #dee2e6;">詳細記録（データベースより - 最新50件）</h4>';
            reportHtml += '<div style="overflow-x: auto; max-height: 400px;">';
            reportHtml += '<table style="width: 100%; border-collapse: collapse; font-size: 12px;">';
            reportHtml += '<thead style="background-color: #f8f9fa; position: sticky; top: 0;">';
            reportHtml += '<tr>';
            reportHtml += '<th style="border: 1px solid #dee2e6; padding: 8px;">日付</th>';
            reportHtml += '<th style="border: 1px solid #dee2e6; padding: 8px;">曜日</th>';
            reportHtml += '<th style="border: 1px solid #dee2e6; padding: 8px;">従業員番号</th>';
            reportHtml += '<th style="border: 1px solid #dee2e6; padding: 8px;">氏名</th>';
            reportHtml += '<th style="border: 1px solid #dee2e6; padding: 8px;">部署</th>';
            reportHtml += '<th style="border: 1px solid #dee2e6; padding: 8px;">出勤</th>';
            reportHtml += '<th style="border: 1px solid #dee2e6; padding: 8px;">退勤</th>';
            reportHtml += '<th style="border: 1px solid #dee2e6; padding: 8px;">実働</th>';
            reportHtml += '<th style="border: 1px solid #dee2e6; padding: 8px;">残業</th>';
            reportHtml += '<th style="border: 1px solid #dee2e6; padding: 8px;">深夜残業</th>';
            reportHtml += '</tr>';
            reportHtml += '</thead>';
            reportHtml += '<tbody>';
            
            // 真実のデータベースデータを使用
            if (data.records && data.records.length > 0) {
                data.records.forEach(record => {
                    let rowClass = '';
                    if (record.dayOfWeek === '土' || record.dayOfWeek === '日') {
                        rowClass = 'style="background-color: #f8f9fa;"';
                    }
                    
                    reportHtml += '<tr ' + rowClass + '>';
                    reportHtml += '<td style="border: 1px solid #dee2e6; padding: 8px; text-align: center;">' + record.date + '</td>';
                    reportHtml += '<td style="border: 1px solid #dee2e6; padding: 8px; text-align: center;">' + record.dayOfWeek + '</td>';
                    reportHtml += '<td style="border: 1px solid #dee2e6; padding: 8px; text-align: center;">' + record.empno + '</td>';
                    reportHtml += '<td style="border: 1px solid #dee2e6; padding: 8px; text-align: center;">' + record.empName + '</td>';
                    reportHtml += '<td style="border: 1px solid #dee2e6; padding: 8px; text-align: center;">' + record.deptName + '</td>';
                    reportHtml += '<td style="border: 1px solid #dee2e6; padding: 8px; text-align: center;">' + record.clockIn + '</td>';
                    reportHtml += '<td style="border: 1px solid #dee2e6; padding: 8px; text-align: center;">' + record.clockOut + '</td>';
                    reportHtml += '<td style="border: 1px solid #dee2e6; padding: 8px; text-align: center;">' + record.workTime + '</td>';
                    reportHtml += '<td style="border: 1px solid #dee2e6; padding: 8px; text-align: center;">' + record.overtimeTime + '</td>';
                    reportHtml += '<td style="border: 1px solid #dee2e6; padding: 8px; text-align: center;">' + record.nightovertimeTime + '</td>';
                    reportHtml += '</tr>';
                });
            } else {
                reportHtml += '<tr><td colspan="9" style="border: 1px solid #dee2e6; padding: 20px; text-align: center; color: #666;">指定期間内に勤怠記録がありません</td></tr>';
            }
            
            reportHtml += '</tbody>';
            reportHtml += '</table>';
            reportHtml += '</div>';
            reportHtml += '</div>';
            reportHtml += '</div>';
            
         // ✅ 部署別モーダルに差し込む
            document.getElementById("departmentReportContent").innerHTML = reportHtml;
            document.getElementById("departmentReportModal").style.display = "block";
        }

        function closeDepartmentReportModal() {
            document.getElementById("departmentReportModal").style.display = "none";
        }
        
        // 部署別報告書のダウンロード
        function downloadDepartmentReport(format) {
            const deptNoFilter = document.getElementById('deptNoFilter').value;
            const startDate = document.getElementById('startDate').value;
            const endDate = document.getElementById('endDate').value;
            
            const params = new URLSearchParams({
                action: 'downloadDepartment',
                deptNoFilter: deptNoFilter || '',
                format: format,
                startDate: startDate || '',
                endDate: endDate || ''
            });
            
            // ダウンロード用のURLを生成
            const downloadUrl = '<%= request.getContextPath() %>/ReportGenerationServlet?' + params.toString();
            
            // 新しいウィンドウでダウンロードを開始
            window.open(downloadUrl, '_blank');
        }
        
     	// 個人別報告書のダウンロード
        function downloadIndividualReport(format) {
            const empNoFilter = document.getElementById('empNoFilter').value;
            const startDate = document.getElementById('startDate')?.value || '';
            const endDate = document.getElementById('endDate')?.value || '';
            
            const params = new URLSearchParams({
                action: 'downloadIndividual',
                empNoFilter: empNoFilter,
                format: format,
                startDate: startDate,
                endDate: endDate
            });
            
            // 現在のタブでダウンロードを開始
            window.location.href = '<%= request.getContextPath() %>/ReportGenerationServlet?' + params.toString();
        }




        // 違反詳細モーダル表示関数
        function showViolationDetails() {
            <% if (complianceResult != null && complianceResult.getViolations() != null) { %>
                let html = '<div style="padding: 10px;">';
                html += '<table style="width: 100%; border-collapse: collapse; font-size: 12px;">';
                html += '<tr style="background-color: #f8f9fa;"><th style="border: 1px solid #dee2e6; padding: 8px;">違反項目</th><th style="border: 1px solid #dee2e6; padding: 8px;">詳細</th><th style="border: 1px solid #dee2e6; padding: 8px;">日付</th></tr>';
                
                <% for (ComplianceViolation violation : complianceResult.getViolations()) { %>
                    html += '<tr>';
                    html += '<td style="border: 1px solid #dee2e6; padding: 8px; text-align: center; font-weight: bold;"><%= violation.getViolationType() %></td>';
                    html += '<td style="border: 1px solid #dee2e6; padding: 8px;"><%= violation.getDescription() != null ? violation.getDescription() : "詳細なし" %></td>';
                    html += '<td style="border: 1px solid #dee2e6; padding: 8px; text-align: center;"><%= violation.getDate() != null ? violation.getDate().toString() : "---" %></td>';
                    html += '</tr>';
                <% } %>
                
                html += '</table>';
                html += '</div>';
                
                document.getElementById('violationDetailsContent').innerHTML = html;
                document.getElementById('violationDetailsModal').style.display = 'block';
            <% } %>
        }

        // 違反詳細モーダルを閉じる関数
        function closeViolationDetailsModal() {
            document.getElementById('violationDetailsModal').style.display = 'none';
        }

        // モーダル外をクリックしたときの処理
        window.onclick = function(event) {
            const violationModal = document.getElementById('violationDetailsModal');
            if (event.target == violationModal) {
                closeViolationDetailsModal();
            }
        }
        const deptFilter = document.getElementById('deptNoFilter');
        const postFilter = document.getElementById('postNoFilter');
        const empFilter = document.getElementById('empNoFilter');

        // 全従業員のオプションを配列に保存
        const allEmpOptions = Array.from(empFilter.options);

        function filterEmployees() {
            const selectedDept = deptFilter.value;
            const selectedPost = postFilter.value;

            

            allEmpOptions.forEach(option => {
                const dept = option.dataset.dept;
                const post = option.dataset.post;

                // 部署・役職の両方に一致する場合のみ追加
                if ((selectedDept === '' || dept === selectedDept) &&
                    (selectedPost === '' || post === selectedPost)) {
                    empFilter.appendChild(option);
                }
            });
        }

        // 部署・役職が変わったら従業員リストを絞り込む
        deptFilter.addEventListener('change', filterEmployees);
        postFilter.addEventListener('change', filterEmployees);

        // ページロード時にも適用（初期選択の復元用）
        window.addEventListener('load', filterEmployees);
    </script>
    <!-- EChartsを読み込む -->
	<script src="https://cdn.jsdelivr.net/npm/echarts@5.4.2/dist/echarts.min.js"></script>

<script>
document.addEventListener("DOMContentLoaded", function() {
    const modal = document.getElementById('projectModal');
    const closeBtn = document.getElementById('modalClose'); 
    const chartDom = document.getElementById('projectPieChart');

    let myChart = null;

    // 小数時間 → 「○時間○分」
    function toHHMM(hours) {
        if (!Number.isFinite(hours)) return '0時間0分';
        var total = Math.round(hours * 60);
        var h = Math.floor(total / 60);
        var m = total % 60;
        return h + "時間" + m + "分";
    }

    // 月単位の分析ボタン
    const analysisBtn = document.getElementById('btn-project-analysis');
    if (analysisBtn) {
        analysisBtn.addEventListener('click', function() {
            const empId = analysisBtn.dataset.empid;
            const start = analysisBtn.dataset.start;
            const end = analysisBtn.dataset.end;
            const title = analysisBtn.dataset.title;   // ← 年月タイトル

            modal.style.display = 'block';
            if (myChart) { myChart.dispose(); myChart = null; }

            fetch("KintaiRecServlet?action=projectAnalysis&empId=" + encodeURIComponent(empId) +
                    "&start=" + encodeURIComponent(start) +
                    "&end=" + encodeURIComponent(end))
                .then(function(res) {
                    if (!res.ok) throw new Error("サーバエラー: " + res.status);
                    return res.json();
                })
                .then(function(data) {
                    if (!data || data.length === 0) {
                        chartDom.innerHTML = '<p style="color:red;">データがありません</p>';
                        return;
                    }

                    var chartData = data
                        .filter(function(d) { return d && d.projectName; }) // null安全チェック
                        .map(function(d) { return { name: d.projectName, value: d.workHours }; });

                    if (chartData.length === 0) {
                        chartDom.innerHTML = '<p style="color:red;">有効なデータがありません</p>';
                        return;
                    }

                    chartDom.innerHTML = ''; // 前回の残骸クリア
                    myChart = echarts.init(chartDom);

                    myChart.setOption({
                        title: { text: title + " プロジェクト別工数割合", left: 'center' },
                        tooltip: {
                            trigger: 'item',
                            formatter: function(p) {
                                return p.name + "<br/>" + toHHMM(p.value) + " (" + p.percent + "%)";
                            }
                        },
                        legend: { top: 'bottom' },
                        series: [{
                            name: '作業時間',
                            type: 'pie',
                            radius: '60%',
                            label: {
                                formatter: function(p) {
                                    return p.name + ": " + toHHMM(p.value) + " (" + p.percent + "%)";
                                }
                            },
                            data: chartData
                        }]
                    });
                })
                .catch(function(err) {
                    console.error("データ取得失敗:", err);
                    chartDom.innerHTML = '<p style="color:red;">データ取得エラー</p>';
                });
        });
    }

    // モーダル閉じる
    closeBtn.addEventListener('click', function() { modal.style.display = 'none'; });
    window.addEventListener('click', function(e) { if (e.target === modal) modal.style.display = 'none'; });
});




</script>
</body>
</html>
