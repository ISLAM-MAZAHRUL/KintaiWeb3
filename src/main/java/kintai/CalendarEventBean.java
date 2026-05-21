package kintai;

import java.io.Serializable;
import java.time.LocalDate; // LocalDate をインポート

/**
 * カレンダーイベント情報（calendar_eventテーブルのレコード）を保持するJavaBean。
 * 新しいER図のcalendar_eventテーブルに準拠し、繰り返しルールIDを追加。
 */
public class CalendarEventBean implements Serializable {
    private static final long serialVersionUID = 1L;

    private LocalDate eventDate;    // calendar_eventテーブルの「EVENT_DATE」列に対応（主キー）
    private String eventName;       // calendar_eventテーブルの「EVENT_NAME」列に対応
    private boolean isWork;         // calendar_eventテーブルの「IS_WORK」列に対応（1:出勤日, 0:休日）
    private Integer repeatRuleId;   // calendar_eventテーブルの「REPEAT_RULE_ID」列に対応（新規追加、null許容のためInteger）
    private boolean isSystemDefined; // calendar_eventテーブルの「IS_SYSTEM_DEFINED」列に対応（システム定義フラグ）

    /**
     * デフォルトコンストラクタ
     */
    public CalendarEventBean() {
    }

    /**
     * 全フィールドを初期化するコンストラクタ
     * @param eventDate イベント日付
     * @param eventName イベント名
     * @param isWork 出勤日かどうか
     */
    public CalendarEventBean(LocalDate eventDate, String eventName, boolean isWork) {
        this.eventDate = eventDate;
        this.eventName = eventName;
        this.isWork = isWork;
        this.repeatRuleId = null; // デフォルトではnull
        this.isSystemDefined = false; // デフォルトではユーザー定義
    }

    // --- 以下、各フィールドのアクセサメソッド (getter/setter) ---

    public LocalDate getEventDate() {
        return eventDate;
    }

    public void setEventDate(LocalDate eventDate) {
        this.eventDate = eventDate;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public boolean isWork() { // boolean型のgetterはis接頭辞が一般的
        return isWork;
    }

    public void setWork(boolean isWork) {
        this.isWork = isWork;
    }

    // 新規追加
    public Integer getRepeatRuleId() {
        return repeatRuleId;
    }

    public void setRepeatRuleId(Integer repeatRuleId) {
        this.repeatRuleId = repeatRuleId;
    }

    /**
     * isWorkの値に基づいて「出勤日」または「休日」の文字列を返します。
     * @return 「出勤日」または「休日」
     */
    public String getWorkStatusName() {
        return isWork ? "出勤日" : "休日";
    }

    // JSPとの互換性のための追加メソッド
    /**
     * isWorkのゲッターメソッド（getIsWork形式）
     * JSPでの使用を考慮した互換性メソッド
     * @return 出勤日かどうか
     */
    public boolean getIsWork() {
        return isWork;
    }

    public boolean isSystemDefined() {
        return isSystemDefined;
    }

    public void setSystemDefined(boolean isSystemDefined) {
        this.isSystemDefined = isSystemDefined;
    }

    public boolean getIsSystemDefined() {
        return isSystemDefined;
    }
}
