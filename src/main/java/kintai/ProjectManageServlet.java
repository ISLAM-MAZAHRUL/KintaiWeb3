	package kintai;
	
	
	import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
	
	
	@WebServlet("/projectManage")
	public class ProjectManageServlet extends HttpServlet {
		private static final long serialVersionUID = 1L;
		
		private ProjectDao ProjectDao = new ProjectDao();
		
		
		protected void doGet(HttpServletRequest request, HttpServletResponse 
				response) throws ServletException, IOException {
			
			String action = request.getParameter("action");
			
			if ("showProjectList".equals(action)) {
				// プロジェクト一覧の弹出窗口
				List<ProjectBean> projectmanagelist = ProjectDao.findAll();
				request.setAttribute("projectmanagelist", projectmanagelist);
				
				RequestDispatcher dispatcher = request.getRequestDispatcher("/web/project_list_popup.jsp");
				dispatcher.forward(request, response);
			} else {
				// 通常のプロジェクト管理ページ
				List<ProjectBean> projectmanagelist = ProjectDao.findAll();
				request.setAttribute("projectmanagelist", projectmanagelist);
				
				RequestDispatcher dispatcher = request.getRequestDispatcher("/web/project_manage.jsp");
				dispatcher.forward(request, response);
			}
		}
	
		protected void doPost(HttpServletRequest request, HttpServletResponse 
				response) throws ServletException, IOException {
			
			 // セッションチェック
	        HttpSession session = request.getSession(false);
	        if (session == null || session.getAttribute("user") == null) {
	            response.sendRedirect(request.getContextPath() + "/web/login.jsp");
	            return;
	        }
	        
	        // 管理者権限チェック (UserBeanのgetRole()からgetRoleId()へ変更)
	        UserBean user = (UserBean) session.getAttribute("user");
	        if (user.getRoleId() != 1) { // ROLEIDが1が管理者
	            response.sendRedirect(request.getContextPath() + "/web/menu.jsp");
	            return;
	        }
	        
	        // アクションを取得
	        String action = request.getParameter("action");
	        
	        boolean success = false;
	        String message = "";
	        
	        try {
	            switch (action) {
	                case "add":
	                    // 新規追加処理
	//                    String newProjectId = request.getParameter("ProjectId");
	                    String newProjectName = request.getParameter("ProjectName");
	                    String newBudgetAmount = request.getParameter("BudgetAmount");
	                    String newStartDate = request.getParameter("StartDate");
	                    String newEndDate = request.getParameter("EndDate");
	                  
	                    // 入力チェック (最低限のチェック、詳細なビジネスロジックはDAOやサービス層で)
	                    if (//newProjectId == null || newProjectId.trim().isEmpty() || 
	                        newProjectName == null || newProjectName.trim().isEmpty() //||
	//                        newBudgetAmount == null || newBudgetAmount.trim().isEmpty() ||
	//                        newStartDate == null || newStartDate.trim().isEmpty() ||
	//                        newEndDate == null || newEndDate.trim().isEmpty())
	                        ){
	                        message = "必須項目をすべて入力してください";
	                        break;
	                    }
	                    
	                    // 日付の前後チェック
	                    if (newStartDate != null && !newStartDate.trim().isEmpty()
	                    		 && newEndDate != null && !newEndDate.trim().isEmpty()) {
	                    		    LocalDate start = LocalDate.parse(newStartDate);
	                    		    LocalDate end = LocalDate.parse(newEndDate);
	                    		    if (start.isAfter(end)) {
	                    		        message = "開始日は終了日以前に設定してください。";
	                    		        break;
	                    		    }
	                    		}
	                    
	                    ProjectBean newProject = new ProjectBean();
	//					newProject.setProjectId(Integer.parseInt(newProjectId));
	                    newProject.setProjectName(newProjectName);
	                    Integer budget = null;
	                    if (newBudgetAmount != null && !newBudgetAmount.trim().isEmpty()) {
	                        budget = Integer.parseInt(newBudgetAmount);
	                    }
	                    newProject.setBudgetAmount(budget != null ? budget : 0);
	//                  newProject.setStartDate(newStartdateDate);
	//                  newProject.setEndDate(newEnddateDate);
	                    
	                    // StartDateとEndDateはnull許容として、JSPからの入力がない場合はnull
	                    if (newStartDate != null && !newStartDate.trim().isEmpty()) {
	                        newProject.setStartDate(java.time.LocalDate.parse(newStartDate));
	                    }
	                    
	                    
	                    if (newEndDate != null && !newEndDate.trim().isEmpty()) {
	                        newProject.setEndDate(java.time.LocalDate.parse(newEndDate));
	                    }
	                    
	                    
	                    success = ProjectDao.insert(newProject, user.getEmpId(), user.getEmpId());
	                    message = success ? "プロジェクトを追加しました" : "プロジェクトの追加に失敗しました";
	                    break;
	                    
	                case "update":
	                    // 更新処理
	                    String updateProjectId = request.getParameter("ProjectId");
	                    String updateProjectName = request.getParameter("ProjectName");
	                    String updateBudgetAmount = request.getParameter("BudgetAmount");
	                    String updateStartDate = request.getParameter("StartDate");
	                    String updateEndDate = request.getParameter("EndDate");
	                  
	                    // 入力チェック
	                    if (updateProjectName == null || updateProjectName.trim().isEmpty() //||
	//                    	updateBudgetAmount == null || updateBudgetAmount.trim().isEmpty() ||
	//                        updateStartDate == null || updateStartDate.trim().isEmpty() ||
	//                        updateEndDate == null || updateEndDate.trim().isEmpty() ) {
	                    	) {
	                        message = "必須項目をすべて入力してください";
	                        break;
	                    }
	                    
	                    // 日付の前後チェック
	                    if (updateStartDate != null && !updateStartDate.trim().isEmpty()
	                    		 && updateEndDate != null && !updateEndDate.trim().isEmpty()) {
	                    		    LocalDate start = LocalDate.parse(updateStartDate);
	                    		    LocalDate end = LocalDate.parse(updateEndDate);
	                    		    if (start.isAfter(end)) {
	                    		        message = "開始日は終了日以前に設定してください。";
	                    		        break;
	                    		    }
	                    		}
	                    
	                    // プロジェクト番号の重複チェック
	//                    if (ProjectDao.exists(updateProjectId)) {
	//                        message = "プロジェクト：「" + updateProjectId + "」は既に存在します";
	//                        break;
	//                    }
	                    
	                    
	                    ProjectBean updateProject = new ProjectBean();
	                    updateProject.setProjectId(Integer.parseInt(updateProjectId));
	                    updateProject.setProjectName(updateProjectName);
	                    Integer budgetUpd = null;
	                    if (updateBudgetAmount != null && !updateBudgetAmount.trim().isEmpty()) {
	                        budgetUpd = Integer.parseInt(updateBudgetAmount);
	                    }
	                    updateProject.setBudgetAmount(budgetUpd != null ? budgetUpd : 0);
	//                    updateProject.setStartDate(updateStartdateDate);
	//                    updateProject.setEndDate(updateEnddateDate);
	//                    if (updateEmpDateStr != null && !updateEmpDateStr.trim().isEmpty()) {
	//                        updateEmp.setEmpDate(java.time.LocalDate.parse(updateEmpDateStr));
	//                    } else {
	//                        updateEmp.setEmpDate(null); // 入力がない場合はnull
	//                    }
	                    
	                 // StartDateとEndDateはnull許容として、JSPからの入力がない場合はnull
	                    if (updateStartDate != null && !updateStartDate.trim().isEmpty()) {
	                        updateProject.setStartDate(java.time.LocalDate.parse(updateStartDate));
	                    }
	                    
	                    
	                    if (updateEndDate != null && !updateEndDate.trim().isEmpty()) {
	                        updateProject.setEndDate(java.time.LocalDate.parse(updateEndDate));
	                    }
	                    
	                    
	                    success = ProjectDao.update(updateProject, user.getEmpId());
	                    message = success ? "プロジェクト名を更新しました" : "プロジェクト情報の更新に失敗しました";
	                    break;
	                    
	                case "delete":
	                    // 削除処理
	                    String deleteProjectIdStr = request.getParameter("ProjectId");
	                    int deleteProjectId = Integer.parseInt(deleteProjectIdStr);
	                    //success = ProjectDao.delete(deleteProjectId);
	                    success = ProjectDao.logicalDelete(deleteProjectId, user.getEmpId());
	                    
	                    if (success) {
	                        message = "プロジェクトを削除しました";
	                    } else {
	                        message = "プロジェクトの削除に失敗しました。この従業員に関連するデータが存在する可能性があります。"; // メッセージを一般化
	                    }
	                    break;
	                    
	                default:
	                    message = "不正な操作です";
	            }
	        } catch (NumberFormatException e) 	{
	            e.printStackTrace();
	            message = "入力された数値（予算）が不正です";
	        } catch (java.time.format.DateTimeParseException e) {
	            e.printStackTrace();
	            message = "プロジェクト開始日/終了日の形式が不正です。YYYY-MM-DD形式で入力してください。";
	        } catch (Exception e) {
	            e.printStackTrace();
	            message = "処理中にエラーが発生しました";
	        }
	        
	        // 処理結果をリクエスト属性に設定
	        request.setAttribute("message", message);
	        request.setAttribute("success", success);
	        
			doGet(request, response);
		}
		
		
	
	}
