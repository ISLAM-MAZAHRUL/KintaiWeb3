<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List" %>
<%@ page import="kintai.EmpBean" %>
<%@ page import="kintai.DeptBean" %>
<%@ page import="kintai.PostBean" %>
<%@ page import="kintai.WorkTimeDao" %>
<%@ page import="kintai.WorkTimeBean" %>
<%
	List<EmpBean> empList = (List<EmpBean>) request.getAttribute("empList");
	List<DeptBean> deptList = (List<DeptBean>) request.getAttribute("deptList");
	String selectedEmpId = (String) request.getAttribute("selectedEmpId");
    List<PostBean> postList = (List<PostBean>) request.getAttribute("postList");
    String selectedDept = (String) request.getAttribute("selectedDept");
    String selectedPost = (String) request.getAttribute("selectedPost");
    List<WorkTimeBean> workTime = (List<WorkTimeBean>) request.getAttribute("workTime");
	
%>
<!DOCTYPE html>
<html>
<head>
	<meta charset="UTF-8">
	<title>勤務時間更新ログ</title>
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
<div class="container">
    <h1>勤務時間更新ログ</h1>

	<!-- 従業員選択フォーム -->
	    <form method="get" action="<%= request.getContextPath() %>/kintaiLog" class="selection-form">
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
	    
	     <% if (selectedEmpId != null && !selectedEmpId.isEmpty()) { %>
        <h2>勤務時間更新ログ一覧</h2>
        <table class="balance-table">
            <thead>
            <tr>
                <th>勤務日</th><th>出勤</th><th>退勤</th><th>作成日時</th><th>更新日時</th>
            </tr>
            </thead>
            <tbody>
            
            <% if (workTime != null && !workTime.isEmpty()) {
                for (WorkTimeBean lb : workTime) {
            %>
                <tr>
                    <td><%= lb.getKintaiDate() %></td>
                    <td><%= lb.getClockIn() %></td>
                    <td><%= lb.getClockOut() %></td>
                    <td><%= lb.getCreatedAt() %></td>
                    <td><%= lb.getUpdatedAt() %></td>

                </tr>
                
            <% } } else { %>
                <tr><td colspan="5">勤務時間更新ログがありません</td></tr>
            <% } %>
            </tbody>
        </table>
        <% } %>
        <a href="<%= request.getContextPath() %>/AdminMenuServlet" class="back-link">メニューへ戻る</a>
</div>
</body>
</html>