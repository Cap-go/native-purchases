package ee.forgr.nativepurchases;

import android.util.Log;
import androidx.annotation.NonNull;
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

  @PluginMethod
  public void restorePurchases(PluginCall call) {
    Log.i("NativePurchases", "restorePurchases");
    call.resolve();
  }

  @PluginMethod
  public void getProducts(PluginCall call) {
    List<String> productIdentifiers = new ArrayList<String>();
    JSONArray productIdentifiersArray = call.getArray("productIdentifiers");
    Log.i("NativePurchases", "getProducts: " + productIdentifiersArray);
    call.resolve();
  }

  @PluginMethod
  public void purchaseProduct(PluginCall call) {
    String productIdentifier = call.getString("productIdentifier");
    Log.i("NativePurchases", "purchaseProduct: " + productIdentifier);
    call.resolve();
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
