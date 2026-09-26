/**
 * Shape returned by every authentication path — `POST /api/auth/login`
 * (admin / demo) and `POST /api/auth/siwe/verify` (wallet). For the SIWE flow
 * `email` carries the lower-cased `0x…` wallet address the user signed in with.
 */
export type LoginResponse = {
    token: string
    email: string
    role: string
}
