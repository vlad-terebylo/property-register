package com.tvo.propertyregister.repository.mongodb;

import com.mongodb.client.result.UpdateResult;
import com.tvo.propertyregister.model.owner.Owner;
import com.tvo.propertyregister.model.property.Property;
import com.tvo.propertyregister.repository.PropertyRepository;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.util.List;

import static java.util.Objects.requireNonNull;

@RequiredArgsConstructor
public class MongoDbPropertyRepository implements PropertyRepository {

    private static final String OWNERS_COLLECTION = "owners";

    private final MongoTemplate mongoTemplate;

    @Override
    public List<Property> findAll(int ownerId) {
        Query criteria = new Query(Criteria.where("id").is(ownerId));
        List<Owner> owners = mongoTemplate.find(criteria, Owner.class, OWNERS_COLLECTION);

        return owners.get(0).getProperties();
    }

    public boolean save(int ownerId, List<Property> updatedProperties) {
        Query criteria = new Query(Criteria.where("id").is(ownerId));
        updatedProperties.get(updatedProperties.size() - 1).setId(getNextPropertyId());

        Update update = new Update().set("properties", updatedProperties);
        UpdateResult result = mongoTemplate.updateFirst(criteria, update, Owner.class, OWNERS_COLLECTION);

        return result.getModifiedCount() > 0;
    }

    @Override
    public boolean update(Owner owner) {
        mongoTemplate.save(owner, OWNERS_COLLECTION);
        return true;
    }

    @Override
    public boolean remove(int ownerId, List<Property> properties) {
        Query criteria = new Query(Criteria.where("id").is(ownerId));

        Update update = new Update().set("properties", properties);
        UpdateResult result = mongoTemplate.updateFirst(criteria, update, Owner.class, OWNERS_COLLECTION);

        return result.getModifiedCount() > 0;
    }

    private int getNextPropertyId() {
        Query query = new Query(Criteria.where("_id").is("propertyId"));
        Update update = new Update().inc("sequence_value", 1);

        FindAndModifyOptions options = new FindAndModifyOptions();
        options.returnNew(true).upsert(true);

        Document counter = mongoTemplate.findAndModify(query, update, options, Document.class, "counters");

        return requireNonNull(counter).getInteger("sequence_value");
    }


}
