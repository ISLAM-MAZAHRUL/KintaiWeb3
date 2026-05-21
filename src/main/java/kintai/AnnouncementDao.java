package kintai;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * announcementテーブルへのデータアクセスを担当するクラス (DAO)。
 * アナウンス情報の検索、追加、更新、削除を行う。
 */
public class AnnouncementDao {
    
    private DBAccess db = new DBAccess();
    
    /**
     * 有効なアナウンス情報を表示順で取得する
     * @return アナウンス情報のリスト
     */
    public List<AnnouncementBean> findActiveAnnouncements() {
        List<AnnouncementBean> announcementList = new ArrayList<>();
        String sql = "SELECT ANNOUNCEMENT_ID, TITLE, CONTENT, IS_ACTIVE, DISPLAY_ORDER, " +
                     "IS_DELETED, DELETED_AT, DELETED_BY, CREATED_AT, CREATED_BY, UPDATED_AT, UPDATED_BY " +
                     "FROM announcement " +
                     "WHERE (IS_ACTIVE = true OR IS_ACTIVE = 1) AND (IS_DELETED = false OR IS_DELETED = 0) " +
                     "ORDER BY DISPLAY_ORDER ASC, CREATED_AT DESC";
        
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            
            while (rs.next()) {
                AnnouncementBean announcement = new AnnouncementBean();
                announcement.setAnnouncementId(rs.getInt("ANNOUNCEMENT_ID"));
                announcement.setTitle(rs.getString("TITLE"));
                announcement.setContent(rs.getString("CONTENT"));
                announcement.setActive(rs.getBoolean("IS_ACTIVE"));
                announcement.setDisplayOrder(rs.getInt("DISPLAY_ORDER"));
                announcement.setDeleted(rs.getBoolean("IS_DELETED"));
                
                Timestamp deletedAt = rs.getTimestamp("DELETED_AT");
                if (deletedAt != null) {
                    announcement.setDeletedAt(deletedAt.toLocalDateTime());
                }
                announcement.setDeletedBy(rs.getString("DELETED_BY"));
                
                Timestamp createdAt = rs.getTimestamp("CREATED_AT");
                if (createdAt != null) {
                    announcement.setCreatedAt(createdAt.toLocalDateTime());
                }
                announcement.setCreatedBy(rs.getString("CREATED_BY"));
                
                Timestamp updatedAt = rs.getTimestamp("UPDATED_AT");
                if (updatedAt != null) {
                    announcement.setUpdatedAt(updatedAt.toLocalDateTime());
                }
                announcement.setUpdatedBy(rs.getString("UPDATED_BY"));
                
                announcementList.add(announcement);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        return announcementList;
    }
    
    /**
     * すべてのアナウンス情報を取得する（管理者用）
     * @return アナウンス情報のリスト
     */
    public List<AnnouncementBean> findAll() {
        List<AnnouncementBean> announcementList = new ArrayList<>();
        String sql = "SELECT ANNOUNCEMENT_ID, TITLE, CONTENT, IS_ACTIVE, DISPLAY_ORDER, " +
                     "IS_DELETED, DELETED_AT, DELETED_BY, CREATED_AT, CREATED_BY, UPDATED_AT, UPDATED_BY " +
                     "FROM announcement " +
                     "WHERE IS_DELETED = false " +
                     "ORDER BY DISPLAY_ORDER ASC, CREATED_AT DESC";
        
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            
            while (rs.next()) {
                AnnouncementBean announcement = new AnnouncementBean();
                announcement.setAnnouncementId(rs.getInt("ANNOUNCEMENT_ID"));
                announcement.setTitle(rs.getString("TITLE"));
                announcement.setContent(rs.getString("CONTENT"));
                announcement.setActive(rs.getBoolean("IS_ACTIVE"));
                announcement.setDisplayOrder(rs.getInt("DISPLAY_ORDER"));
                announcement.setDeleted(rs.getBoolean("IS_DELETED"));
                
                Timestamp deletedAt = rs.getTimestamp("DELETED_AT");
                if (deletedAt != null) {
                    announcement.setDeletedAt(deletedAt.toLocalDateTime());
                }
                announcement.setDeletedBy(rs.getString("DELETED_BY"));
                
                Timestamp createdAt = rs.getTimestamp("CREATED_AT");
                if (createdAt != null) {
                    announcement.setCreatedAt(createdAt.toLocalDateTime());
                }
                announcement.setCreatedBy(rs.getString("CREATED_BY"));
                
                Timestamp updatedAt = rs.getTimestamp("UPDATED_AT");
                if (updatedAt != null) {
                    announcement.setUpdatedAt(updatedAt.toLocalDateTime());
                }
                announcement.setUpdatedBy(rs.getString("UPDATED_BY"));
                
                announcementList.add(announcement);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        return announcementList;
    }
    
    /**
     * IDでアナウンス情報を検索する
     * @param announcementId アナウンスID
     * @return アナウンス情報。見つからない場合はnull
     */
    public AnnouncementBean findById(int announcementId) {
        AnnouncementBean announcement = null;
        String sql = "SELECT ANNOUNCEMENT_ID, TITLE, CONTENT, IS_ACTIVE, DISPLAY_ORDER, " +
                     "IS_DELETED, DELETED_AT, DELETED_BY, CREATED_AT, CREATED_BY, UPDATED_AT, UPDATED_BY " +
                     "FROM announcement " +
                     "WHERE ANNOUNCEMENT_ID = ?";
        
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, announcementId);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    announcement = new AnnouncementBean();
                    announcement.setAnnouncementId(rs.getInt("ANNOUNCEMENT_ID"));
                    announcement.setTitle(rs.getString("TITLE"));
                    announcement.setContent(rs.getString("CONTENT"));
                    announcement.setActive(rs.getBoolean("IS_ACTIVE"));
                    announcement.setDisplayOrder(rs.getInt("DISPLAY_ORDER"));
                    announcement.setDeleted(rs.getBoolean("IS_DELETED"));
                    
                    Timestamp deletedAt = rs.getTimestamp("DELETED_AT");
                    if (deletedAt != null) {
                        announcement.setDeletedAt(deletedAt.toLocalDateTime());
                    }
                    announcement.setDeletedBy(rs.getString("DELETED_BY"));
                    
                    Timestamp createdAt = rs.getTimestamp("CREATED_AT");
                    if (createdAt != null) {
                        announcement.setCreatedAt(createdAt.toLocalDateTime());
                    }
                    announcement.setCreatedBy(rs.getString("CREATED_BY"));
                    
                    Timestamp updatedAt = rs.getTimestamp("UPDATED_AT");
                    if (updatedAt != null) {
                        announcement.setUpdatedAt(updatedAt.toLocalDateTime());
                    }
                    announcement.setUpdatedBy(rs.getString("UPDATED_BY"));
                }
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        return announcement;
    }
    
    /**
     * アナウンス情報を追加する
     * @param announcement 追加するアナウンス情報
     * @return 追加に成功した場合はtrue、失敗した場合はfalse
     */
    public boolean insert(AnnouncementBean announcement) {
        String sql = "INSERT INTO announcement (TITLE, CONTENT, IS_ACTIVE, DISPLAY_ORDER, " +
                     "IS_DELETED, CREATED_AT, CREATED_BY, UPDATED_AT, UPDATED_BY) " +
                     "VALUES (?, ?, ?, ?, false, NOW(), ?, NOW(), ?)";
        
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, announcement.getTitle());
            ps.setString(2, announcement.getContent());
            ps.setBoolean(3, announcement.isActive());
            ps.setInt(4, announcement.getDisplayOrder());
            ps.setString(5, announcement.getCreatedBy());
            ps.setString(6, announcement.getUpdatedBy());
            
            int result = ps.executeUpdate();
            return result > 0;
            
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * アナウンス情報を更新する
     * @param announcement 更新するアナウンス情報
     * @return 更新に成功した場合はtrue、失敗した場合はfalse
     */
    public boolean update(AnnouncementBean announcement) {
        String sql = "UPDATE announcement SET TITLE = ?, CONTENT = ?, IS_ACTIVE = ?, " +
                     "DISPLAY_ORDER = ?, UPDATED_AT = NOW(), UPDATED_BY = ? " +
                     "WHERE ANNOUNCEMENT_ID = ?";
        
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, announcement.getTitle());
            ps.setString(2, announcement.getContent());
            ps.setBoolean(3, announcement.isActive());
            ps.setInt(4, announcement.getDisplayOrder());
            ps.setString(5, announcement.getUpdatedBy());
            ps.setInt(6, announcement.getAnnouncementId());
            
            int result = ps.executeUpdate();
            return result > 0;
            
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * アナウンス情報を論理削除する
     * @param announcementId 削除するアナウンスID
     * @param deletedBy 削除者
     * @return 削除に成功した場合はtrue、失敗した場合はfalse
     */
    public boolean delete(int announcementId, String deletedBy) {
        String sql = "UPDATE announcement SET IS_DELETED = true, DELETED_AT = NOW(), " +
                     "DELETED_BY = ?, UPDATED_AT = NOW(), UPDATED_BY = ? " +
                     "WHERE ANNOUNCEMENT_ID = ?";
        
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, deletedBy);
            ps.setString(2, deletedBy);
            ps.setInt(3, announcementId);
            
            int result = ps.executeUpdate();
            return result > 0;
            
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}