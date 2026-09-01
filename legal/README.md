# Relive legal website

This folder is a dependency-free static website for GitHub Pages. It contains the landing page, Privacy Policy, Terms of Service, and shared stylesheet.

## Before publishing

Replace every occurrence of these placeholders in `index.html`, `privacy.html`, and `terms.html`:

- `REPLACE_WITH_CONTACT_EMAIL`
- `REPLACE_WITH_EFFECTIVE_DATE`
- `REPLACE_WITH_JURISDICTION`

Have the completed legal text reviewed for the jurisdictions in which Relive will be offered. Re-check the policy whenever a new SDK, analytics tool, backup target, account flow, or data collection feature is added.

## Publish with GitHub Pages

1. Replace the placeholders and commit the `legal/` folder to the repository’s default branch, or to the branch you intend to publish.
2. On GitHub, open the repository and choose **Settings → Pages**.
3. Under **Build and deployment**, select **Deploy from a branch**.
4. Select the publishing branch and the `/ (root)` folder, then choose **Save**. Because `legal/` is a subfolder, GitHub Pages will serve these files at the paths below.
5. Wait for the Pages deployment to finish. The repository’s **Settings → Pages** panel shows the site’s exact base URL.

The final URLs will look like:

```text
https://USERNAME.github.io/REPOSITORY/
https://USERNAME.github.io/REPOSITORY/privacy.html
https://USERNAME.github.io/REPOSITORY/terms.html
```

If the repository is published as a user or organization site, the `/REPOSITORY` segment may be absent. Do not put a guessed URL into the app configuration.

## Relive app configuration

After GitHub Pages is live, place the actual URLs in the developer-local `local.properties` file (do not commit secrets or local configuration):

```properties
RELIVE_TERMS_OF_SERVICE_URL=https://USERNAME.github.io/REPOSITORY/terms.html
RELIVE_PRIVACY_POLICY_URL=https://USERNAME.github.io/REPOSITORY/privacy.html
```

These values are intentionally not changed automatically because the final GitHub username, repository name, and Pages configuration are not known yet.
