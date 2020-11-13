<!DOCTYPE html>
<html>
<head>
<title>SolrWayback frontend API</title>
</head>

<h1>SolrWayback frontend API </h1>   
  Build: Version:${pom.version} <br>
  Build time:${build.time}<br><br>


<h2>
 Service method for frontend.<br>
 All services are located under: /services/frontend/
 </h2>

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
        <td>solr/search/results</td>
        <td>
        query (String)<br>
        filterQuery (String)<br>
        grouping (boolean)<br>
        revisists (boolean)<br>
        start (integer)
        </td>
        <td>
           Solr response with results and no facets (JSON).<br>
           Only few key fields returned for each document.
        </td>
    </tr>  
        
    <tr>
        <td>solr/search/facets (NOT IMPLEMENTET YET)</td>
        <td>
        query (String)<br>
        filterQuery (String)<br>        
        revisists (boolean)        
        </td>
        <td>
           Solr response with facets and no results (JSON)
        </td>
    </tr>
    
    
    <tr>
        <td>solr/idlookup</td>
        <td>
         id (String)         
        </td>
        <td>
          Solr response with all fields for that document (JSON)
        </td>
    </tr>
    
    
    <tr>
        <td>properties/solrwaybackweb</td>
        <td>
         (none)         
        </td>
        <td>
         Map with (key,value) pairs.<br>
         Returns all properties defined in solrwaybackweb.properties
        </td>
    </tr>
    
    </tbody>
</table>

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

<br>
<table class="table" border="1">
    <caption><strong>JSON objects</strong></caption>
    <thead>
    <tr>
        <th>Object</th>
        <th>attributes</th>
    </tr>     
    
    </thead>
    <tbody>   
     </tbody>
</table>    

    

<body>
</body>
</html>

