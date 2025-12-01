package com.iseeyou.foretunetelling.repositories;

import com.iseeyou.foretunetelling.models.SeerPerformance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SeerPerformanceRepository extends MongoRepository<SeerPerformance, String> {
    @Query("{ 'seer_id': ?0, 'month': ?1, 'year': ?2 }")
    SeerPerformance findBySeerIdAndMonthAndYear(String seerId, Integer month, Integer year);

    @Query("{ 'month': ?0, 'year': ?1 }")
    List<SeerPerformance> findAllByMonthAndYear(Integer month, Integer year);

    @Query("{ 'month': ?0, 'year': ?1 }")
    Page<SeerPerformance> findAllByMonthAndYear(Integer month, Integer year, Pageable pageable);

    @Query("{ 'year': ?0 }")
    List<SeerPerformance> findAllByYear(Integer year);

    List<SeerPerformance> findAllBySeerId(String seerId);
}
