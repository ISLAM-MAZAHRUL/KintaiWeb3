package kintai;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import kintai.KinmuManageBean.WorkAlloc;

public class KinmuManageDao {
	private DBAccess db = new DBAccess();
	
	/**
     * すべてのプロジェクト状況を取得する
     * @return プロジェクト状況のリスト
     */
    public List<KinmuManageBean.WorkAlloc> findAll() {
        List<KinmuManageBean.WorkAlloc> kinmuManageList = new ArrayList<>();
        // SELECT文にREPEAT_RULE_IDとIS_SYSTEM_DEFINEDを追加
        String sql = "SELECT WORK_DATE, EMP_ID, PROJECT_ID, WORK_HOURS FROM work_alloc ORDER BY WORK_DATE DESC";

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
            	KinmuManageBean.WorkAlloc workAlloc = new KinmuManageBean.WorkAlloc();
            	workAlloc.setWorkDate(rs.getDate("WORK_DATE").toLocalDate());
                workAlloc.setEmpId(rs.getString("EMP_ID"));
                workAlloc.setProjectId(rs.getInt("PROJECT_ID"));
                workAlloc.setWorkHours(rs.getDouble("WORK_HOURS"));
                
                kinmuManageList.add(workAlloc);;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return kinmuManageList;
    }
    
    
	/**
     * 指定された従業員と日付のプロジェクト状況データベースから取得する
     * @param empId 従業員番号
     * @param date 検索する日付
     * @return プロジェクト状況のリスト
     */
	public List<KinmuManageBean.WorkAlloc> findKinmuManageByDate(String empId, LocalDate date) {
		List<KinmuManageBean.WorkAlloc> kinmuManageList = new ArrayList<>();
        String sql = "SELECT w.PROJECT_ID, w.WORK_HOURS, p.PROJECT_NAME FROM work_alloc w LEFT JOIN project p ON w.PROJECT_ID = p.PROJECT_ID WHERE w.EMP_ID = ? AND w.WORK_DATE = ?";

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, empId);
            ps.setDate(2, Date.valueOf(date));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    KinmuManageBean.WorkAlloc workAlloc = new KinmuManageBean.WorkAlloc();
                    workAlloc.setEmpId(empId);
                    workAlloc.setWorkDate(date);
                    workAlloc.setProjectId(rs.getInt("PROJECT_ID"));
                    workAlloc.setWorkHours(rs.getDouble("WORK_HOURS"));
                    workAlloc.setProjectName(rs.getString("PROJECT_NAME")); // 追加
                    
                    kinmuManageList.add(workAlloc);
                }
                   
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return kinmuManageList;
    }
	//「従業員ID + 期間」で WorkAlloc を取得するメソッドを確認/追加する
	public List<KinmuManageBean.WorkAlloc> findByEmpAndRange(String empId, LocalDate start, LocalDate end) {
	    List<KinmuManageBean.WorkAlloc> list = new ArrayList<>();
	    String sql = "SELECT EMP_ID, PROJECT_ID, WORK_DATE, WORK_HOURS " +
	                 "FROM work_alloc " +
	                 "WHERE EMP_ID = ? AND WORK_DATE BETWEEN ? AND ? AND IS_DELETED = FALSE";

	    try (Connection conn = db.getConnection();
	         PreparedStatement ps = conn.prepareStatement(sql)) {

	        ps.setString(1, empId);
	        ps.setDate(2, Date.valueOf(start));
	        ps.setDate(3, Date.valueOf(end));

	        try (ResultSet rs = ps.executeQuery()) {
	            while (rs.next()) {
	                KinmuManageBean.WorkAlloc wa = new KinmuManageBean.WorkAlloc();
	                wa.setEmpId(rs.getString("EMP_ID"));
	                wa.setProjectId(rs.getInt("PROJECT_ID"));   // ← ★ プロジェクトIDもセット
	                wa.setWorkDate(rs.getDate("WORK_DATE").toLocalDate());
	                wa.setWorkHours(rs.getDouble("WORK_HOURS"));
	                list.add(wa);
	            }
	        }
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	    return list;
	}
	
	
	public List<WorkAlloc> findMonthlyByAllEmp(String yearMonth) {
	    List<KinmuManageBean.WorkAlloc> list = new ArrayList<>();
	    String sql = "SELECT w.EMP_ID, e.EMP_NAME, w.PROJECT_ID, p.PROJECT_NAME, " +
	                 "DATE_FORMAT(w.WORK_DATE, '%Y%m') AS YEAR_MONTH, " +
	                 "SUM(w.WORK_HOURS) AS TOTAL_HOURS " +
	                 "FROM work_alloc w " +
	                 "LEFT JOIN emp e ON w.EMP_ID = e.EMP_ID " +
	                 "LEFT JOIN project p ON w.PROJECT_ID = p.PROJECT_ID " +
	                 "WHERE DATE_FORMAT(w.WORK_DATE, '%Y-%m') = ? " +
	                 "AND w.IS_DELETED = FALSE" +
	                 "GROUP BY w.EMP_ID, e.EMP_NAME, w.PROJECT_ID, p.PROJECT_NAME, YEAR_MONTH " +
	                 "ORDER BY w.EMP_ID, w.PROJECT_ID";

	    try (Connection conn = db.getConnection();
	         PreparedStatement ps = conn.prepareStatement(sql)) {

	        ps.setString(1, yearMonth);

	        try (ResultSet rs = ps.executeQuery()) {
	            while (rs.next()) {
	                KinmuManageBean.WorkAlloc wa = new KinmuManageBean.WorkAlloc();
	                wa.setEmpId(rs.getString("EMP_ID"));
	                wa.setEmpName(rs.getString("EMP_NAME"));
	                wa.setProjectId(rs.getInt("PROJECT_ID"));
	                wa.setProjectName(rs.getString("PROJECT_NAME"));
	                wa.setWorkHours(rs.getDouble("TOTAL_HOURS"));
	                list.add(wa);
	            }
	        }
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	    return list;
	}
	public List<KinmuManageBean.WorkAlloc> findMonthlyByAllEmpRange(String startMonth, String endMonth) {
	    List<KinmuManageBean.WorkAlloc> list = new ArrayList<>();
	    String sql = "SELECT w.EMP_ID, e.EMP_NAME, w.PROJECT_ID, p.PROJECT_NAME, p.PROJECT_CODE, " +
	            "DATE_FORMAT(w.WORK_DATE, '%Y%m') AS YM, " +
	            "SUM(w.WORK_HOURS) AS TOTAL_HOURS " +
	            "FROM work_alloc w " +
	            "LEFT JOIN emp e ON w.EMP_ID = e.EMP_ID " +
	            "LEFT JOIN project p ON w.PROJECT_ID = p.PROJECT_ID " +
	            "WHERE w.WORK_DATE BETWEEN ? AND ? " +
	            "AND w.IS_DELETED = 0 " +
	            "GROUP BY w.EMP_ID, e.EMP_NAME, w.PROJECT_ID, p.PROJECT_NAME, p.PROJECT_CODE, YM " +
	            "ORDER BY w.EMP_ID, YM, w.PROJECT_ID";

	    try (Connection conn = db.getConnection();
	         PreparedStatement ps = conn.prepareStatement(sql)) {

	        ps.setDate(1, java.sql.Date.valueOf(startMonth + "-01"));
	        // 終了月の末日を計算
	        java.time.LocalDate endDate = YearMonth.parse(endMonth).atEndOfMonth();
	        ps.setDate(2, java.sql.Date.valueOf(endDate));

	        try (ResultSet rs = ps.executeQuery()) {
	            while (rs.next()) {
	                KinmuManageBean.WorkAlloc wa = new KinmuManageBean.WorkAlloc();
	                wa.setEmpId(rs.getString("EMP_ID"));
	                wa.setEmpName(rs.getString("EMP_NAME"));
	                wa.setProjectId(rs.getInt("PROJECT_ID"));
	                wa.setProjectName(rs.getString("PROJECT_NAME"));
	                wa.setProjectCode(rs.getString("PROJECT_CODE"));
	                wa.setWorkHours(rs.getDouble("TOTAL_HOURS"));
	                wa.setYearMonth(rs.getString("YM"));
	                list.add(wa);
	            }
	        }
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	    return list;
	}
	public List<KinmuManageBean.WorkAlloc> findMonthlyByEmpRange(String empId, String startMonth, String endMonth) {
	    List<KinmuManageBean.WorkAlloc> list = new ArrayList<>();
	    String sql = "SELECT w.EMP_ID, e.EMP_NAME, w.PROJECT_ID, p.PROJECT_NAME, p.PROJECT_CODE, " +
	    		"DATE_FORMAT(w.WORK_DATE, '%Y%m') AS YM, " +
	                 "SUM(w.WORK_HOURS) AS TOTAL_HOURS " +
	                 "FROM work_alloc w " +
	                 "LEFT JOIN emp e ON w.EMP_ID = e.EMP_ID " +
	                 "LEFT JOIN project p ON w.PROJECT_ID = p.PROJECT_ID " +
	                 "WHERE w.EMP_ID = ? " +
	                 "AND w.WORK_DATE BETWEEN ? AND ? " +
	                 "AND w.IS_DELETED = 0 " +
	                 "GROUP BY w.EMP_ID, e.EMP_NAME, w.PROJECT_ID, p.PROJECT_NAME, p.PROJECT_CODE, YM " +
	                 "ORDER BY DATE_FORMAT(w.WORK_DATE, '%Y%m'), w.PROJECT_ID";

	    try (Connection conn = db.getConnection();
	         PreparedStatement ps = conn.prepareStatement(sql)) {

	        ps.setString(1, empId);
	        ps.setDate(2, java.sql.Date.valueOf(startMonth + "-01"));
	        java.time.LocalDate endDate = java.time.YearMonth.parse(endMonth).atEndOfMonth();
	        ps.setDate(3, java.sql.Date.valueOf(endDate));

	        try (ResultSet rs = ps.executeQuery()) {
	            while (rs.next()) {
	                KinmuManageBean.WorkAlloc wa = new KinmuManageBean.WorkAlloc();
	                wa.setEmpId(rs.getString("EMP_ID"));
	                wa.setEmpName(rs.getString("EMP_NAME"));
	                wa.setProjectId(rs.getInt("PROJECT_ID"));
	                wa.setProjectName(rs.getString("PROJECT_NAME"));
	                wa.setProjectCode(rs.getString("PROJECT_CODE"));
	                wa.setWorkHours(rs.getDouble("TOTAL_HOURS"));
	                wa.setYearMonth(rs.getString("YM"));
	                list.add(wa);
	            }
	        }
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	    return list;
	}

}
