/*
 * Copyright 2018 Redlink GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.redlink.lucene.analysis.util;

import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class HashUtil {


    private HashUtil(){
    }

    public static byte[] md5(InputStream is) throws IOException {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 is not supported", e);
        }
        try (DigestInputStream dis = new DigestInputStream(is, md)) {
            byte[] buff = new byte[4096];
            //noinspection StatementWithEmptyBody
            while (dis.read(buff) > 0) {
                // just read to get the Digest filled...
            }
            return md.digest();
        }
    }

}
