<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List" %>
<%@ page import="kintai.ProjectBean" %>
<%@ page import="java.text.NumberFormat" %>

<%
    List<ProjectBean> projectmanagelist = (List<ProjectBean>) request.getAttribute("projectmanagelist");
%>

<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>プロジェクト一覧</title>
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
            margin-top: 0;
        }
        
        /* テーブルスタイル */
        .project-table {
            width: 100%;
            border-collapse: collapse;
            margin-top: 10px;
            border-radius: 8px;
            overflow: hidden;
            box-shadow: 0 2px 8px rgba(0,0,0,0.08);
            background-color: white;
        }
        
        .project-table th, .project-table td {
            border: 1px solid #dee2e6;
            padding: 10px 8px;
            text-align: center;
            vertical-align: middle;
            font-size: 12px;
        }
        
        .project-table th {
            background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%);
            font-weight: 600;
            color: #495057;
            border-bottom: 2px solid #dee2e6;
        }
        
        .project-table tr:nth-child(even) {
            background-color: rgba(0,123,255,0.02);
        }
        
        .project-table tr:hover {
            background-color: rgba(0,123,255,0.05);
            transition: background-color 0.2s;
        }
        
        .close-button {
            margin-top: 20px;
            text-align: center;
        }
        
        .btn {
            padding: 8px 16px;
            border: none;
            border-radius: 6px;
            cursor: pointer;
            font-size: 13px;
            font-weight: 600;
            margin-right: 10px;
            transition: all 0.2s;
        }
        
        .btn-secondary {
            background: linear-gradient(135deg, #6c757d 0%, #545b62 100%);
            color: white;
            box-shadow: 0 2px 4px rgba(108,117,125,0.2);
        }
        
        .btn-secondary:hover {
            background: linear-gradient(135deg, #545b62 0%, #495057 100%);
            transform: translateY(-1px);
            box-shadow: 0 3px 6px rgba(108,117,125,0.3);
        }
        
        .btn-sm {
            padding: 4px 8px;
            font-size: 11px;
            border-radius: 4px;
        }
        
        .btn-success {
            background: linear-gradient(135deg, #28a745 0%, #20c997 100%);
            color: white;
            box-shadow: 0 2px 4px rgba(40,167,69,0.2);
        }
        
        .btn-success:hover {
            background: linear-gradient(135deg, #218838 0%, #1e7e34 100%);
            transform: translateY(-1px);
            box-shadow: 0 3px 6px rgba(40,167,69,0.3);
        }
        
        .btn-danger {
            background: linear-gradient(135deg, #dc3545 0%, #c82333 100%);
            color: white;
            box-shadow: 0 2px 4px rgba(220,53,69,0.2);
        }
        
        .btn-danger:hover {
            background: linear-gradient(135deg, #c82333 0%, #bd2130 100%);
            transform: translateY(-1px);
            box-shadow: 0 3px 6px rgba(220,53,69,0.3);
        }
        
        .btn-primary {
            background: linear-gradient(135deg, #007bff 0%, #0056b3 100%);
            color: white;
            box-shadow: 0 2px 4px rgba(0,123,255,0.2);
        }
        
        .btn-primary:hover {
            background: linear-gradient(135deg, #0056b3 0%, #004085 100%);
            transform: translateY(-1px);
            box-shadow: 0 3px 6px rgba(0,123,255,0.3);
        }
        
        /* モーダルダイアログのスタイル */
        .modal {
            display: none;
            position: fixed;
            z-index: 1000;
            left: 0;
            top: 0;
            width: 100%;
            height: 100%;
            background-color: rgba(0,0,0,0.5);
        }

        .modal-content {
            background-color: white;
            margin: 5% auto;
            padding: 15px;
            border-radius: 8px;
            width: 70%;
            max-width: 600px;
            max-height: 75%;
            overflow-y: auto;
            font-size: 12px;
        }

        .modal-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 15px;
            padding-bottom: 10px;
            border-bottom: 2px solid #007bff;
        }

        .modal-close {
            color: #aaa;
            font-size: 20px;
            font-weight: bold;
            cursor: pointer;
        }

        .modal-close:hover {
            color: #000;
        }
        
        .modal-body {
            padding: 10px 0;
        }
        
        .modal-footer {
            display: flex;
            justify-content: center;
            gap: 10px;
            margin-top: 20px;
            padding-top: 15px;
            border-top: 1px solid #dee2e6;
        }
        
        .form-group {
            margin-bottom: 15px;
        }
        
        .form-group label {
            display: block;
            margin-bottom: 5px;
            font-weight: 600;
            color: #495057;
        }
        
        .form-group input {
            width: 100%;
            padding: 8px;
            border: 1px solid #ced4da;
            border-radius: 4px;
            font-size: 13px;
        }
    </style>
    <script>
        function closeWindow() {
            window.close();
        }
        
        // プロジェクト編集モーダル表示
        function editProject(projectId, projectName, budget, startDate, endDate) {
            var modal = document.getElementById('projectEditModal');
            
            // フォームに値を設定
            document.getElementById('editProjectId').value = projectId;
            document.getElementById('editProjectName').value = projectName;
            document.getElementById('editBudgetAmount').value = budget;
            document.getElementById('editStartDate').value = startDate;
            document.getElementById('editEndDate').value = endDate;
            
            modal.style.display = 'block';
        }
        
        // プロジェクト削除モーダル表示
        function deleteProject(projectId, projectName) {
            var modal = document.getElementById('projectDeleteModal');
            
            // 削除確認メッセージに現在のプロジェクト名を設定
            document.getElementById('deleteConfirmMessage').textContent = 
                'プロジェクト「' + projectName + '」を削除してもよろしいですか？';
            
            // 削除フォームにプロジェクトIDを設定
            document.getElementById('deleteProjectId').value = projectId;
            
            modal.style.display = 'block';
        }
        
        // プロジェクト編集モーダルを閉じる
        function closeProjectEditModal() {
            document.getElementById('projectEditModal').style.display = 'none';
        }
        
        // プロジェクト削除モーダルを閉じる
        function closeProjectDeleteModal() {
            document.getElementById('projectDeleteModal').style.display = 'none';
        }
        
        // プロジェクト削除実行
        function executeProjectDelete() {
            document.getElementById('projectDeleteForm').submit();
        }
        
        // プロジェクト更新確認
        function confirmProjectUpdate() {
            var projectName = document.getElementById('editProjectName').value;
            return confirm('プロジェクト「' + projectName + '」の情報を変更してもよろしいですか？');
        }
        
        // モーダル外部クリック時の処理
        window.onclick = function(event) {
            var editModal = document.getElementById('projectEditModal');
            var deleteModal = document.getElementById('projectDeleteModal');
            
            if (event.target == editModal) {
                editModal.style.display = 'none';
            }
            if (event.target == deleteModal) {
                deleteModal.style.display = 'none';
            }
        }
    </script>
</head>
<body>
    <div class="container">
        <h1>プロジェクト一覧</h1>
        
        <table class="project-table">
            <thead>
                <tr>
                    <th>プロジェクトID</th>
                    <th>プロジェクト名</th>
                    <th>予算</th>
                    <th>開始日</th>
                    <th>終了日</th>
                    <th>操作</th>
                </tr>
            </thead>
            <tbody>
                <% if (projectmanagelist != null && !projectmanagelist.isEmpty()) { %>
                    <% 
                        NumberFormat nf = NumberFormat.getInstance();
                        nf.setGroupingUsed(true); // 3桁区切りを有効にする
                    %>
                    <% for (ProjectBean project : projectmanagelist) { %>
                        <tr>
                            <td><%= project.getProjectId() %></td>
                            <td><%= (project.getProjectName() != null) ? project.getProjectName() : "情報なし" %></td>
                            <td style="text-align: right;">
                                <%
                                    Integer budget = project.getBudgetAmount();
                                    out.print(budget != null && budget != 0 ? nf.format(budget) + "円" : "情報なし");
                                %>
                            </td>
                            <td><%= (project.getStartDate() != null) ? project.getStartDate() : "---" %></td>
                            <td><%= (project.getEndDate() != null) ? project.getEndDate(): "---" %></td>
                            <td>
                                <button type="button" class="btn btn-success btn-sm" onclick="editProject(<%= project.getProjectId() %>, '<%= project.getProjectName() != null ? project.getProjectName() : "" %>', '<%= project.getBudgetAmount() != null ? project.getBudgetAmount() : "" %>', '<%= project.getStartDate() != null ? project.getStartDate() : "" %>', '<%= project.getEndDate() != null ? project.getEndDate() : "" %>')" style="margin: 0 2px;">変更</button>
                                <button type="button" class="btn btn-danger btn-sm" onclick="deleteProject(<%= project.getProjectId() %>, '<%= project.getProjectName() != null ? project.getProjectName() : "" %>')" style="margin: 0 2px;">削除</button>
                            </td>
                        </tr>
                    <% } %>
                <% } else { %>
                    <tr>
                        <td colspan="6" style="text-align: center;">プロジェクトデータがありません</td>
                    </tr>
                <% } %>
            </tbody>
        </table>
        
        <div class="close-button">
            <button class="btn btn-secondary" onclick="closeWindow()">閉じる</button>
        </div>
    </div>

    <%-- プロジェクト編集モーダル --%>
    <div id="projectEditModal" class="modal">
        <div class="modal-content">
            <div class="modal-header">
                <h2>プロジェクト編集</h2>
                <span class="modal-close" onclick="closeProjectEditModal()">&times;</span>
            </div>
            <div class="modal-body">
                <form method="post" action="<%= request.getContextPath() %>/projectManage" onsubmit="return confirmProjectUpdate();">
                    <input type="hidden" name="action" value="update">
                    <input type="hidden" id="editProjectId" name="ProjectId" value="">
                    
                    <div class="form-group">
                        <label for="editProjectName">プロジェクト名：</label>
                        <input type="text" id="editProjectName" name="ProjectName" maxlength="10" required>
                    </div>
                    
                    <div class="form-group">
                        <label for="editBudgetAmount">予算：</label>
                        <input type="number" id="editBudgetAmount" name="BudgetAmount">
                    </div>
                    
                    <div class="form-group">
                        <label for="editStartDate">期間(開始日)：</label>
                        <input type="date" id="editStartDate" name="StartDate">
                    </div>
                    
                    <div class="form-group">
                        <label for="editEndDate">期間(終了日)：</label>
                        <input type="date" id="editEndDate" name="EndDate">
                    </div>
                    
                    <div class="modal-footer">
                        <button type="submit" class="btn btn-primary">更新</button>
                        <button type="button" class="btn btn-secondary" onclick="closeProjectEditModal()">キャンセル</button>
                    </div>
                </form>
            </div>
        </div>
    </div>

    <%-- プロジェクト削除確認モーダル --%>
    <div id="projectDeleteModal" class="modal">
        <div class="modal-content">
            <div class="modal-header">
                <h2>プロジェクト削除確認</h2>
                <span class="modal-close" onclick="closeProjectDeleteModal()">&times;</span>
            </div>
            <div class="modal-body">
                <p id="deleteConfirmMessage">プロジェクトを削除してもよろしいですか？</p>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-danger" onclick="executeProjectDelete()">削除</button>
                <button type="button" class="btn btn-secondary" onclick="closeProjectDeleteModal()">キャンセル</button>
            </div>
        </div>
    </div>

    <%-- 削除用フォーム（非表示） --%>
    <form id="projectDeleteForm" method="post" action="<%= request.getContextPath() %>/projectManage" style="display: none;">
        <input type="hidden" name="action" value="delete">
        <input type="hidden" id="deleteProjectId" name="ProjectId" value="">
    </form>
</body>
</html>