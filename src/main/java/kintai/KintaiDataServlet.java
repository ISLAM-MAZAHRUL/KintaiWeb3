package kintai;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/kintaiData")
public class KintaiDataServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private final WorkTimeDao workTimeDao = new WorkTimeDao();

    // --- DTOs ---
    static class BreakDto {
        String start; // "HH:mm"
        String end;   // "HH:mm"
    }
    static class ProjectDto {
        int projectId;
        double hours; // 0.5単位など
    }
    static class SavePayload {
        String empId;
        String date;        // "YYYY-MM-DD"
        String attendanceType;
        String clockIn;
        String clockOut;
        List<BreakDto> breaks = new ArrayList<>();
        List<ProjectDto> projects = new ArrayList<>();
    }
    static class GetResponse {
        String empId;
        String date;
        String attendanceType;
        String clockIn;
        String clockOut;
        List<BreakDto> breaks = new ArrayList<>();
        List<ProjectDto> projects = new ArrayList<>();
        int totalMinutes;
        boolean success = true;
        String message;
    }

    // =====================
    // GET: 勤怠1日分を取得
    // =====================
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");

        PrintWriter out = response.getWriter();
        GetResponse res = new GetResponse();
        try {
            final String empId = request.getParameter("empId");
            final String dateStr = request.getParameter("date");

            if (empId == null || empId.isEmpty() || dateStr == null || dateStr.isEmpty()) {
                throw new IllegalArgumentException("必須パラメータ不足 (empId/date)");
            }

            LocalDate date = LocalDate.parse(dateStr);
            res.empId = empId;
            res.date = dateStr;

            // --- 勤怠本体 ---
            WorkTimeBean wt = workTimeDao.findWorkTimeByDate(empId, date);
            if (wt != null) {
                String type = wt.getAttendanceType();
                res.attendanceType = (type != null && !type.isEmpty()) ? type : "出勤";
                if ("出勤".equals(res.attendanceType)) {
                    if (wt.getClockIn() != null) res.clockIn = toHHmm(wt.getClockIn().toLocalTime());
                    if (wt.getClockOut() != null) res.clockOut = toHHmm(wt.getClockOut().toLocalTime());
                }
            } else {
                res.attendanceType = "出勤";
            }

            // --- 休憩 ---
            List<BreakBean> breakList = workTimeDao.findBreaksByDate(empId, date);
            for (BreakBean b : breakList) {
                BreakDto d = new BreakDto();
                if (b.getBreakStart() != null) d.start = toHHmm(b.getBreakStart().toLocalTime());
                if (b.getBreakEnd() != null) d.end = toHHmm(b.getBreakEnd().toLocalTime());
                res.breaks.add(d);
            }

            // --- プロジェクト工数 ---
            List<KinmuManageBean.WorkAlloc> allocs = workTimeDao.findWorkAllocsByEmpNoAndDate(empId, date);
            for (KinmuManageBean.WorkAlloc a : allocs) {
                ProjectDto p = new ProjectDto();
                p.projectId = a.getProjectId();
                p.hours = a.getWorkHours();
                res.projects.add(p);
            }

            // 合計（勤務-休憩）
            res.totalMinutes = calcTotalMinutes(res.clockIn, res.clockOut, res.breaks);

        } catch (Exception e) {
            e.printStackTrace();
            res.success = false;
            res.message = "取得に失敗しました: " + e.getMessage();
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }

        // --- JSON出力 ---
        out.write(toJson(res));
    }

    // =====================
    // POST: 勤怠1日分を保存
    // =====================
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        UserBean loginUser = (UserBean) session.getAttribute("user");
        String loginEmpId = loginUser.getEmpId();

        PrintWriter out = response.getWriter();

        try {
            // ★ JSON を直接パースできないので、リクエストは form 送信前提
            SavePayload payload = new SavePayload();
            payload.empId = request.getParameter("empId");
            payload.date = request.getParameter("date");
            payload.attendanceType = request.getParameter("attendanceType");
            payload.clockIn = request.getParameter("clockIn");
            payload.clockOut = request.getParameter("clockOut");

            if (payload.empId == null || payload.date == null || payload.attendanceType == null) {
                throw new IllegalArgumentException("必須項目不足");
            }

            LocalDate date = LocalDate.parse(payload.date);
            WorkTimeBean wt = new WorkTimeBean();
            wt.setEmpId(payload.empId);
            wt.setKintaiDate(date);
            wt.setAttendanceType(payload.attendanceType);

            int totalMinutes = 0;

            if ("出勤".equals(payload.attendanceType)) {
                LocalTime in = parseHHmmOrNull(payload.clockIn);
                LocalTime outT = parseHHmmOrNull(payload.clockOut);
                wt.setClockIn(in == null ? null : Time.valueOf(in));
                wt.setClockOut(outT == null ? null : Time.valueOf(outT));

                totalMinutes = calcTotalMinutes(payload.clockIn, payload.clockOut, payload.breaks);
                BigDecimal workingHours = BigDecimal.valueOf(totalMinutes)
                        .divide(BigDecimal.valueOf(60), 2, BigDecimal.ROUND_HALF_UP);
                wt.setWorkingHours(workingHours);

                WorkTimeBean saved = workTimeDao.saveWorkTime(wt);
                int recId = saved.getKintaiRecId();

                workTimeDao.deleteBreaksByEmpIdAndDate(payload.empId, date);
                workTimeDao.deleteWorkAllocsByEmpIdAndDate(payload.empId, date);

            } else {
                wt.setClockIn(null);
                wt.setClockOut(null);

                BigDecimal workingHours;
                if ("有給".equals(payload.attendanceType)) {
                    workingHours = BigDecimal.valueOf(8);
                } else {
                    workingHours = BigDecimal.ZERO;
                }
                wt.setWorkingHours(workingHours);
                workTimeDao.saveWorkTime(wt);

                workTimeDao.deleteBreaksByEmpIdAndDate(payload.empId, date);
                workTimeDao.deleteWorkAllocsByEmpIdAndDate(payload.empId, date);
            }

            GetResponse res = new GetResponse();
            res.success = true;
            res.empId = payload.empId;
            res.date = payload.date;
            res.attendanceType = payload.attendanceType;
            res.clockIn = payload.clockIn;
            res.clockOut = payload.clockOut;
            res.totalMinutes = totalMinutes;

            out.write(toJson(res));

        } catch (Exception e) {
            e.printStackTrace();
            out.write("{\"success\":false,\"message\":\"保存に失敗しました\"}");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    // ---- Utilities ----
    private static String toHHmm(LocalTime t) {
        return String.format("%02d:%02d", t.getHour(), t.getMinute());
    }
    private static LocalTime parseHHmmOrNull(String s) {
        if (s == null || s.isEmpty()) return null;
        String[] sp = s.split(":");
        return LocalTime.of(Integer.parseInt(sp[0]), Integer.parseInt(sp[1]));
    }
    private static int calcTotalMinutes(String in, String out, List<BreakDto> breaks) {
        if (in == null || out == null || in.isEmpty() || out.isEmpty()) return 0;
        LocalTime tIn = parseHHmmOrNull(in);
        LocalTime tOut = parseHHmmOrNull(out);
        int diff = (int) java.time.Duration.between(tIn, tOut).toMinutes();
        int rest = 0;
        if (breaks != null) {
            for (BreakDto b : breaks) {
                if (b.start == null || b.end == null) continue;
                LocalTime bs = parseHHmmOrNull(b.start);
                LocalTime be = parseHHmmOrNull(b.end);
                rest += (int) java.time.Duration.between(bs, be).toMinutes();
            }
        }
        return Math.max(0, diff - rest);
    }

    // JSON文字列を手動生成
    private static String toJson(GetResponse res) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"success\":").append(res.success).append(",");
        sb.append("\"empId\":\"").append(nullToEmpty(res.empId)).append("\",");
        sb.append("\"date\":\"").append(nullToEmpty(res.date)).append("\",");
        sb.append("\"attendanceType\":\"").append(nullToEmpty(res.attendanceType)).append("\",");
        sb.append("\"clockIn\":\"").append(nullToEmpty(res.clockIn)).append("\",");
        sb.append("\"clockOut\":\"").append(nullToEmpty(res.clockOut)).append("\",");
        sb.append("\"totalMinutes\":").append(res.totalMinutes).append(",");

        sb.append("\"breaks\":[");
        for (int i = 0; i < res.breaks.size(); i++) {
            BreakDto b = res.breaks.get(i);
            if (i > 0) sb.append(",");
            sb.append("{\"start\":\"").append(nullToEmpty(b.start)).append("\",");
            sb.append("\"end\":\"").append(nullToEmpty(b.end)).append("\"}");
        }
        sb.append("],");

        sb.append("\"projects\":[");
        for (int i = 0; i < res.projects.size(); i++) {
            ProjectDto p = res.projects.get(i);
            if (i > 0) sb.append(",");
            sb.append("{\"projectId\":").append(p.projectId).append(",");
            sb.append("\"hours\":").append(p.hours).append("}");
        }
        sb.append("]");

        sb.append("}");
        return sb.toString();
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
