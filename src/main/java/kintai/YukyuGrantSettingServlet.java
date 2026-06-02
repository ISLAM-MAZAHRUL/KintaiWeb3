package kintai;

import java.io.IOException;
import java.util.List;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/YukyuGrantSettingServlet")
public class YukyuGrantSettingServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private EmpDao empDao = new EmpDao();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            response.sendRedirect(request.getContextPath() + "/web/login.jsp");
            return;
        }

        UserBean loginUser = (UserBean) session.getAttribute("user");
        if (loginUser.getRoleId() != 1) {
            response.sendRedirect(request.getContextPath() + "/YukyuKanriServlet");
            return;
        }

        List<EmpBean> empList = empDao.findAll();
        request.setAttribute("empList", empList);
        request.getRequestDispatcher("/web/YukyuGrantSetting.jsp")
               .forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            response.sendRedirect(request.getContextPath() + "/web/login.jsp");
            return;
        }

        UserBean loginUser = (UserBean) session.getAttribute("user");
        if (loginUser.getRoleId() != 1) {
            response.sendRedirect(request.getContextPath() + "/YukyuKanriServlet");
            return;
        }

        String empId = request.getParameter("empId");
        String grantedDaysStr = request.getParameter("grantedDays");

        try {
            int grantedDays = Integer.parseInt(grantedDaysStr);
            empDao.updateGrantedDays(empId, grantedDays);
            request.setAttribute("message", empId + " の付与日数を " + grantedDays + " 日に更新しました。");
        } catch (Exception e) {
            e.printStackTrace();
            request.setAttribute("message", "エラーが発生しました: " + e.getMessage());
        }

        List<EmpBean> empList = empDao.findAll();
        request.setAttribute("empList", empList);
        request.getRequestDispatcher("/web/YukyuGrantSetting.jsp")
               .forward(request, response);
    }
}