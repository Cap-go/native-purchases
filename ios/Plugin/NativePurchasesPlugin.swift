import Foundation
import Capacitor
import StoreKit

/**
 * Please read the Capacitor iOS Plugin Development Guide
 * here: https://capacitorjs.com/docs/plugins/ios
 */
@objc(NativePurchasesPlugin)
public class NativePurchasesPlugin: CAPPlugin {

    private let PLUGIN_VERSION = "2.0.13"

    @objc func purchaseProduct(_ call: CAPPluginCall) async {
        if #available(iOS 15, *) {
            print("purchaseProduct")
            let productIdentifier = call.getString("productIdentifier", "")
            let quantity = call.getInt("quantity", 1)
            if (productIdentifier.isEmpty) {
                call.reject("productIdentifier is Empty, give an id")
                return
            }
            do {
                let products = try await Product.products(for: [productIdentifier])
                let product = products[0]
                var purchaseOptions = Set<Product.PurchaseOption>()
                purchaseOptions.insert(Product.PurchaseOption.quantity(quantity))
                let result = try await product.purchase(options: purchaseOptions)
                print("purchaseProduct result \(result)")
                switch result {
                    case let .success(.verified(transaction)):
                        // Successful purhcase
                        await transaction.finish()
                        call.resolve(["id": transaction.id])
                        break
                    case let .success(.unverified(_, error)):
                        // Successful purchase but transaction/receipt can't be verified
                        // Could be a jailbroken phone
                        call.reject(error.localizedDescription)
                        break
                    case .pending:
                        // Transaction waiting on SCA (Strong Customer Authentication) or
                        // approval from Ask to Buy
                        call.reject("Transaction pending")
                        break
                    case .userCancelled:
                        // ^^^
                        call.reject("User cancelled")
                        break
                    @unknown default:
                        call.reject("Unknown error")
                        break
                }
            } catch {
                print(error)
                call.reject(error.localizedDescription)
            }
        } else {
            print("Not implemented under ios 15")
            call.reject("Not implemented under ios 15")
            return
        }
    }

    @objc func restorePurchases(_ call: CAPPluginCall) async {
        if #available(iOS 15.0, *) {
            print("restorePurchases")
            do {
                try await AppStore.sync()
                call.resolve()
            } catch {
                print(error)
                call.reject(error.localizedDescription)
            }
        } else {
            print("Not implemented under ios 15")
            call.reject("Not implemented under ios 15")
        }
    }

    @objc func getProducts(_ call: CAPPluginCall) async {
        if #available(iOS 15.0, *) {
            let productIdentifiers = call.getArray("productIdentifiers", String.self) ?? []
            do {
                let products = try await Product.products(for: productIdentifiers)
                let productsJson = products.map { (product) -> [String: Any] in
                    return product.dictionary
                }
                call.resolve([
                    "products": productsJson
                ])
            } catch {
                print(error)
                call.reject(error.localizedDescription)
            }
        } else {
            print("Not implemented under ios 15")
            call.reject("Not implemented under ios 15")
        }
    }

}
