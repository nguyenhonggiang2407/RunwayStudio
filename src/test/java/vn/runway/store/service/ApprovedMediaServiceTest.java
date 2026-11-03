package vn.runway.store.service;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import vn.runway.store.dto.Requests;
import vn.runway.store.web.ApiErrors.BadRequest;

class ApprovedMediaServiceTest {
  private final ApprovedMediaService media = new ApprovedMediaService(new ObjectMapper());

  @Test
  void manifestListedPhotoCanBeUsedForProductsAndAdminScenes() {
    String photo =
        media.available().stream()
            .filter(path -> path.startsWith("/media/original/"))
            .findFirst()
            .orElseThrow();
    assertThatCode(() -> media.validate(photo)).doesNotThrowAnyException();
    try (var factory = Validation.buildDefaultValidatorFactory()) {
      var request = new Requests.ProductInput("Photo product", "photo-product", "Áo", "", photo, true);
      assertThat(factory.getValidator().validate(request)).isEmpty();
    }
    assertThatThrownBy(() -> media.available().add(photo))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void existingOwnedMediaRemainsCompatible() {
    assertThatCode(() -> media.validate("/media/owned/daily-sweat.svg"))
        .doesNotThrowAnyException();
    assertThat(media.available()).contains("/media/owned/scene-city.svg");
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(
      strings = {
        "/media/original/not-listed.jpg",
        "/media/owned/not-existing.svg",
        "/media/original/../owned/scene-city.svg",
        "/media/original/%2e%2e/owned/scene-city.svg",
        "/media/original/nested/photo.jpg",
        "https://example.org/photo.jpg",
        "//example.org/photo.jpg",
        "file:///etc/passwd"
      })
  void unlistedRemoteAndTraversalMediaAreRejected(String path) {
    assertThatThrownBy(() -> media.validate(path)).isInstanceOf(BadRequest.class);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "/media/original/../owned/scene-city.svg",
        "/media/original/%2e%2e/owned/scene-city.svg",
        "/media/original/nested/photo.jpg",
        "https://example.org/photo.jpg",
        "//example.org/photo.jpg"
      })
  void productRequestRejectsNonLocalPhotoShapes(String path) {
    try (var factory = Validation.buildDefaultValidatorFactory()) {
      var request = new Requests.ProductInput("Photo product", "photo-product", "Áo", "", path, true);
      assertThat(factory.getValidator().validate(request)).extracting(v -> v.getPropertyPath().toString())
          .contains("image");
    }
  }
}
