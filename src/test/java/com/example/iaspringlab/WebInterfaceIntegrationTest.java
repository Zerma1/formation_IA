package com.example.iaspringlab;

import com.example.iaspringlab.category.CategoryRequest;
import com.example.iaspringlab.category.CategoryResponse;
import com.example.iaspringlab.category.CategoryService;
import com.example.iaspringlab.product.ProductRequest;
import com.example.iaspringlab.product.ProductResponse;
import com.example.iaspringlab.product.ProductService;
import com.example.iaspringlab.support.PostgresTestContainerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestContainerConfig.class)
@ActiveProfiles("test")
class WebInterfaceIntegrationTest {

    private static final RequestPostProcessor ADMIN = user("admin").roles("ADMIN");
    private static final RequestPostProcessor USER = user("user").roles("USER");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductService productService;

    @Autowired
    private CategoryService categoryService;

    @Test
    void defaultAccounts_canLogIn() throws Exception {
        mockMvc.perform(formLogin().user("admin").password("admin"))
                .andExpect(authenticated().withRoles("ADMIN"))
                .andExpect(redirectedUrl("/"));
        mockMvc.perform(formLogin().user("user").password("user"))
                .andExpect(authenticated().withRoles("USER"));
        mockMvc.perform(formLogin().user("admin").password("wrong"))
                .andExpect(unauthenticated());
    }

    @Test
    void pages_areProtectedByRole() throws Exception {
        mockMvc.perform(get("/shop")).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
        mockMvc.perform(get("/login")).andExpect(status().isOk());
        mockMvc.perform(get("/admin/products").with(USER)).andExpect(status().isForbidden());
        mockMvc.perform(get("/shop").with(ADMIN)).andExpect(status().isForbidden());
        mockMvc.perform(get("/").with(ADMIN)).andExpect(redirectedUrl("/admin/products"));
        mockMvc.perform(get("/").with(USER)).andExpect(redirectedUrl("/shop"));
    }

    @Test
    void admin_managesCategoriesAndProducts() throws Exception {
        mockMvc.perform(post("/admin/categories").with(ADMIN).with(csrf()).param("name", "Web-Informatique"))
                .andExpect(redirectedUrl("/admin/categories"));
        Long categoryId = categoryIdNamed("Web-Informatique");

        mockMvc.perform(post("/admin/products").with(ADMIN).with(csrf())
                        .param("reference", "WEB-ADM-1")
                        .param("name", "Clavier web")
                        .param("price", "49.90")
                        .param("active", "true")
                        .param("categoryIds", categoryId.toString()))
                .andExpect(redirectedUrl("/admin/products"));

        mockMvc.perform(get("/admin/products").with(ADMIN))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Clavier web")))
                .andExpect(content().string(containsString("Web-Informatique")));

        ProductResponse product = productReferenced("WEB-ADM-1");
        mockMvc.perform(get("/admin/products/{id}/edit", product.id()).with(ADMIN))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Clavier web")));
        mockMvc.perform(get("/admin/categories").with(ADMIN))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Web-Informatique")));

        mockMvc.perform(post("/admin/categories/{id}/delete", categoryId).with(ADMIN).with(csrf()))
                .andExpect(redirectedUrl("/admin/categories"));

        ProductResponse detached = productService.getById(product.id());
        assertThat(detached.categories()).isEmpty();

        mockMvc.perform(post("/admin/products/{id}/delete", product.id()).with(ADMIN).with(csrf()))
                .andExpect(redirectedUrl("/admin/products"));
        assertThat(productService.getAll()).extracting(ProductResponse::reference).doesNotContain("WEB-ADM-1");
    }

    @Test
    void admin_seesValidationErrors_whenProductFormInvalid() throws Exception {
        mockMvc.perform(post("/admin/products").with(ADMIN).with(csrf())
                        .param("reference", "")
                        .param("name", "Sans référence")
                        .param("price", "-1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("La référence est obligatoire.")))
                .andExpect(content().string(containsString("Le prix doit être strictement positif.")));
    }

    @Test
    void user_fillsCartChecksOutAndFindsOrderInHistory() throws Exception {
        CategoryResponse category = categoryService.create(new CategoryRequest("Web-Audio"));
        ProductResponse headphones = productService.create(new ProductRequest(
                "WEB-SHOP-1", "Casque web", new BigDecimal("25.50"), true, List.of(category.id())));
        ProductResponse discontinued = productService.create(new ProductRequest(
                "WEB-SHOP-2", "Ancien casque", new BigDecimal("9.90"), false));

        mockMvc.perform(get("/shop").with(USER))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Casque web")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("Ancien casque"))));
        mockMvc.perform(get("/shop").param("categoryId", category.id().toString()).with(USER))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Casque web")));

        mockMvc.perform(post("/shop/cart/items").with(USER).with(csrf())
                        .param("productId", headphones.id().toString()).param("quantity", "2"))
                .andExpect(redirectedUrl("/shop"));
        mockMvc.perform(post("/shop/cart/items").with(USER).with(csrf())
                        .param("productId", discontinued.id().toString()).param("quantity", "1")
                        .header("Referer", "http://localhost/shop"))
                .andExpect(redirectedUrl("/shop"))
                .andExpect(flash().attributeExists("error"));

        mockMvc.perform(get("/shop/cart").with(USER))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Casque web")))
                .andExpect(content().string(containsString("51,00 €")))
                .andExpect(content().string(containsString("Commander")));

        String orderUrl = mockMvc.perform(post("/shop/cart/checkout").with(USER).with(csrf()))
                .andExpect(redirectedUrlPattern("/shop/orders/*"))
                .andReturn().getResponse().getRedirectedUrl();

        mockMvc.perform(get(orderUrl).with(USER))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Confirmée")))
                .andExpect(content().string(containsString("Casque web")));
        mockMvc.perform(get("/shop/orders").with(USER))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(orderUrl)));
        mockMvc.perform(get("/shop/cart").with(USER))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Votre panier est vide.")));
    }

    private Long categoryIdNamed(String name) {
        return categoryService.getAll().stream()
                .filter(category -> category.name().equals(name))
                .findFirst().orElseThrow()
                .id();
    }

    private ProductResponse productReferenced(String reference) {
        return productService.getAll().stream()
                .filter(product -> product.reference().equals(reference))
                .findFirst().orElseThrow();
    }
}
