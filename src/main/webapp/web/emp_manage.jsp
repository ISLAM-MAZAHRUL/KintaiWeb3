<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List" %>
<%@ page import="kintai.EmpBean" %>
<%@ page import="kintai.DeptBean" %>
<%@ page import="kintai.PostBean" %>
<%@ page import="kintai.RoleBean" %>    <%-- 新規追加 --%>
<%@ page import="kintai.UserBean" %>
<jsp:useBean id="today" class="java.util.Date" />
<%
    // ログインチェック
    UserBean user = (UserBean) session.getAttribute("user");
if (user == null) { // getRole()からgetRoleId()へ変更
    response.sendRedirect(request.getContextPath() + "/web/login.jsp");
    return;
}
    
    // リストを取得
    List<EmpBean> empList = (List<EmpBean>) request.getAttribute("empList");
    List<DeptBean> deptList = (List<DeptBean>) request.getAttribute("deptList");
    List<PostBean> postList = (List<PostBean>) request.getAttribute("postList");
    List<RoleBean> roleList = (List<RoleBean>) request.getAttribute("roleList");    // 新規追加
    String message = (String) request.getAttribute("message");
    Boolean success = (Boolean) request.getAttribute("success");

    // nullチェック
    if (empList == null) empList = new java.util.ArrayList<>();
    if (deptList == null) deptList = new java.util.ArrayList<>();
    if (postList == null) postList = new java.util.ArrayList<>();
    if (roleList == null) roleList = new java.util.ArrayList<>();
%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>従業員管理</title>
    <style>
        body {
            font-family: 'メイリオ', sans-serif;
            background-color: #f0f0f0;
            margin: 0;
            padding: 20px;
        }
        
        .container {
            max-width: 1400px; /* 幅を少し広めに調整 */
            margin: 0 auto;
            background-color: white;
            padding: 20px;
            border-radius: 8px;
            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
        }
        
        h1 {
            color: #333;
            border-bottom: 2px solid #007bff;
            padding-bottom: 10px;
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
        
        /* 新規追加フォーム */
        .add-form {
            background-color: #f8f9fa;
            padding: 20px;
            margin-bottom: 30px;
            border-radius: 4px;
            border: 1px solid #dee2e6;
        }
        
        .add-form h2 {
            margin-top: 0;
            color: #495057;
        }
        
        .form-group {
            margin-bottom: 15px;
            display: inline-block;
            margin-right: 20px;
            vertical-align: top; /* フォーム要素の上下位置を揃える */
        }
        
        .form-group label {
            display: block; /* ラベルをブロック要素にして、入力フィールドの上に配置 */
            width: auto; /* 幅を自動調整 */
            font-weight: bold;
            margin-bottom: 5px;
        }
        
        .form-group input[type="text"],
        .form-group input[type="password"],
        .form-group input[type="email"], /* 新規追加 */
        .form-group input[type="date"],   /* 新規追加 */
        .form-group select {
            width: 180px; /* 幅を調整 */
            padding: 5px;
            border: 1px solid #ced4da;
            border-radius: 4px;
        }
        
        /* ボタンスタイル */
        .btn {
            padding: 6px 12px;
            border: none;
            border-radius: 4px;
            cursor: pointer;
            font-size: 14px;
            margin-right: 5px;
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
        
        .btn-danger {
            background-color: #dc3545;
            color: white;
        }
        
        .btn-danger:hover {
            background-color: #c82333;
        }
        
        .btn-secondary {
            background-color: #6c757d;
            color: white;
        }
        
        .btn-secondary:hover {
            background-color: #545b62;
        }
        
        /* テーブルスタイル */
        .emp-table {
            width: 100%;
            border-collapse: collapse;
            margin-top: 20px;
        }
        
        .emp-table th, .emp-table td {
            border: 1px solid #dee2e6;
            padding: 10px;
            text-align: left;
            vertical-align: middle; /* セル内容を中央揃え */
        }
        
        .emp-table th {
            background-color: #f8f9fa;
            font-weight: bold;
            color: #495057;
        }
        
        .emp-table tr:nth-child(even) {
            background-color: #f8f9fa;
        }
        
        .emp-table tr:hover {
            background-color: #e9ecef;
        }
        
        /* 編集フォーム */
        .edit-row {
            display: none;
        }
        
        .edit-row input[type="text"],
        .edit-row input[type="password"],
        .edit-row input[type="email"], /* 新規追加 */
        .edit-row input[type="date"],   /* 新規追加 */
        .edit-row select {
            width: 120px; /* 編集行の入力フィールドの幅 */
            padding: 3px;
            border: 1px solid #ced4da;
            border-radius: 4px;
            margin-right: 5px; /* 要素間のスペース */
        }
        
        /* 戻るボタン */
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
        
        .form-inline {
            display: flex; /* 横並びにするためにflexを使用 */
            flex-wrap: wrap; /* 必要に応じて折り返す */
            align-items: center;
            gap: 10px; /* 要素間のスペース */
        }
        .form-inline label {
             font-weight: bold;
             flex-shrink: 0; /* ラベルが縮まないように */
        }
    </style>
    <script>
        // 編集モードの切り替え
        function toggleEdit(empNo) {
            var displayRow = document.getElementById('display-' + empNo);
            var editRow = document.getElementById('edit-' + empNo);
            
            if (editRow.style.display === 'table-row') {
                displayRow.style.display = 'table-row';
                editRow.style.display = 'none';
            } else {
                displayRow.style.display = 'none';
                editRow.style.display = 'table-row';
            }
        }
        
        // 削除確認
        function confirmDelete(empNo, empName) {
            if (confirm('従業員「' + empName + '」を削除してもよろしいですか？')) {
                document.getElementById('deleteForm-' + empNo).submit();
            }
        }

        // 追加確認
        function confirmAdd() {
            return confirm('この内容で新しい従業員を追加してもよろしいですか？');
        }

        // 変更確認
        function confirmUpdate(empName) {
            return confirm('従業員「' + empName + '」の情報を変更してもよろしいですか？');
        }
    </script>
</head>
<body>
    <div class="container">
    <% if (user.getRoleId() == 1) { %>
        <h1>従業員管理</h1>
    <% } else { %>
    	<h1>従業員一覧</h1>
    <% } %>
        
        <%-- メッセージ表示 --%>
        <% if (message != null && !message.isEmpty()) { %>
            <div class="message <%= (success != null && success) ? "success-message" : "error-message" %>">
                <%= message %>
            </div>
        <% } %>
        
        <%-- 削除履歴リンク --%>
        <% if (user.getRoleId() == 1) { %>
        <div style="margin-bottom: 20px; text-align: right;">
            <a href="<%= request.getContextPath() %>/empManage?action=history" class="btn btn-secondary">過去削除履歴一覧</a>
        </div>
        
        <%-- 新規追加フォーム --%>
        <div class="add-form">
            <h2>新規従業員追加</h2>
            <form method="post" action="<%= request.getContextPath() %>/empManage" onsubmit="return confirmAdd();">
                <input type="hidden" name="action" value="add">
                <div class="form-group">
                    <label for="newEmpNo">従業員番号：</label>
                    <input type="text" id="newEmpId" name="empId" maxlength="10" required>
                </div>
                <div class="form-group">
                    <label for="newEmpName">氏名：</label>
                    <input type="text" id="newEmpName" name="empName" maxlength="50" required>
                </div>
                <div class="form-group">
                    <label for="newDeptNo">部署：</label>
                    <select id="newDeptId" name="deptId" required>
                        <option value="">選択してください</option>
                        <% if (deptList != null) { 
                            for (DeptBean dept : deptList) { %>
                                <option value="<%= dept.getDeptId() %>"><%= dept.getDeptName() %></option>
                        <% }} %>
                    </select>
                </div>
                <div class="form-group">
                    <label for="newPostNo">役職：</label>
                    <select id="newPostId" name="postId" required>
                        <option value="">選択してください</option>
                        <% if (postList != null) { 
                            for (PostBean post : postList) { %>
                                <option value="<%= post.getPostId() %>"><%= post.getPostName() %></option>
                        <% }} %>
                    </select>
                </div>
                <div class="form-group">
                    <label for="newRoleId">権限：</label> <%-- 旧roleからroleIdへ変更 --%>
                    <select id="newRoleId" name="roleId" required> <%-- 旧roleからroleIdへ変更 --%>
                        <option value="">選択してください</option>
                        <% if (roleList != null) { 
                            for (RoleBean role : roleList) { %>
                                <option value="<%= role.getRoleId() %>"><%= role.getRoleName() %></option>
                        <% }} %>
                    </select>
                </div>
                <div class="form-group">
                    <label for="newEmpType">社員種別：</label>
                    <select id="newEmpType" name="empType" required>
                        <option value="">選択してください</option>
                        <option value="正社員">正社員</option>
                        <option value="契約社員">契約社員</option>
                        <option value="アルバイト">アルバイト</option>
                        <option value="派遣">派遣</option>
                    </select>
                </div>
                <div class="form-group">
                    <label for="newPass">パスワード：</label>
                    <input type="password" id="newPass" name="pass" maxlength="64" required> <%-- maxlengthを64に増やす --%>
                </div>
                <div class="form-group">
                    <label for="newMail">メールアドレス：</label> <%-- 新規追加 --%>
                    <input type="email" id="newMail" name="mail" maxlength="100"> <%-- 新規追加 --%>
                </div>
                <div class="form-group">
                    <label for="newEmpDate">入社年月日：</label> <%-- 新規追加 --%>
                    <input type="date" id="newEmpDate" name="empDate" value="<%= new java.text.SimpleDateFormat("yyyy-MM-dd").format(today) %>"> <%-- 新規追加 --%>
                </div>
                <div style="margin-top: 15px;">
                    <button type="submit" class="btn btn-primary">追加</button>
                </div>
            </form>
        </div>
        <% } %>
        
        <%-- 従業員一覧テーブル --%>
        <h2>従業員一覧</h2>
        <table class="emp-table">
            <thead>
                <tr>
                    <th>従業員番号</th>
                    <th>氏名</th>
                    <th>部署</th>
                    <th>役職</th>
                    <th>権限</th> <%-- 旧権限から変更 --%>
                    <th>社員種別</th> <%-- 新規追加 --%>
                    <th>メールアドレス</th> <%-- 新規追加 --%>
                    <th>入社年月日</th> <%-- 新規追加 --%>
                    <% if (user.getRoleId() == 1) { %>
                    <th>操作</th>
                    <% } %>
                </tr>
            </thead>
            <tbody>
                <% if (empList != null && !empList.isEmpty()) { %>
                    <% for (EmpBean emp : empList) { %>
                        <%-- 表示行 --%>
                        <tr id="display-<%= emp.getEmpId() %>">
                            <td><%= emp.getEmpId() %></td>
                            <td><%= emp.getEmpName() %></td>
                            <%-- 部署名の表示（nullの場合の処理） --%>
                            <td><%= (emp.getDeptName() != null) ? emp.getDeptName() : "情報なし" %></td>
                            <%-- 役職名の表示（nullの場合の処理） --%>
                            <td><%= (emp.getPostName() != null) ? emp.getPostName() : "情報なし" %></td>
                            <%-- 権限名の表示（emp.getRole()からemp.getRoleName()へ変更） --%>
                            <td><%= (emp.getRoleName() != null) ? emp.getRoleName() : "情報なし" %></td>
                            <%-- 社員種別名の表示（新規追加） --%>
                            <td><%= (emp.getEmpType() != null) ? emp.getEmpType() : "情報なし" %></td>
                            <%-- メールアドレスの表示（新規追加） --%>
                            <td><%= (emp.getMail() != null) ? emp.getMail() : "---" %></td>
                            <%-- 入社年月日の表示（新規追加） --%>
                            <td><%= (emp.getEmpDate() != null) ? emp.getEmpDate().toString() : "---" %></td>
                            <% if (user.getRoleId() == 1) { %>
                            <td>
                                <button class="btn btn-success" onclick="toggleEdit('<%= emp.getEmpId() %>')">編集</button>
                                <button class="btn btn-danger" onclick="confirmDelete('<%= emp.getEmpId() %>', '<%= emp.getEmpName() %>')">削除</button>
                                
                                <%-- 削除用フォーム（非表示） --%>
                                <form id="deleteForm-<%= emp.getEmpId() %>" method="post" 
                                      action="<%= request.getContextPath() %>/empManage" style="display: none;">
                                    <input type="hidden" name="action" value="delete">
                                    <input type="hidden" name="empId" value="<%= emp.getEmpId() %>">
                                </form>
                            </td>
                            <% } %>
                        </tr>
                        
                        <%-- 編集行（初期状態では非表示） --%>
                        <tr id="edit-<%= emp.getEmpId() %>" class="edit-row" style="display: none;">
                            <td><%= emp.getEmpId() %></td>
                            <td colspan="8"> <%-- 列数を調整 --%>
                                <form method="post" action="<%= request.getContextPath() %>/empManage" class="form-inline" onsubmit="return confirmUpdate('<%= emp.getEmpName() %>');">
                                    <input type="hidden" name="action" value="update">
                                    <input type="hidden" name="empId" value="<%= emp.getEmpId() %>">
                                    
                                    <label>氏名：</label><input type="text" name="empName" value="<%= emp.getEmpName() %>" maxlength="50" required>
                                    
                                    <label>部署：</label>
                                    <select name="deptId" required>
                                        <option value="">選択してください</option>
                                        <% if (deptList != null) { 
                                            for (DeptBean dept : deptList) { %>
                                                <option value="<%= dept.getDeptId() %>" 
                                                    <%= dept.getDeptId().equals(emp.getDeptId()) ? "selected" : "" %>>
                                                    <%= dept.getDeptName() %>
                                                </option>
                                        <% }} %>
                                    </select>
                                    
                                    <label>役職：</label>
                                    <select name="postId" required>
                                        <option value="">選択してください</option>
                                        <% if (postList != null) { 
                                            for (PostBean post : postList) { %>
                                                <option value="<%= post.getPostId() %>" 
                                                    <%= post.getPostId().equals(emp.getPostId()) ? "selected" : "" %>>
                                                    <%= post.getPostName() %>
                                                </option>
                                        <% }} %>
                                    </select>
                                    
                                    <label>権限：</label> <%-- 旧権限から変更 --%>
                                    <select name="roleId" required> <%-- 旧roleからroleIdへ変更 --%>
                                        <option value="">選択してください</option>
                                        <% if (roleList != null) { 
                                            for (RoleBean role : roleList) { %>
                                                <option value="<%= role.getRoleId() %>" 
                                                    <%= role.getRoleId() == emp.getRoleId() ? "selected" : "" %>> <%-- == で比較 --%>
                                                    <%= role.getRoleName() %>
                                                </option>
                                        <% }} %>
                                    </select>

                                    <label>社員種別：</label>
                                    <select name="empType" required>
                                        <option value="">選択してください</option>
                                        <option value="正社員" <%= "正社員".equals(emp.getEmpType()) ? "selected" : "" %>>正社員</option>
                                        <option value="契約社員" <%= "契約社員".equals(emp.getEmpType()) ? "selected" : "" %>>契約社員</option>
                                        <option value="アルバイト" <%= "アルバイト".equals(emp.getEmpType()) ? "selected" : "" %>>アルバイト</option>
                                        <option value="派遣" <%= "派遣".equals(emp.getEmpType()) ? "selected" : "" %>>派遣</option>
                                    </select>
                                    
                                    <label>パスワード：</label><input type="password" name="pass" value="<%= emp.getPass() %>" maxlength="64" required> <%-- maxlengthを64に増やす --%>
                                    
                                    <label>メールアドレス：</label><input type="email" name="mail" value="<%= emp.getMail() != null ? emp.getMail() : "" %>" maxlength="100"> <%-- 新規追加 --%>
                                    
                                    <label>入社年月日：</label><input type="date" name="empDate" value="<%= emp.getEmpDate() != null ? emp.getEmpDate().toString() : "" %>"> <%-- 新規追加 --%>
                                    
                                    <button type="submit" class="btn btn-primary">保存</button>
                                    <button type="button" class="btn btn-secondary" onclick="toggleEdit('<%= emp.getEmpId() %>')">キャンセル</button>
                                </form>
                            </td>
                        </tr>
                    <% } %>
                <% } else { %>
                    <tr>
                        <td colspan="9" style="text-align: center;">従業員データがありません</td> <%-- 列数を調整 --%>
                    </tr>
                <% } %>
            </tbody>
        </table>
        
        <!-- 削除された従業員の復元セクション -->
        <% if (user.getRoleId() == 1) { %>
        <%
            List<EmpBean> deletedEmpList = (List<EmpBean>) request.getAttribute("deletedEmpList");
            Boolean hideRestoreSection = (Boolean) session.getAttribute("hideRestoreSection");
            if (deletedEmpList != null && !deletedEmpList.isEmpty() && (hideRestoreSection == null || !hideRestoreSection)) {
                // 復元セクションを表示したことをセッションに記録
                session.setAttribute("hideRestoreSection", true);
        %>
        <div class="add-form" style="margin-top: 30px;">
            <h2>誤削除データの復元</h2>
            <table>
                <thead>
                    <tr>
                        <th>従業員ID</th>
                        <th>氏名</th>
                        <th>部署名</th>
                        <th>役職名</th>
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
        </div>
        <% } %>
        <% } %>
        
    <%
	    String forward;
	    if (user.getRoleId() == 1) {
	        forward = request.getContextPath() + "/web/admin_menu.jsp";
	    } else {
	        forward = request.getContextPath() + "/web/menu.jsp"; // 一般社員用メニュー
	    }
	%>
	
	<a href="<%= forward %>" class="back-link">メニューへ戻る</a>
    </div>
    
    <script>
        // 復元確認
        function confirmRestore(empId, empName) {
            if (confirm('従業員「' + empName + '」を復元してもよろしいですか？')) {
                document.getElementById('restoreForm-' + empId).submit();
            }
        }
    </script>
</body>
</html>

