import { WebPlugin } from '@capacitor/core';

import type { NativePurchasesPlugin } from './definitions';

export class NativePurchasesWeb extends WebPlugin implements NativePurchasesPlugin {
  async echo(options: { value: string }): Promise<{ value: string }> {
    console.log('ECHO', options);
    return options;
  }
}
