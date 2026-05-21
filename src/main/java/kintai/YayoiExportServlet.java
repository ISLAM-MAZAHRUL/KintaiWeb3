package kintai;

import java.io.IOException;
import java.io.OutputStream;
import java.time.YearMonth;
import java.util.List;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/YayoiExportServlet")
public class YayoiExportServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private KintaiRecDao kintaiRecDao = new KintaiRecDao();
    private EmpDao empDao = new EmpDao();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doPost(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // セッションチェック
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        // 対象月取得
     // 対象期間取得
        String startMonth = request.getParameter("startMonth");
        String endMonth = request.getParameter("endMonth");

        if (startMonth == null || startMonth.isEmpty()
                || endMonth == null || endMonth.isEmpty()) {

            YearMonth now = YearMonth.now();

            startMonth = now.toString();
            endMonth = now.toString();
        }

        try {

            // action取得
            String action = request.getParameter("action");

            if (action == null) {
                action = "download";
            }

            System.out.println("action = " + action);

            YearMonth startYm = YearMonth.parse(startMonth);
            YearMonth endYm = YearMonth.parse(endMonth);

            // 全従業員リスト取得
            List<EmpBean> empList = empDao.findAll();

            // preview mode
            if ("preview".equals(action)) {

                List<MonthlySummaryBean> summaryList =
                    new java.util.ArrayList<>();

                for (EmpBean emp : empList) {

                    MonthlySummaryBean totalSummary =
                        new MonthlySummaryBean();

                    YearMonth current = startYm;

                    while (!current.isAfter(endYm)) {

                        MonthlySummaryBean monthly =
                            kintaiRecDao.getMonthlySummary(
                                emp.getEmpId(),
                                current.toString()
                            );

                        totalSummary.setTotalWorkDays(
                            totalSummary.getTotalWorkDays()
                            + monthly.getTotalWorkDays()
                        );

                        totalSummary.setActualAttendanceDays(
                            totalSummary.getActualAttendanceDays()
                            + monthly.getActualAttendanceDays()
                        );

                        totalSummary.setHolidayWorkDays(
                            totalSummary.getHolidayWorkDays()
                            + monthly.getHolidayWorkDays()
                        );

                        totalSummary.setPaidLeaveDays(
                            totalSummary.getPaidLeaveDays()
                            + monthly.getPaidLeaveDays()
                        );

                        totalSummary.setAbsentDays(
                            totalSummary.getAbsentDays()
                            + monthly.getAbsentDays()
                        );

                        totalSummary.setTotalWorkingHours(
                            totalSummary.getTotalWorkingHours()
                            .add(monthly.getTotalWorkingHours())
                        );

                        totalSummary.setTotalOvertimeHours(
                            totalSummary.getTotalOvertimeHours()
                            .add(monthly.getTotalOvertimeHours())
                        );

                        totalSummary.setTotalNightHours(
                            totalSummary.getTotalNightHours()
                            .add(monthly.getTotalNightHours())
                        );

                        current = current.plusMonths(1);
                    }

                    summaryList.add(totalSummary);
                }

                request.setAttribute("empList", empList);
                request.setAttribute("summaryList", summaryList);

                request.setAttribute("startMonth", startMonth);
                request.setAttribute("endMonth", endMonth);

                request.getRequestDispatcher("/web/kinmu_yayoi_export.jsp")
                       .forward(request, response);

                return;
            }

            // Excel download
            if ("excel".equals(action)) {

                String filename =
                    "弥生インポート用_"
                    + startMonth
                    + "_to_"
                    + endMonth
                    + ".xlsx";

                String encodedFilename =
                    java.net.URLEncoder.encode(filename, "UTF-8")
                    .replace("+", "%20");

                response.setContentType(
                    "application/vnd.ms-excel; charset=UTF-8"
                );

                response.setHeader(
                    "Content-Disposition",
                    "attachment; filename*=UTF-8''"
                    + encodedFilename
                );

                StringBuilder sb = new StringBuilder();

                sb.append(
                    "社員番号,氏名,所定労働日,出勤日数,所定労働時間,"
                );

                sb.append(
                    "実働時間,特別休暇日数,有休日数,欠勤回数,"
                );

                sb.append(
                    "普通残業時間,深夜残業時間,出勤基礎日数\n"
                );

                for (EmpBean emp : empList) {

                    MonthlySummaryBean totalSummary =
                        new MonthlySummaryBean();

                    YearMonth current = startYm;

                    while (!current.isAfter(endYm)) {

                        MonthlySummaryBean monthly =
                            kintaiRecDao.getMonthlySummary(
                                emp.getEmpId(),
                                current.toString()
                            );

                        totalSummary.setTotalWorkDays(
                            totalSummary.getTotalWorkDays()
                            + monthly.getTotalWorkDays()
                        );

                        totalSummary.setActualAttendanceDays(
                            totalSummary.getActualAttendanceDays()
                            + monthly.getActualAttendanceDays()
                        );

                        totalSummary.setHolidayWorkDays(
                            totalSummary.getHolidayWorkDays()
                            + monthly.getHolidayWorkDays()
                        );

                        totalSummary.setPaidLeaveDays(
                            totalSummary.getPaidLeaveDays()
                            + monthly.getPaidLeaveDays()
                        );

                        totalSummary.setAbsentDays(
                            totalSummary.getAbsentDays()
                            + monthly.getAbsentDays()
                        );

                        totalSummary.setTotalWorkingHours(
                            totalSummary.getTotalWorkingHours()
                            .add(monthly.getTotalWorkingHours())
                        );

                        totalSummary.setTotalOvertimeHours(
                            totalSummary.getTotalOvertimeHours()
                            .add(monthly.getTotalOvertimeHours())
                        );

                        totalSummary.setTotalNightHours(
                            totalSummary.getTotalNightHours()
                            .add(monthly.getTotalNightHours())
                        );

                        current = current.plusMonths(1);
                    }

                    sb.append(emp.getEmpId()).append(",");
                    sb.append(emp.getEmpName()).append(",");
                    sb.append(totalSummary.getTotalWorkDays()).append(",");
                    sb.append(totalSummary.getActualAttendanceDays()).append(",");
                    sb.append(totalSummary.getTotalWorkDays() * 8).append(",");
                    sb.append(roundHours(totalSummary.getTotalWorkingHours())).append(",");
                    sb.append(totalSummary.getHolidayWorkDays()).append(",");
                    sb.append(totalSummary.getPaidLeaveDays()).append(",");
                    sb.append(totalSummary.getAbsentDays()).append(",");
                    sb.append(roundHours(totalSummary.getTotalOvertimeHours())).append(",");
                    sb.append(roundHours(totalSummary.getTotalNightHours())).append(",");
                    sb.append(totalSummary.getActualAttendanceDays()).append("\n");
                }

                OutputStream out = response.getOutputStream();

                out.write(new byte[]{
                    (byte)0xEF,
                    (byte)0xBB,
                    (byte)0xBF
                });

                out.write(sb.toString().getBytes("UTF-8"));

                out.flush();
                out.close();

                return;
            }

            // CSV生成
            StringBuilder sb = new StringBuilder();

            sb.append(
                "社員番号,氏名,所定労働日,出勤日数,所定労働時間,"
            );

            sb.append(
                "実働時間,特別休暇日数,有休日数,欠勤回数,"
            );

            sb.append(
                "普通残業時間,深夜残業時間,出勤基礎日数\n"
            );

            for (EmpBean emp : empList) {

                MonthlySummaryBean totalSummary =
                    new MonthlySummaryBean();

                YearMonth current = startYm;

                while (!current.isAfter(endYm)) {

                    MonthlySummaryBean monthly =
                        kintaiRecDao.getMonthlySummary(
                            emp.getEmpId(),
                            current.toString()
                        );

                    totalSummary.setTotalWorkDays(
                        totalSummary.getTotalWorkDays()
                        + monthly.getTotalWorkDays()
                    );

                    totalSummary.setActualAttendanceDays(
                        totalSummary.getActualAttendanceDays()
                        + monthly.getActualAttendanceDays()
                    );

                    totalSummary.setHolidayWorkDays(
                        totalSummary.getHolidayWorkDays()
                        + monthly.getHolidayWorkDays()
                    );

                    totalSummary.setPaidLeaveDays(
                        totalSummary.getPaidLeaveDays()
                        + monthly.getPaidLeaveDays()
                    );

                    totalSummary.setAbsentDays(
                        totalSummary.getAbsentDays()
                        + monthly.getAbsentDays()
                    );

                    totalSummary.setTotalWorkingHours(
                        totalSummary.getTotalWorkingHours()
                        .add(monthly.getTotalWorkingHours())
                    );

                    totalSummary.setTotalOvertimeHours(
                        totalSummary.getTotalOvertimeHours()
                        .add(monthly.getTotalOvertimeHours())
                    );

                    totalSummary.setTotalNightHours(
                        totalSummary.getTotalNightHours()
                        .add(monthly.getTotalNightHours())
                    );

                    current = current.plusMonths(1);
                }

                sb.append(emp.getEmpId()).append(",");
                sb.append(emp.getEmpName()).append(",");
                sb.append(totalSummary.getTotalWorkDays()).append(",");
                sb.append(totalSummary.getActualAttendanceDays()).append(",");
                sb.append(totalSummary.getTotalWorkDays() * 8).append(",");
                sb.append(roundHours(totalSummary.getTotalWorkingHours())).append(",");
                sb.append(totalSummary.getHolidayWorkDays()).append(",");
                sb.append(totalSummary.getPaidLeaveDays()).append(",");
                sb.append(totalSummary.getAbsentDays()).append(",");
                sb.append(roundHours(totalSummary.getTotalOvertimeHours())).append(",");
                sb.append(roundHours(totalSummary.getTotalNightHours())).append(",");
                sb.append(totalSummary.getActualAttendanceDays()).append("\n");
            }

            // レスポンス設定
            String filename =
                "弥生インポート用_"
                + startMonth
                + "_to_"
                + endMonth
                + ".csv";

            String encodedFilename =
                java.net.URLEncoder.encode(filename, "UTF-8")
                .replace("+", "%20");

            response.setContentType("text/csv; charset=UTF-8");

            response.setHeader(
                "Content-Disposition",
                "attachment; filename*=UTF-8''"
                + encodedFilename
            );

            OutputStream out = response.getOutputStream();

            out.write(new byte[]{
                (byte)0xEF,
                (byte)0xBB,
                (byte)0xBF
            });

            out.write(sb.toString().getBytes("UTF-8"));

            out.flush();
            out.close();

        } catch (Exception e) {

            e.printStackTrace();

            response.sendError(
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "エクスポート中にエラーが発生しました: "
                + e.getMessage()
            );
        }
    }
    

    private double roundHours(java.math.BigDecimal hours) {
        if (hours == null) return 0.0;
        return Math.round(hours.doubleValue() * 100.0) / 100.0;
    }
    
    
}