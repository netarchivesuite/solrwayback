<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8"%>
<%@ page import="java.net.URLEncoder"%>


<%
//query param example:
//waybackdata=/20090608215905/http://eb.dk/flash/udlandkendte/article998698.ece?utm_source=fb&utm_campaign=sh
String query = request.getQueryString();
query=query.substring(13); //Remove 'waybackdata=/' part as we dont want to url encode this. 
String queryEncoded=URLEncoder.encode(query, "UTF-8");//encode the rest

String newUrl="services/wayback?waybackdata="+queryEncoded; //put back the waybackdata param
%>

</html>

<!DOCTYPE HTML>
<html lang="en-US">
    <head>
        <meta charset="UTF-8">
        <meta http-equiv="refresh" content="1;url=<%=newUrl%>">
        <script type="text/javascript">
            window.location.href = "<%=newUrl%>"
        </script>
        <title>Page Redirection</title>
    </head>
    <body>
        <!-- Note: don't tell people to `click` the link, just tell them that it is a link. -->
        If you are not redirected automatically, follow the <a href='<%=newUrl%>'>link to example</a>
    </body>
</html>