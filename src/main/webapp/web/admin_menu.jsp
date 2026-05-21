<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="kintai.UserBean" %>
<%@ page import="kintai.UserDao" %>
<%@ page import="kintai.AnnouncementBean" %>
<%@ page import="kintai.AnnouncementDao" %>
<%@ page import="java.time.LocalDate" %>
<%@ page import="java.time.format.DateTimeFormatter" %>
<%@ page import="java.util.Locale" %>
<%@ page import="java.util.List, java.util.Map" %>
<%@ page import="kintai.KintaiRecDao" %>
<%@ page import="kintai.MonthlySummaryBean" %>
<%@ page import="kintai.KinmuManageBean" %>
<%@ page import="java.time.format.DateTimeFormatter" %>
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
    
    // 現在の日付と曜日を取得（サーバー側）
    LocalDate today = LocalDate.now();
    int month = today.getMonthValue();
    int day = today.getDayOfMonth();
    String[] weekdays = {"日", "月", "火", "水", "木", "金", "土"};
    String weekday = weekdays[today.getDayOfWeek().getValue() % 7];
    String dateString = "今日は" + month + "月" + day + "日です<br/>" + weekday + "曜日";
    
    // アナウンス情報を取得
    AnnouncementDao announcementDao = new AnnouncementDao();
    List<AnnouncementBean> announcements = announcementDao.findActiveAnnouncements();
    
    String todaystr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日"));
    Map<String, String> workTimeData = (Map<String, String>) session.getAttribute("workTimeData");
    
    boolean hasClockedIn = workTimeData.get("clockInTime") != null;
    boolean hasClockedOut = workTimeData.get("clockOutTime") != null;
%>
<html>
<head>
	<!-- レスポンシブ対応 -->
	<meta name="viewport" content="width=device-width, initial-scale=1">
	
    <title>勤怠管理システムメニュー（管理部）</title>
    
    <style>
    
        body {
            margin: 0;
            font-family: 'メイリオ', sans-serif;
            background: #f5f5f5;
            font-size: 14px;
            min-height: 100vh;
            overflow-y: auto; /* body自体はスクロールさせない */
        }
        /* 2025/8/6 米村 メニューを下まで見れないため変更 */
        .container {
            max-width: 1400px;
            margin: 0 auto;
            background-color: white;
            height: 100vh;
            /*max-height: none; */
            height: calc(100vh - 60px); /* ヘッダーが40pxなら、それを引く */
            overflow: visible;  /* container自体にスクロールはさせない */
            /*overflow-y: auto;*/		/* ← 2025/8/6 米村 hiddenからautoに変更 */
            /*overflow-x: hidden;*/		/* ← 2025/8/6 米村 overflowをxとyを指定 */
            box-shadow: 0 0 10px rgba(0,0,0,0.1);
            padding-bottom: 100px; /* ← 下に空間を作る */
        }
        .header {
        	height: 60px; /* 必ず固定の高さにしておく */
        	padding: 10px 20px;
            display: flex;
            justify-content: space-between;
            align-items: flex-start;
            /*padding: 15px 20px;*/
            background: #fff;
            border-bottom: 2px solid #dc3545; /* 管理者用は赤色 */
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
        
        /* ダッシュボードレイアウト */
        .dashboard {
            padding: 20px;
            display: grid;
            grid-template-columns: 1fr 1fr 1fr; /* 3列レイアウト */
            grid-template-rows: auto auto auto;
            gap: 20px;
        }
        .dashboard::after {
		    content: "";
		    display: block;
		    height: 100px;
		}
        
        .dashboard h1 {
            grid-column: 1 / -1;
            display: flex;
            justify-content: center;
            align-items: center;
            color: #333;
            margin: 0 0 20px 0;
            font-size: 1.8em;
            border-bottom: 2px solid #dc3545; /* 管理者用は赤色 */
            padding-bottom: 10px;
            position: relative;
        }
        
        .dashboard-title {
            text-align: center;
        }
        
        .system-trigger {
            position: absolute;
            right: 0;
        }
        
        /* ウィジェットの共通スタイル */
        .widget {
            background: white;
            border: 1px solid #ddd;
            border-radius: 8px;
           padding: 12px; /* 社員用パディングに統一 */
            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
            transition: box-shadow 0.3s;
            display: flex;
            flex-direction: column;
        }
        
        .widget:hover {
            box-shadow: 0 4px 8px rgba(0,0,0,0.15);
        }
        
        .widget h2 {
            margin: 0 0 12px 0; /* 社員用マージンに統一 */
            color: #dc3545; /* 管理者用は赤色 */
            font-size: 1.2em;
            border-bottom: 1px solid #eee;
           padding-bottom: 6px; /* 社員用パディングに統一 */
        }
        
        /* 基本機能ウィジェット */
        .basic-widget {
       		align-items: stretch;
        	text-align: center;
            border-left: 4px solid #007bff;
        }
        
        .basic-widget h2 {
            color: #007bff;
        }
        
        /* 管理機能ウィジェット */
        .admin-widget {
        	align-items: center;
            border-left: 4px solid #dc3545;
        }
        
        /* その他管理機能ウィジェット */
        .other-admin-widget {
        	align-items: center;
            border-left: 4px solid #fd7e14;
        }
        
        .other-admin-widget h2 {
            color: #fd7e14;
        }
        
        /* 機能ボタンの共通スタイル */
        .function-btn {
            width: 95%;
            padding: 10px;
            margin-bottom: 8px;
            background: #6c757d;
            color: white;
            border: none;
            border-radius: 5px;
            cursor: pointer;
            text-decoration: none;
            display: block;
            text-align: center;
            transition: background-color 0.3s;
            font-size: 0.9em;
        }
        
        .function-btn:hover {
            background: #545b62;
        }
        
        .function-btn.basic {
            background: #007bff;
        }
        
        .function-btn.pass {
            background-color: #f0f6fc;
		    color: #336699;
		    border: 1px solid #cce0f5;
		    padding: 8px 16px;
		    border-radius: 5px;
		    cursor: pointer;
        }
        
        .function-btn.pass:hover {
            background: #f0f6fc;
        }
        
        .function-btn.basic:hover {
            background: #0056b3;
        }
        
        .function-btn.admin {
            background: #dc3545;
        }
        
        .function-btn.admin:hover {
            background: #c82333;
        }
        
        .sliding-sidebar {
            position: fixed;
            top: 0;
            right: -320px;
            width: 320px;
            height: auto;
            max-height: 100vh;
            background: linear-gradient(135deg, #ffffff 0%, #f8f9fa 100%);
            color: #333;
            z-index: 1000;
            transition: right 0.4s ease-in-out;
            box-shadow: -2px 0 15px rgba(0,0,0,0.15);
            padding: 0;
            overflow-y: auto;
            border-radius: 0 0 0 15px;
        }
        
        .sliding-sidebar.open {
            right: 0;
        }
        
        .sidebar-header {
            padding: 20px;
            border-bottom: 1px solid #dee2e6;
            display: flex;
            justify-content: space-between;
            align-items: center;
            background: linear-gradient(135deg, #28a745 0%, #218838 100%);
            color: white;
        }
        
        .sidebar-title {
            margin: 0;
            font-size: 1.3em;
            color: white;
            font-weight: 600;
            text-shadow: 0 1px 2px rgba(0,0,0,0.1);
        }
        
        .sidebar-close {
            background: none;
            border: none;
            color: white;
            font-size: 1.5em;
            cursor: pointer;
            padding: 5px;
            transition: all 0.3s;
            opacity: 0.8;
        }
        
        .sidebar-close:hover {
            opacity: 1;
            transform: scale(1.1);
        }
        
        .sidebar-close:hover {
            transform: scale(1.1);
        }
        
        .sidebar-content {
            padding: 20px;
            padding-bottom: 30px;
        }
        
        .sidebar-greeting {
            padding: 20px;
            text-align: center;
            background: linear-gradient(135deg, #e8f5e8 0%, #d4f4d4 100%);
            border-bottom: 1px solid #dee2e6;
        }
        
        .greeting-text {
            font-size: 1.1em;
            color: #155724;
            font-weight: 600;
            margin-bottom: 5px;
        }
        
        .greeting-reminder {
            font-size: 0.8em;
            color: #6c757d;
            font-style: italic;
        }
        
        .sidebar-item {
            background: linear-gradient(135deg, #ffffff 0%, #f8f9fa 100%);
            border-radius: 8px;
            padding: 15px;
            margin: 15px;
            border: 1px solid #e9ecef;
            box-shadow: 0 2px 4px rgba(0,0,0,0.05);
        }
        
        .sidebar-item.date-item {
            background: linear-gradient(135deg, #fff9c4 0%, #fef08a 100%);
            border: 1px solid #fbbf24;
        }
        
        .summary-grid {
            display: grid;
            grid-template-columns: 1fr 1fr;
            gap: 15px;
        }
        
        .info-item {
            display: flex;
            justify-content: space-between;
            align-items: center;
            padding: 12px 0;
            border-bottom: 1px solid #fbbf24;
        }
        
        .info-item:last-child {
            border-bottom: none;
        }
        
        .info-item .label {
            font-size: 0.9em;
            color: #6c757d;
            font-weight: 500;
        }
        
        .info-item .value {
            font-size: 1.1em;
            font-weight: bold;
            color: #495057;
        }
        
        .date-display {
            text-align: center;
            font-size: 1.0em;
            color: #495057;
            font-weight: 500;
        }
        
        /* システム概要 */
        .system-trigger {
            display: flex;
            align-items: center;
            cursor: pointer;
            transition: all 0.3s;
            background: #28a745;
            color: white;
            padding: 6px 12px;
            border-radius: 20px;
            border: none;
            box-shadow: 0 2px 4px rgba(40, 167, 69, 0.3);
        }
        
        .system-trigger:hover {
            background: #218838;
            transform: translateY(-1px);
            box-shadow: 0 3px 6px rgba(40, 167, 69, 0.4);
        }
        
        .system-label {
            font-size: 0.55em !important;
            color: white;
            font-weight: 500;
            margin-right: 5px;
        }
        
        .system-arrow {
            color: white;
            font-size: 0.55em !important;
            transition: transform 0.3s;
        }
        
        .system-trigger:hover .system-arrow {
            transform: translateX(2px);
        }
        
        /* 遮罩层 */
        .sidebar-overlay {
            position: fixed;
            top: 0;
            left: 0;
            width: 100vw;
            height: 100vh;
            background: rgba(0, 0, 0, 0.5);
            z-index: 999;
            opacity: 0;
            visibility: hidden;
            transition: all 0.3s ease;
        }
        
        .sidebar-overlay.active {
            opacity: 1;
            visibility: visible;
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
                padding: 15px;
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
        }
        
        /* 極小画面対応 */
        @media (max-width: 480px) {
            .dashboard {
                height: calc(100vh - 160px);
                padding: 10px;
                gap: 15px;
            }
            
            .widget {
                padding: 15px;
            }
            
            .function-btn {
                padding: 8px;
                font-size: 0.85em;
                margin-bottom: 6px;
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
        
        .announcement-form {
            background: rgba(255, 255, 255, 0.95);
            border: 2px solid rgba(255, 255, 255, 0.3);
            border-radius: 8px;
            padding: 20px;
            margin-bottom: 15px;
            box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
        }
        
        .announcement-form input, .announcement-form textarea {
            width: 100%;
            padding: 8px 60px 8px 8px;
            border: 1px solid #ddd;
            border-radius: 4px;
            margin-bottom: 10px;
            font-family: inherit;
            box-sizing: border-box;
        }
        
        .announcement-form textarea {
            height: 80px;
            resize: vertical;
        }
        
        .form-buttons {
            display: flex;
            gap: 10px;
        }
        
        .form-btn {
            padding: 8px 16px;
            border: none;
            border-radius: 4px;
            cursor: pointer;
            font-size: 0.9em;
            transition: background-color 0.3s;
        }
        
        .form-btn.submit {
            background: #007bff;
            color: white;
        }
        
        .form-btn.submit:hover {
            background: #0056b3;
        }
        
        .form-btn.cancel {
            background: #6c757d;
            color: white;
        }
        
        .form-btn.cancel:hover {
            background: #545b62;
        }
        
        .char-count {
            position: absolute;
            bottom: 18px;
            right: 8px;
            font-size: 0.7em;
            color: #6c757d;
            background: rgba(248, 249, 250, 0.95);
            padding: 1px 4px;
            border-radius: 2px;
            pointer-events: none;
            z-index: 10;
            border: 1px solid rgba(0, 0, 0, 0.1);
            line-height: 1.2;
        }
        
        .char-count.warning {
            color: #fd7e14;
        }
        
        .char-count.danger {
            color: #dc3545;
        }
        
        .no-announcement {
            color: #6c757d;
            font-style: italic;
            text-align: center;
            padding: 20px;
        }
        
        .add-announcement-btn {
            background: #28a745;
            color: white;
            border: none;
            border-radius: 5px;
            padding: 6px 10px;
            cursor: pointer;
            font-size: 0.85em;
            transition: background-color 0.3s;
            width: auto;
            display: inline-block;
        }
        
        .add-announcement-btn:hover {
            background: #218838;
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
      /* --- 「外部連携・出力」  */
.purple-widget {
    align-items: center;
    border-left: 4px solid #6f42c1 !important;
}

.purple-widget h2 {
    color: #6f42c1 !important;
}

.purple-widget .function-btn {
    background: #6f42c1 !important;
}

.purple-widget .function-btn:hover {
    background: #5a32a3 !important;
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
                
                <button class="add-announcement-btn" onclick="showAddForm()">新しいアナウンスを追加</button>
            </div>
            
            <div class="banner-content collapsed" id="bannerContent">
                <!-- 編集フォーム（初期状態では非表示） -->
                <form id="announcementForm" class="announcement-form" style="display: none;" method="post" action="<%= request.getContextPath() %>/announcement">
                    <input type="hidden" name="action" value="add" id="formAction">
                    <input type="hidden" name="announcementId" value="" id="editAnnouncementId">
                    <div style="position: relative;">
                        <input type="text" name="title" placeholder="タイトル" id="announcementTitle" required maxlength="30" oninput="updateCharCount('announcementTitle', 'titleCharCount', 30)">
                        <div class="char-count" id="titleCharCount">0/30文字</div>
                    </div>
                    <div style="position: relative;">
                        <textarea name="content" placeholder="内容" id="announcementContent" required maxlength="300" oninput="updateCharCount('announcementContent', 'contentCharCount', 300)"></textarea>
                        <div class="char-count" id="contentCharCount">0/300文字</div>
                    </div>
                    <input type="hidden" name="isActive" value="true">
                    <div class="form-buttons" style="justify-content: center;">
                        <button type="submit" class="form-btn submit">送信</button>
                        <button type="button" class="form-btn cancel" onclick="cancelEdit()">キャンセル</button>
                    </div>
                </form>
                
                <!-- 表示部分 -->
                <div id="announcementDisplay">
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
                                        <div>
                                            <button class="announcement-edit-btn" onclick="event.stopPropagation(); editAnnouncement(<%= announcement.getAnnouncementId() %>, '<%= announcement.getTitle().replace("'", "\\'") %>', '<%= announcement.getContent().replace("'", "\\'").replace("\n", "\\n") %>')">編集</button>
                                            <button class="announcement-delete-btn" onclick="event.stopPropagation(); deleteAnnouncement(<%= announcement.getAnnouncementId() %>, '<%= announcement.getTitle().replace("'", "\\'") %>')">削除</button>
                                        </div>
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
						    <div class="announcement-grid">
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
                                            <div>
                                                <button class="announcement-edit-btn" onclick="event.stopPropagation(); editAnnouncement(<%= announcement.getAnnouncementId() %>, '<%= announcement.getTitle().replace("'", "\\'") %>', '<%= announcement.getContent().replace("'", "\\'").replace("\n", "\\n") %>')">編集</button>
                                                <button class="announcement-delete-btn" onclick="event.stopPropagation(); deleteAnnouncement(<%= announcement.getAnnouncementId() %>, '<%= announcement.getTitle().replace("'", "\\'") %>')">削除</button>
                                            </div>
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
        </div>
        
        <div class="header">
            <div class="user-info">
                <%-- 部署名と氏名を表示 --%>
                <p>部署：管理部<%-- <%= deptname %> --%></p>
                <p>氏名：<%= user.getName() %> <span style="color: #dc3545; font-weight: bold;">[管理者]</span></p>
            </div>
        <div style="display: flex; gap: 10px;">
            <!-- パスワード変更ボタン -->
		    <form action="<%= request.getContextPath() %>/PasswordChangeServlet" method="get">
		        <input type="submit" value="パスワード変更" class="function-btn pass" />
		    </form>
            <%-- ログアウトボタン --%>
            <form method="post" action="<%= request.getContextPath() %>/logout" style="margin: 0;">
                <input type="submit" value="ログアウト" class="logout-button">
            </form>
        </div>
        </div>
        
        <div class="dashboard">
            <h1>
                <span class="dashboard-title">管理者メニュー</span>
                <span class="system-trigger" onclick="toggleSlidingSidebar()">
                    <span class="system-label">システム概要</span>
                    <span class="system-arrow">▶</span>
                </span>
            </h1>
            
            
            <!-- 基本機能ウィジェット -->
            <div class="widget basic-widget">
                <h2>基本機能</h2>
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
<!--                <a href="<%= request.getContextPath() %>/showWorkPunchForm" class="function-btn basic">本日分の打刻</a>-->
				<a href="<%= request.getContextPath() %>/KinmuManageServlet" class="function-btn basic">勤務時間管理</a>
                <a href="<%= request.getContextPath() %>/KintaiRecServlet" class="function-btn basic">従業員別勤怠記録表示</a>
            </div>
            
            <!-- 業務管理ウィジェット -->
            <div class="widget admin-widget">
                <h2>業務管理</h2>
                <a href="<%= request.getContextPath() %>/projectManage" class="function-btn admin">プロジェクト管理</a>
                <a href="<%= request.getContextPath() %>/leaveRec" class="function-btn admin">休暇申請管理</a>
                <a href="<%= request.getContextPath() %>/leaveGrantManage" class="function-btn admin"">休暇付与管理</a>
                <a href="<%= request.getContextPath() %>/kintaiLog" class="function-btn admin"">勤務時間更新ログ</a>
            </div>
            
            <!-- マスタ管理ウィジェット -->
            <div class="widget other-admin-widget">
                <h2>マスタ管理</h2>
                <a href="<%= request.getContextPath() %>/empManage" class="function-btn" style="background: #fd7e14;">従業員管理</a>
                <a href="<%= request.getContextPath() %>/deptManage" class="function-btn" style="background: #fd7e14;">部署管理</a>
                <a href="<%= request.getContextPath() %>/postManage" class="function-btn" style="background: #fd7e14;">役職管理</a>
                <a href="<%= request.getContextPath() %>/CalendarManageServlet" class="function-btn" style="background: #fd7e14;">カレンダー・イベント管理</a>
                <a href="<%= request.getContextPath() %>/leaveTypeManage" class="function-btn" style="background: #fd7e14;">休日種別管理</a>
            </div>
            <!-- new ADD -->
             <div class="widget purple-widget">
                <h2>外部連携・出力</h2>
                <a href="<%= request.getContextPath() %>/web/kinmu_yayoi_export.jsp" class="function-btn" style="background: #6f42c1;">弥生給与データ出力</a>
                <a href="<%= request.getContextPath() %>/web/kinmu_acc_export.jsp" class="function-btn" style="background:#6f42c1;">会計データ出力</a>
                <a href="<%= request.getContextPath() %>/web/kinmu_hyo_export.jsp" class="function-btn" style="background:#6f42c1;">勤務表データ出力</a>
                <a href="<%= request.getContextPath() %>/YukyuKanriServlet" class="function-btn" style="background:#6f42c1;">有休管理</a>
                
          		</div>
    </div>

    <!-- 滑动侧边栏 -->
    <div class="sidebar-overlay" id="sidebarOverlay" onclick="closeSlidingSidebar()"></div>
    <div class="sliding-sidebar" id="slidingSidebar">
        <div class="sidebar-header">
            <h2 class="sidebar-title">システム概要</h2>
            <button class="sidebar-close" onclick="closeSlidingSidebar()">&times;</button>
        </div>
        
        <div class="sidebar-greeting" id="sidebarGreeting">
            <div class="greeting-text" id="greetingText">おはようございます</div>
            <div class="greeting-reminder" id="greetingReminder"></div>
        </div>
        
        <div class="sidebar-content">
            <div class="sidebar-item date-item">
                <div class="date-display" id="slideDateDisplay"><%= dateString %></div>
            </div>
            
            <div class="sidebar-item">
                <div class="info-item">
                    <span class="label">今日の出勤者</span>
                    <span class="value" id="slideTodayAttendance">-</span>
                </div>
                <div class="info-item">
                    <span class="label">総従業員数</span>
                    <span class="value" id="slideTotalEmployees">-</span>
                </div>
                <div class="info-item">
                    <span class="label">部署数</span>
                    <span class="value" id="slideTotalDepts">-</span>
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
        
        // サーバーから取得したリアルデータを表示（隠しdivに設定）
        var todayAttendanceValue = '-';
        var totalEmployeesValue = '-';
        var totalDeptsValue = '-';
        
        <% if (request.getAttribute("totalEmployees") != null) { %>
            totalEmployeesValue = '<%= request.getAttribute("totalEmployees") %>名';
        <% } %>
        <% if (request.getAttribute("todayAttendance") != null) { %>
            todayAttendanceValue = '<%= request.getAttribute("todayAttendance") %>名';
        <% } %>
        <% if (request.getAttribute("totalDepts") != null) { %>
            totalDeptsValue = '<%= request.getAttribute("totalDepts") %>部署';
        <% } %>
        
        // 隠しdivに値を設定（データ同期のため）
        var hiddenDiv = document.createElement('div');
        hiddenDiv.style.display = 'none';
        hiddenDiv.innerHTML = '<span id="todayAttendance">' + todayAttendanceValue + '</span>' +
                              '<span id="totalEmployees">' + totalEmployeesValue + '</span>' +
                              '<span id="totalDepts">' + totalDeptsValue + '</span>';
        document.body.appendChild(hiddenDiv);
        
        // 文字数カウント機能
        function updateCharCount(inputId, countId, maxLength) {
            var input = document.getElementById(inputId);
            var counter = document.getElementById(countId);
            var currentLength = input.value.length;
            
            counter.textContent = currentLength + '/' + maxLength + '文字';
            
            // 文字数に応じて色を変更
            counter.className = 'char-count';
            if (currentLength > maxLength * 0.9) {
                counter.className += ' danger';
            } else if (currentLength > maxLength * 0.7) {
                counter.className += ' warning';
            }
        }
        
        // アナウンス管理JavaScript
        function showAddForm() {
            // アナウンスバナーを展開
            var content = document.getElementById('bannerContent');
            var toggle = document.getElementById('bannerToggle');
            if (content.classList.contains('collapsed')) {
                content.classList.remove('collapsed');
                toggle.innerHTML = '▼ <span id="bannerToggleText">折りたたむ</span>';
            }
            
            document.getElementById('formAction').value = 'add';
            document.getElementById('editAnnouncementId').value = '';
            document.getElementById('announcementTitle').value = '';
            document.getElementById('announcementContent').value = '';
            // 文字数をリセット
            updateCharCount('announcementTitle', 'titleCharCount', 30);
            updateCharCount('announcementContent', 'contentCharCount', 300);
            document.getElementById('announcementForm').style.display = 'block';
            document.getElementById('announcementDisplay').style.display = 'none';
        }
        
        function editAnnouncement(id, title, content) {
            document.getElementById('formAction').value = 'update';
            document.getElementById('editAnnouncementId').value = id;
            document.getElementById('announcementTitle').value = title;
            document.getElementById('announcementContent').value = content;
            // 文字数を更新
            updateCharCount('announcementTitle', 'titleCharCount', 30);
            updateCharCount('announcementContent', 'contentCharCount', 300);
            document.getElementById('announcementForm').style.display = 'block';
            document.getElementById('announcementDisplay').style.display = 'none';
        }
        
        function cancelEdit() {
            document.getElementById('announcementForm').style.display = 'none';
            document.getElementById('announcementDisplay').style.display = 'block';
        }
        
        function deleteAnnouncement(id, title) {
            if (confirm('「' + title + '」を削除してもよろしいですか？')) {
                // 削除用の隠しフォームを作成して送信
                var form = document.createElement('form');
                form.method = 'post';
                form.action = '<%= request.getContextPath() %>/announcement';
                
                var actionInput = document.createElement('input');
                actionInput.type = 'hidden';
                actionInput.name = 'action';
                actionInput.value = 'delete';
                form.appendChild(actionInput);
                
                var idInput = document.createElement('input');
                idInput.type = 'hidden';
                idInput.name = 'announcementId';
                idInput.value = id;
                form.appendChild(idInput);
                
                document.body.appendChild(form);
                form.submit();
            }
        }
        
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
        
        // アナウンス詳細を表示するモーダル
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
        
        // サイドバー機能
        function toggleSlidingSidebar() {
            var sidebar = document.getElementById('slidingSidebar');
            var overlay = document.getElementById('sidebarOverlay');
            
            if (sidebar.classList.contains('open')) {
                closeSlidingSidebar();
            } else {
                sidebar.classList.add('open');
                overlay.classList.add('active');
                
                syncSidebarData();
            }
        }
        
        function closeSlidingSidebar() {
            var sidebar = document.getElementById('slidingSidebar');
            var overlay = document.getElementById('sidebarOverlay');
            
            sidebar.classList.remove('open');
            overlay.classList.remove('active');
        }
        
        function syncSidebarData() {
            var todayAttendance = document.getElementById('todayAttendance');
            var totalEmployees = document.getElementById('totalEmployees');
            var totalDepts = document.getElementById('totalDepts');
            
            if (todayAttendance) {
                document.getElementById('slideTodayAttendance').textContent = todayAttendance.textContent;
            }
            if (totalEmployees) {
                document.getElementById('slideTotalEmployees').textContent = totalEmployees.textContent;
            }
            if (totalDepts) {
                document.getElementById('slideTotalDepts').textContent = totalDepts.textContent;
            }
            
            setGreeting();
        }
        
        function setGreeting() {
            // Set time-based greeting
            var now = new Date();
            var hour = now.getHours();
            var greetingText = document.getElementById('greetingText');
            var greetingReminder = document.getElementById('greetingReminder');
            
            if (hour >= 5 && hour < 12) {
                greetingText.textContent = 'おはようございます';
                greetingReminder.textContent = '';
            } else if (hour >= 12 && hour < 17) {
                greetingText.textContent = 'こんにちは';
                greetingReminder.textContent = '';
            } else if (hour >= 17 && hour < 22) {
                greetingText.textContent = 'こんばんは';
                greetingReminder.textContent = '';
            } else {
                greetingText.textContent = 'こんばんは';
                greetingReminder.textContent = '一日お疲れ様でした、ごゆっくり休んでください';
            }
        }
        
        // Display message on page load if exists
        <% 
        String message = (String) session.getAttribute("message");
        if (message != null) {
            session.removeAttribute("message"); // Remove message after display
        %>
            alert('<%= message %>');
        <% } %>

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