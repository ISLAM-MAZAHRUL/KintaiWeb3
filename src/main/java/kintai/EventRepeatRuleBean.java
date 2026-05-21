package kintai;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime; // CREATED_AT, UPDATED_AT用
import java.util.ArrayList;
import java.util.List;

/**
 * イベント繰り返しルール情報（event_repeat_ruleテーブルのレコード）を保持するJavaBean。
 * 新しいER図のevent_repeat_ruleテーブルに準拠。
 */
public class EventRepeatRuleBean implements Serializable {
    private static final long serialVersionUID = 1L;

    private int ruleId;             // 繰り返しルールID (RULE_ID)
    private String repeatType;      // 繰り返しタイプ (REPEAT_TYPE)
    private int repeatInterval;     // 繰り返し間隔 (REPEAT_INTERVAL)
    private String repeatDaysOfWeek; // 繰り返し曜日 (REPEAT_DAYS_OF_WEEK)
    private LocalDate repeatEndDate; // 繰り返し終了日 (REPEAT_END_DATE)
    private LocalDateTime createdAt; // 作成日時 (CREATED_AT)
    private LocalDateTime updatedAt; // 更新日時 (UPDATED_AT)

    /**
     * デフォルトコンストラクタ
     */
    public EventRepeatRuleBean() {
    }

    // --- 以下、各フィールドのアクセサメソッド (getter/setter) ---

    public int getRuleId() {
        return ruleId;
    }

    public void setRuleId(int ruleId) {
        this.ruleId = ruleId;
    }

    public String getRepeatType() {
        return repeatType;
    }

    public void setRepeatType(String repeatType) {
        this.repeatType = repeatType;
    }

    public int getRepeatInterval() {
        return repeatInterval;
    }

    public void setRepeatInterval(int repeatInterval) {
        this.repeatInterval = repeatInterval;
    }

    public String getRepeatDaysOfWeek() {
        return repeatDaysOfWeek;
    }

    public void setRepeatDaysOfWeek(String repeatDaysOfWeek) {
        this.repeatDaysOfWeek = repeatDaysOfWeek;
    }

    public LocalDate getRepeatEndDate() {
        return repeatEndDate;
    }

    public void setRepeatEndDate(LocalDate repeatEndDate) {
        this.repeatEndDate = repeatEndDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * repeatDaysOfWeek の文字列に基づいて、選択された曜日の日本語リストを返します。
     * 例: "1,3,5" -> ["月曜日", "水曜日", "金曜日"]
     * @return 選択された曜日の日本語リスト
     */
    public List<String> getSelectedDaysOfWeekNames() {
        List<String> names = new ArrayList<>();
        if (repeatDaysOfWeek != null && !repeatDaysOfWeek.isEmpty()) {
            String[] days = repeatDaysOfWeek.split(",");
            for (String day : days) {
                switch (day) {
                    case "1": names.add("月曜日"); break;
                    case "2": names.add("火曜日"); break;
                    case "3": names.add("水曜日"); break;
                    case "4": names.add("木曜日"); break;
                    case "5": names.add("金曜日"); break;
                    case "6": names.add("土曜日"); break;
                    case "7": names.add("日曜日"); break;
                }
            }
        }
        return names;
    }

    /**
     * repeatType の値に基づいて、繰り返しタイプを日本語で返します。
     * @return 日本語の繰り返しタイプ
     */
    public String getRepeatTypeJapanese() {
        switch (repeatType) {
            case "DAILY": return "毎日";
            case "WEEKLY": return "毎週";
            case "MONTHLY_DAY": return "毎月（日付指定）";
            case "MONTHLY_WEEKDAY": return "毎月（第N週目・曜日指定）";
            case "YEARLY": return "毎年";
            default: return "単発"; // もしNONEタイプが来る場合や未定義の場合
        }
    }
}
