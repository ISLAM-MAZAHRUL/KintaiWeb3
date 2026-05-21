<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="kintai.UserBean" %>
<%@ page import="kintai.ComplianceCheckResult" %>
<%@ page import="kintai.ComplianceViolation" %>
<%@ page import="java.util.List" %>
<%@ page import="java.time.LocalDate" %>
<%
    // ログインチェック
    UserBean user = (UserBean) session.getAttribute("user");
    if (user == null) {
        response.sendRedirect(request.getContextPath() + "/web/login.jsp");
        return;
    }

    ComplianceCheckResult complianceResult = (ComplianceCheckResult) request.getAttribute("complianceResult");
    String checkType = (String) request.getAttribute("checkType");
    String targetEmpno = (String) request.getAttribute("targetEmpno");
    String successMessage = (String) request.getAttribute("successMessage");
    String errorMessage = (String) request.getAttribute("errorMessage");
    Boolean isSelfMode = (Boolean) request.getAttribute("isSelfMode");
    if (isSelfMode == null) isSelfMode = false;

    String backUrl = (user.getRoleId() == 1) ? request.getContextPath() + "/AdminMenuServlet" : request.getContextPath() + "/web/menu.jsp";
%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>合規チェック結果</title>
    <style>
        body {
            font-family: 'メイリオ', sans-serif;
            background-color: #f0f0f0;
            margin: 0;
            padding: 20px;
            font-size: 13px;
        }
        .container {
            max-width: 1000px;
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
            padding: 10px 20px;
            background: #fff;
            border-bottom: 1px solid #ccc;
            margin: -20px -20px 20px -20px;
        }
        .user-info {
            font-size: 13px;
        }
        .logout-button {
            background-color: #dc3545;
            color: white;
            border: none;
            padding: 8px 16px;
            border-radius: 4px;
            cursor: pointer;
            font-size: 13px;
        }
        .page-title {
            text-align: center;
            margin: 10px 0 20px 0;
            font-size: 18px;
            color: #333;
            border-bottom: 2px solid #007bff;
            padding-bottom: 5px;
        }
        
        /* メッセージ表示 */
        .message {
            padding: 12px;
            margin: 10px 0;
            border-radius: 4px;
            font-size: 13px;
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
        
        /* 合規結果サマリー */
        .compliance-summary {
            display: flex;
            gap: 20px;
            margin-bottom: 30px;
        }
        .summary-card {
            flex: 1;
            background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%);
            border: 1px solid #dee2e6;
            border-radius: 8px;
            padding: 20px;
            text-align: center;
            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
        }
        .summary-card h3 {
            margin: 0 0 10px 0;
            font-size: 14px;
            color: #495057;
        }
        .summary-card .value {
            font-size: 24px;
            font-weight: bold;
            margin: 10px 0;
        }
        .summary-card .detail {
            font-size: 12px;
            color: #6c757d;
            margin: 0;
        }
        
        /* スコア表示 */
        .score-excellent .value { color: #28a745; }
        .score-good .value { color: #17a2b8; }
        .score-fair .value { color: #ffc107; }
        .score-poor .value { color: #dc3545; }
        
        /* リスクレベル表示 */
        .risk-low { border-left: 4px solid #28a745; }
        .risk-medium { border-left: 4px solid #ffc107; }
        .risk-high { border-left: 4px solid #dc3545; }
        
        /* 違反項目テーブル */
        .violations-section {
            margin-top: 30px;
        }
        .violations-table {
            width: 100%;
            border-collapse: collapse;
            margin-top: 15px;
            font-size: 12px;
        }
        .violations-table th, .violations-table td {
            border: 1px solid #dee2e6;
            padding: 10px 8px;
            text-align: left;
            vertical-align: top;
        }
        .violations-table th {
            background-color: #f8f9fa;
            font-weight: bold;
            text-align: center;
        }
        
        /* 重要度別スタイル */
        .severity-high { background-color: #f8d7da; }
        .severity-medium { background-color: #fff3cd; }
        .severity-low { background-color: #d1ecf1; }
        
        /* アクションボタン */
        .action-buttons {
            display: flex;
            gap: 15px;
            margin: 30px 0;
            flex-wrap: wrap;
        }
        .action-btn {
            padding: 10px 20px;
            border: none;
            border-radius: 4px;
            cursor: pointer;
            font-size: 13px;
            font-weight: bold;
            transition: background-color 0.2s;
        }
        .btn-primary {
            background-color: #007bff;
            color: white;
        }
        .btn-primary:hover {
            background-color: #0056b3;
        }
        .btn-secondary {
            background-color: #6c757d;
            color: white;
        }
        .btn-secondary:hover {
            background-color: #545b62;
        }
        .btn-success {
            background-color: #28a745;
            color: white;
        }
        .btn-success:hover {
            background-color: #218838;
        }
        .btn-warning {
            background-color: #ffc107;
            color: #212529;
        }
        .btn-warning:hover {
            background-color: #e0a800;
        }
        
        /* 戻るリンク */
        .back-link {
            display: inline-block;
            background-color: #6c757d;
            color: white;
            text-decoration: none;
            padding: 10px 20px;
            border-radius: 4px;
            margin: 20px 0;
            font-size: 13px;
        }
        .back-link:hover {
            background-color: #545b62;
        }
        
        /* 空の状態表示 */
        .empty-state {
            text-align: center;
            padding: 40px 20px;
            color: #6c757d;
        }
        .empty-state .icon {
            font-size: 48px;
            margin-bottom: 15px;
        }
        .empty-state h3 {
            font-size: 16px;
            margin-bottom: 10px;
        }
        .empty-state p {
            font-size: 13px;
            line-height: 1.5;
        }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <div class="user-info">
                <p>氏名：<%= user.getName() %></p>
            </div>
            <form method="post" action="<%= request.getContextPath() %>/logout" style="margin: 0;">
                <input type="submit" value="ログアウト" class="logout-button">
            </form>
        </div>

        <h1 class="page-title">
            <% if ("legal".equals(checkType)) { %>
                📋 法令遵守チェック結果
            <% } else if ("company".equals(checkType)) { %>
                📖 会社規則チェック結果
            <% } else { %>
                🔍 総合合規チェック結果
            <% } %>
        </h1>

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

        <% if (complianceResult != null) { %>
            <%-- 合規結果サマリー --%>
            <div class="compliance-summary">
                <div class="summary-card <%= complianceResult.getComplianceScoreCssClass() %>">
                    <h3>合規スコア</h3>
                    <div class="value"><%= String.format("%.1f", complianceResult.getComplianceScore()) %>%</div>
                    <div class="detail">100点満点</div>
                </div>
                
                <div class="summary-card <%= complianceResult.getRiskLevelCssClass() %>">
                    <h3>リスクレベル</h3>
                    <div class="value"><%= complianceResult.getRiskLevel() %></div>
                    <div class="detail">総合評価</div>
                </div>
                
                <div class="summary-card">
                    <h3>違反件数</h3>
                    <div class="value" style="color: #dc3545;"><%= complianceResult.getTotalViolations() %>件</div>
                    <div class="detail">
                        高重度: <%= complianceResult.getHighSeverityViolationCount() %>件、
                        中重度: <%= complianceResult.getMediumSeverityViolationCount() %>件、
                        低重度: <%= complianceResult.getLowSeverityViolationCount() %>件
                    </div>
                </div>
                
                <div class="summary-card">
                    <h3>チェック日時</h3>
                    <div class="value" style="color: #495057; font-size: 16px;"><%= complianceResult.getCheckDate() %></div>
                    <div class="detail">対象従業員: <%= targetEmpno %></div>
                </div>
            </div>

            <%-- 合規状況サマリー --%>
            <div class="message <% if (complianceResult.getComplianceScore() >= 80) { %>success-message<% } else { %>error-message<% } %>">
                <%= complianceResult.getComplianceSummary() %>
            </div>

            <%-- アクションボタン --%>
            <div class="action-buttons">
                <% if (!"legal".equals(checkType)) { %>
                    <button class="action-btn btn-primary" onclick="performCheck('legal')">📋 法令遵守チェック</button>
                <% } %>
                <% if (!"company".equals(checkType)) { %>
                    <button class="action-btn btn-secondary" onclick="performCheck('company')">📖 会社規則チェック</button>
                <% } %>
                <% if (!"comprehensive".equals(checkType)) { %>
                    <button class="action-btn btn-success" onclick="performCheck('comprehensive')">🔍 総合チェック</button>
                <% } %>
                <button class="action-btn btn-warning" onclick="generateComplianceReport()">📄 レポート出力</button>
            </div>

            <%-- 違反項目詳細 --%>
            <div class="violations-section">
                <h3>🚨 違反項目詳細</h3>
                
                <% if (complianceResult.getViolations().isEmpty()) { %>
                    <div class="empty-state">
                        <div class="icon">✅</div>
                        <h3>違反項目はありません</h3>
                        <p>すべての項目で合規基準を満たしています。<br>引き続き適切な勤務管理を心がけてください。</p>
                    </div>
                <% } else { %>
                    <table class="violations-table">
                        <thead>
                            <tr>
                                <th style="width: 8%;">重要度</th>
                                <th style="width: 15%;">違反タイプ</th>
                                <th style="width: 10%;">発生日</th>
                                <th style="width: 40%;">詳細説明</th>
                                <th style="width: 15%;">法的根拠</th>
                                <th style="width: 12%;">アクション</th>
                            </tr>
                        </thead>
                        <tbody>
                            <% for (ComplianceViolation violation : complianceResult.getViolations()) { %>
                                <tr class="<%= violation.getSeverityCssClass() %>">
                                    <td style="text-align: center;">
                                        <%= violation.getSeverityIcon() %><br>
                                        <small><%= violation.getSeverity() %></small>
                                    </td>
                                    <td><strong><%= violation.getViolationType() %></strong></td>
                                    <td style="text-align: center;">
                                        <% if (violation.getDate() != null) { %>
                                            <%= violation.getFormattedDate() %>
                                        <% } else { %>
                                            -
                                        <% } %>
                                    </td>
                                    <td><%= violation.getDescription() %></td>
                                    <td>
                                        <small>
                                            <span style="background-color: #e9ecef; padding: 2px 6px; border-radius: 3px; font-size: 11px;">
                                                <%= violation.getLegalBasisType() %>
                                            </span><br>
                                            <%= violation.getLegalBasis() %>
                                        </small>
                                    </td>
                                    <td style="text-align: center;">
                                        <button class="action-btn btn-primary" style="padding: 5px 10px; font-size: 11px;"
                                                onclick="showViolationDetails('<%= violation.getViolationType() %>')">
                                            詳細
                                        </button>
                                    </td>
                                </tr>
                            <% } %>
                        </tbody>
                    </table>
                <% } %>
            </div>
        <% } else { %>
            <div class="empty-state">
                <div class="icon">❓</div>
                <h3>チェック結果がありません</h3>
                <p>合規チェックを実行してください。</p>
            </div>
        <% } %>

        <a href="<%= backUrl %>" class="back-link">← メニューに戻る</a>
    </div>

    <script>
        function performCheck(checkType) {
            const form = document.createElement('form');
            form.method = 'POST';
            form.action = '<%= request.getContextPath() %>/ComplianceCheckServlet';
            
            const actionInput = document.createElement('input');
            actionInput.type = 'hidden';
            actionInput.name = 'action';
            actionInput.value = checkType + 'Check';
            form.appendChild(actionInput);
            
            <% if (targetEmpno != null) { %>
            const empnoInput = document.createElement('input');
            empnoInput.type = 'hidden';
            empnoInput.name = 'empno';
            empnoInput.value = '<%= targetEmpno %>';
            form.appendChild(empnoInput);
            <% } %>
            
            document.body.appendChild(form);
            form.submit();
        }
        
        function generateComplianceReport() {
            const form = document.createElement('form');
            form.method = 'GET';
            form.action = '<%= request.getContextPath() %>/ReportGenerationServlet';
            
            const reportTypeInput = document.createElement('input');
            reportTypeInput.type = 'hidden';
            reportTypeInput.name = 'reportType';
            reportTypeInput.value = 'compliance';
            form.appendChild(reportTypeInput);
            
            const formatInput = document.createElement('input');
            formatInput.type = 'hidden';
            formatInput.name = 'format';
            formatInput.value = 'pdf';
            form.appendChild(formatInput);
            
            <% if (targetEmpno != null) { %>
            const empnoInput = document.createElement('input');
            empnoInput.type = 'hidden';
            empnoInput.name = 'empno';
            empnoInput.value = '<%= targetEmpno %>';
            form.appendChild(empnoInput);
            <% } %>
            
            document.body.appendChild(form);
            form.submit();
        }
        
        function showViolationDetails(violationType) {
            const form = document.createElement('form');
            form.method = 'POST';
            form.action = '<%= request.getContextPath() %>/ComplianceCheckServlet';
            
            const actionInput = document.createElement('input');
            actionInput.type = 'hidden';
            actionInput.name = 'action';
            actionInput.value = 'getViolationDetails';
            form.appendChild(actionInput);
            
            const violationTypeInput = document.createElement('input');
            violationTypeInput.type = 'hidden';
            violationTypeInput.name = 'violationType';
            violationTypeInput.value = violationType;
            form.appendChild(violationTypeInput);
            
            <% if (targetEmpno != null) { %>
            const empnoInput = document.createElement('input');
            empnoInput.type = 'hidden';
            empnoInput.name = 'empno';
            empnoInput.value = '<%= targetEmpno %>';
            form.appendChild(empnoInput);
            <% } %>
            
            document.body.appendChild(form);
            form.submit();
        }
    </script>
</body>
</html>