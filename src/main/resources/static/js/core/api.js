let csrf;
export function resetCsrf() {
  csrf = null;
}
async function token() {
  const r = await fetch("/api/csrf", { credentials: "same-origin" });
  if (!r.ok) throw new Error("Chưa thể xác thực phiên.");
  csrf = await r.json();
}
export async function api(url, options = {}) {
  const method = options.method || "GET";
  if (!["GET", "HEAD"].includes(method) && !csrf) await token();
  const r = await fetch(url, {
    credentials: "same-origin",
    signal: AbortSignal.timeout(20000),
    ...options,
    headers: {
      Accept: "application/json",
      ...(options.body ? { "Content-Type": "application/json" } : {}),
      ...(!["GET", "HEAD"].includes(method)
        ? { [csrf.headerName]: csrf.token }
        : {}),
      ...options.headers,
    },
  });
  const data =
    r.status === 204
      ? {}
      : await r
          .json()
          .catch(() => ({ message: "Máy chủ chưa trả dữ liệu hợp lệ." }));
  if (!r.ok) {
    if (r.status === 403) csrf = null;
    throw Object.assign(new Error(data.message || "Không thể xử lý yêu cầu."), {
      status: r.status,
      code: data.code,
      requestId: data.requestId,
      fieldErrors: data.fieldErrors,
    });
  }
  return data;
}
export const send = (url, body, method = "POST") =>
  api(url, { method, body: JSON.stringify(body) });
export async function updateCount() {
  try {
    document.querySelector("#cart-count").textContent = (
      await api("/api/cart")
    ).item_count;
  } catch {
    document.querySelector("#cart-count").textContent = "!";
  }
}
