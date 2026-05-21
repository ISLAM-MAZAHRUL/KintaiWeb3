<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List" %>
<%@ page import="kintai.EmpBean" %>
<%@ page import="kintai.UserBean" %>
<%
    // ログインチェック
    UserBean user = (UserBean) session.getAttribute("user");
    if (user == null || user.getRoleId() != 1) {
        response.sendRedirect(request.getContextPath() + "/web/login.jsp");
        return;
    }
    
    // リストを取得
    List<EmpBean> deletedEmpList = (List<EmpBean>) request.getAttribute("deletedEmpList");
    String message = (String) request.getAttribute("message");
    Boolean success = (Boolean) request.getAttribute("success");
%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>従業員削除履歴一覧</title>
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
        function confirmRestore(empId, empName) {
            if (confirm('従業員「' + empName + '」を復元してもよろしいですか？')) {
                document.getElementById('restoreForm-' + empId).submit();
            }
        }
    </script>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>従業員削除履歴一覧</h1>
            <a href="<%= request.getContextPath() %>/empManage" class="btn btn-secondary">メニューへ戻る</a>
        </div>
        
        <%-- メッセージ表示 --%>
        <% if (message != null && !message.isEmpty()) { %>
            <div class="message <%= (success != null && success) ? "success-message" : "error-message" %>">
                <%= message %>
            </div>
        <% } %>
        
        <%-- 削除履歴テーブル --%>
        <% if (deletedEmpList != null && !deletedEmpList.isEmpty()) { %>
            <table class="history-table">
                <thead>
                    <tr>
                        <th>従業員ID</th>
                        <th>氏名</th>
                        <th>部署名</th>
                        <th>役職名</th>
                        <th>ロール名</th>
                        <th>退職日</th>
                        <th>操作</th>
                    </tr>
                </thead>
                <tbody>
                    <% for (EmpBean deletedEmp : deletedEmpList) { %>
                        <tr>
                            <td><%= deletedEmp.getEmpId() %></td>
                            <td><%= deletedEmp.getEmpName() %></td>
                            <td><%= deletedEmp.getDeptName() != null ? deletedEmp.getDeptName() : "-" %></td>
                            <td><%= deletedEmp.getPostName() != null ? deletedEmp.getPostName() : "-" %></td>
                            <td><%= deletedEmp.getRoleName() != null ? deletedEmp.getRoleName() : "-" %></td>
                            <td><%= deletedEmp.getLeaveDate() != null ? deletedEmp.getLeaveDate().toString() : "-" %></td>
                            <td>
                                <button class="btn btn-success" onclick="confirmRestore('<%= deletedEmp.getEmpId() %>', '<%= deletedEmp.getEmpName() %>')">復元</button>
                                
                                <%-- 復元用フォーム（非表示） --%>
                                <form id="restoreForm-<%= deletedEmp.getEmpId() %>" method="post" 
                                      action="<%= request.getContextPath() %>/empManage" style="display: none;">
                                    <input type="hidden" name="action" value="restore">
                                    <input type="hidden" name="empId" value="<%= deletedEmp.getEmpId() %>">
                                </form>
                            </td>
                        </tr>
                    <% } %>
                </tbody>
            </table>
        <% } else { %>
            <div class="empty-message">
                削除された従業員データはありません
            </div>
        <% } %>
    </div>
</body>
</html>