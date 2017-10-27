package org.neuromorpho.literature.search.communication;


import org.neuromorpho.literature.search.model.article.Article;
import org.neuromorpho.literature.search.model.article.Search;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class LiteratureConnection {

    @Value("${uriLiteratureService}")
    private String uri;
    
    @Value("${uriPubMedService}")
    private String uriPubMed;
    
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    public ArticleResponse saveArticle(Article article, Boolean inaccessible, String collection) {
        if (inaccessible && collection.equals("To evaluate")){
            collection = "Inaccessible";
        } 
        String url = uri + "/search/" + collection;
        log.debug("Creating rest connection for URI: " + url);
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<ArticleResponse> response = restTemplate.postForEntity(
                url, article, ArticleResponse.class);
        return response.getBody();
    }
    
     public void saveSearchPortal(String id, Search search) {
        
        String url = uri + "/search/" + id;
        log.debug("Creating rest connection for URI: " + url);
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.put(url, search);
    }
     
    public Article findPubMed(String pmid) {
        String url = uriPubMed + "/literature/pubmed/" + pmid;
        log.debug("Creating rest connection for URI: " + url);
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<Article> responseEntity = 
                restTemplate.getForEntity(url, Article.class);
        return responseEntity.getBody();
    }

}
