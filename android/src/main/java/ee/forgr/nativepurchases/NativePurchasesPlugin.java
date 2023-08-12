package ee.forgr.nativepurchases;

import android.util.Log;
import androidx.annotation.NonNull;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.AcknowledgePurchaseResponseListener;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.android.billingclient.api.SkuDetailsResponseListener;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.google.common.base.CaseFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

@CapacitorPlugin(name = "NativePurchases")
public class NativePurchasesPlugin extends Plugin {

  public final String PLUGIN_VERSION = "2.0.13";

  private BillingClient billingClient;
  private PluginCall purchaseCall;

  @Override
  public void load() {
    super.load();
      billingClient = BillingClient.newBuilder(getContext())
          .setListener(new PurchasesUpdatedListener() {
              @Override
              public void onPurchasesUpdated(BillingResult billingResult, List<Purchase> purchases) {
                  if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null) {
                      for (Purchase purchase : purchases) {
                          handlePurchase(purchase);
                      }
                  } else {
                      // Handle any other error codes.
                  }
              }
          })
          .enablePendingPurchases()
          .build();
  }

  private void handlePurchase(Purchase purchase) {
      if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
          // Grant entitlement to the user, then acknowledge the purchase
          acknowledgePurchase(purchase.getPurchaseToken());

          JSObject ret = new JSObject();
          ret.put("transactionId", purchase.getPurchaseToken());
          purchaseCall.resolve(ret);
      } else if (purchase.getPurchaseState() == Purchase.PurchaseState.PENDING) {
          // Here you can confirm to the user that they've started the pending
          // purchase, and to complete it, they should follow instructions that are
          // given to them. You can also choose to remind the user to complete the
          // purchase if you detect that it is still pending.
      }
  }

  private void acknowledgePurchase(String purchaseToken) {
      AcknowledgePurchaseParams acknowledgePurchaseParams =
          AcknowledgePurchaseParams.newBuilder()
              .setPurchaseToken(purchaseToken)
              .build();
      billingClient.acknowledgePurchase(acknowledgePurchaseParams, new AcknowledgePurchaseResponseListener() {
          @Override
          public void onAcknowledgePurchaseResponse(BillingResult billingResult) {
              // Handle the result of the acknowledge purchase
          }
      });
  }

  @PluginMethod
  public void purchaseProduct(PluginCall call) {
      String productIdentifier = call.getString("productIdentifier");
      Log.d("CapacitorPurchases", "purchaseProduct: " + productIdentifier);
      purchaseCall = call;

      if (billingClient.isReady()) {
          List<String> skuList = new ArrayList<>();
          skuList.add(productIdentifier);
          SkuDetailsParams.Builder params = SkuDetailsParams.newBuilder();
          params.setSkusList(skuList).setType(BillingClient.SkuType.INAPP);

          billingClient.querySkuDetailsAsync(params.build(), new SkuDetailsResponseListener() {
              @Override
              public void onSkuDetailsResponse(BillingResult billingResult, List<SkuDetails> skuDetailsList) {
                  if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && skuDetailsList != null) {
                      for (SkuDetails skuDetails : skuDetailsList) {
                          BillingFlowParams flowParams = BillingFlowParams.newBuilder()
                                  .setSkuDetails(skuDetails)
                                  .build();
                          billingClient.launchBillingFlow(getActivity(), flowParams);
                      }
                  }
              }
          });
      }
  }

  @PluginMethod
  public void restorePurchases(PluginCall call) {
      Log.d("NativePurchases", "restorePurchases");

      if (billingClient.isReady()) {
          Purchase.PurchasesResult purchasesResult = billingClient.queryPurchases(BillingClient.SkuType.INAPP);

          if (purchasesResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
              List<Purchase> purchasesList = purchasesResult.getPurchasesList();

              JSObject ret = new JSObject();
              JSONArray purchases = new JSONArray();

              for (Purchase purchase : purchasesList) {
                  JSObject purchaseObject = new JSObject();
                  purchaseObject.put("productId", purchase.getSku());
                  purchaseObject.put("transactionId", purchase.getPurchaseToken());
                  purchaseObject.put("purchaseTime", purchase.getPurchaseTime());
                  purchases.put(purchaseObject);
              }

              ret.put("purchases", purchases);
              call.resolve(ret);
          } else {
              // Handle any other error codes.
              call.reject("Unknown error");
          }
      }
  }


@PluginMethod
public void getProducts(PluginCall call) {
    JSONArray productIdentifiersArray = call.getArray("productIdentifiers");
    List<String> productIdentifiers = new ArrayList<>();

    for (int i = 0; i < productIdentifiersArray.length(); i++) {
        try {
            productIdentifiers.add(productIdentifiersArray.getString(i));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    Log.d("NativePurchases", "getProducts: " + productIdentifiers);

    if (billingClient.isReady()) {
        SkuDetailsParams.Builder params = SkuDetailsParams.newBuilder();
        params.setSkusList(productIdentifiers).setType(BillingClient.SkuType.INAPP);

        billingClient.querySkuDetailsAsync(params.build(), new SkuDetailsResponseListener() {
            @Override
            public void onSkuDetailsResponse(BillingResult billingResult, List<SkuDetails> skuDetailsList) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && skuDetailsList != null) {
                    JSObject ret = new JSObject();
                    JSONArray products = new JSONArray();

                    for (SkuDetails skuDetails : skuDetailsList) {
                        JSObject product = new JSObject();
                        product.put("productId", skuDetails.getSku());
                        product.put("title", skuDetails.getTitle());
                        product.put("description", skuDetails.getDescription());
                        product.put("price", skuDetails.getPrice());
                        product.put("priceAmountMicros", skuDetails.getPriceAmountMicros());
                        product.put("priceCurrencyCode", skuDetails.getPriceCurrencyCode());
                        product.put("type", skuDetails.getType());
                        products.put(product);
                    }

                    ret.put("products", products);
                    call.resolve(ret);
                } else {
                    // Handle any other error codes.
                }
            }
        });
    }
}

  //================================================================================
  // Private methods
  //================================================================================

  private static JSObject convertMapToJson(Map<String, ?> readableMap) {
    JSObject object = new JSObject();

    for (Map.Entry<String, ?> entry : readableMap.entrySet()) {
      String camelKey = entry.getKey().contains("_")
        ? CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, entry.getKey())
        : entry.getKey();
      if (entry.getValue() == null) {
        object.put(camelKey, JSONObject.NULL);
      } else if (entry.getValue() instanceof Map) {
        object.put(
          camelKey,
          convertMapToJson((Map<String, Object>) entry.getValue())
        );
      } else if (entry.getValue() instanceof Object[]) {
        object.put(
          camelKey,
          convertArrayToJsonArray((Object[]) entry.getValue())
        );
      } else if (entry.getValue() instanceof List) {
        object.put(
          camelKey,
          convertArrayToJsonArray(((List) entry.getValue()).toArray())
        );
      } else if (entry.getValue() != null) {
        Object value = entry.getValue();
        if (camelKey == "priceString") {
          String currency_symbol =
            ((String) value).replaceAll("\\d", "")
              .replace(".", "")
              .replace(",", "");
          object.put("currencySymbol", currency_symbol);
        }
        if (camelKey == "title") {
          // value = ((String) value).replace("(" + AppName + ")", "");
          value = ((String) value).replaceAll("\\((.*?)\\)", ""); // TODO find better implementation
        }
        object.put(camelKey, value);
      }
    }

    return object;
  }

  private static JSONArray convertArrayToJsonArray(Object[] array) {
    JSONArray writableArray = new JSONArray();
    for (Object item : array) {
      if (item == null) {
        writableArray.put(JSONObject.NULL);
      } else if (item instanceof Map) {
        writableArray.put(convertMapToJson((Map<String, Object>) item));
      } else if (item instanceof Object[]) {
        writableArray.put(convertArrayToJsonArray((Object[]) item));
      } else if (item instanceof List) {
        writableArray.put(convertArrayToJsonArray(((List) item).toArray()));
      } else {
        writableArray.put(item);
      }
    }
    return writableArray;
  }
}
