package vn.runway.store.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import vn.runway.store.web.ApiErrors.BadRequest;

@Service
public class ApprovedMediaService {
  private static final String OWNED_IMAGE = "/media/owned/[a-z0-9-]+\\.svg";
  private static final String ORIGINAL_PHOTO =
      "/media/original/[A-Za-z0-9][A-Za-z0-9_-]{0,199}\\.(?:png|jpe?g|webp)";
  private static final List<String> OWNED_COVERS =
      List.of(
          "/media/owned/scene-city.svg",
          "/media/owned/scene-weekend.svg",
          "/media/owned/scene-gallery.svg");
  private final Set<String> originalPhotos;
  private final List<String> available;

  public ApprovedMediaService(ObjectMapper json) {
    var paths = new LinkedHashSet<String>();
    try (var input = new ClassPathResource("original-photo-manifest.json").getInputStream()) {
      var files = json.readTree(input).path("files");
      if (!files.isArray()) throw new IllegalStateException("Original photo manifest must list files.");
      for (var file : files) {
        String path = file.path("path").asText();
        if (!path.matches(ORIGINAL_PHOTO) || !resourceExists(path))
          throw new IllegalStateException("Original photo manifest contains unavailable media: " + path);
        if (!paths.add(path))
          throw new IllegalStateException("Original photo manifest contains duplicate media: " + path);
      }
    } catch (IOException e) {
      throw new IllegalStateException("Cannot load original photo manifest.", e);
    }
    originalPhotos = Set.copyOf(paths);
    var choices = new java.util.ArrayList<>(OWNED_COVERS);
    choices.addAll(paths);
    available = List.copyOf(choices);
  }

  private static boolean resourceExists(String path) {
    return new ClassPathResource("static" + path).exists();
  }

  public void validate(String path) {
    if (path == null
        || !(path.matches(OWNED_IMAGE) || originalPhotos.contains(path))
        || !resourceExists(path))
      throw new BadRequest("Chỉ dùng ảnh nội bộ đã được duyệt cho Runway Studio.");
  }

  public List<String> available() {
    return available;
  }
}
