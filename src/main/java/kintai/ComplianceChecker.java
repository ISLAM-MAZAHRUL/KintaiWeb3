package kintai;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 法令遵守チェックと会社規則チェック機能
 * 労働基準法および会社規則に基づく勤怠管理の合規性をチェックする
 */
public class ComplianceChecker {
    
    // 更新されたコンプライアンスチェック基準値
    private static final int CONTINUOUS_WORK_LIMIT = 10;        // 連続勤務日数上限（新規則：10日）
    private static final int TWO_WEEK_OVERTIME_LIMIT = 40;      // 2週間時間外労働上限（80時間超過禁止のため）
    
    // 会社規則の基準値（第15条に基づく）
    private static final LocalTime COMPANY_START_TIME = LocalTime.of(9, 0);     // 始業時刻 午前9時00分
    private static final LocalTime COMPANY_END_TIME = LocalTime.of(18, 0);      // 終業時刻 午後6時00分
    private static final LocalTime COMPANY_LUNCH_START = LocalTime.of(12, 0);   // 休憩開始 正午
    private static final LocalTime COMPANY_LUNCH_END = LocalTime.of(13, 0);     // 休憩終了 午後1時
    private static final int COMPANY_LUNCH_BREAK_MINUTES = 60;                  // 昼休憩時間（分）
//    private static final int COMPANY_MAX_LATE_MINUTES = 0;                      // 遅刻許容時間（分）規程上は厳格
    private static final int COMPANY_STANDARD_WORK_HOURS = 8;                   // 1日の標準労働時間
    
    // 休憩時間チェックの新基準
    private static final int MIN_BREAK_FOR_6_8_HOURS = 45;                     // 6-8時間勤務時の最低休憩時間（分）
    private static final int MIN_BREAK_FOR_OVER_8_HOURS = 60;                  // 8時間超勤務時の最低休憩時間（分）
    
    /**
     * 法令遵守チェックの実行
     */
    public ComplianceCheckResult performLegalComplianceCheck(List<KintaiRecBean> records, String empno) {
        ComplianceCheckResult result = new ComplianceCheckResult();
        result.setEmpno(empno);
        result.setCheckDate(LocalDate.now());
        result.setCheckType("法令遵守チェック");
        
        List<ComplianceViolation> violations = new ArrayList<>();
        
        // 1. 休憩時間チェック（新基準）
        violations.addAll(checkBreakTimeNew(records));
        
        // 2. 深夜勤務チェック（22時〜5時）
        violations.addAll(checkNightWork(records));
        
        // 3. 連続勤務日数チェック（10日超過）
        violations.addAll(checkContinuousWorkDays(records));
        
        // 4. 2週間内の80時間超過チェック
        violations.addAll(checkTwoWeekOvertime(records));
        
        result.setViolations(violations);
        result.setTotalViolations(violations.size());
        result.setComplianceScore(calculateComplianceScore(violations, records.size()));
        
        return result;
    }
    
    /**
     * 会社規則チェックの実行
     */
    public ComplianceCheckResult performCompanyRulesCheck(List<KintaiRecBean> records, String empno) {
        ComplianceCheckResult result = new ComplianceCheckResult();
        result.setEmpno(empno);
        result.setCheckDate(LocalDate.now());
        result.setCheckType("会社規則チェック");
        
        List<ComplianceViolation> violations = new ArrayList<>();
        
//        // 1. 遅刻チェック（残す）
//        violations.addAll(checkLateness(records));
        
        // 2. 会社規程遵守チェック（始業9:00、終業18:00、休憩12:00-13:00）- 遅刻以外は不要のためコメントアウト
        // violations.addAll(checkCompanyRules(records));
        
        result.setViolations(violations);
        result.setTotalViolations(violations.size());
        result.setComplianceScore(calculateComplianceScore(violations, records.size()));
        
        return result;
    }
    
    /*
     * 日次労働時間チェック（法定8時間超過）- 不要のためコメントアウト
     */
    /*
    private List<ComplianceViolation> checkDailyWorkHours(List<KintaiRecBean> records) {
        return records.stream()
            .filter(record -> record.getActualWorkMinutes() > LEGAL_DAILY_WORK_HOURS * 60)
            .map(record -> {
                ComplianceViolation violation = new ComplianceViolation();
                violation.setViolationType("法定労働時間超過");
                violation.setDate(record.getKintaiDate());
                violation.setSeverity("高");
                violation.setDescription(String.format("実働時間%s（%d分）が法定労働時間8時間（480分）を超過しています", 
                    record.getActualWorkTimeFormatted(), record.getActualWorkMinutes()));
                violation.setLegalBasis("労働基準法第32条");
                return violation;
            })
            .collect(Collectors.toList());
    }
    */
    
    /*
     * 月間残業時間チェック（45時間超過）- 不要のためコメントアウト
     */
    /*
    private List<ComplianceViolation> checkMonthlyOvertime(List<KintaiRecBean> records) {
        List<ComplianceViolation> violations = new ArrayList<>();
        
        long totalOvertimeMinutes = records.stream()
            .mapToLong(KintaiRecBean::getOvertimeMinutes)
            .sum();
        
        double totalOvertimeHours = totalOvertimeMinutes / 60.0;
        
        if (totalOvertimeHours > LEGAL_MONTHLY_OVERTIME_LIMIT) {
            ComplianceViolation violation = new ComplianceViolation();
            violation.setViolationType("月間残業時間超過");
            violation.setDate(LocalDate.now());
            violation.setSeverity("高");
            violation.setDescription(String.format("月間残業時間%.1f時間が法定上限45時間を超過しています", 
                totalOvertimeHours));
            violation.setLegalBasis("労働基準法第36条");
            violations.add(violation);
        }
        
        return violations;
    }
    */
    
    /**
     * 連続勤務日数チェック（10日超過）
     */
    private List<ComplianceViolation> checkContinuousWorkDays(List<KintaiRecBean> records) {
        List<ComplianceViolation> violations = new ArrayList<>();
        
        int continuousWorkDays = 0;
        LocalDate lastWorkDate = null;
        
        for (KintaiRecBean record : records) {
            if (record.getClockIn() != null) {
                if (lastWorkDate != null && record.getKintaiDate().equals(lastWorkDate.plusDays(1))) {
                    continuousWorkDays++;
                } else {
                    continuousWorkDays = 1;
                }
                
                if (continuousWorkDays > CONTINUOUS_WORK_LIMIT) {
                    ComplianceViolation violation = new ComplianceViolation();
                    violation.setViolationType("連続勤務日数超過");
                    violation.setDate(record.getKintaiDate());
                    violation.setSeverity("中");
                    violation.setDescription(String.format("連続勤務%d日が上限10日を超過しています", 
                        continuousWorkDays));
                    violation.setLegalBasis("労働基準法第35条");
                    violations.add(violation);
                }
                
                lastWorkDate = record.getKintaiDate();
            } else {
                continuousWorkDays = 0;
                lastWorkDate = null;
            }
        }
        
        return violations;
    }
    
    /*
     * 休憩時間チェック（6時間以上勤務で休憩なし）- 新基準のcheckBreakTimeNewに置き換え
     */
    /*
    private List<ComplianceViolation> checkBreakTime(List<KintaiRecBean> records) {
        return records.stream()
            .filter(record -> record.getActualWorkMinutes() > 6 * 60 && 
                             record.getTotalBreakMinutes() < 45)
            .map(record -> {
                ComplianceViolation violation = new ComplianceViolation();
                violation.setViolationType("休憩時間不足");
                violation.setDate(record.getKintaiDate());
                violation.setSeverity("中");
                violation.setDescription(String.format("6時間以上勤務に対し休憩時間%sが不足しています", 
                    record.getTotalBreakTimeFormatted()));
                violation.setLegalBasis("労働基準法第34条");
                return violation;
            })
            .collect(Collectors.toList());
    }
    */
    
    /**
     * 深夜勤務チェック（22時〜5時）
     */
    private List<ComplianceViolation> checkNightWork(List<KintaiRecBean> records) {
        return records.stream()
            .filter(record -> {
                if (record.getClockOut() == null) return false;
                LocalTime clockOut = record.getClockOut().toLocalTime();
                return clockOut.isAfter(LocalTime.of(22, 0)) || clockOut.isBefore(LocalTime.of(5, 0));
            })
            .map(record -> {
                ComplianceViolation violation = new ComplianceViolation();
                violation.setViolationType("深夜勤務");
                violation.setDate(record.getKintaiDate());
                violation.setSeverity("中");
                violation.setDescription(String.format("深夜時間帯（22:00〜5:00）での勤務が確認されました（退勤: %s）", 
                    record.getClockOut().toLocalTime()));
                violation.setLegalBasis("労働基準法第37条");
                return violation;
            })
            .collect(Collectors.toList());
    }
    
//    /**
//     * 遅刻チェック
//     */
//    private List<ComplianceViolation> checkLateness(List<KintaiRecBean> records) {
//        return records.stream()
//            .filter(record -> {
//                if (record.getClockIn() == null) return false;
//                LocalTime clockIn = record.getClockIn().toLocalTime();
//                return clockIn.isAfter(COMPANY_START_TIME);
//            })
//            .map(record -> {
//                ComplianceViolation violation = new ComplianceViolation();
//                violation.setViolationType("遅刻");
//                violation.setDate(record.getKintaiDate());
//                
//                LocalTime clockIn = record.getClockIn().toLocalTime();
//                long lateMinutes = java.time.Duration.between(COMPANY_START_TIME, clockIn).toMinutes();
//                
//                violation.setSeverity(lateMinutes > COMPANY_MAX_LATE_MINUTES ? "高" : "低");
//                violation.setDescription(String.format("出勤時刻%s（%d分遅刻）が標準時刻9:00を超過しています", 
//                    clockIn.toString().substring(0, 5), lateMinutes));
//                violation.setLegalBasis("就業規則第○条");
//                return violation;
//            })
//            .collect(Collectors.toList());
//    }
    
    /*
     * 早退チェック - 不要のためコメントアウト
     */
    /*
    private List<ComplianceViolation> checkEarlyLeaving(List<KintaiRecBean> records) {
        return records.stream()
            .filter(record -> {
                if (record.getClockOut() == null) return false;
                LocalTime clockOut = record.getClockOut().toLocalTime();
                return clockOut.isBefore(COMPANY_END_TIME);
            })
            .map(record -> {
                ComplianceViolation violation = new ComplianceViolation();
                violation.setViolationType("早退");
                violation.setDate(record.getKintaiDate());
                violation.setSeverity("中");
                
                LocalTime clockOut = record.getClockOut().toLocalTime();
                long earlyMinutes = java.time.Duration.between(clockOut, COMPANY_END_TIME).toMinutes();
                
                violation.setDescription(String.format("退勤時刻%s（%d分早退）が標準時刻18:00を下回っています", 
                    clockOut.toString().substring(0, 5), earlyMinutes));
                violation.setLegalBasis("就業規則第○条");
                return violation;
            })
            .collect(Collectors.toList());
    }
    */
    
    /*
     * 欠勤チェック - 不要のためコメントアウト
     */
    /*
    private List<ComplianceViolation> checkAbsence(List<KintaiRecBean> records) {
        List<ComplianceViolation> violations = new ArrayList<>();
        
        long absentDays = records.stream()
            .filter(record -> record.getClockIn() == null && record.getClockOut() == null)
            .filter(record -> record.getKintaiDate().getDayOfWeek() != DayOfWeek.SATURDAY && 
                             record.getKintaiDate().getDayOfWeek() != DayOfWeek.SUNDAY)
            .count();
        
        if (absentDays > COMPANY_MAX_MONTHLY_ABSENT_DAYS) {
            ComplianceViolation violation = new ComplianceViolation();
            violation.setViolationType("月間欠勤日数超過");
            violation.setDate(LocalDate.now());
            violation.setSeverity("高");
            violation.setDescription(String.format("月間欠勤日数%d日が許容上限%d日を超過しています", 
                absentDays, COMPANY_MAX_MONTHLY_ABSENT_DAYS));
            violation.setLegalBasis("就業規則第○条");
            violations.add(violation);
        }
        
        return violations;
    }
    */
    
    /*
     * 適切な休憩時間チェック - 不要のためコメントアウト
     */
    /*
    private List<ComplianceViolation> checkProperBreakTime(List<KintaiRecBean> records) {
        return records.stream()
            .filter(record -> record.getActualWorkMinutes() > 6 * 60)
            .filter(record -> Math.abs(record.getTotalBreakMinutes() - COMPANY_LUNCH_BREAK_MINUTES) > 15)
            .map(record -> {
                ComplianceViolation violation = new ComplianceViolation();
                violation.setViolationType("休憩時間異常");
                violation.setDate(record.getKintaiDate());
                violation.setSeverity("低");
                violation.setDescription(String.format("休憩時間%sが標準時間60分と大きく異なります", 
                    record.getTotalBreakTimeFormatted()));
                violation.setLegalBasis("就業規則第○条");
                return violation;
            })
            .collect(Collectors.toList());
    }
    */
    
    /*
     * 勤務態度チェック（頻繁な遅刻・早退） - 不要のためコメントアウト
     */
    /*
    private List<ComplianceViolation> checkWorkAttitude(List<KintaiRecBean> records) {
        List<ComplianceViolation> violations = new ArrayList<>();
        
        long lateCount = records.stream()
            .filter(record -> record.getClockIn() != null)
            .filter(record -> record.getClockIn().toLocalTime().isAfter(COMPANY_START_TIME))
            .count();
        
        long earlyLeaveCount = records.stream()
            .filter(record -> record.getClockOut() != null)
            .filter(record -> record.getClockOut().toLocalTime().isBefore(COMPANY_END_TIME))
            .count();
        
        if (lateCount >= 5) {
            ComplianceViolation violation = new ComplianceViolation();
            violation.setViolationType("頻繁な遅刻");
            violation.setDate(LocalDate.now());
            violation.setSeverity("中");
            violation.setDescription(String.format("月間遅刻回数%d回が多発しています", lateCount));
            violation.setLegalBasis("就業規則第○条");
            violations.add(violation);
        }
        
        if (earlyLeaveCount >= 3) {
            ComplianceViolation violation = new ComplianceViolation();
            violation.setViolationType("頻繁な早退");
            violation.setDate(LocalDate.now());
            violation.setSeverity("中");
            violation.setDescription(String.format("月間早退回数%d回が多発しています", earlyLeaveCount));
            violation.setLegalBasis("就業規則第○条");
            violations.add(violation);
        }
        
        return violations;
    }
    */
    
    /*
     * 休日勤務チェック（第16条に基づく：土曜日、日曜日、国民の祝日） - 不要のためコメントアウト
     */
    /*
    private List<ComplianceViolation> checkHolidayWork(List<KintaiRecBean> records) {
        return records.stream()
            .filter(record -> {
                // 土曜日または日曜日の勤務をチェック
                DayOfWeek dayOfWeek = record.getKintaiDate().getDayOfWeek();
                return (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) 
                       && (record.getClockIn() != null || record.getClockOut() != null);
            })
            .map(record -> {
                ComplianceViolation violation = new ComplianceViolation();
                violation.setViolationType("休日勤務");
                violation.setDate(record.getKintaiDate());
                violation.setSeverity("中");
                
                String dayName = record.getKintaiDate().getDayOfWeek() == DayOfWeek.SATURDAY ? "土曜日" : "日曜日";
                violation.setDescription(String.format("所定休日（%s）に勤務が確認されました", dayName));
                violation.setLegalBasis("就業規則第16条");
                return violation;
            })
            .collect(Collectors.toList());
    }
    */
    
    /**
     * 合規スコアの計算
     */
    private double calculateComplianceScore(List<ComplianceViolation> violations, int totalRecords) {
        if (totalRecords == 0) return 100.0;
        
        double penaltyScore = violations.stream()
            .mapToDouble(violation -> {
                switch (violation.getSeverity()) {
                    case "高": return 5.0;
                    case "中": return 3.0;
                    case "低": return 1.0;
                    default: return 1.0;
                }
            })
            .sum();
        
        double maxPossibleScore = totalRecords * 5.0;
        double score = Math.max(0, 100.0 - (penaltyScore / maxPossibleScore * 100));
        
        return Math.round(score * 10.0) / 10.0;
    }
    
    /**
     * 統合的な合規チェック（法令遵守 + 会社規則）
     */
    public ComplianceCheckResult performComprehensiveCheck(List<KintaiRecBean> records, String empno) {
        ComplianceCheckResult legalResult = performLegalComplianceCheck(records, empno);
        ComplianceCheckResult companyResult = performCompanyRulesCheck(records, empno);
        
        ComplianceCheckResult comprehensiveResult = new ComplianceCheckResult();
        comprehensiveResult.setEmpno(empno);
        comprehensiveResult.setCheckDate(LocalDate.now());
        comprehensiveResult.setCheckType("総合合規チェック");
        
        List<ComplianceViolation> allViolations = new ArrayList<>();
        allViolations.addAll(legalResult.getViolations());
        allViolations.addAll(companyResult.getViolations());
        
        comprehensiveResult.setViolations(allViolations);
        comprehensiveResult.setTotalViolations(allViolations.size());
        
        double averageScore = (legalResult.getComplianceScore() + companyResult.getComplianceScore()) / 2.0;
        comprehensiveResult.setComplianceScore(averageScore);
        
        return comprehensiveResult;
    }
    
    /**
     * 新しい休憩時間チェック（6-8時間勤務で45分以上、8時間超勤務で60分以上）
     */
    private List<ComplianceViolation> checkBreakTimeNew(List<KintaiRecBean> records) {
        return records.stream()
            .filter(record -> {
                long workMinutes = record.getActualWorkMinutes();
                long breakMinutes = record.getTotalBreakMinutes();
                
                // 6-8時間勤務で45分未満の休憩
                if (workMinutes >= 6 * 60 && workMinutes <= 8 * 60 && breakMinutes < MIN_BREAK_FOR_6_8_HOURS) {
                    return true;
                }
                // 8時間超勤務で60分未満の休憩
                if (workMinutes > 8 * 60 && breakMinutes < MIN_BREAK_FOR_OVER_8_HOURS) {
                    return true;
                }
                return false;
            })
            .map(record -> {
                ComplianceViolation violation = new ComplianceViolation();
                violation.setViolationType("休憩時間不足");
                violation.setDate(record.getKintaiDate());
                violation.setSeverity("高");
                
                long workMinutes = record.getActualWorkMinutes();
                long breakMinutes = record.getTotalBreakMinutes();
                String workHours = String.format("%.1f", workMinutes / 60.0);
                String breakTime = record.getTotalBreakTimeFormatted();
                
                if (workMinutes <= 8 * 60) {
                    violation.setDescription(String.format("6-8時間勤務（%s時間）に対し休憩時間%sが45分未満です", 
                        workHours, breakTime));
                } else {
                    violation.setDescription(String.format("8時間超勤務（%s時間）に対し休憩時間%sが60分未満です", 
                        workHours, breakTime));
                }
                violation.setLegalBasis("労働基準法第34条");
                return violation;
            })
            .collect(Collectors.toList());
    }
    
    /**
     * 2週間内の80時間超過チェック（2週間で40時間超過時アラート）
     */
    private List<ComplianceViolation> checkTwoWeekOvertime(List<KintaiRecBean> records) {
        List<ComplianceViolation> violations = new ArrayList<>();
        
        // レコードを日付順にソート
        List<KintaiRecBean> sortedRecords = records.stream()
            .sorted((r1, r2) -> r1.getKintaiDate().compareTo(r2.getKintaiDate()))
            .collect(Collectors.toList());
        
        // 14日間のスライディングウィンドウで残業時間をチェック
        for (int i = 0; i <= sortedRecords.size() - 14; i++) {
            long totalOvertimeMinutes = 0;
            LocalDate startDate = sortedRecords.get(i).getKintaiDate();
            LocalDate endDate = sortedRecords.get(i + 13).getKintaiDate();
            
            for (int j = i; j < i + 14 && j < sortedRecords.size(); j++) {
                totalOvertimeMinutes += sortedRecords.get(j).getOvertimeMinutes();
            }
            
            double totalOvertimeHours = totalOvertimeMinutes / 60.0;
            
            if (totalOvertimeHours > TWO_WEEK_OVERTIME_LIMIT) {
                ComplianceViolation violation = new ComplianceViolation();
                violation.setViolationType("2週間時間外労働超過警告");
                violation.setDate(endDate);
                violation.setSeverity("高");
                violation.setDescription(String.format("2週間（%s〜%s）の時間外労働%.1f時間が40時間を超過（80時間超過防止アラート）", 
                    startDate, endDate, totalOvertimeHours));
                violation.setLegalBasis("特別条項付き36協定");
                violations.add(violation);
            }
        }
        
        return violations;
    }
    
    /**
     * 会社規程遵守チェック（始業9:00、終業18:00、休憩12:00-13:00）
     */
    private List<ComplianceViolation> checkCompanyRules(List<KintaiRecBean> records) {
        return records.stream()
            .filter(record -> {
                // 早退時間のみをチェック（遅刻は別のcheckLatenessで処理）
                if (record.getClockOut() != null) {
                    LocalTime clockOut = record.getClockOut().toLocalTime();
                    if (clockOut.isBefore(COMPANY_END_TIME)) {
                        return true;
                    }
                }
                return false;
            })
            .map(record -> {
                ComplianceViolation violation = new ComplianceViolation();
                violation.setViolationType("会社規程不遵守");
                violation.setDate(record.getKintaiDate());
                violation.setSeverity("中");
                
                // 早退のみを記述
                if (record.getClockOut() != null) {
                    LocalTime clockOut = record.getClockOut().toLocalTime();
                    if (clockOut.isBefore(COMPANY_END_TIME)) {
                        long earlyMinutes = java.time.Duration.between(clockOut, COMPANY_END_TIME).toMinutes();
                        violation.setDescription(String.format("退勤時刻%s（%d分早退）が標準時刻18:00を下回っています", 
                            clockOut.toString().substring(0, 5), earlyMinutes));
                    }
                }
                violation.setLegalBasis("就業規則第15条");
                return violation;
            })
            .collect(Collectors.toList());
    }
}