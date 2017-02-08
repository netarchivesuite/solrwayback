
<h1>API SolrWayback REST resources. </h1>
<h2>Version:${pom.version}</h2>
<h2>Build time:${build.time}</h2>
<br>                                                       
<h2> SERVICE METHODS: </h2>
<br>


<table class="table" border="1">  
       <caption><strong>HTTP GET</strong></caption>
        <thead>  
          <tr>  
            <th>URL</th>  
            <th>Input</th>                
            <th>Output</th>
          </tr>  
        </thead>  
        <tbody>                     
            <tr>  
            <td>services/search?searchText=xxx</td>  
            <td>
            Params:searchText
            </td>  
            <td>
              SearchResult
            </td>
          </tr>                                                                     
          <tr>     
            <td>services/image?arcFilePath=zzz&offset=xxx&height=xxx&width=yyy</td>  
            <td>
             Params:arcFilePath,offset,height,width
            </td>  
            <td>
              The image  
            </td>
          </tr>                
         <tr>     
            <td>services/downloadRaw?arcFilePath=xxx&offset=yyy</td>  
            <td>
             Params:arcFilePath,offset
            </td>  
            <td>
              Download the arc entry (any mimetype)  
            </td>
          </tr>                      
         
             <tr>     
            <td>services/findimages?searchText=xxx</td>  
            <td>
             Params:searchText
            </td>  
            <td>
             ArrayList<ArcEntryDescriptor> (arc file names and offset)  
            </td>
          </tr>    
         
            
             <tr>     
            <td>services/getContentType?arcFilePath=xxx&offset=yyy</td>  
            <td>
             Params:arcFilePath,offset
            </td>  
            <td>
             String.( UTF-8 etc.)  
            </td>
          </tr>  
                  
           <tr>     
            <td>services/wayback?waybackdata={waybackformat}</td>  
            <td>
            Same format as Wayback({timestamp}/{url})<br>
            Params example: 19990914144635/http://www.statsbiblioteket.dk/nationalbibliotek/kulturarv
            </td>  
            <td>
              Show the HTML page with harvest time nearest to the timestamp in the Solrwayback engine.               
            </td>
           </tr>
                    
           <tr>     
            <td>/waybacklinkgraph?domain=xxxx&ingoing=true</td>              
            <td>            
            Params:domain (statsbiblioteket.dk etc.), ingoing (true or false)
            </td>  
            <td>
             Returns the D3 node JSON required for visualization in D3.  
            </td>
          </tr>  
                 
         </tbody>  
</table>    
        
<br>


<br>

<table class="table" border="1">  
       <caption><strong>HTTP errors</strong></caption>
        <thead>  
          <tr>  
            <th>Error</th>  
            <th>Reason</th>                
          </tr>  
        </thead>  
        <tbody>  
          <tr>  
            <td>400 (Bad Request)</td>  
            <td>Caused by the input. Validation error etc.</td>    
          </tr>
           <tr>  
            <td>404 (Bad Request)</td>  
            <td>Not found</td>    
          </tr>    
            <tr>  
            <td>500 (Internal Server Error)</td>  
            <td>Server side errors, nothing to do about it.</td>    
          </tr>
        </tbody>  
</table>    
