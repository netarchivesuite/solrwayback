<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8"%>
<%@ page import="
    java.util.*,
    dk.kb.netarchivesuite.solrwayback.service.dto.PagePreview,
    dk.kb.netarchivesuite.solrwayback.facade.Facade"%>

<!DOCTYPE html>


<%
String url = (String) request.getParameter("url");

ArrayList<PagePreview> allPreviews= Facade.getPagePreviewsForUrl(url);


ArrayList<PagePreview> show10Previews = new ArrayList<PagePreview>();

for (int i =0;i<4;i++){
  show10Previews.add(allPreviews.get((int) (Math.random()*allPreviews.size())));  
}

%>


<html>
<head>
    <meta charset="UTF-8">
    <title>Page previews</title>
    <link rel="stylesheet" type="text/css" media="all" href="./css/solrwayback.css">
    <script type="text/javascript">
        var show10Previews= "<%=show10Previews %>";
        console.log("show10Previews: ", show10Previews);
        var allPreviews = "<%=allPreviews %>";
        console.log("allPreviews: ", allPreviews);
    </script>
</head>

<body>

<div class="wrapper">
    <h1>Url:<%=url%></h1>
        <h1>#Harvest:<%=allPreviews.size()%></h1>



    <div>
    <%
    for (PagePreview current: show10Previews){
    Date d = new Date(current.getCrawlDate());
    %>


    <div class="webPageThumb">
     <img src="<%=current.getPagePreviewUrl()%>" alt="<%=d%>" height="512" width="640"><br>
     <a href="<%=current.getSolrWaybackUrl()%>" target="_new"><%=d%></a>
    </div>

    <%}%>

    </div>
</div>


</body>
</html>


