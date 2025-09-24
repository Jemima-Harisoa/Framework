package main.java.com;

import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;

public class RedirectionServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private void doService(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        // Récupérer l’URL demandée
        String url = request.getRequestURL().toString();

        // Configurer la réponse
        response.setContentType("text/plain");
        PrintWriter out = response.getWriter();

        // Afficher l’URL
        out.println("URL demandée : " + url);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        doService(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        doService(request, response);
    }
}
