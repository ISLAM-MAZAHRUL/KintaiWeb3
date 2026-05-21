<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List" %>
<%@ page import="kintai.PostBean" %>
<%@ page import="kintai.UserBean" %>
<%
    // ログインチェック
    UserBean user = (UserBean) session.getAttribute("user");
    if (user == null || user.getRoleId() != 1) {
        response.sendRedirect(request.getContextPath() + "/web/login.jsp");
        return;
    }
    
    // リストを取得
    List<PostBean> postList = (List<PostBean>) request.getAttribute("postList");
    String message = (String) request.getAttribute("message");
    Boolean success = (Boolean) request.getAttribute("success");
%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>役職管理</title>
    <style>
        body {
            font-family: 'メイリオ', sans-serif;
            background-color: #f0f0f0;
            margin: 0;
            padding: 20px;
        }
        
        .container {
            max-width: 900px;
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
        }
        
        .form-group label {
            display: inline-block;
            width: 120px;
            font-weight: bold;
        }
        
        .form-group input[type="text"] {
            width: 200px;
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
        .post-table {
            width: 100%;
            border-collapse: collapse;
            margin-top: 20px;
        }
        
        .post-table th, .post-table td {
            border: 1px solid #dee2e6;
            padding: 10px;
            text-align: left;
        }
        
        .post-table th {
            background-color: #f8f9fa;
            font-weight: bold;
            color: #495057;
        }
        
        .post-table tr:nth-child(even) {
            background-color: #f8f9fa;
        }
        
        .post-table tr:hover {
            background-color: #e9ecef;
        }
        
        /* 編集フォーム */
        .edit-form {
            display: none;
            margin: 0;
        }
        
        .edit-form input[type="text"] {
            width: 150px;
            padding: 3px;
            border: 1px solid #ced4da;
            border-radius: 4px;
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
    </style>
    <script>
        // 編集モードの切り替え
        function toggleEdit(postId) {
            var displaySpan = document.getElementById('display-' + postId);
            var editForm = document.getElementById('edit-' + postId);
            
            if (editForm.style.display === 'inline') {
                displaySpan.style.display = 'inline';
                editForm.style.display = 'none';
            } else {
                displaySpan.style.display = 'none';
                editForm.style.display = 'inline';
            }
        }
        
        // 削除確認
        function confirmDelete(postId, postName) {
            if (confirm('役職「' + postName + '」を削除してもよろしいですか？')) {
                document.getElementById('deleteForm-' + postId).submit();
            }
        }
        
        // 追加確認
        function confirmAdd(form) {
            var postId = form.postId.value;
            var postName = form.postName.value;
            
            if (postId.trim() === '' || postName.trim() === '') {
                alert('役職番号と役職名を入力してください。');
                return false;
            }
            
            return confirm('役職番号「' + postId + '」、役職名「' + postName + '」を追加してもよろしいですか？');
        }
        
        // 更新確認
        function confirmUpdate(form, postId) {
            var postName = form.postName.value;
            
            if (postName.trim() === '') {
                alert('役職名を入力してください。');
                return false;
            }
            
            return confirm('役職番号「' + postId + '」の役職名を「' + postName + '」に更新してもよろしいですか？');
        }
        
        // 復元確認
        function confirmRestore(postId, postName) {
            if (confirm('役職「' + postName + '」を復元してもよろしいですか？')) {
                document.getElementById('restoreForm-' + postId).submit();
            }
        }
    </script>
</head>
<body>
    <div class="container">
        <h1>役職管理</h1>
        
        <%-- メッセージ表示 --%>
        <% if (message != null && !message.isEmpty()) { %>
            <div class="message <%= (success != null && success) ? "success-message" : "error-message" %>">
                <%= message %>
            </div>
        <% } %>
        
        <%-- 削除履歴リンク --%>
        <div style="margin-bottom: 20px; text-align: right;">
            <a href="<%= request.getContextPath() %>/postManage?action=history" class="btn btn-secondary">過去削除履歴一覧</a>
        </div>
        
        <%-- 新規追加フォーム --%>
        <div class="add-form">
            <h2>新規役職追加</h2>
            <form method="post" action="<%= request.getContextPath() %>/postManage" onsubmit="return confirmAdd(this)">
                <input type="hidden" name="action" value="add">
                <div class="form-group">
                    <label for="newPostId">役職番号：</label>
                    <input type="text" id="newPostId" name="postId" maxlength="5" required>
                </div>
                <div class="form-group">
                    <label for="newPostName">役職名：</label>
                    <input type="text" id="newPostName" name="postName" maxlength="50" required>
                </div>
                <button type="submit" class="btn btn-primary">追加</button>
            </form>
        </div>
        
        <%-- 役職一覧テーブル --%>
        <h2>役職一覧</h2>
        <table class="post-table">
            <thead>
                <tr>
                    <th>役職番号</th>
                    <th>役職名</th>
                    <th>操作</th>
                </tr>
            </thead>
            <tbody>
                <% if (postList != null && !postList.isEmpty()) { %>
                    <% for (PostBean post : postList) { %>
                        <tr>
                            <td><%= post.getPostId() %></td>
                            <td>
                                <%-- 表示用 --%>
                                <span id="display-<%= post.getPostId() %>">
                                    <%= post.getPostName() %>
                                </span>
                                
                                <%-- 編集フォーム（初期状態では非表示） --%>
                                <form id="edit-<%= post.getPostId() %>" method="post" 
                                      action="<%= request.getContextPath() %>/postManage" 
                                      class="edit-form" style="display: none;"
                                      onsubmit="return confirmUpdate(this, '<%= post.getPostId() %>')">
                                    <input type="hidden" name="action" value="update">
                                    <input type="hidden" name="postId" value="<%= post.getPostId() %>">
                                    <input type="text" name="postName" value="<%= post.getPostName() %>" 
                                           maxlength="50" required>
                                    <button type="submit" class="btn btn-primary">保存</button>
                                    <button type="button" class="btn btn-secondary" 
                                            onclick="toggleEdit('<%= post.getPostId() %>')">キャンセル</button>
                                </form>
                            </td>
                            <td>
                                <button class="btn btn-success" onclick="toggleEdit('<%= post.getPostId() %>')">編集</button>
                                <button class="btn btn-danger" onclick="confirmDelete('<%= post.getPostId() %>', '<%= post.getPostName() %>')">削除</button>
                                
                                <%-- 削除用フォーム（非表示） --%>
                                <form id="deleteForm-<%= post.getPostId() %>" method="post" 
                                      action="<%= request.getContextPath() %>/postManage" style="display: none;">
                                    <input type="hidden" name="action" value="delete">
                                    <input type="hidden" name="postId" value="<%= post.getPostId() %>">
                                </form>
                            </td>
                        </tr>
                    <% } %>
                <% } else { %>
                    <tr>
                        <td colspan="3" style="text-align: center;">役職データがありません</td>
                    </tr>
                <% } %>
            </tbody>
        </table>
        
        <!-- 削除された役職の復元セクション -->
        <%
            List<PostBean> deletedPostList = (List<PostBean>) request.getAttribute("deletedPostList");
            Boolean hideRestoreSection = (Boolean) session.getAttribute("hideRestoreSection");
            if (deletedPostList != null && !deletedPostList.isEmpty() && (hideRestoreSection == null || !hideRestoreSection)) {
                // 復元セクションを表示したことをセッションに記録
                session.setAttribute("hideRestoreSection", true);
        %>
        <div class="add-form" style="margin-top: 30px;">
            <h2>誤削除データの復元</h2>
            <table>
                <thead>
                    <tr>
                        <th>役職番号</th>
                        <th>役職名</th>
                        <th>操作</th>
                    </tr>
                </thead>
                <tbody>
                    <% for (PostBean deletedPost : deletedPostList) { %>
                        <tr>
                            <td><%= deletedPost.getPostId() %></td>
                            <td><%= deletedPost.getPostName() %></td>
                            <td>
                                <button class="btn btn-success" onclick="confirmRestore('<%= deletedPost.getPostId() %>', '<%= deletedPost.getPostName() %>')">復元</button>
                                
                                <%-- 復元用フォーム（非表示） --%>
                                <form id="restoreForm-<%= deletedPost.getPostId() %>" method="post" 
                                      action="<%= request.getContextPath() %>/postManage" style="display: none;">
                                    <input type="hidden" name="action" value="restore">
                                    <input type="hidden" name="postId" value="<%= deletedPost.getPostId() %>">
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