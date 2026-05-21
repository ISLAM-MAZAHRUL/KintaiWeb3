package kintai;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
public class ProjectDao {
    private DBAccess db = new DBAccess();
    /**
     * すべてのプロジェクト情報を取得する
     * @return プロジェクト情報のリスト
     */
    public List<ProjectBean> findAll() {
        List<ProjectBean> projectmanageList = new ArrayList<>();
        String sql = "SELECT PROJECT_ID, PROJECT_NAME, BUDGET_AMOUNT, START_DATE, END_DATE FROM project WHERE (IS_DELETED IS NULL OR IS_DELETED = FALSE) ORDER BY PROJECT_ID";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                ProjectBean project = new ProjectBean();
                project.setProjectId(rs.getInt("PROJECT_ID"));
                project.setProjectName(rs.getString("PROJECT_NAME"));
                project.setBudgetAmount(rs.getInt("BUDGET_AMOUNT"));
                
                Date start = rs.getDate("START_DATE");
                Date end = rs.getDate("END_DATE");
                project.setStartDate(start != null ? start.toLocalDate() : null);
                project.setEndDate(end != null ? end.toLocalDate() : null);
                
                projectmanageList.add(project);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return projectmanageList;
    }
    /**
     * プロジェクトIDでプロジェクト情報を検索する
     * @param projectId プロジェクトID
     * @return プロジェクト情報。見つからない場合はnull
     */
    public ProjectBean findByProjectId(int projectId) {
        ProjectBean project = null;
        String sql = "SELECT PROJECT_ID, PROJECT_NAME, BUDGET_AMOUNT, START_DATE, END_DATE FROM project WHERE PROJECT_ID = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, projectId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    project = new ProjectBean();
                    project.setProjectId(rs.getInt("PROJECT_ID"));
                    project.setProjectName(rs.getString("PROJECT_NAME"));
                    project.setBudgetAmount(rs.getInt("BUDGET_AMOUNT"));
                    
                    Date start = rs.getDate("START_DATE");
                    Date end = rs.getDate("END_DATE");
                    project.setStartDate(start != null ? start.toLocalDate() : null);
                    project.setEndDate(end != null ? end.toLocalDate() : null);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return project;
    }
    /**
     * プロジェクトIDでプロジェクト情報を検索する (findById用)
     * @param projectId プロジェクトID
     * @return プロジェクト情報。見つからない場合はnull
     */
    public ProjectBean findById(int projectId) {
        return findByProjectId(projectId);
    }
    /**
     * 新しいプロジェクトを追加する
     * @param projectBean 追加するプロジェクト情報
     * @return 追加に成功した場合true、失敗した場合false
     */
    public boolean insert(ProjectBean projectBean, String createdBy, String updatedBy) {
        String sql = "INSERT INTO project (PROJECT_NAME, BUDGET_AMOUNT, START_DATE, END_DATE, CREATED_BY, UPDATED_BY) VALUES (?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, projectBean.getProjectName());
            
            if (projectBean.getBudgetAmount() != null) {
                ps.setInt(2, projectBean.getBudgetAmount());
            } else {
                ps.setNull(2, java.sql.Types.INTEGER);
            }
            
            if (projectBean.getStartDate() != null) {
                ps.setDate(3, java.sql.Date.valueOf(projectBean.getStartDate()));
            } else {
                ps.setNull(3, java.sql.Types.DATE);
            }
            if (projectBean.getEndDate() != null) {
                ps.setDate(4, java.sql.Date.valueOf(projectBean.getEndDate()));
            } else {
                ps.setNull(4, java.sql.Types.DATE);
            }
            
            // 追加
            ps.setString(5, createdBy);
            ps.setString(6, updatedBy);
            
            int count = ps.executeUpdate();
            return count > 0;
            
        } catch (SQLException e) {
            if (e.getSQLState().equals("23000")) {
                System.err.println("プロジェクトが既に存在します: " + projectBean.getProjectId());
            } else {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return false;
    }
    
    /**
     * プロジェクト情報を更新する
     * @param updateProject 更新するプロジェクト情報
     * @return 更新に成功した場合true、失敗した場合false
     */
    public boolean update(ProjectBean updateProject, String updatedBy) {
        String sql = "UPDATE project SET PROJECT_NAME=?, BUDGET_AMOUNT=?, START_DATE=?, END_DATE=?, UPDATED_BY=? WHERE PROJECT_ID = ?";
        
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, updateProject.getProjectName());
            
            if (updateProject.getBudgetAmount() != null) {
                ps.setInt(2, updateProject.getBudgetAmount());
            } else {
                ps.setNull(2, java.sql.Types.INTEGER);
            }
            
            if (updateProject.getStartDate() != null) {
                ps.setDate(3, java.sql.Date.valueOf(updateProject.getStartDate()));
            } else {
                ps.setNull(3, java.sql.Types.DATE);
            }
            if (updateProject.getEndDate() != null) {
                ps.setDate(4, java.sql.Date.valueOf(updateProject.getEndDate()));
            } else {
                ps.setNull(4, java.sql.Types.DATE);
            }
            
            ps.setString(5, updatedBy);
            ps.setInt(6, updateProject.getProjectId());
            
            int count = ps.executeUpdate();
            return count > 0;
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return false;
    }
    
    /**
     * プロジェクトを論理削除する
     * @param projectId 削除するプロジェクトID
     * @return 削除に成功した場合true、失敗した場合false
     */
    public boolean logicalDelete(int projectId, String deletedBy) {
        String sql = "UPDATE project SET IS_DELETED = TRUE, DELETED_AT = NOW(), DELETED_BY = ?, UPDATED_AT = NOW(), UPDATED_BY = ? WHERE PROJECT_ID = ?";
        
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, deletedBy);
            ps.setString(2, deletedBy);
            ps.setInt(3, projectId);
            
            int count = ps.executeUpdate();
            return count > 0;
            
        } catch (SQLException e) {
            if (e.getSQLState().startsWith("23")) {
                System.err.println("このプロジェクトに関連するデータが存在するため削除できません: " + projectId);
            } else {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return false;
    }
    
    /**
     * プロジェクト番号の重複をチェックする
     * @param projectId チェックするプロジェクトID
     * @return 既に存在する場合true、存在しない場合false
     */
    public boolean exists(int projectId) {
        return findByProjectId(projectId) != null;
    }
    
    public String getProjectNameById(int projectId) {
        String sql = "SELECT PROJECT_NAME FROM project WHERE PROJECT_ID = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, projectId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("PROJECT_NAME");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}