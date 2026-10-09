package com.cookies.editor.local;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Locale;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** Device-local profiles. No connection to Google or the reference backend. */
public final class LocalAccounts {
    private static final int ITERATIONS = 120000;
    private LocalAccounts() {}
    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences("CookiesLocalAccounts", Context.MODE_PRIVATE);
    }
    private static String key(String email) throws Exception {
        byte[] bytes = MessageDigest.getInstance("SHA-256").digest(email.getBytes("UTF-8"));
        return Base64.encodeToString(bytes, Base64.NO_WRAP | Base64.URL_SAFE);
    }
    private static byte[] derive(char[] password, byte[] salt, String algorithm) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, 256);
        try { return SecretKeyFactory.getInstance(algorithm).generateSecret(spec).getEncoded(); }
        finally { spec.clearPassword(); }
    }
    public static String authenticate(Context c, String email, char[] password, boolean register) {
        email = email.trim().toLowerCase(Locale.ROOT);
        try {
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) return "أدخل بريدًا إلكترونيًا صحيحًا.";
            if (password.length < 8) return "استخدم كلمة مرور من 8 أحرف على الأقل.";
            SharedPreferences p = prefs(c);
            String k = key(email), record = p.getString("account_" + k, null);
            if (register) {
                if (record != null) return "الملف موجود على هذا الجهاز. اختر تسجيل الدخول.";
                byte[] salt = new byte[24]; new SecureRandom().nextBytes(salt);
                String algorithm = android.os.Build.VERSION.SDK_INT >= 26 ? "PBKDF2WithHmacSHA256" : "PBKDF2WithHmacSHA1";
                byte[] hash = derive(password, salt, algorithm);
                record = algorithm + ":" + Base64.encodeToString(salt, Base64.NO_WRAP) + ":" + Base64.encodeToString(hash, Base64.NO_WRAP);
                Arrays.fill(hash, (byte) 0);
                if (!p.edit().putString("account_" + k, record).putString("session_email", email).commit()) return "تعذر حفظ الملف المحلي.";
            } else {
                if (record == null) return "لا يوجد ملف بهذا البريد على الجهاز. أنشئ ملفًا محليًا أولًا.";
                String[] parts = record.split(":");
                byte[] hash = derive(password, Base64.decode(parts[1], Base64.NO_WRAP), parts[0]);
                boolean valid = MessageDigest.isEqual(hash, Base64.decode(parts[2], Base64.NO_WRAP));
                Arrays.fill(hash, (byte) 0);
                if (!valid) return "البريد أو كلمة المرور غير صحيحين.";
                if (!p.edit().putString("session_email", email).commit()) return "تعذر حفظ الجلسة.";
            }
            return null;
        } catch (Exception error) { return "تعذر فتح الملف المحلي. حاول مرة أخرى."; }
        finally { Arrays.fill(password, '\0'); }
    }
    public static void guest(Context c) { prefs(c).edit().putString("session_email", "guest").apply(); }
    public static String email(Context c) { return prefs(c).getString("session_email", "guest"); }
    public static void signOut(Context c) { prefs(c).edit().remove("session_email").apply(); }
}
