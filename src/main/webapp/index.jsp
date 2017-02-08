<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8"%>
<%@ page import="
    java.util.*,
    dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoader,
    dk.kb.netarchivesuite.solrwayback.facade.Facade,  
    dk.kb.netarchivesuite.solrwayback.service.dto.*"%>

<!DOCTYPE html>
<html>
<head>
    <title>SolrWayback</title>
    <script type="text/javascript" src="js/jquery-1.8.3.js"></script>
    <script type="text/javascript" src="js/bootstrap.min.js"></script>
    <link href="css/bootstrap.min.css" rel="stylesheet" media="screen" />    
    <style>
    body{ padding 1 em;
    }
    div.up{
    border-bottom: 2px solid #999;
     padding: 1em;    
    }
    div.wrapper{
     flex-wrap:wrap;
     display:flex;    
    }    
    </style>
    
</head>
<body>

<script>
function search(searchText){         
  document.imgageSearchRefForm.searchText.value=searchText;
  document.imgageSearchRefForm.submit();
}
</script>

<form name="imgageSearchRefForm" action="searchServlet"" method="POST">
    <input type="hidden" name="searchText" />
    
</form>

<h1>Solrwayback</h1>

<ul class="nav nav-tabs" id="configTab">
    <li class="active"><a href="#Search">Search</a></li>
    <li><a href="#Images">Images</a></li>    
    <li><a href="#API">API</a></li>
</ul>

<%@ include file="message.jsp" %>

    <div class="tab-content">
        <div class="tab-pane active" id="Search">
            <%@ include file="search.jsp" %>
        </div>
        <div class="tab-pane" id="Images">
            <%@ include file="images.jsp" %>
        </div>
        
        <div class="tab-pane" id="API">
            <%@ include file="api.jsp" %>
        </div>            

    </div>


<script>
    $('#configTab a').click(function (e) {
        e.preventDefault();
        $(this).tab('show');
    })
</script>

<%
    //Show correct tab (by number 0,1,2,3,..)
    String tab = (String) request.getAttribute("tab");
    if (tab != null){%>
<script>
    $('#configTab li:eq(<%=tab%>) a').tab('show');
</script>
<%}%>

</body>
</html>