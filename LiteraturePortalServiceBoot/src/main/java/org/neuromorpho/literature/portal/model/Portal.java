package org.neuromorpho.literature.portal.model;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "portal")
public class Portal {

    @Id
    private ObjectId id;

    @Indexed(unique = true)
    private String name;
    private String url;
    private String base;
    private Integer searchPeriod;
    private Boolean active;
    private String db;
    private String apiUrl;
    private String apiUrl2;

    public Portal(String name) {
        this.name = name;
    }

    public Portal() {
    }

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Integer getSearchPeriod() {
        return searchPeriod;
    }

    public void setSearchPeriod(Integer searchPeriod) {
        this.searchPeriod = searchPeriod;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public String getDb() {
        return db;
    }

    public void setDb(String db) {
        this.db = db;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public Boolean hasAPI() {
        return apiUrl != null;
    }

    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    public String getApiUrl2() {
        return apiUrl2;
    }

    public void setApiUrl2(String apiUrl2) {
        this.apiUrl2 = apiUrl2;
    }

    public String getBase() {
        return base;
    }

    public void setBase(String base) {
        this.base = base;
    }

}
