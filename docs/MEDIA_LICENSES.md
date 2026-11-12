# Licensed editorial photography

Reviewed on **3 and 11 November 2026**. This is a separate addition to the inherited media inventory; it does not establish rights for the rest of the catalog.

| Item | Source and use |
| --- | --- |
| Photo | [Man in brown T-shirt and dark jeans](https://www.pexels.com/photo/man-in-brown-crew-neck-t-shirt-and-black-denim-jeans-3116018/) by **Jonnas Duarte**, Pexels photo 3116018 |
| License | [Pexels License](https://www.pexels.com/license/): website use and modification allowed; do not imply endorsement or resell unaltered copies as stock/prints. The source photo is marked Free. |
| Photography evidence | The source page lists a Canon EOS Rebel T6i, capture date 21 July 2019 and upload date 22 October 2019. No generated photo was used. |
| Application | Homepage City Notes editorial. The pictured clothes are **not mapped to catalog products**, and the interface says so. The person does not endorse this project. |
| Assets | `static/media/licensed/city-editorial-600.webp` and `city-editorial-1200.webp`: 600×900 / 1200×1800, 86,028 / 264,588 bytes. |
| Processing | Pexels CDN resize and WebP encoding, plus responsive CSS crop. No alteration to the person or clothing. Local serving avoids third-party image requests during page viewing. |

The homepage's **Mặc gì, mang gì?** section adds two photographs with the same Pexels license. Both are editorial references, with links to catalog categories for exploring the demo. Neither photo identifies a catalog item.

| Local image | Source | Photography evidence |
| --- | --- | --- |
| `wardrobe-rail-480.webp`, `wardrobe-rail-960.webp` | [Clothing rack, Alena Darmel](https://www.pexels.com/photo/close-up-of-a-clothing-rack-8989864/) | Canon EOS 5D Mark IV |
| `everyday-tote-480.webp`, `everyday-tote-960.webp` | [Brown tote bag, Vie Studio](https://www.pexels.com/photo/brown-tote-bag-on-white-table-8148587/) | Fujifilm X-T3 |

`src/main/resources/licensed-photo-manifest.json` records download URLs, dimensions, byte counts and SHA-256 hashes. `npm run check` verifies the local file signatures and hashes. The hero uses 600w and 1200w variants. The two cards use lazy-loaded 480w and 960w variants. The website serves all variants locally.

## Remaining media review

The original 140 images, nine product mappings and six-look controls are preserved unchanged. Their checksums establish provenance only. An upstream license covering those photos is still unconfirmed; see [ATTRIBUTION](ATTRIBUTION.md) and [third-party notices](../THIRD_PARTY_NOTICES.md). This change must not be described as a fully licensed catalog or a complete photography replacement.

Straight Trousers and Weekend Tote still use clearly described SVG illustrations. No unrelated stock image was assigned to those products, and no production database or product identity was changed.
