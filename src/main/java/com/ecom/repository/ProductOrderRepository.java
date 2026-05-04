package com.ecom.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ecom.model.ProductOrder;

import jakarta.transaction.Transactional;

public interface ProductOrderRepository extends JpaRepository<ProductOrder, Integer> {

	List<ProductOrder> findByUserId(Integer userId);

	ProductOrder findByOrderId(String orderId);
	
	@Modifying
    @Transactional
    @Query("DELETE FROM ProductOrder po WHERE po.product.id = :pid")
    void deleteOrderByProductId(@Param("pid") Integer pid);

}
