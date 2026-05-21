package kintai;

import java.io.Serializable;
import java.sql.Time;

/**
 * 休憩情報（breakテーブルのレコード）を保持するJavaBean。
 */
public class BreakBean implements Serializable {
    private static final long serialVersionUID = 1L;

    private int breakId;         // breakテーブルの「BREAK_ID」列に対応
    private int kintaiRecId;     // breakテーブルの「KINTAI_REC_ID」列に対応
    private Time breakStart;     // breakテーブルの「BREAK_START」列に対応
    private Time breakEnd;       // breakテーブルの「BREAK_END」列に対応
    
    private String createdBy;
    private String updatedBy;
    private String empId;

    // --- Getter/Setter ---
    public String getEmpId() { return empId; }
    public void setEmpId(String empId) { this.empId = empId; }

    public int getBreakId() { return breakId; }
    public void setBreakId(int breakId) { this.breakId = breakId; }

    public int getKintaiRecId() { return kintaiRecId; }
    public void setKintaiRecId(int kintaiRecId) { this.kintaiRecId = kintaiRecId; }

    // Backward compatibility
    public int getRecId() { return kintaiRecId; }
    public void setRecId(int recId) { this.kintaiRecId = recId; }

    public Time getBreakStart() { return breakStart; }
    public void setBreakStart(Time breakStart) { this.breakStart = breakStart; }

    public Time getBreakEnd() { return breakEnd; }
    public void setBreakEnd(Time breakEnd) { this.breakEnd = breakEnd; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }

    // JSP互換
    public int getId() { return breakId; }
    public void setId(int id) { this.breakId = id; }

    // --- JSON 出力用 ---
    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"start\":\"").append(breakStart != null ? breakStart.toString().substring(0,5) : "").append("\",");
        sb.append("\"end\":\"").append(breakEnd != null ? breakEnd.toString().substring(0,5) : "").append("\"");
        sb.append("}");
        return sb.toString();
    }
}
