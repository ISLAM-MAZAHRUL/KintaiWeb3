package kintai;

import java.io.Serializable;
import java.time.LocalDate;
// 旧WorkDetailで使用していたTime, LocalTime, Durationはwork_allocで直接時間を入力するため不要になります

/**
 * 勤務時間管理画面（kinmu_manage.jsp）で使用するデータを保持するJavaBean。
 * 特に、工数割り当て（work_allocテーブル）の情報を内部クラスとして定義します。
 */
public class KinmuManageBean implements Serializable {
    private static final long serialVersionUID = 1L;

    // KinmuManageBean自体は、特定の勤怠記録（recId, kintaiDate, empno）
    // とその日の出退勤、休憩時間、工数割り当てのリストを保持するためのコンテナとして使われることが多いです。
    // ここではWorkAlloc内部クラスの定義を主とします。

    /**
     * 工数割り当て（work_allocテーブルのレコード）を保持する内部JavaBean。
     */
    public static class WorkAlloc implements Serializable {
        private static final long serialVersionUID = 1L;

        private int allocationId;   // 割り当てID (ALLOCATION_ID)
        private String empId;       // 従業員番号 (EMP_ID) - 参照用（ただしwork_allocのPKの一部）
        private int projectId;      // プロジェクトID (PROJECT_ID)
        private String projectCode; // プロジェクトコード (PROJECT_CODE)
        private LocalDate workDate; // 作業日 (WORK_DATE)
        private double workHours;   // 作業時間 (WORK_HOURS)

        // 表示用の追加フィールド（JOINで取得）
        private String empName;     // 従業員名
        private String projectName;
        private String yearMonth;// プロジェクト名
        
        private String createdBy;
        private String updatedBy;

        /**
         * デフォルトコンストラクタ
         */
        public WorkAlloc() {
        }

        // --- アクセサメソッド (getter/setter) ---

        public int getAllocationId() {
            return allocationId;
        }

        public void setAllocationId(int allocationId) {
            this.allocationId = allocationId;
        }

        public String getEmpno() {
            return empId;
        }

        public void setEmpno(String empno) {
            this.empId = empno;
        }

        public String getEmpId() {
            return empId;
        }

        public void setEmpId(String empId) {
            this.empId = empId;
        }

        public int getProjectId() {
            return projectId;
        }

        public void setProjectId(int projectId) {
            this.projectId = projectId;
        }

        public String getProjectCode() {
            return projectCode;
        }

        public void setProjectCode(String projectCode) {
            this.projectCode = projectCode;
        }

        public LocalDate getWorkDate() {
            return workDate;
        }

        public void setWorkDate(LocalDate workDate) {
            this.workDate = workDate;
        }

        public double getWorkHours() {
            return workHours;
        }

        public void setWorkHours(double workHours) {
            this.workHours = workHours;
        }

        public String getEmpName() {
            return empName;
        }

        public void setEmpName(String empName) {
            this.empName = empName;
        }

        public String getProjectName() {
            return projectName;
        }

        public void setProjectName(String projectName) {
            this.projectName = projectName;
        }
        
        public String getCreatedBy() { return createdBy; }
        public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
        
        public String getUpdatedBy() { return updatedBy; }
        public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
        
        public String getYearMonth() { return yearMonth; }
        public void setYearMonth(String yearMonth) { this.yearMonth = yearMonth; }

        /**
         * 作業時間をHH.HH形式の文字列で取得します。
         * @return HH.HH形式の作業時間
         */
        public String getWorkHoursFormatted() {
            // DecimalFormatなどを使用する方が厳密だが、ここではString.formatで簡易的に対応
            return String.format("%.2f", workHours);
        }
        
        /**
         * 時間＋分形式 (例: 7:30)
         */
        public String getWorkHoursHHMM() {
            int h = (int) workHours;
            int m = (int) Math.round((workHours - h) * 60);
            return String.format("%d:%02d", h, m);
        }
        
        /**
         * 小数時間（例: 7.5）を HH:MM 形式（例: 7:30）に変換するユーティリティ
         * @param hours 小数時間
         * @return HH:MM 形式の文字列
         */
        public static String formatHoursToHHMM(double hours) {
            int h = (int) hours;
            int m = (int) Math.round((hours - h) * 60);
            return String.format("%d:%02d", h, m);
        }
    }
}
