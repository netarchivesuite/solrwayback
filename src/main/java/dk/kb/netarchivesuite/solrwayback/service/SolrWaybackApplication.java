package dk.kb.netarchivesuite.solrwayback.service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

import dk.kb.netarchivesuite.solrwayback.service.exception.ServiceExceptionMapper;


public class SolrWaybackApplication extends Application {

    public Set<Class<?>> getClasses() {
        return new HashSet<>(Arrays.asList(
            JacksonJsonProvider.class,
            SolrWaybackResource.class,
            SolrWaybackResourceWeb.class,
            SolrWaybackMementoAPI.class,
            ServiceExceptionMapper.class
            ));
    }


}