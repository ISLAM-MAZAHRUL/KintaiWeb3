package kintai;

import java.sql.Connection;
import java.sql.Date; // java.sql.Date をインポート
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * empテーブルへのデータアクセスを担当するクラス (DAO)。
 * 社員情報の検索、追加、更新、削除を行う。
 * 新しいER図のempテーブルの構造に合わせて修正。
 */
public class EmpDao {
    
    private DBAccess db = new DBAccess();
    
    /**
     * すべての社員情報を取得する（部署名、役職名、ロール名、等級名も含む）
     * @return 社員情報のリスト
     */
    public List<EmpBean> findAll() {
        List<EmpBean> empList = new ArrayList<>();
        // 新しいER図のempテーブルの列と結合するテーブルに合わせてSQLを修正
        String sql = "SELECT e.EMP_ID, e.EMP_NAME, e.DEPT_ID, e.POST_ID, e.ROLE_ID, e.EMP_TYPE, " +
                     "e.PASS, e.MAIL, e.EMP_DATE, e.IS_ACTIVE, e.LEAVE_DATE, " +
                     "d.DEPT_NAME, p.POST_NAME, r.ROLE_NAME " +
                     "FROM emp e " +
                     "LEFT JOIN dept d ON e.DEPT_ID = d.DEPT_ID " +
                     "LEFT JOIN post p ON e.POST_ID = p.POST_ID " +
                     "LEFT JOIN role r ON e.ROLE_ID = r.ROLE_ID " +
                     "WHERE e.IS_ACTIVE = true " +
                     "ORDER BY e.EMP_ID";
        
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            
            while (rs.next()) {
                EmpBean emp = new EmpBean();
                emp.setEmpId(rs.getString("EMP_ID"));
                emp.setEmpName(rs.getString("EMP_NAME"));
                emp.setDeptId(rs.getString("DEPT_ID"));
                emp.setPostId(rs.getString("POST_ID"));
                emp.setRoleId(rs.getInt("ROLE_ID"));
                emp.setEmpType(rs.getString("EMP_TYPE"));
                emp.setPass(rs.getString("PASS"));
                emp.setMail(rs.getString("MAIL"));
                
                // EMP_DATEはNULLの場合もあるので、nullチェック
                Date empDateSql = rs.getDate("EMP_DATE");
                if (empDateSql != null) {
                    emp.setEmpDate(empDateSql.toLocalDate());
                } else {
                    emp.setEmpDate(null);
                }
                
                emp.setActive(rs.getBoolean("IS_ACTIVE"));
                Date leaveDateSql = rs.getDate("LEAVE_DATE");
                if (leaveDateSql != null) {
                    emp.setLeaveDate(leaveDateSql.toLocalDate());
                }
                
                emp.setDeptName(rs.getString("DEPT_NAME"));
                emp.setPostName(rs.getString("POST_NAME"));
                emp.setRoleName(rs.getString("ROLE_NAME"));
                empList.add(emp);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        return empList;
    }
    
    /**
     * 社員番号で社員情報を検索する
     * 新しいER図のempテーブルの構造に合わせて修正。
     * @param empId 社員番号
     * @return 社員情報。見つからない場合はnull
     */
    public EmpBean findByEmpId(String empId) {
        EmpBean emp = null;
        String sql = "SELECT e.EMP_ID, e.EMP_NAME, e.DEPT_ID, e.POST_ID, e.ROLE_ID, e.EMP_TYPE, " +
                     "e.PASS, e.MAIL, e.EMP_DATE, e.IS_ACTIVE, e.LEAVE_DATE, " +
                     "d.DEPT_NAME, p.POST_NAME, r.ROLE_NAME " +
                     "FROM emp e " +
                     "LEFT JOIN dept d ON e.DEPT_ID = d.DEPT_ID " +
                     "LEFT JOIN post p ON e.POST_ID = p.POST_ID " +
                     "LEFT JOIN role r ON e.ROLE_ID = r.ROLE_ID " +
                     "WHERE e.EMP_ID = ?";
        
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, empId);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    emp = new EmpBean();
                    emp.setEmpId(rs.getString("EMP_ID"));
                    emp.setEmpName(rs.getString("EMP_NAME"));
                    emp.setDeptId(rs.getString("DEPT_ID"));
                    emp.setPostId(rs.getString("POST_ID"));
                    emp.setRoleId(rs.getInt("ROLE_ID"));
                    emp.setEmpType(rs.getString("EMP_TYPE"));
                    emp.setPass(rs.getString("PASS"));
                    emp.setMail(rs.getString("MAIL"));
                    
                    // EMP_DATEはNULLの場合もあるので、nullチェック
                    Date empDateSql = rs.getDate("EMP_DATE");
                    if (empDateSql != null) {
                        emp.setEmpDate(empDateSql.toLocalDate());
                    } else {
                        emp.setEmpDate(null);
                    }
                    
                    emp.setActive(rs.getBoolean("IS_ACTIVE"));
                    Date leaveDateSql = rs.getDate("LEAVE_DATE");
                    if (leaveDateSql != null) {
                        emp.setLeaveDate(leaveDateSql.toLocalDate());
                    }

                    emp.setDeptName(rs.getString("DEPT_NAME"));
                    emp.setPostName(rs.getString("POST_NAME"));
                    emp.setRoleName(rs.getString("ROLE_NAME"));
                }
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        return emp;
    }
    
    /**
     * 新しい社員を追加する
     * 新しいER図のempテーブルの構造に合わせて修正。
     * @param emp 追加する社員情報
     * @return 追加に成功した場合true、失敗した場合false
     */
    public boolean insert(EmpBean emp) {
        // 新しいER図のempテーブルの列に合わせてSQLを修正
        String sql = "INSERT INTO emp (EMP_ID, EMP_NAME, DEPT_ID, POST_ID, ROLE_ID, EMP_TYPE, PASS, MAIL, EMP_DATE, IS_ACTIVE, CREATED_AT, CREATED_BY, UPDATED_AT, UPDATED_BY) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), ?, NOW(), ?)";
        
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, emp.getEmpId());
            ps.setString(2, emp.getEmpName());
            ps.setString(3, emp.getDeptId());
            ps.setString(4, emp.getPostId());
            ps.setInt(5, emp.getRoleId());
            ps.setString(6, emp.getEmpType());
            ps.setString(7, emp.getPass());
            ps.setString(8, emp.getMail());
            
            // EMP_DATEはNULL許容
            if (emp.getEmpDate() != null) {
                ps.setDate(9, Date.valueOf(emp.getEmpDate()));
            } else {
                ps.setNull(9, java.sql.Types.DATE);
            }
            
            ps.setBoolean(10, emp.isActive());
            ps.setString(11, "admin"); // CREATED_BY
            ps.setString(12, "admin"); // UPDATED_BY
            
            int count = ps.executeUpdate();
            return count > 0;
            
        } catch (SQLException e) {
            // 主キー重複エラーの場合
            if (e.getSQLState().equals("23000")) {
                System.err.println("社員番号が既に存在します: " + emp.getEmpId());
            } else {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return false;
    }
    
    /**
     * 社員情報を更新する
     * 新しいER図のempテーブルの構造に合わせて修正。
     * @param emp 更新する社員情報
     * @return 更新に成功した場合true、失敗した場合false
     */
    public boolean update(EmpBean emp) {
        String sql = "UPDATE emp SET EMP_NAME = ?, DEPT_ID = ?, POST_ID = ?, ROLE_ID = ?, EMP_TYPE = ?, PASS = ?, MAIL = ?, EMP_DATE = ?, UPDATED_AT = NOW(), UPDATED_BY = 'system' " +
                     "WHERE EMP_ID = ? AND IS_ACTIVE = true";
        
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, emp.getEmpName());
            ps.setString(2, emp.getDeptId());
            ps.setString(3, emp.getPostId());
            ps.setInt(4, emp.getRoleId());
            ps.setString(5, emp.getEmpType());
            ps.setString(6, emp.getPass());
            ps.setString(7, emp.getMail());
            
            // EMP_DATEはNULL許容
            if (emp.getEmpDate() != null) {
                ps.setDate(8, Date.valueOf(emp.getEmpDate()));
            } else {
                ps.setNull(8, java.sql.Types.DATE); // nullの場合はSQLのDATE型でnullをセット
            }
            
            ps.setString(9, emp.getEmpId());
            
            int count = ps.executeUpdate();
            return count > 0;
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return false;
    }
    
    /**
     * 社員を削除する
     * @param empId 削除する社員番号
     * @return 削除に成功した場合true、失敗した場合false
     */
    public boolean delete(String empId) {
        String sql = "UPDATE emp SET IS_ACTIVE = false, LEAVE_DATE = NOW(), UPDATED_AT = NOW(), UPDATED_BY = 'system' WHERE EMP_ID = ?";
        
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, empId);
            
            int count = ps.executeUpdate();
            return count > 0;
            
        } catch (SQLException e) {
            // 外部キー制約エラーの場合（この社員に関連する勤怠データなどがある場合）
            if (e.getSQLState().startsWith("23")) {
                System.err.println("この社員に関連するデータが存在するため削除できません: " + empId);
            } else {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return false;
    }
    
    /**
     * 削除された社員を復旧する
     * @param empId 復旧する社員番号
     * @return 復旧に成功した場合true、失敗した場合false
     */
    public boolean restore(String empId) {
        String sql = "UPDATE emp SET IS_ACTIVE = true, LEAVE_DATE = NULL, UPDATED_AT = NOW(), UPDATED_BY = 'admin' WHERE EMP_ID = ? AND IS_ACTIVE = false";
        
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, empId);
            
            int count = ps.executeUpdate();
            return count > 0;
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return false;
    }
    
    /**
     * 社員番号の重複をチェックする
     * @param empId チェックする社員番号
     * @return 既に存在する場合true、存在しない場合false
     */
    public boolean exists(String empId) {
        return findByEmpId(empId) != null;
    }
    
    /**
     * 個人レポート用に社員の詳細情報を取得する
     * @param empId 社員番号
     * @return PersonalReportBean用の社員情報、見つからない場合はnull
     */
    public PersonalReportBean getEmployeeForReport(String empId) {
        String sql = "SELECT e.EMP_ID, e.EMP_NAME, e.DEPT_ID, e.POST_ID, " +
                     "d.DEPT_NAME, p.POST_NAME " +
                     "FROM emp e " +
                     "LEFT JOIN dept d ON e.DEPT_ID = d.DEPT_ID " +
                     "LEFT JOIN post p ON e.POST_ID = p.POST_ID " +
                     "WHERE e.EMP_ID = ?";
        
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, empId);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    PersonalReportBean report = new PersonalReportBean();
                    report.setEmpno(rs.getString("EMP_ID"));
                    report.setEmpName(rs.getString("EMP_NAME"));
                    report.setDeptName(rs.getString("DEPT_NAME") != null ? rs.getString("DEPT_NAME") : "未設定");
                    report.setPostName(rs.getString("POST_NAME") != null ? rs.getString("POST_NAME") : "未設定");
                    return report;
                }
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return null;
    }
    
    /**
     * 部署・役職フィルターに基づいて従業員一覧を取得する
     * @param deptId 部署番号（nullまたは空文字の場合は全部署）
     * @param postId 役職番号（nullまたは空文字の場合は全役職）
     * @return フィルター条件に一致する従業員のリスト
     */
    public List<EmpBean> findByFilters(String deptId, String postId) {
        List<EmpBean> empList = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
            "SELECT e.EMP_ID, e.EMP_NAME, e.DEPT_ID, e.POST_ID, e.ROLE_ID, e.EMP_TYPE, " +
            "e.PASS, e.MAIL, e.EMP_DATE, e.IS_ACTIVE, e.LEAVE_DATE, " +
            "d.DEPT_NAME, p.POST_NAME, r.ROLE_NAME " +
            "FROM emp e " +
            "LEFT JOIN dept d ON e.DEPT_ID = d.DEPT_ID " +
            "LEFT JOIN post p ON e.POST_ID = p.POST_ID " +
            "LEFT JOIN role r ON e.ROLE_ID = r.ROLE_ID " +
            "WHERE e.IS_ACTIVE = true "
        );
        
        // フィルター条件を動的に追加
        List<String> params = new ArrayList<>();
        if (deptId != null && !deptId.trim().isEmpty()) {
            sql.append("AND e.DEPT_ID = ? ");
            params.add(deptId);
        }
        if (postId != null && !postId.trim().isEmpty()) {
            sql.append("AND e.POST_ID = ? ");
            params.add(postId);
        }
        
        sql.append("ORDER BY e.EMP_ID");
        
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            
            // パラメータを設定
            for (int i = 0; i < params.size(); i++) {
                ps.setString(i + 1, params.get(i));
            }
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    EmpBean emp = new EmpBean();
                    emp.setEmpId(rs.getString("EMP_ID"));
                    emp.setEmpName(rs.getString("EMP_NAME"));
                    emp.setDeptId(rs.getString("DEPT_ID"));
                    emp.setPostId(rs.getString("POST_ID"));
                    emp.setRoleId(rs.getInt("ROLE_ID"));
                    emp.setEmpType(rs.getString("EMP_TYPE"));
                    emp.setPass(rs.getString("PASS"));
                    emp.setMail(rs.getString("MAIL"));
                    
                    // 日付のnullチェック
                    Date empDate = rs.getDate("EMP_DATE");
                    if (empDate != null) {
                        emp.setEmpDate(empDate.toLocalDate());
                    }
                    
                    emp.setActive(rs.getBoolean("IS_ACTIVE"));
                    Date leaveDateSql = rs.getDate("LEAVE_DATE");
                    if (leaveDateSql != null) {
                        emp.setLeaveDate(leaveDateSql.toLocalDate());
                    }
                    
                    emp.setDeptName(rs.getString("DEPT_NAME"));
                    emp.setPostName(rs.getString("POST_NAME"));
                    emp.setRoleName(rs.getString("ROLE_NAME"));
                    
                    empList.add(emp);
                }
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return empList;
    }
    
    
    /**
     * 削除された社員情報を取得する（部署名、役職名、ロール名も含む）
     * @return 削除された社員情報のリスト
     */
    public List<EmpBean> findDeleted() {
        List<EmpBean> empList = new ArrayList<>();
        String sql = "SELECT e.EMP_ID, e.EMP_NAME, e.DEPT_ID, e.POST_ID, e.ROLE_ID, e.EMP_TYPE, " +
                     "e.PASS, e.MAIL, e.EMP_DATE, e.IS_ACTIVE, e.LEAVE_DATE, " +
                     "d.DEPT_NAME, p.POST_NAME, r.ROLE_NAME " +
                     "FROM emp e " +
                     "LEFT JOIN dept d ON e.DEPT_ID = d.DEPT_ID " +
                     "LEFT JOIN post p ON e.POST_ID = p.POST_ID " +
                     "LEFT JOIN role r ON e.ROLE_ID = r.ROLE_ID " +
                     "WHERE e.IS_ACTIVE = false " +
                     "ORDER BY e.LEAVE_DATE DESC, e.EMP_ID";
        
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            
            while (rs.next()) {
                EmpBean emp = new EmpBean();
                emp.setEmpId(rs.getString("EMP_ID"));
                emp.setEmpName(rs.getString("EMP_NAME"));
                emp.setDeptId(rs.getString("DEPT_ID"));
                emp.setPostId(rs.getString("POST_ID"));
                emp.setRoleId(rs.getInt("ROLE_ID"));
                emp.setEmpType(rs.getString("EMP_TYPE"));
                emp.setPass(rs.getString("PASS"));
                emp.setMail(rs.getString("MAIL"));
                
                // EMP_DATEはNULLの場合もあるので、nullチェック
                Date empDateSql = rs.getDate("EMP_DATE");
                if (empDateSql != null) {
                    emp.setEmpDate(empDateSql.toLocalDate());
                } else {
                    emp.setEmpDate(null);
                }
                
                emp.setActive(rs.getBoolean("IS_ACTIVE"));
                Date leaveDateSql = rs.getDate("LEAVE_DATE");
                if (leaveDateSql != null) {
                    emp.setLeaveDate(leaveDateSql.toLocalDate());
                }
                
                emp.setDeptName(rs.getString("DEPT_NAME"));
                emp.setPostName(rs.getString("POST_NAME"));
                emp.setRoleName(rs.getString("ROLE_NAME"));
                empList.add(emp);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        return empList;
    }
    
    /**
     * 契約形態で社員情報を検索する
     * @param 
     * @return 正社員の社員情報。見つからない場合はnull
     */
    public List<EmpBean> findAllFullTimeEmployees() {
        List<EmpBean> list = new ArrayList<>();

        String sql = "SELECT * FROM emp WHERE emp_type = ? AND IS_ACTIVE = TRUE"; // emp_type=正社員
        try ( Connection conn = db.getConnection();
              PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "正社員");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    EmpBean emp = new EmpBean();
                    emp.setEmpId(rs.getString("emp_id"));
                    emp.setEmpName(rs.getString("emp_name"));
                    emp.setDeptNo(rs.getString("dept_id"));
                    emp.setPostNo(rs.getString("post_id"));
                    emp.setRoleId(rs.getInt("role_id"));
                    emp.setPass(rs.getString("pass"));
                    emp.setMail(rs.getString("mail"));
                    emp.setEmpDate(rs.getDate("emp_date").toLocalDate());
                    list.add(emp);
                }
            }
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }

        return list;
    }
}
