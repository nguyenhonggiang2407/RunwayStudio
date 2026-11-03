import assert from "node:assert/strict";
import { createHash } from "node:crypto";
import { readFile, readdir } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const resources = path.join(root, "src/main/resources");
const staticRoot = path.join(resources, "static");
const json = async (file) => JSON.parse(await readFile(file, "utf8"));
const manifest = await json(
  path.join(resources, "original-photo-manifest.json"),
);
const catalog = await json(path.join(resources, "catalog-seed.json"));
const photos = await json(path.join(staticRoot, "product-photos.json"));
const looks = await json(path.join(staticRoot, "runway-looks.json"));
const expectedSlugs = [
  "air-jordan-12-retro-copy",
  "blazzer",
  "hoodie",
  "hoodie-black",
  "sweatshirt",
  "sweatshirt-plus",
  "tatum-5-orbit",
  "tatum-5-orbit-copy",
  "tshirt",
];
const media = new Map(manifest.files.map((file) => [file.path, file]));
const resourceFile = (uri) => {
  assert.match(uri, /^\/(?!\/)/, "image must use this application origin");
  const resolved = path.resolve(staticRoot, "." + uri);
  assert.ok(
    resolved.startsWith(staticRoot + path.sep),
    "image stays in static resources",
  );
  return resolved;
};
const verifiedImage = (uri) => {
  assert.ok(media.has(uri), `unlisted original image: ${uri}`);
  return media.get(uri);
};
const sourceKey = (source) => {
  const url = new URL(source);
  assert.equal(
    url.hostname,
    "nike-makknzjs.myshopify.com",
    "original source hostname",
  );
  return url.pathname + "?v=" + url.searchParams.get("v");
};

assert.equal(manifest.upstream.repo, "https://github.com/IdeaToCodee/runway");
assert.equal(
  manifest.upstream.commit,
  "e0219d0082e31af42438ad787a73b988d5e2fa16",
);
assert.equal(
  manifest.files.length,
  140,
  "all original baseline photos retained",
);
assert.equal(media.size, 140, "original paths are unique");
assert.equal(
  new Set(manifest.files.map((file) => file.sha256)).size,
  140,
  "original bytes are unique",
);
for (const file of manifest.files) {
  assert.match(
    file.path,
    /^\/media\/original\/[a-f0-9]{24}\.(?:png|jpe?g|webp)$/i,
  );
  assert.match(file.sha256, /^[a-f0-9]{64}$/);
  assert.equal(
    path.basename(file.path).split(".")[0],
    file.sha256.slice(0, 24),
    "original filename fingerprint",
  );
  const bytes = await readFile(resourceFile(file.path));
  assert.equal(
    createHash("sha256").update(bytes).digest("hex"),
    file.sha256,
    `original bytes changed: ${file.path}`,
  );
  sourceKey(file.source);
}
const onDisk = await readdir(path.join(staticRoot, "media/original"));
assert.equal(onDisk.length, 140, "original directory matches manifest count");
assert.ok(
  onDisk.every((name) => media.has("/media/original/" + name)),
  "no unlisted original resources",
);

assert.deepEqual(
  Object.keys(photos).sort(),
  expectedSlugs.sort(),
  "exactly nine original product mappings",
);
for (const slug of expectedSlugs) {
  const pair = photos[slug],
    product = catalog.find((item) => item.slug === slug);
  assert.ok(product, `missing catalog slug: ${slug}`);
  assert.equal(
    pair.image,
    product.image,
    `card photo differs from catalog: ${slug}`,
  );
  assert.equal(
    sourceKey(verifiedImage(pair.image).source),
    sourceKey(verifiedImage(pair.full).source),
    `card/full photo mismatch: ${slug}`,
  );
}
for (const slug of ["straight-trousers", "weekend-tote"]) {
  assert.ok(!photos[slug], "new demo product has no invented original mapping");
  assert.match(
    catalog.find((item) => item.slug === slug)?.image || "",
    /^\/media\/owned\/.*\.svg$/,
    "new demo product retains illustration",
  );
}
assert.equal(looks.length, 6, "six original model looks");
assert.equal(
  new Set(looks.map((look) => look.name)).size,
  6,
  "look names are unique",
);
assert.equal(
  new Set(looks.map((look) => look.image)).size,
  6,
  "model photos are unique",
);
for (const look of looks) {
  assert.equal(
    sourceKey(verifiedImage(look.image).source),
    sourceKey(verifiedImage(look.preview).source),
    "model preview matches full photo",
  );
  assert.equal(look.products.length, 3, "three suggestions per original look");
  assert.ok(
    look.products.every((slug) => photos[slug]),
    "look suggestions use valid original product slugs",
  );
}

const externalImages = [
  /\b(?:src|poster)\s*=\s*["']\s*(?:https?:)?\/\//i,
  /<image\b[^>]*\b(?:href|xlink:href)\s*=\s*["']\s*(?:https?:)?\/\//i,
  /url\(\s*["']?\s*(?:https?:)?\/\//i,
  /\b(?:image|coverImage|preview|full)\s*:\s*["']\s*(?:https?:)?\/\//i,
];
for (const entry of await readdir(staticRoot, {
  recursive: true,
  withFileTypes: true,
})) {
  if (!entry.isFile() || !/\.(?:html|css|js|svg)$/i.test(entry.name)) continue;
  const file = path.join(entry.parentPath || entry.path, entry.name),
    text = await readFile(file, "utf8");
  assert.ok(
    !externalImages.some((pattern) => pattern.test(text)),
    `unexpected external image import: ${path.relative(root, file)}`,
  );
}
for (const file of ["hero.json", "runway-looks.json", "product-photos.json"]) {
  const data = await json(path.join(staticRoot, file));
  const visit = (value) => {
    if (Array.isArray(value)) {
      value.forEach(visit);
      return;
    }
    if (!value || typeof value !== "object") return;
    for (const [key, item] of Object.entries(value)) {
      if (
        ["image", "preview", "full", "coverImage"].includes(key) &&
        typeof item === "string"
      )
        resourceFile(item);
      else visit(item);
    }
  };
  visit(data);
}
console.log(
  "PASS: 140 original photo hashes/resources; 9 card/full pairs; 6 model looks; no external image imports.",
);
