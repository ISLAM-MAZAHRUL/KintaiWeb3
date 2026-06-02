<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="kintai.UserBean, kintai.EmpBean" %>
<%@ page import="java.util.List" %>
<%
    UserBean user = (UserBean) session.getAttribute("user");
    if (user == null) {
        response.sendRedirect(request.getContextPath() + "/web/login.jsp");
        return;
    }
    List<EmpBean> empList = (List<EmpBean>) request.getAttribute("empList");
    String message = (String) request.getAttribute("message");
%>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>付与日数設定</title>
<style>
    body { margin: 0; font-family: Meiryo, sans-serif; background: #f4f7f6; }
    .main-wrapper { max-width: 900px; margin: 30px auto; background: white; padding: 30px; border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.05); }
    .page-header { display: flex; align-items: center; gap: 12px; border-bottom: 3px solid #6f42c1; padding-bottom: 15px; margin-bottom: 25px; }
    .page-header h1 { margin: 0; font-size: 24px; }
    table { width: 100%; border-collapse: collapse; margin-top: 10px; }
    th { background: #6f42c1; color: white; padding: 12px; text-align: center; font-size: 13px; }
    td { padding: 10px; border: 1px solid #ddd; text-align: center; font-size: 13px; }
    tr:nth-child(even) { background: #f9f9f9; }
    tr:hover { background: #f0e6ff; }
    .btn { padding: 6px 14px; border: none; border-radius: 4px; cursor: pointer; font-size: 13px; }
    .btn-edit { background: #6f42c1; color: white; }
    .btn-save { background: #28a745; color: white; }
    .btn-cancel { background: #6c757d; color: white; }
    .btn-back { background: #6c757d; color: white; padding: 10px 20px; border: none; border-radius: 5px; cursor: pointer; text-decoration: none; display: inline-block; margin-top: 20px; }
    .edit-form { display: none; background: #f8f0ff; border: 1px solid #6f42c1; border-radius: 8px; padding: 20px; margin: 20px 0; }
    .edit-form h3 { color: #6f42c1; margin-top: 0; }
    .edit-form input[type=number] { padding: 6px 10px; border: 1px solid #ccc; border-radius: 4px; font-size: 14px; width: 80px; }
    .alert-success { background: #d4edda; border: 1px solid #c3e6cb; color: #155724; padding: 10px 15px; border-radius: 5px; margin-bottom: 15px; }
</style>
</head>
<body>
<div class="main-wrapper">

    <div class="page-header">
        <span style="font-size: 28px;">⚙️</span>
        <h1>付与日数設定</h1>
    </div>

    <% if (message != null && !message.isEmpty()) { %>
    <div class="alert-success"><%= message %></div>
    <% } %>

    <!-- 編集フォーム -->
    <div class="edit-form" id="editForm">
        <h3>📝 付与日数を編集</h3>
        <form method="post" action="<%= request.getContextPath() %>/YukyuGrantSettingServlet">
            <input type="hidden" id="editEmpId" name="empId">
            <table style="width:auto; border:none;">
                <tr>
                    <td style="border:none; text-align:left; padding:6px 12px; font-weight:bold;">社員番号</td>
                    <td style="border:none; padding:6px 12px;" id="editEmpIdDisplay"></td>
                </tr>
                <tr>
                    <td style="border:none; text-align:left; padding:6px 12px; font-weight:bold;">氏名</td>
                    <td style="border:none; padding:6px 12px;" id="editEmpNameDisplay"></td>
                </tr>
                <tr>
                    <td style="border:none; text-align:left; padding:6px 12px; font-weight:bold;">付与日数</td>
                    <td style="border:none; padding:6px 12px;">
                        <input type="number" name="grantedDays" id="editGrantedDays" min="1" max="40" required>
                        日
                    </td>
                </tr>
            </table>
            <div style="margin-top: 15px; display: flex; gap: 10px;">
                <button type="submit" class="btn btn-save">💾 保存</button>
                <button type="button" class="btn btn-cancel" onclick="closeEdit()">✖ キャンセル</button>
            </div>
        </form>
    </div>

    <!-- 社員一覧テーブル -->
    <% if (empList != null && !empList.isEmpty()) { %>
    <table>
        <tr>
            <th>社員番号</th>
            <th>氏名</th>
            <th>付与日数</th>
            <th>操作</th>
        </tr>
        <% for (EmpBean emp : empList) { %>
        <tr>
            <td><%= emp.getEmpId() %></td>
            <td><%= emp.getEmpName() %></td>
            <td><%= emp.getGrantedDays() %>日</td>
            <td>
                <button class="btn btn-edit"
                    onclick="openEdit('<%= emp.getEmpId() %>', '<%= emp.getEmpName() %>', <%= emp.getGrantedDays() %>)">
                    ✏️ 編集
                </button>
            </td>
        </tr>
        <% } %>
    </table>
    <% } else { %>
        <p>データがありません。</p>
    <% } %>

    <a href="<%= request.getContextPath() %>/YukyuKanriServlet" class="btn-back">
        ← 有休管理に戻る
    </a>
</div>

<script>
    function openEdit(empId, empName, grantedDays) {
        document.getElementById('editEmpId').value = empId;
        document.getElementById('editEmpIdDisplay').textContent = empId;
        document.getElementById('editEmpNameDisplay').textContent = empName;
        document.getElementById('editGrantedDays').value = grantedDays;
        document.getElementById('editForm').style.display = 'block';
        document.getElementById('editForm').scrollIntoView({behavior: 'smooth'});
    }

    function closeEdit() {
        document.getElementById('editForm').style.display = 'none';
    }
</script>
</body>
</html>
