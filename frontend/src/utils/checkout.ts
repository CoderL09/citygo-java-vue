import type { CheckoutRequest } from '@/api/booking'
/** Retain the same key after an unknown network result; changing the request starts a new attempt. */
export class CheckoutAttempt {
  private fingerprint = ''
  private key = ''
  constructor(private newKey: () => string = () => crypto.randomUUID()) {}
  forRequest(request: CheckoutRequest): string {
    const canonical = { ...request, remark: request.remark.trim(), items: [...request.items].sort((a, b) => a.itemType.localeCompare(b.itemType) || a.itemId - b.itemId) }
    const fingerprint = JSON.stringify(canonical)
    if (fingerprint !== this.fingerprint || !this.key) {
      this.key = this.newKey()
      this.fingerprint = fingerprint
    }
    return this.key
  }
  complete() { this.key = ''; this.fingerprint = '' }
}
