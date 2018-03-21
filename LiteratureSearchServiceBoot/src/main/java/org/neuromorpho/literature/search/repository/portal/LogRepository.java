/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.neuromorpho.literature.search.repository.portal;


import java.util.List;
import org.bson.types.ObjectId;
import org.neuromorpho.literature.search.model.portal.Log;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LogRepository extends MongoRepository<Log, ObjectId> {
 
    public List<Log> findFirst10ByOrderByStartDesc();
}
