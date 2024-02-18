package com.github.vitor.rinha2024q1;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionRepository extends JpaRepository<TransactionEntity, Integer> {
	
	List<TransactionEntity> findFirst10ByClientIdOrderByCreatedAtDesc(Integer id);
}
