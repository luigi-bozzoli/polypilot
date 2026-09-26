package com.polypilot.wallet.dto;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class L2Credentials {

    @JsonProperty("api_key")
    private String apiKey;

    @JsonProperty("secret")
    private String secret;

    @JsonProperty("passphrase")
    private String passphrase;
}