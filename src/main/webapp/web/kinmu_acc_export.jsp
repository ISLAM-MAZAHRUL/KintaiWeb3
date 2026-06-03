<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="kintai.UserBean" %>
<%@ page import="kintai.KinmuManageBean" %>
<%@ page import="java.util.List" %>
<%
    UserBean user = (UserBean) session.getAttribute("user");
    if (user == null) {
        response.sendRedirect(request.getContextPath() + "/web/login.jsp");
        return;
    }


  

    List<KinmuManageBean.WorkAlloc> results =
        (List<KinmuManageBean.WorkAlloc>) request.getAttribute("results");
    String startMonth = (String) request.getAttribute("startMonth");
    String endMonth = (String) request.getAttribute("endMonth");
%>
<html>
<head>
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>プロジェクト別勤怠</title>
    <style>
        body { margin: 0; font-family: "Helvetica Neue", Arial, Meiryo, sans-serif; background: #f4f7f6; color: #333; }
        .main-wrapper { max-width: 1200px; margin: 0 auto; background: white; padding: 30px; border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.05); }
        .breadcrumb { font-size: 13px; color: #007bff; margin-bottom: 25px; }
        .breadcrumb a { color: #007bff; text-decoration: none; }
        .page-header { display: flex; align-items: center; gap: 12px; border-bottom: 3px solid #28a745; padding-bottom: 15px; margin-bottom: 25px; }
        .page-header h1 { margin: 0; font-size: 24px; }
        .badge { background: #007bff; color: white; font-size: 12px; padding: 3px 10px; border-radius: 20px; }

        .quick-select { margin-bottom: 15px; }
        .quick-select label { font-weight: bold; font-size: 14px; margin-right: 10px; }
        .quick-btn { padding: 6px 14px; border: 1px solid #ccc; border-radius: 20px; background: white; cursor: pointer; font-size: 13px; margin-right: 8px; transition: all 0.2s; }
        .quick-btn:hover { background: #e8f5e9; border-color: #28a745; color: #28a745; }

        .form-container { background: #fcfcfc; border: 1px solid #eee; border-radius: 8px; padding: 20px 30px; margin-bottom: 20px; }
        .form-row { display: flex; align-items: center; gap: 15px; flex-wrap: wrap; }
        .form-row label { font-weight: bold; font-size: 14px; }
        input[type="month"] { padding: 8px 12px; border: 1px solid #ccc; border-radius: 5px; font-size: 15px; }

        .button-group { display: flex; gap: 10px; margin-top: 15px; flex-wrap: wrap; }
        .btn { padding: 10px 22px; border-radius: 5px; font-size: 14px; font-weight: bold; border: none; cursor: pointer; display: flex; align-items: center; gap: 8px; text-decoration: none; transition: all 0.2s; }
        .btn-blue { background: #007bff; color: white; }
        .btn-blue:hover { background: #0056b3; }
        .btn-green { background: #28a745; color: white; }
        .btn-green:hover { background: #218838; }
        .btn-grey { background: #6c757d; color: white; }
        .btn-grey:hover { background: #5a6268; }

        .result-section { margin-top: 25px; }
        .result-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }
        .result-header h2 { margin: 0; font-size: 18px; }
        .period-badge { background: #e8f5e9; color: #28a745; border: 1px solid #28a745; border-radius: 20px; padding: 4px 14px; font-size: 13px; font-weight: bold; }
        .result-count { font-size: 14px; color: #666; margin-bottom: 10px; }

        table { width: 100%; border-collapse: collapse; font-size: 14px; }
        th { background: #28a745; color: white; padding: 10px 12px; text-align: center; white-space: nowrap; }
        td { padding: 8px 12px; border: 1px solid #e0e0e0; text-align: center; }
        tr:nth-child(even) { background: #f9f9f9; }
        tr:hover { background: #f0fff0; }
    </style>
</head>
<body>
<div class="main-wrapper">

    <div class="breadcrumb">
        <a href="<%= request.getContextPath() %><%= user.getRoleId() == 1 ? "/AdminMenuServlet" : "/MenuServlet" %>">管理者メニュー</a> ＞ プロジェクト別勤怠
    </div>

    <div class="page-header">
        <span style="font-size: 28px;">📊</span>
        <h1>プロジェクト別勤怠</h1>
        <span class="badge"><%= user.getRoleId() == 1 ? "管理者：全従業員" : "一般：自分のみ" %></span>
    </div>

    <div class="form-container">
        <div class="quick-select">
            <label>クイック選択：</label>
            <button type="button" class="quick-btn" onclick="setThisMonth()">今月</button>
            <button type="button" class="quick-btn" onclick="setLastYear()">直近1年間</button>
            <button type="button" class="quick-btn" onclick="setThisYear()">今年度（7月始）</button>
            <button type="button" class="quick-btn" onclick="setLastFiscalYear()">前年度</button>
        </div>

        <form id="exportForm" action="<%= request.getContextPath() %>/AccountingExportServlet" method="post">
            <div class="form-row">
                <label>開始年月：</label>
                <input type="month" name="startMonth" id="startMonth" value="<%= startMonth != null ? startMonth : "" %>" required>
                <span style="font-size: 20px; color: #888;">〜</span>
                <label>終了年月：</label>
                <input type="month" name="endMonth" id="endMonth" value="<%= endMonth != null ? endMonth : "" %>" required>
            </div>

            <div class="button-group">
                <button type="submit" name="action" value="preview" class="btn btn-blue">
                    🔍 集計する
                </button>
                <button type="submit" name="action" value="download" class="btn btn-green">
                    📥 CSVダウンロード
                </button>
                <button type="submit" name="action" value="excel" class="btn btn-green" style="background:#217346;">
                    📗 Excelダウンロード
                </button>
                <a href="<%= request.getContextPath() %><%= user.getRoleId() == 1 ? "/AdminMenuServlet" : "/MenuServlet" %>" class="btn btn-grey">
                    ← 戻る
                </a>
            </div>
        </form>
    </div>

    <!-- 集計結果 -->
    <% if (results != null) { %>
    <div class="result-section">
        <div class="result-header">
            <h2>集計結果</h2>
            <span class="period-badge"><%= startMonth %> 〜 <%= endMonth %></span>
        </div>
        <div class="result-count">対象：<%= results.size() %>件</div>

        <table>
            <tr>
                <th>社員番号</th>
                <th>社員氏名</th>
                <th>年月</th>
                <th>プロジェクト名</th>
                <th>プロジェクトコード</th>
                <th>勤務時間</th>
            </tr>
            <% for (KinmuManageBean.WorkAlloc wa : results) { %>
            <tr>
                <td><%= wa.getEmpId() %></td>
                <td><%= wa.getEmpName() %></td>
                <td><%= wa.getYearMonth() %></td>
                <td><%= wa.getProjectName() %></td>
                <td><%= wa.getProjectCode() %></td>
                <td><%= String.format("%.1f", wa.getWorkHours()) %></td>
            </tr>
            <% } %>
        </table>
    </div>
    <% } %>

</div>

<script>
    function setThisMonth() {
        const now = new Date();
        const ym = now.getFullYear() + '-' + String(now.getMonth() + 1).padStart(2, '0');
        document.getElementById('startMonth').value = ym;
        document.getElementById('endMonth').value = ym;
    }

    function setLastYear() {
        const now = new Date();
        const end = now.getFullYear() + '-' + String(now.getMonth() + 1).padStart(2, '0');
        const startDate = new Date(now.getFullYear() - 1, now.getMonth() + 1, 1);
        const start = startDate.getFullYear() + '-' + String(startDate.getMonth() + 1).padStart(2, '0');
        document.getElementById('startMonth').value = start;
        document.getElementById('endMonth').value = end;
    }

    function setThisYear() {
        const now = new Date();
        const fiscalStart = now.getMonth() >= 6 ? now.getFullYear() : now.getFullYear() - 1;
        const start = fiscalStart + '-07';
        const end = (fiscalStart + 1) + '-06';
        document.getElementById('startMonth').value = start;
        document.getElementById('endMonth').value = end;
    }

    function setLastFiscalYear() {
        const now = new Date();
        const fiscalStart = (now.getMonth() >= 6 ? now.getFullYear() : now.getFullYear() - 1) - 1;
        const start = fiscalStart + '-07';
        const end = (fiscalStart + 1) + '-06';
        document.getElementById('startMonth').value = start;
        document.getElementById('endMonth').value = end;
    }
</script>
</body>
</html>

