# Render Free — public portfolio demo

This is a deliberately **ephemeral public demo**, not a production deployment.

## Apply the Blueprint

1. Merge the branch containing `render.yaml` into `main` after CI passes.
2. Open [Deploy Blueprint](https://dashboard.render.com/blueprint/new?repo=https://github.com/nguyenhonggiang2407/RunwayStudio).
3. Select the **Free** plan and review that the Blueprint only creates **one web service**, with no paid database, disk, or background worker.
4. Confirm Render has access to the repository, then apply.
5. Open the actual `onrender.com` URL shown by Render, and verify the health endpoint and storefront before publishing a GitHub Live Demo link.

The Blueprint uses the existing multi-stage `Dockerfile` (Java 17), the existing `demo` Spring profile, and H2 in-memory database. Render supplies randomized secrets for local-demo accounts, so the hard-coded *local only* passwords in the README are **not valid** for the public deployment. A visitor may register a fresh customer account for testing; do not share or publish the generated administrator secret.

## Expected limits

- The Render Free web instance may sleep when idle and take time to wake.
- H2 is memory-only in this deployment. Restart/redeploy erases demo customers, orders, outfits and any other changes, then re-creates the seed catalog.
- No actual payment, fulfillment, or real customer data should be used. Do not advertise reliable storage or a production shop.
- Free web instances have limited CPU and memory. Monitor build, startup and health. If the JVM exceeds the memory budget, diagnose logs and tune memory settings; do not silently upgrade to paid resources.
- `SPRING_PROFILES_ACTIVE=demo` is intentional here **only for the ephemeral public demo**; it must not be reused for persistent production, where `mysql,prod` and no demo seed are expected.

## Verify before adding a Live Demo link

- Render deploy status is **Live** and `/actuator/health/readiness` reports UP.
- Homepage, category filter, product details, and Outfit Studio load.
- New user can register, add to cart, and exercise COD with fictional details.
- Browser shows HTTPS and session cookies with Secure attribute.
- Account A cannot read Account B's orders, wardrobe or private outfits.
- An ordinary customer cannot access `/api/admin/**`.
- Restart behavior is documented on the public demo page/README.

Do not put an assumed URL such as `https://runwaystudio-portfolio-demo.onrender.com` into the README until Render actually confirms the domain and the checks above.

The repository's existing GitHub Actions verify Java, MySQL, browser and security tests. Their success is necessary but does **not** prove the deployment is healthy.
