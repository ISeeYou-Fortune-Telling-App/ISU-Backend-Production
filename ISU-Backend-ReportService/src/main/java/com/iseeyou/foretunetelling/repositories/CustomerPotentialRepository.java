package com.iseeyou.foretunetelling.repositories;

import com.iseeyou.foretunetelling.models.CustomerPotential;
import com.iseeyou.foretunetelling.utils.Constants;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface CustomerPotentialRepository extends MongoRepository<CustomerPotential, String> {
    @Query("{ 'customer_id': ?0, 'month': ?1, 'year': ?2 }")
    CustomerPotential findByCustomerIdAndMonthAndYear(String customerId, Integer month, Integer year);

    @Query("{ 'month': ?0, 'year': ?1 }")
    List<CustomerPotential> findAllByMonthAndYear(Integer month, Integer year);

    @Query("{ 'month': ?0, 'year': ?1 }")
    Page<CustomerPotential> findAllByMonthAndYear(Integer month, Integer year, Pageable pageable);

    @Query("{ 'year': ?0 }")
    List<CustomerPotential> findAllByYear(Integer year);

    List<CustomerPotential> findAllByCustomerId(String customerId);
}
