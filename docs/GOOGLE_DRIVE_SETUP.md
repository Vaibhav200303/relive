# Google Drive backup setup

Relive Android uses Credential Manager to choose an account and Google Identity Services `AuthorizationClient` to request a short-lived Drive access token. It requests exactly:

`https://www.googleapis.com/auth/drive.appdata`

Google classifies this as a **non-sensitive** scope. It grants access only to the hidden `appDataFolder` owned by Relive; the app cannot list or modify ordinary Drive files. The code creates resumable uploads under `appDataFolder` and never persists access or refresh tokens.

Official references: [Drive app data](https://developers.google.com/workspace/drive/api/guides/appdata), [OAuth app states](https://developers.google.com/identity/protocols/oauth2/production-readiness/overview), [Android OAuth clients](https://developers.google.com/workspace/guides/create-credentials), and [OAuth branding requirements](https://support.google.com/cloud/answer/15549049).

## One-time Google Cloud configuration

1. Open [Google Cloud Console](https://console.cloud.google.com/) and select the project intended for Relive production.
2. Go to **APIs & Services → Library**, find **Google Drive API**, and click **Enable**.
3. Go to **Google Auth Platform → Branding**.
   - App name: `Relive`
   - User support email: an actively monitored address
   - App homepage: a public HTTPS page on a domain you own that explains Relive and links to the same Privacy Policy
   - Privacy Policy: a public HTTPS page explaining the use of Google account identity and private Drive app-data backup
   - Terms of Service: the public Terms page used by the app
   - Authorized domains: the registrable domains used by those URLs
   - Developer contact: an actively monitored address
4. Verify ownership of every authorized domain in Google Search Console using a Google account that is an owner or editor of the Cloud project.
5. Go to **Google Auth Platform → Data Access → Add or remove scopes** and select only `.../auth/drive.appdata` plus the basic identity scopes automatically required by Sign in with Google. Do not add `drive`, `drive.readonly`, or `drive.file`.
6. Go to **Google Auth Platform → Audience**. Select **External**, then change Publishing status from **Testing** to **In production**. Testing is limited to explicitly allowlisted test users; External/In production removes that requirement.
7. Complete brand verification when prompted. Because `drive.appdata` is non-sensitive, Relive does not require sensitive-scope verification, restricted-scope security assessment, or a restricted-scope verification video solely for this scope. Google may still review the brand, domain ownership, homepage, and privacy disclosures.

## OAuth clients

Create clients under **Google Auth Platform → Clients → Create Client**:

1. **Web application** client. No client secret is used by this app. Copy its public client ID into `RELIVE_GOOGLE_WEB_CLIENT_ID`; Credential Manager passes it to `setServerClientId`.
2. **Android** client for production:
   - Package: `com.vaibhav.relive`
   - SHA-1: the release upload/app-signing certificate fingerprint
3. **Android** client for the Shipaton demo:
   - Package: `com.vaibhav.relive.demo`
   - SHA-1: the certificate used to sign the demo APK
4. **Android** client for the friends APK:
   - Package: `com.vaibhav.relive.friends`
   - SHA-1: the certificate used to sign the friends APK (the standard local debug certificate when sharing `friendsDebug`, or the locally configured release certificate for `friendsRelease`)
5. If Play App Signing is enabled, add a production Android client for the Play App Signing SHA-1 from **Play Console → Release → Setup → App integrity**. The local upload-key and Play app-signing fingerprints are different and can both need clients.

Find local fingerprints with:

```bash
./gradlew :androidApp:signingReport
```

The production SHA-256 is useful for Android App Links and other certificate association, but the Google Android OAuth client form currently requires SHA-1. Record both fingerprints in the release checklist.

## Local configuration

Put the Web client ID in an environment variable, Gradle user property, or untracked `local.properties`:

```properties
RELIVE_GOOGLE_WEB_CLIENT_ID=000000000000-example.apps.googleusercontent.com
```

OAuth client IDs are public identifiers, but keeping deployment configuration out of source avoids coupling a public clone to one Cloud project. Never commit an OAuth client secret, access token, refresh token, service-account file, or `google-services.json`.

## Physical verification

Use a Google account that is not a Cloud test user after publishing:

1. Install the correctly signed variant.
2. Open **Profile → Backup → Connect Google account**.
3. Select the account and grant the single private app-data permission.
4. Create Moments with text, tags, timelines, favorites, places, images, video, and audio.
5. Tap **Back up now** and wait for the verified upload result.
6. On a clean installation signed for the same OAuth client, connect the same account and choose Restore.
7. Confirm that the replacement warning appears and every Moment, relationship, and media attachment returns.
8. Revoke Relive under Google Account third-party connections and confirm the app requests reconnection instead of crashing.

Until the Cloud project is External/In production, the correct Android clients exist for the signing certificates, and this physical test passes, arbitrary-user Drive readiness remains blocked.
