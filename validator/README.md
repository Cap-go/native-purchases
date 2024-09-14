# Receipt Validator

This project provides a Cloudflare Worker for validating Apple and Google receipts.

## Installation

1. Copy the project files to your local machine.

2. Set up environment variables:
   - `APPLE_SECRET`: Your Apple shared secret key. See [Generating a Shared Secret](https://developer.apple.com/documentation/appstorereceipts/generating_a_shared_secret) for more information.
   - `GOOGLE_SERVICE_ACCOUNT`: Your Google service account JSON key. See [Using a service account](https://developers.google.com/android-publisher/getting_started#using_a_service_account) for more information.
   - `GOOGLE_PACKAGE_NAME`: Your Google Play package name.

   You can set these environment variables in your Cloudflare Worker settings. See [Environment Variables](https://developers.cloudflare.com/workers/platform/environment-variables/) for more information.

3. Deploy the worker:
   ```
   wrangler publish
   ```

## Usage

### Apple Receipt Validation

To validate an Apple receipt, send a POST request to `/apple` with the following JSON payload:

```json
{
"receipt": "your_apple_receipt_data"
}
```

### Google Receipt Validation

To validate a Google receipt, send a POST request to `/google` with the following JSON payload:

```json
{
"receipt": "your_google_receipt_data"
}
```


The worker will respond with the validation result in JSON format.
