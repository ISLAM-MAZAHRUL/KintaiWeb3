package kintai;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/YukyuKanriServlet")
public class YukyuKanriServlet extends HttpServlet{
	 private static final long serialVersionUID = 1L;
	 private LeaveBalanceDao leaveBalanceDao = new LeaveBalanceDao();
	 @Override
	    protected void doGet(HttpServletRequest request, HttpServletResponse response)
	            throws ServletException, IOException {

	        HttpSession session = request.getSession(false);
	        if (session == null || session.getAttribute("user") == null) {
	            response.sendRedirect(request.getContextPath() + "/web/login.jsp");
	            return;
	        }

	        List<Map<String, Object>> summaryList = leaveBalanceDao.findAllSummary();
	        request.setAttribute("summaryList", summaryList);
	        request.getRequestDispatcher("/web/YukyuKanri.jsp")
	               .forward(request, response);
	 }

}
