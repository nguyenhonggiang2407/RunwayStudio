package vn.runway.store.service;

import java.util.List;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import vn.runway.store.web.ApiErrors.BadRequest;

@Service
public class ApprovedMediaService {
  public void validate(String path) {
    if (path == null
        || !path.matches("/media/owned/[a-z0-9-]+\\.svg")
        || !new ClassPathResource("static" + path).exists())
      throw new BadRequest("Chỉ dùng hình minh họa đã duyệt trong media/owned.");
  }

  public List<String> available() {
    return List.of(
        "/media/owned/scene-city.svg",
        "/media/owned/scene-weekend.svg",
        "/media/owned/scene-gallery.svg");
  }
}
