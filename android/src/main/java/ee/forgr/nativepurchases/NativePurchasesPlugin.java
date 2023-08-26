package ee.forgr.nativepurchases;

import android.util.Log;
import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.AcknowledgePurchaseResponseListener;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.ProductDetailsResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.android.billingclient.api.SkuDetailsResponseListener;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import org.json.JSONArray;
import org.json.JSONException;

@CapacitorPlugin(name = "NativePurchases")
public class NativePurchasesPlugin extends Plugin {

  public final String PLUGIN_VERSION = "2.0.13";
  private BillingClient billingClient;

  private void isBillingSupported(PluginCall call) {
    JSObject ret = new JSObject();
    ret.put("isBillingSupported", true);
    call.resolve();
  }

  private void handlePurchase(Purchase purchase, PluginCall purchaseCall) {
    if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
      // Grant entitlement to the user, then acknowledge the purchase
      acknowledgePurchase(purchase.getPurchaseToken());

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
            "NativePurchases",
            "onAcknowledgePurchaseResponse" + billingResult
          );
        }
      }
    );
  }

  private void initBillingClient(PluginCall purchaseCall) {
    if (billingClient != null) {
      billingClient.endConnection();
      billingClient = null;
    }
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
              Log.i("NativePurchases", "onPurchasesUpdated" + billingResult);
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
                Log.i("NativePurchases", "onPurchasesUpdated" + billingResult);
                if (purchaseCall != null) {
                  purchaseCall.reject("Purchase is not purchased");
                }
              }
              billingClient.endConnection();
              billingClient = null;
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
    if (planIdentifier.isEmpty()) {
      // Handle error: quantity is less than 1
      call.reject("productType is empty");
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
        .setProductId(productIdentifier)
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
    billingClient.queryProductDetailsAsync(
      params,
      new ProductDetailsResponseListener() {
        public void onProductDetailsResponse(
          BillingResult billingResult,
          List<ProductDetails> productDetailsList
        ) {
          if (productDetailsList.size() == 0) {
            billingClient.endConnection();
            billingClient = null;
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
                if (offerDetails.getOfferId().equals(planIdentifier)) {
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
            "NativePurchases",
            "onProductDetailsResponse2" + billingResult2
          );
        }
      }
    );
  }

  @PluginMethod
  public void restorePurchases(PluginCall call) {
    Log.d("NativePurchases", "restorePurchases");
    this.initBillingClient(null);
    call.resolve();
  }

  @PluginMethod
  public void getProducts(PluginCall call) {
    JSONArray productIdentifiersArray = call.getArray("productIdentifiers");
    if (productIdentifiersArray == null) {
      call.reject("productIdentifiers is missing");
      return;
    }
    String productType = call.getString("productType", "inapp");
    List<String> productIdentifiers = new ArrayList<>();

    for (int i = 0; i < productIdentifiersArray.length(); i++) {
      try {
        productIdentifiers.add(productIdentifiersArray.getString(i));
      } catch (JSONException e) {
        e.printStackTrace();
      }
    }

    Log.d("NativePurchases", "getProducts: " + productIdentifiers);
    List<QueryProductDetailsParams.Product> productList = new ArrayList<>();
    for (String productIdentifier : productIdentifiers) {
      productList.add(
        QueryProductDetailsParams.Product
          .newBuilder()
          .setProductId(productIdentifier)
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
    billingClient.queryProductDetailsAsync(
      params,
      new ProductDetailsResponseListener() {
        public void onProductDetailsResponse(
          BillingResult billingResult,
          List<ProductDetails> productDetailsList
        ) {
          if (productDetailsList.size() == 0) {
            billingClient.endConnection();
            billingClient = null;
            call.reject("Product not found");
            return;
          }
          Log.i(
            "NativePurchases",
            "onProductDetailsResponse" + billingResult + productDetailsList
          );
          // Process the result
          JSObject ret = new JSObject();
          JSONArray products = new JSONArray();
          for (ProductDetails productDetails : productDetailsList) {
            JSObject product = new JSObject();
            product.put("identifier", productDetails.getProductId());
            product.put("title", productDetails.getTitle());
            product.put("description", productDetails.getDescription());
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
            product.put("isFamilyShareable", false);
            products.put(product);
          }
          ret.put("products", products);
          billingClient.endConnection();
          billingClient = null;
          call.resolve(ret);
        }
      }
    );
  }
}
