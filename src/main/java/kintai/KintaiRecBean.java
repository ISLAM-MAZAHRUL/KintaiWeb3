package kintai;

import java.io.Serializable;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 勤怠記録表示画面で使用するデータを保持するJavaBean。
 * kintai、emp、dept、post、breakテーブルの情報を集約し、
 * 計算済みの休憩時間合計や実働時間も保持します。
 */
public class KintaiRecBean implements Serializable {
    private static final long serialVersionUID = 1L;

    // --- kintaiテーブルからの情報 ---
    private int kintaiRecId;     // 勤怠記録ID
    private LocalDate kintaiDate;  // 勤怠日付
    private String empno;        // 従業員番号
    private Time clockIn;        // 出勤時刻
    private Time clockOut;       // 退勤時刻

    // --- emp、dept、postテーブルからの情報（表示用）---
    private String empName;      // 従業員名
    private String deptNo;       // 部署番号
    private String deptName;     // 部署名
    private String postNo;       // 役職番号
    private String postName;     // 役職名

    // --- 計算済みの集計情報 ---
    private long totalBreakMinutes; // 総休憩時間（分単位）
    private long actualWorkMinutes; // 実働時間（分単位）
    private long overtimeMinutes;   // 残業時間（分単位）
    private long nightovertimeMinutes; // 深夜残業時間(分単位)
    
    // ★ 勤怠区分を追加
    private String attendanceType; // 出勤 / 有給 / 無給 / 欠勤
    private String attendanceStatus;
    
    private double totalWorkingHours;
    private double totalOvertimeHours;
    private double totalNightHours;
    private double totalBreakHours;
    private String projectId;
    /**
     * デフォルトコンストラクタ
     */
    public KintaiRecBean() {
    }


    // --- アクセサメソッド (getter/setter) ---
    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }
    public String getAttendanceStatus() {
    	     return attendanceStatus;
    }
    public void setAttendanceStatus(String attendanceStatus) {
    	   this.attendanceStatus=attendanceStatus;
    }
    public double getTotalWorkingHours() {
        return totalWorkingHours;
    }

    public void setTotalWorkingHours(double totalWorkingHours) {
        this.totalWorkingHours = totalWorkingHours;
    }

    public double getTotalOvertimeHours() {
        return totalOvertimeHours;
    }

    public void setTotalOvertimeHours(double totalOvertimeHours) {
        this.totalOvertimeHours = totalOvertimeHours;
    }

    public double getTotalNightHours() {
        return totalNightHours;
    }

    public void setTotalNightHours(double totalNightHours) {
        this.totalNightHours = totalNightHours;
    }

    public double getTotalBreakHours() {
        return totalBreakHours;
    }

    public void setTotalBreakHours(double totalBreakHours) {
        this.totalBreakHours = totalBreakHours;
    }

    public int getKintaiRecId() {
        return kintaiRecId;
    }

    public void setKintaiRecId(int kintaiRecId) {
        this.kintaiRecId = kintaiRecId;
    }

    public int getRecId() {
        return kintaiRecId;
    }

    public void setRecId(int recId) {
        this.kintaiRecId = recId;
    }

    public LocalDate getKintaiDate() {
        return kintaiDate;
    }

    public void setKintaiDate(LocalDate kintaiDate) {
        this.kintaiDate = kintaiDate;
    }

    public String getEmpno() {
        return empno;
    }

    public void setEmpno(String empno) {
        this.empno = empno;
    }

    public String getEmpId() {
        return empno;
    }

    public void setEmpId(String empId) {
        this.empno = empId;
    }

    public Time getClockIn() {
        return clockIn;
    }

    public void setClockIn(Time clockIn) {
        this.clockIn = clockIn;
    }

    public Time getClockOut() {
        return clockOut;
    }

    public void setClockOut(Time clockOut) {
        this.clockOut = clockOut;
    }

    public String getEmpName() {
        return empName;
    }

    public void setEmpName(String empName) {
        this.empName = empName;
    }

    public String getDeptNo() {
        return deptNo;
    }

    public void setDeptNo(String deptNo) {
        this.deptNo = deptNo;
    }

    public String getDeptName() {
        return deptName;
    }

    public void setDeptName(String deptName) {
        this.deptName = deptName;
    }

    public String getPostNo() {
        return postNo;
    }

    public void setPostNo(String postNo) {
        this.postNo = postNo;
    }

    public String getPostName() {
        return postName;
    }

    public void setPostName(String postName) {
        this.postName = postName;
    }

    public long getTotalBreakMinutes() {
        return totalBreakMinutes;
    }

    public void setTotalBreakMinutes(long totalBreakMinutes) {
        this.totalBreakMinutes = totalBreakMinutes;
    }

    public long getActualWorkMinutes() {
        return actualWorkMinutes;
    }

    public void setActualWorkMinutes(long actualWorkMinutes) {
        this.actualWorkMinutes = actualWorkMinutes;
    }

    public String getTotalBreakTimeFormatted() {
        return formatMinutesToHHMM(totalBreakMinutes);
    }

    public String getActualWorkTimeFormatted() {
        return formatMinutesToHHMM(actualWorkMinutes);
    }

    public long getOvertimeMinutes() {
        return overtimeMinutes;
    }

    public void setOvertimeMinutes(long overtimeMinutes) {
        this.overtimeMinutes = overtimeMinutes;
    }

    public long getNightovertimeMinutes() {
        return nightovertimeMinutes;
    }

    public void setNightovertimeMinutes(long nightovertimeMinutes) {
        this.nightovertimeMinutes = nightovertimeMinutes;
    }

    public String getOvertimeFormatted() {
        return formatMinutesToHHMM(overtimeMinutes);
    }

    public String getNightovertimeFormatted() {
        return formatMinutesToHHMM(nightovertimeMinutes);
    }

    private String formatMinutesToHHMM(long minutes) {
        long hours = minutes / 60;
        long remainingMinutes = minutes % 60;
        return String.format("%02d:%02d", hours, remainingMinutes);
    }
    
    // ★ 勤怠区分 getter/setter
    public String getAttendanceType() { return attendanceType; }
    public void setAttendanceType(String attendanceType) { this.attendanceType = attendanceType; }

    // --- 休憩情報 ---
    private List<BreakBean> breaks = new ArrayList<>();

    public List<BreakBean> getBreaks() {
        return breaks;
    }
    public void setBreaks(List<BreakBean> breaks) {
        this.breaks = breaks;
    }

    // --- JSON文字列組み立て ---
    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"kintaiRecId\":").append(kintaiRecId).append(",");
        sb.append("\"kintaiDate\":\"").append(kintaiDate != null ? kintaiDate.toString() : "").append("\",");
        sb.append("\"empId\":\"").append(empno != null ? empno : "").append("\",");
        sb.append("\"clockIn\":\"").append(clockIn != null ? clockIn.toLocalTime().toString() : "").append("\",");
        sb.append("\"clockOut\":\"").append(clockOut != null ? clockOut.toLocalTime().toString() : "").append("\",");
        sb.append("\"attendanceType\":\"").append(attendanceType != null ? attendanceType : "").append("\",");
        sb.append("\"projectId\":\"").append(projectId != null ? projectId : "").append("\",");
        sb.append("\"totalBreakMinutes\":").append(totalBreakMinutes).append(",");
        sb.append("\"actualWorkMinutes\":").append(actualWorkMinutes).append(",");

        // --- 休憩をJSON配列に追加 ---
        sb.append("\"breaks\":[");
        if (breaks != null && !breaks.isEmpty()) {
            for (int i = 0; i < breaks.size(); i++) {
                BreakBean b = breaks.get(i);
                sb.append("{\"start\":\"")
                  .append(b.getBreakStart() != null ? b.getBreakStart().toLocalTime().toString().substring(0,5) : "")
                  .append("\",\"end\":\"")
                  .append(b.getBreakEnd() != null ? b.getBreakEnd().toLocalTime().toString().substring(0,5) : "")
                  .append("\"}");
                if (i < breaks.size() - 1) sb.append(",");
            }
        }
        sb.append("]");

        sb.append("}");
        return sb.toString();
    }


    // 簡易JSON文字列からBeanに変換
    public static KintaiRecBean fromJson(String json) {
        KintaiRecBean bean = new KintaiRecBean();
        try {
            json = json.replaceAll("[{}\"]", ""); // { } と " を削除
            String[] pairs = json.split(",");
            for (String pair : pairs) {
                String[] kv = pair.split(":", 2);
                if (kv.length != 2) continue;
                String key = kv[0].trim();
                String value = kv[1].trim();
                switch (key) {
                    case "kintaiRecId": bean.setKintaiRecId(Integer.parseInt(value)); break;
                    case "kintaiDate": bean.setKintaiDate(value.isEmpty() ? null : LocalDate.parse(value)); break;
                    case "empId": bean.setEmpId(value); break;
                    case "clockIn": bean.setClockIn(value.isEmpty() ? null : Time.valueOf(LocalTime.parse(value))); break;
                    case "clockOut": bean.setClockOut(value.isEmpty() ? null : Time.valueOf(LocalTime.parse(value))); break;
                    case "empName": bean.setEmpName(value); break;
                    case "deptNo": bean.setDeptNo(value); break;
                    case "deptName": bean.setDeptName(value); break;
                    case "postNo": bean.setPostNo(value); break;
                    case "postName": bean.setPostName(value); break;
                    case "totalBreakMinutes": bean.setTotalBreakMinutes(Long.parseLong(value)); break;
                    case "actualWorkMinutes": bean.setActualWorkMinutes(Long.parseLong(value)); break;
                    case "overtimeMinutes": bean.setOvertimeMinutes(Long.parseLong(value)); break;
                    case "nightovertimeMinutes": bean.setNightovertimeMinutes(Long.parseLong(value)); break;
                    case "attendanceType": bean.setAttendanceType(value); break; // ★追加
                    case "projectId": bean.setProjectId(value); break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            // エラー時は空のBeanを返す
        }
        return bean;
    }



}