package com.example.iaspringlab.web.shop;

import com.example.iaspringlab.account.AccountService;
import com.example.iaspringlab.category.CategoryService;
import com.example.iaspringlab.order.CartService;
import com.example.iaspringlab.order.OrderLineResponse;
import com.example.iaspringlab.order.OrderResponse;
import com.example.iaspringlab.product.ProductService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/shop")
public class ShopController {

    private final AccountService accountService;
    private final ProductService productService;
    private final CategoryService categoryService;
    private final CartService cartService;

    public ShopController(AccountService accountService, ProductService productService,
                          CategoryService categoryService, CartService cartService) {
        this.accountService = accountService;
        this.productService = productService;
        this.categoryService = categoryService;
        this.cartService = cartService;
    }

    @ModelAttribute("cartItemCount")
    public int cartItemCount(Principal principal) {
        return cartService.getCart(customerId(principal))
                .map(cart -> cart.lines().stream().mapToInt(OrderLineResponse::quantity).sum())
                .orElse(0);
    }

    @GetMapping
    public String catalog(@RequestParam(required = false) Long categoryId, Model model) {
        model.addAttribute("products", productService.getActive(categoryId));
        model.addAttribute("categories", categoryService.getAll());
        model.addAttribute("selectedCategoryId", categoryId);
        return "shop/catalog";
    }

    @PostMapping("/cart/items")
    public String addToCart(@RequestParam Long productId, @RequestParam(defaultValue = "1") Integer quantity,
                            @RequestParam(required = false) Long categoryId,
                            Principal principal, RedirectAttributes redirectAttributes) {
        cartService.addProduct(customerId(principal), productId, quantity);
        redirectAttributes.addFlashAttribute("success", "Produit ajouté au panier.");
        if (categoryId != null) {
            redirectAttributes.addAttribute("categoryId", categoryId);
        }
        return "redirect:/shop";
    }

    @GetMapping("/cart")
    public String cart(Principal principal, Model model) {
        model.addAttribute("cart", cartService.getCart(customerId(principal)).orElse(null));
        return "shop/cart";
    }

    @PostMapping("/cart/lines/{lineId}")
    public String updateLine(@PathVariable Long lineId, @RequestParam Integer quantity, Principal principal,
                             RedirectAttributes redirectAttributes) {
        cartService.updateQuantity(customerId(principal), lineId, quantity);
        redirectAttributes.addFlashAttribute("success", "Quantité mise à jour.");
        return "redirect:/shop/cart";
    }

    @PostMapping("/cart/lines/{lineId}/delete")
    public String removeLine(@PathVariable Long lineId, Principal principal, RedirectAttributes redirectAttributes) {
        cartService.removeLine(customerId(principal), lineId);
        redirectAttributes.addFlashAttribute("success", "Produit retiré du panier.");
        return "redirect:/shop/cart";
    }

    @PostMapping("/cart/checkout")
    public String checkout(Principal principal, RedirectAttributes redirectAttributes) {
        OrderResponse order = cartService.checkout(customerId(principal));
        redirectAttributes.addFlashAttribute("success", "Commande n°" + order.id() + " confirmée. Merci !");
        return "redirect:/shop/orders/" + order.id();
    }

    @GetMapping("/orders")
    public String orders(Principal principal, Model model) {
        model.addAttribute("orders", cartService.getOrderHistory(customerId(principal)));
        return "shop/orders";
    }

    @GetMapping("/orders/{orderId}")
    public String order(@PathVariable Long orderId, Principal principal, Model model) {
        model.addAttribute("order", cartService.getCustomerOrder(customerId(principal), orderId));
        return "shop/order-detail";
    }

    private Long customerId(Principal principal) {
        return accountService.getCustomerId(principal.getName());
    }
}
