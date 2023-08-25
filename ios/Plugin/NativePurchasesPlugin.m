#import <Foundation/Foundation.h>
#import <Capacitor/Capacitor.h>

// Define the plugin using the CAP_PLUGIN Macro, and
// each method the plugin supports using the CAP_PLUGIN_METHOD macro.
CAP_PLUGIN(NativePurchasesPlugin, "NativePurchases",
        CAP_PLUGIN_METHOD(getProducts, CAPPluginReturnPromise);
        CAP_PLUGIN_METHOD(purchaseProduct, CAPPluginReturnPromise);
        CAP_PLUGIN_METHOD(restorePurchases, CAPPluginReturnPromise);
        CAP_PLUGIN_METHOD(isBillingSupported, CAPPluginReturnPromise);
)
