package org.neuromorpho.literature.controller;

import java.util.Date;
import java.util.List;
import org.neuromorpho.literature.model.article.Portal;

public class ArticleDto implements java.io.Serializable {

    private Date evaluatedDate;
    private String id;
    private String pmid;
    private String title;
    private String journal;
    private String doi;
    private Date publishedDate;
    private Date ocDate;
    private List<AuthorDto> authorList;
    private String link;
    private List<Portal> searchPortal;
    private String articleStatus;
    private List<String> usage;
    private String abstact;

    public ArticleDto(String id) {
        this.id = id;
    }

    public ArticleDto() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPmid() {
        return pmid;
    }

    public void setPmid(String pmid) {
        this.pmid = pmid;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getJournal() {
        return journal;
    }

    public void setJournal(String journal) {
        this.journal = journal;
    }

    public String getDoi() {
        return doi;
    }

    public void setDoi(String doi) {
        this.doi = doi;
    }

    public Date getPublishedDate() {
        return publishedDate;
    }

    public void setPublishedDate(Date publishedDate) {
        this.publishedDate = publishedDate;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public List<AuthorDto> getAuthorList() {
        return authorList;
    }

    public void setAuthorList(List<AuthorDto> authorList) {
        this.authorList = authorList;
    }

    public Date getEvaluatedDate() {
        return evaluatedDate;
    }

    public void setEvaluatedDate(Date evaluatedDate) {
        this.evaluatedDate = evaluatedDate;
    }

    public List<Portal> getSearchPortal() {
        return searchPortal;
    }

    public void setSearchPortal(List<Portal> searchPortal) {
        this.searchPortal = searchPortal;
    }

    public String getArticleStatus() {
        return articleStatus;
    }

    public void setArticleStatus(String articleStatus) {
        this.articleStatus = articleStatus;
    }

    public List<String> getUsage() {
        return usage;
    }

    public void setUsage(List<String> usage) {
        this.usage = usage;
    }

    public String getAbstact() {
        return abstact;
    }

    public void setAbstact(String abstact) {
        this.abstact = abstact;
    }

    public Date getOcDate() {
        return ocDate;
    }

    public void setOcDate(Date ocDate) {
        this.ocDate = ocDate;
    }

    @Override
    public String toString() {
        return "ArticleDto{" + "evaluatedDate=" + evaluatedDate + ", id=" + id
                + ", pmid=" + pmid + ", title=" + title + ", journal=" + journal
                + ", doi=" + doi + ", publishedDate=" + publishedDate + ", authorList="
                + this.toString(authorList) + ", link=" + link + '}';
    }

    public String toString(List<AuthorDto> authorList) {
        String authorLstStr = "";
        if (authorList != null) {
            for (AuthorDto authorDto : authorList) {
                authorLstStr = authorLstStr + authorDto.toString();
            }
        }
        return authorLstStr;
    }

}
