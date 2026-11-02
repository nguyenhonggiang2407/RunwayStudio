package vn.runway.store.web;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;
import vn.runway.store.dto.*;
import vn.runway.store.service.*;

@RestController
public class WardrobeController {
  private final WardrobeService service;
  private final AddressService addresses;

  public WardrobeController(WardrobeService s, AddressService a) {
    service = s;
    addresses = a;
  }

  @GetMapping("/api/me/wardrobe")
  public List<Responses.Wardrobe> list() {
    return service.list();
  }

  @PostMapping("/api/me/wardrobe")
  public Responses.Wardrobe save(@Valid @RequestBody StudioRequests.Wardrobe i) {
    return service.save(i);
  }

  @DeleteMapping("/api/me/wardrobe/{id}")
  public Responses.Message delete(@PathVariable Long id) {
    service.delete(id);
    return new Responses.Message("Đã bỏ lưu.");
  }

  @GetMapping("/api/me/addresses")
  public List<Responses.Address> addresses() {
    return addresses.list();
  }

  @PostMapping("/api/me/addresses")
  public Responses.Address createAddress(@Valid @RequestBody Requests.Address i) {
    return addresses.save(null, i);
  }

  @PutMapping("/api/me/addresses/{id}")
  public Responses.Address updateAddress(
      @PathVariable Long id, @Valid @RequestBody Requests.Address i) {
    return addresses.save(id, i);
  }

  @DeleteMapping("/api/me/addresses/{id}")
  public Responses.Message deleteAddress(@PathVariable Long id) {
    addresses.delete(id);
    return new Responses.Message("Đã xóa địa chỉ.");
  }
}
