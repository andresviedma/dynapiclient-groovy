package dynapiclient.auth

import java.security.MessageDigest

class EncryptionUtils {

    static String md5(String s) {
        MessageDigest digest = MessageDigest.getInstance("MD5")
        digest.update(s.bytes);
        new BigInteger(1, digest.digest()).toString(16).padLeft(32, '0')
    }
}
