package io.github.athirson010.adapters.out.persistence.mongo.repository;

import io.github.athirson010.adapters.out.persistence.mongo.document.PolicyProposalEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PolicyProposalMongoRepository extends MongoRepository<PolicyProposalEntity, String> {

    Optional<PolicyProposalEntity> findByProposalNumber(String proposalNumber);

    List<PolicyProposalEntity> findByCustomerId(String customerId);

    List<PolicyProposalEntity> findByCustomerIdAndStatus(String customerId, String status);

    List<PolicyProposalEntity> findByStatus(String status);

    boolean existsByProposalNumber(String proposalNumber);

    @Query("{ 'customerId': ?0, 'status': { $in: ?1 } }")
    List<PolicyProposalEntity> findByCustomerIdAndStatusIn(String customerId, List<String> statuses);

    @Query("{ 'category': ?0, 'status': ?1 }")
    List<PolicyProposalEntity> findByCategoryAndStatus(String category, String status);

    long countByStatus(String status);

    long countByCustomerId(String customerId);
}
