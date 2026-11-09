package vn.runway.store.web;

import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import org.springframework.dao.*;
import org.springframework.http.*;
import org.springframework.security.authentication.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class ApiErrors {
  public static class BadRequest extends RuntimeException {
    public BadRequest(String m) {
      super(m);
    }
  }

  public static class Missing extends RuntimeException {
    public Missing(String m) {
      super(m);
    }
  }

  public static class Conflict extends RuntimeException {
    public Conflict(String m) {
      super(m);
    }
  }

  public static class CheckoutRequestConflict extends RuntimeException {
    public CheckoutRequestConflict() {
      super("Mã yêu cầu đã tạo đơn với thông tin khác. Hãy kiểm tra Đơn hàng trước khi đặt lại.");
    }
  }

  public record ErrorBody(
      String code, String message, Map<String, String> fieldErrors, String requestId) {}

  public static ErrorBody body(String code, String message, HttpServletRequest r) {
    return new ErrorBody(
        code,
        message,
        Map.of(),
        Objects.toString(r.getAttribute("requestId"), UUID.randomUUID().toString()));
  }

  private ResponseEntity<ErrorBody> error(
      int status, String code, String message, HttpServletRequest r) {
    return ResponseEntity.status(status).body(body(code, message, r));
  }

  @ExceptionHandler(BadRequest.class)
  ResponseEntity<ErrorBody> bad(BadRequest e, HttpServletRequest r) {
    return error(400, "INVALID_REQUEST", e.getMessage(), r);
  }

  @ExceptionHandler(Missing.class)
  ResponseEntity<ErrorBody> missing(Missing e, HttpServletRequest r) {
    return error(404, "NOT_FOUND", e.getMessage(), r);
  }

  @ExceptionHandler(Conflict.class)
  ResponseEntity<ErrorBody> conflict(Conflict e, HttpServletRequest r) {
    return error(409, "CONFLICT", e.getMessage(), r);
  }

  @ExceptionHandler(CheckoutRequestConflict.class)
  ResponseEntity<ErrorBody> checkoutRequestConflict(
      CheckoutRequestConflict e, HttpServletRequest r) {
    return error(409, "CHECKOUT_REQUEST_CONFLICT", e.getMessage(), r);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<ErrorBody> validation(MethodArgumentNotValidException e, HttpServletRequest r) {
    var fields = new TreeMap<String, String>();
    e.getBindingResult()
        .getFieldErrors()
        .forEach(
            f ->
                fields.putIfAbsent(
                    f.getField(), Objects.toString(f.getDefaultMessage(), "Invalid")));
    return ResponseEntity.badRequest()
        .body(
            new ErrorBody(
                "VALIDATION_FAILED",
                "Kiểm tra các trường thông tin.",
                fields,
                body("", "", r).requestId()));
  }

  @ExceptionHandler({
    org.springframework.http.converter.HttpMessageNotReadableException.class,
    org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,
    org.springframework.web.bind.MissingServletRequestParameterException.class
  })
  ResponseEntity<ErrorBody> malformed(Exception e, HttpServletRequest r) {
    return error(400, "MALFORMED_REQUEST", "Dữ liệu hoặc tham số không hợp lệ.", r);
  }

  @ExceptionHandler({
    DataIntegrityViolationException.class,
    OptimisticLockingFailureException.class,
    PessimisticLockingFailureException.class
  })
  ResponseEntity<ErrorBody> duplicate(Exception e, HttpServletRequest r) {
    return error(409, "DATA_CONFLICT", "Dữ liệu đã tồn tại hoặc vừa thay đổi.", r);
  }

  @ExceptionHandler({
    BadCredentialsException.class,
    AuthenticationCredentialsNotFoundException.class
  })
  ResponseEntity<ErrorBody> auth(Exception e, HttpServletRequest r) {
    return error(
        401, "AUTHENTICATION_REQUIRED", "Email, mật khẩu chưa đúng hoặc phiên chưa đăng nhập.", r);
  }

  @ExceptionHandler({
    org.springframework.web.servlet.resource.NoResourceFoundException.class,
    org.springframework.web.servlet.NoHandlerFoundException.class
  })
  ResponseEntity<ErrorBody> route(Exception e, HttpServletRequest r) {
    return error(404, "NOT_FOUND", "Không tìm thấy tài nguyên.", r);
  }

  @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
  ResponseEntity<ErrorBody> method(Exception e, HttpServletRequest r) {
    return error(405, "METHOD_NOT_ALLOWED", "Phương thức không hỗ trợ.", r);
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<ErrorBody> unexpected(Exception e, HttpServletRequest r) {
    org.slf4j.LoggerFactory.getLogger(ApiErrors.class)
        .error(
            "Unhandled request failure requestId={} exceptionType={}",
            r.getAttribute("requestId"),
            e.getClass().getSimpleName());
    return error(500, "INTERNAL_ERROR", "Máy chủ chưa thể xử lý yêu cầu.", r);
  }
}
