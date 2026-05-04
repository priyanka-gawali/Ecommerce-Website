package com.ecom.service.Impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import com.ecom.model.Cart;
import com.ecom.model.Product;
import com.ecom.model.UserDtls;
import com.ecom.repository.CartRepository;
import com.ecom.repository.ProductRepository;
import com.ecom.repository.UserRepository;
import com.ecom.service.CartService;

@Service
public class CartServiceImpl implements CartService {

	@Autowired
	private CartRepository cartRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ProductRepository productRepository;

	@Override
	public Cart saveCart(Integer productId, Integer userId) {

	    UserDtls userDtls = userRepository.findById(userId).get();
	    Product product = productRepository.findById(productId).get();

	    // ✅ SAFE PRICE (IMPORTANT)
	    Double price = (product.getDiscountPrice() != null) 
	            ? product.getDiscountPrice() 
	            : product.getPrice();

	    Cart cartStatus = cartRepository.findByProductIdAndUserId(productId, userId);

	    Cart cart = null;

	    if (ObjectUtils.isEmpty(cartStatus)) {
	        cart = new Cart();
	        cart.setProduct(product);
	        cart.setUser(userDtls);
	        cart.setQuantity(1);
	        cart.setTotalPrice(price);
	    } else {
	        cart = cartStatus;
	        cart.setQuantity(cart.getQuantity() + 1);
	        cart.setTotalPrice(cart.getQuantity() * price);
	    }

	    return cartRepository.save(cart);
	}
	@Override
	public List<Cart> getCartsByUser(Integer userId) {

	    List<Cart> carts = cartRepository.findByUserId(userId);

	    Double totalOrderPrice = 0.0;
	    List<Cart> updateCarts = new ArrayList<>();

	    for (Cart c : carts) {

	        Double price = (c.getProduct().getDiscountPrice() != null) 
	                ? c.getProduct().getDiscountPrice() 
	                : c.getProduct().getPrice();

	        Double totalPrice = price * c.getQuantity();

	        c.setTotalPrice(totalPrice);

	        totalOrderPrice = totalOrderPrice + totalPrice;
	        c.setTotalOrderPrice(totalOrderPrice);

	        updateCarts.add(c);
	    }

	    return updateCarts;
	}

	@Override
	public Integer getCountCart(Integer userId) {
		Integer countByUserId = cartRepository.countByUserId(userId);
		return countByUserId;
	}

	@Override
	public void updateQuantity(String sy, Integer cid) {

		Cart cart = cartRepository.findById(cid).get();
		int updateQuantity;

		if (sy.equalsIgnoreCase("de")) {
			updateQuantity = cart.getQuantity() - 1;

			if (updateQuantity <= 0) {
				cartRepository.delete(cart);
			} else {
				cart.setQuantity(updateQuantity);
				cartRepository.save(cart);
			}

		} else {
			updateQuantity = cart.getQuantity() + 1;
			cart.setQuantity(updateQuantity);
			cartRepository.save(cart);
		}

	}
	
	@Override
	public void deleteCartByProductId(Integer pid) {
	    cartRepository.deleteCartByProductId(pid);
	}

}
