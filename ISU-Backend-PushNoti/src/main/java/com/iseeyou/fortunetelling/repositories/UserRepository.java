package com.iseeyou.fortunetelling.repositories;

import com.iseeyou.fortunetelling.models.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends MongoRepository<User, String> {
    // TODO: QUESTION: Database has duplicate userId records. Current implementation
    // returns first match.
    // Consider cleanup: (1) Keep earliest record, (2) Keep record with most tokens,
    // (3) Merge all tokens
    Optional<User> findFirstByUserId(String userId);
}
