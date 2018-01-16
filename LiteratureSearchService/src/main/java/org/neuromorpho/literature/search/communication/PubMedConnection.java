package org.neuromorpho.literature.search.communication;


import org.neuromorpho.literature.search.model.article.Article;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class PubMedConnection {

    
    @Value("${uriPubMedService}")
    private String uri;
    
    private final Logger log = LoggerFactory.getLogger(this.getClass());
  
     public String findTitleFromPMID(String title, String db) {
        String url = uri + "/pmid?title=" + title + "&db=" + db;
        log.debug("Creating rest connection for URI: " + url);
        RestTemplate restTemplate = new RestTemplate();
        String response = restTemplate.getForObject(url, String.class);
        return response;
    }
     
     
    public Article findArticleFromPMID(String pmid, String db) {
        String url = uri + "?pmid=" + pmid + "&db=" + db;
        log.debug("Creating rest connection for URI: " + url);
        RestTemplate restTemplate = new RestTemplate();
        Article response = restTemplate.getForObject(url, Article.class);
        return response;
    }

}