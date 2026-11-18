package edu.itc.se.shop_jpa.web;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import edu.itc.se.shop_jpa.config.ShopProperties;
import edu.itc.se.shop_jpa.service.ProductService;
import edu.itc.se.shop_jpa.web.dto.CreateProduct;
import edu.itc.se.shop_jpa.web.dto.ProductDto;
import edu.itc.se.shop_jpa.web.dto.ProductItemView;

@Controller
@RequestMapping("/products")
public class ProductPageController {

    private static final Logger log = LoggerFactory.getLogger(ProductPageController.class);

    private final ProductService service;
    private final ShopProperties properties;

    public ProductPageController(ProductService service, ShopProperties properties) {
        this.service = service;
        this.properties = properties;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String q, Model model) {
        BigDecimal multiplier = BigDecimal.ONE.add(properties.taxRate());

        List<ProductItemView> items = service.search(q).stream()
                .map(p -> new ProductItemView(
                        p.id(),
                        p.name(),
                        p.price(),
                        p.price().multiply(multiplier),
                        p.stock()
                ))
                .toList();

        model.addAttribute("products", items);
        model.addAttribute("currency", properties.currency());
        model.addAttribute("threshold", properties.lowStockThreshold());
        model.addAttribute("q", q);
        return "products/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        if (!model.containsAttribute("createProduct")) {
            model.addAttribute("createProduct", new CreateProduct("", new BigDecimal("0.01"), 0));
        }
        return "products/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("createProduct") CreateProduct cmd,
                         BindingResult bindingResult,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            log.warn("Validation failed for product: {}", bindingResult.getAllErrors());
            return "products/form";
        }

        ProductDto created = service.create(cmd);
        redirectAttributes.addFlashAttribute("message", "Created product " + created.id() + " '" + created.name() + "'");
        return "redirect:/products";
    }
}