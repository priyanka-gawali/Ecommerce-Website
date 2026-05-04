package com.ecom.controller;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.multipart.MultipartFile;

import com.ecom.model.Category;
import com.ecom.model.Product;
import com.ecom.model.UserDtls;
import com.ecom.service.CartService;
import com.ecom.service.CategoryService;
import com.ecom.service.ProductService;
import com.ecom.service.UserService;
import com.ecom.util.CommonUtil;

import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
public class HomeController {

	@Autowired
	private CategoryService categoryService;

	@Autowired
	private ProductService productService;

	@Autowired
	private UserService userService;

	@Autowired
	private CommonUtil commonUtil;

	@Autowired
	private BCryptPasswordEncoder passwordEncoder;

	@Autowired
	private CartService cartService;

	@ModelAttribute
	public void getUserDetails(Principal p, Model m) {
		if (p != null) {
			String email = p.getName();
			UserDtls userDtls = userService.getUserByEmail(email);
			m.addAttribute("user", userDtls);
			Integer countCart = cartService.getCountCart(userDtls.getId());
			m.addAttribute("countCart", countCart);
			m.addAttribute("title", "Ecom Store");
		}

		m.addAttribute("categorys", categoryService.getAllActiveCategory());
	}

	@GetMapping("/")
	public String index(Model m) {

		m.addAttribute("title", "Home");

		List<Category> allActiveCategory = categoryService.getAllActiveCategory().stream()
		        .sorted((c1, c2) -> c2.getId().compareTo(c1.getId()))
		        .toList();

		List<Product> allActiveProducts = productService.getAllActiveProducts("").stream()
				.sorted((p1, p2) -> p2.getId().compareTo(p1.getId()))
				.toList();

		m.addAttribute("category", allActiveCategory);
		m.addAttribute("products", allActiveProducts);

		return "index";
	}

	@GetMapping("/signin")
	public String login(Model m) {
		m.addAttribute("title", "Login Page");
		return "signin";
	}

	@GetMapping("/register")
	public String register(Model m) {
		m.addAttribute("title", "Register Page");
		return "register";
	}

	// ✅ Products WITHOUT pagination
	@GetMapping("/products")
	public String products(Model m,
			@RequestParam(value = "category", defaultValue = "") String category,
			@RequestParam(defaultValue = "") String ch) {

		m.addAttribute("title", "Products");

		List<Category> categories = categoryService.getAllActiveCategory();
		m.addAttribute("paramValue", category);
		m.addAttribute("categories", categories);

		List<Product> products;

		if (ch != null && ch.length() > 0) {
			products = productService.searchProduct(ch);
		} else {
			products = productService.getAllActiveProducts(category);
		}

		m.addAttribute("products", products);
		m.addAttribute("productsSize", products.size());

		return "product";
	}

	@GetMapping("/product/{id}")
	public String product(@PathVariable int id, Model m) {
		m.addAttribute("title", "View Products");
		m.addAttribute("product", productService.getProductById(id));
		return "view_product";
	}

	@PostMapping("/saveUser")
	public String saveUser(@ModelAttribute UserDtls user,
			@RequestParam("img") MultipartFile file,
			HttpSession session, Model m) throws IOException {

		m.addAttribute("title", "Register");

		if (userService.existsEmail(user.getEmail())) {
			session.setAttribute("errorMsg", "Email already Registered ");
		} else {

			String imageName = file.isEmpty() ? "default.jpg" : file.getOriginalFilename();
			user.setProfileImage(imageName);

			UserDtls saveUser = userService.saveUser(user);

			if (!ObjectUtils.isEmpty(saveUser)) {

				if (!file.isEmpty()) {
					File saveFile = new ClassPathResource("static/img").getFile();

					Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + "profile_img"
							+ File.separator + file.getOriginalFilename());

					Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
				}

				session.setAttribute("succMsg", "Register successfully");
			} else {
				session.setAttribute("errorMsg", "something wrong on server");
			}
		}

		return "redirect:/register";
	}

	// Forgot Password

	@GetMapping("/forgot-password")
	public String showForgotPassword() {
		return "forgot_password.html";
	}

	@PostMapping("/forgot-password")
	public String processForgotPassword(@RequestParam String email,
			HttpSession session,
			HttpServletRequest request,
			Model m) throws UnsupportedEncodingException, MessagingException {

		m.addAttribute("title", "Forgot Password");

		UserDtls userByEmail = userService.getUserByEmail(email);

		if (ObjectUtils.isEmpty(userByEmail)) {
			session.setAttribute("errorMsg", "Invalid email");
		} else {

			String resetToken = UUID.randomUUID().toString();
			userService.updateUserResetToken(email, resetToken);

			String url = CommonUtil.generateUrl(request) + "/reset-password?token=" + resetToken;

			Boolean sendMail = commonUtil.sendMail(url, email);

			if (sendMail) {
				session.setAttribute("succMsg", "Please check your email..Password Reset link sent");
			} else {
				session.setAttribute("errorMsg", "Somethong wrong on server ! Email not send");
			}
		}

		return "redirect:/forgot-password";
	}

	@GetMapping("/reset-password")
	public String showResetPassword(@RequestParam String token, Model m) {

		m.addAttribute("title", "Reset Password");

		UserDtls userByToken = userService.getUserByToken(token);

		if (userByToken == null) {
			m.addAttribute("msg", "Your link is invalid or expired !!");
			return "message";
		}

		m.addAttribute("token", token);
		return "reset_password";
	}

	
	@PostMapping("/reset-password")
	public String resetPassword(@RequestParam String token,
	        @RequestParam String password,
	        @RequestParam String confirmPassword,
	        Model m) {

	    if (!password.equals(confirmPassword)) {
	        m.addAttribute("errorMsg", "Password and Confirm Password must match");
	        return "reset_password";
	    }

	    UserDtls userByToken = userService.getUserByToken(token);

	    if (userByToken == null) {
	        m.addAttribute("errorMsg", "Your link is invalid or expired !!");
	        return "message";
	    } else {
	        userByToken.setPassword(passwordEncoder.encode(password));
	        userByToken.setResetToken(null);
	        userService.updateUser(userByToken);

	        m.addAttribute("msg", "Password change successfully");
	        return "message";
	    }
	}

	@GetMapping("/search")
	public String searchProduct(@RequestParam String ch, Model m) {

		m.addAttribute("products", productService.searchProduct(ch));
		m.addAttribute("categories", categoryService.getAllActiveCategory());

		return "product";
	}
}