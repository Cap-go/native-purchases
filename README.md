# native-purchases
  <a href="https://capgo.app/"><img src='https://raw.githubusercontent.com/Cap-go/capgo/main/assets/capgo_banner.png' alt='Capgo - Instant updates for capacitor'/></a>
  
<div align="center">
<h2><a href="https://capgo.app/">Check out: Capgo — live updates for capacitor</a></h2>
</div>

In-app Subscriptions Made Easy

## Install

```bash
npm install native-purchases
npx cap sync
```

## Android

Add this to manifest

```xml
<uses-permission android:name="com.android.vending.BILLING" />
```

## API

<docgen-index>

* [`restorePurchases()`](#restorepurchases)
* [`purchaseProduct(...)`](#purchaseproduct)
* [`getProducts(...)`](#getproducts)
* [Interfaces](#interfaces)

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
purchaseProduct(options: { productIdentifier: string; quantity: number; }) => any
```

| Param         | Type                                                          |
| ------------- | ------------------------------------------------------------- |
| **`options`** | <code>{ productIdentifier: string; quantity: number; }</code> |

**Returns:** <code>any</code>

--------------------


### getProducts(...)

```typescript
getProducts(options: { productIdentifiers: string[]; }) => any
```

| Param         | Type                                     |
| ------------- | ---------------------------------------- |
| **`options`** | <code>{ productIdentifiers: {}; }</code> |

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

| Prop                        | Type                | Description                                                        |
| --------------------------- | ------------------- | ------------------------------------------------------------------ |
| **`transactionIdentifier`** | <code>string</code> | RevenueCat Id associated to the transaction.                       |
| **`productIdentifier`**     | <code>string</code> | <a href="#product">Product</a> Id associated with the transaction. |
| **`purchaseDate`**          | <code>string</code> | Purchase date of the transaction in ISO 8601 format.               |


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

</docgen-api>
