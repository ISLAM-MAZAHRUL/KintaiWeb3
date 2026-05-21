package kintai;

import java.io.IOException;
import java.time.DayOfWeek; // DayOfWeek をインポート
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter; // DateTimeFormatter をインポート
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors; // Collectors をインポート

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import com.google.gson.Gson; // Gsonライブラリをインポート (JSON変換用)
import com.google.gson.GsonBuilder; // GsonBuilderをインポート (日付フォーマット用)

/**
 * カレンダー管理機能とイベント管理機能を統合したサーブレット。
 * FullCalendarへのイベントデータ提供、カレンダーおよびサイドパネルからのイベント追加・編集・削除を処理する。
 * 繰り返しイベントの展開ロジックもここに実装される。
 */
@WebServlet("/CalendarManageServlet") // 全体ファイルまとめ.xlsx - Sheet1.pdf の Calendar ManageServlet.java に対応
public class CalendarManageServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private CalendarEventDao calendarEventDao = new CalendarEventDao();
    private EventRepeatRuleDao eventRepeatRuleDao = new EventRepeatRuleDao();

    // Gsonインスタンスを初期化 (LocalDateとLocalDateTimeのシリアライズ対応)
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(LocalDate.class, new LocalDateAdapter())
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .create();

    /**
     * GETリクエストの処理メソッド。
     * カレンダー表示用のイベントデータ（JSON形式）とサイドパネル表示用のイベント・ルールリストを提供し、
     * カレンダー・イベント管理画面を表示する。
     * @param request HTTPリクエストオブジェクト
     * @param response HTTPレスポンスオブジェクト
     * @throws ServletException サーブレット例外
     * @throws IOException 入出力例外
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // セッションチェック
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            response.sendRedirect(request.getContextPath() + "/web/login.jsp");
            return;
        }
        
        // ユーザー情報を取得
        UserBean user = (UserBean) session.getAttribute("user");
        
        // 管理者かどうかをリクエスト属性に設定（JSPで編集ボタン表示可否に使う）
        request.setAttribute("isAdmin", user.getRoleId() == 1);
        
        // ※ ここでは管理者チェックを削除し、全員アクセス可にする

//        // 管理者権限チェック (ROLEIDが1が管理者)
//        UserBean user = (UserBean) session.getAttribute("user");
//        if (user.getRoleId() != 1) {
//            response.sendRedirect(request.getContextPath() + "/web/menu.jsp");
//            return;
//        }

        // --- イベントデータの取得と展開 ---
        List<CalendarEventBean> allEvents = calendarEventDao.findAll(); // 全てのイベント（単発・繰り返し元）を取得
        List<EventRepeatRuleBean> allRules = eventRepeatRuleDao.findAll(); // 繰り返しルール機能を有効化

        // FullCalendarに渡すイベントリスト (展開済みイベントインスタンス)
        List<Map<String, Object>> fcEvents = new ArrayList<>();

        // イベントを展開してFullCalendar形式に変換
        for (CalendarEventBean event : allEvents) {
            // 基本イベント情報をFullCalendar用リストに追加
            Map<String, Object> fcEvent = new HashMap<>();
            fcEvent.put("title", event.getEventName());
            fcEvent.put("start", event.getEventDate().toString()); // ISO 8601形式 (YYYY-MM-DD)
            fcEvent.put("allDay", true); // 終日イベント
            // イベントを識別するためのユニークなID（単発イベントの編集時に元のイベントを特定するため）
            fcEvent.put("id", event.getEventDate().toString()); 
            

            String eventColor = getEventColor(event);
            fcEvent.put("backgroundColor", eventColor);
            fcEvent.put("borderColor", eventColor);
            
            // isWorkとrepeatRuleIdをextendedPropsに含める
            Map<String, Object> extendedProps = new HashMap<>();
            extendedProps.put("isWork", event.isWork());
            extendedProps.put("repeatRuleId", event.getRepeatRuleId());
            extendedProps.put("isSystemDefined", event.isSystemDefined());
            fcEvent.put("extendedProps", extendedProps);

            fcEvents.add(fcEvent);

            // 繰り返しルールがある場合、イベントを展開して追加
            if (event.getRepeatRuleId() != null) {
                // 繰り返しルールIDに紐づくルールをallRulesリストから検索
                EventRepeatRuleBean rule = allRules.stream()
                                            .filter(r -> r.getRuleId() == event.getRepeatRuleId()) // == でintとIntegerの比較
                                            .findFirst()
                                            .orElse(null);
                if (rule != null) {
                    // generateRepeatingEventsメソッドで、主イベント以降の繰り返しインスタンスを生成してfcEventsに追加
                    generateRepeatingEvents(event, rule, fcEvents);
                }
            }
        }

        // --- リクエスト属性にデータを設定 ---
        // FullCalendar用のイベントJSON文字列
        String eventsJson = GSON.toJson(fcEvents);
        request.setAttribute("eventsJson", eventsJson);
        
        // サイドパネルのイベント一覧表示用データ（単発・繰り返し元イベント）
        request.setAttribute("eventList", allEvents); 
        
        // ルールリストもJSPに渡す
        request.setAttribute("ruleList", allRules);
        
        // サイドパネルのイベント一覧での繰り返しルール表示用データ（JSPのJavaScriptで利用）
        // Map<Integer, EventRepeatRuleBean> を直接JSON文字列に変換して渡す
        Map<Integer, EventRepeatRuleBean> rulesByRuleIdMap = new HashMap<>();
        for(EventRepeatRuleBean rule : allRules) {
            rulesByRuleIdMap.put(rule.getRuleId(), rule);
        }
        String rulesByRuleIdJson = GSON.toJson(rulesByRuleIdMap); // MapをJSON文字列に変換
        request.setAttribute("rulesByRuleIdJson", rulesByRuleIdJson); // 新しい属性名でJSON文字列を渡す


        // メッセージの引き渡し
        String message = (String) request.getAttribute("message");
        Boolean success = (Boolean) request.getAttribute("success");
        if (message != null) request.setAttribute("message", message);
        if (success != null) request.setAttribute("success", success);

        // カレンダー・イベント管理画面にフォワード
        RequestDispatcher dispatcher = request.getRequestDispatcher("/web/calendar_manage.jsp");
        dispatcher.forward(request, response);
    }

    /**
     * POSTリクエストの処理メソッド。
     * カレンダー上およびサイドパネルからのイベント追加・編集・削除を処理する。
     * @param request HTTPリクエストオブジェクト
     * @param response HTTPレスポンスオブジェクト
     * @throws ServletException サーブレット例外
     * @throws IOException 入出力例外
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // セッションチェック
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            response.sendRedirect(request.getContextPath() + "/web/login.jsp");
            return;
        }

        // 管理者権限チェック
        UserBean user = (UserBean) session.getAttribute("user");
        if (user.getRoleId() != 1) {
            response.sendRedirect(request.getContextPath() + "/web/menu.jsp");
            return;
        }

        String action = request.getParameter("action");
        String message = null;
        Boolean success = null;

        try {
            // JSPからの日付はeventDateとoriginalEventDateの2つが来る想定
            String eventDateStr = request.getParameter("eventDate"); // 新規・更新対象の日付
            String originalEventDateStr = request.getParameter("originalEventDate"); // 更新時の元のイベント日付 (主キー)
            LocalDate eventDate = LocalDate.parse(eventDateStr);
            // originalEventDate が渡されない場合は eventDate と同じとみなす
            LocalDate originalEventDate = (originalEventDateStr != null && !originalEventDateStr.isEmpty()) ? LocalDate.parse(originalEventDateStr) : eventDate; 

            String eventName = request.getParameter("eventName");
            // isWorkはラジオボタンから取得
            boolean isWork = Boolean.parseBoolean(request.getParameter("isWork")); 
            
            // 繰り返し設定関連のパラメータ
            String repeatType = request.getParameter("repeatType");
            String repeatIntervalStr = request.getParameter("repeatInterval");
            String[] repeatDaysOfWeekArr = request.getParameterValues("repeatDaysOfWeek"); // 複数選択の可能性あり
            String repeatEndDateStr = request.getParameter("repeatEndDate");
            Integer repeatRuleId = (request.getParameter("repeatRuleId") != null && !request.getParameter("repeatRuleId").isEmpty()) ? Integer.parseInt(request.getParameter("repeatRuleId")) : null;

            switch (action) {
                case "add":
                    // 入力チェック (最低限)
                    if (eventDateStr == null || eventDateStr.trim().isEmpty() ||
                        eventName == null || eventName.trim().isEmpty() ||
                        request.getParameter("isWork") == null || request.getParameter("isWork").trim().isEmpty()) {
                        message = "日付、イベント名、種別は必須です";
                        success = false;
                        break;
                    }
                    // 主キー重複チェック
                    if (calendarEventDao.exists(eventDate)) { 
                        message = "指定された日付(" + eventDateStr + ")は既にイベントが登録されています";
                        success = false;
                        break;
                    }

                    // CalendarEventBeanを作成し、calendar_eventテーブルに挿入（まずはREPEAT_RULE_IDはnullで）
                    CalendarEventBean addEvent = new CalendarEventBean(eventDate, eventName, isWork);
                    addEvent.setRepeatRuleId(null); 
                    success = calendarEventDao.insert(addEvent);

                    if (success && !repeatType.equals("NONE")) { // 繰り返しイベントの場合
                        EventRepeatRuleBean newRule = createEventRepeatRuleBean(eventDate, repeatType, repeatIntervalStr, repeatDaysOfWeekArr, repeatEndDateStr);
                        boolean ruleInsertSuccess = eventRepeatRuleDao.insert(newRule);
                        if (ruleInsertSuccess) {
                            // 新しく挿入されたルールのIDを取得して、イベントに関連付ける
                            // 最新のルールを取得（INSERT直後なので最大IDのルール）
                            List<EventRepeatRuleBean> allRules = eventRepeatRuleDao.findAll();
                            EventRepeatRuleBean insertedRule = allRules.stream()
                                    .max((r1, r2) -> Integer.compare(r1.getRuleId(), r2.getRuleId()))
                                    .orElse(null);
                            if (insertedRule != null) {
                                addEvent.setRepeatRuleId(insertedRule.getRuleId());
                                calendarEventDao.update(addEvent); // REPEAT_RULE_IDを更新
                            }
                            message = "イベントと繰り返しルールを追加しました";
                        } else {
                            message = "イベントは追加されましたが、繰り返しルールの追加に失敗しました";
                            // TODO: イベントだけ追加された場合のロールバック処理や手動修正の案内 (重要度低)
                        }
                    } else if (success) { // 単発イベントの場合
                        message = "イベントを追加しました";
                    } else { // イベント追加自体が失敗した場合
                        message = "イベントの追加に失敗しました";
                    }
                    break;

                case "update":
                    // 入力チェック (最低限)
                     if (eventDateStr == null || eventDateStr.trim().isEmpty() ||
                        eventName == null || eventName.trim().isEmpty() ||
                        request.getParameter("isWork") == null || request.getParameter("isWork").trim().isEmpty()) {
                        message = "日付、イベント名、種別は必須です";
                        success = false;
                        break;
                    }
                    
                    CalendarEventBean existingEvent = calendarEventDao.findByEventDate(originalEventDate); // 元の主キーで検索
                    if (existingEvent == null) {
                        message = "更新対象のイベントが見つかりません";
                        success = false;
                        break;
                    }
                    
                    // 主キーである日付が変わった場合、DELETE & INSERTで処理
                    if (!eventDate.equals(originalEventDate)) {
                        // 新しい日付で既存イベントがないかチェック
                        if (calendarEventDao.exists(eventDate)) {
                            message = "変更先の日付(" + eventDateStr + ")は既にイベントが登録されています";
                            success = false;
                            break;
                        }
                        
                        // 既存イベントを削除してから新しい日付で挿入
                        success = calendarEventDao.delete(originalEventDate);
                        if (success) {
                            CalendarEventBean newEvent = new CalendarEventBean(eventDate, eventName, isWork);
                            newEvent.setRepeatRuleId(existingEvent.getRepeatRuleId()); // 既存のREPEAT_RULE_IDを引き継ぎ
                            success = calendarEventDao.insert(newEvent);
                            if (!success) {
                                message = "新しい日付でのイベント作成に失敗しました";
                                break;
                            }
                            existingEvent = newEvent; // 以降の処理で使用するため更新
                        } else {
                            message = "元のイベントの削除に失敗しました";
                            break;
                        }
                    } else {
                        // 日付変更がない場合は通常の更新
                        existingEvent.setEventName(eventName);
                        existingEvent.setWork(isWork);
                        success = calendarEventDao.update(existingEvent);
                    }

                    if (success) {
                        EventRepeatRuleBean existingRule = null;
                        if (repeatRuleId != null) { // JSPから既存ルールIDが送られてきた場合
                            existingRule = eventRepeatRuleDao.findByRuleId(repeatRuleId);
                        } else if (existingEvent.getRepeatRuleId() != null) { // DBから取得した既存イベントにルールIDがある場合
                             existingRule = eventRepeatRuleDao.findByRuleId(existingEvent.getRepeatRuleId());
                        }
                        
                        if (!repeatType.equals("NONE")) { // 繰り返しイベントとして更新する場合（既存ルール操作or新規作成）
                            EventRepeatRuleBean ruleToSave = createEventRepeatRuleBean(eventDate, repeatType, repeatIntervalStr, repeatDaysOfWeekArr, repeatEndDateStr);
                            
                            boolean ruleOpSuccess;
                            if (existingRule != null) { // 既存ルールがあれば更新
                                ruleToSave.setRuleId(existingRule.getRuleId()); // 既存のRULE_IDをセット
                                ruleOpSuccess = eventRepeatRuleDao.update(ruleToSave);
                            } else { // 既存ルールがなければ新規挿入
                                ruleOpSuccess = eventRepeatRuleDao.insert(ruleToSave);
                                if (ruleOpSuccess) {
                                    // 新しく挿入されたルールのIDを取得してイベントに関連付ける
                                    List<EventRepeatRuleBean> allRules = eventRepeatRuleDao.findAll();
                                    EventRepeatRuleBean insertedRule = allRules.stream()
                                            .max((r1, r2) -> Integer.compare(r1.getRuleId(), r2.getRuleId()))
                                            .orElse(null);
                                    if (insertedRule != null) {
                                        existingEvent.setRepeatRuleId(insertedRule.getRuleId());
                                        calendarEventDao.update(existingEvent); // REPEAT_RULE_IDを更新
                                    }
                                }
                            }

                            if (ruleOpSuccess) {
                                message = "イベントと繰り返しルールを更新しました";
                            } else {
                                message = "イベントは更新されましたが、繰り返しルールの操作に失敗しました";
                            }
                        } else { // 単発イベントとして更新（既存ルールがあれば削除）
                            if (existingRule != null) {
                                eventRepeatRuleDao.delete(existingRule.getRuleId());
                                message = "イベントは更新され、繰り返しルールが削除されました";
                            } else {
                                message = "イベントを更新しました";
                            }
                            // calendar_eventのREPEAT_RULE_IDをNULLに更新
                            existingEvent.setRepeatRuleId(null);
                            calendarEventDao.update(existingEvent); // REPEAT_RULE_IDをNULLに更新
                        }
                    } else { // イベント更新自体が失敗した場合
                        message = "イベントの更新に失敗しました";
                    }
                    break;

                case "delete":
                    String deleteEventDateStr = request.getParameter("eventDate"); // 削除対象の日付
                    if (deleteEventDateStr == null || deleteEventDateStr.trim().isEmpty()) {
                        message = "削除対象のイベント日付が指定されていません";
                        success = false;
                        break;
                    }
                    LocalDate deleteEventDate = LocalDate.parse(deleteEventDateStr);
                    
                    CalendarEventBean eventToDelete = calendarEventDao.findByEventDate(deleteEventDate);
                    if (eventToDelete == null) {
                        message = "削除対象のイベントが見つかりません";
                        success = false;
                        break;
                    }

                    // calendar_eventに紐づく繰り返しルールがあれば先に削除
                    if (eventToDelete.getRepeatRuleId() != null) {
                        eventRepeatRuleDao.delete(eventToDelete.getRepeatRuleId());
                    }
                    
                    success = calendarEventDao.delete(deleteEventDate); // calendar_event を削除

                    message = success ? "イベントを削除しました" : "イベントの削除に失敗しました";
                    break;

                default:
                    message = "不正な操作です";
                    success = false;
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
            message = "入力された数値（繰り返し間隔）が不正です";
            success = false;
        } catch (DateTimeParseException e) {
            e.printStackTrace();
            message = "日付の形式が不正です。YYYY-MM-DD形式で入力してください";
            success = false;
        } catch (Exception e) {
            e.printStackTrace();
            message = "処理中にエラーが発生しました";
            success = false;
        }

        // メッセージをリクエスト属性に設定し、doGetを呼び出して画面を再表示
        request.setAttribute("message", message);
        request.setAttribute("success", success);
        doGet(request, response);
    }

    /**
     * 指定された繰り返しルールと主イベントから、日付範囲内の繰り返しイベントインスタンスを生成し、
     * FullCalendar形式のイベントリストに追加します。
     * （シンプルなロジックのため、複雑な繰り返しルールには対応していません。例えば、毎月第N週目・曜日指定など）
     * @param masterEvent 主となるCalendarEventBean（単発イベントの基本情報）
     * @param rule 繰り返しルール
     * @param fcEvents FullCalendarイベントリストに追加するリスト
     */
    private void generateRepeatingEvents(CalendarEventBean masterEvent, EventRepeatRuleBean rule, List<Map<String, Object>> fcEvents) {
        // 主イベントの日付は既にfcEventsにadd済みなので、次の繰り返し日付から開始
        LocalDate currentDate = calculateNextRepeatDate(masterEvent.getEventDate(), rule, masterEvent.getEventDate());
        // 繰り返し終了日がない場合、デフォルトで主イベントの5年後まで展開
        LocalDate endDate = rule.getRepeatEndDate() != null ? rule.getRepeatEndDate() : LocalDate.of(masterEvent.getEventDate().getYear() + 5, 12, 31); 


        while (currentDate != null && !currentDate.isAfter(endDate)) {
            Map<String, Object> fcEvent = new HashMap<>();
            fcEvent.put("title", masterEvent.getEventName());
            fcEvent.put("start", currentDate.toString());
            fcEvent.put("allDay", true);
            // IDはユニークにするために日付とルールID、展開日を組み合わせる
            fcEvent.put("id", "REPEATING-" + masterEvent.getEventDate().toString() + "-" + rule.getRuleId() + "-" + currentDate.toString()); 
            
            // イベントの種類に応じて色を設定
            String eventColor = getEventColor(masterEvent);
            fcEvent.put("backgroundColor", eventColor);
            fcEvent.put("borderColor", eventColor);
            
            // extendedPropsにisWorkとrepeatRuleIdを含める
            Map<String, Object> extendedProps = new HashMap<>();
            extendedProps.put("isWork", masterEvent.isWork());
            extendedProps.put("repeatRuleId", rule.getRuleId());
            extendedProps.put("isSystemDefined", masterEvent.isSystemDefined());
            fcEvent.put("extendedProps", extendedProps);

            fcEvents.add(fcEvent);

            // 次の繰り返し日付を計算
            currentDate = calculateNextRepeatDate(currentDate, rule, masterEvent.getEventDate());
        }
    }

    /**
     * 現在日付と繰り返しルールに基づいて、次の繰り返しイベントの日付を計算する補助メソッド。
     * generateRepeatingEvents メソッド内で使用。
     * @param currentBaseDate 現在の基準日付（この日付を含まない次の繰り返し日付を計算）
     * @param rule 繰り返しルール
     * @param masterEventDate 主イベントの日付（月・年の繰り返しで使用）
     * @return 次の繰り返し日付。繰り返し終了の場合や不正なタイプの場合はnull。
     */
    private LocalDate calculateNextRepeatDate(LocalDate currentBaseDate, EventRepeatRuleBean rule, LocalDate masterEventDate) {
        LocalDate nextDate = null;
        
        switch (rule.getRepeatType()) {
            case "DAILY":
                nextDate = currentBaseDate.plusDays(rule.getRepeatInterval());
                break;
            case "WEEKLY":
                if (rule.getRepeatDaysOfWeek() != null && !rule.getRepeatDaysOfWeek().isEmpty()) {
                    List<Integer> selectedDays = java.util.Arrays.stream(rule.getRepeatDaysOfWeek().split(","))
                                                .map(Integer::parseInt)
                                                .sorted() // 曜日を昇順にソート
                                                .collect(Collectors.toList());
                    
                    int currentDayOfWeek = currentBaseDate.getDayOfWeek().getValue(); // 1(月) - 7(日)

                    // 今の週の残りの曜日の中から、次の繰り返し曜日を探す
                    for (int dayNum : selectedDays) {
                        if (dayNum > currentDayOfWeek) { // 今の曜日より後の曜日
                            nextDate = currentBaseDate.plusDays(dayNum - currentDayOfWeek);
                            break;
                        }
                    }
                    if (nextDate == null) { // 今の週に次の曜日がない場合、次の繰り返し間隔の最初の選択曜日へ
                        nextDate = currentBaseDate.plusWeeks(rule.getRepeatInterval()).with(DayOfWeek.of(selectedDays.get(0)));
                    }

                } else { // 繰り返し曜日が指定されていない場合（毎週同じ日付）
                    nextDate = currentBaseDate.plusWeeks(rule.getRepeatInterval());
                }
                break;
            case "MONTHLY_DAY":
                LocalDate targetDayOfMonthDate = currentBaseDate.withDayOfMonth(masterEventDate.getDayOfMonth());
                
                // 次の繰り返し日付の計算。目標の日付が現在の月で過ぎていれば、次の月に移る
                if (targetDayOfMonthDate.isAfter(currentBaseDate)) { // 次の繰り返し日が今月にある場合
                    nextDate = targetDayOfMonthDate;
                } else { // 次の繰り返し日が翌月以降にある場合
                    nextDate = currentBaseDate.plusMonths(rule.getRepeatInterval()).withDayOfMonth(masterEventDate.getDayOfMonth());
                    // 月の最終日を超える場合は調整 (例: 1月31日から2月31日へは行けない)
                    if (nextDate.getDayOfMonth() != masterEventDate.getDayOfMonth()) {
                        nextDate = nextDate.withDayOfMonth(nextDate.lengthOfMonth()); // その月の最終日
                    }
                }
                break;
            case "YEARLY":
                LocalDate targetDayOfYearDate = currentBaseDate.withMonth(masterEventDate.getMonthValue()).withDayOfMonth(masterEventDate.getDayOfMonth());
                
                // 次の繰り返し日付の計算。目標の日付が現在の年で過ぎていれば、次の年に移る
                if (targetDayOfYearDate.isAfter(currentBaseDate)) { // 次の繰り返し日が今年にある場合
                    nextDate = targetDayOfYearDate;
                } else { // 次の繰り返し日が翌年以降にある場合
                    nextDate = currentBaseDate.plusYears(rule.getRepeatInterval()).withMonth(masterEventDate.getMonthValue()).withDayOfMonth(masterEventDate.getDayOfMonth());
                    // 閏年などの日付調整
                    if (nextDate.getDayOfMonth() != masterEventDate.getDayOfMonth()) {
                         nextDate = nextDate.withDayOfMonth(nextDate.lengthOfMonth());
                    }
                }
                break;
            case "NONE":
                return null; // 単発イベントなので次はない
            default:
                return null; // サポートされていない繰り返しタイプ
        }
        return nextDate;
    }

    /**
     * イベント繰り返しルールBeanをHttpServletRequestから作成する補助メソッド。
     */
    private EventRepeatRuleBean createEventRepeatRuleBean(LocalDate eventDateFk, String repeatType, String repeatIntervalStr, String[] repeatDaysOfWeekArr, String repeatEndDateStr) {
        EventRepeatRuleBean rule = new EventRepeatRuleBean();
        rule.setRepeatType(repeatType);
        rule.setRepeatInterval(repeatIntervalStr != null && !repeatIntervalStr.trim().isEmpty() ? Integer.parseInt(repeatIntervalStr) : 1);
        
        if (repeatDaysOfWeekArr != null && repeatDaysOfWeekArr.length > 0) {
            rule.setRepeatDaysOfWeek(String.join(",", repeatDaysOfWeekArr));
        } else {
            rule.setRepeatDaysOfWeek(null);
        }

        if (repeatEndDateStr != null && !repeatEndDateStr.trim().isEmpty()) {
            rule.setRepeatEndDate(LocalDate.parse(repeatEndDateStr));
        } else {
            rule.setRepeatEndDate(null);
        }
        return rule;
    }

    /**
     * イベントの種類に応じて色を決定する補助メソッド
     * @param event カレンダーイベント
     * @return 色コード文字列
     */
    private String getEventColor(CalendarEventBean event) {
//        if (event.isSystemDefined()) {
            // システム定義のイベント
        if (isJapaneseHoliday(event.getEventName())) {
            return "#ff9900"; // 祝日はオレンジ
            
        } else {
            // ユーザー定義のイベント
            if (event.isWork()) {
                return "#007bff"; // 出勤日は青
            } else {
                return "#ff4444"; // ユーザー休日は赤
            }
        }
    }

    /**
     * イベント名から日本の祝日かどうかを判定する補助メソッド
     * @param eventName イベント名
     * @return 祝日の場合true
     */
    private boolean isJapaneseHoliday(String eventName) {
        // 日本の祝日名のリスト
        String[] holidays = {
            "元日", "成人の日", "建国記念の日", "天皇誕生日", "春分の日", "昭和の日",
            "憲法記念日", "みどりの日", "こどもの日", "海の日", "山の日", "敬老の日",
            "秋分の日", "スポーツの日", "文化の日", "勤労感謝の日","振替休日",
        };
        
        for (String holiday : holidays) {
            if (holiday.equals(eventName)) {
                return true;
            }
        }
        return false;
    }

    // FullCalendarがLocalDateをJSONに変換するためのTypeAdapter
    private static class LocalDateAdapter implements com.google.gson.JsonSerializer<LocalDate>, com.google.gson.JsonDeserializer<LocalDate> {
        private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

        @Override
        public com.google.gson.JsonElement serialize(LocalDate src, java.lang.reflect.Type typeOfSrc, com.google.gson.JsonSerializationContext context) {
            return new com.google.gson.JsonPrimitive(FORMATTER.format(src));
        }

        @Override
        public LocalDate deserialize(com.google.gson.JsonElement json, java.lang.reflect.Type typeOfT, com.google.gson.JsonDeserializationContext context) throws com.google.gson.JsonParseException {
            return LocalDate.parse(json.getAsString(), FORMATTER);
        }
    }

    // LocalDateTimeをJSONに変換するためのTypeAdapter
    private static class LocalDateTimeAdapter implements com.google.gson.JsonSerializer<LocalDateTime>, com.google.gson.JsonDeserializer<LocalDateTime> {
        private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

        @Override
        public com.google.gson.JsonElement serialize(LocalDateTime src, java.lang.reflect.Type typeOfSrc, com.google.gson.JsonSerializationContext context) {
            return src != null ? new com.google.gson.JsonPrimitive(FORMATTER.format(src)) : null;
        }

        @Override
        public LocalDateTime deserialize(com.google.gson.JsonElement json, java.lang.reflect.Type typeOfT, com.google.gson.JsonDeserializationContext context) throws com.google.gson.JsonParseException {
            return json != null && !json.isJsonNull() ? LocalDateTime.parse(json.getAsString(), FORMATTER) : null;
        }
    }
}
