<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="kintai.UserBean" %>
<%
    // ログインチェック
    UserBean user = (UserBean) session.getAttribute("user");
    if (user == null) {
        response.sendRedirect(request.getContextPath() + "/web/login.jsp");
        return;
    }

    // セッションからユーザー情報を取得
    String loggedInUserName = user.getName();
    String loggedInDeptName = (String) session.getAttribute("deptName");
    int userRoleId = user.getRoleId(); 

    String successMessage = (String) request.getAttribute("successMessage");
    String errorMessage = (String) request.getAttribute("errorMessage");

    // メニューへ戻るリンクのURLを権限に応じて設定
    String backUrl = (userRoleId == 1) ? request.getContextPath() + "/AdminMenuServlet" : request.getContextPath() + "/web/menu.jsp";
%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>パスワード変更</title>
    <style>
        body {
            font-family: 'メイリオ', sans-serif;
            background-color: #f0f0f0;
            margin: 0;
            padding: 20px;
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: center;
            height: 100vh;
            overflow: hidden;
        }
        .container {
            max-width: 600px;
            width: 90%;
            background-color: white;
            padding: 20px;
            border-radius: 8px;
            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
            max-height: 90vh;
            overflow: hidden;
            margin: 0 auto;     /* 横中央寄せ */
        }
        /* スマホ対応 */
		@media (max-width: 480px) {
		    .container {
		        padding: 15px;
		    }
		    h1 {
		        font-size: 1.2em;
		    }
		}

        .header {
            display: flex;
            justify-content: space-between;
            align-items: flex-start;
            padding: 8px 15px;
            background: #fff;
            border-bottom: 1px solid #ccc;
            margin: -20px -20px 15px -20px;
            border-top-left-radius: 8px;
            border-top-right-radius: 8px;
        }
        .user-info {
            display: flex;
            flex-direction: column;
            line-height: 1.3;
            text-align: left;
            font-size: 12px;
        }
        .logout-button {
            background-color: #dc3545;
            color: white;
            border: 1px solid #dc3545;
            border-radius: 5px;
            padding: 6px 12px;
            cursor: pointer;
            font-size: 12px;
            text-decoration: none;
            align-self: center;
        }
        .logout-button:hover {
            background-color: #c82333;
            border-color: #bd2130;
        }
        h1 {
            color: #333;
            text-align: center;
            border-bottom: 2px solid #007bff;
            padding-bottom: 8px;
            margin-top: 0;
            margin-bottom: 20px;
            font-size: 1.5em;
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
        .form-group {
            margin-bottom: 15px;
        }
        .form-group label {
            display: block;
            margin-bottom: 5px;
            font-weight: bold;
            color: #555;
            font-size: 14px;
        }
        .form-group input[type="password"] {
            width: calc(100% - 16px);
            padding: 8px;
            border: 1px solid #ced4da;
            border-radius: 4px;
            font-size: 14px;
        }
        .button-container {
            text-align: center;
            margin-top: 20px;
        }
        .btn-primary {
            background-color: #007bff;
            color: white;
            padding: 8px 16px;
            border: none;
            border-radius: 5px;
            cursor: pointer;
            font-size: 14px;
            transition: background-color 0.2s ease;
        }
        .btn-primary:hover {
            background-color: #0056b3;
        }
        .back-link {
            display: inline-block;
            margin-top: 15px;
            padding: 8px 16px;
            background-color: #6c757d;
            color: white;
            text-decoration: none;
            border-radius: 4px;
            text-align: center;
            font-size: 14px;
            transition: background-color 0.2s ease;
        }
        .back-link:hover {
            background-color: #545b62;
        }
    </style>
    <script>
        function validateForm() {
            var currentPass = document.getElementById("currentPassword").value;
            var newPass = document.getElementById("newPassword").value;
            var confirmNewPass = document.getElementById("confirmNewPassword").value;
            var errorMessageDiv = document.getElementById("errorMessage");

            if (currentPass.trim() === "" || newPass.trim() === "" || confirmNewPass.trim() === "") {
                errorMessageDiv.innerHTML = "すべてのパスワードフィールドを入力してください";
                return false;
            }

            if (newPass !== confirmNewPass) {
                errorMessageDiv.innerHTML = "新しいパスワードと確認用パスワードが一致しません";
                return false;
            }

            // TODO: 必要に応じてパスワードの複雑性チェックを追加

            errorMessageDiv.innerHTML = ""; // エラーメッセージをクリア
            return confirm("パスワードを変更してもよろしいですか？");
        }
    </script>
</head>
<body>
    <div class="container">
        <div class="header">
            <div class="user-info">
                <p>部署：<%= loggedInDeptName != null ? loggedInDeptName : "情報なし" %></p>
                <p>氏名：<%= loggedInUserName %></p>
            </div>
            <form method="post" action="<%= request.getContextPath() %>/logout" style="margin: 0;">
                <input type="submit" value="ログアウト" class="logout-button">
            </form>
        </div>

        <h1>パスワード変更</h1>

        <%-- メッセージ表示 --%>
        <% if (successMessage != null) { %>
            <div class="message success-message">
                <%= successMessage %>
            </div>
        <% } %>
        <% if (errorMessage != null) { %>
            <div class="message error-message">
                <%= errorMessage %>
            </div>
        <% } %>

        <form action="<%= request.getContextPath() %>/PasswordChangeServlet" method="post" onsubmit="return validateForm()">
            <div class="form-group">
                <label for="currentPassword">現在のパスワード:</label>
                <input type="password" id="currentPassword" name="currentPassword" required>
            </div>
            <div class="form-group">
                <label for="newPassword">新しいパスワード:</label>
                <input type="password" id="newPassword" name="newPassword" required>
                <p style="color: red; font-size: 0.83em; margin-top: 5px;">
			        ・パスワードには大文字・小文字・数字・記号のうち3種類以上を含めてください<br>
			        ・12文字以上に設定してください
            </div>
            <div class="form-group">
                <label for="confirmNewPassword">新しいパスワード（確認用）:</label>
                <input type="password" id="confirmNewPassword" name="confirmNewPassword" required>
            </div>
            <div class="button-container">
                <button type="submit" class="btn-primary">パスワード変更</button>
            </div>
        </form>

        <div class="button-container">
            <a href="<%= backUrl %>" class="back-link">メニューへ戻る</a>
        </div>
    </div>
</body>
</html>
