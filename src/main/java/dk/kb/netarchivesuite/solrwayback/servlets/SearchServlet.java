package dk.kb.netarchivesuite.solrwayback.servlets;

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import dk.kb.netarchivesuite.solrwayback.facade.Facade;
import dk.kb.netarchivesuite.solrwayback.service.dto.SearchResult;
import dk.kb.netarchivesuite.solrwayback.solr.SolrClient;



public class SearchServlet extends HttpServlet{
    private static final long serialVersionUID = 1L;

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        response.setContentType("text/html; charset=UTF-8");
        request.setCharacterEncoding("UTF-8");
        try{        
          String searchText = request.getParameter("searchText");
          String filterText = request.getParameter("filterText");
          SearchResult result = Facade.search(searchText, filterText);
          
          request.setAttribute("searchResult", result);       
          request.setAttribute("searchText",searchText);
          request.setAttribute("filterText",filterText);
        
        request.setAttribute("tab","0");
        }
        
        catch(Exception e){
            e.printStackTrace();
        }        
        returnIndexPage(request, response);        
    }
    
    private void returnIndexPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = request.getRequestDispatcher("index.jsp");
        dispatcher.forward(request, response);
        return;
    }
    
}
