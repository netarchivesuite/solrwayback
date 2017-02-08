<%
String message=  (String) request.getAttribute("message");

if (message != null){%>
<p class="inputError"> <%=message%> </>
<%}
%>

