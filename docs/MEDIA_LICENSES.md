# Licensed editorial photography

Reviewed on **3 November 2026**. This is a separate addition to the inherited media inventory; it does not establish rights for the rest of the catalog.

| Item | Source and use |
| --- | --- |
| Photo | [Man in brown T-shirt and dark jeans](https://www.pexels.com/photo/man-in-brown-crew-neck-t-shirt-and-black-denim-jeans-3116018/) by **Jonnas Duarte**, Pexels photo 3116018 |
| License | [Pexels License](https://www.pexels.com/license/): website use and modification allowed; do not imply endorsement or resell unaltered copies as stock/prints. The source photo is marked Free. |
| Photography evidence | The source page lists a Canon EOS Rebel T6i, capture date 21 July 2019 and upload date 22 October 2019. No generated photo was used. |
| Application | Homepage City Notes editorial. The pictured clothes are **not mapped to catalog products**, and the interface says so. The person does not endorse this project. |
| Assets | `static/media/licensed/city-editorial-600.webp` and `city-editorial-1200.webp`: 600×900 / 1200×1800, 86,028 / 264,588 bytes. |
| Processing | Pexels CDN resize and WebP encoding, plus responsive CSS crop. No alteration to the person or clothing. Local serving avoids third-party image requests during page viewing. |

`src/main/resources/licensed-photo-manifest.json` records download URLs, dimensions, byte counts and SHA-256 hashes. `npm run check` verifies the local file signatures and hashes. The browser selects a 600w or 1200w source, with explicit dimensions, async decoding and high fetch priority for the editorial image.

## Remaining media review

The original 140 images, nine product mappings and six-look controls are preserved unchanged. Their checksums establish provenance only. An upstream license covering those photos is still unconfirmed; see [ATTRIBUTION](ATTRIBUTION.md) and [third-party notices](../THIRD_PARTY_NOTICES.md). This change must not be described as a fully licensed catalog or a complete photography replacement.

Straight Trousers and Weekend Tote still use clearly described SVG illustrations. No unrelated stock image was assigned to those products, and no production database or product identity was changed.
