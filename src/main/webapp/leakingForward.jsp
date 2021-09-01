<%@page import="dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoader"%>

<%


String orgUrl = (String) request.getAttribute("javax.servlet.error.request_uri");

  //http://localhost:8080/solrwayback/
  String redirectURL = PropertiesLoader.WAYBACK_BASEURL+"services/resolveLeak?url="+orgUrl;
  response.sendRedirect(redirectURL);
%>

