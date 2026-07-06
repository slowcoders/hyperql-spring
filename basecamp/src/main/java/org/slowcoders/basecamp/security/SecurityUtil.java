package org.slowcoders.basecamp.security;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * 암호화 클래스
 *
 * @author Byun Sang June
 * @version 1.0
 * @since 2022. 3. 2.
 */
@Slf4j
@NoArgsConstructor
@Component
public class SecurityUtil  {

	@Value("${basecamp.security.aes256.key:any-text-16bytes}")
	private String key;

	@Value("${basecamp.security.aes256.iv:any-text-16bytes}")
	private String iv;

	private static final String UTF8 = "UTF-8";
	private static String alg = "AES/CBC/PKCS5Padding";

//	@Override
    public String encrypt(String text) {
		try {
			Cipher cipher = getCipher(Cipher.ENCRYPT_MODE);
            byte[] encrypted = cipher.doFinal(text.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (IllegalArgumentException e) {
			log.error("Invalid input for encryption. Text is null or invalid encoding.", e);
			return "";
		} catch (Exception e) {
            log.error(e.getMessage(), e);
            return "";
        }
	}
	
//	@Override
    public String decrypt(String cipherText) {
		try {
			Cipher cipher = getCipher(Cipher.DECRYPT_MODE);
			byte[] decodedBytes = Base64.getDecoder().decode(cipherText);
            byte[] decrypted = cipher.doFinal(decodedBytes);
            return new String(decrypted, StandardCharsets.UTF_8);
		} catch (IllegalArgumentException e) {
			log.error("Invalid Base64 format for decryption. Input: {}", cipherText, e);
			return "";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
            return "";
		}
	}
	
	/**
	 * 
	 * @param  "암/복호화 mode"
	 * @return Cipher
	 */
	private Cipher getCipher(int mode) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException {
		Cipher cipher = Cipher.getInstance(alg);
		SecretKey sKey = new SecretKeySpec(key.getBytes(), "AES");
		IvParameterSpec ivParameterSpec = new IvParameterSpec(iv.getBytes());
		cipher.init(mode, sKey, ivParameterSpec);
		return cipher;
	}
	
}
