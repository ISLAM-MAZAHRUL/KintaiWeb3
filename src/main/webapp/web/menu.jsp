<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="kintai.UserBean" %>
<%@ page import="kintai.UserDao" %>
<%@ page import="kintai.AnnouncementBean" %>
<%@ page import="kintai.AnnouncementDao" %>
<%@ page import="kintai.KintaiRecDao" %>
<%@ page import="kintai.MonthlySummaryBean" %>
<%@ page import="java.util.List, java.util.Map" %>
<%@ page import="kintai.KinmuManageBean" %>
<%@ page import="java.time.format.DateTimeFormatter" %>
<%@ page import="java.time.LocalDate" %>
<%@ page import="kintai.LeaveRecBean" %>
<%@ page import="kintai.LeaveTypeDao" %>
<%@ page import="kintai.LeaveTypeBean" %>
<%
    UserBean user = (UserBean) session.getAttribute("user");
    // ログインチェック
    if (user == null) {
        response.sendRedirect(request.getContextPath() + "/web/login.jsp");
        return;
    }
    
    String deptname = (String)session.getAttribute("deptname");
    
    // アナウンス情報を取得
    AnnouncementDao announcementDao = new AnnouncementDao();
    List<AnnouncementBean> announcements = announcementDao.findActiveAnnouncements();
    
    // 統計データを取得
    KintaiRecDao kintaiRecDao = new KintaiRecDao();
    String currentMonth = java.time.YearMonth.now().toString();
    MonthlySummaryBean monthlySummary = null;
    try {
        monthlySummary = kintaiRecDao.getMonthlySummary(user.getEmpId(), currentMonth);
    } catch (Exception e) {
        e.printStackTrace();
    }
    
    String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日"));
    Map<String, String> workTimeData = (Map<String, String>) session.getAttribute("workTimeData");
    
    boolean hasClockedIn = workTimeData.get("clockInTime") != null;
    boolean hasClockedOut = workTimeData.get("clockOutTime") != null;
    

    

%>
<html>
  <head>
  	<meta name="viewport" content="width=device-width, initial-scale=1">
  
    <title>勤怠管理システムメニュー</title>

    <style>
      body {
        margin: 0;
        font-family: "メイリオ", sans-serif;
        background: #f5f5f5;
        font-size: 14px;
      }
      .container {
        max-width: 1400px;
        margin: 0 auto;
        background-color: white;
        min-height: 100vh;
        max-height: 100vh;
        overflow: hidden;
        box-shadow: 0 0 10px rgba(0, 0, 0, 0.1);
      }
      .header {
        display: flex;
        justify-content: space-between;
        align-items: flex-start;
        padding: 15px 20px;
        background: #fff;
        border-bottom: 2px solid #007bff;
      }
      .user-info {
        display: flex;
        flex-direction: column;
        line-height: 1.5;
      }
      .user-info p {
        margin: 2px 0;
        color: #333;
      }
      .logout-button {
        background-color: #dc3545;
        color: white;
        border: 1px solid #dc3545;
        border-radius: 5px;
        padding: 8px 16px;
        cursor: pointer;
        font-size: 1em;
        text-decoration: none;
		transition: background-color 0.3s;
      }
      .logout-button:hover {
        background-color: #c82333;
        border-color: #bd2130;
      }
      
      .password-button {
		background-color: #f0f6fc;
	    color: #336699;
	    border: 1px solid #cce0f5;
	    padding: 8px 16px;
	    border-radius: 5px;
	    cursor: pointer;
	  }
	  .password-button:hover {
		background-color: #f0f6fc;
	  }
		      
      

      /* ダッシュボードレイアウト */
      .dashboard {
        padding: 15px;
        display: grid;
        grid-template-columns: 1fr 1fr;
        grid-template-rows: auto auto auto;
        gap: 15px;
        height: calc(100vh - 100px);
        overflow-y: auto;
        overflow-x: hidden;
      }

      .dashboard h1 {
        grid-column: 1 / -1;
        display: flex;
        align-items: center;
        justify-content: center;
        text-align: center;
        color: #333;
        margin: 0 0 5px 0;
        font-size: 1.6em;
        border-bottom: 2px solid #007bff;
        padding-bottom: 2px;
        min-height: 45px;
      }

      /* ウィジェットの共通スタイル */
      .widget {
        background: white;
        border: 1px solid #ddd;
        border-radius: 8px;
        padding: 12px;
        box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
        transition: box-shadow 0.3s;
      }

      .widget:hover {
        box-shadow: 0 4px 8px rgba(0, 0, 0, 0.15);
      }

      .widget h2 {
        margin: 0 0 12px 0;
        color: #007bff;
        font-size: 1.2em;
        border-bottom: 1px solid #eee;
        padding-bottom: 6px;
      }

      /* 打刻ウィジェット */
      .punch-widget {
        display: flex;
        flex-direction: column;
        align-items: stretch;
        text-align: center;
      }
     
      .punch-panel { 
      	display: flex; 
      	grid-template-columns: 1fr 1fr; 
      	gap: 12px; 
      	margin-bottom: 15px; 
      	width: 100%;
      	align-items: stretch;
      }
      
      .punch-button { 
	      font-size: 14px;
	      width: 100%;
	      padding: 12px; 
	      cursor: pointer; 
	      border-radius: 6px; 
	      border: 1px solid; 
      }
      .punch-button:disabled { 
	      background-color: #e9ecef; 
	      color: #6c757d; 
	      cursor: not-allowed; 
      }
      .clock-in { 
	      background-color: #28a745;
		  border-color: #28a745; 
		  color: white; 
	  }
      .clock-out { 
	      background-color: #dc3545; 
	      border-color: #dc3545; 
	      color: white; 
      }
      
      .status-display { 
	      text-align: center; 
	      margin-bottom: 12px; 
	      font-size: 13px; 
	      background-color: #f8f9fa; 
	      padding: 8px; 
	      border-radius: 4px; 
      }
      
      .widget .btn {
        width: 97%;
        margin: 0 auto 8px auto; /* 上0 下8px 左右自動で中央寄せ */
        padding: 8px;
        background: #007bff;
        color: white;
        border: none;
        border-radius: 5px;
        font-size: 1.0em;
        cursor: pointer;
        text-decoration: none;
        display: block;
        transition: background-color 0.3s;
        margin-bottom: 8px;
      }

      .widget .btn:hover {
        background: #0056b3;
      }

<!--      .punch-buttons {-->
<!--        display: grid;-->
<!--        grid-template-columns: 1fr 1fr;-->
<!--        gap: 10px;-->
<!--        width: 100%;-->
<!--        margin-bottom: 15px;-->
<!--      }-->

<!--      .punch-btn {-->
<!--        padding: 10px;-->
<!--        border: none;-->
<!--        border-radius: 5px;-->
<!--        font-size: 1.0em;-->
<!--        font-weight: bold;-->
<!--        cursor: pointer;-->
<!--        transition: all 0.3s;-->
<!--        text-decoration: none;-->
<!--        text-align: center;-->
<!--        display: block;-->
<!--      }-->

<!--      .punch-btn.start {-->
<!--        background: #28a745;-->
<!--        color: white;-->
<!--      }-->
<!--      .punch-btn.start:hover {-->
<!--        background: #218838;-->
<!--      }-->

<!--      .punch-btn.end {-->
<!--        background: #dc3545;-->
<!--        color: white;-->
<!--      }-->
<!--      .punch-btn.end:hover {-->
<!--        background: #c82333;-->
<!--      }-->

      .current-time {
        font-size: 1.0em;
        font-weight: bold;
        color: #333;
        margin-bottom: 8px;
      }

      /* パスワード変更ウィジェット */
      .password-widget {
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: flex-start;
        text-align: center;
      }

      .password-widget h2 {
        text-align: left;
        margin: 0 0 12px 0;
        border-bottom: 1px solid #eee;
        padding-bottom: 6px;
        font-size: 1.2em;
        color: #007bff;
      }

      .password-widget .btn {
        width: 95%;
        padding: 8px;
        background: #007bff;
        color: white;
        border: none;
        border-radius: 5px;
        font-size: 1.0em;
        cursor: pointer;
        text-decoration: none;
        display: block;
        text-align: center;
        transition: background-color 0.3s;
        margin-bottom: 8px;
      }

      .password-widget .btn:hover {
        background: #0056b3;
      }

      .password-widget .account-info {
        color: #666;
        font-size: 0.8em;
        margin-top: 8px;
        line-height: 1.3;
      }

      /* 記録表示ウィジェット */
      .records-widget {
        grid-column: 1 / -1;
      }

      .records-summary {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
        gap: 12px;
        margin-bottom: 12px;
      }

      .summary-item {
        background: #f8f9fa;
        padding: 12px;
        border-radius: 5px;
        text-align: center;
        border-left: 4px solid #007bff;
      }

      .summary-item .label {
        font-size: 0.85em;
        color: #666;
        margin-bottom: 4px;
      }

      .summary-item .value {
        font-size: 1.3em;
        font-weight: bold;
        color: #333;
      }

      .view-all-btn {
        width: 100%;
        padding: 10px;
        background: #6c757d;
        color: white;
        border: none;
        border-radius: 5px;
        cursor: pointer;
        text-decoration: none;
        display: block;
        text-align: center;
        transition: background-color 0.3s;
      }

      .view-all-btn:hover {
        background: #545b62;
      }

      /* レスポンシブ対応 */
        @media (max-width: 1024px) {
            .dashboard {
                grid-template-columns: 1fr 1fr;
                height: calc(100vh - 120px);
                overflow-y: auto;
                overflow-x: hidden;
            }
            
            .main-content {
                flex-direction: column;
            }
            
            .sidebar {
                width: 100%;
                border-left: none;
                border-top: 1px solid #dee2e6;
                position: static;
                max-height: none;
            }
            
            .content-area {
                padding-right: 0;
            }
        }
        
        @media (max-width: 768px) {
            .dashboard {
                grid-template-columns: 1fr;
                padding: 12px;
                height: calc(100vh - 140px);
                overflow-y: auto;
                overflow-x: hidden;
            }
            
            .container {
                max-height: 100vh;
                overflow: hidden;
            }
            
            .sidebar {
                padding: 15px;
            }
            
            .widget {
                padding: 10px;
            }
        }
        
        /* 極小画面対応 */
        @media (max-width: 480px) {
            .dashboard {
                height: calc(100vh - 160px);
                padding: 8px;
                gap: 10px;
            }
            
            .widget {
                padding: 8px;
            }
            
            .punch-btn {
                padding: 8px;
                font-size: 0.9em;
            }
            
            .view-all-btn {
                padding: 8px;
                font-size: 0.9em;
            }
        }
      
      /* アナウンス横幅バナーのスタイル */
      .announcement-banner {
        background: linear-gradient(135deg, #6f42c1 0%, #5a32a3 100%);
        color: white;
        box-shadow: 0 2px 8px rgba(111, 66, 193, 0.3);
        margin-bottom: 0;
      }
      
      .banner-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 15px 20px;
        border-bottom: 1px solid rgba(255, 255, 255, 0.2);
      }
      
      .announcement-preview {
        flex: 1;
        margin: 0 20px;
        text-align: center;
        max-width: 500px;
        overflow: hidden;
      }
      
      .preview-title {
        color: rgba(255, 255, 255, 0.9);
        font-size: 1.0em;
        cursor: pointer;
        transition: color 0.3s;
        text-decoration: underline;
        text-decoration-color: transparent;
        transition: all 0.3s;
        font-weight: 500;
      }
      
      .preview-title:hover {
        color: white;
        text-decoration-color: white;
      }
      
      .preview-more {
        color: rgba(255, 255, 255, 0.7);
        font-size: 0.8em;
        margin-left: 10px;
        cursor: pointer;
        transition: color 0.3s;
        text-decoration: underline;
        text-decoration-color: transparent;
      }
      
      .preview-more:hover {
        color: rgba(255, 255, 255, 0.9);
        text-decoration-color: rgba(255, 255, 255, 0.7);
      }
      
      .preview-empty {
        color: rgba(255, 255, 255, 0.6);
        font-size: 0.8em;
        font-style: italic;
      }
      
      .banner-title {
        margin: 0;
        font-size: 1.3em;
        display: flex;
        align-items: center;
        gap: 10px;
      }
      
      .banner-icon {
        font-size: 1.2em;
      }
      
      .banner-toggle {
        background: none;
        border: none;
        color: white;
        font-size: 0.85em;
        cursor: pointer;
        padding: 5px;
        margin-left: 10px;
        transition: transform 0.3s;
      }
      
      .banner-toggle:hover {
        transform: scale(1.1);
      }
      
      .banner-content {
        padding: 20px;
        transition: all 0.3s ease;
        overflow: hidden;
      }
      
      .banner-content.collapsed {
        padding: 0;
        max-height: 0;
        opacity: 0;
      }
      
      .announcement-grid {
        display: grid;
        grid-template-columns: repeat(3, 1fr);
        gap: 15px;
        margin-bottom: 15px;
        grid-auto-flow: column;
      }
      
      .announcement-grid.waterfall {
        display: grid;
        grid-template-columns: repeat(3, 1fr);
        gap: 15px;
        grid-auto-flow: row;
      }
      
      .announcement-grid.waterfall .announcement-card {
        display: block;
        margin-bottom: 0;
        break-inside: unset;
      }
      
      .announcement-card {
        background: rgba(255, 255, 255, 0.95);
        border-radius: 8px;
        padding: 15px;
        box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
        transition: transform 0.2s, box-shadow 0.2s;
      }
      
      .announcement-card:hover {
        transform: translateY(-2px);
        box-shadow: 0 4px 8px rgba(0, 0, 0, 0.15);
      }
      
      .announcement-title {
        margin: 0 0 10px 0;
        color: #6f42c1;
        font-size: 1.1em;
        cursor: pointer;
        transition: color 0.3s;
        padding: 5px;
        border-radius: 4px;
      }
      
      .announcement-title:hover {
        background: #f8f9fa;
        color: #5a32a3;
      }
      
      .announcement-meta {
        display: flex;
        justify-content: space-between;
        align-items: center;
      }
      
      .announcement-date {
        font-size: 0.85em;
        color: #6c757d;
        font-weight: normal;
      }
      
      .more-section {
        text-align: center;
        margin-top: 15px;
      }
      
      .more-announcements-btn, .less-announcements-btn {
        background: rgba(255, 255, 255, 0.9);
        color: #6f42c1;
        border: 1px solid rgba(255, 255, 255, 0.3);
        border-radius: 20px;
        padding: 8px 16px;
        cursor: pointer;
        font-size: 0.9em;
        transition: all 0.3s;
      }
      
      .more-announcements-btn:hover, .less-announcements-btn:hover {
        background: white;
        transform: translateY(-1px);
        box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
      }
      
      .no-announcement {
        color: #6c757d;
        font-style: italic;
        text-align: center;
        padding: 20px;
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
        background-color: #fefefe;
        margin: 15% auto;
        padding: 20px;
        border: 1px solid #888;
        border-radius: 8px;
        width: 50%;
        max-width: 600px;
        position: relative;
      }
      
      .modal-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 15px;
        border-bottom: 1px solid #dee2e6;
        padding-bottom: 10px;
      }
      
      .modal-title {
        margin: 0;
        color: #6f42c1;
        font-size: 1.2em;
      }
      
      .close {
        color: #aaa;
        font-size: 28px;
        font-weight: bold;
        cursor: pointer;
      }
      
      .close:hover {
        color: #000;
      }
      
      .modal-body {
        line-height: 1.6;
        color: #333;
        word-wrap: break-word;
        word-break: break-all;
        overflow-wrap: break-word;
      }
      
      .modal-date {
        color: #6c757d;
        font-size: 0.9em;
        margin-bottom: 10px;
      }
      
      .announcement-edit-btn {
            background: #ffc107;
            color: #333;
            border: none;
            border-radius: 15px;
            padding: 4px 8px;
            cursor: pointer;
            font-size: 0.8em;
            transition: all 0.3s;
            position: relative;
            z-index: 10;
        }
        
        .announcement-edit-btn:hover {
            background: #e0a800;
            transform: scale(1.05);
        }
        
        .announcement-delete-btn {
            background: #dc3545;
            color: white;
            border: none;
            border-radius: 15px;
            padding: 4px 8px;
            cursor: pointer;
            font-size: 0.8em;
            transition: all 0.3s;
            position: relative;
            z-index: 10;
            margin-left: 5px;
        }
        
        .announcement-delete-btn:hover {
            background: #c82333;
            transform: scale(1.05);
        }
        
    </style>
  </head>
  <body>
    <div class="container">
      <%-- アナウンス横幅バナー --%>
      <div class="announcement-banner">
        <div class="banner-header">
          <h2 class="banner-title">
            <i class="banner-icon">📢</i> アナウンス
            <button class="banner-toggle" onclick="toggleBanner()" id="bannerToggle">▶ <span id="bannerToggleText">展開</span></button>
          </h2>
          
          <!-- 公告标题预览区域 -->
          <div class="announcement-preview" id="announcementPreview">
            <% if (announcements != null && !announcements.isEmpty()) { %>
              <% for (int i = 0; i < Math.min(3, announcements.size()); i++) { 
                  AnnouncementBean announcement = announcements.get(i);
              %>
                  <span class="preview-title" onclick="expandAndShowDetail(<%= announcement.getAnnouncementId() %>, '<%= announcement.getTitle().replace("'", "\\'") %>', '<%= announcement.getContent().replace("'", "\\'").replace("\n", "\\n") %>', '<%= announcement.getCreatedAt() != null ? announcement.getCreatedAt().toLocalDate().toString() : "" %>')">
                      <%= (i + 1) %>. <%= announcement.getTitle() %><%= i < Math.min(3, announcements.size()) - 1 ? " ｜ " : "" %>
                  </span>
              <% } %>
              <% if (announcements.size() > 3) { %>
                  <span class="preview-more" onclick="expandAnnouncements()">他<%= announcements.size() - 3 %>件</span>
              <% } %>
            <% } else { %>
                <span class="preview-empty">現在、アナウンスはありません</span>
            <% } %>
          </div>
        </div>
        
        <div class="banner-content collapsed" id="bannerContent">
          <% if (announcements != null && !announcements.isEmpty()) { %>
            <div class="announcement-grid">
              <% 
              int displayCount = Math.min(3, announcements.size());
              for (int i = 0; i < displayCount; i++) { 
                  AnnouncementBean announcement = announcements.get(i);
                  String dateStr = "";
                  if (announcement.getCreatedAt() != null) {
                      dateStr = announcement.getCreatedAt().toLocalDate().toString();
                  }
              %>
                <div class="announcement-card">
                  <h3 class="announcement-title" onclick="showAnnouncementDetail(<%= announcement.getAnnouncementId() %>, '<%= announcement.getTitle().replace("'", "\\'") %>', '<%= announcement.getContent().replace("'", "\\'").replace("\n", "\\n") %>', '<%= dateStr %>')">
                    <%= (i + 1) %>. <%= announcement.getTitle() %>
                  </h3>
                  <div class="announcement-meta">
                    <span class="announcement-date"><%= dateStr %></span>
                  </div>
                </div>
              <% } %>
            </div>
            
            <%
			    List<LeaveRecBean> pendingLeaves = (List<LeaveRecBean>) session.getAttribute("pendingLeaves");
			    UserDao userDao = new UserDao(); 
			    LeaveTypeDao typeDao = new LeaveTypeDao(); // 追加
			    if (pendingLeaves != null && !pendingLeaves.isEmpty()) {
			%>
			    <h3>承認待ち休暇申請</h3>
			    <div id="announcement-grid" class="announcement-grid">
			        <% for (LeaveRecBean leave : pendingLeaves) { 
			            UserBean emp = userDao.findByEmpId(leave.getEmpId());
			            String empName = (emp != null) ? emp.getName() : leave.getEmpId();
			
			            // 休暇種類を取得
			            String leaveTypeName = "不明";
			            LeaveTypeBean type = typeDao.findById(leave.getLeaveTypeId());
			            if (type != null) leaveTypeName = type.getLeaveTypeName();
			
			            String title = empName + " さんの " + leaveTypeName + " 申請";
			            String content = "休暇種類: " + leaveTypeName
			                    + "<br>期間: " + leave.getStartDate() + " ～ " + leave.getEndDate()
			                    + "<br>理由: " + leave.getReason();
			
			            // 作成日
			            String dateStr = "";
			            if (leave.getCreatedAt() != null) {
			                dateStr = leave.getCreatedAt().toLocalDateTime().toLocalDate().toString();
			            }
			        %>
			            <div class="announcement-card">
			                <h3 class="announcement-title" 
			                    onclick="showAnnouncementDetail(<%= leave.getLeaveId() %>, '<%= title.replace("'", "\\'") %>', '<%= content.replace("'", "\\'").replace("\n", "\\n") %>', '<%= dateStr %>')">
			                    <%= title %>（承認待ち）
			                </h3>
			                <% if (user != null && user.getRoleId() == 2) { %>
				            <div class="announcement-actions">
				                <button class="announcement-edit-btn" 
				                        onclick="event.stopPropagation(); approveLeave(<%= leave.getLeaveId() %>)">
				                    承認
				                </button>
				                <button class="announcement-delete-btn" 
				                        onclick="event.stopPropagation(); rejectLeave(<%= leave.getLeaveId() %>)">
				                    却下
				                </button>
				            </div>
				            <% } %>
			            </div>
			        <% } %>
			    </div>
			<% } %>



            
            <% if (announcements.size() > 3) { %>
              <div class="more-section">
                <button class="more-announcements-btn" onclick="showAllAnnouncements()">さらに表示 (<%= announcements.size() - 3 %>件)</button>
              </div>
            <% } %>
            
            <!-- 隠された全アナウンス表示エリア -->
            <div id="allAnnouncementsArea" style="display: none;">
              <div class="announcement-grid waterfall">
                <% for (int i = 3; i < announcements.size(); i++) { 
                    AnnouncementBean announcement = announcements.get(i);
                    String dateStr = "";
                    if (announcement.getCreatedAt() != null) {
                        dateStr = announcement.getCreatedAt().toLocalDate().toString();
                    }
                %>
                  <div class="announcement-card">
                    <h3 class="announcement-title" onclick="showAnnouncementDetail(<%= announcement.getAnnouncementId() %>, '<%= announcement.getTitle().replace("'", "\\'") %>', '<%= announcement.getContent().replace("'", "\\'").replace("\n", "\\n") %>', '<%= dateStr %>')">
                      <%= (i + 1) %>. <%= announcement.getTitle() %>
                    </h3>
                    <div class="announcement-meta">
                      <span class="announcement-date"><%= dateStr %></span>
                    </div>
                  </div>
                <% } %>
              </div>
              <div class="more-section">
                <button class="less-announcements-btn" onclick="hideExtraAnnouncements()">折りたたむ</button>
              </div>
            </div>
          <% } else { %>
            <div class="no-announcement">
              現在、アナウンスはありません
            </div>
          <% } %>
        </div>
      </div>
      
      <div class="header">
        <div class="user-info">
          <%-- 部署名と氏名を表示 --%>
          <%-- 部署を　営業部→deptnameに変更 --%>
          <p>部署：<%= session.getAttribute("deptName") != null ? session.getAttribute("deptName") : "情報なし" %></p>	
          <p>氏名：<%= user.getName() %><% if (user.getRoleId() == 2) { %> <span style="color: #dc3545; font-weight: bold;">[部長]</span><% } %></p>
        </div>
        
       <div style="display: flex; gap: 10px;">
       		<!-- パスワード変更ボタン -->
		    <form action="<%= request.getContextPath() %>/PasswordChangeServlet" method="get">
		        <input type="submit" value="パスワード変更" class="password-button" />
		    </form>
		
		    <!-- ログアウトボタン -->
		    <form action="<%= request.getContextPath() %>/logout" method="post">
		        <input type="submit" value="ログアウト" class="logout-button" />
		    </form>
		</div>

        
      </div>

      <div class="dashboard">
        <h1><% if (user.getRoleId() == 2) { %>管理職メニュー<% } else { %>基本メニュー<% } %></h1>

        <!-- 今日の打刻ウィジェット -->
        <div class="widget punch-widget">
          <h2>本日分の打刻</h2>
          		<div class="section">
                    <div class="status-display">
                        <strong>出勤:</strong> <%= workTimeData.getOrDefault("clockInTime", "---") %> | 
                        <strong>退勤:</strong> <%= workTimeData.getOrDefault("clockOutTime", "---") %>
                    </div>
                    <form action="<%= request.getContextPath() %>/workPunch" method="post">
                        <div class="punch-panel">
                            <button type="submit" name="action" value="clock_in" class="punch-button clock-in" <%= (hasClockedIn) ? "disabled" : "" %>>出勤</button>
                            <button type="submit" name="action" value="clock_out" class="punch-button clock-out" <%= (!hasClockedIn || hasClockedOut) ? "disabled" : "" %>>退勤</button>
                        </div>
                    </form>
                    <a href="<%= request.getContextPath() %>/KinmuManageServlet" class="btn" >勤務時間管理</a>
                </div>
                
<!--          <div class="current-time" id="currentTime"></div>-->
<!--          <div class="punch-buttons">-->
<!--            <a-->
<!--              href="<%= request.getContextPath() %>/showWorkPunchForm"-->
<!--              class="punch-btn start"-->
<!--              >出勤</a-->
<!--            >-->
<!--            <a-->
<!--              href="<%= request.getContextPath() %>/showWorkPunchForm"-->
<!--              class="punch-btn end"-->
<!--              >退勤</a-->
<!--            >-->
          
          
<!--          <small style="color: #666; text-align: center"-->
<!--            >※クリックして打刻画面へ</small-->
<!--          >-->

        </div>

        <!-- 休暇申請ウィジェット -->
        <div class="widget password-widget">
          <h2>メニュー</h2>
          <a href="<%= request.getContextPath() %>/CalendarManageServlet" class="btn">カレンダー・イベント</a>
          <a href="<%= request.getContextPath() %>/empManage" class="btn">従業員一覧</a>
          <a href="<%= request.getContextPath() %>/leaveRec" class="btn">休暇申請</a>
          <a href="<%= request.getContextPath() %>/web/kinmu_hyo_export.jsp" class="btn" style="background:#6f42c1;">
    					勤務表データ出力

          </a>
<!--          <div class="account-info">-->
<!--            セキュリティ向上のため<br />-->
<!--            定期的なパスワード変更を<br />-->
<!--            推奨しています-->
<!--          </div>-->
        </div>

        <!-- 勤怠記録概要ウィジェット -->
        <div class="widget records-widget">
          <div style="display: grid; grid-template-columns: 1fr; gap: 10px; margin-bottom: 20px">
            <a href="<%= request.getContextPath() %>/KintaiRecServlet"
              class="view-all-btn">
              <% if (user.getRoleId() == 1) { %>従業員別勤怠記録表示<% } else if (user.getRoleId() == 2) { %>勤怠記録表示(自分/部下)<% } else { %>勤怠記録表示<% } %>
              </a>
          </div>
          <div class="records-summary">
            <div class="summary-item">
              <div class="label">今月の出勤日数</div>
              <div class="value" id="workDays">
              <%= monthlySummary != null ? monthlySummary.getAttendanceRateString() : "-" %>
              </div>
            </div>
            <div class="summary-item">
              <div class="label">今月の総稼働時間</div>
              <div class="value" id="totalHours">
               <%= monthlySummary != null ? monthlySummary.getTotalWorkingHoursString() : "-" %>
              </div>
            </div>
            <div class="summary-item">
              <div class="label">今月の残業時間</div>
              <div class="value" id="weekHours">
              <%= monthlySummary != null ? monthlySummary.getTotalOvertimeHoursString() : "-" %>
              </div>
            </div>
            <div class="summary-item">
              <div class="label">今月の休憩時間</div>
              <div class="value" id="avgStartTime">
              <%= monthlySummary != null ? monthlySummary.getTotalBreakHoursString() : "-" %>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- アナウンス詳細モーダル -->
    <div id="announcementModal" class="modal">
      <div class="modal-content">
        <div class="modal-header">
          <h2 class="modal-title" id="modalTitle">アナウンス詳細</h2>
          <span class="close" onclick="closeModal()">&times;</span>
        </div>
        <div class="modal-body">
          <div class="modal-date" id="modalDate"></div>
          <div id="modalContent"></div>
        </div>
      </div>
    </div>

    <script>
      // 現在時刻の表示
      function updateCurrentTime() {
        const now = new Date();
        const timeString = now.toLocaleTimeString("ja-JP", {
          hour: "2-digit",
          minute: "2-digit",
          second: "2-digit",
        });
        const dateString = now.toLocaleDateString("ja-JP", {
          year: "numeric",
          month: "long",
          day: "numeric",
          weekday: "long",
        });
        document.getElementById(
          "currentTime"
        ).innerHTML = `<div style="font-size: 0.8em; color: #666;">${dateString}</div>
                 <div>${timeString}</div>`;
      }

      // 1秒ごとに時刻を更新
      updateCurrentTime();
      setInterval(updateCurrentTime, 1000);

      // 統計データの表示
      document.addEventListener("DOMContentLoaded", function() {
	      <% if (monthlySummary != null) { %>
	        document.getElementById("workDays").textContent = "<%= monthlySummary.getAttendanceRateString() %>";
	        document.getElementById("totalHours").textContent = "<%= monthlySummary.getTotalWorkingHoursString() %>";
	        document.getElementById("weekHours").textContent = "<%= monthlySummary.getTotalOvertimeHoursString() %>";
	        document.getElementById("avgStartTime").textContent = "<%= monthlySummary.getTotalBreakHoursString() %>";
	      <% } else { %>
	        document.getElementById("workDays").textContent = "-";
	        document.getElementById("totalHours").textContent = "-";
	        document.getElementById("weekHours").textContent = "-";
	        document.getElementById("avgStartTime").textContent = "-";
	      <% } %>
      });
      
      // プレビュータイトルクリック時、展開して詳細表示
      function expandAndShowDetail(id, title, content, date) {
        // まずアナウンスエリアを展開
        var bannerContent = document.getElementById('bannerContent');
        var toggle = document.getElementById('bannerToggle');
        if (bannerContent.classList.contains('collapsed')) {
          bannerContent.classList.remove('collapsed');
          toggle.innerHTML = '▼ <span id="bannerToggleText">折りたたむ</span>';
        }
        // その後詳細モーダルを表示
        showAnnouncementDetail(id, title, content, date);
      }
      
      // 「他X件」クリック時アナウンスエリアを展開
      function expandAnnouncements() {
        var bannerContent = document.getElementById('bannerContent');
        var toggle = document.getElementById('bannerToggle');
        if (bannerContent.classList.contains('collapsed')) {
          bannerContent.classList.remove('collapsed');
          toggle.innerHTML = '▼ <span id="bannerToggleText">折りたたむ</span>';
        }
      }
      
      // アナウンス関連のJavaScript
      function showAnnouncementDetail(id, title, content, date) {
        document.getElementById('modalTitle').textContent = title;
        document.getElementById('modalDate').textContent = '投稿日: ' + date;
        document.getElementById('modalContent').innerHTML = content.replace(/\n/g, '<br/>');
        document.getElementById('announcementModal').style.display = 'block';
      }
      
      function closeModal() {
        document.getElementById('announcementModal').style.display = 'none';
      }
      
      // モーダルの外側をクリックしたら閉じる
      window.onclick = function(event) {
        var modal = document.getElementById('announcementModal');
        if (event.target == modal) {
          closeModal();
        }
      }
      
      // more/lessボタンの機能
      function showAllAnnouncements() {
        document.getElementById('allAnnouncementsArea').style.display = 'block';
        // moreボタンを隠す
        var moreBtn = document.querySelector('.more-announcements-btn');
        if (moreBtn) moreBtn.style.display = 'none';
      }
      
      function hideExtraAnnouncements() {
        document.getElementById('allAnnouncementsArea').style.display = 'none';
        // moreボタンを表示
        var moreBtn = document.querySelector('.more-announcements-btn');
        if (moreBtn) moreBtn.style.display = 'inline-block';
      }
      
      // 横幅バナーの折りたたみ機能
      function toggleBanner() {
        var content = document.getElementById('bannerContent');
        var toggle = document.getElementById('bannerToggle');
        var toggleText = document.getElementById('bannerToggleText');
        
        if (content.classList.contains('collapsed')) {
          content.classList.remove('collapsed');
          toggle.innerHTML = '▼ <span id="bannerToggleText">折りたたむ</span>';
        } else {
          content.classList.add('collapsed');
          toggle.innerHTML = '▶ <span id="bannerToggleText">展開</span>';
        }
      }

    //承認ボタンの操作
      function approveLeave(leaveId) {
          if (!confirm("この休暇を承認しますか？")) return;

          fetch('<%=request.getContextPath()%>/leaveApproval', {
              method: 'POST',
              headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
              body: 'leaveId=' + leaveId + '&action=approve'
          })
          .then(response => response.text())
          .then(data => {
              alert("承認しました");
              location.reload();
          })
          .catch(err => alert("エラーが発生しました: " + err));
      }

		// 却下ボタンの操作
      function rejectLeave(leaveId) {
          if (!confirm("この休暇を却下しますか？")) return;

          fetch('<%=request.getContextPath()%>/leaveApproval', {
              method: 'POST',
              headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
              body: 'leaveId=' + leaveId + '&action=reject'
          })
          .then(response => response.text())
          .then(data => {
              alert("却下しました");
              location.reload();
          })
          .catch(err => alert("エラーが発生しました: " + err));
      }

    </script>
  </body>
</html>
