package org.paperbot.literature.search.communication;


import org.paperbot.literature.search.model.article.Article;
import org.paperbot.literature.search.model.article.Search;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class LiteratureConnection {

    @Value("${uriLiterature}")
    private String uri;
    
   
    
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    public ArticleResponse saveArticle(Article article, String collection) {
        String url = uri + "/" + collection;
        log.debug("Creating rest connection for URI: " + url);
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<ArticleResponse> response = restTemplate.postForEntity(
                url, article, ArticleResponse.class);
        return response.getBody();
    }
    
    public void updateCollection(String id, String oldCollection, String newCollection) {
        String url = uri + "/status/" + id + "?oldArticleStatus=" + oldCollection + "&newArticleStatus=" + newCollection;
        log.debug("Creating rest connection for URI: " + url);
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.put(url, null);
    }
    
     public void saveSearchPortal(String id, String source, String keyWord) {
        
        String url = uri + "/search/" + id;
        log.debug("Creating rest connection for URI: " + url);
        RestTemplate restTemplate = new RestTemplate();
        
        restTemplate.put(url, new Search(source, keyWord));
    }
     

}
