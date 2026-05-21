package kintai;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * postテーブルへのデータアクセスを担当するクラス (DAO)。
 * 役職情報の検索、追加、更新、削除を行う。
 */
public class PostDao {
    
    private DBAccess db = new DBAccess();
    
    /**
     * すべての役職情報を取得する
     * @return 役職情報のリスト
     */
    public List<PostBean> findAll() {
        List<PostBean> postList = new ArrayList<>();
        String sql = "SELECT POST_ID, POST_NAME FROM post WHERE (IS_DELETED IS NULL OR IS_DELETED = false) ORDER BY POST_ID";
        
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            
            while (rs.next()) {
                PostBean post = new PostBean();
                post.setPostId(rs.getString("POST_ID"));
                post.setPostName(rs.getString("POST_NAME"));
                postList.add(post);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return postList;
    }
    
    /**
     * 役職番号で役職情報を検索する
     * @param postId 役職番号
     * @return 役職情報。見つからない場合はnull
     */
    public PostBean findByPostId(String postId) {
        PostBean post = null;
        String sql = "SELECT POST_ID, POST_NAME FROM post WHERE POST_ID = ? AND (IS_DELETED IS NULL OR IS_DELETED = false)";
        
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, postId);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    post = new PostBean();
                    post.setPostId(rs.getString("POST_ID"));
                    post.setPostName(rs.getString("POST_NAME"));
                }
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return post;
    }
    
    /**
     * 新しい役職を追加する
     * @param post 追加する役職情報
     * @return 追加に成功した場合true、失敗した場合false
     */
    public boolean insert(PostBean post) {
        return insert(post, "system");
    }
    
    public boolean insert(PostBean post, String createdBy) {
        String sql = "INSERT INTO post (POST_ID, POST_NAME, IS_DELETED, CREATED_AT, CREATED_BY, UPDATED_AT, UPDATED_BY) VALUES (?, ?, false, NOW(), ?, NOW(), ?)";
        
        System.out.println("PostDao.insert - attempting to insert: " + post.getPostId() + ", " + post.getPostName() + " by " + createdBy);
        
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, post.getPostId());
            ps.setString(2, post.getPostName());
            ps.setString(3, createdBy);
            ps.setString(4, createdBy);
            
            int count = ps.executeUpdate();
            System.out.println("PostDao.insert - insert successful, rows affected: " + count);
            return count > 0;
            
        } catch (SQLException e) {
            // 主キー重複エラーの場合
            if (e.getSQLState().equals("23000")) {
                System.err.println("役職番号が既に存在します: " + post.getPostId());
            } else {
                System.err.println("SQL例外が発生しました: " + e.getMessage());
                e.printStackTrace();
            }
        } catch (Exception e) {
            System.err.println("予期しない例外が発生しました: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }
    
    /**
     * 役職情報を更新する
     * @param post 更新する役職情報
     * @return 更新に成功した場合true、失敗した場合false
     */
    public boolean update(PostBean post) {
        return update(post, "system");
    }
    
    public boolean update(PostBean post, String updatedBy) {
        String sql = "UPDATE post SET POST_NAME = ?, UPDATED_AT = NOW(), UPDATED_BY = ? WHERE POST_ID = ? AND IS_DELETED = false";
        
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, post.getPostName());
            ps.setString(2, updatedBy);
            ps.setString(3, post.getPostId());
            
            int count = ps.executeUpdate();
            return count > 0;
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return false;
    }
    
    /**
     * 役職を削除する
     * @param postId 削除する役職番号
     * @return 削除に成功した場合true、失敗した場合false
     */
    public boolean delete(String postId) {
        return delete(postId, "system");
    }
    
    public boolean delete(String postId, String deletedBy) {
        String sql = "UPDATE post SET IS_DELETED = true, DELETED_AT = NOW(), DELETED_BY = ? WHERE POST_ID = ?";
        
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, deletedBy);
            ps.setString(2, postId);
            
            int count = ps.executeUpdate();
            return count > 0;
            
        } catch (SQLException e) {
            // 外部キー制約エラーの場合（この役職に所属する社員がいる場合）
            if (e.getSQLState().startsWith("23")) {
                System.err.println("この役職に所属する社員が存在するため削除できません: " + postId);
            } else {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return false;
    }
    
    /**
     * 役職番号の重複をチェックする
     * @param postId チェックする役職番号
     * @return 既に存在する場合true、存在しない場合false
     */
    public boolean exists(String postId) {
        return findByPostId(postId) != null;
    }
    
    /**
     * 削除された役職一覧を取得する
     * @return 削除された役職情報のリスト
     */
    public List<PostBean> findDeleted() {
        List<PostBean> postList = new ArrayList<>();
        String sql = "SELECT POST_ID, POST_NAME, DELETED_AT, DELETED_BY FROM post WHERE IS_DELETED = true ORDER BY DELETED_AT DESC, POST_ID";
        
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            
            while (rs.next()) {
                PostBean post = new PostBean();
                post.setPostId(rs.getString("POST_ID"));
                post.setPostName(rs.getString("POST_NAME"));
                post.setDeletedAt(rs.getTimestamp("DELETED_AT"));
                post.setDeletedBy(rs.getString("DELETED_BY"));
                postList.add(post);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return postList;
    }
    
    /**
     * 削除された役職を復元する
     * @param postId 復元する役職番号
     * @return 復元に成功した場合true、失敗した場合false
     */
    public boolean restore(String postId) {
        return restore(postId, "system");
    }
    
    public boolean restore(String postId, String updatedBy) {
        String sql = "UPDATE post SET IS_DELETED = false, DELETED_AT = NULL, DELETED_BY = NULL, UPDATED_AT = NOW(), UPDATED_BY = ? WHERE POST_ID = ? AND IS_DELETED = true";
        
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, updatedBy);
            ps.setString(2, postId);
            
            int count = ps.executeUpdate();
            return count > 0;
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return false;
    }
}