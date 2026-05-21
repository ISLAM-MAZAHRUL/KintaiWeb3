<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List" %>
<%@ page import="java.text.SimpleDateFormat" %>
<%@ page import="kintai.LeaveTypeBean" %>
<%@ page import="kintai.UserBean" %>
<%
    // ログインチェック
    UserBean user = (UserBean) session.getAttribute("user");
    if (user == null || user.getRoleId() != 1) {
        response.sendRedirect(request.getContextPath() + "/web/login.jsp");
        return;
    }
    
    // リストを取得
    List<LeaveTypeBean> deletedLeaveTypeList = (List<LeaveTypeBean>) request.getAttribute("deletedLeaveTypeList");
    String message = (String) request.getAttribute("message");
    Boolean success = (Boolean) request.getAttribute("success");
    
 	// 日時フォーマット用
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
%>
<!DOCTYPE html>
<html>
<head>
	<meta charset="UTF-8">
	<title>休日種別削除履歴一覧</title>
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
            align-items: center;
            margin-bottom: 30px;
            padding-bottom: 15px;
            border-bottom: 2px solid #007bff;
        }
        
        h1 {
            color: #333;
            margin: 0;
            font-size: 1.8em;
        }
        
        /* メッセージ表示エリア */
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
        
        /* ボタンスタイル */
        .btn {
            padding: 8px 16px;
            border: none;
            border-radius: 4px;
            cursor: pointer;
            font-size: 14px;
            margin-right: 5px;
            text-decoration: none;
            display: inline-block;
        }
        
        .btn-primary {
            background-color: #007bff;
            color: white;
        }
        
        .btn-primary:hover {
            background-color: #0056b3;
        }
        
        .btn-success {
            background-color: #28a745;
            color: white;
        }
        
        .btn-success:hover {
            background-color: #218838;
        }
        
        .btn-secondary {
            background-color: #6c757d;
            color: white;
        }
        
        .btn-secondary:hover {
            background-color: #545b62;
        }
        
        /* テーブルスタイル */
        .history-table {
            width: 100%;
            border-collapse: collapse;
            margin-top: 20px;
        }
        
        .history-table th, .history-table td {
            border: 1px solid #dee2e6;
            padding: 12px;
            text-align: left;
        }
        
        .history-table th {
            background: linear-gradient(135deg, #007bff 0%, #0056b3 100%);
            color: white;
            font-weight: bold;
            border-bottom: 3px solid #fd7e14;
        }
        
        .history-table tr:nth-child(even) {
            background-color: #f8f9fa;
        }
        
        .history-table tr:hover {
            background-color: #e9ecef;
            transform: scale(1.01);
            transition: all 0.2s ease;
        }
        
        .empty-message {
            text-align: center;
            padding: 40px;
            color: #6c757d;
            font-style: italic;
        }
	</style>
	<script>
		// 復元確認
	    function confirmRestore(leaveTypeId, leaveTypeName) {
	        if (confirm('休日「' + leaveTypeName + '」を復元してもよろしいですか？')) {
	            document.getElementById('restoreForm-' + leaveTypeId).submit();
	        }
	    }
	</script>
</head>
<body>
	<div class="container">
		<div class="header">
			<h1>休日種別削除履歴一覧</h1>
			<a href="<%= request.getContextPath() %>/leaveTypeManage" class="btn btn-secondary">メニューへ戻る</a>
		</div>
		
		<%-- メッセージ表示 --%>
        <% if (message != null && !message.isEmpty()) { %>
            <div class="message <%= (success != null && success) ? "success-message" : "error-message" %>">
                <%= message %>
            </div>
        <% } %>
        
        <%-- 削除履歴テーブル --%>
        <% if (deletedLeaveTypeList != null && !deletedLeaveTypeList.isEmpty()) { %>
            <table class="history-table">
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
                            <td><%= deletedLeaveType.isPaid() ? "有給" : "無給" %></td>
<!--                            <td><%= deletedLeaveType.getDeletedBy() != null ? deletedLeaveType.getDeletedBy() : "-" %></td>-->
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
        <% } else { %>
            <div class="empty-message">
                削除された休日種別データはありません
            </div>
        <% } %>
	</div>
</body>
</html>