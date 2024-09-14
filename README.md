# native-purchases
 <a href="https://capgo.app/"><img src='https://raw.githubusercontent.com/Cap-go/capgo/main/assets/capgo_banner.png' alt='Capgo - Instant updates for capacitor'/></a>

<div align="center">
  <h2><a href="https://capgo.app/?ref=plugin"> ➡️ Get Instant updates for your App with Capgo 🚀</a></h2>
  <h2><a href="https://capgo.app/consulting/?ref=plugin"> Fix your annoying bug now, Hire a Capacitor expert 💪</a></h2>
</div>

## In-app Purchases Made Easy

This plugin allows you to implement in-app purchases and subscriptions in your Capacitor app using native APIs.

## Install

```bash
npm install @capgo/native-purchases
npx cap sync
```

## Android

Add this to manifest

```xml
<uses-permission android:name="com.android.vending.BILLING" />
```

## Usage

Import the plugin in your TypeScript file:

```typescript
import { NativePurchases } from '@capgo/native-purchases';
```

### Check if billing is supported

Before attempting to make purchases, check if billing is supported on the device:
We only support Storekit 2 on iOS (iOS 15+) and google play on Android

```typescript
const checkBillingSupport = async () => {
  try {
    const { isBillingSupported } = await NativePurchases.isBillingSupported();
    if (isBillingSupported) {
      console.log('Billing is supported on this device');
    } else {
      console.log('Billing is not supported on this device');
    }
  } catch (error) {
    console.error('Error checking billing support:', error);
  }
};
```

### Get available products

Retrieve information about available products:

```typescript
const getAvailableProducts = async () => {
  try {
    const { products } = await NativePurchases.getProducts({
      productIdentifiers: ['product_id_1', 'product_id_2'],
      productType: PURCHASE_TYPE.INAPP // or PURCHASE_TYPE.SUBS for subscriptions
    });
    console.log('Available products:', products);
  } catch (error) {
    console.error('Error getting products:', error);
  }
};
```

### Purchase a product

To initiate a purchase:

```typescript
const purchaseProduct = async (productId: string) => {
  try {
    const transaction = await NativePurchases.purchaseProduct({
      productIdentifier: productId,
      productType: PURCHASE_TYPE.INAPP // or PURCHASE_TYPE.SUBS for subscriptions
    });
    console.log('Purchase successful:', transaction);
    // Handle the successful purchase (e.g., unlock content, update UI)
  } catch (error) {
    console.error('Purchase failed:', error);
  }
};
```

### Restore purchases

To restore previously purchased products:

```typescript
const restorePurchases = async () => {
  try {
    const { customerInfo } = await NativePurchases.restorePurchases();
    console.log('Restored purchases:', customerInfo);
    // Update your app's state based on the restored purchases
  } catch (error) {
    console.error('Failed to restore purchases:', error);
  }
};
```

## Example: Implementing a simple store

Here's a basic example of how you might implement a simple store in your app:

```typescript
import { Capacitor } from '@capacitor/core';
import { NativePurchases, PURCHASE_TYPE, Product } from '@capgo/native-purchases';

class Store {
  private products: Product[] = [];

  async initialize() {
    if (Capacitor.isNativePlatform()) {
      try {
        await this.checkBillingSupport();
        await this.loadProducts();
      } catch (error) {
        console.error('Store initialization failed:', error);
      }
    }
  }

  private async checkBillingSupport() {
    const { isBillingSupported } = await NativePurchases.isBillingSupported();
    if (!isBillingSupported) {
      throw new Error('Billing is not supported on this device');
    }
  }

  private async loadProducts() {
    const productIds = ['premium_subscription', 'remove_ads', 'coin_pack'];
    const { products } = await NativePurchases.getProducts({
      productIdentifiers: productIds,
      productType: PURCHASE_TYPE.INAPP
    });
    this.products = products;
  }

  getProducts() {
    return this.products;
  }

  async purchaseProduct(productId: string) {
    try {
      const transaction = await NativePurchases.purchaseProduct({
        productIdentifier: productId,
        productType: PURCHASE_TYPE.INAPP
      });
      console.log('Purchase successful:', transaction);
      // Handle the successful purchase
      return transaction;
    } catch (error) {
      console.error('Purchase failed:', error);
      throw error;
    }
  }

  async restorePurchases() {
    try {
      const { customerInfo } = await NativePurchases.restorePurchases();
      console.log('Restored purchases:', customerInfo);
      // Update app state based on restored purchases
      return customerInfo;
    } catch (error) {
      console.error('Failed to restore purchases:', error);
      throw error;
    }
  }
}

// Usage
const store = new Store();
await store.initialize();

// Display products
const products = store.getProducts();
console.log('Available products:', products);

// Purchase a product
try {
  await store.purchaseProduct('premium_subscription');
  console.log('Purchase completed successfully');
} catch (error) {
  console.error('Purchase failed:', error);
}

// Restore purchases
try {
  await store.restorePurchases();
  console.log('Purchases restored successfully');
} catch (error) {
  console.error('Failed to restore purchases:', error);
}
```

This example provides a basic structure for initializing the store, loading products, making purchases, and restoring previous purchases. You'll need to adapt this to fit your specific app's needs, handle UI updates, and implement proper error handling and user feedback.

## Backend Validation

It's crucial to validate receipts on your server to ensure the integrity of purchases. Here's an example of how to implement backend validation using a Cloudflare Worker:

Cloudflare Worker Setup
Create a new Cloudflare Worker and follow the instructions in folder (`validator`)[/validator/README.md]

Then in your app, modify the purchase function to validate the receipt on the server:

```typescript
import { Capacitor } from '@capacitor/core';
import { NativePurchases, PURCHASE_TYPE, Product, Transaction } from '@capgo/native-purchases';
import axios from 'axios'; // Make sure to install axios: npm install axios

class Store {
  // ... (previous code remains the same)

  async purchaseProduct(productId: string) {
    try {
      const transaction = await NativePurchases.purchaseProduct({
        productIdentifier: productId,
        productType: PURCHASE_TYPE.INAPP
      });
      console.log('Purchase successful:', transaction);
      
      // Immediately grant access to the purchased content
      await this.grantAccess(productId);
      
      // Initiate server-side validation asynchronously
      this.validatePurchaseOnServer(transaction).catch(console.error);
      
      return transaction;
    } catch (error) {
      console.error('Purchase failed:', error);
      throw error;
    }
  }

  private async grantAccess(productId: string) {
    // Implement logic to grant immediate access to the purchased content
    console.log(`Granting access to ${productId}`);
    // Update local app state, unlock features, etc.
  }

  private async validatePurchaseOnServer(transaction: Transaction) {
    const serverUrl = 'https://your-server-url.com/validate-purchase';
    try {
      const response = await axios.post(serverUrl, {
        transactionId: transaction.transactionId,
        platform: Capacitor.getPlatform(),
        // Include any other relevant information
      });

      console.log('Server validation response:', response.data);
      // The server will handle the actual validation with the Cloudflare Worker
    } catch (error) {
      console.error('Error in server-side validation:', error);
      // Implement retry logic or notify the user if necessary
    }
  }
}

// Usage remains the same
const store = new Store();
await store.initialize();

try {
  await store.purchaseProduct('premium_subscription');
  console.log('Purchase completed successfully');
} catch (error) {
  console.error('Purchase failed:', error);
}
```

Now, let's look at how the server-side (Node.js) code might handle the validation:

```typescript
import express from 'express';
import axios from 'axios';

const app = express();
app.use(express.json());

const CLOUDFLARE_WORKER_URL = 'https://your-cloudflare-worker-url.workers.dev';

app.post('/validate-purchase', async (req, res) => {
  const { transactionId, platform } = req.body;

  try {
    const endpoint = platform === 'ios' ? '/apple' : '/google';
    const validationResponse = await axios.post(`${CLOUDFLARE_WORKER_URL}${endpoint}`, {
      receipt: transactionId
    });

    const validationResult = validationResponse.data;

    // Process the validation result
    if (validationResult.isValid) {
      // Update user status in the database
      // await updateUserStatus(userId, 'paid');
      
      // Log the successful validation
      console.log(`Purchase validated for transaction ${transactionId}`);
      
      // You might want to store the validation result for future reference
      // await storeValidationResult(userId, transactionId, validationResult);
    } else {
      // Handle invalid purchase
      console.warn(`Invalid purchase detected for transaction ${transactionId}`);
      // You might want to flag this for further investigation
      // await flagSuspiciousPurchase(userId, transactionId);
    }

    // Always respond with a success to the app
    // This ensures the app doesn't block the user's access
    res.json({ success: true });
  } catch (error) {
    console.error('Error validating purchase:', error);
    // Still respond with success to the app
    res.json({ success: true });
    // You might want to log this error or retry the validation later
    // await logValidationError(userId, transactionId, error);
  }
});

// Start the server
app.listen(3000, () => console.log('Server running on port 3000'));
```

Key points about this approach:

1. The app immediately grants access after a successful purchase, ensuring a smooth user experience.
2. The app initiates server-side validation asynchronously, not blocking the user's access.
3. The server handles the actual validation by calling the Cloudflare Worker.
4. The server always responds with success to the app, even if validation fails or encounters an error.
5. The server can update the user's status in the database, log results, and handle any discrepancies without affecting the user's immediate experience.

Comments on best practices:

```typescript
// After successful validation:
// await updateUserStatus(userId, 'paid');

// It's crucial to not block or revoke access immediately if validation fails
// Instead, flag suspicious transactions for review:
// if (!validationResult.isValid) {
//   await flagSuspiciousPurchase(userId, transactionId);
// }

// Implement a system to periodically re-check flagged purchases
// This could be a separate process that runs daily/weekly

// Consider implementing a grace period for new purchases
// This allows for potential delays in server communication or store processing
// const GRACE_PERIOD_DAYS = 3;
// if (daysSincePurchase < GRACE_PERIOD_DAYS) {
//   grantAccess = true;
// }

// For subscriptions, regularly check their status with the stores
// This ensures you catch any cancelled or expired subscriptions
// setInterval(checkSubscriptionStatuses, 24 * 60 * 60 * 1000); // Daily check

// Implement proper error handling and retry logic for network failures
// This is especially important for the server-to-Cloudflare communication

// Consider caching validation results to reduce load on your server and the stores
// const cachedValidation = await getCachedValidation(transactionId);
// if (cachedValidation) return cachedValidation;
```

This approach balances immediate user gratification with proper server-side validation, adhering to Apple and Google's guidelines while still maintaining the integrity of your purchase system.

## API

<docgen-index>

* [`restorePurchases()`](#restorepurchases)
* [`purchaseProduct(...)`](#purchaseproduct)
* [`getProducts(...)`](#getproducts)
* [`getProduct(...)`](#getproduct)
* [`isBillingSupported()`](#isbillingsupported)
* [`getPluginVersion()`](#getpluginversion)
* [Interfaces](#interfaces)
* [Enums](#enums)

</docgen-index>

<docgen-api>
<!--Update the source file JSDoc comments and rerun docgen to update the docs below-->

### restorePurchases()

```typescript
restorePurchases() => any
```

Restores a user's previous  and links their appUserIDs to any user's also using those .

**Returns:** <code>any</code>

--------------------


### purchaseProduct(...)

```typescript
purchaseProduct(options: { productIdentifier: string; planIdentifier?: string; productType?: PURCHASE_TYPE; quantity?: number; }) => any
```

Started purchase process for the given product.

| Param         | Type                                                                                                                                              | Description               |
| ------------- | ------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------- |
| **`options`** | <code>{ productIdentifier: string; planIdentifier?: string; productType?: <a href="#purchase_type">PURCHASE_TYPE</a>; quantity?: number; }</code> | - The product to purchase |

**Returns:** <code>any</code>

--------------------


### getProducts(...)

```typescript
getProducts(options: { productIdentifiers: string[]; productType?: PURCHASE_TYPE; }) => any
```

Gets the product info associated with a list of product identifiers.

| Param         | Type                                                                                               | Description                                                    |
| ------------- | -------------------------------------------------------------------------------------------------- | -------------------------------------------------------------- |
| **`options`** | <code>{ productIdentifiers: {}; productType?: <a href="#purchase_type">PURCHASE_TYPE</a>; }</code> | - The product identifiers you wish to retrieve information for |

**Returns:** <code>any</code>

--------------------


### getProduct(...)

```typescript
getProduct(options: { productIdentifier: string; productType?: PURCHASE_TYPE; }) => any
```

Gets the product info for a single product identifier.

| Param         | Type                                                                                                  | Description                                                   |
| ------------- | ----------------------------------------------------------------------------------------------------- | ------------------------------------------------------------- |
| **`options`** | <code>{ productIdentifier: string; productType?: <a href="#purchase_type">PURCHASE_TYPE</a>; }</code> | - The product identifier you wish to retrieve information for |

**Returns:** <code>any</code>

--------------------


### isBillingSupported()

```typescript
isBillingSupported() => any
```

Check if billing is supported for the current device.

**Returns:** <code>any</code>

--------------------


### getPluginVersion()

```typescript
getPluginVersion() => any
```

Get the native Capacitor plugin version

**Returns:** <code>any</code>

--------------------


### Interfaces


#### CustomerInfo

| Prop                                 | Type                        | Description                                                                                                                                                                                                                                                                                                                                      |
| ------------------------------------ | --------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| **`activeSubscriptions`**            | <code>[string]</code>       | Set of active subscription skus                                                                                                                                                                                                                                                                                                                  |
| **`allPurchasedProductIdentifiers`** | <code>[string]</code>       | Set of purchased skus, active and inactive                                                                                                                                                                                                                                                                                                       |
| **`nonSubscriptionTransactions`**    | <code>{}</code>             | Returns all the non-subscription a user has made. The are ordered by purchase date in ascending order.                                                                                                                                                                                                                                           |
| **`latestExpirationDate`**           | <code>string \| null</code> | The latest expiration date of all purchased skus                                                                                                                                                                                                                                                                                                 |
| **`firstSeen`**                      | <code>string</code>         | The date this user was first seen in RevenueCat.                                                                                                                                                                                                                                                                                                 |
| **`originalAppUserId`**              | <code>string</code>         | The original App User Id recorded for this user.                                                                                                                                                                                                                                                                                                 |
| **`requestDate`**                    | <code>string</code>         | Date when this info was requested                                                                                                                                                                                                                                                                                                                |
| **`originalApplicationVersion`**     | <code>string \| null</code> | Returns the version number for the version of the application when the user bought the app. Use this for grandfathering users when migrating to subscriptions. This corresponds to the value of CFBundleVersion (in iOS) in the Info.plist file when the purchase was originally made. This is always null in Android                            |
| **`originalPurchaseDate`**           | <code>string \| null</code> | Returns the purchase date for the version of the application when the user bought the app. Use this for grandfathering users when migrating to subscriptions.                                                                                                                                                                                    |
| **`managementURL`**                  | <code>string \| null</code> | URL to manage the active subscription of the user. If this user has an active iOS subscription, this will point to the App Store, if the user has an active Play Store subscription it will point there. If there are no active subscriptions it will be null. If there are multiple for different platforms, it will point to the device store. |


#### Transaction

| Prop                | Type                | Description                                  |
| ------------------- | ------------------- | -------------------------------------------- |
| **`transactionId`** | <code>string</code> | RevenueCat Id associated to the transaction. |


#### Product

| Prop                              | Type                                                                    | Description                                                              |
| --------------------------------- | ----------------------------------------------------------------------- | ------------------------------------------------------------------------ |
| **`identifier`**                  | <code>string</code>                                                     | <a href="#product">Product</a> Id.                                       |
| **`description`**                 | <code>string</code>                                                     | Description of the product.                                              |
| **`title`**                       | <code>string</code>                                                     | Title of the product.                                                    |
| **`price`**                       | <code>number</code>                                                     | Price of the product in the local currency.                              |
| **`priceString`**                 | <code>string</code>                                                     | Formatted price of the item, including its currency sign, such as €3.99. |
| **`currencyCode`**                | <code>string</code>                                                     | Currency code for price and original price.                              |
| **`currencySymbol`**              | <code>string</code>                                                     | Currency symbol for price and original price.                            |
| **`isFamilyShareable`**           | <code>boolean</code>                                                    | Boolean indicating if the product is sharable with family                |
| **`subscriptionGroupIdentifier`** | <code>string</code>                                                     | Group identifier for the product.                                        |
| **`subscriptionPeriod`**          | <code><a href="#subscriptionperiod">SubscriptionPeriod</a></code>       | The <a href="#product">Product</a> subcription group identifier.         |
| **`introductoryPrice`**           | <code><a href="#skproductdiscount">SKProductDiscount</a> \| null</code> | The <a href="#product">Product</a> introductory Price.                   |
| **`discounts`**                   | <code>{}</code>                                                         | The <a href="#product">Product</a> discounts list.                       |


#### SubscriptionPeriod

| Prop                | Type                | Description                             |
| ------------------- | ------------------- | --------------------------------------- |
| **`numberOfUnits`** | <code>number</code> | The Subscription Period number of unit. |
| **`unit`**          | <code>number</code> | The Subscription Period unit.           |


#### SKProductDiscount

| Prop                     | Type                                                              | Description                                                              |
| ------------------------ | ----------------------------------------------------------------- | ------------------------------------------------------------------------ |
| **`identifier`**         | <code>string</code>                                               | The <a href="#product">Product</a> discount identifier.                  |
| **`type`**               | <code>number</code>                                               | The <a href="#product">Product</a> discount type.                        |
| **`price`**              | <code>number</code>                                               | The <a href="#product">Product</a> discount price.                       |
| **`priceString`**        | <code>string</code>                                               | Formatted price of the item, including its currency sign, such as €3.99. |
| **`currencySymbol`**     | <code>string</code>                                               | The <a href="#product">Product</a> discount currency symbol.             |
| **`currencyCode`**       | <code>string</code>                                               | The <a href="#product">Product</a> discount currency code.               |
| **`paymentMode`**        | <code>number</code>                                               | The <a href="#product">Product</a> discount paymentMode.                 |
| **`numberOfPeriods`**    | <code>number</code>                                               | The <a href="#product">Product</a> discount number Of Periods.           |
| **`subscriptionPeriod`** | <code><a href="#subscriptionperiod">SubscriptionPeriod</a></code> | The <a href="#product">Product</a> discount subscription period.         |


### Enums


#### PURCHASE_TYPE

| Members     | Value                | Description                        |
| ----------- | -------------------- | ---------------------------------- |
| **`INAPP`** | <code>"inapp"</code> | A type of SKU for in-app products. |
| **`SUBS`**  | <code>"subs"</code>  | A type of SKU for subscriptions.   |

</docgen-api>
