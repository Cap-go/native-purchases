import { WebPlugin } from "@capacitor/core";

import type {
  NativePurchasesPlugin,
  CustomerInfo,
  Product,
} from "./definitions";

export const mockCustomerInfo: CustomerInfo = {
  activeSubscriptions: [""],
  allPurchasedProductIdentifiers: [""],
  nonSubscriptionTransactions: [],
  latestExpirationDate: null,
  firstSeen: "2020-01-01T00:00:00.000Z",
  originalAppUserId: "",
  requestDate: "2020-01-01T00:00:00.000Z",
  originalApplicationVersion: "",
  originalPurchaseDate: null,
  managementURL: null,
};

export class NativePurchasesWeb
  extends WebPlugin
  implements NativePurchasesPlugin
{
  async restorePurchases(): Promise<{ customerInfo: CustomerInfo }> {
    console.error("purchasePackage only mocked in web");
    return { customerInfo: mockCustomerInfo };
  }

  async getProducts(options: {
    productIdentifiers: string[];
  }): Promise<{ products: Product[] }> {
    console.error("getProducts only mocked in web " + options);
    return { products: [] };
  }

  async purchaseProduct(options: {
    productIdentifier: string;
    planIdentifier: string;
    quantity: number;
  }): Promise<{ transactionId: string }> {
    console.error("purchaseProduct only mocked in web" + options);
    return { transactionId: "transactionId" };
  }

  async isBillingSupported(): Promise<{ isBillingSupported: boolean }> {
    console.error("isBillingSupported only mocked in web");
    return { isBillingSupported: false };
  }
  async getPluginVersion(): Promise<{ version: string }> {
    console.warn("Cannot get plugin version in web");
    return { version: "default" };
  }
}
