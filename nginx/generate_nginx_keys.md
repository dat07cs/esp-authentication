# Generate nginx key, cert, and cacert
```shell
# Generate private key
openssl genrsa -out nginx.key 2048
# Generate server self-signed certificate from private key
# Must specify CN and subjectAltName (can use IP address)
openssl req -x509 -nodes -days 365 -key nginx.key -out nginx.crt \
  -nodes -subj "/C=VN/ST=HCM/L=HCM/O=Palexy/OU=Dev/CN=bookstore.palexy.com" \
  -addext "subjectAltName=DNS:bookstore.palexy.com,IP:34.87.33.91"
# Generate cacert from self-signed certificate to use for testing purpose
openssl x509 -in nginx.crt -out nginx.pem -outform PEM
```

# Utilities to view certificates' info
- From cert file
```shell
openssl x509 -noout -text -in nginx.crt
```
- From server
```shell
echo | openssl s_client -showcerts -servername www.google.com -connect 142.250.199.68:443 -verify 99 -verify_return_error
```
