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
%>
<html>
<head>
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>有休管理</title>
    <style>
        body { margin: 0; font-family: Meiryo, sans-serif; background: #f4f7f6; }
        .main-wrapper { max-width: 1000px; margin: 0 auto; background: white; padding: 30px; border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.05); }
        .page-header { display: flex; align-items: center; gap: 12px; border-bottom: 3px solid #6f42c1; padding-bottom: 15px; margin-bottom: 25px; }
        .page-header h1 { margin: 0; font-size: 24px; }
        table { width: 100%; border-collapse: collapse; margin-top: 20px; }
        th { background: #6f42c1; color: white; padding: 12px; text-align: center; }
        td { padding: 10px; border: 1px solid #ddd; text-align: center; }
        tr:nth-child(even) { background: #f9f9f9; }
        .achieved { color: #28a745; font-weight: bold; }
        .not-achieved { color: #dc3545; font-weight: bold; }
        .btn-back { background: #6c757d; color: white; padding: 10px 20px; border: none; border-radius: 5px; cursor: pointer; text-decoration: none; }
    </style>
</head>
<body>
<div class="main-wrapper">
    <div class="page-header">
        <span style="font-size: 28px;">🏖️</span>
        <h1>有休管理</h1>
    </div>

    <% if (summaryList != null) { %>
    <table>
        <tr>
            <th>社員番号</th>
            <th>氏名</th>
            <th>付与日数</th>
            <th>消化日数</th>
            <th>残日数</th>
            <th>5日義務達成</th>
        </tr>
        <% for (Map<String, Object> row : summaryList) {
            int used = (int) row.get("used");
        %>
        <tr>
            <td><%= row.get("empId") %></td>
            <td><%= row.get("empName") %></td>
            <td><%= row.get("granted") %></td>
            <td><%= row.get("used") %></td>
            <td><%= row.get("remaining") %></td>
            <td class="<%= used >= 5 ? "achieved" : "not-achieved" %>">
                <%= used >= 5 ? "✅ 達成" : "❌ 未達成" %>
            </td>
        </tr>
        <% } %>
    </table>
    <% } %>

    <br>
    <a href="<%= request.getContextPath() %>/AdminMenuServlet" class="btn-back">← 戻る</a>
</div>
</body>
</html>