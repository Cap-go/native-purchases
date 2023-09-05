package ee.forgr.nativepurchases;

import android.util.Log;
import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.AcknowledgePurchaseResponseListener;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.ConsumeResponseListener;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.ProductDetailsResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Phaser;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.json.JSONArray;
import org.json.JSONException;

@CapacitorPlugin(name = "NativePurchases")
public class NativePurchasesPlugin extends Plugin {

  public final String PLUGIN_VERSION = "0.0.25";
  public static final String TAG = "NativePurchases";
  private static final Phaser semaphoreReady = new Phaser(1);
  private BillingClient billingClient;

  private void isBillingSupported(PluginCall call) {
    JSObject ret = new JSObject();
    ret.put("isBillingSupported", true);
    call.resolve();
  }

  @Override
  public void load() {
    super.load();
    Log.i(NativePurchasesPlugin.TAG, "load");
    semaphoreDown();
  }

  private void semaphoreWait(Number waitTime) {
    Log.i(NativePurchasesPlugin.TAG, "semaphoreWait " + waitTime);
    try {
      //        Log.i(CapacitorUpdater.TAG, "semaphoreReady count " + CapacitorUpdaterPlugin.this.semaphoreReady.getCount());
      NativePurchasesPlugin.this.semaphoreReady.awaitAdvanceInterruptibly(
          NativePurchasesPlugin.this.semaphoreReady.getPhase(),
          waitTime.longValue(),
          TimeUnit.SECONDS
        );
      //        Log.i(CapacitorUpdater.TAG, "semaphoreReady await " + res);
      Log.i(
        NativePurchasesPlugin.TAG,
        "semaphoreReady count " +
        NativePurchasesPlugin.this.semaphoreReady.getPhase()
      );
    } catch (InterruptedException e) {
      Log.i(NativePurchasesPlugin.TAG, "semaphoreWait InterruptedException");
      e.printStackTrace();
    } catch (TimeoutException e) {
      throw new RuntimeException(e);
    }
  }

  private void semaphoreUp() {
    Log.i(NativePurchasesPlugin.TAG, "semaphoreUp");
    NativePurchasesPlugin.this.semaphoreReady.register();
  }

  private void semaphoreDown() {
    Log.i(NativePurchasesPlugin.TAG, "semaphoreDown");
    Log.i(
      NativePurchasesPlugin.TAG,
      "semaphoreDown count " +
      NativePurchasesPlugin.this.semaphoreReady.getPhase()
    );
    NativePurchasesPlugin.this.semaphoreReady.arriveAndDeregister();
  }

  private void closeBillingClient() {
    if (billingClient != null) {
      billingClient.endConnection();
      billingClient = null;
      semaphoreDown();
    }
  }

  private void handlePurchase(Purchase purchase, PluginCall purchaseCall) {
    if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
      // Grant entitlement to the user, then acknowledge the purchase
      //     if sub then acknowledgePurchase
      //      if one time then consumePurchase
      if (purchase.isAcknowledged()) {
        ConsumeParams consumeParams = ConsumeParams
          .newBuilder()
          .setPurchaseToken(purchase.getPurchaseToken())
          .build();
        billingClient.consumeAsync(
          consumeParams,
          new ConsumeResponseListener() {
            @Override
            public void onConsumeResponse(
              BillingResult billingResult,
              String purchaseToken
            ) {
              // Handle the result
              Log.i(
                NativePurchasesPlugin.TAG,
                "onConsumeResponse" + billingResult + purchaseToken
              );
            }
          }
        );
      } else {
        acknowledgePurchase(purchase.getPurchaseToken());
      }

      JSObject ret = new JSObject();
      ret.put("transactionId", purchase.getPurchaseToken());
      if (purchaseCall != null) {
        purchaseCall.resolve(ret);
      }
    } else if (purchase.getPurchaseState() == Purchase.PurchaseState.PENDING) {
      // Here you can confirm to the user that they've started the pending
      // purchase, and to complete it, they should follow instructions that are
      // given to them. You can also choose to remind the user to complete the
      // purchase if you detect that it is still pending.
      if (purchaseCall != null) {
        purchaseCall.reject("Purchase is pending");
      }
    } else {
      // Handle any other error codes.
      if (purchaseCall != null) {
        purchaseCall.reject("Purchase is not purchased");
      }
    }
  }

  private void acknowledgePurchase(String purchaseToken) {
    AcknowledgePurchaseParams acknowledgePurchaseParams = AcknowledgePurchaseParams
      .newBuilder()
      .setPurchaseToken(purchaseToken)
      .build();
    billingClient.acknowledgePurchase(
      acknowledgePurchaseParams,
      new AcknowledgePurchaseResponseListener() {
        @Override
        public void onAcknowledgePurchaseResponse(BillingResult billingResult) {
          // Handle the result of the acknowledge purchase
          Log.i(
            NativePurchasesPlugin.TAG,
            "onAcknowledgePurchaseResponse" + billingResult
          );
        }
      }
    );
  }

  private void initBillingClient(PluginCall purchaseCall) {
    semaphoreWait(10);
    closeBillingClient();
    semaphoreUp();
    CountDownLatch semaphoreReady = new CountDownLatch(1);
    billingClient =
      BillingClient
        .newBuilder(getContext())
        .setListener(
          new PurchasesUpdatedListener() {
            @Override
            public void onPurchasesUpdated(
              BillingResult billingResult,
              List<Purchase> purchases
            ) {
              Log.i(
                NativePurchasesPlugin.TAG,
                "onPurchasesUpdated" + billingResult
              );
              if (
                billingResult.getResponseCode() ==
                BillingClient.BillingResponseCode.OK &&
                purchases != null
              ) {
                //                          for (Purchase purchase : purchases) {
                //                              handlePurchase(purchase, purchaseCall);
                //                          }
                handlePurchase(purchases.get(0), purchaseCall);
              } else {
                // Handle any other error codes.
                Log.i(
                  NativePurchasesPlugin.TAG,
                  "onPurchasesUpdated" + billingResult
                );
                if (purchaseCall != null) {
                  purchaseCall.reject("Purchase is not purchased");
                }
              }
              closeBillingClient();
              return;
            }
          }
        )
        .enablePendingPurchases()
        .build();
    billingClient.startConnection(
      new BillingClientStateListener() {
        @Override
        public void onBillingSetupFinished(BillingResult billingResult) {
          if (
            billingResult.getResponseCode() ==
            BillingClient.BillingResponseCode.OK
          ) {
            // The BillingClient is ready. You can query purchases here.
            semaphoreReady.countDown();
          }
        }

        @Override
        public void onBillingServiceDisconnected() {
          // Try to restart the connection on the next request to
          // Google Play by calling the startConnection() method.
        }
      }
    );
    try {
      semaphoreReady.await();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  @PluginMethod
  public void getPluginVersion(final PluginCall call) {
    try {
      final JSObject ret = new JSObject();
      ret.put("version", this.PLUGIN_VERSION);
      call.resolve(ret);
    } catch (final Exception e) {
      call.reject("Could not get plugin version", e);
    }
  }

  @PluginMethod
  public void purchaseProduct(PluginCall call) {
    String productIdentifier = call.getString("productIdentifier");
    String planIdentifier = call.getString("planIdentifier");
    String productType = call.getString("productType", "inapp");
    Number quantity = call.getInt("quantity", 1);
    // cannot use quantity, because it's done in native modal
    Log.d("CapacitorPurchases", "purchaseProduct: " + productIdentifier);
    if (productIdentifier.isEmpty()) {
      // Handle error: productIdentifier is empty
      call.reject("productIdentifier is empty");
      return;
    }
    if (productType.isEmpty()) {
      // Handle error: productType is empty
      call.reject("productType is empty");
      return;
    }
    if (productType.equals("subs") && planIdentifier.isEmpty()) {
      // Handle error: no planIdentifier with productType subs
      call.reject("planIdentifier cannot be empty if productType is subs");
      return;
    }
    if (quantity.intValue() < 1) {
      // Handle error: quantity is less than 1
      call.reject("quantity is less than 1");
      return;
    }
    ImmutableList<QueryProductDetailsParams.Product> productList = ImmutableList.of(
      QueryProductDetailsParams.Product
        .newBuilder()
        .setProductId(
          productType.equals("inapp") ? productIdentifier : planIdentifier
        )
        .setProductType(
          productType.equals("inapp")
            ? BillingClient.ProductType.INAPP
            : BillingClient.ProductType.SUBS
        )
        .build()
    );
    QueryProductDetailsParams params = QueryProductDetailsParams
      .newBuilder()
      .setProductList(productList)
      .build();
    this.initBillingClient(call);
    try {
      billingClient.queryProductDetailsAsync(
        params,
        new ProductDetailsResponseListener() {
          public void onProductDetailsResponse(
            BillingResult billingResult,
            List<ProductDetails> productDetailsList
          ) {
            if (productDetailsList.size() == 0) {
              closeBillingClient();
              call.reject("Product not found");
              return;
            }
            // Process the result
            List<BillingFlowParams.ProductDetailsParams> productDetailsParamsList = new ArrayList<>();
            for (ProductDetails productDetailsItem : productDetailsList) {
              BillingFlowParams.ProductDetailsParams.Builder productDetailsParams = BillingFlowParams.ProductDetailsParams
                .newBuilder()
                .setProductDetails(productDetailsItem);
              if (productType.equals("subs")) {
                // list the SubscriptionOfferDetails and find the one who match the planIdentifier if not found get the first one
                ProductDetails.SubscriptionOfferDetails selectedOfferDetails =
                  null;
                for (ProductDetails.SubscriptionOfferDetails offerDetails : productDetailsItem.getSubscriptionOfferDetails()) {
                  if (offerDetails.getBasePlanId().equals(planIdentifier)) {
                    selectedOfferDetails = offerDetails;
                    break;
                  }
                }
                if (selectedOfferDetails == null) {
                  selectedOfferDetails =
                    productDetailsItem.getSubscriptionOfferDetails().get(0);
                }
                productDetailsParams.setOfferToken(
                  selectedOfferDetails.getOfferToken()
                );
              }
              productDetailsParamsList.add(productDetailsParams.build());
            }
            BillingFlowParams billingFlowParams = BillingFlowParams
              .newBuilder()
              .setProductDetailsParamsList(productDetailsParamsList)
              .build();
            // Launch the billing flow
            BillingResult billingResult2 = billingClient.launchBillingFlow(
              getActivity(),
              billingFlowParams
            );
            Log.i(
              NativePurchasesPlugin.TAG,
              "onProductDetailsResponse2" + billingResult2
            );
          }
        }
      );
    } catch (Exception e) {
      closeBillingClient();
      call.reject(e.getMessage());
    }
  }

  @PluginMethod
  public void restorePurchases(PluginCall call) {
    Log.d(NativePurchasesPlugin.TAG, "restorePurchases");
    this.initBillingClient(null);
    call.resolve();
  }

  @PluginMethod
  public void getProducts(PluginCall call) {
    JSONArray productIdentifiersArray = call.getArray("productIdentifiers");
    String planIdentifier = call.getString("planIdentifier");
    String productType = call.getString("productType", "inapp");
    if (productType.equals("subs") && planIdentifier.isEmpty()) {
      // Handle error: no planIdentifier with productType subs
      call.reject("planIdentifier cannot be empty if productType is subs");
      return;
    }
    if (
      productIdentifiersArray == null || productIdentifiersArray.length() == 0
    ) {
      call.reject("productIdentifiers array missing");
      return;
    }
    List<String> productIdentifiers = new ArrayList<>();

    for (int i = 0; i < productIdentifiersArray.length(); i++) {
      try {
        productIdentifiers.add(productIdentifiersArray.getString(i));
      } catch (JSONException e) {
        e.printStackTrace();
      }
    }

    Log.d(NativePurchasesPlugin.TAG, "getProducts: " + productIdentifiers);
    List<QueryProductDetailsParams.Product> productList = new ArrayList<>();
    for (String productIdentifier : productIdentifiers) {
      productList.add(
        QueryProductDetailsParams.Product
          .newBuilder()
          .setProductId(
            productType.equals("inapp") ? productIdentifier : planIdentifier
          )
          .setProductType(
            productType.equals("inapp")
              ? BillingClient.ProductType.INAPP
              : BillingClient.ProductType.SUBS
          )
          .build()
      );
    }
    QueryProductDetailsParams params = QueryProductDetailsParams
      .newBuilder()
      .setProductList(productList)
      .build();
    this.initBillingClient(call);
    try {
      billingClient.queryProductDetailsAsync(
        params,
        new ProductDetailsResponseListener() {
          public void onProductDetailsResponse(
            BillingResult billingResult,
            List<ProductDetails> productDetailsList
          ) {
            if (productDetailsList.size() == 0) {
              closeBillingClient();
              call.reject("Product not found");
              return;
            }
            Log.i(
              NativePurchasesPlugin.TAG,
              "onProductDetailsResponse" + billingResult + productDetailsList
            );
            // Process the result
            JSObject ret = new JSObject();
            JSONArray products = new JSONArray();
            Number productIdIndex = 0;
            for (ProductDetails productDetails : productDetailsList) {
              JSObject product = new JSObject();
              product.put("title", productDetails.getName());
              product.put("description", productDetails.getDescription());
              if (productType.equals("inapp")) {
                product.put("identifier", productDetails.getProductId());
                product.put(
                  "price",
                  productDetails
                    .getOneTimePurchaseOfferDetails()
                    .getPriceAmountMicros() /
                  1000000.0
                );
                product.put(
                  "priceString",
                  productDetails
                    .getOneTimePurchaseOfferDetails()
                    .getFormattedPrice()
                );
                product.put(
                  "currencyCode",
                  productDetails
                    .getOneTimePurchaseOfferDetails()
                    .getPriceCurrencyCode()
                );
              } else {
                // productIdIndex is used to get the correct SubscriptionOfferDetails by productIdentifiersArray index and increment it
                String subscriptionOfferIdentifier = productIdentifiers.get(
                  productIdIndex.intValue()
                );
                productIdIndex = productIdIndex.intValue() + 1;
                // get the SubscriptionOfferDetails who match the subscriptionOfferIdentifier
                ProductDetails.SubscriptionOfferDetails selectedOfferDetails =
                  null;
                for (ProductDetails.SubscriptionOfferDetails offerDetails : productDetails.getSubscriptionOfferDetails()) {
                  if (
                    offerDetails
                      .getBasePlanId()
                      .equals(subscriptionOfferIdentifier)
                  ) {
                    selectedOfferDetails = offerDetails;
                    break;
                  }
                }
                if (selectedOfferDetails == null) {
                  selectedOfferDetails =
                    productDetails.getSubscriptionOfferDetails().get(0);
                }
                product.put("planIdentifier", productDetails.getProductId());
                product.put("identifier", selectedOfferDetails.getBasePlanId());
                product.put(
                  "price",
                  selectedOfferDetails
                    .getPricingPhases()
                    .getPricingPhaseList()
                    .get(0)
                    .getPriceAmountMicros() /
                  1000000.0
                );
                product.put(
                  "priceString",
                  selectedOfferDetails
                    .getPricingPhases()
                    .getPricingPhaseList()
                    .get(0)
                    .getFormattedPrice()
                );
                product.put(
                  "currencyCode",
                  selectedOfferDetails
                    .getPricingPhases()
                    .getPricingPhaseList()
                    .get(0)
                    .getPriceCurrencyCode()
                );
              }

              product.put("isFamilyShareable", false);
              products.put(product);
            }
            ret.put("products", products);
            closeBillingClient();
            call.resolve(ret);
          }
        }
      );
    } catch (Exception e) {
      closeBillingClient();
      call.reject(e.getMessage());
    }
  }
}
