<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List" %>
<%@ page import="java.math.BigDecimal" %>
<%@ page import="kintai.UserBean" %>
<%@ page import="kintai.ProjectBean" %>
<%@ page import="kintai.ProjectMemberReportBean" %>

<%
    // ログインチェック
    UserBean user = (UserBean) session.getAttribute("user");
    if (user == null || user.getRoleId() != 1) {
        response.sendRedirect(request.getContextPath() + "/web/login.jsp");
        return;
    }
    
    ProjectBean project = (ProjectBean) request.getAttribute("project");
    List<ProjectMemberReportBean> memberReports = (List<ProjectMemberReportBean>) request.getAttribute("memberReports");
    BigDecimal totalActual = (BigDecimal) request.getAttribute("totalActual");
    BigDecimal budgetVariance = (BigDecimal) request.getAttribute("budgetVariance");
    String selectedMonth = (String) request.getAttribute("selectedMonth");
    
    // 計算済みデータがあるかチェック
    boolean hasCalculatedData = false;
    if (memberReports != null && !memberReports.isEmpty()) {
        ProjectMemberReportBean firstMember = memberReports.get(0);
        hasCalculatedData = (firstMember.getActualAmount() != null);
    }
%>

<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>プロジェクト予実績管理</title>
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
            align-items: flex-start;
            padding: 10px 20px;
            background: #fff;
            border-bottom: 1px solid #ccc;
            margin: -20px -20px 20px -20px;
            border-top-left-radius: 8px;
            border-top-right-radius: 8px;
        }
        
        .user-info {
            display: flex;
            flex-direction: column;
            line-height: 1.5;
            text-align: left;
            font-size: 13px;
        }
        
        h1 {
            color: #333;
            border-bottom: 2px solid #007bff;
            padding-bottom: 10px;
            margin-top: 0;
        }
        
        .project-info {
            background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%);
            padding: 15px;
            margin-bottom: 20px;
            border-radius: 8px;
            border: 1px solid #dee2e6;
            box-shadow: 0 2px 4px rgba(0,0,0,0.05);
        }
        
        .project-info h2 {
            margin-top: 0;
            color: #495057;
            font-size: 16px;
        }
        
        .info-row {
            margin-bottom: 8px;
            font-size: 13px;
        }
        
        .info-label {
            font-weight: 600;
            display: inline-block;
            width: 120px;
            color: #495057;
        }
        
        /* テーブルのスタイル（kintai_rec.jspと統一） */
        .member-table {
            width: 100%;
            border-collapse: collapse;
            margin-bottom: 20px;
            border-radius: 8px;
            overflow: hidden;
            box-shadow: 0 2px 8px rgba(0,0,0,0.08);
            background-color: white;
        }
        
        .member-table th, .member-table td {
            border: 1px solid #dee2e6;
            padding: 10px 8px;
            text-align: center;
            vertical-align: middle;
            font-size: 12px;
        }
        
        .member-table th {
            background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%);
            font-weight: 600;
            color: #495057;
            border-bottom: 2px solid #dee2e6;
            position: relative;
        }
        
        .member-table tr:nth-child(even) {
            background-color: rgba(0,123,255,0.02);
        }
        
        .member-table tr:hover {
            background-color: rgba(0,123,255,0.05);
            transition: background-color 0.2s;
        }
        
        .text-right {
            text-align: right !important;
        }
        
        .summary-section {
            background-color: transparent;
            padding: 15px;
            margin-top: 20px;
            border-radius: 8px;
            border: none;
            box-shadow: none;
            display: inline-block;
            float: right;
            width: auto;
            min-width: 280px;
        }
        
        .summary-row {
            margin-bottom: 10px;
            font-size: 14px;
            text-align: left;
        }
        
        .summary-label {
            font-weight: 600;
            display: inline-block;
            width: 150px;
            color: #495057;
        }
        
        .summary-value {
            font-weight: 600;
            color: #1976d2;
        }
        
        .variance-positive {
            color: #2e7d32;
        }
        
        .variance-negative {
            color: #d32f2f;
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
        
        .calculation-section {
            margin-top: 20px;
            text-align: center;
            clear: both;
        }
        
        .no-data {
            text-align: center;
            color: #6c757d;
            font-style: italic;
            padding: 40px;
            background-color: #f8f9fa;
            border-radius: 8px;
            border: 1px solid #dee2e6;
        }
    </style>
    <script>
        function calculateResults() {
            // 時給データを収集
            var hourlyRateInputs = document.querySelectorAll('.hourly-rate-input');
            var hourlyRateData = [];
            
            hourlyRateInputs.forEach(function(input) {
                hourlyRateData.push({
                    empNo: input.getAttribute('data-empno'),
                    hourlyRate: parseFloat(input.value) || 0
                });
            });
            
            // パラメータ取得
            var projectId = '<%= project != null ? project.getProjectId() : "" %>';
            var month = '<%= selectedMonth %>';
            
            console.log('計算開始 - projectId:', projectId, 'month:', month);
            console.log('hourlyRateData:', hourlyRateData);
            
            // ボタンを無効化
            var calcButton = document.getElementById('calculateButton');
            calcButton.disabled = true;
            calcButton.textContent = '計算中...';
            
            // Ajaxリクエスト送信
            var xhr = new XMLHttpRequest();
            xhr.open('POST', '<%= request.getContextPath() %>/ProjectBudgetReportServlet', true);
            xhr.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded');
            
            xhr.onreadystatechange = function() {
                if (xhr.readyState === 4) {
                    calcButton.disabled = false;
                    if (xhr.status === 200) {
                        try {
                            var response = JSON.parse(xhr.responseText);
                            if (response.success) {
                                updateResults(response.data);
                                calcButton.textContent = '集計済み';
                                calcButton.style.background = 'linear-gradient(135deg, #6c757d 0%, #545b62 100%)';
                                calcButton.style.cursor = 'not-allowed';
                                calcButton.disabled = true;
                            } else {
                                alert('計算エラー: ' + response.message);
                                calcButton.textContent = '集計';
                            }
                        } catch (e) {
                            alert('データ処理エラーが発生しました');
                            calcButton.textContent = '集計';
                        }
                    } else {
                        alert('サーバーエラーが発生しました');
                        calcButton.textContent = '集計';
                    }
                }
            };
            
            // データ送信
            var formData = 'action=updateHourlyRates&projectId=' + projectId + '&month=' + month + 
                          '&hourlyRateData=' + encodeURIComponent(JSON.stringify(hourlyRateData));
            xhr.send(formData);
        }
        
        function updateResults(data) {
            // 実績額を更新
            var actualAmountPlaceholders = document.querySelectorAll('.actual-amount-placeholder');
            var actualAmountTotals = document.querySelectorAll('.actual-amount-total');
            
            data.memberReports.forEach(function(member, index) {
                if (actualAmountTotals[index]) {
                    actualAmountTotals[index].textContent = '¥' + Number(member.actualAmount).toLocaleString();
                }
            });
            
            // 表示切り替え
            actualAmountPlaceholders.forEach(function(placeholder) {
                placeholder.style.display = 'none';
            });
            
            actualAmountTotals.forEach(function(total, index) {
                total.style.display = 'inline';
                total.style.opacity = '0';
                total.style.transform = 'translateY(10px)';
                
                setTimeout(function() {
                    total.style.transition = 'opacity 0.3s ease, transform 0.3s ease';
                    total.style.opacity = '1';
                    total.style.transform = 'translateY(0)';
                }, index * 80);
            });
            
            // 総集計セクション更新・表示
            setTimeout(function() {
                var summaryValues = document.querySelectorAll('.summary-section .summary-value');
                
                // 実績額合計（最初の要素）
                if (summaryValues[0]) {
                    summaryValues[0].textContent = '¥' + Number(data.totalActual).toLocaleString();
                }
                
                // データを保存してupdateBudgetVariance関数で使用
                window.currentProjectData = data;
                
                // 初期の予算実績差異を更新
                updateBudgetVariance();
                
                var summarySection = document.getElementById('summarySection');
                summarySection.style.display = 'block';
                summarySection.style.opacity = '0';
                summarySection.style.transform = 'translateY(20px)';
                
                setTimeout(function() {
                    summarySection.style.transition = 'opacity 0.5s ease, transform 0.5s ease';
                    summarySection.style.opacity = '1';
                    summarySection.style.transform = 'translateY(0)';
                }, 100);
            }, actualAmountTotals.length * 80 + 300);
        }
        
        // 予算実績差異を更新する関数（その他経費を考慮）
        function updateBudgetVariance() {
            if (!window.currentProjectData) {
                return;
            }
            
            var data = window.currentProjectData;
            var otherExpenses = parseFloat(document.getElementById('otherExpenses').value) || 0;
            var budget = <%= project != null && project.getBudget() != null ? project.getBudget() : 0 %>;
            
            // 新しい計算式：予算 - 人件費合計(実績額) - その他経費 = 予算実績差異
            var newVariance = budget - data.totalActual - otherExpenses;
            
            var varianceDisplay = document.getElementById('budgetVarianceDisplay');
            if (varianceDisplay) {
                varianceDisplay.textContent = '¥' + Number(newVariance).toLocaleString();
                varianceDisplay.className = 'summary-value ' + (newVariance >= 0 ? 'variance-positive' : 'variance-negative');
            }
        }
        
        // その他経費入力値検証関数
        function validateOtherExpenses(input) {
            var value = parseFloat(input.value);
            var maxValue = 999999999999; // 12タケ（12桁）
            
            if (value > maxValue) {
                alert('その他経費は12桁以内で入力してください（最大値：¥' + maxValue.toLocaleString() + '）');
                input.value = maxValue;
                input.style.borderColor = '#dc3545';
                setTimeout(function() {
                    input.style.borderColor = '#ced4da';
                }, 2000);
            }
        }
        
        // 時給入力値検証関数
        function validateHourlyRate(input) {
            var value = parseFloat(input.value);
            var maxValue = 99999.99; // DECIMAL(7,2)の最大値
            
            if (value > maxValue) {
                alert('時給は99999.99以内で入力してください（最大値：¥' + maxValue.toLocaleString() + '）');
                input.value = maxValue;
                input.style.borderColor = '#dc3545';
                setTimeout(function() {
                    input.style.borderColor = '#ced4da';
                }, 2000);
            }
        }
        
        function closeWindow() {
            window.close();
        }
    </script>
</head>
<body>
    <div class="container">
        <div class="header">
            <div class="user-info">
                <p>部署：管理部</p>
                <p>氏名：<%= user.getName() %></p>
            </div>
        </div>
        
        <h1>プロジェクト予実績管理</h1>
        
        <% if (project != null) { %>
            <div class="project-info">
                <h2>プロジェクト情報</h2>
                <div class="info-row">
                    <span class="info-label">プロジェクト名：</span>
                    <%= project.getProjectName() %>
                </div>
                <div class="info-row">
                    <span class="info-label">対象月：</span>
                    <%= selectedMonth %>
                </div>
                <div class="info-row">
                    <span class="info-label">予算：</span>
                    ¥<%= String.format("%,d", project.getBudget()) %>
                </div>
            </div>
            
            <% if (memberReports != null && !memberReports.isEmpty()) { %>
                <table class="member-table">
                    <thead>
                        <tr>
                            <th>従業員番号</th>
                            <th>従業員名</th>
                            <th>月間総稼働時間</th>
                            <th>時給</th>
                            <th>実績額小計</th>
                        </tr>
                    </thead>
                    <tbody>
                        <% for (ProjectMemberReportBean member : memberReports) { %>
                            <tr>
                                <td><%= member.getEmpNo() %></td>
                                <td><%= member.getEmpName() %></td>
                                <td class="text-right">
                                    <%= member.getTotalHours() != null ? String.format("%.2f", member.getTotalHours()) : "0.00" %>時間
                                </td>
                                <td class="text-right">
                                    <input type="number" 
                                           class="hourly-rate-input" 
                                           value="<%= (member.getHourlyRate() != null && member.getHourlyRate().compareTo(BigDecimal.ZERO) > 0) ? member.getHourlyRate().intValue() : "" %>"
                                           data-empno="<%= member.getEmpNo() %>"
                                           min="0" 
                                           max="99999.99"
                                           step="100"
                                           placeholder="--"
                                           style="width: 80px; text-align: right; padding: 4px; border: 1px solid #ced4da; border-radius: 4px;"
                                           oninput="validateHourlyRate(this)">
                                </td>
                                <td class="text-right">
                                    <% if (hasCalculatedData && member.getActualAmount() != null) { %>
                                        <span class="actual-amount-placeholder">¥<%= String.format("%,.0f", member.getActualAmount()) %></span>
                                        <span class="actual-amount-total" style="display: none;">
                                            ¥<%= String.format("%,.0f", member.getActualAmount()) %>
                                        </span>
                                    <% } else { %>
                                        <span class="actual-amount-placeholder">--</span>
                                        <span class="actual-amount-total" style="display: none;">
                                            --
                                        </span>
                                    <% } %>
                                </td>
                            </tr>
                        <% } %>
                    </tbody>
                </table>
                
                <div class="calculation-section">
                    <button id="calculateButton" class="btn btn-primary" onclick="calculateResults()">集計</button>
                    <button class="btn btn-secondary" onclick="closeWindow()">閉じる</button>
                </div>
                
                <% if (hasCalculatedData) { %>
                    <div id="summarySection" class="summary-section" style="display: block;">
                        <div class="summary-row">
                            <span class="summary-label">実績額合計：</span>
                            <span class="summary-value">¥<%= totalActual != null ? String.format("%,.0f", totalActual) : "0" %></span>
                        </div>
                        <div class="summary-row">
                            <span class="summary-label">その他経費：</span>
                            <span>¥<input type="number" id="otherExpenses" placeholder="0" min="0" max="999999999999" style="width: 80px; text-align: right; padding: 4px; border: 1px solid #ced4da; border-radius: 4px;" onchange="updateBudgetVariance()" oninput="validateOtherExpenses(this)"></span>
                        </div>
                        <div class="summary-row">
                            <span class="summary-label">予算実績差異：</span>
                            <% if (budgetVariance != null) { %>
                                <span id="budgetVarianceDisplay" class="summary-value <%= budgetVariance.compareTo(BigDecimal.ZERO) >= 0 ? "variance-positive" : "variance-negative" %>">
                                    ¥<%= String.format("%,.0f", budgetVariance) %>
                                </span>
                            <% } else { %>
                                <span id="budgetVarianceDisplay" class="summary-value">計算不可</span>
                            <% } %>
                        </div>
                    </div>
                <% } else { %>
                    <div id="summarySection" class="summary-section" style="display: none;">
                        <div class="summary-row">
                            <span class="summary-label">実績額合計：</span>
                            <span class="summary-value">--</span>
                        </div>
                        <div class="summary-row">
                            <span class="summary-label">その他経費：</span>
                            <span>¥<input type="number" id="otherExpenses" placeholder="0" min="0" max="999999999999" style="width: 80px; text-align: right; padding: 4px; border: 1px solid #ced4da; border-radius: 4px;" onchange="updateBudgetVariance()" oninput="validateOtherExpenses(this)"></span>
                        </div>
                        <div class="summary-row">
                            <span class="summary-label">予算実績差異：</span>
                            <span id="budgetVarianceDisplay" class="summary-value">--</span>
                        </div>
                    </div>
                <% } %>
                
            <% } else { %>
                <div class="no-data">
                    <p>指定された月にこのプロジェクトの作業記録がありません</p>
                </div>
                <div class="calculation-section">
                    <button class="btn btn-secondary" onclick="closeWindow()">閉じる</button>
                </div>
            <% } %>
            
        <% } else { %>
            <div class="no-data">
                <p>プロジェクト情報が見つかりません</p>
            </div>
            <div class="calculation-section">
                <button class="btn btn-secondary" onclick="closeWindow()">閉じる</button>
            </div>
        <% } %>
    </div>
</body>
</html>