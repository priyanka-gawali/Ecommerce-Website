package com.ecom.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.multipart.MultipartFile;

import com.ecom.model.*;
import com.ecom.service.*;
import com.ecom.util.CommonUtil;
import com.ecom.util.OrderStatus;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/admin")
public class AdminController {

	@Autowired
	private CategoryService categoryService;

	@Autowired
	private ProductService productService;

	@Autowired
	private UserService userService;

	@Autowired
	private CartService cartService;

	@Autowired
	private OrderService orderService;

	@Autowired
	private CommonUtil commonUtil;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@ModelAttribute
	public void getUserDetails(Principal p, Model m) {
		if (p != null) {
			String email = p.getName();
			UserDtls userDtls = userService.getUserByEmail(email);
			m.addAttribute("user", userDtls);
			Integer countCart = cartService.getCountCart(userDtls.getId());
			m.addAttribute("countCart", countCart);
		}

		List<Category> allActiveCategory = categoryService.getAllActiveCategory();
		m.addAttribute("categorys", allActiveCategory);
	}

	
	
	@GetMapping("/")
	public String index(Model m) {
		
		m.addAttribute("title", "Admin Dashboard");

	    long productCount = productService.getAllProducts().size();

	    long userCount = userService.getUsersByRole("ROLE_USER").size();

	    long orderCount = orderService.getAllOrders().size();

	    m.addAttribute("productCount", productCount);
	    m.addAttribute("userCount", userCount);
	    m.addAttribute("orderCount", orderCount);

	    return "admin/index";
	}

	@GetMapping("/loadAddProduct")
	public String loadAddProduct(Model m) {
		m.addAttribute("title", "Add Products");
		m.addAttribute("categories", categoryService.getAllCategory());
		return "admin/add_product";
	}

	// ✅ Category WITHOUT pagination
	@GetMapping("/category")
	public String category(Model m) {
		m.addAttribute("title", "Category");
		m.addAttribute("categorys", categoryService.getAllCategory());
		return "admin/category";
	}

	@PostMapping("/saveCategory")
	public String saveCategory(@ModelAttribute Category category, @RequestParam("file") MultipartFile file,
			HttpSession session) throws IOException {

		String fileName = file.isEmpty() ? "default.jpg"
				: System.currentTimeMillis() + "_" + file.getOriginalFilename();

		category.setImageName(fileName);

		if (categoryService.existCategory(category.getName())) {
			session.setAttribute("errorMsg", "Category Name already exists");
		} else {
			Category saveCategory = categoryService.saveCategory(category);

			if (ObjectUtils.isEmpty(saveCategory)) {
				session.setAttribute("errorMsg", "Not saved ! internal server error");
			} else {

				// ✅ NEW CODE STARTS HERE
				if (!file.isEmpty()) {
					String uploadDir = "C:/ecom_upload/category_img/";

					File uploadPath = new File(uploadDir);
					if (!uploadPath.exists()) {
						uploadPath.mkdirs(); // auto create folder
					}

					Path path = Paths.get(uploadDir + fileName);
					Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
				}
				// ✅ NEW CODE ENDS HERE

				session.setAttribute("succMsg", "Saved successfully");
			}
		}

		return "redirect:/admin/category";
	}

	@GetMapping("/deleteCategory/{id}")
	public String deleteCategory(@PathVariable int id, HttpSession session) {
		if (categoryService.deleteCategory(id)) {
			session.setAttribute("succMsg", "category delete success");
		} else {
			session.setAttribute("errorMsg", "something wrong on server");
		}
		return "redirect:/admin/category";
	}

	@GetMapping("/loadEditCategory/{id}")
	public String loadEditCategory(@PathVariable int id, Model m) {
		m.addAttribute("category", categoryService.getCategoryById(id));
		return "admin/edit_category";
	}

	@PostMapping("/updateCategory")
	public String updateCategory(@ModelAttribute Category category, @RequestParam("file") MultipartFile file,
			HttpSession session) throws IOException {

		Category oldCategory = categoryService.getCategoryById(category.getId());
		String imageName = file.isEmpty() ? oldCategory.getImageName() : file.getOriginalFilename();

		oldCategory.setName(category.getName());
		oldCategory.setIsActive(category.getIsActive());
		oldCategory.setImageName(imageName);

		Category updateCategory = categoryService.saveCategory(oldCategory);

		if (!ObjectUtils.isEmpty(updateCategory)) {
			if (!file.isEmpty()) {
				File saveFile = new ClassPathResource("static/img").getFile();
				Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + "category_img" + File.separator
						+ file.getOriginalFilename());

				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
			}
			session.setAttribute("succMsg", "Category update success");
		} else {
			session.setAttribute("errorMsg", "something wrong on server");
		}

		return "redirect:/admin/loadEditCategory/" + category.getId();
	}

	// ✅ Products WITHOUT pagination
	@GetMapping("/products")
	public String loadViewProduct(Model m, @RequestParam(defaultValue = "") String ch) {
		m.addAttribute("title", "View Products");

		List<Product> products = (ch != null && ch.length() > 0) ? productService.searchProduct(ch)
				: productService.getAllProducts();

		m.addAttribute("products", products);
		return "admin/products";
	}

	@PostMapping("/saveProduct")
	public String saveProduct(@ModelAttribute Product product, @RequestParam("file") MultipartFile file,
			HttpSession session) throws IOException {

		String fileName = file.isEmpty() ? "default.jpg"
				: System.currentTimeMillis() + "_" + file.getOriginalFilename();

		product.setImage(fileName);

		// ✅ ADD THIS BLOCK HERE
		double price = product.getPrice();
		int discount = product.getDiscount();

		double discountAmount = price * discount / 100;
		double discountPrice = price - discountAmount;

		product.setDiscountPrice(discountPrice);

		// THEN SAVE
		Product saveProduct = productService.saveProduct(product);

		if (ObjectUtils.isEmpty(saveProduct)) {
			session.setAttribute("errorMsg", "Product not saved! Server error");
		} else {

			if (!file.isEmpty()) {
				String uploadDir = "C:/ecom_upload/product_img/";

				File uploadPath = new File(uploadDir);
				if (!uploadPath.exists()) {
					uploadPath.mkdirs();
				}

				Path path = Paths.get(uploadDir + fileName);
				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
			}

			session.setAttribute("succMsg", "Product added successfully");
		}

		return "redirect:/admin/loadAddProduct";
	}

	@PostMapping("/updateProduct")
	public String updateProduct(@ModelAttribute Product product, @RequestParam("file") MultipartFile file,
			HttpSession session) throws IOException {

		Product oldProduct = productService.getProductById(product.getId());

		String imageName = file.isEmpty() ? oldProduct.getImage()
				: System.currentTimeMillis() + "_" + file.getOriginalFilename();

		oldProduct.setTitle(product.getTitle());
		oldProduct.setDescription(product.getDescription());
		oldProduct.setCategory(product.getCategory());
		oldProduct.setPrice(product.getPrice());
		oldProduct.setDiscount(product.getDiscount());
		oldProduct.setStock(product.getStock());
		oldProduct.setIsActive(product.getIsActive());
		

		oldProduct.setImage(imageName);

		// ✅ ADD THIS BLOCK RIGHT HERE
		double price = product.getPrice();
		int discount = product.getDiscount();

		double discountAmount = price * discount / 100;
		double discountPrice = price - discountAmount;

		oldProduct.setDiscountPrice(discountPrice);

		// ✅ THEN SAVE
		Product updateProduct = productService.saveProduct(oldProduct);
		

		if (!ObjectUtils.isEmpty(updateProduct)) {

			if (!file.isEmpty()) {
				String uploadDir = "C:/ecom_upload/product_img/";

				File uploadPath = new File(uploadDir);
				if (!uploadPath.exists()) {
					uploadPath.mkdirs();
				}

				Path path = Paths.get(uploadDir + imageName);
				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
			}

			session.setAttribute("succMsg", "Product updated successfully");
		} else {
			session.setAttribute("errorMsg", "Update failed");
		}

		return "redirect:/admin/editProduct/" + product.getId();
	}

	@GetMapping("/deleteProduct/{id}")
	public String deleteProduct(@PathVariable int id, HttpSession session) {
		if (productService.deleteProduct(id)) {
			session.setAttribute("succMsg", "Product delete success");
		} else {
			session.setAttribute("errorMsg", "Something wrong on server");
		}
		return "redirect:/admin/products";
	}

	@GetMapping("/editProduct/{id}")
	public String editProduct(@PathVariable int id, Model m) {
		m.addAttribute("title", "Edit Product");
		m.addAttribute("product", productService.getProductById(id));
		m.addAttribute("categories", categoryService.getAllCategory());
		return "admin/edit_product";
	}

	// ✅ Orders WITHOUT pagination
	@GetMapping("/orders")
	public String getAllOrders(Model m) {
		m.addAttribute("title", "Orders");
		m.addAttribute("orders", orderService.getAllOrders());
		m.addAttribute("srch", false);
		return "/admin/orders";
	}

	@GetMapping("/search-order")
	public String searchProduct(@RequestParam String orderId, Model m, HttpSession session) {

		if (orderId != null && orderId.length() > 0) {
			ProductOrder order = orderService.getOrdersByOrderId(orderId.trim());

			if (ObjectUtils.isEmpty(order)) {
				session.setAttribute("errorMsg", "Incorrect orderId");
				m.addAttribute("orderDtls", null);
			} else {
				m.addAttribute("orderDtls", order);
			}

			m.addAttribute("srch", true);
		} else {
			m.addAttribute("orders", orderService.getAllOrders());
			m.addAttribute("srch", false);
		}

		return "/admin/orders";
	}
	
	@GetMapping("/users")
	public String getAllUsers(@RequestParam(defaultValue = "1") int type, Model m) {

	    m.addAttribute("title", "Users");
	    m.addAttribute("userType", type);

	    List<UserDtls> users;

	    if (type == 1) {
	        users = userService.getUsersByRole("ROLE_USER");
	    } else {
	        users = userService.getUsersByRole("ROLE_ADMIN");
	    }

	    m.addAttribute("users", users);

	    return "admin/users";
	}
	
	@GetMapping("/profile")
	public String profilePage() {
	    return "admin/profile"; // profile.html
	}
	
	@PostMapping("/change-password")
	public String changePassword(@RequestParam String currentPassword,
	                             @RequestParam String newPassword,
	                             @RequestParam String confirmPassword,
	                             Principal p,
	                             HttpSession session) {

	    String email = p.getName();
	    UserDtls user = userService.getUserByEmail(email);

	    // check current password
	    if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
	        session.setAttribute("errorMsg", "Current password is incorrect");
	        return "redirect:/admin/profile";
	    }

	    // check new password match
	    if (!newPassword.equals(confirmPassword)) {
	        session.setAttribute("errorMsg", "Password mismatch");
	        return "redirect:/admin/profile";
	    }

	    // update password
	    user.setPassword(passwordEncoder.encode(newPassword));
	    userService.updateUser(user);

	    session.setAttribute("succMsg", "Password changed successfully");
	    return "redirect:/admin/profile";
	}
	
	@PostMapping("/update-profile")
	public String updateProfile(@ModelAttribute UserDtls user,
	                           @RequestParam("img") MultipartFile file,
	                           HttpSession session) throws IOException {

	    UserDtls oldUser = userService.getUserById(user.getId());

	    String imageName = file.isEmpty() ? oldUser.getProfileImage()
	            : System.currentTimeMillis() + "_" + file.getOriginalFilename();

	    oldUser.setName(user.getName());
	    oldUser.setMobileNumber(user.getMobileNumber());
	    oldUser.setAddress(user.getAddress());
	    oldUser.setCity(user.getCity());
	    oldUser.setState(user.getState());
	    oldUser.setPincode(user.getPincode());
	    oldUser.setProfileImage(imageName);

	    userService.updateUser(oldUser);

	    if (!file.isEmpty()) {
	        String uploadDir = "C:/ecom_upload/profile_img/";

	        File uploadPath = new File(uploadDir);
	        if (!uploadPath.exists()) {
	            uploadPath.mkdirs();
	        }

	        Path path = Paths.get(uploadDir + imageName);
	        Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
	    }

	    session.setAttribute("succMsg", "Profile updated successfully");
	    return "redirect:/admin/profile";
	}
	
	@PostMapping("/update-order-status")
	public String updateOrderStatus(@RequestParam Integer id,
	                               @RequestParam Integer st,
	                               HttpSession session) {

	    OrderStatus[] values = OrderStatus.values();
	    String status = null;

	    for (OrderStatus orderSt : values) {
	        if (orderSt.getId().equals(st)) {
	            status = orderSt.getName();
	        }
	    }

	    ProductOrder updateOrder = orderService.updateOrderStatus(id, status);

	    if (!ObjectUtils.isEmpty(updateOrder)) {
	        session.setAttribute("succMsg", "Status Updated");
	    } else {
	        session.setAttribute("errorMsg", "Status not updated");
	    }

	    return "redirect:/admin/orders"; // ✅ FIXED
	}
}