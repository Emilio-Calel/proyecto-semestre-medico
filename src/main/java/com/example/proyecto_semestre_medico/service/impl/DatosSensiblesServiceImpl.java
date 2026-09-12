package com.example.proyecto_semestre_medico.service.impl;

import com.example.proyecto_semestre_medico.service.DatosSensiblesService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class DatosSensiblesServiceImpl
        implements DatosSensiblesService {

    private static final String AES_ALGORITHM =
            "AES/GCM/NoPadding";

    private static final String HMAC_ALGORITHM =
            "HmacSHA256";

    private static final int GCM_TAG_LENGTH = 128;
    private static final int IV_LENGTH = 12;

    private final SecretKeySpec aesKey;
    private final SecretKeySpec hmacKey;

    private final SecureRandom secureRandom =
            new SecureRandom();

    public DatosSensiblesServiceImpl(
            @Value("${app.crypto.secret}") String secret
    ) {

        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException(
                    "APP_CRYPTO_SECRET debe tener al menos "
                            + "32 caracteres"
            );
        }

        try {

            MessageDigest sha256 =
                    MessageDigest.getInstance("SHA-256");

            byte[] keyBytes = sha256.digest(
                    secret.getBytes(StandardCharsets.UTF_8)
            );

            this.aesKey =
                    new SecretKeySpec(keyBytes, "AES");

            this.hmacKey =
                    new SecretKeySpec(
                            keyBytes,
                            HMAC_ALGORITHM
                    );

        } catch (Exception e) {
            throw new IllegalStateException(
                    "No fue posible inicializar cifrado",
                    e
            );
        }
    }

    @Override
    public String cifrar(String valor) {

        if (valor == null || valor.isBlank()) {
            return null;
        }

        try {

            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher =
                    Cipher.getInstance(AES_ALGORITHM);

            GCMParameterSpec spec =
                    new GCMParameterSpec(
                            GCM_TAG_LENGTH,
                            iv
                    );

            cipher.init(
                    Cipher.ENCRYPT_MODE,
                    aesKey,
                    spec
            );

            byte[] cifrado = cipher.doFinal(
                    valor.getBytes(
                            StandardCharsets.UTF_8
                    )
            );

            byte[] resultado =
                    new byte[iv.length + cifrado.length];

            System.arraycopy(
                    iv,
                    0,
                    resultado,
                    0,
                    iv.length
            );

            System.arraycopy(
                    cifrado,
                    0,
                    resultado,
                    iv.length,
                    cifrado.length
            );

            return Base64.getEncoder()
                    .encodeToString(resultado);

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Error al cifrar información",
                    e
            );
        }
    }

    @Override
    public String descifrar(String valorCifrado) {

        if (valorCifrado == null
                || valorCifrado.isBlank()) {
            return null;
        }

        try {

            byte[] resultado =
                    Base64.getDecoder()
                            .decode(valorCifrado);

            byte[] iv = new byte[IV_LENGTH];

            byte[] cifrado =
                    new byte[
                            resultado.length
                                    - IV_LENGTH
                            ];

            System.arraycopy(
                    resultado,
                    0,
                    iv,
                    0,
                    IV_LENGTH
            );

            System.arraycopy(
                    resultado,
                    IV_LENGTH,
                    cifrado,
                    0,
                    cifrado.length
            );

            Cipher cipher =
                    Cipher.getInstance(AES_ALGORITHM);

            GCMParameterSpec spec =
                    new GCMParameterSpec(
                            GCM_TAG_LENGTH,
                            iv
                    );

            cipher.init(
                    Cipher.DECRYPT_MODE,
                    aesKey,
                    spec
            );

            byte[] plano =
                    cipher.doFinal(cifrado);

            return new String(
                    plano,
                    StandardCharsets.UTF_8
            );

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Error al descifrar información",
                    e
            );
        }
    }

    @Override
    public String generarHashBusqueda(String valor) {

        if (valor == null || valor.isBlank()) {
            return null;
        }

        try {

            Mac mac =
                    Mac.getInstance(HMAC_ALGORITHM);

            mac.init(hmacKey);

            byte[] hash = mac.doFinal(
                    valor.trim()
                            .getBytes(
                                    StandardCharsets.UTF_8
                            )
            );

            return HexFormat.of()
                    .formatHex(hash);

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Error generando hash de búsqueda",
                    e
            );
        }
    }
}