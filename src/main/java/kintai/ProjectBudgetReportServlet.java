package kintai;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.util.List;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
@WebServlet("/ProjectBudgetReportServlet")
public class ProjectBudgetReportServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    
    private ProjectDao projectDao = new ProjectDao();
    private ProjectBudgetReportDao budgetReportDao = new ProjectBudgetReportDao();
    
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        // セッションチェック
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            response.sendRedirect(request.getContextPath() + "/web/login.jsp");
            return;
        }
        
        // 管理者権限チェック
        UserBean user = (UserBean) session.getAttribute("user");
        if (user.getRoleId() != 1) {
            response.sendRedirect(request.getContextPath() + "/web/menu.jsp");
            return;
        }
        
        String action = request.getParameter("action");
        
        if ("getProjectMembers".equals(action)) {
            getProjectMembers(request, response);
        } else if ("getProjectMembersJson".equals(action)) {
            getProjectMembersJson(request, response);
        } else {
            // プロジェクト一覧を取得
            List<ProjectBean> projectList = projectDao.findAll();
            request.setAttribute("projectList", projectList);
            
            RequestDispatcher dispatcher = request.getRequestDispatcher("/web/project_budget_report.jsp");
            dispatcher.forward(request, response);
        }
    }
    
    private void getProjectMembers(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        String projectIdStr = request.getParameter("projectId");
        String month = request.getParameter("month");
        
        if (projectIdStr == null || month == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "パラメータが不足しています");
            return;
        }
        
        try {
            int projectId = Integer.parseInt(projectIdStr);
            
            // デバッグ用ログ
            System.out.println("getProjectMembers called with projectId: " + projectId + ", month: " + month);
            
            // プロジェクト情報を取得
            ProjectBean project = projectDao.findById(projectId);
            if (project == null) {
                System.out.println("Project not found for ID: " + projectId);
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "プロジェクトが見つかりません");
                return;
            }
            
            // 保存済みの時給データがあるかチェック
            boolean hasCalculatedData = budgetReportDao.hasCalculatedData(projectId, month);
            
            // プロジェクトメンバーの実績データを取得
            List<ProjectMemberReportBean> memberReports;
            if (hasCalculatedData) {
                // 既に計算済みのデータがある場合は計算結果付きで取得
                memberReports = budgetReportDao.getProjectMemberReportsWithCalculation(projectId, month);
            } else {
                // 初回表示の場合は基本データのみ
                memberReports = budgetReportDao.getProjectMemberReports(projectId, month);
            }
            
            System.out.println("Found " + memberReports.size() + " member reports for project " + projectId + ", month " + month);
            
            // 総集計を計算
            BigDecimal totalActual = BigDecimal.ZERO;
            for (ProjectMemberReportBean member : memberReports) {
                if (member.getActualAmount() != null) {
                    totalActual = totalActual.add(member.getActualAmount());
                }
            }
            
            // 予算実績差異を計算
            BigDecimal budgetVariance = null;
            if (project.getBudget() != null && project.getBudget() > 0) {
                budgetVariance = new BigDecimal(project.getBudget()).subtract(totalActual);
            }
            
            request.setAttribute("project", project);
            request.setAttribute("memberReports", memberReports);
            request.setAttribute("totalActual", totalActual);
            request.setAttribute("budgetVariance", budgetVariance);
            request.setAttribute("selectedMonth", month);
            
            RequestDispatcher dispatcher = request.getRequestDispatcher("/web/project_member_detail.jsp");
            dispatcher.forward(request, response);
            
        } catch (NumberFormatException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "無効なプロジェクトIDです");
        }
    }
    
    private void getProjectMembersJson(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        
        String projectIdStr = request.getParameter("projectId");
        String month = request.getParameter("month");
        
        if (projectIdStr == null || month == null) {
            out.print("{\"success\": false, \"message\": \"パラメータが不足しています\"}");
            return;
        }
        
        try {
            int projectId = Integer.parseInt(projectIdStr);
            
            // プロジェクト情報を取得
            ProjectBean project = projectDao.findById(projectId);
            if (project == null) {
                out.print("{\"success\": false, \"message\": \"プロジェクトが見つかりません\"}");
                return;
            }
            
            // プロジェクトメンバーの実績データを取得
            List<ProjectMemberReportBean> memberReports = budgetReportDao.getProjectMemberReports(projectId, month);
            
            // 総集計を計算
            BigDecimal totalActual = BigDecimal.ZERO;
            for (ProjectMemberReportBean member : memberReports) {
                if (member.getActualAmount() != null) {
                    totalActual = totalActual.add(member.getActualAmount());
                }
            }
            
            // 予算実績差異を計算
            BigDecimal budgetVariance = null;
            if (project.getBudget() != null && project.getBudget() > 0) {
                budgetVariance = new BigDecimal(project.getBudget()).subtract(totalActual);
            }
            
            // JSON レスポンスを構築（エスケープ処理付き）
            StringBuilder jsonResponse = new StringBuilder();
            jsonResponse.append("{");
            jsonResponse.append("\"success\": true,");
            jsonResponse.append("\"data\": {");
            
            // プロジェクト情報
            jsonResponse.append("\"project\": {");
            jsonResponse.append("\"projectName\": \"").append(escapeJson(project.getProjectName() != null ? project.getProjectName() : "")).append("\",");
            jsonResponse.append("\"budget\": ").append(project.getBudget() != null ? project.getBudget() : 0).append(",");
            jsonResponse.append("\"month\": \"").append(escapeJson(month)).append("\"");
            jsonResponse.append("},");
            
            // メンバーレポート
            jsonResponse.append("\"memberReports\": [");
            for (int i = 0; i < memberReports.size(); i++) {
                ProjectMemberReportBean member = memberReports.get(i);
                if (i > 0) jsonResponse.append(",");
                jsonResponse.append("{");
                jsonResponse.append("\"empNo\": \"").append(escapeJson(member.getEmpNo() != null ? member.getEmpNo() : "")).append("\",");
                jsonResponse.append("\"empName\": \"").append(escapeJson(member.getEmpName() != null ? member.getEmpName() : "")).append("\",");
                jsonResponse.append("\"totalHours\": ").append(member.getTotalHours() != null ? member.getTotalHours() : 0).append(",");
                jsonResponse.append("\"hourlyRate\": ").append(member.getHourlyRate() != null ? member.getHourlyRate() : 0).append(",");
                jsonResponse.append("\"actualAmount\": ").append(member.getActualAmount() != null ? member.getActualAmount() : 0);
                jsonResponse.append("}");
            }
            jsonResponse.append("],");
            
            // 合計値
            jsonResponse.append("\"totalActual\": ").append(totalActual).append(",");
            jsonResponse.append("\"budgetVariance\": ").append(budgetVariance != null ? budgetVariance : 0);
            jsonResponse.append("}}");
            
            out.print(jsonResponse.toString());
            
        } catch (NumberFormatException e) {
            out.print("{\"success\": false, \"message\": \"無効なプロジェクトIDです\"}");
        } catch (Exception e) {
            out.print("{\"success\": false, \"message\": \"サーバーエラーが発生しました: " + e.getMessage() + "\"}");
        } finally {
            out.flush();
        }
    }
    
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        // セッションチェック
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "ログインが必要です");
            return;
        }
        
        // 管理者権限チェック
        UserBean user = (UserBean) session.getAttribute("user");
        if (user.getRoleId() != 1) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "管理者権限が必要です");
            return;
        }
        
        String action = request.getParameter("action");
        
        if ("updateHourlyRates".equals(action)) {
            updateHourlyRates(request, response);
        } else {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "無効なアクションです");
        }
    }
    
    private void updateHourlyRates(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        
        try {
            String projectIdStr = request.getParameter("projectId");
            String month = request.getParameter("month");
            String hourlyRateDataStr = request.getParameter("hourlyRateData");
            
            // デバッグ用ログ
            System.out.println("updateHourlyRates called with projectId: " + projectIdStr + ", month: " + month);
            System.out.println("hourlyRateData: " + hourlyRateDataStr);
            
            if (projectIdStr == null || month == null || hourlyRateDataStr == null) {
                out.print("{\"success\": false, \"message\": \"パラメータが不足しています\"}");
                return;
            }
            
            int projectId = Integer.parseInt(projectIdStr);
            
            // 手動でJSON形式の時給データを解析（簡略化）
            hourlyRateDataStr = hourlyRateDataStr.trim();
            if (hourlyRateDataStr.startsWith("[") && hourlyRateDataStr.endsWith("]")) {
                hourlyRateDataStr = hourlyRateDataStr.substring(1, hourlyRateDataStr.length() - 1);
                
                if (!hourlyRateDataStr.isEmpty()) {
                    // より簡単な解析方法
                    String[] items = hourlyRateDataStr.split("\\},\\{");
                    for (String item : items) {
                        item = item.replace("{", "").replace("}", "").replace("\"", "");
                        
                        String empNo = null;
                        BigDecimal hourlyRate = null;
                        
                        // empNo:値,hourlyRate:値の形式を想定
                        String[] pairs = item.split(",");
                        for (String pair : pairs) {
                            String[] keyValue = pair.split(":");
                            if (keyValue.length == 2) {
                                String key = keyValue[0].trim();
                                String value = keyValue[1].trim();
                                
                                if ("empNo".equals(key)) {
                                    empNo = value;
                                } else if ("hourlyRate".equals(key)) {
                                    try {
                                        // 値が空文字またはゼロの場合はスキップ
                                        if (value.isEmpty() || "0".equals(value)) {
                                            hourlyRate = BigDecimal.ZERO;
                                        } else {
                                            hourlyRate = new BigDecimal(value);
                                        }
                                    } catch (NumberFormatException e) {
                                        hourlyRate = BigDecimal.ZERO;
                                    }
                                }
                            }
                        }
                        
                        // データベースの時給を更新（時給が0より大きい場合のみ）
                        if (empNo != null && hourlyRate != null && hourlyRate.compareTo(BigDecimal.ZERO) > 0) {
                            System.out.println("Updating hourly rate for empNo: " + empNo + ", rate: " + hourlyRate);
                            boolean success = budgetReportDao.updateHourlyRate(empNo, projectId, month, hourlyRate);
                            System.out.println("Update result: " + success);
                        }
                    }
                }
            }
            
            // 更新後のデータを再取得（計算結果付き）
            ProjectBean project = projectDao.findById(projectId);
            List<ProjectMemberReportBean> memberReports = budgetReportDao.getProjectMemberReportsWithCalculation(projectId, month);
            
            // 総実績額を計算
            BigDecimal totalActual = BigDecimal.ZERO;
            for (ProjectMemberReportBean member : memberReports) {
                if (member.getActualAmount() != null) {
                    totalActual = totalActual.add(member.getActualAmount());
                }
            }
            
            // 予算実績差異を計算
            BigDecimal budgetVariance = BigDecimal.ZERO;
            if (project.getBudget() != null && project.getBudget() > 0) {
                budgetVariance = new BigDecimal(project.getBudget()).subtract(totalActual);
            }
            
            // JSON レスポンスを構築（シンプルに）
            StringBuilder jsonResponse = new StringBuilder();
            jsonResponse.append("{\"success\": true, \"data\": {");
            jsonResponse.append("\"memberReports\": [");
            
            for (int i = 0; i < memberReports.size(); i++) {
                ProjectMemberReportBean member = memberReports.get(i);
                if (i > 0) jsonResponse.append(",");
                jsonResponse.append("{");
                jsonResponse.append("\"empNo\": \"").append(member.getEmpNo() != null ? member.getEmpNo() : "").append("\",");
                jsonResponse.append("\"actualAmount\": ").append(member.getActualAmount() != null ? member.getActualAmount() : 0);
                jsonResponse.append("}");
            }
            
            jsonResponse.append("],");
            jsonResponse.append("\"totalActual\": ").append(totalActual).append(",");
            jsonResponse.append("\"budgetVariance\": ").append(budgetVariance);
            jsonResponse.append("}}");
            
            out.print(jsonResponse.toString());
            
        } catch (Exception e) {
            e.printStackTrace(); // デバッグ用
            out.print("{\"success\": false, \"message\": \"サーバーエラーが発生しました: " + escapeJson(e.getMessage()) + "\"}");
        } finally {
            out.flush();
        }
    }
    
    // JSON文字列エスケープメソッド
    private String escapeJson(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\b", "\\b")
                   .replace("\f", "\\f")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }
}