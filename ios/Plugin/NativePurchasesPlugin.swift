import Foundation
import Capacitor
import StoreKit

/**
 * Please read the Capacitor iOS Plugin Development Guide
 * here: https://capacitorjs.com/docs/plugins/ios
 */
@objc(NativePurchasesPlugin)
public class NativePurchasesPlugin: CAPPlugin {
    public let identifier = "NativePurchasesPlugin"
    public let jsName = "NativePurchases"
    public let pluginMethods: [CAPPluginMethod] = [
        CAPPluginMethod(name: "isBillingSupported", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "purchaseProduct", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "restorePurchases", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "getProducts", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "getProduct", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "getPluginVersion", returnType: CAPPluginReturnPromise),
    ]

    private let PLUGIN_VERSION = "0.0.25"

    @objc func getPluginVersion(_ call: CAPPluginCall) {
        call.resolve(["version": self.PLUGIN_VERSION])
    }

    @objc func isBillingSupported(_ call: CAPPluginCall) {
        if #available(iOS 15, *) {
            call.resolve([
                "isBillingSupported": true
            ])
        } else {
            call.resolve([
                "isBillingSupported": false
            ])
        }
    }

    @objc func purchaseProduct(_ call: CAPPluginCall) {
        if #available(iOS 15, *) {
            print("purchaseProduct")
            let productIdentifier = call.getString("productIdentifier", "")
            let quantity = call.getInt("quantity", 1)
            if productIdentifier.isEmpty {
                call.reject("productIdentifier is Empty, give an id")
                return
            }

            Task {
                do {
                    let products = try await Product.products(for: [productIdentifier])
                    guard let product = products.first else {
                        call.reject("Cannot find product for id \(productIdentifier)")
                        return
                    }
                    var purchaseOptions = Set<Product.PurchaseOption>()
                    purchaseOptions.insert(Product.PurchaseOption.quantity(quantity))
                    let result = try await product.purchase(options: purchaseOptions)
                    print("purchaseProduct result \(result)")
                    switch result {
                    case let .success(.verified(transaction)):
                        // Successful purhcase
                        await transaction.finish()
                        call.resolve(["transactionId": transaction.id])
                    case let .success(.unverified(_, error)):
                        // Successful purchase but transaction/receipt can't be verified
                        // Could be a jailbroken phone
                        call.reject(error.localizedDescription)
                    case .pending:
                        // Transaction waiting on SCA (Strong Customer Authentication) or
                        // approval from Ask to Buy
                        call.reject("Transaction pending")
                    case .userCancelled:
                        // ^^^
                        call.reject("User cancelled")
                    @unknown default:
                        call.reject("Unknown error")
                    }
                } catch {
                    print(error)
                    call.reject(error.localizedDescription)
                }
            }
        } else {
            print("Not implemented under ios 15")
            call.reject("Not implemented under ios 15")
        }
    }

    @objc func restorePurchases(_ call: CAPPluginCall) {
        if #available(iOS 15.0, *) {
            print("restorePurchases")
            DispatchQueue.global().async {
                Task {
                    do {
                        try await AppStore.sync()
                        // make finish() calls for all transactions and consume all consumables
                        for transaction in SKPaymentQueue.default().transactions {
                            SKPaymentQueue.default().finishTransaction(transaction)
                        }
                        call.resolve()
                    } catch {
                        call.reject(error.localizedDescription)
                    }
                }
            }
        } else {
            print("Not implemented under ios 15")
            call.reject("Not implemented under ios 15")
        }
    }

    @objc func getProducts(_ call: CAPPluginCall) {
        if #available(iOS 15.0, *) {
            let productIdentifiers = call.getArray("productIdentifiers", String.self) ?? []
            DispatchQueue.global().async {
                Task {
                    do {
                        let products = try await Product.products(for: productIdentifiers)
                        let productsJson: [[String: Any]] = products.map { $0.dictionary }
                        call.resolve([
                            "products": productsJson
                        ])
                    } catch {
                        print(error)
                        call.reject(error.localizedDescription)
                    }
                }
            }
        } else {
            print("Not implemented under ios 15")
            call.reject("Not implemented under ios 15")
        }
    }

    @objc func getProduct(_ call: CAPPluginCall) {
        if #available(iOS 15.0, *) {
            let productIdentifier = call.getString("productIdentifier") ?? ""
            if productIdentifier.isEmpty {
                call.reject("productIdentifier is empty")
                return
            }

            DispatchQueue.global().async {
                Task {
                    do {
                        let products = try await Product.products(for: [productIdentifier])
                        if let product = products.first {
                            let productJson = product.dictionary
                            call.resolve(["product": productJson])
                        } else {
                            call.reject("Product not found")
                        }
                    } catch {
                        print(error)
                        call.reject(error.localizedDescription)
                    }
                }
            }
        } else {
            print("Not implemented under iOS 15")
            call.reject("Not implemented under iOS 15")
        }
    }

}
