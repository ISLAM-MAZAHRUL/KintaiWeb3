package kintai;

import java.time.LocalDate;

public class LeaveBalanceBean {
    private String empId;
    private int leaveTypeId;
    private String leaveTypeName;
    private LocalDate grantedDate;      // java.sql.Dateで統一
    private LocalDate expirationDate;
    private int grantedDays;       // 付与日数
    private int usedDays;

    public String getEmpId() { return empId; }
    public void setEmpId(String empId) { this.empId = empId; }

    public int getLeaveTypeId() { return leaveTypeId; }
    public void setLeaveTypeId(int leaveTypeId) { this.leaveTypeId = leaveTypeId; }

    public String getLeaveTypeName() { return leaveTypeName; }
    public void setLeaveTypeName(String leaveTypeName) { this.leaveTypeName = leaveTypeName; }

    public LocalDate getGrantedDate() { return grantedDate; }
    public void setGrantedDate(LocalDate localDate) { this.grantedDate = localDate; }

    public LocalDate getExpirationDate() { return expirationDate; }
    public void setExpirationDate(LocalDate localDate) { this.expirationDate = localDate; }

    public int getGrantedDays() { return grantedDays; }
    public void setGrantedDays(int grantedDays) { this.grantedDays = grantedDays; }

    public int getUsedDays() { return usedDays; }
    public void setUsedDays(int usedDays) { this.usedDays = usedDays; }
}
