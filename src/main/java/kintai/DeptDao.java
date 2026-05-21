package kintai;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * deptテーブルへのデータアクセスを担当するクラス (DAO)。
 * 部署情報の検索、追加、更新、削除を行う。
 */
public class DeptDao {
    
    private DBAccess db = new DBAccess();
    
    /**
     * すべての部署情報を取得する
     * @return 部署情報のリスト
     */
    public List<DeptBean> findAll() {
        List<DeptBean> deptList = new ArrayList<>();
        String sql = "SELECT DEPT_ID, DEPT_NAME FROM dept WHERE (IS_DELETED IS NULL OR IS_DELETED = false) ORDER BY DEPT_ID";
        
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            
            while (rs.next()) {
                DeptBean dept = new DeptBean();
                dept.setDeptId(rs.getString("DEPT_ID"));
                dept.setDeptName(rs.getString("DEPT_NAME"));
                deptList.add(dept);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return deptList;
    }
    
    /**
     * 部署番号で部署情報を検索する
     * @param deptId 部署番号
     * @return 部署情報。見つからない場合はnull
     */
    public DeptBean findByDeptId(String deptId) {
        DeptBean dept = null;
        String sql = "SELECT DEPT_ID, DEPT_NAME FROM dept WHERE DEPT_ID = ? AND (IS_DELETED IS NULL OR IS_DELETED = false)";
        
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, deptId);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    dept = new DeptBean();
                    dept.setDeptId(rs.getString("DEPT_ID"));
                    dept.setDeptName(rs.getString("DEPT_NAME"));
                }
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return dept;
    }
    
    /**
     * 新しい部署を追加する
     * @param dept 追加する部署情報
     * @return 追加に成功した場合true、失敗した場合false
     */
    public boolean insert(DeptBean dept) {
        return insert(dept, "system");
    }
    
    public boolean insert(DeptBean dept, String createdBy) {
        String sql = "INSERT INTO dept (DEPT_ID, DEPT_NAME, IS_DELETED, CREATED_AT, CREATED_BY, UPDATED_AT, UPDATED_BY) VALUES (?, ?, false, NOW(), ?, NOW(), ?)";
        
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, dept.getDeptId());
            ps.setString(2, dept.getDeptName());
            ps.setString(3, createdBy);
            ps.setString(4, createdBy);
            
            int count = ps.executeUpdate();
            return count > 0;
            
        } catch (SQLException e) {
            // 主キー重複エラーの場合
            if (e.getSQLState().equals("23000")) {
                System.err.println("部署番号が既に存在します: " + dept.getDeptId());
            } else {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return false;
    }
    
    /**
     * 部署情報を更新する
     * @param dept 更新する部署情報
     * @return 更新に成功した場合true、失敗した場合false
     */
    public boolean update(DeptBean dept) {
        return update(dept, "system");
    }
    
    public boolean update(DeptBean dept, String updatedBy) {
        String sql = "UPDATE dept SET DEPT_NAME = ?, UPDATED_AT = NOW(), UPDATED_BY = ? WHERE DEPT_ID = ? AND IS_DELETED = false";
        
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, dept.getDeptName());
            ps.setString(2, updatedBy);
            ps.setString(3, dept.getDeptId());
            
            int count = ps.executeUpdate();
            return count > 0;
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return false;
    }
    
    /**
     * 部署を削除する
     * @param deptId 削除する部署番号
     * @return 削除に成功した場合true、失敗した場合false
     */
    public boolean delete(String deptId) {
        return delete(deptId, "system");
    }
    
    public boolean delete(String deptId, String deletedBy) {
        String sql = "UPDATE dept SET IS_DELETED = true, DELETED_AT = NOW(), DELETED_BY = ? WHERE DEPT_ID = ?";
        
        System.out.println("DeptDao.delete - Deleting dept: " + deptId + " by user: " + deletedBy);
        
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, deletedBy);
            ps.setString(2, deptId);
            
            int count = ps.executeUpdate();
            System.out.println("DeptDao.delete - Update count: " + count);
            
            // 削除後の確認のためのSQL実行
            String confirmSql = "SELECT DEPT_ID, DELETED_AT, DELETED_BY FROM dept WHERE DEPT_ID = ? AND IS_DELETED = true";
            try (PreparedStatement confirmPs = conn.prepareStatement(confirmSql)) {
                confirmPs.setString(1, deptId);
                try (ResultSet rs = confirmPs.executeQuery()) {
                    if (rs.next()) {
                        System.out.println("DeptDao.delete - Confirmed deletion: " + rs.getString("DEPT_ID") 
                            + ", deletedAt: " + rs.getTimestamp("DELETED_AT") 
                            + ", deletedBy: " + rs.getString("DELETED_BY"));
                    } else {
                        System.out.println("DeptDao.delete - No deleted record found for confirmation");
                    }
                }
            }
            
            return count > 0;
            
        } catch (SQLException e) {
            // 外部キー制約エラーの場合（この部署に所属する社員がいる場合）
            if (e.getSQLState().startsWith("23")) {
                System.err.println("この部署に所属する社員が存在するため削除できません: " + deptId);
            } else {
                System.err.println("DeptDao.delete - SQL Exception: " + e.getMessage());
                e.printStackTrace();
            }
        } catch (Exception e) {
            System.err.println("DeptDao.delete - Exception: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }
    
    /**
     * 部署番号の重複をチェックする
     * @param deptId チェックする部署番号
     * @return 既に存在する場合true、存在しない場合false
     */
    public boolean exists(String deptId) {
        return findByDeptId(deptId) != null;
    }
    
    /**
     * 削除された部署一覧を取得する
     * @return 削除された部署情報のリスト
     */
    public List<DeptBean> findDeleted() {
        List<DeptBean> deptList = new ArrayList<>();
        String sql = "SELECT DEPT_ID, DEPT_NAME, DELETED_AT, DELETED_BY FROM dept WHERE IS_DELETED = true ORDER BY DELETED_AT DESC, DEPT_ID";
        
        System.out.println("DeptDao.findDeleted - SQL: " + sql);
        
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            
            while (rs.next()) {
                DeptBean dept = new DeptBean();
                dept.setDeptId(rs.getString("DEPT_ID"));
                dept.setDeptName(rs.getString("DEPT_NAME"));
                dept.setDeletedAt(rs.getTimestamp("DELETED_AT"));
                dept.setDeletedBy(rs.getString("DELETED_BY"));
                
                System.out.println("DeptDao.findDeleted - Found deleted dept: " + dept.getDeptId() 
                    + ", deletedAt: " + dept.getDeletedAt() + ", deletedBy: " + dept.getDeletedBy());
                
                deptList.add(dept);
            }
            
            System.out.println("DeptDao.findDeleted - Total deleted depts found: " + deptList.size());
            
        } catch (Exception e) {
            System.err.println("DeptDao.findDeleted - Exception occurred: " + e.getMessage());
            e.printStackTrace();
        }
        
        return deptList;
    }
    
    /**
     * 削除された部署を復元する
     * @param deptId 復元する部署番号
     * @return 復元に成功した場合true、失敗した場合false
     */
    public boolean restore(String deptId) {
        return restore(deptId, "system");
    }
    
    public boolean restore(String deptId, String updatedBy) {
        String sql = "UPDATE dept SET IS_DELETED = false, DELETED_AT = NULL, DELETED_BY = NULL, UPDATED_AT = NOW(), UPDATED_BY = ? WHERE DEPT_ID = ? AND IS_DELETED = true";
        
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, updatedBy);
            ps.setString(2, deptId);
            
            int count = ps.executeUpdate();
            return count > 0;
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return false;
    }
}