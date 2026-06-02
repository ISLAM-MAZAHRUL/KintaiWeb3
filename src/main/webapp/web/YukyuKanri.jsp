<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="kintai.UserBean" %>
<%@ page import="java.util.List, java.util.Map" %>
<%
    UserBean user = (UserBean) session.getAttribute("user");
    if (user == null) {
        response.sendRedirect(request.getContextPath() + "/web/login.jsp");
        return;
    }
    List<Map<String, Object>> summaryList =
        (List<Map<String, Object>>) request.getAttribute("summaryList");
    String searchKeyword = request.getParameter("search") != null ? request.getParameter("search") : "";
    String filterMode = request.getParameter("filter") != null ? request.getParameter("filter") : "all";
%>
<html>
<head>
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>有休管理</title>
    <style>
        body { margin: 0; font-family: Meiryo, sans-serif; background: #f4f7f6; }
        .main-wrapper { max-width: 1200px; margin: 0 auto; background: white; padding: 30px; border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.05); }
        .page-header { display: flex; align-items: center; gap: 12px; border-bottom: 3px solid #6f42c1; padding-bottom: 15px; margin-bottom: 25px; }
        .page-header h1 { margin: 0; font-size: 24px; }

        /* 検索・フィルターエリア */
        .search-area { display: flex; gap: 10px; margin-bottom: 15px; flex-wrap: wrap; align-items: center; }
        .search-area input { padding: 8px 12px; border: 1px solid #ccc; border-radius: 5px; font-size: 14px; width: 250px; }
        .btn { padding: 8px 16px; border: none; border-radius: 5px; cursor: pointer; font-size: 14px; }
        .btn-search { background: #6f42c1; color: white; }
        .btn-all { background: #17a2b8; color: white; }
        .btn-filter { background: #dc3545; color: white; }
        .btn-grant { background: #28a745; color: white; }
        .btn-back { background: #6c757d; color: white; padding: 10px 20px; border: none; border-radius: 5px; cursor: pointer; text-decoration: none; }

        /* テーブル */
        table { width: 100%; border-collapse: collapse; margin-top: 10px; }
        th { background: #6f42c1; color: white; padding: 12px; text-align: center; font-size: 13px; }
        td { padding: 10px; border: 1px solid #ddd; text-align: center; font-size: 13px; }
        tr:nth-child(even) { background: #f9f9f9; }
        tr:hover { background: #f0e6ff; }

        /* アラート */
        .alert-red { background: #ffe0e0 !important; }
        .alert-yellow { background: #fff8e0 !important; }

        .achieved { color: #28a745; font-weight: bold; }
        .not-achieved { color: #dc3545; font-weight: bold; }

        .btn-detail { background: #6f42c1; color: white; padding: 4px 10px; border: none; border-radius: 4px; cursor: pointer; font-size: 12px; text-decoration: none; }

        /* カウント */
        .summary-bar { display: flex; gap: 20px; margin-bottom: 15px; flex-wrap: wrap; }
        .summary-card { background: #f8f0ff; border: 1px solid #6f42c1; border-radius: 8px; padding: 10px 20px; text-align: center; }
        .summary-card .num { font-size: 24px; font-weight: bold; color: #6f42c1; }
        .summary-card .label { font-size: 12px; color: #666; }
    </style>
</head>
<body>
<div class="main-wrapper">
    <div class="page-header">
        <span style="font-size: 28px;">🏖️</span>
        <h1>有休管理</h1>
    </div>

    <%
        // 集計
        int totalEmp = 0;
        int achievedCount = 0;
        int notAchievedCount = 0;
        int alertCount = 0;
        if (summaryList != null) {
            for (Map<String, Object> r : summaryList) {
                int u = r.get("used") != null ? (int) r.get("used") : 0;
                int t = r.get("totalRemaining") != null ? (int) r.get("totalRemaining") : 0;
                totalEmp++;
                if (u >= 5) achievedCount++;
                else notAchievedCount++;
                if (t <= 5) alertCount++;
            }
        }
    %>

    <!-- サマリーカード -->
    <div class="summary-bar">
        <div class="summary-card">
            <div class="num"><%= totalEmp %></div>
            <div class="label">総社員数</div>
        </div>
        <div class="summary-card">
            <div class="num" style="color:#28a745;"><%= achievedCount %></div>
            <div class="label">5日義務達成</div>
        </div>
        <div class="summary-card">
            <div class="num" style="color:#dc3545;"><%= notAchievedCount %></div>
            <div class="label">5日義務未達成</div>
        </div>
        <div class="summary-card">
            <div class="num" style="color:#e6a817;"><%= alertCount %></div>
            <div class="label">残日数少ない（5日以下）</div>
        </div>
    </div>

    <!-- 検索・フィルターエリア -->
    <div class="search-area">
        <input type="text" id="searchInput" placeholder="🔍 社員番号または氏名で検索..." 
               value="<%= searchKeyword %>" onkeyup="filterTable()">
        <button class="btn btn-all" onclick="showAll()">全件表示</button>
        <button class="btn btn-filter" onclick="filterNotAchieved()">❌ 未達成のみ</button>
        <button class="btn btn-filter" style="background:#e6a817;" onclick="filterAlert()">⚠️ 残少ない</button>
        <button class="btn btn-grant" onclick="grantConfirm()">➕ 今年度分を付与する</button>
        <% if (user.getRoleId() == 1) { %>
        <a href="<%= request.getContextPath() %>/YukyuGrantSettingServlet" class="btn" style="background:#6f42c1;color:white;text-decoration:none;">⚙️ 付与日数設定</a>
        <% } %>
    </div>

    <!-- テーブル -->
    <% if (summaryList != null && !summaryList.isEmpty()) { %>
    <table id="yukyuTable">
        <tr>
            <th>社員番号</th>
            <th>氏名</th>
            <th>今年残</th>
            <th>去年残</th>
            <th>残合計</th>
            <th>消化日数</th>
            <th>5日義務達成</th>
            <th>詳細</th>
        </tr>
        <% for (Map<String, Object> row : summaryList) {
            int used = row.get("used") != null ? (int) row.get("used") : 0;
            int thisYear = row.get("thisYearRemaining") != null ? (int) row.get("thisYearRemaining") : 0;
            int lastYear = row.get("lastYearRemaining") != null ? (int) row.get("lastYearRemaining") : 0;
            int total = row.get("totalRemaining") != null ? (int) row.get("totalRemaining") : 0;
            String empId = (String) row.get("empId");
            String empName = (String) row.get("empName");

            // アラートクラス
            String rowClass = "";
            if (total <= 3) rowClass = "alert-red";
            else if (total <= 5) rowClass = "alert-yellow";
        %>
        <tr class="data-row <%= rowClass %>" 
            data-empid="<%= empId %>" 
            data-empname="<%= empName %>"
            data-achieved="<%= used >= 5 ? "yes" : "no" %>"
            data-total="<%= total %>">
            <td><%= empId %></td>
            <td><%= empName %></td>
            <td><%= thisYear %>日</td>
            <td><%= lastYear %>日</td>
            <td><%= total %>日</td>
            <td><%= used %>日</td>
            <td class="<%= used >= 5 ? "achieved" : "not-achieved" %>">
                <%= used >= 5 ? "✅ 達成" : "❌ 未達成" %>
            </td>
            <td>
                <a href="<%= request.getContextPath() %>/YukyuKanriServlet?action=detail&empId=<%= empId %>" 
                   class="btn-detail">📋 履歴</a>
            </td>
        </tr>
        <% } %>
    </table>
    <% } else { %>
        <p>データがありません。</p>
    <% } %>

    <br>
    <a href="<%= request.getContextPath() %>/AdminMenuServlet" class="btn-back">メニューに戻る</a>
</div>

<script>
    // 検索フィルター
    function filterTable() {
        var input = document.getElementById("searchInput").value.toLowerCase();
        var rows = document.getElementsByClassName("data-row");
        for (var i = 0; i < rows.length; i++) {
            var empId = rows[i].getAttribute("data-empid").toLowerCase();
            var empName = rows[i].getAttribute("data-empname").toLowerCase();
            if (empId.includes(input) || empName.includes(input)) {
                rows[i].style.display = "";
            } else {
                rows[i].style.display = "none";
            }
        }
    }

    // 全件表示
    function showAll() {
        document.getElementById("searchInput").value = "";
        var rows = document.getElementsByClassName("data-row");
        for (var i = 0; i < rows.length; i++) {
            rows[i].style.display = "";
        }
    }

    // 未達成のみ表示
    function filterNotAchieved() {
        var rows = document.getElementsByClassName("data-row");
        for (var i = 0; i < rows.length; i++) {
            if (rows[i].getAttribute("data-achieved") === "no") {
                rows[i].style.display = "";
            } else {
                rows[i].style.display = "none";
            }
        }
    }

    // 残少ない表示（5日以下）
    function filterAlert() {
        var rows = document.getElementsByClassName("data-row");
        for (var i = 0; i < rows.length; i++) {
            var total = parseInt(rows[i].getAttribute("data-total"));
            if (total <= 5) {
                rows[i].style.display = "";
            } else {
                rows[i].style.display = "none";
            }
        }
    }

    // 付与確認
    function grantConfirm() {
        if (confirm("今年度分（10日）を全社員に付与しますか？")) {
            window.location.href = "<%= request.getContextPath() %>/YukyuKanriServlet?action=grant";
        }
    }
</script>
</body>
</html>
