package kintai;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/menu")
public class MenuServlet extends HttpServlet {

    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doPost(request,response);
        
    }
    public void doPost(HttpServletRequest requset, HttpServletResponse response)
            throws ServletException, IOException {

        requset.getRequestDispatcher("/web/menu.jsp").forward(requset, response);
    }

}
