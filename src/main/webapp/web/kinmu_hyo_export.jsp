<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="kintai.UserBean" %>
<%@ page import="kintai.EmpBean" %>
<%@ page import="kintai.KintaiRecBean" %>
<%@ page import="kintai.MonthlySummaryBean" %>
<%@ page import="java.util.List" %>
<%@ page import="java.time.LocalDate" %>
<%@ page import="java.time.YearMonth" %>
<%
    UserBean user = (UserBean) session.getAttribute("user");
    if (user == null) {
        response.sendRedirect(request.getContextPath() + "/web/login.jsp");
        return;
    }
    EmpBean emp = (EmpBean) request.getAttribute("emp");
    List<KintaiRecBean> records = (List<KintaiRecBean>) request.getAttribute("records");
    MonthlySummaryBean summary =
    	    (MonthlySummaryBean) request.getAttribute("summary");
    String targetMonth = (String) request.getAttribute("targetMonth");
    String startMonth = (String) request.getAttribute("startMonth");
    String endMonth = (String) request.getAttribute("endMonth");
    YearMonth ym = (YearMonth) request.getAttribute("ym");

    // 一般ユーザー向けは /menu、管理者向けは /AdminMenuServlet です。
    String menuPath = (user.getRoleId() == 1) ? "/AdminMenuServlet" : "/menu";
    String[] weekdays = {"日", "月", "火", "水", "木", "金", "土"};
%>
<html>
<head>
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>勤務表エクスポート</title>
    <style>
        body { margin: 0; font-family: 'メイリオ', sans-serif; background: #f5f5f5; }
        .container { max-width: 1200px; margin: 0 auto; background: white; padding: 20px; }
        .header { display: flex; justify-content: space-between; align-items: center;
                  padding: 15px 20px; border-bottom: 2px solid #ff9800; margin-bottom: 20px; }
        h1 { color: #333; border-bottom: 2px solid #ff9800; padding-bottom: 10px; }
        .form-section { background: #f8f9fa; padding: 20px; border-radius: 8px; margin-bottom: 20px; }
        .form-row { display: flex; gap: 15px; align-items: flex-end; flex-wrap: wrap; margin-bottom: 15px; }
        .form-group { display: flex; flex-direction: column; gap: 5px; }
        label { font-size: 0.9em; color: #666; font-weight: bold; }
        input[type="month"], input[type="text"] {
            padding: 8px; border: 1px solid #ddd; border-radius: 5px; font-size: 1em; }
        .btn { padding: 10px 20px; border: none; border-radius: 5px; cursor: pointer;
               color: white; font-size: 0.9em; text-decoration: none; display: inline-flex; align-items: center; gap: 5px; }
        .btn-preview { background: #007bff; }
        .btn-csv { background: #28a745; }
        .btn-back { background: #6c757d; }
        table { width: 100%; border-collapse: collapse; margin-top: 20px; table-layout: fixed; }
        th { background: #ff9800; color: white; padding: 10px; text-align: center; border: 1px solid #e08e0b; }
        td { padding: 8px; border: 1px solid #ddd; text-align: center; }
        tr:nth-child(even) { background: #f8f9fa; }
        .weekend { background: #ffe0e0 !important; }
        .info-section { background: #f8f9fa; padding: 15px; border-radius: 8px; margin-bottom: 15px; }
        .info-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
        .logout-button { background: #dc3545; color: white; border: none; padding: 8px 16px;
                         border-radius: 5px; cursor: pointer; }
        
        .quick-select-container {
            display: flex;
            align-items: center;
            gap: 8px;
            flex-wrap: wrap;
            margin-bottom: 15px;
        }
        .quick-btn {
            background: #f0f0f0;
            border: 1px solid #ccc;
            border-radius: 20px;
            padding: 6px 14px;
            font-size: 13px;
            cursor: pointer;
            transition: 0.2s;
        }
        .quick-btn:hover {
            background: #007bff;
            color: white;
            border-color: #007bff;
        }
        .line-break {
            flex-basis: 100%;
            height: 10px;
        }
        .status-badge {
    display: inline-block;
    padding: 6px 14px;
    border-radius: 20px;
    font-weight: bold;
    font-size: 14px;
}

.badge-work {
    background: #e3f2fd;
    color: #1565c0;
}

.badge-paid {
    background: #e8f5e9;
    color: #2e7d32;
}

.badge-sick {
    background: #fdecea;
    color: #c62828;
}

.badge-dayoff {
    background: #eeeeee;
    color: #555555;
}

.badge-holiday {
    background: #fce4ec;
    color: #ad1457;
}
    </style>
</head>
<body>
<div class="container">
    <div class="header">
        <div>
            <p style="margin:2px 0;">部署：<%= session.getAttribute("deptName") %></p>
            <p style="margin:2px 0;">氏名：<%= user.getName() %></p>
        </div>
        <form method="post" action="<%= request.getContextPath() %>/logout">
            <button type="submit" class="logout-button">ログアウト</button>
        </form>
    </div>

    <h1>勤務表データ出力</h1>
    
    <div class="form-section">
        <form action="<%= request.getContextPath() %>/KinmuHyoExportServlet" method="post">
            
            <div class="quick-select-container">
                <label>クイック選択：</label>
                <button type="button" class="quick-btn" onclick="setThisMonth()">今月</button>
                <button type="button" class="quick-btn" onclick="setLastYear()">直近1年間</button>
                <button type="button" class="quick-btn" onclick="setThisYear()">今年度</button>
                <button type="button" class="quick-btn" onclick="setLastFiscalYear()">前年度</button>
                
                <div class="line-break"></div>
            </div>

            <div class="form-row">
                <% if (user.getRoleId() == 1) { %>
                <div class="form-group">
                    <label>社員番号</label>
                    <input type="text" name="empId" value="<%= emp != null ? emp.getEmpId() : "" %>" placeholder="社員番号" />
                </div>
                <% } else { %>
                <input type="hidden" name="empId" value="<%= user.getEmpId() %>" />
                <% } %>

                <div class="form-group">
                    <label>開始月</label>
                    <input type="month" id="startMonth" name="startMonth" value="<%= startMonth != null ? startMonth : "" %>" required />
                </div>

                <div class="form-group">
                    <label>終了月</label>
                    <input type="month" id="endMonth" name="endMonth" value="<%= endMonth != null ? endMonth : "" %>" required />
                </div>
                



            <div class="form-row">
                <button type="submit" name="action" value="preview" class="btn btn-preview">
                    🔍 集計する
                </button>
                <button type="submit" name="action" value="download" class="btn btn-csv">
                    📥 CSVダウンロード
                </button>
                <button type="submit" name="action" value="excel" class="btn btn-csv">
    📊 Excelダウンロード
</button>
                <a href="<%= request.getContextPath() + menuPath %>" class="btn btn-back"> メニューに戻る</a>
                
            </div>

        </form>
    </div>

    <% if (emp != null && startMonth != null && endMonth != null) { %>
    <div class="info-section">
        <div class="info-grid">
            <div><strong>社員番号：</strong><%= emp.getEmpId() %></div>
            <div><strong>氏名：</strong><%= emp.getEmpName() %></div>
            <div>
                <strong>対象期間：</strong>
                <%= startMonth %> 〜 <%= endMonth %>
            </div>
        </div>
    </div>

    <% if (records != null) { %>
    <h2>プレビュー結果</h2>
    <table>
        <tr>
            <th>日付</th>
            <th>出勤状況</th>
            <th>曜日</th>
            <th>始業</th>
            <th>終了</th>
            <th>休憩時間</th>
			<th>勤務時間</th>
			<th>残業時間</th>
			<th>プロジェクトコード</th>
			
			
		 	
        </tr>
        <%
        // 日付テキストを安全にLocalDateに変換する
        LocalDate startDate = YearMonth.parse(startMonth).atDay(1);
        LocalDate endDate = YearMonth.parse(endMonth).atEndOfMonth();
        
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            String weekday = weekdays[date.getDayOfWeek().getValue() % 7];
            boolean isWeekend = date.getDayOfWeek().getValue() == 6 || date.getDayOfWeek().getValue() == 7;

            KintaiRecBean rec = null;
            for (KintaiRecBean r : records) {
                if (r.getKintaiDate().equals(date)) { rec = r; break; }
            }
        %>
        <tr class="<%= isWeekend ? "weekend" : "" %>">

    <td>
        <%= date.getMonthValue() %>/<%= String.format("%02d", date.getDayOfMonth()) %>
    </td>

    <td>
    <%
    String status = "";

    if (rec != null && rec.getAttendanceType() != null) {
        status = rec.getAttendanceType();
    }

    // Auto status
    if (status == null || status.isEmpty()) {

        if (isWeekend) {
            status = "休み";
        } else if (rec != null && rec.getClockIn() != null) {
            status = "出勤";
        }

    }

    String badgeClass = "badge-normal";

    if ("出勤".equals(status)) {
        badgeClass = "badge-work";
    } else if ("有給休暇".equals(status)) {
        badgeClass = "badge-paid";
    } else if ("病気休暇".equals(status)) {
        badgeClass = "badge-sick";
    } else if ("休み".equals(status)) {
        badgeClass = "badge-dayoff";
    } else if ("祝日".equals(status)) {
        badgeClass = "badge-holiday";
    }
    %>

    <span class="status-badge <%= badgeClass %>">
        <%= status %>
    </span>
    </td>

    

<td><%= weekday %></td>

<!-- 始業 -->
<td>

    <%= rec != null && rec.getClockIn() != null
        ? rec.getClockIn().toString().substring(0, 5)
        : "" %>
</td>

<!-- 終了 -->
<td>
    <%= rec != null && rec.getClockOut() != null
        ? rec.getClockOut().toString().substring(0, 5)
        : "" %>
</td>

<!-- 休憩時間 -->
<td>
    <%= rec != null && rec.getClockIn() != null
        ? String.format("%.1f",
            rec.getTotalBreakMinutes() / 60.0)
        : "" %>
</td>

<!-- 勤務時間 -->
<td>
    <%= rec != null && rec.getClockIn() != null
        ? String.format("%.1f",
            rec.getActualWorkMinutes() / 60.0)
        : "" %>
</td>

<!-- 残業時間 -->
<td>
    <%= rec != null && rec.getClockIn() != null
        ? String.format("%.1f",
            rec.getOvertimeMinutes() / 60.0)
        : "" %>
</td>
<!-- プロジェクトコード -->
<td>
    <%= rec != null && rec.getProjectId() != null
        ? rec.getProjectId()
        : "" %>
</td>


</tr>
<% } %>

    
  

<% if (summary != null) { %>
<tr style="font-weight:bold; background:#f1f3f5;">
    <td style="text-align:left; padding-left:200px;" colspan="5">合計</td>

    <td>
        <%= summary.getTotalBreakHours() %>
    </td>

    <td>
        <%= summary.getTotalWorkingHours() %>
    </td>

    <td>
        <%= summary.getTotalOvertimeHours() %>
    </td>
    <td></td>
</tr>
<% } %>

</table> 

<% if (summary != null) { %>

<h3>集計結果</h3>

<p>
合計勤務時間：
<%= summary.getTotalWorkingHours() %>
</p>

<p>
合計残業時間：
<%= summary.getTotalOvertimeHours() %>
</p>


<p>
合計休憩時間：
<%= summary.getTotalBreakHours() %>
</p>
<% } %>
<% } %>

<% } %>

</div> <script>
function formatMonth(date) {
    const y = date.getFullYear();
    const m = String(date.getMonth() + 1).padStart(2, '0');
    return y + '-' + m;
}

function setThisMonth() {
    const now = new Date();
    const ym = formatMonth(now);
    document.getElementById("startMonth").value = ym;
    document.getElementById("endMonth").value = ym;
}

function setLastYear() {
    const end = new Date();
    const start = new Date();
    start.setMonth(start.getMonth() - 11);
    document.getElementById("startMonth").value = formatMonth(start);
    document.getElementById("endMonth").value = formatMonth(end);
}

function setThisYear() {
    const now = new Date();
    let year = now.getFullYear();
    if ((now.getMonth() + 1) < 4) {
        year--;
    }
    const start = new Date(year, 3, 1);
    document.getElementById("startMonth").value = formatMonth(start);
    document.getElementById("endMonth").value = formatMonth(now);
}

function setLastFiscalYear() {
    const now = new Date();
    let year = now.getFullYear();
    if ((now.getMonth() + 1) < 4) {
        year--;
    }
    const start = new Date(year - 1, 3, 1);
    const end = new Date(year, 2, 1);
    document.getElementById("startMonth").value = formatMonth(start);
    document.getElementById("endMonth").value = formatMonth(end);
}
</script>
</body>
</html>