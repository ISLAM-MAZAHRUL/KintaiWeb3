package kintai;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/leaveGrantManage")
public class LeaveGrantManageServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private LeaveGrantDao grantDao;
    private EmpDao empDao;

    
    
    @Override
    public void init() throws ServletException {
        grantDao = new LeaveGrantDao();
        empDao = new EmpDao();
        
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String mode = request.getParameter("mode");
        String leaveType = request.getParameter("leaveType");
        if (leaveType == null) leaveType = "annual";

        String grantDateStr = request.getParameter("grantDate");
        LocalDate grantDate;
        if (grantDateStr != null && !grantDateStr.isEmpty()) {
            grantDate = LocalDate.parse(grantDateStr);
        } else {
            grantDate = LocalDate.now();
        }
        
        LocalDate grantDateForAnnual = LocalDate.of(grantDate.getYear(), 7, 1);
        LocalDate grantDateForSpecial = LocalDate.of(grantDate.getYear(), 7, 1);
        
        String showAllStr = request.getParameter("showAll");
        boolean showAll = "true".equals(showAllStr);
        
        List<EmpBean> allEmp = empDao.findAllFullTimeEmployees();
        
        List<EmpBean> unissuedList = new ArrayList<>();

        int unissuedAnnual = 0;
        int unissuedInitial = 0;
        int unissuedSpecial = 0;
        int unissuedSubstitute = 0;

        for (EmpBean emp : allEmp) {
        	
        	// 追加
        	// 年次
            if (grantDao.isEligible(emp, grantDateForAnnual) &&
                !grantDao.alreadyGranted(emp.getEmpId(), grantDateForAnnual, LeaveGrantDao.LEAVE_TYPE_ANNUAL)) {
                unissuedAnnual++;
            }

            // 初回（3ヶ月 or 6ヶ月）
            LocalDate date3m = emp.getEmpDate().plusMonths(3);
            LocalDate date6m = emp.getEmpDate().plusMonths(6);
            boolean need3m = !grantDao.alreadyGranted(emp.getEmpId(), date3m, LeaveGrantDao.LEAVE_TYPE_INITIAL_3M)
                    && !grantDate.isBefore(date3m) && grantDao.isEligible(emp, date3m);
            boolean need6m = !grantDao.alreadyGranted(emp.getEmpId(), date6m, LeaveGrantDao.LEAVE_TYPE_INITIAL_6M)
                    && !grantDate.isBefore(date6m) && grantDao.isEligible(emp, date6m);
            if (need3m || need6m) {
                unissuedInitial++;
            }

            // 特別
            if ((grantDate.isEqual(grantDateForSpecial) || grantDate.isAfter(grantDateForSpecial)) &&
                !grantDao.alreadyGranted(emp.getEmpId(), grantDateForSpecial, LeaveGrantDao.LEAVE_TYPE_SPECIAL)) {
                unissuedSpecial++;
            }

            // 代休
            if (grantDao.isSubstituteLeaveNotGranted(emp, grantDate)) {
                unissuedSubstitute++;
            }
        	
        	
            boolean canGrantCurrent = false;

            switch (leaveType) {
            case "annual":
                canGrantCurrent = grantDao.isEligible(emp, grantDateForAnnual)
                                  && !grantDao.alreadyGranted(emp.getEmpId(), grantDateForAnnual, LeaveGrantDao.LEAVE_TYPE_ANNUAL);
                break;

            case "initial":
                canGrantCurrent = need3m || need6m;
                break;

            case "special":
                canGrantCurrent = (grantDate.isEqual(grantDateForSpecial) || grantDate.isAfter(grantDateForSpecial))
                                  && !grantDao.alreadyGranted(emp.getEmpId(), grantDateForSpecial, LeaveGrantDao.LEAVE_TYPE_SPECIAL);
                break;

            case "substitute":
                canGrantCurrent = grantDao.isSubstituteLeaveNotGranted(emp, grantDate);
                break;

            default:
                canGrantCurrent = false;
                break;
            }

            emp.setCanGrant(canGrantCurrent);

            // showAll が trueなら付与可・不可にかかわらず含める
            if (canGrantCurrent || showAll) {
                emp.setCanGrant(canGrantCurrent);
                int days = grantDao.calcGrantedDays(emp, switch (leaveType) {
                    case "annual" -> LeaveGrantDao.LEAVE_TYPE_ANNUAL;
                    case "initial" -> LeaveGrantDao.LEAVE_TYPE_INITIAL_3M; 
                    case "special" -> LeaveGrantDao.LEAVE_TYPE_SPECIAL;
                    case "substitute" -> LeaveGrantDao.LEAVE_TYPE_SUBSTITUTE;
                    default -> 0;
                });
                emp.setGrantedDays(days);

                unissuedList.add(emp);
            }
        }
        
        request.setAttribute("unissuedAnnual", unissuedAnnual);
        request.setAttribute("unissuedInitial", unissuedInitial);
        request.setAttribute("unissuedSpecial", unissuedSpecial);
        request.setAttribute("unissuedSubstitute", unissuedSubstitute);
        request.setAttribute("leaveType", leaveType);
        request.setAttribute("showAll", showAll);
        
        if ("preview".equals(mode)) {
            request.setAttribute("unissuedList", unissuedList);
            request.setAttribute("mode", "preview");
        }

        RequestDispatcher dispatcher = request.getRequestDispatcher("/web/leave_grant_manage.jsp");
        dispatcher.forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

    	String leaveType = request.getParameter("leaveType");
        String grantDateStr = request.getParameter("grantDate");
        LocalDate grantDate;
        if (grantDateStr != null && !grantDateStr.isEmpty()) {
            grantDate = LocalDate.parse(grantDateStr);
        } else {
            grantDate = LocalDate.now();
        }
        
        LocalDate grantDateForAnnual = LocalDate.of(grantDate.getYear(), 7, 1);
        LocalDate grantDateForSpecial = LocalDate.of(grantDate.getYear(), 7, 1);

        int grantedCount = 0;

        HttpSession session = request.getSession();
        UserBean user = (UserBean) session.getAttribute("user");
        String loginUser = (user != null) ? user.getEmpId() : "system";

        List<EmpBean> empList = empDao.findAllFullTimeEmployees();

        for (EmpBean emp : empList) {
            switch (leaveType) {
	            case "annual":
	                if (grantDao.grantAnnualLeave(emp, grantDateForAnnual, loginUser)) grantedCount++;
	                break;
	            case "initial":
	                if (grantDao.grantInitialAnnualLeave(emp, 1, grantDate, loginUser)) grantedCount++;
	                if (grantDao.grantInitialAnnualLeave(emp, 2, grantDate, loginUser)) grantedCount++;
	                break;
	            case "special":
	                if ((grantDate.isEqual(grantDateForSpecial) || grantDate.isAfter(grantDateForSpecial))
	                    && !grantDao.alreadyGranted(emp.getEmpId(), grantDateForSpecial, LeaveGrantDao.LEAVE_TYPE_SPECIAL)) {
	                    if (grantDao.grantSpecialLeave(emp, grantDateForSpecial, loginUser)) grantedCount++;
	                }
	                break;
	            case "substitute":
	                int added = grantDao.grantAllPendingCompLeaves(emp, grantDate, loginUser); // 付与すべき分すべて
	                grantedCount += added;
	                break;
            }
        }

        String leaveTypeName = switch (leaveType) {
        case "annual" -> "年次有給休暇";
        case "initial" -> "初回付与休暇（3・6か月）";
        case "special" -> "特別休暇";
        case "substitute" -> "代休";
        default -> leaveType;
        };
        
        if (grantedCount > 0) {
            request.setAttribute("message", grantedCount + "名に" + leaveTypeName + "を付与しました");
            request.setAttribute("success", true);
        } else {
            request.setAttribute("message", "該当する従業員がいなかったか、すでに付与済みです");
            request.setAttribute("success", false);
        }

        request.setAttribute("grantedCount", grantedCount);
        request.setAttribute("mode", "execute");
        request.setAttribute("leaveType", leaveType);
        request.setAttribute("grantDate", grantDate); 

        doGet(request, response);
    }
}
