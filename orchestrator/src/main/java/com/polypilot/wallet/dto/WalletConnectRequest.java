package com.polypilot.wallet.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WalletConnectRequest{
        private String address;    // 0x... wallet address the user claims to own
        private String signature;  // EIP-712 signature produced by MetaMask
        private String timestamp;  // must match the timestamp issued in the challenge
        private String nonce;     // "0"
}