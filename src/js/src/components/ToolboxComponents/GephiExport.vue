<template>
  <div class="gephiExportContainer">
    <h2 class="toolboxHeadline">
      Link Grap Gephi Export
    </h2>
    <div class="gephiExportInterface">
      <div class="gephiQueryContainer">
        <h3>Query</h3>
        <textarea />
        <button>Generate gephi export</button>
        <div>
          <h3>Query examples</h3>
          <table border="1">
            <tbody>
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
            </tbody>
          </table>
        </div>
        <div>
          <h3>
            Limitations
          </h3>
          <ul>
            <li>Extraction will stop after 1 million different domains has been extrated.</li>
            <li>Only extract links from 'slashpage' of a given domain: etc http://test.dk/ or http://test.dk/index.html</li>
            <li>Only extract links once from a domain, will skip later hits from the same domain</li>
            <li>This filter is added to the query: content_type_norm:html AND links_domains:* AND url_type:slashpage</li>
          </ul>
        </div>
      </div>
      <div class="gephiGuideContainer">
        <h3>Gephi quick guide</h3>
        <p><span class="highlightText">1)</span> File -> Open -> (select csv file) -> next -> finished -> ok.</p>
        <br>
        <p><span class="highlightText">2)</span> overview tab (default). See #nodes in top right corner.</p>
        <p>If more than 10.000 nodes, consider: filter tab (right), open topology, drag 'giant compontent' down to filters. Click 'filter'.</p>
        <br>
        <p><span class="highlightText">3)</span> Select layout -> Yifan Hu. Click 'Run' and wait. Repeat and clicking 'Run' until graph looks 'nice'. This can take hours for graphs with 1 million nodes.</p>
        <br>
        <p><span class="highlightText">4)</span> Click statistics tab, click 'Network diameter' (wait), then click 'Modularity'.</p>
        <br>
        <p><span class="highlightText">5)</span> Click nodes(left, top), click the color palette, click partition, select 'Modularity class' -> apply.</p>
        <br>
        <p><span class="highlightText">6)</span> Click label size (tT), click ranking, select 'Betweeness centrality', click apply. (change min/max and rerun if fonts turn out to be too small or big).</p>
        <br>
        <p><span class="highlightText">7)</span> Click preview tab (top), click "Refresh".</p>
        <br>
        <p><span class="highlightText">8)</span> To export click 'Export SVG/PDF/PNG'.</p>
        <br>
        <p>
          Note:
          For huge linkgraphs over 100000 nodes, you might need Gephi startet with 16 GM ram. For 1.000.000 nodes you need 32 GB ram.
        </p>
      </div>
    </div>
  </div>
</template>

<script>

export default {
  name: 'GephiExport',
  data() {
    return {
    }
  },
  mounted () {

  },
  methods: {
  }
}
</script>
