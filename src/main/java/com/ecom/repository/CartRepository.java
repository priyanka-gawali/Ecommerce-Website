package com.ecom.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.repository.query.Param;

import com.ecom.model.Cart;

public interface CartRepository extends JpaRepository<Cart, Integer> {

	public Cart findByProductIdAndUserId(Integer productId, Integer userId);

	public Integer countByUserId(Integer userId);

	public List<Cart> findByUserId(Integer userId);

	// ✅ ADD THIS METHOD INSIDE INTERFACE
	@Modifying
	@Transactional
	@Query("DELETE FROM Cart c WHERE c.product.id = :pid")
	void deleteCartByProductId(@Param("pid") Integer pid);
}