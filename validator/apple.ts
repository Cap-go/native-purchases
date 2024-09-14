interface VerifyResponse {
  status: number;
  receipt: {
    in_app: {
      product_id: string;
      transaction_id: string;
      // ... other fields
    }[];
    // ... other fields
  };
}

export async function validateAppleReceipt(
  receipt: string,
  env: Env
): Promise<VerifyResponse> {
  const { APPLE_SECRET } = env;

  const response = await fetch("https://buy.itunes.apple.com/verifyReceipt", {
    method: "POST",
    body: JSON.stringify({
      "receipt-data": receipt,
      password: APPLE_SECRET,
      "exclude-old-transactions": true,
    }),
  });

  const data: VerifyResponse = await response.json();

  if (data.status === 0) {
    return data;
  } else {
    throw new Error("Invalid receipt");
  }
}