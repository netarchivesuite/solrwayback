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

for (int i =0;i<10;i++){
  show10Previews.add(allPreviews.get((int) (Math.random()*allPreviews.size())));  
}

%>



<html>
<head>
    <meta charset="UTF-8">
</head>

<body>
<title>page previews</title>
<h1> url:<%=url%></h1>
<h1> #Harvest:<%=allPreviews.size()%></h1>


<table border="1">
<tr>
<%
for (PagePreview current: show10Previews){
Date d = new Date(current.getCrawlDate());
%> 


<td>
 <img src="<%=current.getPagePreviewUrl()%>" alt="<%=d%>" height="512" width="640"><br>
 <a href="<%=current.getSolrWaybackUrl()%>" target="_new"><%=d%></a>
</td>

<%}%>

</tr>
</table>


</body>
</html>


