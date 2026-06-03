<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="kintai.UserBean" %>
<%@ page import="kintai.EmpBean" %>
<%@ page import="kintai.MonthlySummaryBean" %>
<%@ page import="java.util.List" %>


<%
    UserBean user = (UserBean) session.getAttribute("user");

    if (user == null) {
        response.sendRedirect(request.getContextPath() + "/web/login.jsp");
        return;
    }

    List<EmpBean> results =
        (List<EmpBean>) request.getAttribute("results");

    String startMonth =
    	    (String) request.getAttribute("startMonth");

    	String endMonth =
    	    (String) request.getAttribute("endMonth");
%>

<html>
<head>
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>弥生給与データ出力</title>

    <style>
        body {
            margin: 0;
            font-family: "Helvetica Neue", Arial, Meiryo, sans-serif;
            background: #f4f7f6;
            color: #333;
        }

        .main-wrapper {
            max-width: 1300px; /* কলাম বেশি হওয়ায় উইডথ একটু বাড়ানো হয়েছে */
            margin: 0 auto;
            background: white;
            padding: 30px;
            border-radius: 8px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.05);
        }

        .page-header {
            display: flex;
            align-items: center;
            gap: 12px;
            border-bottom: 3px solid #ff9800;
            padding-bottom: 15px;
            margin-bottom: 25px;
        }

        .page-header h1 {
            margin: 0;
            font-size: 24px;
        }

        .badge {
            background: #ff9800;
            color: white;
            font-size: 12px;
            padding: 3px 10px;
            border-radius: 20px;
        }

        .form-container {
            background: #fcfcfc;
            border: 1px solid #eee;
            border-radius: 8px;
            padding: 20px 30px;
            margin-bottom: 20px;
        }

        .form-row {
            display: flex;
            align-items: center;
            gap: 15px;
            flex-wrap: wrap;
        }

        input[type="month"] {
            padding: 8px 12px;
            border: 1px solid #ccc;
            border-radius: 5px;
            font-size: 15px;
        }

        .button-group {
            display: flex;
            gap: 10px;
            margin-top: 15px;
            flex-wrap: wrap;
        }

        .btn {
            padding: 10px 22px;
            border-radius: 5px;
            font-size: 14px;
            font-weight: bold;
            border: none;
            cursor: pointer;
            display: flex;
            align-items: center;
            gap: 8px;
            text-decoration: none;
        }

        .btn-blue {
            background: #007bff;
            color: white;
        }

        .btn-green {
            background: #28a745;
            color: white;
        }

        .btn-grey {
            background: #6c757d;
            color: white;
        }

        /* টেবিলটি যেন স্ক্রিনের বাইরে না চলে যায় তার জন্য রেসপন্সিভ কন্টেইনার */
        .table-container {
            width: 100%;
            overflow-x: auto;
            margin-top: 20px;
        }

        table {
            width: 100%;
            border-collapse: collapse;
            white-space: nowrap; /* কলামের নাম যেন ভেঙে নিচে না নেমে যায় */
        }

        th {
            background: #ff9800;
            color: white;
            padding: 12px 10px;
            text-align: center;
            font-size: 13px;
            border: 1px solid #e08e0b;
        }

        td {
            border: 1px solid #ddd;
            padding: 10px 8px;
            text-align: center;
            font-size: 14px;
        }

        tr:nth-child(even) {
            background: #f9f9f9;
        }
    </style>
</head>

<body>

<div class="main-wrapper">

    <div class="page-header">
        <span style="font-size: 28px;">💹</span>
        <h1>弥生給与データ出力</h1>

        <span class="badge">
            <%= user.getRoleId() == 1 ? "管理者：全従業員" : "一般：自分のみ" %>
        </span>
    </div>

    <div class="form-container">

        <form action="<%= request.getContextPath() %>/YayoiExportServlet"
              method="post">

            <div class="form-row">

              
               <div class="quick-select">
            <label>クイック選択：</label>
            <button type="button" class="quick-btn" onclick="setThisMonth()">今月</button>
            <button type="button" class="quick-btn" onclick="setLastYear()">直近1年間</button>
            <button type="button" class="quick-btn" onclick="setThisYear()">今年度（7月始）</button>
            <button type="button" class="quick-btn" onclick="setLastFiscalYear()">前年度</button>
        </div>
        

<div style="flex-basis: 100%; height: 10px;"></div>

<label>開始年月：</label> 
<input type="month"
       name="startMonth"
       value="<%= startMonth != null ? startMonth : "" %>"
       required>

<label>終了年月：</label>

<input type="month"
       name="endMonth"
       value="<%= endMonth != null ? endMonth : "" %>"
       required>

</div>

            <div class="button-group">

                <button type="submit"
                        name="action"
                        value="preview"
                        class="btn btn-blue">
                    🔍 集計する
                </button>

                <button type="submit"
                        name="action"
                        value="download"
                        class="btn btn-green">
                    📥 CSVダウンロード
                </button>

                <button type="submit"
                        name="action"
                        value="excel"
                        class="btn btn-green"
                        style="background:#217346;">
                    📗 Excelダウンロード
                </button>

                <a href="<%= request.getContextPath() %><%= user.getRoleId() == 1 ? "/AdminMenuServlet" :"/menu" %>"
                   class="btn btn-grey">
                    ← 戻る
                </a>
                

            </div>

        </form>
    </div>

    <%
List<EmpBean> empList =
    (List<EmpBean>) request.getAttribute("empList");

List<MonthlySummaryBean> summaryList =
    (List<MonthlySummaryBean>) request.getAttribute("summaryList");
%>

<% if (empList != null && summaryList != null) { %>

<div style="background:#fff3cd;border:1px solid #ffc107;border-radius:6px;padding:10px 16px;margin-bottom:16px;font-size:13px;">
    <strong>対象期間：</strong><%= startMonth %> 〜 <%= endMonth %>
    &nbsp;&nbsp;
    <strong>対象社員数：</strong><%= empList.size() %>名
</div>

<h2>プレビュー結果</h2>

<div class="table-container">
    <table>
        <tr>
            <th>社員番号</th>
            <th>氏名</th>
            <th>所定労働日数</th>
            <th>出勤日数</th>
            <th>所定労働時間</th>
            <th>実働時間</th>
            <th>特別休暇日数</th>
            <th>有休日数</th>
            <th>欠勤回数</th>
            <th>普通残業時間</th>
            <th>深夜残業時間</th>
            <th>出勤基礎日数</th>
        </tr>

        <% 
            for (int i = 0; i < empList.size(); i++) {
                EmpBean emp = empList.get(i);
                MonthlySummaryBean s = (i < summaryList.size()) ? summaryList.get(i) : null;
                if (s == null) {
                    s = new MonthlySummaryBean();
                }
        %>

        <tr>
            <td><%= emp.getEmpId() %></td>
            
            <td><%= emp.getEmpName() != null ? emp.getEmpName() : "" %></td>
            
            <td><%= s.getTotalWorkDays() %></td>
            
            <td><%= s.getActualAttendanceDays() %></td>
            
            <td><%= s.getTotalWorkDays() * 8 %></td>
            
            <td><%= s.getTotalWorkingHours() %></td>
            
            <td><%= s.getHolidayWorkDays() %></td>
            
            <td><%= s.getPaidLeaveDays() %></td>
            
            <td><%= s.getAbsentDays() %></td>
            
            <td><%= s.getTotalOvertimeHours() %></td>
            
            <td><%= s.getTotalNightHours() %></td>
            
            <td><%= s.getActualAttendanceDays() %></td>
        </tr>

        <% } %>

    </table>
</div>

<% } %>

</div>
<script>

function formatMonth(date) {

    const y = date.getFullYear();
    const m = String(date.getMonth() + 1).padStart(2, '0');

    return y + '-' + m;
}

function setThisMonth() {

    const now = new Date();

    document.getElementsByName("startMonth")[0].value =
        formatMonth(now);

    document.getElementsByName("endMonth")[0].value =
        formatMonth(now);
}

function setLastYear() {

    const end = new Date();
    const start = new Date();

    start.setMonth(start.getMonth() - 11);

    document.getElementsByName("startMonth")[0].value =
        formatMonth(start);

    document.getElementsByName("endMonth")[0].value =
        formatMonth(end);
}

function setThisYear() {
    const now = new Date();
    let year = now.getFullYear();
    // 7月始まり
    if (now.getMonth() + 1 < 7) {
        year--;
    }
    const start = new Date(year, 6, 1); // 7月
    const end = new Date(year + 1, 5, 1); // 翌年6月
    document.getElementsByName("startMonth")[0].value = formatMonth(start);
    document.getElementsByName("endMonth")[0].value = formatMonth(end);
}

function setLastFiscalYear() {

    const now = new Date();

    let year = now.getFullYear();

    if (now.getMonth() + 1 < 4) {
        year--;
    }

    const start = new Date(year - 1, 3, 1);
    const end = new Date(year, 2, 1);

    document.getElementsByName("startMonth")[0].value =
        formatMonth(start);

    document.getElementsByName("endMonth")[0].value =
        formatMonth(end);
}

</script>
</body>
</html>
