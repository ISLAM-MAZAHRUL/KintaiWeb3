package kintai;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/projectDetail")
public class ProjectDetailServlet extends HttpServlet {
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String empId = req.getParameter("empId");
        String dateStr = req.getParameter("date");

        if(empId == null || empId.isEmpty() || dateStr == null || dateStr.isEmpty()){
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "empId または date が指定されていません");
            return;
        }

        LocalDate date;
        try {
            date = LocalDate.parse(dateStr);
        } catch(Exception e){
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "date の形式が不正です");
            return;
        }

        List<KinmuManageBean.WorkAlloc> workList = new KinmuManageDao().findKinmuManageByDate(empId, date);

        // JSON を自作（HH:MM形式も追加）
        StringBuilder json = new StringBuilder();
        json.append("[");
        for (int i = 0; i < workList.size(); i++) {
            KinmuManageBean.WorkAlloc k = workList.get(i);
            String projectName = k.getProjectName() != null ? k.getProjectName().replace("\"", "\\\"") : "";
            String hhmm = KinmuManageBean.WorkAlloc.formatHoursToHHMM(k.getWorkHours()); // ← static メソッド使用
            json.append("{\"projectName\":\"").append(projectName).append("\",")
                .append("\"workHours\":").append(k.getWorkHours()).append(",")
                .append("\"workHoursHHMM\":\"").append(hhmm).append("\"}");
            if (i < workList.size() - 1) json.append(",");
        }
        json.append("]");

        resp.setContentType("application/json;charset=UTF-8");
        resp.getWriter().write(json.toString());
    }
}

