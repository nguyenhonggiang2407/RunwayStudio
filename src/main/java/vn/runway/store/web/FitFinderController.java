package vn.runway.store.web;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;
import vn.runway.store.dto.*;
import vn.runway.store.service.*;

@RestController
public class FitFinderController {
  private final FitFinderService service;

  public FitFinderController(FitFinderService s) {
    service = s;
  }

  @GetMapping("/api/size-guides")
  public List<Responses.SizeGuide> guides() {
    return service.guides();
  }

  @PostMapping("/api/size-recommendations")
  public Responses.Fit recommend(@Valid @RequestBody StudioRequests.Fit i) {
    return service.recommend(i);
  }
}
