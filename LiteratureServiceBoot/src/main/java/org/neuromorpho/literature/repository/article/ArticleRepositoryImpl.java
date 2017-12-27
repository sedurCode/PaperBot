/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.neuromorpho.literature.repository.article;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.apache.lucene.search.spell.JaroWinklerDistance;
import org.bson.types.ObjectId;
import org.neuromorpho.literature.exceptions.DuplicatedException;
import org.neuromorpho.literature.model.article.Article;
import org.neuromorpho.literature.model.article.ArticleCollection;
import org.neuromorpho.literature.model.article.ArticleCollection.ArticleStatus;
import org.neuromorpho.literature.model.article.SearchPortal;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.group;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.match;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.project;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.sort;
import org.springframework.data.mongodb.core.query.Update;

@Repository
public class ArticleRepositoryImpl implements ArticleRepository {

    private final String positivesCollection = ArticleStatus.POSITIVE.getCollection();
    private final String toEvaluateCollection = ArticleStatus.TO_EVALUATE.getCollection();

    private final Integer pageSize = 50;
    private final org.slf4j.Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    MongoTemplate mongoOperations;

    @Override
    public String save(ArticleCollection article) {
        ArticleCollection oldArticle = this.existsArticle(article.getArticle());
        if (oldArticle != null) {
            throw new DuplicatedException(oldArticle.getArticleStatus().getStatus());
        }
        if (!article.getArticleStatus().getCollection().equals(toEvaluateCollection)) {
            article.getArticle().setEvaluatedDateToday();
        }
        mongoOperations.save(article.getArticle(), article.getArticleStatus().getCollection());

        return article.getArticle().getId().toString();
    }

    @Override
    public String saveOrUpdate(ArticleCollection article) {
        ArticleCollection oldArticle = this.existsArticle(article.getArticle());
        String id;
        if (oldArticle != null) {
            Article old = oldArticle.getArticle();
            Article newArticle = article.getArticle();
            //if article was in collection inaccessible update
            if (oldArticle.getArticleStatus().isInaccessible()) {
                this.update(old.getId().toString(), article.getArticleStatus());
            }
            // if new article has more data than saved article update
            Query query = new Query(Criteria.where("_id").is(old.getId()));
            Update update = new Update();
            if (!newArticle.isDoiNull() && old.isDoiNull()) {
                update.set("doi", newArticle.getDoi());
            }
            if (!newArticle.isPMIDNull() && old.isPMIDNull()) {
                update.set("pmid", newArticle.getPmid());
            }
            if (!newArticle.isPublishedDateNull() && old.isPublishedDateNull()) {
                update.set("publishedDate", newArticle.getPublishedDate());
            }
            if (!newArticle.isJournalNull() && old.isJournalNull()) {
                update.set("journal", newArticle.getJournal());
            }
            old.mergeAuthorData(newArticle.getAuthorList());
            update.set("authorList", old.getAuthorList());
            mongoOperations.updateFirst(query, update, oldArticle.getArticleStatus().getCollection());

            id = oldArticle.getArticle().getId().toString();
        } else {
            mongoOperations.save(article.getArticle(), article.getArticleStatus().getCollection());
            id = article.getArticle().getId().toString();

        }
        return id;
    }

    @Override
    public Map<String, Long> getSummary(Date date) {
        Map<String, Long> articlesNumbers = new HashMap();
        if (date != null) {
            Query query = new Query();
            query.addCriteria(Criteria.where("ocDate").lt(date));
            Long articles = mongoOperations.count(query, toEvaluateCollection);
            articlesNumbers.put("old", articles);

            query = new Query();
            query.addCriteria(Criteria.where("ocDate").gte(date));
            articles = mongoOperations.count(query, toEvaluateCollection);
            articlesNumbers.put("new", articles);
        }
        for (ArticleStatus status : ArticleStatus.values()) {
            Long articles = mongoOperations.count(null, status.getCollection());
            articlesNumbers.put(status.getStatus(), articles);
        }
        
        return articlesNumbers;
    }

    @Override
    public void delete(String id) {
        for (ArticleStatus status : ArticleStatus.values()) {
            Article article = mongoOperations.findById(id, Article.class, status.getCollection());
            if (article != null) {
                mongoOperations.remove(article);
            }
        }

    }

    @Override
    public void update(String id, ArticleStatus newCollection) {
        ArticleCollection articleOld = findById(id);
        log.debug("Updating collection article title: " + articleOld.getArticle().getTitle());
        if (newCollection != articleOld.getArticleStatus()) {

            if (articleOld.getArticleStatus().equals(ArticleStatus.TO_EVALUATE)) {
                articleOld.getArticle().setEvaluatedDate(new Date());
            }
            mongoOperations.save(articleOld.getArticle(), newCollection.getCollection());
            mongoOperations.remove(articleOld.getArticle(), articleOld.getArticleStatus().getCollection());
        }
    }

    @Override
    public void update(String id, Article articleNew) {
        ArticleCollection articleOld = findById(id);
        if (articleOld == null) {
            articleNew.setId(new ObjectId(id));
            this.save(new ArticleCollection(articleNew, ArticleStatus.TO_EVALUATE));
        } else {
             Article article = articleOld.getArticle();
            article.setPmid(articleNew.getArticle().getPmid());
            article.setDoi(articleNew.getArticle().getDoi());
            article.setLink(articleNew.getArticle().getLink());
            article.setTitle(articleNew.getArticle().getTitle());
            article.setPublishedDate(articleNew.getArticle().getPublishedDate());
            article.setJournal(articleNew.getArticle().getJournal());
            article.setAuthorList(articleNew.getArticle().getAuthorList());
            article.setDataUsage(articleNew.getArticle().getDataUsage());
            mongoOperations.save(article, articleOld.getArticleStatus().getCollection());
        }

    }


    @Override
    public void update(String id, SearchPortal searchPortal, String keyWord) {
        ArticleCollection articleOld = this.findById(id);
        Query query = new Query(Criteria.where("_id").is(id));
        Update update = new Update();
        if (articleOld.getArticle().getSearchPortal() == null) {//no search
            update.set("searchPortal", searchPortal);
        } else {
            articleOld.getArticle().updateSearchPortal(searchPortal, keyWord);
            update.set("searchPortal", articleOld.getArticle().getSearchPortal());
        }
        mongoOperations.updateFirst(query, update, articleOld.getArticleStatus().getCollection());

    }

    /*
    * Search in all the collections for a given article
     */
    @Override
    public ArticleCollection existsArticle(Article article) {
        for (ArticleStatus status : ArticleStatus.values()) {
            Article duplicatedArticle = this.existsArticleInCollection(article, status.getCollection());
            if (duplicatedArticle != null) {
                return new ArticleCollection(duplicatedArticle, status);
            }
        }
        return null;

    }

    private Article existsArticleInCollection(Article article, String collection) {

        //exact title
        Article duplicatedExactTitle = mongoOperations.findOne(Query.query(Criteria.where("title").is(article.getTitle())),
                Article.class, collection);
        if (duplicatedExactTitle != null) {
            return duplicatedExactTitle;
        }

        //in case no exact title but contains pmid
        if (article.getPmid() != null && !article.getPmid().isEmpty()) {
            Article duplicatePmid = mongoOperations.findOne(Query.query(Criteria.where("pmid").is(article.getPmid())),
                    Article.class, collection);
            if (duplicatePmid != null) {
                return duplicatePmid;

            }
        }
        //in case no exact title but contains doi
        if (article.getDoi() != null && !article.getDoi().isEmpty()) {
            Article duplicateDoi = mongoOperations.findOne(Query.query(Criteria.where("doi").is(article.getDoi())),
                    Article.class, collection);
            if (duplicateDoi != null) {
                return duplicateDoi;
            }

        }
        //similar title only for the ones without pmid & or doi
        if ((article.getPmid() == null || article.getPmid().isEmpty())
                && (article.getDoi() == null || article.getDoi().isEmpty())) {
            List<Article> duplicateTitleList = mongoOperations.findAll(Article.class, collection);
            for (Article articleTitle : duplicateTitleList) {
                JaroWinklerDistance jwDistance = new JaroWinklerDistance();
                Float distance = jwDistance.getDistance(article.getTitle(), articleTitle.getTitle());
                if (distance > 0.8) {
                    return articleTitle;
                }
            }
        }

        return null;
    }

    /*
    * Find article in all the DB by id
     */
    @Override
    public ArticleCollection findById(String id) {
        ArticleCollection articleCollection = null;
        for (ArticleStatus status : ArticleStatus.values()) {
            Article article = this.existsArticleByIdInCollection(id, status.getCollection());
            if (article != null) {
                articleCollection = new ArticleCollection(article, status);
            }
        }
        return articleCollection;

    }

    private Article existsArticleByIdInCollection(String id, String collection) {
        Article duplicate = mongoOperations.findById(id, Article.class, collection);
        return duplicate;
    }


    /*
    * From positive collection. What about others?
     */
    @Override
    public List<String> findFieldValues(String field) {

        Aggregation aggregation;
        if (field.equals("publishedDate") || field.equals("evaluatedDate")) {
            aggregation = newAggregation(
                    match(Criteria.where("dataUsage").is("DESCRIBING_NEURONS")),
                    project().and(field).project("year").as("value"),
                    group("value"),
                    sort(Sort.Direction.DESC, "value"));
        } else {
            aggregation = newAggregation(
                    project().and(field).as("value"),
                    group("value"),
                    sort(Sort.Direction.DESC, "value"));

        }
        AggregationResults<AggregationValue> groupResults = mongoOperations.aggregate(aggregation, positivesCollection, AggregationValue.class
        );
        List<AggregationValue> result = groupResults.getMappedResults();
        List<String> resultList = new ArrayList();
        if (result.get(0).getId() != null) {
            for (AggregationValue value : result) {
                resultList.add(value.getId());
                log.debug("field value found: " + value.getId());
            }
        }
        return resultList;
    }

    @Override
    public Page<Article> findByText(String text, ArticleStatus status, Integer pageStart) {
        PageRequest pageRequest = new PageRequest(pageStart, pageSize);

        Query query = new Query();

        //search in title, doi, pmid, authors
        if (text != null && !text.isEmpty()) {
            Map<String, String> pair = new HashMap();
            pair.put("pmid", text);
            pair.put("doi", text);
            pair.put("title", text);
            pair.put("authorList.name", text);
            pair.put("authorList.email", text);
            Criteria criteriaOr = getOrCriteriaList(pair);
            query.addCriteria(criteriaOr);
        }
        query.with(new Sort(Sort.Direction.DESC, "publishedDate"));
        query.skip(pageStart * pageSize);
        query.limit(pageSize);
        List<Article> articleList = mongoOperations.find(query, Article.class, status.getCollection());

        Long n = mongoOperations.count(query, Article.class, status.getCollection());
        Page<Article> articlePage = new PageImpl<>(articleList, pageRequest, n);
        return articlePage;

    }

    @Override
    public Page<Article> findByFieldQuery(Map<String, List<String>> fieldQuery,
            Integer pageStart) {
        PageRequest pageRequest = new PageRequest(pageStart, pageSize);
        Query query = new Query();
        String collection = positivesCollection;
        for (Map.Entry pair : fieldQuery.entrySet()) {
            List<String> valueList = (List<String>) pair.getValue();
            if (pair.getKey().equals("articleStatus")) {
                collection = ArticleStatus.valueOf(valueList.get(0)).getCollection();
            } else {
                Criteria criteriaOr = getOrCriteriaListExactMatch(pair);
                query.addCriteria(criteriaOr);
            }
        }

        query.with(
                new Sort(Sort.Direction.DESC, "publishedDate"));
        query.skip(pageStart
                * pageSize);
        query.limit(pageSize);

        List<Article> articleList = mongoOperations.find(query, Article.class, collection);

//        if (articleList.isEmpty()) {
//            articleList = mongoOperations.find(query, Article.class, negativesCollection);
//            for (Article article : articleList) {
//                article.addNegativeStatus();
//
//            }
//        }
        Long n = mongoOperations.count(query, Article.class, collection);
        Page<Article> articlePage = new PageImpl<>(articleList, pageRequest, n);
        return articlePage;

    }

    private Criteria getOrCriteriaList(Map<String, String> pairMap) {
        ArrayList<Criteria> criteriaList = new ArrayList();
        Criteria criteria = new Criteria();
        for (Map.Entry<String, String> entry : pairMap.entrySet()) {
            if (entry.getKey().equals("title")
                    || entry.getKey().equals("authorList.name")
                    || entry.getKey().equals("authorList.email")) {
                criteria = Criteria.where(entry.getKey()).
                        regex(Pattern.compile(entry.getValue(), Pattern.CASE_INSENSITIVE));
            } else {
                criteria = Criteria.where(entry.getKey()).is(entry.getValue());
            }
            criteriaList.add(criteria);
            criteria = new Criteria();
        }

        if (pairMap.size() > 1) {
            Criteria[] criteriaArr = new Criteria[criteriaList.size()];
            criteria.orOperator(criteriaList.toArray(criteriaArr));
        } else {
            criteria = criteriaList.get(0);
        }
        return criteria;
    }

    private Criteria getOrCriteriaList(Map.Entry pair) {
        ArrayList<Criteria> criteriaList = new ArrayList();
        Criteria criteria = new Criteria();
        for (String pairString : (List<String>) pair.getValue()) {
            if (pair.getKey().equals("publishedDate")) {
                criteria = Criteria.where(pair.getKey().toString())
                        .gte(this.getStartYearDate(pairString))
                        .lte(this.getEndYearDate(pairString));
            } else if (pair.getKey().equals("ltDate")) {
                criteria = Criteria.where("ocDate")
                        .lt(this.getFirstDay(pairString));
            } else if (pair.getKey().equals("gtDate")) {
                criteria = Criteria.where("ocDate")
                        .gte(this.getFirstDay(pairString));
            } else {
                criteria = Criteria.where(pair.getKey().toString()).regex(pairString);
            }
            criteriaList.add(criteria);
            criteria = new Criteria();
        }
        List<String> orValues = (List<String>) pair.getValue();

        if (orValues.size()
                > 1) {
            Criteria[] criteriaArr = new Criteria[criteriaList.size()];
            criteria.orOperator(criteriaList.toArray(criteriaArr));
        } else {
            criteria = criteriaList.get(0);
        }
        return criteria;
    }

    private Criteria getOrCriteriaListExactMatch(Map.Entry pair) {
        ArrayList<Criteria> criteriaList = new ArrayList();
        Criteria criteria = new Criteria();
        for (String pairString : (List<String>) pair.getValue()) {
            if (pair.getKey().equals("publishedDate")) {
                criteria = Criteria.where(pair.getKey().toString())
                        .gte(this.getStartYearDate(pairString))
                        .lte(this.getEndYearDate(pairString));
            } else if (pair.getKey().equals("ltDate")) {
                criteria = Criteria.where("ocDate")
                        .lt(this.getFirstDay(pairString));
            } else if (pair.getKey().equals("gtDate")) {
                criteria = Criteria.where("ocDate")
                        .gte(this.getFirstDay(pairString));
            } else {
                criteria = Criteria.where(pair.getKey().toString()).is(pairString);
            }
            criteriaList.add(criteria);
            criteria = new Criteria();
        }
        List<String> orValues = (List<String>) pair.getValue();

        if (orValues.size()
                > 1) {
            Criteria[] criteriaArr = new Criteria[criteriaList.size()];
            criteria.orOperator(criteriaList.toArray(criteriaArr));
        } else {
            criteria = criteriaList.get(0);
        }
        return criteria;
    }

    private Boolean isNumeric(String s) {
        return java.util.regex.Pattern.matches("\\d+", s);
    }

    private Date getStartYearDate(String year) {
        Calendar date = Calendar.getInstance();   //current date
        date.set(Calendar.SECOND, date.getActualMinimum(Calendar.SECOND));
        date.set(Calendar.MINUTE, date.getActualMinimum(Calendar.MINUTE));
        date.set(Calendar.HOUR_OF_DAY, date.getActualMinimum(Calendar.HOUR_OF_DAY));
        date.set(Calendar.DAY_OF_MONTH, date.getActualMinimum(Calendar.DAY_OF_MONTH));
        date.set(Calendar.MONTH, date.getActualMinimum(Calendar.MONTH));
        date.set(Calendar.YEAR, Integer.parseInt(year));
        return date.getTime();
    }

    private Date getEndYearDate(String year) {
        Calendar date = Calendar.getInstance();   //current date
        date.set(Calendar.SECOND, date.getActualMaximum(Calendar.SECOND));
        date.set(Calendar.MINUTE, date.getActualMaximum(Calendar.MINUTE));
        date.set(Calendar.HOUR_OF_DAY, date.getActualMaximum(Calendar.HOUR_OF_DAY));
        date.set(Calendar.DAY_OF_MONTH, date.getActualMaximum(Calendar.DAY_OF_MONTH));
        date.set(Calendar.DAY_OF_MONTH, date.getActualMaximum(Calendar.DAY_OF_MONTH));
        date.set(Calendar.MONTH, date.getActualMaximum(Calendar.MONTH));
        date.set(Calendar.YEAR, Integer.parseInt(year));
        return date.getTime();
    }

    public Date getFirstDay(String dateStr) {
        Date result = null;
        if (dateStr != null) {
            String[] dateListStr = dateStr.split("-");

            Calendar date = Calendar.getInstance();   //current date
            date.set(Calendar.SECOND, date.getActualMinimum(Calendar.SECOND));
            date.set(Calendar.MINUTE, date.getActualMinimum(Calendar.MINUTE));
            date.set(Calendar.HOUR_OF_DAY, date.getActualMinimum(Calendar.HOUR_OF_DAY));
            date.set(Calendar.DAY_OF_MONTH, date.getActualMinimum(Calendar.DAY_OF_MONTH));
            date.set(Calendar.MONTH, Integer.parseInt(dateListStr[1]) - 1);
            date.set(Calendar.YEAR, Integer.parseInt(dateListStr[0]));
            result = date.getTime();
        }
        return result;
    }

    @Override
    public ArticleCollection findByPMID(String pmid) {
        Article article = new Article();
        article.setPmid(pmid);
        article.setDoi(pmid);
        article.setTitle(pmid);
        return this.existsArticle(article);
    }

}
