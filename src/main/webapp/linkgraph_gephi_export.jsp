<html>

<title>Export domain link graph</title>
<body>
<h1> Export domain link graph in Gephi format</h1>

<h3>Examples:<h3>
<table border="1">
<tr>
  <th> Example </th> <th> Query </th>
 </tr>
<tr>
  <td>
 Local neighborhood link graph for a domain.<br> 
 </td>
  <td>
   domain:wikipedia.dk OR links_domains:wikipedia.dk  
  </td>
</tr>
<tr>
  <td>
 Complete domain link graph for a given crawl time interval<br> 
 </td>
  <td> 
   crawl_date:[2015-01-01T00:00:00Z TO 2015-03-01T00:00:00Z]    
  </td>
</tr>
<tr>
  <td>
 Extract complete top-level domain<br> 
 </td>
  <td> 
   host_surt:"(uk,"    
  </td>
</tr>

<tr>
  <td>
    Extract only domains based on text (on slashpage)<br> 
 </td>
  <td> 
text:"commodore 64" OR text:"commodore64" OR text:"commodore amiga"    
  </td>
</tr>
</table>


<form name="export" action="/solrwayback/services/export/linkgraph">
Query:<br>  

 <textarea rows="4" cols="50" name="query"></textarea> 
 
 <br>
  <input type="submit" value="Export in Gephi format">
</form>
<br>

<h3>Limitations:</h3>
 <ul>
  <li>Extraction will stop after 1 million different domains has been extrated. </li>
  <li>Only extract links from 'slashpage' of a given domain: etc http://test.dk/ or http://test.dk/index.html</li>
  <li>Only extract links once from a domain, will skip later hits from the same domain</li>
  <li>This filter is added to the query: content_type_norm:html AND links_domains:* AND url_type:slashpage</li>  
</ul> 

<h3>Gephi quick guide:</h3>

1) File -> Open -> (select csv file) -> next -> finished -> ok <br>
2) overview tab (default).See #nodes in top right corner.<br>
If more than 10.000 nodes, consider: filter tab (right), open topology, drag 'giant compontent' down to filters. Click 'filter'<br>
3)Select layout -> Yifan Hu. Click 'Run' and wait. Repeat and clicking 'Run' until graph looks 'nice'. This can take hours for graphs with 1 million nodes.<br>
4) Click statistics tab, click 'Network diameter' (wait), then click 'Modularity'<br>
5) Click nodes(left, top), click the color palette, click partition, select 'Modularity class' -> apply <br>
6) Click label size (tT), click ranking, select 'Betweeness centrality', click apply. (change min/max and rerun if fonts turn out to be too small or big)<br>
7) Click preview tab (top),  click "Refresh"<br>
8) To export click 'Export SVG/PDF/PNG'.<br> 
<br>

Note:<br>
For huge linkgraphs over 100000 nodes, you might need Gephi startet with 16 GM ram. For 1.000.000 nodes you need 32 GB ram.


</body>

</html>