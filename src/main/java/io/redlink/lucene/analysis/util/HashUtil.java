package io.redlink.lucene.analysis.util;

import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class HashUtil {

    
    private HashUtil(){
        throw new RuntimeException();
    }

        public static final byte[] md5(InputStream is) throws IOException {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 is not supported", e);
        }
        try (DigestInputStream dis = new DigestInputStream(is, md)) {
            byte[] buff = new byte[4096];
            while (dis.read(buff) > 0); // just read to get the Digest filled...
            return md.digest();
        }
    }
        
}
