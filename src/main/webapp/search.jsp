<%
 SearchResult result =  (SearchResult) request.getAttribute("searchResult");
 String searchText =  (String) request.getAttribute("searchText");
 if(searchText==null){
  searchText="";
 } 
 
 
 String filterText =  (String) request.getAttribute("filterText");
 if(filterText==null){
     filterText="content_type_norm:html";
 }
%>

<h1> SEARCH</h1>

<form name="searchForm" class="well" action="searchServlet" method="POST">
<Strong>Filter Query:</Strong></br>
<textarea class="form-control field span6" rows="1"  name="filterText" value=""><%=filterText%> </textarea> (types: "html","image", "text","other","pdf","video","audio","word","excel","powerpoint")<br>
<Strong>Search text:</Strong></br>
<textarea class="form-control field span12" rows="4"  name="searchText" value=""><%=searchText%> </textarea>
<input type="submit" value="Search">
</form>



<% if (result != null){%>
<p><mark>Number of results: <%=result.getNumberOfResults()%> (Showing max 10)</p><br><br> 
<div class="container-fluid">



<%
List<IndexDoc> list = result.getResults();

for (IndexDoc current : list){

  String arc_full = current.getArc_full();       
  String id = current.getId();
  String title = current.getTitle();
  String source_file_s = current.getSource_file_s();
  long offset=current.getOffset(); 
  String url = current.getUrl();   
  String mineType = current.getMimeType();
  String contentTypeNorm=current.getContentTypeNorm();
  
  
  if ("image".equals(contentTypeNorm)){
  
%>    
      <div class="row-fluid">
        <div class="span10" style="word-wrap: break-word; " >
         <Strong>id:</Strong> <%=id%></br>
         <Strong>arc:</Strong> <%=arc_full%></br>         
         <Strong>offset:</Strong> <%=offset%></br>
         <Strong>url:</Strong> <%=url%></br>
         <Strong>mimeType:</Strong> <%=mineType%></br>
          <Strong>contentTypeNorm:</Strong> <%=contentTypeNorm%></br>
        </div>
        <div class="span2">
         <a href="<%=PropertiesLoader.WAYBACK_BASEURL%>services/downloadRaw?arcFilePath=<%=arc_full%>&offset=<%=offset%>" target="new">
         <img src="<%=PropertiesLoader.WAYBACK_BASEURL%>services/image?arcFilePath=<%=arc_full%>&offset=<%=offset%>&height=150&width=150">
         </a>
        </div>
      </div>    
<hr>
<%} else if ("html".equals(contentTypeNorm)){
    %>
 <div class="row-fluid">
        <div class="span10" style="word-wrap: break-word; " >
         <Strong>title:</Strong> <%=title%></br>
         <Strong>id:</Strong> <%=id%></br>
         <Strong>arc:</Strong> <%=arc_full%></br>         
         <Strong>offset:</Strong> <%=offset%></br>
         <Strong>url:</Strong> <%=url%></br>
         <Strong>mimeType:</Strong> <%=mineType%></br>
          <Strong>contentTypeNorm:</Strong> <%=contentTypeNorm%></br>
        </div>
        <div class="span2">
      <!--  Download binary/Wayback view disabled-->
      
         <a href="<%=PropertiesLoader.WAYBACK_BASEURL%>services/downloadRaw?arcFilePath=<%=arc_full%>&offset=<%=offset%>" target="new">
              <button type="button" class="btn btn-primary">Download</button> 
         </a>
         
          <a href="<%=PropertiesLoader.WAYBACK_BASEURL%>services/view?arcFilePath=<%=arc_full%>&offset=<%=offset%>" target="new">
            <button type="button" class="btn btn-primary">View</button> 
         </a>
         
         
        </div>        
        
      
      </div>
<%
        ArrayList<? extends ArcEntryDescriptor> images = Facade.getImagesFromHtmlPage(current);
        for (ArcEntryDescriptor currentImage:images){
            String arc_full2 = currentImage.getArcFull();       
            long offset2=currentImage.getOffset();
                      
            %>
            <a href="<%=PropertiesLoader.WAYBACK_BASEURL%>services/downloadRaw?arcFilePath=<%=arc_full2%>&offset=<%=offset2%>" target="new">
           <img src="<%=PropertiesLoader.WAYBACK_BASEURL%>services/image?arcFilePath=<%=arc_full2%>&offset=<%=offset2%>&height=150&width=150">
            </a> 
            <%
            
        }
        
        // show images on page
        
        %>


<hr>
<%}
else{ %>
<div class="row-fluid">
       <div class="span10" style="word-wrap: break-word; " >
        <Strong>id:</Strong> <%=id%></br>
        <Strong>arc:</Strong> <%=arc_full%></br>         
        <Strong>arc file:</Strong> <%=arc_full %></br>
        <Strong>offset:</Strong> <%=offset%></br>
        <Strong>url:</Strong> <%=url%></br>
        <Strong>mimeType:</Strong> <%=mineType%></br>
         <Strong>contentTypeNorm:</Strong> <%=contentTypeNorm%></br>
       </div>
       <div class="span2">
        <a href="<%=PropertiesLoader.WAYBACK_BASEURL%>services/downloadRaw?arcFilePath=<%=arc_full%>&offset=<%=offset%>" target="new">
           <button type="button" class="btn btn-primary">Download</button>  
        </a>
       </div>
     </div>
<hr>
<%}

}%>
 </div>
<%}%>





