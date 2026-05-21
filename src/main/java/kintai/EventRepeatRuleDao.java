package kintai;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * イベント繰り返しルール情報（event_repeat_ruleテーブルのレコード）へのデータアクセスを担当するクラス (DAO)。
 * 繰り返しルールの検索、追加、更新、削除を行う。
 * 新しいER図のevent_repeat_ruleテーブルに準拠。
 */
public class EventRepeatRuleDao {

    private DBAccess db = new DBAccess();

    /**
     * すべてのイベント繰り返しルール情報を取得する
     * @return イベント繰り返しルール情報のリスト
     */
    public List<EventRepeatRuleBean> findAll() {
        List<EventRepeatRuleBean> ruleList = new ArrayList<>();
        String sql = "SELECT REPEAT_RULE_ID, REPEAT_TYPE, REPEAT_INTERVAL, REPEAT_DAYS_OF_WEEK, REPEAT_END_DATE, CREATED_AT, UPDATED_AT FROM event_repeat_rule ORDER BY REPEAT_RULE_ID";

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                EventRepeatRuleBean rule = new EventRepeatRuleBean();
                rule.setRuleId(rs.getInt("REPEAT_RULE_ID"));
                rule.setRepeatType(rs.getString("REPEAT_TYPE"));
                rule.setRepeatInterval(rs.getInt("REPEAT_INTERVAL"));
                rule.setRepeatDaysOfWeek(rs.getString("REPEAT_DAYS_OF_WEEK"));
                
                Date repeatEndDateSql = rs.getDate("REPEAT_END_DATE");
                if (repeatEndDateSql != null) {
                    rule.setRepeatEndDate(repeatEndDateSql.toLocalDate());
                } else {
                    rule.setRepeatEndDate(null);
                }

                rule.setCreatedAt(rs.getTimestamp("CREATED_AT") != null ? rs.getTimestamp("CREATED_AT").toLocalDateTime() : null);
                rule.setUpdatedAt(rs.getTimestamp("UPDATED_AT") != null ? rs.getTimestamp("UPDATED_AT").toLocalDateTime() : null);
                ruleList.add(rule);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return ruleList;
    }

    /**
     * ルールIDでイベント繰り返しルール情報を検索する
     * @param ruleId ルールID
     * @return イベント繰り返しルール情報。見つからない場合はnull
     */
    public EventRepeatRuleBean findByRuleId(int ruleId) {
        EventRepeatRuleBean rule = null;
        String sql = "SELECT REPEAT_RULE_ID, REPEAT_TYPE, REPEAT_INTERVAL, REPEAT_DAYS_OF_WEEK, REPEAT_END_DATE, CREATED_AT, UPDATED_AT FROM event_repeat_rule WHERE REPEAT_RULE_ID = ?";

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, ruleId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    rule = new EventRepeatRuleBean();
                    rule.setRuleId(rs.getInt("REPEAT_RULE_ID"));
                    rule.setRepeatType(rs.getString("REPEAT_TYPE"));
                    rule.setRepeatInterval(rs.getInt("REPEAT_INTERVAL"));
                    rule.setRepeatDaysOfWeek(rs.getString("REPEAT_DAYS_OF_WEEK"));
                    
                    Date repeatEndDateSql = rs.getDate("REPEAT_END_DATE");
                    if (repeatEndDateSql != null) {
                        rule.setRepeatEndDate(repeatEndDateSql.toLocalDate());
                    } else {
                        rule.setRepeatEndDate(null);
                    }

                    rule.setCreatedAt(rs.getTimestamp("CREATED_AT") != null ? rs.getTimestamp("CREATED_AT").toLocalDateTime() : null);
                    rule.setUpdatedAt(rs.getTimestamp("UPDATED_AT") != null ? rs.getTimestamp("UPDATED_AT").toLocalDateTime() : null);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return rule;
    }

    /**
     * 新しいイベント繰り返しルールを追加する
     * @param rule 追加するイベント繰り返しルール情報
     * @return 追加に成功した場合true、失敗した場合false
     */
    public boolean insert(EventRepeatRuleBean rule) {
        String sql = "INSERT INTO event_repeat_rule (REPEAT_TYPE, REPEAT_INTERVAL, REPEAT_DAYS_OF_WEEK, REPEAT_END_DATE, CREATED_AT, CREATED_BY, UPDATED_AT, UPDATED_BY) VALUES (?, ?, ?, ?, NOW(), 'admin', NOW(), 'admin')";
        
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, rule.getRepeatType());
            ps.setInt(2, rule.getRepeatInterval());
            ps.setString(3, rule.getRepeatDaysOfWeek());
            
            if (rule.getRepeatEndDate() != null) {
                ps.setDate(4, Date.valueOf(rule.getRepeatEndDate()));
            } else {
                ps.setNull(4, java.sql.Types.DATE);
            }

            int count = ps.executeUpdate();
            return count > 0;
            
        } catch (SQLException e) {
            // UNIQUE制約違反など
            if (e.getSQLState().startsWith("23")) {
                System.err.println("繰り返しルールの重複または外部キー制約違反: " + e.getMessage());
            } else {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * イベント繰り返しルール情報を更新する
     * @param rule 更新するイベント繰り返しルール情報
     * @return 更新に成功した場合true、失敗した場合false
     */
    public boolean update(EventRepeatRuleBean rule) {
        String sql = "UPDATE event_repeat_rule SET REPEAT_TYPE = ?, REPEAT_INTERVAL = ?, REPEAT_DAYS_OF_WEEK = ?, REPEAT_END_DATE = ?, UPDATED_AT = NOW(), UPDATED_BY = 'admin' WHERE REPEAT_RULE_ID = ?";
        
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, rule.getRepeatType());
            ps.setInt(2, rule.getRepeatInterval());
            ps.setString(3, rule.getRepeatDaysOfWeek());
            
            if (rule.getRepeatEndDate() != null) {
                ps.setDate(4, Date.valueOf(rule.getRepeatEndDate()));
            } else {
                ps.setNull(4, java.sql.Types.DATE);
            }
            ps.setInt(5, rule.getRuleId());

            int count = ps.executeUpdate();
            return count > 0;
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * イベント繰り返しルールを削除する
     * @param ruleId 削除する繰り返しルールID
     * @return 削除に成功した場合true、失敗した場合false
     */
    public boolean delete(int ruleId) {
        String sql = "DELETE FROM event_repeat_rule WHERE REPEAT_RULE_ID = ?";
        
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, ruleId);
            
            int count = ps.executeUpdate();
            return count > 0;
            
        } catch (SQLException e) {
            // 外部キー制約エラーの場合（このルールが calendar_event に紐づいている場合）
            if (e.getSQLState().startsWith("23")) {
                System.err.println("この繰り返しルールに関連するイベントが存在するため削除できません: " + ruleId);
            } else {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

}
