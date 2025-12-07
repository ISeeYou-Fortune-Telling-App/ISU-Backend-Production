package com.iseeyou.fortunetelling.repository.certificate;

import com.iseeyou.fortunetelling.entity.certificate.Certificate;
import com.iseeyou.fortunetelling.util.Constants;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CertificateRepository extends JpaRepository<Certificate, UUID>, JpaSpecificationExecutor<Certificate> {
    @EntityGraph(attributePaths = {"certificateCategories.knowledgeCategory", "seer"})
    Page<Certificate> findAllBySeer_Id(UUID seerId, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"certificateCategories.knowledgeCategory", "seer"})
    Page<Certificate> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"certificateCategories.knowledgeCategory", "seer"})
    Page<Certificate> findAllByStatus(Constants.CertificateStatusEnum status, Pageable pageable);

    @EntityGraph(attributePaths = {"certificateCategories.knowledgeCategory", "seer"})
    @Query("SELECT DISTINCT c FROM Certificate c WHERE (:name IS NULL OR LOWER(c.certificateName) LIKE LOWER(CONCAT('%', :name, '%')) OR LOWER(c.seer.fullName) LIKE LOWER(CONCAT('%', :name, '%')))")
    Page<Certificate> findAllByNameFilter(@Param("name") String name, Pageable pageable);

    @EntityGraph(attributePaths = {"certificateCategories.knowledgeCategory", "seer"})
    @Query("SELECT DISTINCT c FROM Certificate c WHERE c.status = :status AND (:name IS NULL OR LOWER(c.certificateName) LIKE LOWER(CONCAT('%', :name, '%')) OR LOWER(c.seer.fullName) LIKE LOWER(CONCAT('%', :name, '%')))")
    Page<Certificate> findAllByStatusAndNameFilter(@Param("status") Constants.CertificateStatusEnum status, @Param("name") String name, Pageable pageable);

    @EntityGraph(attributePaths = {"certificateCategories.knowledgeCategory", "seer"})
    @Query("SELECT c FROM Certificate c JOIN c.certificateCategories cc WHERE c.seer.id = :seerId AND cc.knowledgeCategory.id = :categoryId")
    Page<Certificate> findBySeerIdAndCategoryId(@Param("seerId") UUID seerId, @Param("categoryId") UUID categoryId, Pageable pageable);

    @EntityGraph(attributePaths = {"certificateCategories.knowledgeCategory", "seer"})
    @Query("SELECT c FROM Certificate c JOIN c.certificateCategories cc WHERE cc.knowledgeCategory.id = :categoryId")
    Page<Certificate> findByCategoryId(@Param("categoryId") UUID categoryId, Pageable pageable);

    @EntityGraph(attributePaths = {"certificateCategories.knowledgeCategory", "seer"})
    @Query("SELECT c FROM Certificate c WHERE c.id = :id")
    Optional<Certificate> findByIdWithCategories(@Param("id") UUID id);

    // Statistics methods
    long countByStatus(Constants.CertificateStatusEnum status);
}
