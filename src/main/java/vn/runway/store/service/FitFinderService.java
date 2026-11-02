package vn.runway.store.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.runway.store.dto.*;
import vn.runway.store.repository.*;
import vn.runway.store.web.ApiErrors.Missing;

@Service
@Transactional(readOnly = true)
public class FitFinderService {
  private static final String DISCLAIMER =
      "Bảng size minh họa, không bảo đảm vừa tuyệt đối. Đối chiếu bảng size thực tế trước khi mua."
          + " Số đo không được lưu.";
  private final SizeGuideRepository guides;
  private final SizeGuideRowRepository rows;

  public FitFinderService(SizeGuideRepository g, SizeGuideRowRepository r) {
    guides = g;
    rows = r;
  }

  public List<Responses.SizeGuide> guides() {
    return guides.findAll().stream()
        .map(
            g ->
                new Responses.SizeGuide(
                    g.id,
                    g.category,
                    g.version,
                    g.unit,
                    rows.findByGuideIdOrderBySortOrderAsc(g.id).stream()
                        .map(
                            r ->
                                new Responses.GuideRow(
                                    r.size,
                                    r.minChest,
                                    r.maxChest,
                                    r.minWaist,
                                    r.maxWaist,
                                    r.minFoot,
                                    r.maxFoot))
                        .toList()))
        .toList();
  }

  public Responses.Fit recommend(StudioRequests.Fit input) {
    var g =
        guides
            .findByCategory(input.category())
            .orElseThrow(() -> new Missing("Chưa có bảng size cho danh mục này."));
    boolean shoe = input.category().equals("Giày"), bottom = input.category().equals("Quần");
    Double value = shoe ? input.footLength() : bottom ? input.waist() : input.chest();
    String measure = shoe ? "chiều dài bàn chân" : bottom ? "vòng eo" : "vòng ngực";
    if (value == null)
      return new Responses.Fit(
          input.category(),
          null,
          "INSUFFICIENT_DATA",
          "Cần " + measure + " (cm). Chiều cao/cân nặng riêng lẻ chưa đủ.",
          g.version,
          DISCLAIMER);
    for (var r : rows.findByGuideIdOrderBySortOrderAsc(g.id)) {
      Double min = shoe ? r.minFoot : bottom ? r.minWaist : r.minChest,
          max = shoe ? r.maxFoot : bottom ? r.maxWaist : r.maxChest;
      if (min != null && max != null && value >= min && value < max)
        return new Responses.Fit(
            input.category(),
            r.size,
            "GUIDE_MATCH",
            measure
                + " "
                + value
                + " cm nằm trong ["
                + min
                + ", "
                + max
                + ") cm. Nếu khoảng chồng lấn, ưu tiên sortOrder nhỏ hơn.",
            g.version,
            DISCLAIMER);
    }
    return new Responses.Fit(
        input.category(),
        null,
        "OUTSIDE_GUIDE",
        "Số đo nằm ngoài bảng minh họa.",
        g.version,
        DISCLAIMER);
  }
}
