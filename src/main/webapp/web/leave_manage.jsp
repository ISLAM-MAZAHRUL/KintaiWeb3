<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List" %>
<%@ page import="kintai.LeaveTypeBean" %>
<%@ page import="kintai.UserBean" %>
<%
    UserBean user = (UserBean) session.getAttribute("user");
    if (user == null || user.getRoleId() != 1) {
        response.sendRedirect(request.getContextPath() + "/web/login.jsp");
        return;
    }

    List<LeaveTypeBean> leaveTypeList = (List<LeaveTypeBean>) request.getAttribute("leaveTypeList");
    String message = (String) request.getAttribute("message");
    Boolean success = (Boolean) request.getAttribute("success");
    List<Integer> protectedIds = java.util.Arrays.asList(1, 2, 3, 11, 12, 20, 21, 22, 23, 30, 40, 41);
%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>休日種別管理</title>
    <style>
        body {
            font-family: 'メイリオ', sans-serif;
            background-color: #f0f0f0;
            margin: 0; padding: 20px;
        }

        .container {
            max-width: 900px;
            margin: auto;
            background: white;
            padding: 20px;
            border-radius: 8px;
            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
        }

        h1 {
            color: #333;
            border-bottom: 2px solid #007bff;
            padding-bottom: 10px;
        }

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

        .add-form {
            background-color: #f8f9fa;
            padding: 20px;
            margin-bottom: 30px;
            border-radius: 4px;
            border: 1px solid #dee2e6;
        }

        .form-group {
            margin-bottom: 15px;
        }

        label {
            display: inline-block;
            width: 120px;
            font-weight: bold;
        }

        input[type="text"], select {
            width: 200px;
            padding: 5px;
            border: 1px solid #ced4da;
            border-radius: 4px;
        }

        .btn {
            padding: 6px 12px;
            border: none;
            border-radius: 4px;
            cursor: pointer;
            font-size: 14px;
            margin-right: 5px;
            color: white;
        }

        .btn-primary { background-color: #007bff; }
        .btn-primary:hover { background-color: #0056b3; }
        .btn-success { background-color: #28a745; }
        .btn-success:hover { background-color: #218838; }
        .btn-danger { background-color: #dc3545; }
        .btn-danger:hover { background-color: #c82333; }
        .btn-secondary { background-color: #6c757d; }
        .btn-secondary:hover { background-color: #545b62; }

        table {
            width: 100%;
            border-collapse: collapse;
            margin-top: 20px;
        }

        th, td {
            border: 1px solid #dee2e6;
            padding: 10px;
            text-align: left;
        }

        th {
            background-color: #f8f9fa;
            font-weight: bold;
            color: #495057;
        }

        tr:nth-child(even) {
            background-color: #f8f9fa;
        }

        tr:hover {
            background-color: #e9ecef;
        }

        .edit-form {
            display: none;
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
    <script>
	    function toggleEdit(id) {
	        const row = document.getElementById("row-" + id);
	        const editRow = document.getElementById("editRow-" + id);
	        if (editRow.style.display === "none") {
	            row.style.display = "none";
	            editRow.style.display = "";
	        } else {
	            row.style.display = "";
	            editRow.style.display = "none";
	        }
	    }

        function confirmAdd(form) {
            const name = form.leaveTypeName.value;
            const id = form.leaveTypeId.value;
            if (id.trim() === '' || name.trim() === '') {
                alert('IDと名称を入力してください。');
                return false;
            }
            return confirm('休日種別「' + name + '」を追加してもよろしいですか？');
        }

        function confirmUpdate(form, id) {
            const name = form.leaveTypeName.value;
            if (name.trim() === '') {
                alert('名称を入力してください。');
                return false;
            }
            return confirm('ID「' + id + '」を更新してもよろしいですか？');
        }

        function confirmDelete(id, name) {
            if (confirm('休日種別「' + name + '」を削除してもよろしいですか？')) {
                document.getElementById('deleteForm-' + id).submit();
            }
        }

     	// 復元確認
        function confirmRestore(id, name) {
            if (confirm('部署「' + name + '」を復元してもよろしいですか？')) {
                document.getElementById('restoreForm-' + id).submit();
            }
        }
    </script>
</head>
<body>
<div class="container">
    <h1>休日種別管理</h1>

    <% if (message != null && !message.isEmpty()) { %>
        <div class="message <%= (success != null && success) ? "success-message" : "error-message" %>">
            <%= message %>
        </div>
    <% } %>
    
    <%-- 削除履歴リンク --%>
    <div style="margin-bottom: 20px; text-align: right;">
        <a href="<%= request.getContextPath() %>/leaveTypeManage?action=history" class="btn btn-secondary">過去削除履歴一覧</a>
    </div>

    
    <%-- 新規追加フォーム --%>
    <div class="add-form">
        <h2>新規休日種別追加</h2>
        <form method="post" action="<%= request.getContextPath() %>/leaveTypeManage" onsubmit="return confirmAdd(this)">
            <input type="hidden" name="action" value="add">
            <div class="form-group">
                <label>種別ID：</label>
                <input type="number" name="leaveTypeId" maxlength="5" required>
            </div>
            <div class="form-group">
                <label>種別名：</label>
                <input type="text" name="leaveTypeName" maxlength="50" required>
            </div>
            <div class="form-group">
                <label>有給フラグ：</label>
                <select name="isPaid" required>
                    <option value="true">有給</option>
                    <option value="false">無給</option>
                </select>
            </div>
            <button type="submit" class="btn btn-primary">追加</button>
        </form>
    </div>

    <h2>休日種別一覧</h2>
    <table>
        <thead>
        <tr>
            <th>ID</th>
            <th>休日種別名</th>
            <th>有給/無給</th>
            <th>操作</th>
        </tr>
        </thead>
        <tbody>
        
<% if (leaveTypeList != null && !leaveTypeList.isEmpty()) {
       for (LeaveTypeBean lt : leaveTypeList) {
           boolean isProtected = protectedIds.contains(lt.getLeaveTypeId());
%>
<tr id="row-<%= lt.getLeaveTypeId() %>">
    <td><%= lt.getLeaveTypeId() %></td>
    <td><%= lt.getLeaveTypeName() %></td>
    <td><%= lt.isPaid() ? "有給" : "無給" %></td>
    <td>
        <% if (!isProtected) { %>
            <button class="btn btn-success" onclick="toggleEdit('<%= lt.getLeaveTypeId() %>')">編集</button>
            <button class="btn btn-danger" onclick="confirmDelete('<%= lt.getLeaveTypeId() %>', '<%= lt.getLeaveTypeName() %>')">削除</button>
            <form id="deleteForm-<%= lt.getLeaveTypeId() %>" method="post" action="<%= request.getContextPath() %>/leaveTypeManage" style="display:none;">
                <input type="hidden" name="action" value="delete">
                <input type="hidden" name="leaveTypeId" value="<%= lt.getLeaveTypeId() %>">
            </form>
        <% } else { %>
            （固定）
        <% } %>
    </td>
</tr>

<%-- 編集フォーム（初期は非表示） --%>
<tr id="editRow-<%= lt.getLeaveTypeId() %>" style="display:none;">
    <td colspan="4">
        <form method="post" action="<%= request.getContextPath() %>/leaveTypeManage"
        
              onsubmit="return confirmUpdate(this, '<%= lt.getLeaveTypeId() %>')">
            <input type="hidden" name="action" value="update">
            <input type="hidden" name="originalLeaveTypeId" value="<%= lt.getLeaveTypeId() %>">
            <input type="number" name="leaveTypeId" value="<%= lt.getLeaveTypeId() %>" required>
            <input type="text" name="leaveTypeName" value="<%= lt.getLeaveTypeName() %>" required>
            <select name="isPaid">
                <option value="true" <%= lt.isPaid() ? "selected" : "" %>>有給</option>
                <option value="false" <%= !lt.isPaid() ? "selected" : "" %>>無給</option>
            </select>
            <button type="submit" class="btn btn-primary">保存</button>
            <button type="button" class="btn btn-secondary" onclick="toggleEdit('<%= lt.getLeaveTypeId() %>')">キャンセル</button>
        </form>
    </td>
</tr>
<% } } else { %>
<tr><td colspan="4" style="text-align:center;">データがありません</td></tr>
<% } %>
</tbody>

    </table>
    
    <!-- 削除された休日種別の復元セクション -->
        <%
            List<LeaveTypeBean> deletedLeaveTypeList = (List<LeaveTypeBean>) request.getAttribute("deletedLeaveTypeList");
            Boolean hideRestoreSection = (Boolean) session.getAttribute("hideRestoreSection");
            if (deletedLeaveTypeList != null && !deletedLeaveTypeList.isEmpty() && (hideRestoreSection == null || !hideRestoreSection)) {
                // 復元セクションを表示したことをセッションに記録
                session.setAttribute("hideRestoreSection", true);
        %>
        <div class="add-form" style="margin-top: 30px;">
            <h2>誤削除データの復元</h2>
            <table>
                <thead>
                    <tr>
                        <th>ID</th>
                        <th>休日種別名</th>
                        <th>有給/無給</th>
                        <th>操作</th>
                    </tr>
                </thead>
                <tbody>
                    <% for (LeaveTypeBean deletedLeaveType : deletedLeaveTypeList) { %>
                        <tr>
                            <td><%= deletedLeaveType.getLeaveTypeId() %></td>
                            <td><%= deletedLeaveType.getLeaveTypeName() %></td>
                            <td>
                                <button class="btn btn-success" onclick="confirmRestore('<%= deletedLeaveType.getLeaveTypeId() %>', '<%= deletedLeaveType.getLeaveTypeName() %>')">復元</button>
                                
                                <%-- 復元用フォーム（非表示） --%>
                                <form id="restoreForm-<%= deletedLeaveType.getLeaveTypeId() %>" method="post" 
                                      action="<%= request.getContextPath() %>/leaveTypeManage" style="display: none;">
                                    <input type="hidden" name="action" value="restore">
                                    <input type="hidden" name="leaveTypeId" value="<%= deletedLeaveType.getLeaveTypeId() %>">
                                </form>
                            </td>
                        </tr>
                    <% } %>
                </tbody>
            </table>
        </div>
        <% } %>

    <a href="<%= request.getContextPath() %>/AdminMenuServlet" class="back-link">メニューへ戻る</a>
</div>
</body>
</html>
