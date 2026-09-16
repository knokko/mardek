Generate keystore & key:
```
keytool -genkeypair -keystore key-store.p12 -storetype PKCS12 -storepass MARDEK_EDITOR_SERVER -alias mardek_editor_key -keyalg RSA -keysize 2048 -validity 99999 -dname "CN=localhost"
```

Generate public key:
```
keytool -exportcert -keystore key-store.p12 -storepass MARDEK_EDITOR_SERVER -alias mardek_editor_key -rfc -file public-certificate.pem
```
