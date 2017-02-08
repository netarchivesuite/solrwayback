<%
 ArrayList<ArcEntryDescriptor> imageResult =  (ArrayList<ArcEntryDescriptor>) request.getAttribute("findimagesResult");
 
String findimagesText =  (String) request.getAttribute("findimagesText");
 if(findimagesText==null){
     findimagesText="";
 } 
 if ( imageResult == null){
     imageResult = new ArrayList<ArcEntryDescriptor> ();
 }
 
%>

<h1> Find images</h1>

<form name="searchForm" class="well" action="findimagesServlet" method="POST">
<Strong>Search text:</Strong></br>
<textarea class="form-control field span12" rows="4"  name="findimagesText" value=""><%=findimagesText%> </textarea>
<input type="submit" value="Search">

 
</form>
<form action="Commonsfileuploadservlet" enctype="multipart/form-data" method="POST">
                    <input type="file" name="file1" accept="application/xlsx" accesskey="g">
                    <input type="Submit" value="Upload File" accesskey="u"   class="button"><br/>
 </form>

<div class="wrapper">
<%
   
       
   for (ArcEntryDescriptor currentImage:imageResult){
            String arc_full2 = currentImage.getArcFull();                   
            long offset2=currentImage.getOffset();
                      
            %>
           

  <div class="up">
    <a href="<%=PropertiesLoader.WAYBACK_BASEURL%>services/downloadRaw?arcFilePath=<%=arc_full2%>&offset=<%=offset2%>" target="new">
       <img src="<%=PropertiesLoader.WAYBACK_BASEURL%>services/image?arcFilePath=<%=arc_full2%>&offset=<%=offset2%>&height=150&width=150">
    </a>
      <br>
      <input class="btn btn-primary " type="button" value="REF" onclick="javascript:search('hash:&quot;<%=currentImage.getHash()%>&quot;');"/>
    </div>

            <%
            
        }
        
        // show images on page
        
        %>
  </div>      
        