package io.github.athirson010.adapters.out.persistence.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PolicyRequestMongoRepository extends MongoRepository<PolicyRequestDocument, String> {

    Optional<PolicyRequestDocument> findByCustomerId(String customerId);

    Optional<PolicyRequestDocument> findByProductId(String productId);

    boolean existsByCustomerId(String customerId);
}
