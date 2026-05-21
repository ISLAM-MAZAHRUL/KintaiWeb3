<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List" %>
<%@ page import="kintai.EmpBean" %>
<%@ page import="kintai.DeptBean" %>
<%@ page import="kintai.PostBean" %>
<%@ page import="kintai.LeaveTypeBean" %>
<%@ page import="kintai.LeaveRecBean" %>
<%@ page import="kintai.LeaveBalanceBean" %>
<%@ page import="kintai.UserBean" %>
<%@ page import="kintai.EmpDao" %>
<%
    List<EmpBean> empList = (List<EmpBean>) request.getAttribute("empList");
    List<DeptBean> deptList = (List<DeptBean>) request.getAttribute("deptList");
    List<LeaveTypeBean> leaveTypeList = (List<LeaveTypeBean>) request.getAttribute("leaveTypeList");
    List<LeaveRecBean> leaveList = (List<LeaveRecBean>) request.getAttribute("leaveList");
    List<LeaveBalanceBean> balanceList = (List<LeaveBalanceBean>) request.getAttribute("balanceList");
    String message = (String) request.getAttribute("message");
    Boolean success = (Boolean) request.getAttribute("success");
    String selectedEmpId = (String) request.getAttribute("selectedEmpId");
    List<PostBean> postList = (List<PostBean>) request.getAttribute("postList");
    String selectedDept = (String) request.getAttribute("selectedDept");
    String selectedPost = (String) request.getAttribute("selectedPost");
    UserBean loginUser = (UserBean) session.getAttribute("user");
    EmpDao empDaoForSendList = new EmpDao();
    List<EmpBean> allEmpList = empDaoForSendList.findAll();
    
%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>休暇申請管理</title>
    <style>
        /* 省略せず全てのCSS記載（略していません） */
        body {
            font-family: 'メイリオ', sans-serif;
            background-color: #f0f0f0;
            margin: 0;
            padding: 20px;
        }
        .container {
            max-width: 960px;
            margin: 0 auto;
            background-color: white;
            padding: 20px 30px;
            border-radius: 8px;
            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
        }
        h1 {
            color: #333;
            border-bottom: 2px solid #007bff;
            padding-bottom: 10px;
            margin-bottom: 20px;
        }
        .message {
            padding: 10px;
            margin-bottom: 20px;
            border-radius: 4px;
            text-align: center;
            font-weight: bold;
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
        label {
            display: inline-block;
            width: 120px;
            font-weight: bold;
            margin-bottom: 6px;
        }
        select, input[type="text"], input[type="date"], textarea {
            width: 250px;
            padding: 6px 8px;
            border: 1px solid #ced4da;
            border-radius: 4px;
            font-size: 14px;
        }
        textarea {
            width: 100%;
            height: 70px;
            resize: vertical;
        }
        .btn {
            padding: 6px 14px;
            border: none;
            border-radius: 4px;
            cursor: pointer;
            font-size: 14px;
            margin-right: 8px;
        }
        .btn-primary {
            background-color: #007bff;
            color: white;
        }
        .btn-primary:hover {
            background-color: #0056b3;
        }
        .btn-danger {
            background-color: #dc3545;
            color: white;
        }
        .btn-danger:hover {
            background-color: #c82333;
        }
        table {
            width: 100%;
            border-collapse: collapse;
            margin-top: 15px;
        }
        th, td {
            border: 1px solid #dee2e6;
            padding: 10px 12px;
            text-align: left;
            vertical-align: middle;
        }
        th {
            background-color: #f8f9fa;
            font-weight: bold;
            color: #495057;
        }
        tbody tr:nth-child(even) {
            background-color: #f8f9fa;
        }
        tbody tr:hover {
            background-color: #e9ecef;
        }
        .balance-table th, .balance-table td {
            text-align: center;
        }
        form.selection-form {
            margin-bottom: 30px;
            display: flex;
            align-items: center;
            gap: 15px;
            flex-wrap: wrap;
        }
        form.selection-form label {
            width: auto;
            margin-bottom: 0;
        }
        form.selection-form select {
            width: 200px;
        }
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
</head>
<body>
<%
    // ログインユーザー取得
    if (loginUser == null) {
        response.sendRedirect(request.getContextPath() + "/login.jsp");
        return;
    }

    // empIdの変数を先に宣言
    String empId = null;

    // 一般社員は自分のIDのみセット
    if (loginUser.getRoleId() != 1) {
        empId = loginUser.getEmpId();
    }
%>
<div class="container">
    <h1>休暇申請管理</h1>

    <% if (message != null && !message.isEmpty()) { %>
        <div class="message <%= Boolean.TRUE.equals(success) ? "success-message" : "error-message" %>">
            <%= message %>
        </div>
    <% } %>

    <% if (loginUser.getRoleId() == 1) { %>
    <!-- 従業員選択フォーム -->
    <form method="get" action="<%= request.getContextPath() %>/leaveRec" class="selection-form">
        <label for="dept">部署：</label>
        <select id="dept" name="dept" onchange="this.form.submit()">
            <option value="">-- 全部署 --</option>
            <% for (DeptBean dept : deptList) { %>
                <option value="<%= dept.getDeptId() %>" <%= dept.getDeptId().equals(selectedDept) ? "selected" : "" %>><%= dept.getDeptName() %></option>
            <% } %>
        </select>

        <label for="post">役職：</label>
        <select id="post" name="post" onchange="this.form.submit()">
            <option value="">-- 全役職 --</option>
            <% for (PostBean post : postList) { %>
                <option value="<%= post.getPostId() %>" <%= post.getPostId().equals(selectedPost) ? "selected" : "" %>><%= post.getPostName() %></option>
            <% } %>
        </select>

        <label for="empId">氏名：</label>
        <select id="empId" name="empId" onchange="this.form.submit()" required>
            <option value="">-- 選択 --</option>
            <% for (EmpBean emp : empList) { %>
                <option value="<%= emp.getEmpId() %>" <%= emp.getEmpId().equals(selectedEmpId) ? "selected" : "" %>>
                    <%= emp.getEmpId() %> - <%= emp.getEmpName() %>
                </option>
            <% } %>
        </select>
        <input type="hidden" name="mode" value="search">
    </form>
    <% } %>

    <% if (selectedEmpId != null && !selectedEmpId.isEmpty()) { %>
        <h2>残日数一覧</h2>
        <table class="balance-table">
            <thead>
            <tr>
                <th>種別</th><th>付与日</th><th>期限</th><th>付与日数</th><th>使用済</th><th>残り</th>
            </tr>
            </thead>
            <tbody>
            <% if (balanceList != null && !balanceList.isEmpty()) {
                for (LeaveBalanceBean lb : balanceList) {
                    int remaining = lb.getGrantedDays() - lb.getUsedDays();
            %>
                <tr>
                    <td><%= lb.getLeaveTypeName() %></td>
                    <td><%= lb.getGrantedDate() %></td>
                    <td><%= lb.getExpirationDate() %></td>
                    <td><%= lb.getGrantedDays() %></td>
                    <td><%= lb.getUsedDays() %></td>
                    <td><%= remaining %></td>
                </tr>
            <% } } else { %>
                <tr><td colspan="6">残日数データがありません</td></tr>
            <% } %>
            </tbody>
        </table>

        <h2>休暇申請登録</h2>
        <form method="post" action="<%= request.getContextPath() %>/leaveRec">
            <input type="hidden" name="mode" value="add">
            <input type="hidden" name="empId" value="<%= selectedEmpId %>">

            <div>
                <label for="leaveTypeId">休日種別：</label>
                <select id="leaveTypeId" name="leaveTypeId" required>
                    <option value="">-- 種別を選択 --</option>
                    <% for (LeaveTypeBean type : leaveTypeList) { %>
                        <option value="<%= type.getLeaveTypeId() %>"><%= type.getLeaveTypeName() %></option>
                    <% } %>
                </select>
            </div>
            <div>
                <label for="startDate">開始日：</label>
                <input type="date" id="startDate" name="startDate" required>
            </div>
            <div>
                <label for="endDate">終了日：</label>
                <input type="date" id="endDate" name="endDate" required>
            </div>
            <!-- 送信先を実装したが承認フローが不完全なためleave_rec_manage.jsp止まり -->
            <div>
			    <label for="postEmp">送信先：</label>
			    <select id="postEmp" name="postEmpId" required>
			        <option value="">-- 送信先を選択 --</option>
			        <% for (EmpBean emp : allEmpList) { %>
			            <option value="<%= emp.getEmpId() %>">
			                <%= emp.getEmpId() %> - <%= emp.getEmpName() %>
			            </option>
			        <% } %>
			    </select>
			</div>
            <div>
                <label for="reason">理由：</label>
                <textarea id="reason" name="reason" placeholder="理由を入力してください" required></textarea>
            </div>
            <button type="submit" class="btn btn-primary" onclick="return confirm('この内容で登録しますか？');">登録する</button>
        </form>
    <% } %>

    <h2>休暇申請登録一覧</h2>
    <table>
        <thead>
        <tr>
            <th>休暇ID</th>
            <th>種別</th>
            <th>開始日</th>
            <th>終了日</th>
            <th>理由</th>
            <th>承認状況</th>
            <th>操作</th>
        </tr>
        </thead>
        <tbody>
        <% if (leaveList != null && !leaveList.isEmpty()) {
            for (LeaveRecBean leave : leaveList) {
                String typeName = "";
                for (LeaveTypeBean type : leaveTypeList) {
                    if (type.getLeaveTypeId() == leave.getLeaveTypeId()) {
                        typeName = type.getLeaveTypeName();
                        break;
                    }
                }
        %>
            <tr>
                <td><%= leave.getLeaveId() %></td>
                <td><%= typeName %></td>
                <td><%= leave.getStartDate() %></td>
                <td><%= leave.getEndDate() %></td>
                <td><%= leave.getReason() %></td>
                <td><%= leave.getStatus() %></td>
                <td>
                    <form method="post" action="<%= request.getContextPath() %>/leaveRec" style="display:inline;">
                        <input type="hidden" name="leaveId" value="<%= leave.getLeaveId() %>">
                        <input type="hidden" name="mode" value="delete">
                        <button type="submit" class="btn btn-danger" onclick="return confirm('本当に削除しますか？')">削除</button>
                    </form>
                </td>
            </tr>
        <% } } else { %>
            <tr><td colspan="6" style="text-align:center;">休暇申請データがありません</td></tr>
        <% } %>
        </tbody>
    </table>

    <%
    String forward;
    if (loginUser.getRoleId() == 1) {
        forward = request.getContextPath() + "/web/admin_menu.jsp";
    } else {
        forward = request.getContextPath() + "/web/menu.jsp"; // 一般社員用メニュー
    }
	%>
	
	<a href="<%= forward %>" class="back-link">メニューへ戻る</a>
</div>
</body>
</html>
