<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="kintai.UserBean, kintai.EmpBean" %>
<%@ page import="java.util.List, java.util.Map" %>
<%
    UserBean user = (UserBean) session.getAttribute("user");
    if (user == null) {
        response.sendRedirect(request.getContextPath() + "/web/login.jsp");
        return;
    }
    EmpBean emp = (EmpBean) request.getAttribute("emp");
    List<Map<String, Object>> historyList =
        (List<Map<String, Object>>) request.getAttribute("historyList");
%>
<html>
<head>
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>有休履歴</title>
    <style>
        body { margin: 0; font-family: Meiryo, sans-serif; background: #f4f7f6; }
        .main-wrapper { max-width: 900px; margin: 30px auto; background: white; padding: 30px; border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.05); }
        .page-header { display: flex; align-items: center; gap: 12px; border-bottom: 3px solid #6f42c1; padding-bottom: 15px; margin-bottom: 25px; }
        .page-header h1 { margin: 0; font-size: 24px; }

        /* 社員情報カード */
        .emp-card { background: #f8f0ff; border: 1px solid #6f42c1; border-radius: 8px; padding: 15px 20px; margin-bottom: 25px; display: flex; gap: 30px; }
        .emp-card .label { font-size: 12px; color: #666; }
        .emp-card .value { font-size: 16px; font-weight: bold; color: #6f42c1; }

        /* テーブル */
        table { width: 100%; border-collapse: collapse; margin-top: 10px; }
        th { background: #6f42c1; color: white; padding: 12px; text-align: center; font-size: 13px; }
        td { padding: 10px; border: 1px solid #ddd; text-align: center; font-size: 13px; }
        tr:nth-child(even) { background: #f9f9f9; }
        tr:hover { background: #f0e6ff; }

        /* ステータス */
        .status-active { color: #28a745; font-weight: bold; }
        .status-expired { color: #dc3545; font-weight: bold; }
        .status-warning { color: #e6a817; font-weight: bold; }

        .btn-back { background: #6c757d; color: white; padding: 10px 20px; border: none; border-radius: 5px; cursor: pointer; text-decoration: none; display: inline-block; margin-top: 20px; }
    </style>
</head>
<body>
<div class="main-wrapper">
    <div class="page-header">
        <span style="font-size: 28px;">📋</span>
        <h1>有休付与履歴</h1>
    </div>

    <!-- 社員情報カード -->
    <% if (emp != null) { %>
    <div class="emp-card">
        <div>
            <div class="label">社員番号</div>
            <div class="value"><%= emp.getEmpId() %></div>
        </div>
        <div>
            <div class="label">氏名</div>
            <div class="value"><%= emp.getEmpName() %></div>
        </div>
        <div>
            <div class="label">部署</div>
            <div class="value"><%= emp.getDeptName() != null ? emp.getDeptName() : "-" %></div>
        </div>
    </div>
    <% } %>

    <!-- 付与履歴テーブル -->
    <h3 style="color:#6f42c1;">📅 付与履歴一覧</h3>
    <% if (historyList != null && !historyList.isEmpty()) { %>
    <table>
        <tr>
            <th>付与日</th>
            <th>有効期限</th>
            <th>付与日数</th>
            <th>消化日数</th>
            <th>残日数</th>
            <th>ステータス</th>
        </tr>
        <% 
            java.time.LocalDate today = java.time.LocalDate.now();
            for (Map<String, Object> row : historyList) {
                java.sql.Date expireSql = (java.sql.Date) row.get("expireDate");
                java.time.LocalDate expireDate = expireSql != null ? expireSql.toLocalDate() : null;
                int remaining = row.get("remaining") != null ? (int) row.get("remaining") : 0;
                
                String status = "";
                String statusClass = "";
                if (expireDate != null && expireDate.isBefore(today)) {
                    status = "期限切れ";
                    statusClass = "status-expired";
                } else if (remaining <= 3) {
                    status = "残少ない";
                    statusClass = "status-warning";
                } else {
                    status = "有効";
                    statusClass = "status-active";
                }
        %>
        <tr>
            <td><%= row.get("grantDate") %></td>
            <td><%= row.get("expireDate") %></td>
            <td><%= row.get("grantedDays") %>日</td>
            <td><%= row.get("usedDays") %>日</td>
            <td><strong><%= remaining %>日</strong></td>
            <td class="<%= statusClass %>"><%= status %></td>
        </tr>
        <% } %>
    </table>
    <% } else { %>
        <p>付与履歴がありません。</p>
    <% } %>

    <a href="<%= request.getContextPath() %>/YukyuKanriServlet" class="btn-back">← 有休管理に戻る</a>
</div>
</body>
</html>