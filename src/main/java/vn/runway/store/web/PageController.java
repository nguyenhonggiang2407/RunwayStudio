package vn.runway.store.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {
  @GetMapping({
    "/shop",
    "/product/{slug}",
    "/cart",
    "/checkout",
    "/account",
    "/orders",
    "/admin",
    "/studio",
    "/wardrobe",
    "/scenes",
    "/scenes/{slug}",
    "/fit-finder"
  })
  String app() {
    return "forward:/index.html";
  }

  @GetMapping({"/pages/runway", "/pages/runway/", "/pages/runway/index.html"})
  String runway() {
    return "redirect:/studio";
  }

  @GetMapping({"/pages/nike-orbit", "/pages/nike-orbit/", "/pages/nike-orbit/index.html"})
  String orbit() {
    return "redirect:/scenes";
  }

  @GetMapping({"/pages/scene-rail", "/pages/scene-rail/", "/pages/scene-rail/index.html"})
  String rail() {
    return "redirect:/scenes";
  }

  @GetMapping({
    "/experience/runway.html",
    "/experience/nike-orbit.html",
    "/experience/scene-rail.html"
  })
  String oldExperience() {
    return "redirect:/scenes";
  }

  @GetMapping("/products/{slug}")
  String oldProduct(@org.springframework.web.bind.annotation.PathVariable String slug) {
    return "redirect:/product/" + slug;
  }

  @GetMapping("/collections/all")
  String oldCollection() {
    return "redirect:/shop";
  }
}
